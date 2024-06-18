import os
import re
import time
import torch
import numpy as np
import asyncio
import torchaudio
from util_actor import Actor, ActorStoppedException
from util_itr import teeThreadSafeEager, words
from util_play import SdActor, SdEvent
from collections.abc import Iterator
from util_dir_cache import cache_file
from util_http import HttpHandler
from itertools import chain
from util_wrt import Writer
from util_str import *
from util_fut import *
from util_ctx import *
from util_num import *
from imports import *

@dataclass
class TtsPause:
    ms: int

@dataclass
class TtsHistory:
    text: str
    skippable: bool
    location: Location

class Tts(Actor):
    def __init__(self, speakOn: bool, tts, audioOutDef: str, write: Writer):
        super().__init__("tts-preprocessor", 'Tts-preprocessor', None, write, True)
        self.tts = tts
        self.play = SdActor(audioOutDef, write)
        self.speakOn = speakOn
        self._skip = False
        self._stop_event = (None, None, None, None, None)
        self.history: [TtsHistory] = []

    def start(self):
        super().start()
        self.tts.start()
        self.play.start()

    def stop(self):
        super().stop()
        self.tts.stop()
        self.play.stop()

    def skip(self):
        self.tts.skip()
        self.play.skip()

    def skipWithoutSound(self):
        self.tts.skip()

    def skippable(self, event: str, location: Location) -> Future[None]:
        location = self._locationOrLast(location)
        f = Future()
        if not self.speakOn:
            f.set_result(None)
        else:
            self.write('SYS: ' + event)
            self.queue.put((words(event), True, False, location, f))
        return f

    def repeatLast(self):
        if self.history and self.speakOn:
            text, skippable, location = self.history[-1]
            self.write('SYS: ' + text)
            self.queue.put((words(text), skippable, True, location, Future()))

    def __call__(self, event: str | Iterator, location: Location) -> Future[None]:
        location = self._locationOrLast(location)
        f = Future()
        if not self.speakOn:
            f.set_result(None)
        elif isinstance(event, str):
            self.write('SYS: ' + event)
            self.queue.put((words(event), False, False, location, f))
        elif isinstance(event, Iterator):
            self.queue.put((event, True, False, location, f))
        return f

    def speakPause(self, ms: int) -> Future[None]:
        f = Future()
        self.queue.put((TtsPause(ms), True, False, None, f))

    def _locationOrLast(self, location: Location) -> Location:
        if len(location)>0: return location
        if len(self.history)>0: self.history[-1].location
        return CTX.location

    def _loop(self):
        from stream2sentence import generate_sentences
            
        with (self._looping()):
            while not self._stop:
                with self._loopProcessEvent() as (event, skippable, repeated, location, f):
                    if event is None:
                        break
                    elif isinstance(event, TtsPause):
                        futureOnDone(self.play.playEvent(SdEvent.pause(event.ms, True), location), complete_also(f))
                    else:
                        text = ''
                        try:
                            # start
                            self.tts.gen(None, skippable=False)
                            self.play.playEvent(SdEvent.boundary(), location)
                            fAll = []
                            # sentences tts
                            for sentence in generate_sentences(event, cleanup_text_links=True, cleanup_text_emojis=True):
                                text = text + sentence
                                if (len(sentence)>0): fAll.append(flatMap(self.tts.gen(sentence, skippable=skippable), lambda it: self.play.playEvent(it, location)))

                            # join tts results
                            if len(fAll)>0: fResult = fAll[-1]
                            else: fResult = futureCompleted(None)

                            # end
                            futureOnDone(fResult, complete_also(f))
                            self.tts.gen(None, skippable=False)
                            self.play.playEvent(SdEvent.boundary(), location)
                            
                            # history
                            if not repeated: self.history.append(TtsHistory(text, skippable, location))
                            
                        except Exception as e:
                            f.set_exception(e)
                            raise e


