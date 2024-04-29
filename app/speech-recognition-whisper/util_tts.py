import asyncio
import numpy
import os
import time
import torch
import torchaudio
import traceback
from collections.abc import Iterator
from concurrent.futures import Future
from queue import Queue
from threading import Thread
from typing import cast

from util_actor import Actor, ActorStoppedException
from util_dir_cache import cache_file
from util_http import HttpHandler
from util_itr import teeThreadSafeEager, words
from util_play_engine import SdActor
from util_str import *
from util_wrt import Writer


class Tts(Actor):
    def __init__(self, speakOn: bool, tts, write: Writer):
        super().__init__("tts-preprocessor", 'tts-preprocessor', None, write, True)
        self.tts = tts
        self.speakOn = speakOn
        self._skip = False
        self.history = []

    def start(self):
        super().start()
        self.tts.start()

    def stop(self):
        super().stop()
        self.tts.stop()

    def skip(self):
        self.tts.skip()

    def skipWithoutSound(self):
        self.tts.skipWithoutSound()

    def skippable(self, event: str) -> Future[str | None]:
        self.write('SYS: ' + event)
        self.queue.put((words(event), True, False, Future()))

    def repeatLast(self):
        if self.history:
            text, skippable = self.history[-1]
            self.write('SYS: ' + event)
            self.queue.put((words(text), skippable, True, Future()))

    def __call__(self, event: str | Iterator) -> Future[str | None]:
        f = Future()
        if not self.speakOn:
            f.set_result(None)
        if isinstance(event, str):
            self.write('SYS: ' + event)
            self.queue.put((words(event), False, False, f))
        elif isinstance(event, Iterator):
            self.queue.put((event, True, False, f))
        return f

    def _loop(self):
        from stream2sentence import generate_sentences
            
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as (event, skippable, repeated, f):
                    text = ''
                    try:
                        self.tts.speak(None, skippable=False)
                        for sentence in generate_sentences(event, cleanup_text_links=True, cleanup_text_emojis=True):
                            text = text + sentence
                            if (len(sentence)>0): self.tts.speak(sentence, skippable=skippable)
                        self.tts.speak(None, skippable=False)
                        if not repeated: self.history.append((text, skippable))
                        f.set_result(text)
                    except Exception as e:
                        f.set_exception(e)
                        raise e


class TtsBase(Actor):
    def __init__(self, name: str, write: Writer):
        super().__init__("tts", name, None, write, True)
        self.max_text_length = 400
        self._skip = False

    def skip(self):
        """
        Stop processing this element and skip any next element until first non-skippable
        """
        self._skip = True

    def skipWithoutSound(self):
        self._skip = True

    def speak(self, text: str, skippable: bool):
        """
        Adds the text to queue
        """
        self.queue.put((text, skippable))

    def _get_event_text(self, e: (str, bool)) -> str | None:
        if e is None: return e
        else: return e[0]

    def _get_next_event(self) -> (str, bool):
        e = self._get_next_event_impl()
        if e is None: raise ActorStoppedException()
        return e

    def _get_next_event_impl(self) -> (str, bool):
        """
        :return: next element from queue, skipping over skippable elements if _skip=True, blocks until then
        """
        while not self._stop:
            textRaw, skippable = self.queue.get()

            # skip skippable
            if self._skip and skippable:
                continue

            self._skip = False

            # skip boundary value
            if textRaw is None:
                self._boundary()
                continue

            # skip empty value
            if len(textRaw.strip()) == 0:
                continue

            # gather all elements that are already ready (may improve tts quality)
            if skippable:
                ts = textRaw
                while True:
                    if self.queue.not_empty: break
                    t, s = self.queue[0]
                    if t is None or not s: break
                    if len(ts) + len(t) > self.max_text_length: break
                    ts = ts + t
                    self.queue.get_nowait()

            return (textRaw, skippable)

    def _boundary(self):
        pass


