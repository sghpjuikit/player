import os
import time
import asyncio
import traceback
from threading import Thread
from util_itr import teeThreadSafe, teeThreadSafeEager
from util_http import HttpHandler
from util_dir_cache import cache_file
from util_wrt import Writer
from util_play_engine import SdActor
from collections.abc import Iterator
from typing import cast
from queue import Queue
from TTS.tts.configs.xtts_config import XttsConfig
from TTS.tts.models.xtts import Xtts


class Tty:
    def __init__(self, speakOn: bool, tty, write: Writer):
        self.sentence_min_length = 40 # shorter == faster feedback, longer == less audio hallucinations in short audio
        self.tty = tty
        self.speakOn = speakOn
        self.write = write
        self._stop = False
        self._skip = False
        self.ignored_chars = set(".,?!_-:#\n\r\t\\`'\"")
        self.space_chars = set("_-\n\r ")
        self.queue = Queue()
        self.history = []

    def start(self):
        Thread(name='Tty', target=self._loop, daemon=True).start()
        self.tty.start()

    def stop(self):
        self._stop = True
        self.tty.stop()

    def skip(self):
        self._skip = True
        self.tty.skip()

    def skippable(self, event: str):
        self.write('SYS: ' + event)
        self(iter(map(lambda x: x + ' ', event.split(' '))))

    def repeatLast(self):
        if self.history:
            text, skippable = self.history[-1]
            self.queue.put((text, skippable, True))

    def __call__(self, event: str | Iterator):
        if not self.speakOn:
            return

        if isinstance(event, str):
            self.write('SYS: ' + event)
            self.queue.put((iter(map(lambda x: x + ' ', event.split(' '))), False, False))

        elif isinstance(event, Iterator):
            self.queue.put((event, True, False))

    def _loop(self):
        while not self._stop and self.speakOn:
            event, skippable, repeated = self.queue.get()
            self._skip = False
            sentence = ''
            text = ''

            self.tty.speak(None, skippable=False)
            for e in event:

                # skip skippable
                if self._skip and skippable: break

                text = text + e
                sentence = sentence + e
                while len(sentence) >= self.sentence_min_length:
                    index = -1
                    c = ''
                    if index==-1:
                        index = sentence.rfind('. ')
                        c = '. '
                    if index==-1:
                        index = sentence.rfind('\n')
                        c = '\n'
                    if index==-1:
                        break
                    else:
                        s = sentence[:index]
                        self.process(s+c, skippable, end=False)
                        sentence = sentence[index+1:]

            self.history.append((text, skippable))
            if not self._skip or not skippable:
                self.process(sentence, skippable, end=True)
                sentence = ''

    def process(self, s: str, skippable: bool, end: bool):
        ss = ''.join(c if c not in self.space_chars else ' ' for c in s)
        ss = ''.join(c for c in ss if c not in self.ignored_chars)
        ss = ss.strip()

        if (len(ss)>0): self.tty.speak(s, skippable=skippable)
        if end: self.tty.speak(None, skippable=False)


class TtyBase:
    def __init__(self):
        self.max_text_length = 400
        self._skip = False
        self._stop = False
        self.queue = Queue()

    def start(self):
        pass

    def stop(self):
        """
        Stop processing all elements and release all resources
        """
        _stop = True

    def skip(self):
        """
        Stop processing this element and skip any next element until first non-skippable
        """
        _skip = True

    def speak(self, text: str, skippable: bool):
        """
        Adds the text to queue
        """
        self.queue.put((text, skippable))

    def get_next_element(self) -> (str, bool):
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


class TtyNone(TtyBase):
    def __init__(self):
        super().__init__()

    def start(self):
        pass

    def stop(self):
        pass

    def speak(self, text: str, skippable: bool): # pylint: disable=unused-argument
        pass


class TtyOsMac(TtyBase):
    def __init__(self):
        super().__init__()
        self.allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,?!-_$:+-/ ")

    def start(self):
        Thread(name='TtyOsMac', target=self._loop, daemon=True).start()

    def _loop(self):
        while not self._stop:
            textRaw, skippable = self.get_next_element()
            text = ''.join(c for c in textRaw if c in self.allowed_chars)
            os.system(f"say '{text}'")


