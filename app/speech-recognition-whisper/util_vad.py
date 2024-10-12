
from pysilero_vad import SileroVoiceActivityDetector
import onnxruntime
import os

class SileroVoiceActivityDetectorWithCuda:
    "Detects speech/silence using Silero VAD."

    def __init__(self):
        opts = onnxruntime.SessionOptions()
        opts.inter_op_num_threads = 1
        opts.intra_op_num_threads = 1

        self._silero = SileroVoiceActivityDetector()
        self._silero.session = onnxruntime.InferenceSession(
            str(os.path.join(os.path.dirname(__import__('pysilero_vad').__file__), 'models', "silero_vad.onnx")),
            providers=["CUDAExecutionProvider", "CPUExecutionProvider"],
            sess_options=opts
        )

    def reset(self) -> None:
        "Reset state."
        self._silero.reset()

    def __call__(self, audio: bytes) -> float:
        "Return probability of speech [0-1] in a single audio chunk. Audio *must* be 512 samples of 16Khz 16-bit mono PCM."
        return self._silero.__call__(audio)