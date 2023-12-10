import gpt4all.gpt4all
from gpt4all import GPT4All  # https://docs.gpt4all.io/index.html
from gpt4all.gpt4all import empty_chat_session
from threading import Thread
from queue import Queue
from itertools import chain
from util_tty_engines import Tty
from util_write_engine import Writer
from util_itr import teeThreadSafe, teeThreadSafeEager

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
        self.listening_for_chat_prompt = False
        self.listening_for_chat_generation = False
        self.generating = False

    def start(self):
        pass

    def stop(self):
        """
        Stop processing all elements and release all resources
        """
        self._stop = True

    def __call__(self, prompt: str):
        self.queue.put(prompt)

    def _loop(self):
        pass

    def chatStart(self):
        self.queue.put(ChatStart())

    def chatStop(self):
        self.queue.put(ChatStop())


class LlmNone(LlmBase):
    def __init__(self, ):
        super().__init__()
        pass


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

                        if isinstance(t, ChatStop):
                            break

                        if isinstance(t, ChatStart):
                            pass

                        if isinstance(t, str):

                            def stop_on_token_callback(token_id, token_string):
                                return not self._stop and self.listening_for_chat_generation

                            # generate & stream response
                            self.listening_for_chat_generation = True
                            self.generating = True
                            tokens = llm.generate(t, streaming=True, max_tokens=self.maxTokens, top_p=self.topp, top_k=self.topk, temp=self.temp, callback=stop_on_token_callback)
                            consumer, tokensWrite, tokensSpeech, tokensText = teeThreadSafeEager(tokens, 3)
                            self.write(chain(['CHAT: '], tokensWrite))
                            self.speak(tokensSpeech)
                            consumer()
                            text_all = ''.join(tokensText)
                            self.generating = False

                            # end LLM conversation (if cancelled by user)
                            self.listening_for_chat_generation = False
                            if self.listening_for_chat_prompt is False:
                                self.speak.skip()
                                self.speak("Ok")
                                break


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

        client = OpenAI(api_key=self.bearer, base_url=self.url)

        while not self._stop:
            e = self.queue.get()
            if isinstance(e, ChatStart):
                while not self._stop:
                    t = self.queue.get()

                    if isinstance(t, ChatStop):
                        messages = [ ]
                        break

                    if isinstance(t, ChatStart):
                        message1 = { "role": "system", "content": self.sysPrompt }
                        messages = [ message1 ]
                        pass

                    if isinstance(t, str):
                        try:
                            self.listening_for_chat_generation = True
                            self.generating = True
                            messages.append({ "role": "user", "content": t })

                            def process():
                                stream = client.chat.completions.create(model=self.modelName, messages=messages, max_tokens=self.maxTokens, temperature=self.temp, top_p=self.topp, stream=True)
                                try:
                                    for chunk in stream:
                                        if not self._stop and self.listening_for_chat_generation:
                                            if chunk.choices[0].delta.content is not None:
                                                yield chunk.choices[0].delta.content
                                        else:
                                            break
                                finally:
                                    stream.response.close()

                            consumer, tokensWrite, tokensSpeech, tokensText = teeThreadSafeEager(process(), 3)
                            self.write(chain(['CHAT: '], tokensWrite))
                            self.speak(tokensSpeech)
                            consumer()
                            text_all = ''.join(tokensText)
                            messages.append({ "role": "assistant", "content": text_all })
                            self.generating = False

                            # end LLM conversation (if cancelled by user)
                            self.listening_for_chat_generation = False
                            if self.listening_for_chat_prompt is False:
                                self.speak.skip()
                                self.speak("Ok")
                                break

                        except openai.APIConnectionError as e:
                            self.write("OpenAI server could not be reached")
                            self.write(e.__cause__)
                        except openai.RateLimitError as e:
                            self.write("OpenAI returned 429 status code - rate limit error")
                        except openai.APIStatusError as e:
                            self.write(f"OpenAI returned non {e.status_code} status code")
                            self.write(e.response)
