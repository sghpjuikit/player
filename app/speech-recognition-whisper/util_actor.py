
from queue import Queue, Empty
from threading import Thread
from contextlib import contextmanager
import time
import traceback

class ActorStoppedException(Exception):
    pass

class Event:
    def str(self): return str(this)

class Actor:

    def __init__(self, group: str, name: str, deviceName: str | None, write, enabled: bool):
        self.group = group
        self.name = name
        self.deviceName = deviceName
        self.write = write
        self.queue = Queue()
        self.events_processed: int = 0
        self._stop: bool = False
        self._loaded: bool = False
        self.enabled: bool = enabled
        self.processing = False
        self.processing_event = None
        self.processing_start = None
        self.processing_stop = None
        self.processing_times = []
        self.processing_times_last_len = 0
        self.processing_time = None
        self.processing_time_avg = None

    def queued(self) -> list:
        """
        Returns all currently queued events as list
        """
        return list(self.queue.queue)

    def _clear_queue(self):
        """
        Clears all currently queued events..
        """
        while not self.queue.empty(): self.queue.get()

    def start(self):
        """
        Start processing all future elements in queue, on newly started thread. Asynchronous
        """
        Thread(name=self.name, target=self._loop, daemon=True).start()

    def stop(self):
        """
        Stop processing all elements, stop the loop,, release all resources and end thread when done. Asynchronous
        """
        self._stop = True

    def _loop(self):
        """
        Loop that loads necessary resources, loops events until stop is called and releases resources.
        Override in implementation.
        """
        pass

    @contextmanager
    def _looping(self, set_loading: bool = True):
        """
        Convenience method around loop
        """
        try:
            if set_loading: self._loaded = True
            yield
            self._clear_queue()
        except Exception as e:
            self._clear_queue()
            # interrupting thread while in context manager while stopping thorws 'generator didn't yield'
            if isinstance(e, RuntimeError) and str(e) == "generator didn't yield" and self._stop: pass
            else: raise e

    @contextmanager
    def _loopProcessEvent(self):
        """
        Convenience method around single event processing in the loop
        """
        try:
            # if self._stop or not self.enabled: return
            # try: event = self.queue.get(timeout=0.1)
            # except Empty: return
            # if self._stop or not self.enabled: return

            if self._stop or not self.enabled: return
            event = self.queue.get()
            if self._stop or not self.enabled: return

            self.events_processed += 1
            self.processing_event = event
            self.processing = True

            self.processing_start = time.time()
            yield event
            self.processing_stop = time.time()

            self.processing_time = self.processing_stop - self.processing_start
            self.processing_times.append(self.processing_time)
            self.processing_time_avg = sum(self.processing_times) / len(self.processing_times)

            self.processing_stop = None
            self.processing_start = None
            self.processing = False
            self.processing_event = None
        except Exception as e:
            self.processing_stop = None
            self.processing_start = None
            self.processing = False
            self.processing_event = None
            if not isinstance(e, ActorStoppedException): self.write("ERR: Error occurred:" + str(e))
            if not isinstance(e, ActorStoppedException): traceback.print_exc()

    def _loopLoadAndIgnoreEvents(self):
        """
        Convenience method that loops and ignores events until stop is called. Use for noop implementations.
        """
        self._loaded = True
        while not self._stop:
            time.sleep(0.1)
            self._clear_queue()
        self._clear_queue()

    def processingTimeLast(self) -> float | None:
        # capture values to prevent mutation
        last = self.processing_time
        start = self.processing_start
        stop = self.processing_stop
        # no time
        if start is None: return last
        # ongoing time
        if stop is None: return time.time() - self.processing_start
        # last time
        else: return v - start

    def processingTimeAvg(self) -> float | None:
        # capture values to prevent mutation
        times = self.processing_times
        times_len = len(times)
        last_len = self.processing_times_last_len
        avg = self.processing_time_avg
        # no avg
        if times_len==0:
            return None
        # last computed avg
        if times_len==last_len:
            return avg
        # recompute avg
        else:
            self.processing_times_last_len = times_len
            self.processing_time_avg = sum(times) / times_len
            return self.processing_time_avg

    def state(self) -> str:
        if self._stop: return "STOPPED"
        if not self.enabled: return "PAUSED"
        if not self._loaded: return "STARTING"
        else: return "ACTIVE"