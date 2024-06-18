from typing import TextIO, AnyStr, List
from collections.abc import Iterator
from util_actor import Actor
from imports import *
from util_itr import progress
from util_now import wait_until
from time import sleep
import threading
import sys

class Writer(Actor):
    suppress = threading.local()

    def __init__(self):
        super().__init__("stdout", "Writer", "cpu", None, True)

        self.stdout = self.StdoutWrapper(self)
        self.stdout.switchStdout()
        self.write = self
        self.write(f"RAW: {self.name} starting")
        self.busy_status = set()
        self._progress_suffix = ''
        self._last_progress_suffix = ''

    def __call__(self, event: str | Iterator | object):
        if not getattr(self.suppress, 'var', False):
            self.queue.put(event)

    @contextmanager
    def active(self):
        try:
            id = object()
            self.busy_status.add(id)
            yield
        finally:
            self.busy_status.remove(id)

    @contextmanager
    def suppressed(self):
        try:
            self.suppress.var = True
            yield
        finally:
            self.suppress.var = False

    def suppressOutput(self, suppress: bool):
        self.suppress.var = suppress

    def start(self):
        super().start()
        Thread(target=self._progress_loop, daemon=True).start()

    def _progress_loop(self):
        progress_bar = progress()
        while True:
            if self.busy_status:
                try: self._progress_suffix = next(progress_bar)
                except StopIteration: break
            else:
                self._progress_suffix = ''
            sleep(0.125)

    def _loop(self):

        def print(s, end):
            ps = self._progress_suffix
            self.stdout.stdout.write('\b'*len(self._last_progress_suffix) + s + end + ps)
            self.stdout.stdout.flush()
            self._last_progress_suffix = ps

        with self._looping():
            progress_bar = progress()

            while not self._stop:
                wait_until(lambda: self.busy_status or not self.queue.empty())

                if self.queue.empty():
                    print('', '')
                    while self.busy_status and self.queue.empty():
                        try: print('', '')
                        except StopIteration: break
                        sleep(0.125)
                    print('', '')

                with self._loopProcessEvent() as event:
                    if event is None:
                        break
                    elif isinstance(event, str):
                        print(event.replace('\n', '\u2028'), end='\n')
                    elif isinstance(event, Iterator):
                        for eventPart in event:
                            print(eventPart.replace('\n', '\u2028'), end='')
                        print('', end='\n')
                    else:
                        print(str(event), end='\n')


    def stop(self):
        self.stdout.switchStdoutBack()

    class StdoutWrapper:
        def __init__(self, queue):
            self.queue = queue
            self.stdout: TextIO = sys.stdout

        def switchStdout(self):
            self.sout = sys.stdout
            sys.stdout = self
        def switchStdoutBack(self):
            sys.stdout = self.sout
            del self.sout

        def close(self): pass
        def detach(self): pass
        def fileno(self): return 0
        def flush(self): self.stdout.flush()
        def isatty(self): return False
        def read(self, n: int = -1) -> AnyStr: return ''
        def readable(self) -> bool: return False
        def readline(self, limit: int = -1) -> AnyStr: return ''
        def readlines(self, hint: int = -1) -> List[AnyStr]: return []
        def seek(self, offset: int, whence: int = 0) -> int: pass
        def seekable(self) -> bool: return False
        def tell(self) -> int: return 0
        def truncate(self, size: int = None) -> int: pass
        def writable(self): return True
        def writelines(self, lines: List[AnyStr]) -> None: self.queue(iter(lines))
        def write(self, s: AnyStr) -> int:
            s = s.decode(self.encoding) if isinstance(s, bytes) else s
            if len(s)>0: self.queue(s)
            return 0