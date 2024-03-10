from threading import Thread
from queue import Queue
from collections.abc import Iterator
from util_actor import Actor


class Writer(Actor):
    def __init__(self):
        super().__init__("sdout", "Writer", True)

    def __call__(self, event: str | Iterator | object):
        self.queue.put(event)

    def _loop(self):
        self._loaded = True
        while not self._stop:
            event = self.queue.get()
            self.events_processed += 1
            if isinstance(event, str):
                print(event.replace('\n', '\u2028'), end='\n', flush=True)
            elif isinstance(event, Iterator):
                for eventPart in event:
                    print(eventPart.replace('\n', '\u2028'), end='', flush=True)
                print('', end='\n', flush=True)
            else:
                print(event, end='\n', flush=True)

        self._clear_queue()

    def stop(self):
        self._stop = True