class TtsBase(Actor):
    def __init__(self, name: str, write: Writer):
        super().__init__("tts", name, None, write, True)
        self.max_text_length = 400
        self._skip = False
        self._stop_event = (None, None, None)
        self._loopResult = None

    def skip(self):
        self._skip = True

    def skipWithoutSound(self):
        self._skip = True

    def gen(self, text: str, skippable: bool) -> Future[SdEvent]:
        """
        Adds the text to queue
        """
        f = Future()
        self.queue.put((text, skippable, f))
        return f

    def _get_event_text(self, e: (str, bool)) -> str | None:
        if e is None: return e
        if isinstance(e[0], TtsPause): f'pause({e[0].ms}ms)'
        else: return e[0]

    def _get_next_event(self) -> (str, bool, Future):
        e = self._get_next_event_impl()
        if e is None: raise ActorStoppedException()
        return e

    def _get_next_event_impl(self) -> (str, bool, Future):
        """
        :return: next element from queue, skipping over skippable elements if _skip=True, blocks until then
        """
        while not self._stop:
            textRaw, skippable, fut = self.queue.get()

            # skip skippable
            if self._skip and skippable:
                fut.set_result(SdEvent.empty())
                continue

            self._skip = False

            # skip boundary value
            if textRaw is None:
                fut.set_result(SdEvent.boundary())
                continue

            # skip empty value
            if len(textRaw.strip()) == 0:
                fut.set_result(SdEvent.empty())
                continue

            # gather all elements that are already ready (may improve tts quality)
            if skippable:
                fs = [fut]
                ts = textRaw
                while True:
                    if self.queue.not_empty: break
                    t, s, f = self.queue[0]
                    if t is None or not s: break
                    if len(ts) + len(t) > self.max_text_length: break
                    ts = ts + t
                    fs.append(f)
                    self.queue.get_nowait()
                if len(fs)>1:
                    fAll = Future()
                    for f in fs: futureOnDone(fAll, complete_also(f))
                    return (textRaw, skippable, fAll)

            return (textRaw, skippable, fut)

    def _cache_dir(self, *names: str):
        dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", *[name.replace('.','-') for name in names])
        if not os.path.exists(dir): os.makedirs(dir)
        return dir

    def _cache_file_try(self, cache_name: str, text: str) -> (str, bool, bool):
        # compute cache dir
        self.cache_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", cache_name)
        # prepare cache dir
        if not os.path.exists(self.cache_dir): os.makedirs(self.cache_dir)
        # compute cache file
        audio_file, audio_file_exists = cache_file(text, self.cache_dir)
        cache_used = len(text) < 100
        return (audio_file, audio_file_exists, cache_used)

    @contextmanager
    def _loopProcessEventFut(self, cacheName: str, sample_rate: int, save):
        with self._loopProcessEvent() as (text, skippable, f):
            if text is None:
                yield self._stop_event
            else:
                audio_file, audio_file_exists, cache_used = self._cache_file_try(cacheName, text)
                # use tts
                if not cache_used or not audio_file_exists:
                    try:
                        # tts
                        yield (text, skippable, f)
                        # update cache
                        if cache_used and text and f.exception() is None:
                            try: torchaudio.save(audio_file, save(f.result()), sample_rate)
                            except Exception as e: self.write(f"ERR: error saving cache file='{audio_file}' text='{text}' error={e}")
                    except Exception as e:
                        f.set_exception(e)
                        print_exc()
                        raise e
                # use cache
                else:
                    yield (None, None, None)
                    f.set_result(SdEvent.file(text, audio_file, skippable))


class TtsNone(TtsBase):
    def __init__(self, write: Writer):
        super().__init__("TtsNone", write)

    def _loop(self):
        self._loopLoadAndIgnoreEvents()


