
import os
import pyttsx3
import threading
import asyncio
import vlc
import util_dir_cache
from typing import cast
from queue import Queue
from PyCharacterAI import Client  # https://github.com/Xtr4F/PyCharacterAI


class Tty:
    def __init__(self):
        self.queue = Queue()
        threading.Thread(target=self.loop, daemon=True).start()

    # noinspection PyUnusedLocal
    def speak(self, text, use_cache=True):
        self.queue.put(text)

    def loop(self):
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

    def __init__(self, token, voice):
        self.token = token
        self.voice = voice
        self.queue = Queue()
        threading.Thread(target=self.process_queue_start, daemon=True).start()

    def speak(self, text, use_cache=True):
        self.queue.put((text, use_cache))

    def process_queue_start(self):
        asyncio.run(self.loop())

    async def loop(self):
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
