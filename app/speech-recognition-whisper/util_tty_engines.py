import os
from threading import Thread
import asyncio
import uuid
from util_itr import teeThreadSafe
from util_dir_cache import cache_file
from util_write_engine import Writer
from util_play_engine import SdActor, VlcActor
from collections.abc import Iterator
from typing import cast
from queue import Queue
from TTS.tts.configs.xtts_config import XttsConfig
from TTS.tts.models.xtts import Xtts


class Tty:
    def __init__(self, tty, write: Writer):
        self.sentence_min_length = 40 # shorter == faster feedback, longer == less audio hallucinations in short audio
        self.tty = tty
        self.write = write
        self.sentence = None
        self.sentence_mode = False
        self.sentence_queue = Queue()
        self.speeches_queue = Queue()

    def start(self):
        self.tty.start()

    def stop(self):
        self.tty.stop()

    def skip(self):
        self.sentence_mode = False
        while not self.sentence_queue.empty():
            try:
                self.sentence_queue.get(block=False)
            except Exception:
                break
        self.tty.skip()

    def __call__(self, event: str, use_cache=True):

        if isinstance(event, list):
            event = iter(event)

        if isinstance(event, str):
            self.write('SYS: ' + event)
            self.speeches_queue.put((event, use_cache))
            self.process()

        elif isinstance(event, Iterator):

            # iterableStart
            self.sentence = ''
            self.sentence_mode = True

            # iterablePart
            for e in event:
                if not self.sentence_mode:
                    continue

                self.sentence = self.sentence + e
                while len(self.sentence) >= self.sentence_min_length:
                    index = -1
                    if index==-1:
                        index = self.sentence.rfind('. ')
                    if index==-1:
                        index = self.sentence.rfind('\n')
                    if index==-1:
                        break
                    else:
                        sentence = self.sentence[:index]
                        self.sentence = self.sentence[index+1:]
                        if len(sentence)>0:
                            self.sentence_queue.put(sentence+'. ')
                            self.process()

            # iterableEnd
            if len(self.sentence)>0:
                self.sentence_queue.put(self.sentence)
                self.process()
            self.sentence = ''
            self.sentence_mode = False
            self.process()

    def speak(self, text: str, use_cache=True):
        self.write('SYS: ' + text)
        self.speeches_queue.put((text, use_cache))
        self.process()

    def process(self):
        if self.sentence_mode:
            while not self.sentence_queue.empty():
                try:
                    text = self.sentence_queue.get(block=False)
                    if (len(text)>0):
                        self.tty.speak(text, skippable=True, use_cache=False)
                except Exception:
                    break
        else:
            while not self.speeches_queue.empty():
                try:
                    text, use_cache = self.speeches_queue.get(block=False)
                    if (len(text)>0):
                        self.tty.speak(text, skippable=False, use_cache=use_cache)
                except Exception:
                    break


class TtyBase:
    def __init__(self):
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

    def speak(self, text: str, skippable: bool, use_cache: bool):
        """
        Adds the text to queue
        """
        self.queue.put((text, skippable, use_cache))

    def get_next_element(self):
        """
        :return: next element from queue, skipping over skippable elements if _skip=True, blocks until then
        """
        textRaw, skippable, use_cache = self.queue.get()

        # skip skippable
        while self._skip and skippable:
            continue

        # stop skipping
        self._skip = False

        return (textRaw, skippable, use_cache)


class TtyNone(TtyBase):
    def __init__(self):
        super().__init__()

    def start(self):
        pass

    def stop(self):
        pass

    def speak(self, text: str, skippable: bool, use_cache: bool): # pylint: disable=unused-argument
        pass


class TtyOsMac(TtyBase):
    def __init__(self):
        super().__init__()
        self.allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,?!-_$:+-/ ")

    def start(self):
        Thread(name='TtyOsMac', target=self._loop, daemon=True).start()

    def _loop(self):
        while not self._stop:
            textRaw, skippable, use_cache = self.get_next_element()
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
            text, skippable, use_cache = self.get_next_element()

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
    def __init__(self, token: str, voice: int, play: VlcActor, write: Writer):
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
            text, skippable, cache_requested = self.get_next_element()
            audio_cache_file, audio_cache_file_exists = cache_file(text, self.cache_dir)
            cache_used = cache_requested and len(text) < 100
            audio_file = audio_cache_file if cache_used else os.path.join(self.cache_dir, "user_" + str(uuid.uuid4()) + ".wav")

            # generate audio
            if not cache_used or not audio_cache_file_exists:
                # login once lazily
                if client is None:
                    client = Client()
                    await client.authenticate_with_token(self.token)
                # generate
                audio = await client.generate_voice(self.voice, text[:4094])
                # save
                with open(audio_file, 'wb') as f:
                    f.write(audio.read())

            self.play.queue.put(('f', audio_file, skippable))

    def skip(self):
        super().skip()
        self.play.skip()

    def stop(self):
        super().stop()
        self.play.stop()


# https://pypi.org/project/TTS/
class TtyCoqui(TtyBase):
    def __init__(self, voice: str, play: SdActor, write: Writer):
        super().__init__()
        self.speed = 1.0
        self.voice = voice
        self._voice = voice
        self.play = play
        self.write = write

    def start(self):
        Thread(name='TtyCoqui', target=self._loop, daemon=True).start()
        self.play.start()

    def _loop(self):
        # initialize torch
        try:
            import torch
            import torchaudio
            import numpy
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
                self.model.cuda()
                loadVoice()
            except Exception:
                self.write("ERR: Failed to load TTS model")

        # load model asynchronously (so we do not block speaking from cache)
        loadModelThread = Thread(name='TtyCoqui-load-model', target=loadModel, daemon=True)
        loadModelThread.start()

        # loop
        while not self._stop:
            loadVoiceIfNew()
            self.cache_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", "coqui", self.voice.replace('.','-'))
            if not os.path.exists(self.cache_dir):
                os.makedirs(self.cache_dir)
            text, skippable, cache_requested = self.get_next_element()
            audio_cache_file, audio_cache_file_exists = cache_file(text, self.cache_dir)
            cache_used = cache_requested and len(text) < 100
            audio_file = audio_cache_file if cache_used else os.path.join(self.cache_dir, "user_" + str(uuid.uuid4()) + ".wav")

            cache_used = False # currently blocks processing for some reason and audio stutters

            # generate
            if not cache_used or not audio_cache_file_exists:

                # wait for init
                loadModelThread.join()

                # generate
                audio_chunks = self.model.inference_stream(text, "en", self.gpt_cond_latent, self.speaker_embedding, temperature=0.7, enable_text_splitting=False, speed=self.speed)
                audio_chunks_play, audio_chunks_cache = teeThreadSafe(audio_chunks, 2)

                # play
                self.play.playWavChunk(map(lambda x: x.squeeze().unsqueeze(0).cpu().squeeze().numpy(), audio_chunks_play), skippable)

                # update cache
                if cache_used:
                    audio_chunks_all = []
                    for audio_chunk in audio_chunks_play:
                        audio_chunks_all.append(audio_chunk)

                    wav = torch.cat(audio_chunks_all, dim=0)
                    torchaudio.save(audio_file, wav.squeeze().unsqueeze(0).cpu(), 24000)

            else:
                self.play.playFile(audio_file, skippable)

    def skip(self):
        super().skip()
        self.play.skip()

    def stop(self):
        super().stop()
        self.play.stop()