class TtsOs(TtsBase):
    def __init__(self, write: Writer):
        super().__init__('TtsOs', write)
        self.deviceName = 'cpu'
        self._engine = None
        
    def _loop(self):
        # init
        import uuid
        import rlvoice # https://github.com/Akul-AI/rlvoice-1
        # init dir
        cache_dir = self._cache_dir("os")
        # init engine
        self._engine = rlvoice.init(debug=True)
        # self._engine.setProperty('volume', 1.0)
        # self._engine.setProperty('rate', 100)
        voices = cast(list, self._engine.getProperty('voices'))
        for voice in voices:
            if 'Zira' in voice.name:
                self._engine.setProperty('voice', voices[1].id)

        # from rlvoice.driver import DriverProxy # https://github.com/Akul-AI/rlvoice-1
        # import weakref
        # import time
        # self._engine = DriverProxy(Eng(), None, True)

        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as (text, skippable, f):
                    if text is None: break
                    audio_file, audio_file_exists = cache_file(text, cache_dir)
                    cache_used = len(text) < 100
                    if not cache_used or not audio_file_exists:
                        try:
    
                            # classic way (cant be interrupted, but low latency)
                            # self._engine.say(text)
                            # self._engine.runAndWait()
                            # self._engine.stop()
    
                            # in-memory (can be interrupted, but high latency and only windows), !work after 1st event for some reason
                            # audios = []
                            # self._engine.to_memory(text, audios)
                            # self._engine.runAndWait()
                            # audio = audios[0]
                            # audio = np.array(audio, dtype=np.int32) # to numpty
                            # audio = audio.astype(np.float32) / 32768.0 # normalize
                            # self.play.playWavChunk(text, audio, skippable)
                            # save file
    
                            # in-memory (can be interrupted, but high latency and only windows), experimental
                            # f = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", "os", str(uuid.uuid4()) + '.wav')
                            # self._engine._driver.save_to_file(text, f)
                            # self._engine._driver.startLoop()
                            # for i in self._engine._driver.iterate(): pass
                            # self._engine._driver.endLoop()
                            # self._engine._driver.stop()
                            # self.play.playFile(text, f, skippable)
                            # save file
    
                            # custom implementation (in-memory, windows only)
                            # from ctypes import c_int16
                            # import win32com.client
                            # import math
                            # class TextToSpeech:
                            #     def __init__(self):
                            #         self.speech = win32com.client.Dispatch("SAPI.SpVoice")
                            #
                            #     def say(self, text: str) -> bytes:
                            #         self.speech.Rate = int(math.log(400 / 156.63, 1.11))
                            #         self.stream = win32com.client.Dispatch("SAPI.SpMemoryStream")
                            #         self.speech.AudioOutputStream = self.stream
                            #         self.speech.Speak(text)
                            #
                            #         data = self.stream.GetData()
                            #         data = [c_int16((data[i])|data[i+1]<<8).value for i in range(0,len(data),2)]
                            #         return data
                            #
                            # tts = TextToSpeech()
                            # with self._looping():
                            #     while not self._stop:
                            #         with self._loopProcessEvent() as (text, skippable, f):
                            #             audio = tts.say(text)
                            #             audio = np.array(audio, dtype=np.int32) # to numpty
                            #             audio = audio.astype(np.float32) / 32768.0 # normalize
                            #             self.play.playWavChunk(text, audio, skippable)

                            # file-based (interruptable, cachable, high latency, all platforms)
                            audio_file = audio_file if cache_used else os.path.join(cache_dir, str(uuid.uuid4()))
                            self._engine.save_to_file(text, audio_file)
                            self._engine.runAndWait()
                            self._engine.stop()
                            f.set_result(SdEvent.file(text, audio_file, skippable))
                        except Exception as e:
                            f.set_exception(e)
                    else:
                        f.set_result(SdEvent.file(text, audio_file, skippable))


