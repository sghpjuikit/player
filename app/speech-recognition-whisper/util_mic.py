from collections.abc import Iterator
from time import sleep, time
from speech_recognition import Recognizer, Microphone, WaitTimeoutError # https://github.com/Uberi/speech_recognition
from util_tts import Tts
from util_wrt import Writer
from itertools import chain
import nemo.collections.asr as nemo_asr
from pysilero_vad import SileroVoiceActivityDetector
from speech_recognition.audio import AudioData
from speech_recognition import AudioSource
from collections import deque
from datetime import datetime
from util_actor import Actor
from imports import *
import speech_recognition
import soundfile as sf
import numpy as np
import collections
import pyaudio
import librosa
import audioop
import torch
import math
import os
import io

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

@dataclass
class SpeechStart:
    start: datetime
    user: str

@dataclass
class Speech:
    start: datetime
    audio: AudioData
    stop: datetime
    user: str

OnSpeechStart = Callable[[SpeechStart], None]

OnSpeechEnd = Callable[[Speech], None]

class Mic(Actor):
    def __init__(self, micName: str | None, enabled: bool, sample_rate: int, onSpeechStart: OnSpeechStart, onSpeechEnd: OnSpeechEnd, speak: Tts, write: Writer, micEnergy: int, verbose: bool, speakerDiarLoader):
        super().__init__("mic", "Mic", "cpu", write, enabled)
        self.listening = None
        self.sample_rate: int = sample_rate
        self.onSpeechStart: OnSpeechStart = onSpeechStart
        self.onSpeechEnd: OnSpeechEnd = onSpeechEnd
        self.speak = speak
        self.micName = micName

        # recognizer fields
        self.energy_debug = verbose
        self.energy_threshold = micEnergy  # minimum audio energy to consider for recording
        self.pause_threshold = 0.7  # seconds of non-speaking audio before a phrase is considered complete
        self.phrase_threshold = 0.3  # minimum seconds of speaking audio before we consider the speaking audio a phrase - values below this are ignored (for filtering out clicks and pops)
        self.non_speaking_duration = 0.5  # seconds of non-speaking audio to keep on both sides of the recording

        # voice activity detection
        self.vad_treshold = 0.5
        self.vad_detector = SileroVoiceActivityDetector()
        if self.sample_rate != 16000: raise Exception("Sample rate for voice activity detection must be 16000")

        # speaker detection
        self.speakerDiarLoader = speakerDiarLoader
        self.speaker_diar = MicVoiceDetectNone()

    def set_pause_threshold_normal(self):
        self.pause_threshold = 0.7

    def set_pause_threshold_talk(self):
        self.pause_threshold = 2.0

    def _get_event_text(self, e) -> str | None:
        return f'{e}'

    def _loop(self):
        self.speaker_diar = self.speakerDiarLoader()
        self._loaded = True

        import time
        self.processing_start = time.time()
        self.processing_event = "Microphone audio streaming..."
        self.processing = True

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
                        print_exc()

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
                            speech = self.listen(source, timeout=1)
                            if not self._stop and self.enabled and speech is not None: self.onSpeechEnd(speech)

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
                    print_exc()
                pass

            # go reconnect mic
            except Exception as e:
                self.write("ERR: Error occurred:" + str(e))
                print_exc()
                pass

        self.processing_event = None
        self.processing = False
        self.processing_start = None


    # This class is derived work of Recognizer class from speech_recognition,
    # which is under BSD 3-Clause "New" or "Revised" License (https://github.com/Uberi/speech_recognition/blob/master/LICENSE.txt)
    # Copyright (c) 2014-2017, Anthony Zhang <azhang9@gmail.com>
    # All rights reserved.
    # Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
    # 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    # 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
    # 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
    # THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
    def listen(self, source: Microphone, timeout=None, phrase_time_limit=None) -> Speech | None:
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
        isEnoughEnergyAndSpeech = False
        isEnoughEnergyAndCorrectSpeech = False
        speechStart = None
        user = None
        while True:
            frames = deque()
            isEnoughEnergyAndSpeech = False
            isEnoughEnergyAndCorrectSpeech = False


            # store audio input until the phrase starts
            while True:
                # handle waiting too long for phrase by raising an exception
                elapsed_time += seconds_per_buffer
                if timeout and elapsed_time > timeout: raise WaitTimeoutError("listening timed out while waiting for phrase to start")

                buffer = source.stream.read(source.CHUNK)
                if len(buffer) == 0: break  # reached end of the stream
                frames.append(buffer)
                if len(frames) > non_speaking_buffer_count: frames.popleft()  # ensure we only keep the needed amount of non-speaking buffers

                # energy
                energy = audioop.rms(buffer, source.SAMPLE_WIDTH)  # energy of the audio signal
                energyDebugNeeded = self.energy_debug and time()-energy_debug_last>0.5
                if energyDebugNeeded: energy_debug_last = time()
                if energyDebugNeeded: self.write(f"RAW: Mic energy={energy}/{self.energy_threshold}")

                # detect whether speaking has started
                isEnoughEnergy = energy > self.energy_threshold
                isEnoughEnergyAndSpeech = isEnoughEnergy and self.vad(buffer)
                if isEnoughEnergyAndSpeech: break

            # read audio input until the phrase ends
            pause_count, phrase_count = 0, 0
            phrase_start_time = elapsed_time
            isEnoughEnergyAndCorrectSpeechEvaluated = False
            while True:
                # handle phrase being too long by cutting off the audio
                elapsed_time += seconds_per_buffer
                if phrase_time_limit and elapsed_time - phrase_start_time > phrase_time_limit: break

                buffer = source.stream.read(source.CHUNK)
                if len(buffer) == 0: break  # reached end of the stream
                frames.append(buffer)
                phrase_count += 1

                # energy
                energy = audioop.rms(buffer, source.SAMPLE_WIDTH)  # unit energy of the audio signal within the buffer
                energyDebugNeeded = self.energy_debug and time()-energy_debug_last>0.5
                if energyDebugNeeded: energy_debug_last = time()
                if energyDebugNeeded: self.write(f"RAW: Mic energy={energy}/{self.energy_threshold}")

                # detect whether correct speaking has started
                isEnoughEnergy = energy > self.energy_threshold
                isEnoughEnergyAndSpeech_buffer = isEnoughEnergy and self.vad(buffer)
                if isEnoughEnergyAndSpeech_buffer and phrase_count >= phrase_buffer_count:
                    if not isEnoughEnergyAndCorrectSpeechEvaluated:
                        isEnoughEnergyAndCorrectSpeechEvaluated = True
                        user = self.speaker_diar.isCorrectSpeaker(AudioData(b"".join(frames), self.sample_rate, source.SAMPLE_WIDTH))
                        isEnoughEnergyAndCorrectSpeech = user is not None
                        if isEnoughEnergyAndCorrectSpeech:
                            speechStart = datetime.now()
                            self.onSpeechStart(SpeechStart(speechStart, user)) # invoke speech start handler

                # check if speaking has stopped for longer than the pause threshold on the audio input
                if isEnoughEnergyAndSpeech_buffer: pause_count = 1
                else: pause_count += 1
                if pause_count > pause_buffer_count: break  # end of the phrase

            # check how long the detected phrase is, and retry listening if the phrase is too short
            phrase_count -= pause_count  # exclude the buffers for the pause before the phrase
            if phrase_count >= phrase_buffer_count: break  # phrase is long enough

        # obtain frame data
        for i in range(pause_count - non_speaking_buffer_count): frames.pop()  # remove extra non-speaking frames at the end
        frame_data = b"".join(frames)

        if isEnoughEnergyAndCorrectSpeech: return Speech(speechStart, AudioData(frame_data, source.SAMPLE_RATE, source.SAMPLE_WIDTH), datetime.now(), user)
        else: return None

    def vad(self, buffer) -> bool:
        return self.vad_detector(buffer) >= self.vad_treshold

