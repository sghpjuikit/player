
import os
import threading
import asyncio
import util_dir_cache
from util_write_engine import Writer
from typing import cast
from queue import Queue


class TtyNone:
    # noinspection PyUnusedLocal
    def speak(self, text, use_cache=True):
        pass

    def stop(self):
        pass

class TtyOsMac:
    allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,?!-_$:+-/ ")

    def __init__(self):
        self.queue = Queue()
        threading.Thread(target=self.loop, daemon=True).start()

    # noinspection PyUnusedLocal
    def speak(self, text, use_cache=True):
        self.queue.put(text)

    def loop(self):
        while True:
            textRaw = self.queue.get()
            text = ''.join(c for c in textRaw if c in allowed_chars)
            os.system(f"say '{text}'")

    def stop(self):
        pass

class TtyOs:
    def __init__(self, write: Writer):
        self.write = write
        self.queue = Queue()
        threading.Thread(target=self.loop, daemon=True).start()

    # noinspection PyUnusedLocal
    def speak(self, text, use_cache=True):
        self.queue.put(text)

    def loop(self):
        # initialize pyttsx3
        try:
            import pyttsx3
        except ImportError as e:
            self.write("pyttsx3 python module failed to load")
            return

        while True:
            text = self.queue.get()

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

    def stop(self):
        pass


class TtyCharAi:
    token = None
    audioTmpFile = "voice.mp3"  # Path to the directory where you want to save the audio
    cache_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "cache")

    def __init__(self, token: str, voice: int, vlc_path: str, write: Writer):
        self.token = token
        self.voice = voice
        self.vlc_path = vlc_path
        self.write = write
        self.queue = Queue()
        threading.Thread(target=self.process_queue_start, daemon=True).start()

    def speak(self, text, use_cache=True):
        self.queue.put((text, use_cache))

    def process_queue_start(self):
        asyncio.run(self.loop())

    async def loop(self):
        # initialize vlc
        if len(self.vlc_path)>0 and os.path.exists(self.vlc_path):
            os.environ['PYTHON_VLC_MODULE_PATH'] = self.vlc_path
            os.environ['PYTHON_VLC_LIB_PATH'] = os.path.join(self.vlc_path, "libvlc.dll")
        try:
            import vlc
        except ImportError as e:
            self.write("Vlc player or vlc python module faile to load")
            return

        # initialize character.ai https://github.com/Xtr4F/PyCharacterAI
        from PyCharacterAI import Client
        client = None

        while True:
            text, cache_requested = self.queue.get()
            audio_cache_file, audio_cache_file_exists = util_dir_cache.cache_file(text, self.cache_dir)
            cache_used = cache_requested and len(text) < 100
            audio_file = audio_cache_file if cache_used else self.audioTmpFile

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

            # play audio
            audio_player = vlc.MediaPlayer(audio_file)
            audio_player.play()

    def stop(self):
        pass