# https://pypi.org/project/TTS/
# https://docs.coqui.ai/en/stable/models/xtts.html#inference-parameters # inference documentation
class TtsCoqui(TtsBase):
    def __init__(self, voice: str, device: str, write: Writer):
        super().__init__('TtsCoqui', write)
        self.speed = 1.0
        self.voice = voice
        self.device = device
        self.deviceName = device
        self._voice = voice
        self.model: Xtts | None = None
        self.http_handler: HttpHandler | None = None

    def _gen(self, text: str):
        text_to_gen = replace_numbers_with_words(text)
        text_to_gen = text_to_gen.strip()
        text_to_gen = text_to_gen.replace("</s>", "").replace("```", "").replace("...", " ")
        text_to_gen = re.sub(" +", " ", text_to_gen)

        return self.model.inference_stream(text_to_gen, "en", self.gpt_cond_latent, self.speaker_embedding, temperature=0.7, enable_text_splitting=False, speed=self.speed)

    def _loop(self):
        # init
        from TTS.tts.configs.xtts_config import XttsConfig
        from TTS.tts.models.xtts import Xtts
        import json
        # init voice
        voiceFile = os.path.join("voices-coqui", self.voice)
        if not os.path.exists(voiceFile):
            self.write("ERR: Voice " + self.voice + " does not exist")
            return

        def loadVoice():
            voiceFile = os.path.join("voices-coqui", self.voice)
            voiceFileJson = voiceFile+".json"
            # voice unavailable, ignore
            if not os.path.exists(voiceFile):
                self.write("ERR: Voice " + self.voice + " does not exist")
                return
            # latents exist, load
            if os.path.exists(voiceFileJson):
                with open(voiceFileJson, "r") as jf:
                    voice_props = json.load(jf)
                    self.speed = clip_float(voice_props.get("speed", 1.0), 0.5, 1.5)
                    self.speaker_embedding = (torch.tensor(voice_props["speaker_embedding"]).unsqueeze(0).unsqueeze(-1))
                    self.gpt_cond_latent = (torch.tensor(voice_props["gpt_cond_latent"]).reshape((-1, 1024)).unsqueeze(0))
            # latents !exist, generate & load
            else:
                self.gpt_cond_latent, self.speaker_embedding = self.model.get_conditioning_latents(audio_path=[voiceFile])
                self._voice = self.voice
                voice_props = {
                    "gpt_cond_latent": self.gpt_cond_latent.cpu().squeeze().half().tolist(),
                    "speaker_embedding": self.speaker_embedding.cpu().squeeze().half().tolist(),
                }
                with open(voiceFileJson, "w") as jf:
                    json.dump(voice_props, jf)

        def loadVoiceIfNew():
            if self._voice != self.voice:
                loadVoice()

        def loadModel():
            try:
                import warnings
                warnings.filterwarnings("ignore", category=UserWarning, module='TTS.tts.layers.xtts.stream_generator', lineno=138)
                # originally at C:/Users/USER/AppData/Local/tts/tts_models--multilingual--multi-dataset--xtts_v2
                dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'models-coqui')
                config = XttsConfig()
                config.load_json(os.path.join(dir, 'config.json'))
                self.model = Xtts.init_from_config(config)
                self.model.load_checkpoint(config, checkpoint_dir=dir, use_deepspeed=False)
                self.model.cuda(self.device)
                loadVoice()
                self._loaded = True
            except Exception as x:
                self.model = None
                self._stop = True
                self.write(f"ERR: Failed to load TTS model {x}")

        # load model asynchronously (so we do not block speaking from cache)
        loadModelThread = Thread(name='TtsCoqui-load-model', target=loadModel, daemon=True)
        loadModelThread.start()

        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEventFut(os.path.join('coqui', self._voice), 24000, lambda it: self._loop_result) as (text, skippable, f):
                    if text is not None:
                        # init
                        loadModelThread.join()
                        loadVoiceIfNew()
                        if self.model is None: f.set_exception(Exception(f'{self.name} model failed to load'))
                        if self.model is None: continue
                        # generate
                        audio_chunks = self._gen(text)
                        consumer, audio_play, audio_save = teeThreadSafeEager(audio_chunks, 2)
                        # result
                        f.set_result(SdEvent.wavChunks(text, map(lambda x: x.cpu().numpy(), audio_play), skippable))
                        consumer()
                        self._loop_result = torch.cat(list(audio_save), dim=0).squeeze().unsqueeze(0).cpu()


