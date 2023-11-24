from threading import Thread
from queue import Queue
from collections.abc import Iterator


class Writer:
    def __init__(self):
        self.event_queue = Queue()
        Thread(target=self.loop, daemon=True).start()

    def __call__(self, event: str):
        self.event_queue.put(event)

    def loop(self):
        while True:
            event = self.event_queue.get()
            if isinstance(event, str):
                print(event, end='\n', flush=True)
            elif isinstance(event, Iterator):
                for eventPart in event:
                    print(eventPart.replace('\n', '\u2028'), end='', flush=True)
                print('', end='\n', flush=True)
