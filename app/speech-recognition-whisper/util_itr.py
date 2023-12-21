from threading import Lock
from queue import Queue
from itertools import tee
import time

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
            if item is sentinel:
                break
            else:
                yield item

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

def chain(iterator1, iterator2):
    """Returns iterator that iterates all elements of the 1st iterator and then all elements of the 2nd iterator"""
    yield from iterator1
    yield from iterator2


def progress(consumer, iterator):
    """Returns the specified iterator with prepended elements for progress until first element is evaluated"""
    yield '   '
    bs = 0
    while not consumer.hasStarted():
        time.sleep(0.125)
        bs = bs + 1
        yield '\b\b\b'
        if bs % 5 == 1: yield '.  '
        if bs % 5 == 2: yield '.. '
        if bs % 5 == 3: yield '...'
        if bs % 5 == 4: yield ' ..'
        if bs % 5 == 0: yield '  .'

    yield '\b\b\b'
    yield from iterator
