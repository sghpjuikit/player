import collections.abc
import os
import threading
import asyncio
import uuid
import util_dir_cache
from util_write_engine import Writer
from collections.abc import Iterator
from typing import cast
from queue import Queue

class VlcActor:

    def __init__(self, vlc_path: str, write: Writer):
        self.vlc_path = vlc_path
        self.write = write
        self.skip_ = False
        self.queue = Queue()
        threading.Thread(target=self.loop, daemon=True).start()

    def loop(self):
        # initialize vlc
        if len(self.vlc_path)>0 and os.path.exists(self.vlc_path):
            os.environ['PYTHON_VLC_MODULE_PATH'] = self.vlc_path
            os.environ['PYTHON_VLC_LIB_PATH'] = os.path.join(self.vlc_path, "libvlc.dll")
        try:
            import vlc
            import ctypes
        except ImportError as e:
            self.write("Vlc player or vlc python module failed to load")
            return

        # loop
        vlcInstance = None
        vlcPlayer = None
        while True:
            type, audio, skippable = self.queue.get()

            if self.skip_ and skippable:
                if vlcPlayer is not None:
                    player.stop()
                continue
            else:
                self.skip_ = False

            # initialize vlc once lazily
            if vlcInstance is None:
                vlcInstance = vlc.Instance()
                vlcPlayer = vlcInstance.media_player_new()

            # play audio file
            if type == 'f':
                media = vlcInstance.media_new(audio)
            # play audio data
            if type == 'b':

                # Define the read callback function
                def read_callback(data, n, p_buffer):
                    # Convert the audio data to bytes and copy it to the buffer
                    p_buffer[0] = audio_data.tobytes()
                    return len(audio_data) * audio_data.itemsize

                def seek_callback(opaque, offset):
                    return 0

                def close_callback(opaque):
                    pass

                media = instance.media_new_callbacks(
                    read_callback,
                    seek_callback,
                    close_callback,
                    len(audio)
                )

            vlcPlayer.set_media(media)
            vlcPlayer.play()

            # Wait to finish
            while vlcPlayer.get_state() != vlc.State.Ended:
                pass

        # dispose
        if vlcPlayer is not None:
            player.stop()
            player.release()
        if vlcInstance is not None:
            vlcInstance.release()

    def skip(self):
        self.skip_ = True

    def stop(self):
        pass


class Tty:
    def __init__(self, tty, write: Writer):
        self.sentence_min_length = 40 # shorter == faster feedback, longer == less audio hallucinations in short audio
        self.tty = tty
        self.write = write
        self.sentence = None
        self.sentence_mode = False
        self.sentence_queue = Queue()
        self.speeches_queue = Queue()

    def __call__(self, event: str, use_cache=True):
        if isinstance(event, str):
            self.speak(event, use_cache)

        elif isinstance(event, list):
            self.__call__(iter(event), use_cache)

        elif isinstance(event, Iterator):

            # iterableStart
            self.sentence = ''
            self.sentence_mode = True

            for e in event:
                self.iterablePart(e)

            # iterableEnd
            if len(self.sentence)>0:
                self.sentence_queue.put(self.sentence)
                self.process()
            self.sentence = ''
            self.sentence_mode = False
            self.process()

    def iterablePart(self, text: str):
        self.sentence = self.sentence + text
        while len(self.sentence) >= self.sentence_min_length:
            index = -1
            if index==-1:
                index = self.sentence.rfind('.')
            if index==-1:
                index = self.sentence.rfind('\n')
            if index==-1:
                break
            else:
                sentence = self.sentence[:index]
                self.sentence = self.sentence[index+1:]
                if len(sentence)>0:
                    self.sentence_queue.put(sentence+'.')
                    self.process()

    def iterableSkip(self):
        while not self.sentence_queue.empty():
            try:
                self.sentence_queue.get(block=False)
            except Exception:
                break
        self.tty.skip()

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

    def skip(self):
        self.tty.skip()

    def stop(self):
        self.tty.stop()


class TtyNone:
    # noinspection PyUnusedLocal
    def speak(self, text, skippable, use_cache=True):
        pass

    def stop(self):
        pass

class TtyOsMac:
    def __init__(self):
        self.allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,?!-_$:+-/ ")
        self.skip_ = False
        self.queue = Queue()
        threading.Thread(target=self.loop, daemon=True).start()

    # noinspection PyUnusedLocal
    def speak(self, text, skippable, use_cache):
        self.queue.put((text, skippable))

    def loop(self):
        while True:
            textRaw, skippable = self.queue.get()
            if self.skip_ and skippable:
                continue
            else:
                self.skip_ = False
            text = ''.join(c for c in textRaw if c in self.allowed_chars)
            os.system(f"say '{text}'")

    def skip(self):
        skipping = True

    def stop(self):
        pass

