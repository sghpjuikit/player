from util_llm import Llm, ChatProceed
from util_tts import Tts
from util_ctx import *
from imports import *

class Api:
    def __init__(self, llm: Llm, tts: Tts):
        self._llm = llm
        self._tts = tts

    def llm(self, e: ChatProceed, ctx: Ctx = CTX) -> Future[str]:
        return self._llm(e)

    def ttsSkippable(self, event: str, location: Location) -> Future[None]:
        return self._tts.skippable(event, location)

    def ttsPause(self, ms: int) -> Future[None]:
        return self._tts.speakPause(ms)

    def tts(self, event: str | Iterator, location: Location) -> Future[None]:
        return self._tts(event, location)