from collections.abc import Iterator
from time import sleep, time
from util_tts import Tts
from util_wrt import Writer
from itertools import chain
import nemo.collections.asr as nemo_asr
from util_vad import SileroVoiceActivityDetectorWithCuda
from collections import deque
from datetime import datetime
from util_actor import Actor
from threading import Lock
from util_now import *
from imports import *
from threading import current_thread
import sounddevice as sd
import soundfile as sf
import numpy as np
import librosa
import pyaudio
import audioop
import struct
import torch
import wave
import time
import math
import os
import io
import audioop

@dataclass
class SpeechStart:
    start: datetime
    user: str
    location: str

@dataclass
class AudioData:
    frame_data: bytes
    sample_rate: int
    sample_width: int

    def __str__(self):
        return f"AudioData({self.sample_rate}Hz, {len(self.frame_data)/self.sample_width/self.sample_rate}s)"

    def __repr__(self):
        return f"AudioData({self.sample_rate}Hz, {len(self.frame_data)/self.sample_width/self.sample_rate}s)"

    def get_wav_data(self):
        """
        Returns a byte string representing the contents of a WAV file containing the audio represented by the ``AudioData`` instance.
        This method uses BytesIO to temporarily store the WAV file data before returning it.
        """
        # Create a BytesIO object to hold the WAV file data
        wav_io = io.BytesIO()

        # Open a temporary WAV file in memory
        with wave.open(wav_io, 'wb') as wav_file:
            # Set the properties of the WAV file
            wav_file.setnchannels(1)  # Mono
            wav_file.setsampwidth(self.sample_width)
            wav_file.setframerate(self.sample_rate)

            # Write the raw audio data to the WAV file
            wav_file.writeframesraw(self.frame_data)

        # Get the WAV file data from the BytesIO object
        wav_data = wav_io.getvalue()

        return wav_data

@dataclass
class Speech:
    start: datetime
    audio: AudioData
    stop: datetime
    user: str
    location: str

OnSpeechStart = Callable[[SpeechStart], None]

OnSpeechEnd = Callable[[Speech], None]