class TtyOs:
    def __init__(self, write: Writer):
        self.write = write
        self.skip_ = False
        self.queue = Queue()
        threading.Thread(target=self.loop, daemon=True).start()

    # noinspection PyUnusedLocal
    def speak(self, text, skippable, use_cache):
        self.queue.put((text, skippable))

    def loop(self):
        # initialize pyttsx3
        try:
            import pyttsx3
        except ImportError as e:
            self.write("pyttsx3 python module failed to load")
            return

        while True:
            text, skippable = self.queue.get()
            if self.skip_ and skippable:
                continue
            else:
                self.skip_ = False

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

    def skip(self):
        skip = True

    def stop(self):
        pass


class TtyCharAi:
    def __init__(self, token: str, voice: int, vlcActor: VlcActor, write: Writer):
        self.cache_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", "charai")
        self.token = token
        self.voice = voice
        self.vlcActor = vlcActor
        self.write = write
        self.skip_ = False
        self.queue = Queue()
        threading.Thread(target=self.process_queue_start, daemon=True).start()

    def speak(self, text, skippable, use_cache):
        self.queue.put((text, skippable, use_cache))

    def process_queue_start(self):
        asyncio.run(self.loop())

    async def loop(self):
        # initialize character.ai https://github.com/Xtr4F/PyCharacterAI
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
        while True:
            text, skippable, cache_requested = self.queue.get()
            if self.skip_ and skippable:
                continue
            else:
                self.skip_ = False
            audio_cache_file, audio_cache_file_exists = util_dir_cache.cache_file(text, self.cache_dir)
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

            self.vlcActor.queue.put(('f', audio_file, skippable))

    def skip(self):
        skip_ = True

    def stop(self):
        pass

# https://pypi.org/project/TTS/
class TtyCoqui:
    def __init__(self, voice: str, vlcActor: VlcActor, write: Writer):
        self.cache_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache", "coqui")
        self.voice = voice
        self.vlcActor = vlcActor
        self.write = write
        self.skip_ = False
        self.queue = Queue()
        threading.Thread(target=self.loop, daemon=True).start()

    def speak(self, text, skippable, use_cache):
        self.queue.put((text, skippable, use_cache))

    def loop(self):
        # initialize torch
        try:
            import torch
            import numpy
        except ImportError:
            self.write("Torch python module failed to load")
            return
        try:
            assert torch.cuda.is_available(), "CUDA is not availabe on this machine."
        except Exception:
            self.write("Torch cuda not available. Make sure you have cuda compatible hardware and software")
            return
        # initialize coqui
        try:
            from TTS.api import TTS
        except ImportError:
            self.write("Coqui TTS python module failed to load")
            return

        # load model once lazily
        device = "cuda" if torch.cuda.is_available() else "cpu"
        model = TTS("tts_models/multilingual/multi-dataset/xtts_v2").to(device)

        # loop
        while True:
            audioTmpFile = os.path.join(self.cache_dir, "user_" + str(uuid.uuid4()) + ".wav")
            text, skippable, cache_requested = self.queue.get()
            if self.skip_ and skippable:
                continue
            else:
                self.skip_ = False
            audio_cache_file, audio_cache_file_exists = util_dir_cache.cache_file(text, self.cache_dir)
            cache_used = cache_requested and len(text) < 100
            audio_file = audio_cache_file if cache_used else os.path.join(self.cache_dir, "user_" + str(uuid.uuid4()) + ".wav")

            # generate
            if not cache_used or not audio_cache_file_exists:
                voiceFile = os.path.join("voices-coqui", self.voice)
                if not os.path.exists(voiceFile):
                   self.write("Voice " + self.voice + "does not exist")
                else:
                    if cache_used:
                        class Segment:
                            def segment(self, text):
                                return [text]
                        model.synthesizer.seg = Segment()
                        model.tts_to_file(text=text, file_path=audio_file, speed = 1.5, speaker_wav=voiceFile, language="en")
                        self.vlcActor.queue.put(('f', audio_file, skippable))
                    else:
                        model.tts_to_file(text=text, file_path=audio_file, speed = 1.5, speaker_wav=voiceFile, language="en")
                        self.vlcActor.queue.put(('f', audio_file, skippable))
                        # avoid file and play from ram
                        # audio = model.tts(text=text, speed=1.5, speaker_wav=self.voice, language="en")
                        # self.vlcActor.queue.put(('b', numpy.array(audio, dtype=numpy.uint8).tobytes(), skippapable))
            else:
                self.vlcActor.queue.put(('f', audio_file, skippable))

    def skip(self):
        skip_ = True
        self.vlcActor.skip()

    def stop(self):
        pass