class TtyOs(TtyBase):
    def __init__(self, write: Writer):
        super().__init__()
        self.write = write

    def start(self):
        Thread(name='TtyOs', target=self._loop, daemon=True).start()

    def _loop(self):
        # initialize pyttsx3
        try:
            import pyttsx3
        except ImportError as e:
            self.write("pyttsx3 python module failed to load")
            return

        while not self._stop:
            text, skippable = self.get_next_element()

            engine = pyttsx3.init()

            # set Zira voice
            voices = cast(list, engine.getProperty('voices'))
            for voice in voices:
                if 'Zira' in voice.name:
                    engine.setProperty('voice', voices[1].id)

            engine.say(text)
            engine.runAndWait()
            engine.stop()
            del engine


# https://github.com/Xtr4F/PyCharacterAI
class TtyCharAi(TtyBase):
    def __init__(self, token: str, voice: int, play: SdActor, write: Writer):
        super().__init__()
        self.token = token
        self.voice = voice
        self.play = play
        self.write = write

    def start(self):
        Thread(name='TtyCharAi', target=self.process_queue_start, daemon=True).start()
        self.play.start()

    def process_queue_start(self):
        asyncio.run(self.loop())

    async def loop(self):
        # initialize character.ai
        if len(self.token)==0:
            self.write("Character.ai speech engine token missing")
            return
        try:
            from PyCharacterAI import Client
        except ImportError as e:
            self.write("Character.ai python module failed to load")
            return

        # loop
        client = None
        while not self._stop:
            self.cache_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", "charai", str(self.voice).replace('.','-'))
            if not os.path.exists(self.cache_dir):
                os.makedirs(self.cache_dir)

            text, skippable = self.get_next_element()
            audio_file, audio_file_exists = cache_file(text, self.cache_dir)
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

    def _boundary(self):
        self.play.boundary()

    def skip(self):
        super().skip()
        self.play.skip()

    def stop(self):
        super().stop()
        self.play.stop()


class TtyWithModelBase(TtyBase):
    def __init__(self):
        super().__init__()

    def _boundary(self):
        self.play.boundary()

    def skip(self):
        super().skip()
        self.play.skip()

    def stop(self):
        super().stop()
        self.play.stop()

    def _cache_file_try(self, cache_name: str, text: str) -> (str, bool, bool):
        # compute cache dir
        self.cache_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", cache_name)
        # prepare cache dir
        if not os.path.exists(self.cache_dir): os.makedirs(self.cache_dir)
        # compute cache file
        audio_file, audio_file_exists = cache_file(text, self.cache_dir)
        cache_used = len(text) < 100
        return (audio_file, audio_file_exists, cache_used)