class TtsNone(TtsBase):
    def __init__(self, write: Writer):
        super().__init__("TtsNone", write)

    def _loop(self):
        self._loopLoadAndIgnoreEvents()

    def speak(self, text: str, skippable: bool): # pylint: disable=unused-argument
        pass


class TtsWithModelBase(TtsBase):
    def __init__(self, name: str, play: SdActor, write: Writer):
        super().__init__(name, write)
        self.play = play

    def _boundary(self):
        self.play.boundary()

    def skip(self):
        super().skip()
        self.play.skip()

    def start(self):
        super().start()
        self.play.start()

    def stop(self):
        super().stop()
        self.play.stop()

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


class TtsOs(TtsWithModelBase):
    def __init__(self, play: SdActor, write: Writer):
        super().__init__('TtsOs', play, write)
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

        # alternative for playing around
        # class Eng:
        #     def notify(self, topic, **kwargs): pass
        #     def endLoop(self): pass

        # from rlvoice.driver import DriverProxy # https://github.com/Akul-AI/rlvoice-1
        # import weakref
        # import time
        # self._engine = DriverProxy(Eng(), None, True)

        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as (text, skippable):
                    audio_file, audio_file_exists = cache_file(text, cache_dir)
                    cache_used = len(text) < 100
                    if not cache_used or not audio_file_exists:

                        # classic way (cant be interrupted, but low latency)
                        # self._engine.say(text)
                        # self._engine.runAndWait()
                        # self._engine.stop()

                        # in-memory (can be interrupted, but high latency and only windows), !work after 1st event for some reason
                        # audios = []
                        # self._engine.to_memory(text, audios)
                        # self._engine.runAndWait()
                        # audio = audios[0]
                        # audio = numpy.array(audio, dtype=numpy.int32) # to numpty
                        # audio = audio.astype(numpy.float32) / 32768.0 # normalize
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
                        #         with self._loopProcessEvent() as (text, skippable):
                        #             audio = tts.say(text)
                        #             audio = numpy.array(audio, dtype=numpy.int32) # to numpty
                        #             audio = audio.astype(numpy.float32) / 32768.0 # normalize
                        #             self.play.playWavChunk(text, audio, skippable)

                        # file-based (interruptable, cachable, high latency, all platforms)
                        audio_file = audio_file if cache_used else os.path.join(cache_dir, str(uuid.uuid4()))
                        self._engine.save_to_file(text, audio_file)
                        self._engine.runAndWait()
                        self._engine.stop()
                        self.play.playFile(text, audio_file, skippable)
                    else:
                        self.play.playFile(text, audio_file, skippable)


# https://github.com/Xtr4F/PyCharacterAI
class TtsCharAi(TtsWithModelBase):
    def __init__(self, token: str, voice: int, play: SdActor, write: Writer):
        super().__init__('TtsCharAi', play, write)
        self.token = token
        self.voice = voice

    def _loop(self):
        asyncio.run(self._loopasync())

    async def _loopasync(self):
        # init
        if len(self.token)==0: raise Exception("Auth token missing")
        from PyCharacterAI import Client
        # init dir
        cache_dir = self._cache_dir("charai", str(self.voice))
        # init client
        client = None
        # loop
        with self.looping():
            while not self._stop:
                with self._loopProcessEvent() as (text, skippable):
                    audio_file, audio_file_exists = cache_file(text, cache_dir)
                    cache_used = len(text) < 100

                    # generate audio
                    if not cache_used or not audio_file_exists:
                        # login once lazily
                        if client is None:
                            client = Client()
                            await client.authenticate_with_token(self.token)

                        # generate
                        audio_data = await client.generate_voice(self.voice, text[:4094])

                        # play
                        self.play.playWavChunk(text, audio_data, skippable)

                        # update cache
                        if cache_used:
                            sf.write(audio_file, audio_data, 24000)
                    else:
                        self.play.playFile(text, audio_file, skippable)


