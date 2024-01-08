import gpt4all.gpt4all
from gpt4all import GPT4All  # https://docs.gpt4all.io/index.html
from gpt4all.gpt4all import empty_chat_session
from threading import Thread
from queue import Queue
from typing import Callable
from util_tty_engines import Tty
from util_write_engine import Writer
from util_itr import teeThreadSafe, teeThreadSafeEager, progress, chain, SingleLazyIterator
from util_paste import pasteTokens

class ChatProceed:
    def __init__(self, sysPrompt: str, userPrompt: str | None):
        self.outStart = 'CHAT '
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
        super().__init__(
            "From now on, identify user intent by returning one of following commands. " +
            "Only respond with command in format : `COM-command-COM`. $ is command parameter." +
            "Commands: \n" +
            "- repeat // last speech\n" +
            "- open-weather-info\n" +
            "- play-music\n" +
            "- stop-music\n" +
            "- play-previous-song\n" +
            "- play-next-song\n" +
            "- what-time-is-it\n" +
            "- what-date-is-it\n" +
            "- list-light-scenes\n" +
            "- generate from? clipboard\n" +
            "- speak|say from? clipboard\n" +
            "- speak|say $text\n" +
            "- lights-on?/off?\n" +
            "- lights-scene-$scene-name\n" +
            "- unidentified // no other command probable",
            userPrompt
        )
        self.outStart = 'COM-DET: '

class ChatPaste(ChatProceed):
    def __init__(self, userPrompt: str):
        super().__init__(
            "From now on, seamlessly complete user messages. Only complete the message so it connects with user's.",
            userPrompt
        )
        self.outStart = 'PASTE: '

class Chat:
    def __init__(self, userPrompt: str):
        self.outStart = 'CHAT: '
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
                            # generate & stream response
                            try:
                                self.generating = True
                                tokens = llm.generate(
                                    t.userPrompt, streaming=True,
                                    max_tokens=self.maxTokens, top_p=self.topp, top_k=self.topk, temp=self.temp,
                                    callback=lambda token_id, token_string: not self._stop and self.generating
                                )
                                consumer, tokensWrite, tokensSpeech, tokensText = teeThreadSafeEager(tokens, 3)
                                self.write(chain(['CHAT: '], progress(consumer.hasStarted, tokensWrite)))
                                self.speak(tokensSpeech)
                                consumer()
                                text_all = ''.join(tokensText)
                            finally:
                                self.generating = False


# home https://github.com/openai/openai-python
# howto https://cookbook.openai.com/examples/how_to_stream_completions
class LlmHttpOpenAi(LlmBase):

    def __init__(self, url: str, bearer: str, modelName: str, speak: Tty, write: Writer, commandExecutor: Callable[[str], str], sysPrompt: str, maxTokens: int, temp: float, topp: float, topk: int):
        super().__init__()
        self.write = write
        self.speak = speak
        self.commandExecutor = commandExecutor
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
            from httpx import Timeout
            import openai
        except ImportError as e:
            self.write("OpenAi python module failed to load")
            return

        chat: ChatProceed | None = None
        client = OpenAI(api_key=self.bearer, base_url=self.url)

        while not self._stop:
            e = self.queue.get()

            if isinstance(e, ChatStart):
                if chat is not None:
                    chat = ChatProceed.start(self.sysPrompt)

            if isinstance(e, ChatStop):
                chat = None

            if isinstance(e, Chat | ChatProceed):
                try:
                    isCommand = isinstance(e, ChatIntentDetect)
                    self.generating = True

                    if isinstance(e, Chat):
                        if (chat is None): chat = ChatProceed.start(self.sysPrompt)
                        chat.messages.append({ "role": "user", "content": e.userPrompt })

                    def process():
                        messages = []
                        if isinstance(e, Chat): messages = chat.messages
                        if isinstance(e, ChatProceed): messages = e.messages

                        stream = client.chat.completions.create(
                            model=self.modelName, messages=messages, max_tokens=self.maxTokens, temperature=self.temp, top_p=self.topp,
                            stream=True, timeout=Timeout(write=1.0, read=None, connect=1.0, pool=None),
                            stop = "-COM" if isCommand else [],
                        )
                        try:
                            for chunk in stream:
                                if self._stop or not self.generating: break
                                if chunk.choices[0].delta.content is not None: yield chunk.choices[0].delta.content
                        finally:
                            stream.response.close()

                    commandIterator = SingleLazyIterator(lambda: not self.generating)
                    consumer, tokensWrite, tokensSpeech, tokensPaste, tokensText = teeThreadSafeEager(process(), 4)
                    try:
                        if not isCommand: self.write(chain([e.outStart], progress(consumer.hasStarted, tokensWrite)))
                        if     isCommand: self.write(chain([e.outStart], progress(commandIterator.hasStarted, commandIterator)))
                        if isinstance(e, Chat): self.speak(tokensSpeech)
                        if isinstance(e, ChatPaste): pasteTokens(tokensPaste)
                        consumer()
                        canceled = self.generating is False
                        text = ''.join(tokensText)

                        if isinstance(e, Chat):
                            if len(text)==0: self.write("ERR: chat responded with empty message")
                            else: chat.messages.append({ "role": "assistant", "content": text })

                        if isCommand:
                            command = text.strip().lstrip("COM-").rstrip("-COM").strip()
                            command = command.replace('-', ' ')
                            command = command.replace('unidentified', e.userPrompt)
                            command = 'unidentified' if len(command.strip())==0 else command
                            command = 'unidentified' if canceled else command
                            command = self.commandExecutor(command)
                            commandIterator.put(command)
                    except Exception as x:
                        if isCommand:
                            commandIterator.put('unidentified')
                        raise x

                except openai.APIConnectionError as e:
                    self.write(f"OpenAI server could not be reached: {e.__cause__}")
                except openai.RateLimitError as e:
                    self.write("OpenAI returned 429 status code - rate limit error")
                except openai.APIStatusError as e:
                    self.write(f"OpenAI returned {e.status_code} status code with response {e.response}")
                finally:
                    self.generating = False