class Mic(Actor):
    def __init__(
        self,
        micName: str | None, enabled: bool, location: str,
        sample_rate: int, onSpeechStart: OnSpeechStart, onSpeechEnd: OnSpeechEnd,
        speak: Tts, write: Writer, micEnergy: int, verbose: bool,
        vad, micSpeakerDetector
    ):
        super().__init__(f"Mic({micName})", f"Mic({micName})", micName, write, enabled)
        self.listening = None
        self.group = micName
        self.name = micName
        self.location = location
        self.sample_rate: int = sample_rate
        self.onSpeechStart: OnSpeechStart = onSpeechStart
        self.onSpeechEnd: OnSpeechEnd = onSpeechEnd
        self.speak = speak
        self.micName = micName
        self.last_energy = 0.0
        # recognizer fields
        self.energy_debug = verbose
        self.energy_threshold = micEnergy  # minimum audio energy to consider for recording
        self.pause_threshold = 0.7  # seconds of non-speaking audio before a phrase is considered complete
        self.phrase_threshold = 0.3  # minimum seconds of speaking audio before we consider the speaking audio a phrase - values below this are ignored (for filtering out clicks and pops)
        self.non_speaking_duration = 0.3  # seconds of non-speaking audio to keep on both sides of the recording
        # voice activity detection
        self.vad = vad
        # speaker detection
        self.speaker_diar = micSpeakerDetector
        self.p = pyaudio.PyAudio()

    def set_pause_threshold_normal(self):
        self.pause_threshold = 0.7

    def set_pause_threshold_talk(self):
        self.pause_threshold = 2.0

    def _get_event_text(self, e) -> str | None:
        return f'{e}'

    def get_default_microphone_index(self) -> (int, str):
        d = sd.query_devices(kind='input')
        return (d['index'], d['name'])

    def get_microphone_index_by_name(self, name) -> int | None:
        ds = sd.query_devices()
        # windows has 31 name limit, if name is cut off, the matching will fail
        for d in ds:
            if name[:31] == d['name']:
                return d['index']
        for d in ds:
            if name == d['name']:
                return d['index']
        return None

    def get_microphone_names(self) -> [str]:
        names = []
        for d in sd.query_devices():
            if d['name'] is not None:
                names.append(d['name'])
        return names

    def _loop(self):
        self._loaded = True
        self.write(f"RAW: {self.name} loaded")

        self.processing_start = time.time()
        self.processing_event = "Microphone audio streaming..."
        self.processing = True
        while not self._stop:

            # re/connect microphone
            iError = -1
            iDefaut = None
            i = iError
            source = None
            loop = 0
            while not self._stop and source is None:
                loop = loop+1
                loopInit = loop==1
                wait_until(1.0, lambda: self.enabled)

                try:
                    if self.micName is None:
                        i, name = self.get_default_microphone_index()
                        self.micName = name # this helps retain mic after reconnecting
                    else:
                        i = self.get_microphone_index_by_name(self.micName)

                    self.write(f"RAW: Using microphone: {self.micName}")
                    self.name = f"Mic - {self.location}"
                    self.group = f"Mic - {self.location}"
                    self.deviceName = self.micName
                    source = i

                    # notify of error
                    loopInitFail = source is None and loopInit
                    if loopInitFail: self.speak(f"Failed to use microphone {self.location}. See log for details")
                    if loopInitFail: self.write(chain([f'ERR: no microphone {self.micName} found. Use one of:'], map(lambda name: '\n\t' + name, self.get_microphone_names())))
                except Exception as e:
                    # notify of error
                    if loopInit: self.speak(f"Failed to use microphone {self.location}. See log for details")
                    if loopInit: print_exc()

                # notify mic connected
                if source is not None and not loopInit: self.speak(f"Microphone {self.location} back online.")
                if source is not None: break

                # keep reconnecting microphone
                if source is None: sleep(1)
                if source is None: continue

            if self._stop: break

            # listen to microphone
            try:
                while True:
                    wait_until(1.0, lambda: self.enabled or self._stop)
                    if self._stop: break

                    # listen to mic (record to queue)
                    blocksize = int(0.0625 * self.sample_rate)
                    blocksize = 1024
                    stream = self.p.open(format=pyaudio.paInt16, channels=1, rate=self.sample_rate, input=True, frames_per_buffer=blocksize, input_device_index=source)
                    while not self._stop and self.enabled and not stream.is_stopped():
                        speech = self.listen(stream)
                        if not self._stop and self.enabled and speech is not None: self.onSpeechEnd(speech)
                    stream.stop_stream()
                    stream.close()
                    self.p.terminate()


            # go reconnect mic
            except OSError as e:
                source = None
                if e.errno == -9988:
                    # notify mic disconnected
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
    def listen(self, stream) -> Speech | None:
        assert self.pause_threshold >= self.non_speaking_duration >= 0

        # CHUNK_SIZE = int(0.0625 * self.sample_rate)
        CHUNK_SIZE = 1024
        CHANNELS = 1
        SAMPLE_WIDTH = 2
        SAMPLE_RATE = self.sample_rate

        phrase_time_limit = None
        seconds_per_buffer = float(CHUNK_SIZE) / SAMPLE_RATE
        pause_buffer_count = int(math.ceil(self.pause_threshold / seconds_per_buffer))  # number of buffers of non-speaking audio during a phrase, before the phrase should be considered complete
        phrase_buffer_count = int(math.ceil(self.phrase_threshold / seconds_per_buffer))  # minimum number of buffers of speaking audio before we consider the speaking audio a phrase
        non_speaking_buffer_count = int(math.ceil(self.non_speaking_duration / seconds_per_buffer))  # maximum number of buffers of non-speaking audio to retain before and after a phrase

        # read audio input for phrases until there is a phrase that is long enough
        elapsed_time = 0  # number of seconds of audio read
        buffer = b""  # an empty buffer means that the stream has ended and there is no data left to read
        energy_debug_last = time.time()
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
                if self._stop or not self.enabled: return None

                # handle waiting too long for phrase by raising an exception
                elapsed_time += seconds_per_buffer

                buffer = stream.read(CHUNK_SIZE)
                # dnp = np.frombuffer(buffer, dtype=np.int16).astype(np.float32) / 32767
                # energy = np.sqrt(np.sum(dnp)**2 / CHUNK_SIZE)
                energy = audioop.rms(buffer, SAMPLE_WIDTH)
                self.last_energy = energy
                if len(buffer) == 0 and self.energy_debug: self.write(f'{self.name} end of stream')
                if len(buffer) == 0: return None # reached end of the stream
                frames.append(buffer)
                if len(frames) > non_speaking_buffer_count: frames.popleft()  # ensure we only keep the needed amount of non-speaking buffers

                # energy
                energyDebugNeeded = self.energy_debug and time.time()-energy_debug_last>1.0
                if energyDebugNeeded: energy_debug_last = time.time()
                if energyDebugNeeded: self.write(f"RAW: {self.name} energy={int(energy)}/{int(self.energy_threshold)}")

                # detect whether speaking has started
                isEnoughEnergy = energy > self.energy_threshold
                isEnoughEnergyAndSpeech = isEnoughEnergy and self.vad.is_voice(buffer)
                if isEnoughEnergyAndSpeech and self.energy_debug: self.write(f'{self.name} energy treshold reached')
                if isEnoughEnergyAndSpeech: break

            # read audio input until the phrase ends
            pause_count, phrase_count = 0, 0
            phrase_start_time = elapsed_time
            isEnoughEnergyAndCorrectSpeechEvaluated = False
            while True:
                if self._stop or not self.enabled: return None

                # handle phrase being too long by cutting off the audio
                elapsed_time += seconds_per_buffer
                if phrase_time_limit and elapsed_time - phrase_start_time > phrase_time_limit and self.energy_debug: self.write(f'{self.name} phrase_time_limit({elapsed_time})')
                if phrase_time_limit and elapsed_time - phrase_start_time > phrase_time_limit: break

                buffer = stream.read(CHUNK_SIZE)
                # dnp = np.frombuffer(buffer, dtype=np.int16).astype(np.float32) / 32767
                # energy = np.sqrt(np.sum(dnp)**2 / CHUNK_SIZE)
                energy = audioop.rms(buffer, SAMPLE_WIDTH)
                self.last_energy = energy
                if len(buffer) == 0 and self.energy_debug: self.write(f'{self.name} end of stream')
                if len(buffer) == 0: return None # reached end of the stream
                frames.append(buffer)
                phrase_count += 1

                # energy
                energyDebugNeeded = self.energy_debug and time.time()-energy_debug_last>1.0
                if energyDebugNeeded: energy_debug_last = time.time()
                if energyDebugNeeded: self.write(f"RAW: {self.name} energy={int(energy)}/{int(self.energy_threshold)}")

                # detect speech start
                isEnoughEnergy = energy > self.energy_threshold
                isEnoughEnergyAndSpeech_buffer = isEnoughEnergy and self.vad.is_voice(buffer)
                if isEnoughEnergyAndSpeech_buffer and phrase_count >= phrase_buffer_count:
                    if not isEnoughEnergyAndCorrectSpeechEvaluated:
                        isEnoughEnergyAndCorrectSpeechEvaluated = True
                        user = self.speaker_diar.isCorrectSpeaker(AudioData(b"".join(frames), SAMPLE_RATE, SAMPLE_WIDTH))
                        isEnoughEnergyAndCorrectSpeech = user is not None
                        if isEnoughEnergyAndCorrectSpeech:
                            speechStart = SpeechStart(datetime.now(), user, self.location)
                            if self.energy_debug: self.write(f'{self.name} {speechStart}')
                            self.onSpeechStart(speechStart) # invoke speech start handler

                # calculate silence length
                if isEnoughEnergyAndSpeech_buffer: pause_count = 1
                else: pause_count += 1

                # check if silence is too short - continue
                if pause_count < pause_buffer_count:
                    continue
                # silence marks end of speech - complete
                elif isEnoughEnergyAndCorrectSpeech:
                    for i in range(pause_count - non_speaking_buffer_count): frames.pop()  # remove extra non-speaking frames at the end
                    speech = Speech(speechStart.start, AudioData(b"".join(frames), SAMPLE_RATE, SAMPLE_WIDTH), datetime.now(), user, self.location)
                    if self.energy_debug: self.write(f'{self.name} {speech}')



                    # debug
                    # sometimes this audio stutters, as if every other frame was silence, always happens after 1st speech
                    # try:
                    #     # prepare data
                    #     from io import BytesIO
                    #     wav_stream = BytesIO(speech.audio.get_wav_data())
                    #     audio_array, sampling_rate = sf.read(wav_stream)
                    #     audio_array = audio_array.astype(np.float32)
                    #     # gather data as file
                    #     import uuid
                    #     file = str(uuid.uuid4()) + '.wav'
                    #     sf.write(file, audio_array, sampling_rate)
                    # except Exception as e:
                    #     print_exc()



                    return speech
                # silence marks end of non-speech - restart
                else:
                    if self.energy_debug: self.write(f'{self.name} end of the non-speech phrase {pause_buffer_count*seconds_per_buffer}s')  # end of the phrase
                    break