# https://pypi.org/project/TTS/
class TtsCoqui(TtsWithModelBase):
    def __init__(self, voice: str, device: str, play: SdActor, write: Writer):
        super().__init__('TtsCoqui', play, write)
        self.speed = 1.0
        self.voice = voice
        self.device = device
        self.deviceName = device
        self._voice = voice
        self.model: Xtts | None = None
        self.http_handler: HttpHandler | None = None

    def _httpHandler(self) -> HttpHandler:
        import soundfile as sf
        import re
        from http.server import BaseHTTPRequestHandler
        tts = self

        def waitTillLoaded():
            while self._loaded is False:
                if self._stop: return
                time.sleep(0.1)

        def gen(text):
            text_to_gen = replace_numbers_with_words(text)
            text_to_gen = text_to_gen.strip()
            text_to_gen = text_to_gen.replace("</s>", "").replace("```", "").replace("...", " ")
            text_to_gen = re.sub(" +", " ", text_to_gen)
            return self.model.inference_stream(text_to_gen, "en", self.gpt_cond_latent, self.speaker_embedding, temperature=0.7, enable_text_splitting=False, speed=self.speed)

        class MyRequestHandler(HttpHandler):
            def __init__(self, ):
                super().__init__('POST', '/speech')

            def __call__(self, req: BaseHTTPRequestHandler):
                if tts._stop: return
                try:
                    content_length = int(req.headers['Content-Length'])
                    body = req.rfile.read(content_length)
                    text = body.decode('utf-8')
                    audio_file, audio_file_exists, cache_used = tts._cache_file_try(os.path.join('coqui', tts._voice), text)

                    # generate
                    if not cache_used or not audio_file_exists:
                        waitTillLoaded()

                        req.send_response(200)
                        req.send_header('Content-type', 'application/octet-stream')
                        req.end_headers()

                        # generate
                        audio_chunks = []
                        for audio_chunk in gen(text):
                            audio_chunks.append(audio_chunk)

                            if req.wfile.closed: return
                            if tts._stop: req.wfile.close()
                            if tts._stop: return

                            req.wfile.write(audio_chunk.cpu().numpy().tobytes())
                            req.wfile.flush()

                        # update cache
                        if cache_used and text:
                            wav = torch.cat(audio_chunks, dim=0)
                            try: torchaudio.save(audio_file, wav.squeeze().unsqueeze(0).cpu(), 24000)
                            except Exception as e: tts.write(f"ERR: error saving cache file='{audio_file}' text='{text}' error={e}")

                    # play file
                    else:

                        req.send_response(200)
                        req.send_header('Content-type', 'application/octet-stream')
                        req.end_headers()

                        audio_data, fs = sf.read(audio_file, dtype='float32')
                        if fs!=24000: return
                        chunk_size = 1024
                        audio_length = len(audio_data)
                        start_pos = 0
                        while start_pos < audio_length:

                            if req.wfile.closed: return
                            if tts._stop: req.wfile.close()
                            if tts._stop: return

                            end_pos = min(start_pos + chunk_size, audio_length)
                            chunk = audio_data[start_pos:end_pos]
                            start_pos = end_pos
                            req.wfile.write(chunk)
                            req.wfile.flush()

                except Exception as e:
                    tts.write("ERR: error generating voice for http " + str(e))
                    traceback.print_exc()

        return MyRequestHandler()

    def _loop(self):
        # init
        from TTS.tts.configs.xtts_config import XttsConfig
        from TTS.tts.models.xtts import Xtts
        import json
        # init voice
        voiceFile = os.path.join("voices-coqui", self.voice)
        if not os.path.exists(voiceFile):
            self.write("ERR: Voice " + self.voice + "does not exist")
            return

        def loadVoice():
            voiceFile = os.path.join("voices-coqui", self.voice)
            voiceFileJson = voiceFile+".json"
            # voice unavailable, ignore
            if not os.path.exists(voiceFile):
                self.write("ERR: Voice " + self.voice + "does not exist")
                return
            # latents exist, load
            if os.path.exists(voiceFileJson):
                with open(voiceFileJson, "r") as jf:
                    latents = json.load(jf)
                    self.speaker_embedding = (torch.tensor(latents["speaker_embedding"]).unsqueeze(0).unsqueeze(-1))
                    self.gpt_cond_latent = (torch.tensor(latents["gpt_cond_latent"]).reshape((-1, 1024)).unsqueeze(0))
            # latents !exist, generate & load
            else:
                self.gpt_cond_latent, self.speaker_embedding = self.model.get_conditioning_latents(audio_path=[voiceFile])
                self._voice = self.voice
                latents = {
                    "gpt_cond_latent": self.gpt_cond_latent.cpu().squeeze().half().tolist(),
                    "speaker_embedding": self.speaker_embedding.cpu().squeeze().half().tolist(),
                }
                with open(voiceFileJson, "w") as jf:
                    json.dump(latents, jf)



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
                self.write(f"ERR: Failed to load TTS model {x}")

        # load model asynchronously (so we do not block speaking from cache)
        loadModelThread = Thread(name='TtsCoqui-load-model', target=loadModel, daemon=True)
        loadModelThread.start()

        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as (text, skippable):
                    audio_file, audio_file_exists, cache_used = self._cache_file_try(os.path.join('coqui', self._voice), text)

                    # generate
                    if not cache_used or not audio_file_exists:

                        # wait for init
                        loadModelThread.join()
                        loadVoiceIfNew()

                        # generate
                        audio_chunks = self.model.inference_stream(text, "en", self.gpt_cond_latent, self.speaker_embedding, temperature=0.7, enable_text_splitting=False, speed=self.speed)
                        consumer, audio_chunks_play, audio_chunks_cache = teeThreadSafeEager(audio_chunks, 2)

                        # play
                        self.play.playWavChunk(text, map(lambda x: x.cpu().numpy(), audio_chunks_play), skippable)
                        consumer()

                        # update cache
                        if cache_used and text:
                            audio_chunks = []
                            for audio_chunk in audio_chunks_cache:
                                audio_chunks.append(audio_chunk)

                            wav = torch.cat(audio_chunks, dim=0)
                            try: torchaudio.save(audio_file, wav.squeeze().unsqueeze(0).cpu(), 24000)
                            except Exception as e: self.write(f"ERR: error saving cache file='{audio_file}' text='{text}' error={e}")
                    else:
                        self.play.playFile(text, audio_file, skippable)


