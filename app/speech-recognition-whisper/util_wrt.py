from threading import Thread
from queue import Queue
from collections.abc import Iterator
from util_actor import Actor
from typing import TextIO, AnyStr, List
import sys

class Writer(Actor):

    def __init__(self):
        super().__init__("stdout", "Writer", "cpu", None, True)

        self.stdout = self.StdoutWrapper(self.queue)
        self.stdout.switchStdout()
        self.write = self
        self.write(f"RAW: {self.name} starting")

    def __call__(self, event: str | Iterator | object):
        self.queue.put(event)

    def _loop(self):

        def print(s, end):
            self.stdout.stdout.write(s)
            self.stdout.stdout.write(end)
            self.stdout.stdout.flush()

        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as event:
                    if isinstance(event, str):
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
        def __init__(self, queue: Queue):
            self.queue = queue
            self.stdout: TextIO = sys.stdout

        def switchStdout(self): sys.stdout = self
        def switchStdoutBack(self): sys.stdout = self.sout

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
            if len(s)>0: self.queue.put(s)
            return 0