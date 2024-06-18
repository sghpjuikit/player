from contextlib import contextmanager
from concurrent.futures import Future
from dataclasses import dataclass
from typing import Callable, cast
from threading import Thread
from queue import Queue
from util_ctx import *
import traceback
import sys

def print_exc():
    traceback.print_exc(file=sys.stdout)
    traceback.print_exc(file=sys.stderr)