class TtsHttp(TtsWithModelBase):
    def __init__(self, url: str, port: int, play: SdActor, write: Writer):
        super().__init__('TtsHttp', play, write)
        self.url = url
        self.port = port

    def _loop(self):
        # init
        import io, http.client
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as (text, skippable):

                    text = text.encode('utf-8')
                    conn = http.client.HTTPConnection(self.url, self.port)
                    conn.set_debuglevel(0)
                    conn.request('POST', '/speech', text, {})
                    response = conn.getresponse()

                    def read_wav_chunks_from_response(response):
                        chunk_size = 1024*(numpy.zeros(1, dtype=numpy.float32).nbytes)  # Adjust the chunk size as needed
                        buffer = io.BytesIO()
                        for chunk in response:
                            if self._skip: conn.close()
                            buffer.write(chunk)
                            while buffer.tell() >= chunk_size:
                                if self._skip: conn.close()
                                buffer.seek(0)
                                wav_data = buffer.read(chunk_size)
                                remaining_data = buffer.read()
                                buffer.seek(0)
                                buffer.write(remaining_data)
                                yield numpy.frombuffer(wav_data, dtype=numpy.float32)
                            buffer.truncate(buffer.tell())
                        if buffer.tell() > 0:
                            buffer.seek(0)
                            wav_data = buffer.read()
                            yield numpy.frombuffer(wav_data, dtype=numpy.float32)

                    audio_chunks = read_wav_chunks_from_response(response)
                    consumer, audio_chunks = teeThreadSafeEager(audio_chunks, 1)
                    self.play.playWavChunk(text, audio_chunks, skippable)
                    consumer()
                    conn.close()


