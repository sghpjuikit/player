from collections.abc import Iterator
from time import sleep, time
from speech_recognition import Recognizer, Microphone, WaitTimeoutError # https://github.com/Uberi/speech_recognition
from util_tts import Tty
from util_wrt import Writer
from itertools import chain
from pysilero_vad import SileroVoiceActivityDetector

from util_actor import Actor
from typing import Callable
from collections import deque
import speech_recognition
from speech_recognition.audio import AudioData
from speech_recognition import AudioSource
import collections
import audioop
import os
import io
import numpy as np
import soundfile as sf
import torch
import traceback
import pyaudio
import math

def get_microphone_index_by_name(name):
    p = pyaudio.PyAudio()
    device_index = None

    for i in range(p.get_device_count()):
        device_info = p.get_device_info_by_index(i)
        if name == device_info['name']:
            device_index = i
            break

    p.terminate()
    return device_index


class Mic(Actor):
    def __init__(self, micName: str | None, enabled: bool, sample_rate: int, onSpeechStart: Callable[[], None], onSpeechEnd: Callable[[AudioData], None], speak: Tty, write: Writer, micEnergy: int, micEnergyDebug: bool):
        super().__init__("mic", "Mic", enabled)
        self.listening = None
        self.sample_rate: int = sample_rate
        self.onSpeechStart: Callable[[], None] = onSpeechStart
        self.onSpeechEnd: Callable[[AudioData], None] = onSpeechEnd
        self.speak = speak
        self.write = write
        self.micName = micName

        # recognizer fields
        self.energy_debug = micEnergyDebug
        self.energy_threshold = micEnergy  # minimum audio energy to consider for recording
        self.pause_threshold = 0.7  # seconds of non-speaking audio before a phrase is considered complete
        self.phrase_threshold = 0.3  # minimum seconds of speaking audio before we consider the speaking audio a phrase - values below this are ignored (for filtering out clicks and pops)
        self.non_speaking_duration = 0.5  # seconds of non-speaking audio to keep on both sides of the recording

        # voice activity detection
        self.vad_treshold = 0.5
        self.vad = SileroVoiceActivityDetector()
        if self.sample_rate != 16000: raise Exception("Sample rate for voice activity detection must be 16000")

    def set_pause_threshold_normal(self):
        self.pause_threshold = 0.7

    def set_pause_threshold_talk(self):
        self.pause_threshold = 2.0

    def _loop(self):
        self._loaded = True
        while not self._stop:

            # reconnect microphone
            source = None
            sourceI = 0
            while not self._stop and source is None:
                sourceI = sourceI+1

                # wait till mic is on
                if not self.enabled:
                    sleep(1)
                    continue

                try:
                    if self.micName is None:
                        source = Microphone(sample_rate=self.sample_rate)
                        # this helps retain mic after reconnecting
                        audio = source.pyaudio_module.PyAudio()
                        self.micName = audio.get_default_input_device_info()['name']
                        self.write(f"RAW: Using microphone: {self.micName}")
                        audio.terminate()
                    else:
                        i = get_microphone_index_by_name(self.micName)
                        if i is not None:
                            source = Microphone(device_index=i, sample_rate=self.sample_rate)
                            self.write(f"RAW: Using microphone: {self.micName}")
                        else:
                            if sourceI==1:
                                self.write(chain([f'ERR: no microphone {self.micName} found. Use one of:'], map(lambda name: '\n\t' + name, Microphone.list_microphone_names())))
                except Exception as e:
                    if sourceI==1:
                        self.speak("Failed to use microphone. See log for details")
                        traceback.print_exc()

                # mic connected
                if source is not None:
                    if sourceI>1: self.speak("Microphone back online.")
                    break

                # keep reconnecting microphone
                else:
                    sleep(1)
                    continue

            if self._stop: break

            # listen to microphone
            try:
                with source:
                    while True:
                        if self._stop: break

                        # wait till mic is on
                        while not self._stop and not self.enabled:
                            sleep(0.1)
                            continue

                        # listen to mic
                        try:
                            audio_data = self.listen(source, timeout=1)

                            # speech recognition
                            if not self._stop and self.enabled and audio_data is not None: self.onSpeechEnd(audio_data)

                        # ignore silence
                        except WaitTimeoutError:
                            pass

            # go reconnect mic
            except OSError as e:
                source = None
                if e.errno == -9988:
                    self.speak("Microphone offline. Check your microphone connection please.")
                else:
                    self.write("ERR: Other OSError occurred:" + str(e))
                    traceback.print_exc()
                pass

            # go reconnect mic
            except Exception as e:
                self.write("ERR: Error occurred:" + str(e))
                traceback.print_exc()
                pass

    # This class is derived work of Recognizer class from speech_recognition,
    # which is under BSD 3-Clause "New" or "Revised" License (https://github.com/Uberi/speech_recognition/blob/master/LICENSE.txt)
    # Copyright (c) 2014-2017, Anthony Zhang <azhang9@gmail.com>
    # All rights reserved.
    # Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
    # 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    # 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    # 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
    # THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
    def listen(self, source: Microphone, timeout=None, phrase_time_limit=None) -> AudioData | None:
        """
        Records a single phrase from ``source`` (an ``AudioSource`` instance) into an ``AudioData`` instance, which it returns.

        This is done by waiting until the audio has an energy above ``recognizer_instance.energy_threshold`` (the user has started speaking), and then recording until it encounters ``recognizer_instance.pause_threshold`` seconds of non-speaking or there is no more audio input. The ending silence is not included.

        The ``timeout`` parameter is the maximum number of seconds that this will wait for a phrase to start before giving up and throwing an ``speech_recognition.WaitTimeoutError`` exception. If ``timeout`` is ``None``, there will be no wait timeout.

        The ``phrase_time_limit`` parameter is the maximum number of seconds that this will allow a phrase to continue before stopping and returning the part of the phrase processed before the time limit was reached. The resulting audio will be the phrase cut off at the time limit. If ``phrase_timeout`` is ``None``, there will be no phrase time limit.

        This operation will always complete within ``timeout + phrase_timeout`` seconds if both are numbers, either by returning the audio data, or by raising a ``speech_recognition.WaitTimeoutError`` exception.
        """
        assert isinstance(source, AudioSource), "Source must be an audio source"
        assert source.stream is not None, "Audio source must be entered before listening, see documentation for ``AudioSource``; are you using ``source`` outside of a ``with`` statement?"
        assert self.pause_threshold >= self.non_speaking_duration >= 0

        seconds_per_buffer = float(source.CHUNK) / source.SAMPLE_RATE
        pause_buffer_count = int(math.ceil(self.pause_threshold / seconds_per_buffer))  # number of buffers of non-speaking audio during a phrase, before the phrase should be considered complete
        phrase_buffer_count = int(math.ceil(self.phrase_threshold / seconds_per_buffer))  # minimum number of buffers of speaking audio before we consider the speaking audio a phrase
        non_speaking_buffer_count = int(math.ceil(self.non_speaking_duration / seconds_per_buffer))  # maximum number of buffers of non-speaking audio to retain before and after a phrase

        # read audio input for phrases until there is a phrase that is long enough
        elapsed_time = 0  # number of seconds of audio read
        buffer = b""  # an empty buffer means that the stream has ended and there is no data left to read
        energy_debug_last = time()
        while True:
            frames = deque()

            # store audio input until the phrase starts
            while True:
                # handle waiting too long for phrase by raising an exception
                elapsed_time += seconds_per_buffer
                if timeout and elapsed_time > timeout: raise WaitTimeoutError("listening timed out while waiting for phrase to start")

                buffer = source.stream.read(source.CHUNK)
                if len(buffer) == 0: break  # reached end of the stream
                frames.append(buffer)
                if len(frames) > non_speaking_buffer_count: frames.popleft()  # ensure we only keep the needed amount of non-speaking buffers

                # detect whether speaking has started on audio input
                energy = audioop.rms(buffer, source.SAMPLE_WIDTH)  # energy of the audio signal
                if self.energy_debug and time()-energy_debug_last>0.5:
                    energy_debug_last = time()
                    self.write(f"RAW: Mic energy={energy}/{self.energy_threshold}")
                if energy > self.energy_threshold: break

            # read audio input until the phrase ends
            pause_count, phrase_count = 0, 0
            phrase_start_time = elapsed_time
            has_speech = False
            while True:
                # handle phrase being too long by cutting off the audio
                elapsed_time += seconds_per_buffer
                if phrase_time_limit and elapsed_time - phrase_start_time > phrase_time_limit: break

                buffer = source.stream.read(source.CHUNK)
                if len(buffer) == 0: break  # reached end of the stream
                frames.append(buffer)
                phrase_count += 1

                # vad
                is_speech = self.vad(buffer) >= self.vad_treshold
                if not has_speech and is_speech:
                    has_speech = True
                    # invoke speech start handler
                    self.onSpeechStart()

                # check if speaking has stopped for longer than the pause threshold on the audio input
                energy = audioop.rms(buffer, source.SAMPLE_WIDTH)  # unit energy of the audio signal within the buffer
                if self.energy_debug and time()-energy_debug_last>0.5:
                    energy_debug_last = time()
                    self.write(f"RAW: Mic energy={energy}/{self.energy_threshold}")
                if energy > self.energy_threshold: pause_count = 0
                else: pause_count += 1
                if pause_count > pause_buffer_count: break  # end of the phrase

            # check how long the detected phrase is, and retry listening if the phrase is too short
            phrase_count -= pause_count  # exclude the buffers for the pause before the phrase
            if phrase_count >= phrase_buffer_count or len(buffer) == 0: break  # phrase is long enough or we've reached the end of the stream, so stop listening

        # obtain frame data
        for i in range(pause_count - non_speaking_buffer_count): frames.pop()  # remove extra non-speaking frames at the end
        frame_data = b"".join(frames)

        if has_speech:
            return AudioData(frame_data, source.SAMPLE_RATE, source.SAMPLE_WIDTH)
        else:
            return None