# https://pypi.org/project/TTS/
class TtyCoqui(TtyWithModelBase):
    def __init__(self, voice: str, cudeDevice: int | None, play: SdActor, write: Writer):
        super().__init__()
        self.speed = 1.0
        self.voice = voice
        self.cudeDevice: int | None = cudeDevice
        self._voice = voice
        self.play = play
        self.write = write
        self.model: Xtts | None = None
        self.loaded = False
        self.http_handler: HttpHandler | None = None

    def start(self):
        Thread(name='TtyCoqui', target=self._loop, daemon=True).start()
        self.play.start()

    def _httpHandler(self) -> HttpHandler:
        import torch, torchaudio
        import numpy
        import soundfile as sf
        from http.server import BaseHTTPRequestHandler
        tty = self

        def waitTillLoaded():
            while self.loaded is False:
                if self._stop: return
                time.sleep(0.1)

        def gen(text):
            return self.model.inference_stream(text, "en", self.gpt_cond_latent, self.speaker_embedding, temperature=0.7, enable_text_splitting=False, speed=self.speed)

        class MyRequestHandler(HttpHandler):
            def __init__(self, ):
                super().__init__('POST', '/speech')

            def __call__(self, req: BaseHTTPRequestHandler):
                if tty._stop: return
                try:
                    content_length = int(req.headers['Content-Length'])
                    body = req.rfile.read(content_length)
                    text = body.decode('utf-8')
                    audio_file, audio_file_exists, cache_used = tty._cache_file_try('coqui', text)

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
                            if tty._stop: req.wfile.close()
                            if tty._stop: return

                            req.wfile.write(audio_chunk.cpu().numpy().tobytes())
                            req.wfile.flush()

                        # update cache
                        if cache_used and text:
                            wav = torch.cat(audio_chunks, dim=0)
                            try: torchaudio.save(audio_file, wav.squeeze().unsqueeze(0).cpu(), 24000)
                            except Exception as e: tty.write(f"ERR: error saving cache file='{audio_file}' text='{text}' error={e}")

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
                            if tty._stop: req.wfile.close()
                            if tty._stop: return

                            end_pos = min(start_pos + chunk_size, audio_length)
                            chunk = audio_data[start_pos:end_pos]
                            start_pos = end_pos
                            req.wfile.write(chunk)
                            req.wfile.flush()

                except Exception as e:
                    tty.write("ERR: error generating voice for http " + str(e))
                    traceback.print_exc()

        return MyRequestHandler()

    def _loop(self):
        # initialize torch
        try:
            import torch, torchaudio, numpy
        except ImportError:
            self.write("ERR: Torch, torchaudio, numpy or TTS python module failed to load")
            return

        # initialize gpu
        try:
            assert torch.cuda.is_available(), "CUDA is not availabe on this machine."
        except Exception:
            self.write("ERR: Torch cuda not available. Make sure you have cuda compatible hardware and software")
            return

        voiceFile = os.path.join("voices-coqui", self.voice)
        if not os.path.exists(voiceFile):
            self.write("ERR: Voice " + self.voice + "does not exist")
            return

        def loadVoice():
            voiceFile = os.path.join("voices-coqui", self.voice)
            if not os.path.exists(voiceFile):
                self.write("ERR: Voice " + self.voice + "does not exist")
            else:
                self.gpt_cond_latent, self.speaker_embedding = self.model.get_conditioning_latents(audio_path=[voiceFile])
                self._voice = self.voice

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
                self.model.cuda(self.cudeDevice)
                loadVoice()
                self.loaded = True
            except Exception as x:
                self.write(f"ERR: Failed to load TTS model {x}")

        # load model asynchronously (so we do not block speaking from cache)
        loadModelThread = Thread(name='TtyCoqui-load-model', target=loadModel, daemon=True)
        loadModelThread.start()

        # loop
        while not self._stop:
            text, skippable = self.get_next_element()
            audio_file, audio_file_exists, cache_used = self._cache_file_try('coqui', text)

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


class TtyHttp(TtyBase):
    def __init__(self, url: str, port: int, play: SdActor, write: Writer):
        super().__init__()
        self.url = url
        self.port = port
        self.play = play
        self.write = write

    def start(self):
        Thread(name='TtyHttp', target=self._loop, daemon=True).start()
        self.play.start()

    def _loop(self):
        # initialize http
        try:
            import io, numpy, http.client
        except ImportError:
            self.write("ERR: http python module failed to load")
            return

        # loop
        while not self._stop:
            text, skippable = self.get_next_element()

            try:
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
            except Exception as e:
                self.write("ERR: Failed to generate audio " + str(e))
                traceback.print_exc()


# https://pytorch.org/hub/nvidia_deeplearningexamples_tacotron2/
class TtyTacotron2(TtyBase):
    def __init__(self, voice: str, device: None | str, play: SdActor, write: Writer):
        super().__init__()
        self.speed = 1.0
        self.voice = voice
        self.device: int | None = device
        self._voice = voice
        self.play = play
        self.write = write
        self.model: Xtts | None = None
        self.loaded = False

    def start(self):
        Thread(name='TtyTacotron2', target=self._loop, daemon=True).start()
        self.play.start()

    def _loop(self):

        # initialize
        try:
            import torch, torchaudio, numpy
        except ImportError:
            self.write("ERR: Torch, torchaudio, numpy python module failed to load")
            return

        device = None if self.device is None or len(self.device)==0 else torch.device(self.device)
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
        while not self._stop:
            text, skippable = self.get_next_element()
            audio_file, audio_file_exists, cache_used = self._cache_file_try("tacotron2", text)

            # generate
            if not cache_used or not audio_file_exists:

                # Format the input using utility methods
                sequences, lengths = utils.prepare_input_sequence([text])

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
