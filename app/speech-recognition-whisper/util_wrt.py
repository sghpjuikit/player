from typing import TextIO, AnyStr, List
from collections.abc import Iterator
from util_actor import Actor
from imports import *
from util_itr import progress
from util_now import wait_until
from time import sleep
from threading import Lock
import threading
import sys
import io

class Writer(Actor):
    suppress = threading.local()

    def __init__(self):
        super().__init__("stdout", "Writer", "cpu", None, True)

        self.stdout = self.StdoutWrapper(self)
        self.stdout.switchStdout()
        self.write = self
        self.write(f"RAW: {self.name} starting")
        self.busy_status = set()
        self._busy_lock = Lock()
        self._print_lock = Lock()
        self._progress_suffix = ''
        self._last_progress_suffix = ''
        self._last_progress_active = False

    def __call__(self, event: str | Iterator | object):
        if not getattr(self.suppress, 'var', False):
            self.queue.put(event)

    @contextmanager
    def active(self):
        'yield action and print progress indicator until it is done'
        try:
            id = object()
            with self._busy_lock: self.busy_status.add(id)
            yield
        finally:
            with self._busy_lock: self.busy_status.remove(id)

    @contextmanager
    def suppressed(self):
        'yield action and avoid logging on current thread until it is done'
        try:
            self.suppress.var = True
            yield
        finally:
            self.suppress.var = False

    def suppressOutput(self, suppress: bool):
        self.suppress.var = suppress

    def start(self):
        super().start()
        Thread(target=self._loop_progress, daemon=True).start()

    def stop(self):
        super().stop()
        self.stdout.switchStdoutBack()

    def _print(self, s, end):
        with self._print_lock:
            ps = self._progress_suffix
            active = len(ps)>0

            # notify activity started
            if self._last_progress_active!=active and active:
                self.stdout.stdout.write('COM: System::activity-start')
                self.stdout.stdout.flush()

            self.stdout.stdout.write('\033[1;32m' + '\b'*(len(self._last_progress_suffix)+len('\033[1;32m\033[0m')) + '\033[0m')

            # notify activity ended
            if self._last_progress_active!=active and not active:
                self.stdout.stdout.write('COM: System::activity-stop')
                self.stdout.stdout.flush()

            self.stdout.stdout.write(s + end + '\033[1;32m' + ps + '\033[0m')
            self.stdout.stdout.flush()
            self._last_progress_suffix = ps
            self._last_progress_active = active

    def _loop_progress(self):
        # generate progress suffixes
        progress_suffixes = progress()
        # update progress regularly
        while not self._stop:
            # update progress suffix
            if self.busy_status:
                try: self._progress_suffix = next(progress_suffixes)
                except StopIteration: break
            else:
                self._progress_suffix = ''
            # update progress
            if self._last_progress_suffix!=self._progress_suffix: self._print('', end='')
            # next cycle
            sleep(0.125)
        # clear progress
        self._progress_suffix = ''
        self._print('', end='')

    def _loop(self):
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as event:
                    if event is None:
                        break
                    elif isinstance(event, str):
                        self._print(event.replace('\n', '\u2028'), end='\n')
                    elif isinstance(event, Iterator):
                        for eventPart in event: self._print(eventPart.replace('\n', '\u2028'), end='')
                        self._print('', end='\n')
                    else:
                        self._print(str(event), end='\n')


    class StdoutWrapper:
        def __init__(self, queue):
            self.queue = queue
            self.stdout: TextIO = sys.stdout

        def switchStdout(self):
            self.sout = sys.stdout
            sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
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