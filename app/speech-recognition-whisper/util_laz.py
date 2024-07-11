import threading

class Lazy:
    def __init__(self, initializer):
        self.initializer = initializer
        self.value = None
        self.lock = threading.Lock()

    def __call__(self):
        if self.value is None:
            with self.lock:
                if self.value is None:
                    self.value = self.initializer()
        return self.value