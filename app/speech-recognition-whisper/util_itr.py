from imports import *
from threading import Lock
from itertools import tee, chain as ichain
from time import sleep
from stream2sentence import generate_sentences

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
            try:
                for item in iterable:
                    self.started = True
                    for queue in queues:
                        queue.put(item)
            except Exception as e:
                raise e
            finally:
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


def progress():
    """Returns the iterator with progress indicator elements"""
    bs = 0
    while True:
        bs = bs + 1
        if bs % 5 == 1: yield '.  '
        if bs % 5 == 2: yield '.. '
        if bs % 5 == 3: yield '...'
        if bs % 5 == 4: yield ' ..'
        if bs % 5 == 0: yield '  .'

def words(text: str):
    words = iter(text.split(' '))
    yield next(words)
    for element in words:
        yield ' '
        yield element

def sentences(input_generator: Iterator[str]):
    return generate_sentences(input_generator, cleanup_text_links=True, cleanup_text_emojis=True)

def lines(input_generator):
    """
    Takes a generator of strings and returns a generator of lines,
    accumulating text until a '\n' is found, then yielding the line.
    Repeats this process until all lines are returned.
    """
    return chunks(input_generator, '\n')

def chunks(input_generator, separator: str):
    """
    Takes a generator of strings and returns a generator of strings,
    accumulating text until a separator is found, then yielding the text chunk.
    Repeats this process until all chunks are returned.
    """
    chunk = ""
    for chunk_part in input_generator:
        chunk += chunk_part
        while separator in chunk:
            i = chunk.find(separator)
            yield chunk[:i]
            chunk = chunk[i+1:]
    if chunk:
        yield chunk

def python_code_chunks(input_generator):
    """
    Takes a generator of strings representing python code and returns a generator of executable python chunks,
    accumulating text until an executable python code chunk is complete, then yielding it.
    Repeats this process until all executable python code chunks are returned.

    If the input python code is invalid, it will still be returned in chunks, but some may be affected.
    Usually, the affected line of code stays invalid, but unterminated strings may corrupt all chunks.
    """
    current_chunk = ""
    char_last = None
    in_string = False
    string_quote = None

    for token in input_generator:
        for char in token:
            if in_string:
                current_chunk += char
                if current_chunk[-3:]==string_quote:
                    in_string = False
            else:
                if char_last == "\n" and char != " ":
                    yield current_chunk.rstrip("\n")
                    current_chunk = ""
                current_chunk += char
                if char in ['"', "'"] and current_chunk[-3:] in ['"""', "'''"]:
                    in_string = True
                    string_quote = current_chunk[-3:]

            char_last = char

    if current_chunk:
        yield current_chunk.rstrip("\n")

def vec(sentence: str):
    from nomic import embed
    import numpy as np
    output = embed.text(
        texts=[
            'Nomic Embed now supports local and dynamic inference to save you inference latency and cost!',
            'Hey Nomic, why don\'t you release a multimodal model soon?',
        ],
        model='nomic-embed-text-v1.5',
        task_type="search_document",
        inference_mode='local',
        dimensionality=768,
    )

    print(output['usage'])

    embeddings = np.array(output['embeddings'])

    print(embeddings.shape)

def get(query: str) -> None:
    if query in memory:
        embed_model = NomicEmbedding(
            model_name='nomic-embed-text-v1.5',
            inference_mode='local',
            device='gpu',
        )
        result = embed_model.get_text_embedding(query)
        # perform cosine similarity search on the stored vectors
        scores = []
        for text, vector in memory.items():
            score = cosine_similarity(result, vector)[0][0]
            scores.append((text, score))
        scores.sort(key=lambda x: x[1], reverse=True)
        top_results = [x[0] for x in scores[:10]]
        speak(f"Top 10 similar results: {', '.join(top_results)}")
    else:
        speak(f"No embedding found for '{query}'")

def set(text: str) -> None:
    embed_model = NomicEmbedding(
        model_name='nomic-embed-text-v1.5',
        inference_mode='local',
        device='gpu',
    )
    result = embed_model.get_text_embedding(text)
    memory[text] = result
    speak(f"Embedded '{text}' and stored it in the RAG.")

# vec('')
# data = index_md_file('README-ASSISTANT.md')
# data_rag = {}
# for header, text in data.items():
#     data_rag[header] = {}
#     data_rag[header]