class Vad:
    def __init__(self, treshold: float, sample_rate: int):
        self.lock = Lock()
        self.treshold = treshold
        self.vad = SileroVoiceActivityDetectorWithCuda()
        if sample_rate != 16000: raise Exception("Sample rate for voice activity detection must be 16000")

    def is_voice(self, audio: bytes) -> bool:
        with self.lock:
            return self.vad(audio) >= self.treshold


@dataclass
class MicVoice:
    name: str
    data: object | None
    def __str__(self): return f'{self.name}'
    def __repr__(self): return f'{self.name}'


class MicVoiceDetectNone:
    def isCorrectSpeaker(self, audio_data: AudioData) -> str | None:
        return "User"

class MicVoiceDetectNvidia:
    def __init__(self, speaker_treshold: float, verbose: bool, device: str):
        self.lock = Lock()
        self.device = device
        self.sample_rate = 16000
        if self.sample_rate != 16000: raise Exception("Sample rate for voice activity detection must be 16000")
        self.speaker_treshold = speaker_treshold # cosine similarity score used as a threshold to distinguish two embeddings (default = 0.7)
        self.speaker_model = self.loadSpeakerModel()
        self.speakers_correct = self.loadSpeakersCorrect()
        self.verbose = verbose

    def loadSpeakerModel(self):
        try:
            m = nemo_asr.models.EncDecSpeakerLabelModel.from_pretrained("nvidia/speakerverification_en_titanet_large")
            m.to(torch.device(self.device))
            return m
        except Exception as e:
            print(f"ERR: failed to load speaker detector model {e}", end='')
            print_exc()
            return None

    def loadSpeakersCorrect(self) -> list[MicVoice] | None:
        if self.speaker_model is None:
            return None
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

    def isCorrectSpeaker(self, audio_data: AudioData) -> bool:
        # no model loaded
        if self.speaker_model is None: return True
        # no verified speakers set
        if self.speakers_correct is None: return True

        with self.lock:
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