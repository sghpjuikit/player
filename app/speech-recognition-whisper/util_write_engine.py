
import os
import threading
import asyncio
import queue


class Writer:
    def __init__(self):
        self.event_queue = queue.Queue()
        self.parts_queue = queue.Queue()
        threading.Thread(target=self.process_event, daemon=False).start()

    def __call__(self, event: str):
        self.event_queue.put(event)

    def iterableStart(self):
        self.event_queue.put(None)

    def iterablePart(self, eventPart: str):
        self.parts_queue.put(eventPart)

    def iterableEnd(self):
        self.parts_queue.put(None)

    def process_event(self):
        while True:
            event = self.event_queue.get()
            if event is None:
                is_iterable = True
                while is_iterable:
                    eventPart = self.parts_queue.get()
                    if eventPart is None:
                        is_iterable = False
                    else:
                        print(eventPart, end='', flush=False)
                print('\n', end='', flush=True)
            else:
                print(event, end='\n', flush=True)