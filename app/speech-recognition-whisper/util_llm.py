import gpt4all.gpt4all
from gpt4all import GPT4All  # https://docs.gpt4all.io/index.html
from gpt4all.gpt4all import empty_chat_session
from threading import Thread
from queue import Queue
from itertools import chain
from util_tty_engines import Tty
from util_write_engine import Writer
from util_itr import teeThreadSafe, teeThreadSafeEager


class ChatProceed:
    def __init__(self, sysPrompt: str, userPrompt: str | None):
        self.sysPrompt = sysPrompt
        self.userPrompt = userPrompt
        self.messages = [ ]
        self.messages.append({ "role": "system", "content": self.sysPrompt })
        if (userPrompt is not None): self.messages.append({ "role": "user", "content": self.userPrompt })

    @classmethod
    def start(cls, sysPrompt: str):
        return cls(sysPrompt, None)


class ChatIntentDetect(ChatProceed):
    def __init__(self, userPrompt: str):
        super.__init__(
            "From now on, identify user intent by returning one of following functions. " +
            "Only respond in format function: `COM-function-COM`. " +
            "Funs: " +
            "- open-weather-info " +
            "- play-music " +
            "- stop-music " +
            "- play-previous-song " +
            "- play-next-song " +
            "- unidentified // no other intent seems probable",
            userPrompt
        )


class Chat:
    def __init__(self, userPrompt: str):
        self.userPrompt = userPrompt


class ChatStart:
    def __init__(self):
        pass


class ChatStop:
    def __init__(self):
        pass


class LlmBase:
    def __init__(self):
        self._stop = False
        self.queue = Queue()
        self.generating = False

    def start(self):
        pass

    def stop(self):
        """
        Stop processing all elements and release all resources
        """
        self._stop = True

    def __call__(self, prompt: ChatStart | Chat | ChatProceed | ChatStop):
        self.queue.put(prompt)

    def _loop(self):
        pass


class LlmNone(LlmBase):
    def __init__(self, speak: Tty, write: Writer):
        super().__init__()
        self.write = write
        self.speak = speak


# home: https://github.com/nomic-ai/gpt4all
# doc https://docs.gpt4all.io/gpt4all_python.html
class LlmGpt4All(LlmBase):

    def __init__(self, model: str, speak: Tty, write: Writer, sysPrompt: str, maxTokens: int, temp: float, topp: float, topk: int):
        super().__init__()
        self.write = write
        self.speak = speak
        # gpt4all.gpt4all.DEFAULT_MODEL_DIRECTORY = chatDir
        self.sysPrompt = sysPrompt
        self.maxTokens = maxTokens
        self.temp = temp
        self.topp = topp
        self.topk = topk

    def start(self):
        Thread(name='LlmGpt4All', target=self._loop, daemon=True).start()

    def _loop(self):
        llm = None
        while not self._stop:
            e = self.queue.get()

            # load model lazily
            if llm is None: llm = GPT4All(model, allow_download=False)

            if isinstance(e, ChatStart):
                with llm.chat_session(self.sysPrompt):
                    while not self._stop:
                        t = self.queue.get()

                        if isinstance(t, ChatStart):
                            pass

                        if isinstance(t, ChatStop):
                            break

                        if isinstance(t, Chat):

                            def stop_on_token_callback(token_id, token_string):
                                return not self._stop and self.generating

                            # generate & stream response
                            self.generating = True
                            tokens = llm.generate(t.userPrompt, streaming=True, max_tokens=self.maxTokens, top_p=self.topp, top_k=self.topk, temp=self.temp, callback=stop_on_token_callback)
                            consumer, tokensWrite, tokensSpeech, tokensText = teeThreadSafeEager(tokens, 3)
                            self.write(chain(['CHAT: '], tokensWrite))
                            self.speak(tokensSpeech)
                            consumer()
                            text_all = ''.join(tokensText)
                            self.generating = False


# home https://github.com/openai/openai-python
# howto https://cookbook.openai.com/examples/how_to_stream_completions
class LlmHttpOpenAi(LlmBase):

    def __init__(self, url: str, bearer: str, modelName: str, speak: Tty, write: Writer, sysPrompt: str, maxTokens: int, temp: float, topp: float, topk: int):
        super().__init__()
        self.write = write
        self.speak = speak
        self.url = url
        self.bearer = bearer
        self.modelName = modelName
        self.sysPrompt = sysPrompt
        self.maxTokens = maxTokens
        self.temp = temp
        self.topp = topp
        self.topk = topk

    def start(self):
        Thread(name='LlmHttpOpenAi', target=self._loop, daemon=True).start()

    def _loop(self):
        try:
            from openai import OpenAI
            import openai
        except ImportError as e:
            self.write("OpenAi python module failed to load")
            return

        chat: ChatProceed | None = None
        client = OpenAI(api_key=self.bearer, base_url=self.url)

        while not self._stop:
            e = self.queue.get()

            if isinstance(e, ChatStart):
                chat = ChatProceed.start(self.sysPrompt)

            if isinstance(e, ChatStop):
                chat = None

            if isinstance(e, Chat | ChatProceed):
                try:
                    self.generating = True
                    isCommand = isinstance(e, ChatIntentDetect)

                    if isinstance(e, Chat):
                        if (chat is None): chat = ChatProceed.start(self.sysPrompt)
                        chat.messages.append({ "role": "user", "content": e.userPrompt })

                    def process():
                        messages = []
                        if isinstance(e, Chat): messages = chat.messages
                        if isinstance(e, ChatProceed): messages = e.messages

                        stream = client.chat.completions.create(model=self.modelName, messages=messages, max_tokens=self.maxTokens, temperature=self.temp, top_p=self.topp, stream=True)
                        try:
                            for chunk in stream:
                                if not self._stop and self.generating:
                                    if chunk.choices[0].delta.content is not None:
                                        yield chunk.choices[0].delta.content
                                else:
                                    break
                        finally:
                            stream.response.close()

                    consumer, tokensWrite, tokensSpeech, tokensText = teeThreadSafeEager(process(), 3)
                    if not isCommand: self.write(chain(['CHAT: '], tokensWrite))
                    if not isCommand: self.speak(tokensSpeech)
                    consumer()
                    text_all = ''.join(tokensText)

                    if isinstance(e, Chat):
                        chat.messages.append({ "role": "assistant", "content": text_all })

                    if isCommand:
                        command = text_all.strip().lstrip("COM-").rstrip("-COM").strip()
                        self.write('COM: ' + command.replace('-', ' '))

                    self.generating = False

                except openai.APIConnectionError as e:
                    self.write("OpenAI server could not be reached")
                    self.write(e.__cause__)
                except openai.RateLimitError as e:
                    self.write("OpenAI returned 429 status code - rate limit error")
                except openai.APIStatusError as e:
                    self.write(f"OpenAI returned non {e.status_code} status code")
                    self.write(e.response)
