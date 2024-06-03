
# reuquires to load model
from pysilero_vad import SileroVoiceActivityDetector


from pathlib import Path
from typing import Final, Union
import numpy as np
import onnxruntime
import os

_RATE: Final = 16000
_MAX_WAV: Final = 32767
_DEFAULT_ONNX_PATH = os.path.join(os.path.dirname(__import__('pysilero_vad').__file__), 'models', "silero_vad.onnx")


# this code is copied from silero-vad https://github.com/snakers4/silero-vad
# which does not support CUDA
#
# MIT License
#
# Copyright (c) 2020-present Silero Team
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
class SileroVoiceActivityDetectorWithCuda:
    "Detects speech/silence using Silero VAD."

    def __init__(self, onnx_path: Union[str, Path] = _DEFAULT_ONNX_PATH) -> None:
        onnx_path = str(onnx_path)
        onnx_providers = ["CUDAExecutionProvider", "CPUExecutionProvider"]
        opts = onnxruntime.SessionOptions()
        opts.inter_op_num_threads = 1
        opts.intra_op_num_threads = 1

        self.session = onnxruntime.InferenceSession(onnx_path, sess_options=opts, providers=onnx_providers)
        self.reset()

    def reset(self) -> None:
        """Reset state."""
        self._h = np.zeros((2, 1, 64)).astype("float32")
        self._c = np.zeros((2, 1, 64)).astype("float32")

    def __call__(self, audio: bytes) -> float:
        """Return probability of speech in audio [0-1].

        Audio must be 16Khz 16-bit mono PCM.
        """
        audio_array = np.frombuffer(audio, dtype=np.int16).astype(np.float32) / _MAX_WAV

        # Add batch dimension
        audio_array = np.expand_dims(audio_array, 0)

        ort_inputs = { "input": audio_array, "h": self._h, "c": self._c, "sr": np.array(_RATE, dtype=np.int64) }
        ort_outs = self.session.run(None, ort_inputs)
        out, self._h, self._c = ort_outs

        return out.squeeze()