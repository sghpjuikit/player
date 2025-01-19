from queue import Empty
from imports import *
import requests
import json

class Events:
    def __init__(self, address: str):
        self.address = address
        self.queue = Queue()

        if len(address)>0:
            def loop():
                while True:
                    try:
                        event = self.queue.get(block=True, timeout=0.2)
                        response = requests.post(self.address, headers = {'Content-Type': 'application/json'}, data=json.dumps(event))
                    except Empty:
                        continue
                    except Exception as e:
                        print(f'ERR: Events failed to process event={event} due error {e}')
                        print_exc()
                        continue

            Thread(target=loop, daemon=True).start()


    def __call__(self, event: object):
        if len(self.address)>0:
            self.queue.put(event)