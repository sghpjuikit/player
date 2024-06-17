from typing import Callable
from time import sleep

def wait_until(test: Callable[[], bool]):
    'Waits forever. Returns if condition, tested every 0.1s, gives True.'
    while True:
        if test(): break
        else: sleep(0.1)

def wait_loop_exp(period: float, base: float, i: int, test: Callable[[], bool]):
    'Waits for period time with exponential backoff, i.e.: period * (base**i). Returns if condition, tested every 0.1s, gives True.'
    'Waits until a given condition is true'
    t = period * (base**i)
    c = 0
    while True:
        if c>=t: return
        if test(): return
        c += 0.1
        sleep(0.1)