import sys
from dataclasses import dataclass
from concurrent.futures import Future
from typing import Callable, cast
from queue import Queue
from threading import Thread
import traceback

def print_exc():
    traceback.print_exc(file=sys.stdout)
    traceback.print_exc(file=sys.stderr)