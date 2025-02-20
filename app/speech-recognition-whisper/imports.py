from contextlib import contextmanager
from concurrent.futures import Future
from collections.abc import Iterator
from dataclasses import dataclass, astuple
from typing import Callable, Iterator, cast
from threading import Thread
from queue import Queue
from util_ctx import *
import traceback
import sys

def print_exc():
    traceback.print_exc(file=sys.stdout)
    traceback.print_exc(file=sys.stderr)