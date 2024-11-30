from util_events import *
from util_llm import Llm, ChatProceed
from util_tts import Tts
from util_ctx import *
from imports import *

class Api:
    def __init__(self, events: Events, llm: Llm, tts: Tts):
        self._llm = llm
        self._tts = tts
        self.events = events

    def llm(self, e: ChatProceed, ctx: Ctx = CTX, stream = False) -> Future[str | Iterator[str]]:
        return self._llm(e, ctx = ctx, stream=stream)

    def ttsSkippable(self, event: str, location: Location) -> Future[None]:
        return self._tts.skippable(event, location)

    def ttsPause(self, ms: int) -> Future[None]:
        return self._tts.speakPause(ms)

    def tts(self, event: str | Iterator, location: Location) -> Future[None]:
        return self._tts(event, location)