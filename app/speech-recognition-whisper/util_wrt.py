from threading import Thread
from queue import Queue
from collections.abc import Iterator
from util_actor import Actor


class Writer(Actor):
    def __init__(self):
        super().__init__("stdout", "Writer", "cpu", None, True)
        self.write = self

    def __call__(self, event: str | Iterator | object):
        self.queue.put(event)

    def _loop(self):
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as event:
                    if isinstance(event, str):
                        print(event.replace('\n', '\u2028'), end='\n', flush=True)
                    elif isinstance(event, Iterator):
                        for eventPart in event:
                            print(eventPart.replace('\n', '\u2028'), end='', flush=True)
                        print('', end='\n', flush=True)
                    else:
                        print(event, end='\n', flush=True)
