from imports import *
from threading import Lock
from itertools import tee, chain as ichain
from time import sleep

def teeThreadSafeEager(iterable, n=2):
    """
    Tuple of n independent thread-safe iterators backed by queues.
    The returned iterables are independent and lock-free
    The first argument is callable that consumes the specified iterator eagely.
    The source iterable is consumed on current thread at maximum speed.
    """

    queues = [Queue() for _ in range(n)]

    sentinel = object()  # Sentinel value to indicate the end of iteration

    def generator(q):
        while True:
            item = q.get()
            if item is sentinel: break
            else: yield item

    # Create an additional iterator to consume elements on the current thread
    class ConsumeIterator:
        def __init__(self):
            self.started = False

        def hasStarted(self):
            return self.started

        def __call__(self):
            for item in iterable:
                self.started = True
                for queue in queues:
                    queue.put(item)
            self.started = True
            for queue in queues:
                queue.put(sentinel)  # Put the sentinel value into the queues

    iterators = [ConsumeIterator()] + [generator(queue) for queue in queues]

    return tuple(iterators)


def teeThreadSafe(iterable, n=2):
    """Tuple of n dependent thread-safe iterators, using lock"""

    class safeteeobject(object):
        def __init__(self, teeobj, lock):
            self.teeobj = teeobj
            self.lock = lock
        def __iter__(self):
            return self
        def __next__(self):
            with self.lock:
                return next(self.teeobj)
        def __copy__(self):
            return safeteeobject(self.teeobj.__copy__(), self.lock)

    lock = Lock()
    return tuple(safeteeobject(teeobj, lock) for teeobj in tee(iterable, n))


def chain(*iterators):
    """Returns an iterator that iterates all elements of all iterators in order"""
    return ichain(*iterators)


def progress(iterator_has_started: Callable[[], bool], iterator):
    """Returns the specified iterator with prepended elements for progress until first element is evaluated"""
    bs = 0
    while not iterator_has_started():
        for _ in range(10):
            if not iterator_has_started(): sleep(0.0125)
            else: break
        if bs>0: yield '\b\b\b'
        bs = bs + 1
        if bs % 5 == 1: yield '.  '
        if bs % 5 == 2: yield '.. '
        if bs % 5 == 3: yield '...'
        if bs % 5 == 4: yield ' ..'
        if bs % 5 == 0: yield '  .'

    if bs>0: yield '\b\b\b'
    yield from iterator


def words(text: str):
    words = iter(text.split(' '))
    yield next(words)
    for element in words:
        yield ' '
        yield element