from concurrent.futures import Future

def complete_also(future2: Future):
    """
    Returns on done callback that completes the future passed as argument when the original future completes. Usage:
    future.add_done_callback(complete_also(future2))
    """

    def copy_result(future1):
        if future1.exception() is not None:
            future2.set_exception(future1.exception())
        else:
            future2.set_result(future1.result())

    return copy_result  # Return the callback function


def flatMap(f: Future, func) -> Future:
    "Ordinary flatMap. Be aware that mapper executes on thread the original future completes on."
    
    rf = Future()
    def callback(fut):
        try:
            if fut.exception() is not None:
                rf.set_exception(fut.exception())
            else:
                mapped = func(fut.result())
                if isinstance(mapped, Future): futureOnDone(mapped, complete_also(rf))
                else: rf.set_exception(ValueError('Function must return a Future object'))
        except Exception as e:
            rf.set_exception(e)

    futureOnDone(f, callback)
    return rf


def futureCompleted(result) -> Future:
    "Returns future completed with the result."
    
    future = Future()
    future.set_result(result)
    return future

def futureFailed(exception: Exception) -> Future:
    "Returns future failed with the exception."

    future = Future()
    future.set_exception(exception)
    return future

def futureOnDone(future, onDone):
    if future.done(): onDone(future)
    else: future.add_done_callback(onDone)