class TtsHttp(TtsBase):
    def __init__(self, url: str, port: int, write: Writer):
        super().__init__('TtsHttp', write)
        self.url = url
        self.port = port
        self.deviceName = 'http'

    def _loop(self):
        # init
        import http.client
        # plumbing
        class Conn:
            def __init__(self, url, port): self.conn = http.client.HTTPConnection(url, port)
            def __enter__(self): return self.conn
            def __exit__(self, exc_type, exc_val, exc_tb): self.conn.close()
        
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as (text, skippable, f):
                    if text is None: break
                    try:
                        text = text.encode('utf-8')
                        with Conn(self.url, self.port) as con:
                            con.set_debuglevel(0)
                            con.request('POST', '/speech', text, {})
                            res = con.getresponse()
        
                            if res.status != 200:
                                f.set_exception(Exception(f"Http status={res.status} {res.reason}"))
                                raise Exception(f"Http status={res.status} {res.reason}")

                            def audio_chunks_generator(remaining_bytes=b''):
                                for chunk in res:
                                    if self._skip: break
                                    data = remaining_bytes + chunk
                                    remaining_bytes_count = len(data) % 4
                                    if remaining_bytes_count != 0:  # if size isn't a multiple of 4 (size of float32)
                                        remaining_bytes = data[-remaining_bytes_count:]
                                        data = data[:-remaining_bytes_count]
                                    else:
                                        remaining_bytes = b''
                                    yield np.frombuffer(data, dtype=np.float32)
                                # process any remaining bytes
                                if remaining_bytes and not self._skip:
                                    yield np.frombuffer(remaining_bytes, dtype=np.float32)
                                    
                            audio_chunks = audio_chunks_generator()
                            consumer, audio_chunks = teeThreadSafeEager(audio_chunks, 1)
                            f.set_result(SdEvent.wavChunks(text, audio_chunks, skippable))
                            consumer()
                    except Exception as e:
                        f.set_exception(e)
                        raise e


# https://pytorch.org/hub/nvidia_deeplearningexamples_tacotron2/
class TtsTacotron2(TtsBase):
    def __init__(self, device: str, write: Writer):
        super().__init__('TtsTacotron2', write)
        self.deviceName = device
        self.device = device

    def _loop(self):
        device = torch.device(self.device)
        # load the Tacotron2 model pre-trained on LJ Speech dataset and prepare it for inference:
        tacotron2 = torch.hub.load('NVIDIA/DeepLearningExamples:torchhub', 'nvidia_tacotron2', model_math='fp16')
        tacotron2 = tacotron2.to(device)
        tacotron2.eval()
        # Load pretrained WaveGlow model
        waveglow = torch.hub.load('NVIDIA/DeepLearningExamples:torchhub', 'nvidia_waveglow', model_math='fp16')
        waveglow = waveglow.remove_weightnorm(waveglow)
        waveglow = waveglow.to(device)
        waveglow.eval()
        # load utils
        utils = torch.hub.load('NVIDIA/DeepLearningExamples:torchhub', 'nvidia_tts_utils')
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEventFut("tacotron2", 24000, lambda audio: torch.tensor(audio.audio[0]).unsqueeze(0)) as (text, skippable, f):
                    if text is not None:
                        # preprocess
                        with self.write.suppressed(): sequences, lengths = utils.prepare_input_sequence([text])
                        sequences, lengths = (sequences.to(device), lengths.to(device))
                        # gen
                        with torch.no_grad():
                            mel, _, _ = tacotron2.infer(sequences, lengths)
                            mel, _, _ = tacotron2.infer(sequences.to(device), lengths.to(device))
                            audio = waveglow.infer(mel)
                        audio_numpy = audio[0].data.cpu().numpy()
                        # result
                        f.set_result(SdEvent.wavChunks(text, [audio_numpy], skippable))


