import sounddevice as sd
import numpy as np
import scipy.io.wavfile as wavfile
import os

# recommended assistant input
CHANNELS = 1
RATE = 16000
DURATION = 20
OUTPUT_DIR = 'voices-verified'
WAVE_OUTPUT_FILENAME = os.path.join('voices-verified', f"{in_name}.wav")

# Record audio
duration = DURATION  # Duration in seconds
recording_length = int(DURATION * RATE)
with sd.InputStream(samplerate=RATE, channels=CHANNELS, dtype='int16') as stream:
    data, overflowed = stream.read(recording_length)

# Save the recorded audio to a WAV file
wavfile.write(WAVE_OUTPUT_FILENAME, RATE, data.astype(np.int16))