@dataclass
class MicVoice:
    name: str
    data: object | None
    def __str__(self): return f'{self.name}'
    def __repr__(self): return f'{self.name}'

class MicVoiceDetectNone:
    def isCorrectSpeaker(self, audio_data) -> str | None:
        return "User"

class MicVoiceDetectNvidia:
    def __init__(self, speaker_treshold: float, verbose: bool):
        self.speaker_treshold = speaker_treshold # cosine similarity score used as a threshold to distinguish two embeddings (default = 0.7)
        self.speaker_model = self.loadSpeakerModel()
        self.speakers_correct = self.loadSpeakersCorrect()
        self.verbose = verbose
        self.sample_rate = 16000
        if self.sample_rate != 16000: raise Exception("Sample rate for voice activity detection must be 16000")

    def loadSpeakerModel(self):
        try:
            m = nemo_asr.models.EncDecSpeakerLabelModel.from_pretrained("nvidia/speakerverification_en_titanet_large")
            m.to(torch.device("cuda:1"))
            return m
        except Exception as r:
            print(f"ERR: failed to load speaker detector model {e}", end='')
            print_exc()
            return None

    def loadSpeakersCorrect(self) -> list[MicVoice] | None:
        try:
            dir = 'voices-verified'
            voices = []
            for f in os.listdir(dir):
                if f.endswith('.wav'):
                    voices.append(MicVoice(f.removesuffix('.wav'), self.loadSpeakerFromFile(os.path.join(dir, f))))
            print(f"RAW: loading verified voices: {voices}", end='')
            return voices
        except Exception as e:
            print(f"ERR: loading verified voices failed: {e}", end='')
            print_exc()
            return None

    def loadSpeakerFromFile(self, file: str) -> object:
        embs = self.speaker_model.get_embedding(file).squeeze()
        return embs / torch.linalg.norm(embs)

    def loadSpeakerFromAudio(self, buffer) -> bool:
        embs = self.infer_audio(buffer).squeeze()
        return embs / torch.linalg.norm(embs)

    @torch.no_grad()
    def infer_audio(self, audio):
        """
        Args:
            path2audio_file: path to an audio wav file

        Returns:
            emb: speaker embeddings (Audio representations)
            logits: logits corresponding of final layer
        """
        audio, sr = audio, self.sample_rate
        target_sr = self.speaker_model._cfg.train_ds.get('sample_rate', 16000)
        if sr != target_sr: audio = librosa.core.resample(audio, orig_sr=sr, target_sr=target_sr)
        audio_length = audio.shape[0]
        device = self.speaker_model.device
        audio = np.array([audio])
        audio_signal, audio_signal_len = (
            torch.tensor(audio, device=device),
            torch.tensor([audio_length], device=device),
        )
        mode = self.speaker_model.training
        self.speaker_model.freeze()

        _, emb = self.speaker_model.forward(input_signal=audio_signal, input_signal_length=audio_signal_len)

        self.speaker_model.train(mode=mode)
        if mode is True:
            self.speaker_model.unfreeze()
        del audio_signal, audio_signal_len
        return emb

    def isCorrectSpeaker(self, audio_data) -> bool:
        # no model loaded
        if self.speaker_model is None: return True
        # no verified speakers set
        if self.speakers_correct is None: return True

        try:
            # impl from check nemo_asr.models.EncDecSpeakerLabelModel.verify_speakers method
            from io import BytesIO
            import soundfile as sf
            wav_bytes = audio_data.get_wav_data()
            audio_array, sampling_rate = sf.read(BytesIO(wav_bytes))
            audio_array = audio_array.astype(np.float32)

            for voice in self.speakers_correct:
                X = voice.data
                Y = self.loadSpeakerFromAudio(audio_array)
                similarity_score = torch.dot(X, Y) / ((torch.dot(X, X) * torch.dot(Y, Y)) ** 0.5)
                similarity_score = (similarity_score + 1) / 2
                verified = similarity_score >= self.speaker_treshold
                if self.verbose: print(f'RAW: {voice.name}:{verified} {similarity_score}{">=" if verified else "<"}{self.speaker_treshold}', end='')
                if verified: return voice.name
            return None
        except Exception as e:
            print(f'ERR: failed to determine speaker: {e}')
            print_exc()
            return None