# https://speechbrain.github.io
class TtsSpeechBrain(TtsBase):
    def __init__(self, device: str, write: Writer):
        super().__init__('TtsSpeechBrain', write)
        self.deviceName = device
        self.device = device

    def _loop(self):
        # init
        from speechbrain.inference.TTS import Tacotron2
        from speechbrain.inference.vocoders import HIFIGAN
        # init model
        device = torch.device(self.device)
        tacotron2 = Tacotron2.from_hparams(source="speechbrain/tts-tacotron2-ljspeech", savedir=os.path.join("cache", "speechbrain" , "tts-tacotron2-ljspeech"))
        hifi_gan = HIFIGAN.from_hparams(source="speechbrain/tts-hifigan-ljspeech", savedir=os.path.join("cache", "speechbrain" , "tts-hifigan-ljspeech"))
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEventFut("speechbrain", 24000, lambda audio: audio.audio[0].unsqueeze(0)) as (text, skippable, f):
                    if text is not None:
                        # preprocess
                        text_to_gen = replace_numbers_with_words(text) + f"{'.' if text.endswith('.') else ''}"
                        # gen
                        mel_output, mel_length, alignment = tacotron2.encode_text(text_to_gen)
                        waveforms = hifi_gan.decode_batch(mel_output)
                        audio_numpy = waveforms.detach().cpu().squeeze()
                        # result
                        f.set_result(SdEvent.wavChunks(text, [audio_numpy], skippable))


# https://huggingface.co/nvidia/tts_en_fastpitch
class TtsFastPitch(TtsBase):
    def __init__(self, device: str, write: Writer):
        super().__init__('TtsFastPitch', write)
        self.deviceName = device
        self.device = device

    def _download_file(self, url, target_path):
        import wget
        if not os.path.exists(target_path):
            self.write(f'RAW: {self.name} downloading {target_path}')
            wget.download(url, target_path)
        return target_path

    def _loop(self):
        # Download files
        cmudict = self._download_file('https://raw.githubusercontent.com/NVIDIA/NeMo/main/scripts/tts_dataset_files/cmudict-0.7b_nv22.10', os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", 'fastpitch-files', 'cmudict-0.7b'))
        heteronyms = self._download_file('https://raw.githubusercontent.com/NVIDIA/NeMo/main/scripts/tts_dataset_files/heteronyms-052722', os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", 'fastpitch-files', 'heteronyms'))
        # Load models
        fastpitch, generator_train_setup = torch.hub.load('NVIDIA/DeepLearningExamples:torchhub', 'nvidia_fastpitch')
        hifigan, vocoder_train_setup, denoiser = torch.hub.load('NVIDIA/DeepLearningExamples:torchhub', 'nvidia_hifigan')
        tp = torch.hub.load('NVIDIA/DeepLearningExamples:torchhub', 'nvidia_textprocessing_utils', cmudict_path=cmudict, heteronyms_path=heteronyms)

        # Verify that generator and vocoder models agree on input parameters.
        CHECKPOINT_SPECIFIC_ARGS = [
            'sampling_rate', 'hop_length', 'win_length', 'p_arpabet', 'text_cleaners',
            'symbol_set', 'max_wav_value', 'prepend_space_to_text',
            'append_space_to_text'
        ]
        for k in CHECKPOINT_SPECIFIC_ARGS:
            v1 = generator_train_setup.get(k, None)
            v2 = vocoder_train_setup.get(k, None)
            assert v1 is None or v2 is None or v1 == v2, f'{k} mismatch in spectrogram generator and vocoder'

        # Put all models on available device.
        device = torch.device(self.device)
        fastpitch.to(device)
        hifigan.to(device)
        denoiser.to(device)

        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEventFut("fastpitch", vocoder_train_setup['sampling_rate'], lambda it: self._loop_result) as (text, skippable, f):
                    if text is not None:
                        # preprocess
                        with self.write.suppressed(): batches = tp.prepare_input_sequence([text], batch_size=1)
                        # generate
                        gen_kw = {'pace': 1.0, 'speaker': 0, 'pitch_tgt': None, 'pitch_transform': None}
                        denoising_strength = 0.005
                        with torch.no_grad():
                            mel, mel_lens, *_ = fastpitch(batches[0]['text'].to(device), **gen_kw)
                            audio = hifigan(mel).float()
                            audio = denoiser(audio.squeeze(1), denoising_strength)
                            audio = audio.cpu().squeeze(1)
                            # result
                            self._loop_result = audio.clone().detach()
                            f.set_result(SdEvent.wavChunks(text, audio[0].data.cpu().numpy(), skippable))