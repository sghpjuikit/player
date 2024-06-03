from typing import Callable
from time import sleep

def wait_until(period_s: float, test: Callable[[], bool]):
        while True:
            if test(): break
            else: sleep(0.1)