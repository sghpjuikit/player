from threading import Thread
from queue import Queue
from collections.abc import Iterator


class Writer:
    def __init__(self):
        self.queue = Queue()
        self._stop = False
        Thread(name='Writer', target=self._loop, daemon=True).start()

    def __call__(self, event: str | Iterator | object):
        self.queue.put(event)

    def _loop(self):
        while not self._stop:
            event = self.queue.get()
            if isinstance(event, str):
                print(event.replace('\n', '\u2028'), end='\n', flush=True)
            elif isinstance(event, Iterator):
                for eventPart in event:
                    print(eventPart.replace('\n', '\u2028'), end='', flush=True)
                print('', end='\n', flush=True)
            else:
                print(event, end='\n', flush=True)

    def stop(self):
        self._stop = True