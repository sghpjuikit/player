
from queue import Queue
from threading import Thread

class Actor:

    def __init__(self, group: str, name: str, enabled: bool):
        self.group = group
        self.name = name
        self.queue = Queue()
        self.events_processed: int = 0
        self._stop: bool = False
        self._loaded: bool = False
        self.enabled: bool = enabled

    def queued(self) -> int:
        return list(self.queue.queue)

    def _clear_queue(self):
        while not self.queue.empty(): self.queue.get()

    def start(self):
        Thread(name=self.name, target=self._loop, daemon=True).start()

    def _loop(self):
        pass

    def _loopLoadAndIgnoreEvents(self):
        self._loaded = True
        while not self._stop:
            sleep(0.1)
            self._clear_queue()
        self._clear_queue()

    def stop(self):
        """
        Stop processing all elements, release all resources and end thread when done. Asynchronous
        """
        self._stop = True

    def state(self) -> str:
        if self._stop: return "STOPPED"
        if not self.enabled: return "PAUSED"
        if not self._loaded: return "STARTING"
        else: return "ACTIVE"