# https://pytorch.org/hub/nvidia_deeplearningexamples_tacotron2/
class TtsTacotron2(TtsWithModelBase):
    def __init__(self, device: str, play: SdActor, write: Writer):
        super().__init__('TtsTacotron2', play, write)
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
                with self._loopProcessEvent() as (text, skippable):
                    audio_file, audio_file_exists, cache_used = self._cache_file_try("tacotron2", text)

                    # generate
                    if not cache_used or not audio_file_exists:

                        # Format the input using utility methods
                        sequences, lengths = utils.prepare_input_sequence([text])
                        sequences, lengths = (sequences.to(device), lengths.to(device))

                        # Run the chained models
                        with torch.no_grad():
                            mel, _, _ = tacotron2.infer(sequences, lengths)
                            audio = waveglow.infer(mel)
                        audio_numpy = audio[0].data.cpu().numpy()

                        # play
                        self.play.playWavChunk(text, [audio_numpy], skippable)

                        # update cache
                        if cache_used and text:
                            try: torchaudio.save(audio_file, torch.tensor(audio_numpy).unsqueeze(0), 24000)
                            except Exception as e: self.write(f"ERR: error saving cache file='{audio_file}' text='{text}' error={e}")
                    else:
                        self.play.playFile(text, audio_file, skippable)


# https://speechbrain.github.io
class TtsSpeechBrain(TtsWithModelBase):
    def __init__(self, device: str, play: SdActor, write: Writer):
        super().__init__('TtsSpeechBrain', play, write)
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
                with self._loopProcessEvent() as (text, skippable):
                    audio_file, audio_file_exists, cache_used = self._cache_file_try("speechbrain", text)

                    # generate
                    if not cache_used or not audio_file_exists:
                        text_to_gen = replace_numbers_with_words(text) + f"{'.' if text.endswith('.') else ''}"
                        mel_output, mel_length, alignment = tacotron2.encode_text(text_to_gen)
                        waveforms = hifi_gan.decode_batch(mel_output)
                        audio_numpy = waveforms.detach().cpu().squeeze()

                        # play
                        self.play.playWavChunk(text, [audio_numpy], skippable)

                        # update cache
                        if cache_used and text:
                            try: torchaudio.save(audio_file, audio_numpy.unsqueeze(0), 24000)
                            except Exception as e: self.write(f"ERR: error saving cache file='{audio_file}' text='{text}' error={e}")
                    else:
                        self.play.playFile(text, audio_file, skippable)


# https://huggingface.co/nvidia/tts_en_fastpitch
class TtsFastPitch(TtsWithModelBase):
    def __init__(self, device: str, play: SdActor, write: Writer):
        super().__init__('TtsFastPitch', play, write)
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
                with self._loopProcessEvent() as (text, skippable):
                    audio_file, audio_file_exists, cache_used = self._cache_file_try("fastpitch", text)

                    # generate
                    if not cache_used or not audio_file_exists:

                        batches = tp.prepare_input_sequence([text], batch_size=1)
                        gen_kw = {'pace': 1.0, 'speaker': 0, 'pitch_tgt': None, 'pitch_transform': None}
                        denoising_strength = 0.005
                        batch = batches[0]
                        with torch.no_grad():
                            mel, mel_lens, *_ = fastpitch(batch['text'].to(device), **gen_kw)
                            audio = hifigan(mel).float()
                            audio = denoiser(audio.squeeze(1), denoising_strength)
                            audio = audio.cpu().squeeze(1)

                        # play
                        self.play.playWavChunk(text, audio, skippable)

                        # update cache
                        if cache_used and text:
                            try: torchaudio.save(audio_file, audio.clone().detach(), vocoder_train_setup['sampling_rate'])
                            except Exception as e: self.write(f"ERR: error saving cache file='{audio_file}' text='{text}' error={e}")
                    else:
                        self.play.playFile(text, audio_file, skippable)