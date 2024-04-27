import gpt4all.gpt4all
from gpt4all import GPT4All  # https://docs.gpt4all.io/index.html
from gpt4all.gpt4all import empty_chat_session
from typing import Callable, cast
from util_tts import Tts
from util_wrt import Writer
from util_actor import Actor
from util_itr import teeThreadSafe, teeThreadSafeEager, progress, chain, SingleLazyIterator
from util_paste import pasteTokens
from concurrent.futures import Future
from dataclasses import dataclass

@dataclass
class EventLlm:
    event: object
    future: Future

    def __iter__(self):
        yield self.event
        yield self.future


class ChatStart:
    def __init__(self):
        pass

class ChatStop:
    def __init__(self):
        pass

class Chat:
    def __init__(self, userPrompt: str):
        self.outStart = 'CHAT: '
        self.userPrompt = userPrompt
        self.speakTokens = True
        self.writeTokens = True
        self.processTokens = lambda tokens: None

class ChatProceed:
    def __init__(self, sysPrompt: str, userPrompt: str | None):
        self.outStart = 'CHAT: '
        self.sysPrompt = sysPrompt
        self.userPrompt = userPrompt
        self.messages = [ ]
        self.messages.append({ "role": "system", "content": self.sysPrompt })
        self.speakTokens = True
        self.writeTokens = True
        self.processTokens = lambda tokens: None
        if (userPrompt is not None): self.messages.append({ "role": "user", "content": self.userPrompt })

    @classmethod
    def start(cls, sysPrompt: str):
        return cls(sysPrompt, None)

class ChatWhatCanYouDo(ChatProceed):
    def __init__(self, assist_function_prompt):
        super().__init__(
            "You are voice assistant capable of these functions. "
            "If user askes you about what you can do, you give him overview of your functions. "
            "Funs: \n" + assist_function_prompt,
            "Give me summary of your capabilities"
        )

class ChatIntentDetect(ChatProceed):
    def __init__(self, assist_function_prompt: str, userPrompt: str, writeTokens: bool = True):
        super().__init__(
            "From now on, identify user intent by returning one of following commands. " +
            "Only respond with command in format: `COM-command-COM`. " +
            "? is optional, $ is command parameter, : is default value. " +
            "Use '-' as word separator in command. " +
            "Use '_' as word separator in $ parameter values. " +
            "Do not write $ after resolving parameter, e.g. `$number` -> `5`. " +
            "Command example: COM-command-prefix-parameter_value-command-suffix-COM. " +
            "Commands: \n" + assist_function_prompt,
            userPrompt
        )
        self.outStart = 'COM-DET: ' if writeTokens else ''
        self.speakTokens = False
        self.writeTokens = writeTokens

class ChatReact(ChatProceed):
    def __init__(self, sys_prompt: str, event_to_react_to: str, fallback: str):
        super().__init__(
            f"{sys_prompt}. You are a friend. You are terse and concise. You only react to provided events with single word or sentence.",
            f"{event_to_react_to}."
        )
        self.outStart = 'CHAT: '
        self.speakTokens = False
        self.writeTokens = False
        self.fallback = fallback

class ChatPaste(ChatProceed):
    def __init__(self, userPrompt: str):
        super().__init__(
            "From now on, seamlessly complete user messages. " +
            "Only complete the message so it connects with user's. " +
            "If user asks to write code, only provide the code",
            userPrompt
        )
        self.outStart = 'CHAT: '
        self.speakTokens = False
        self.processTokens = pasteTokens


class Llm(Actor):
    def __init__(self, name: str, deviceName: str, write: Writer, speak: Tts):
        super().__init__("llm", name,  deviceName, write, True)
        self.speak = speak
        self.generating = False

    def __call__(self, e: ChatStart | Chat | ChatProceed | ChatStop) -> Future[str]:
        ef = EventLlm(e, Future())
        self.queue.put(ef)

        def on_done(future):
            try: (text, canceled, commandIterator) = future.result()
            except Exception: (text, canceled, commandIterator) = (None, None, None)

            # speak generated text or fallback if error
            if isinstance(e, ChatReact):
                self.speak(e.fallback if text is None else text)

            # run generated command or unidentified if error
            if isinstance(e, ChatIntentDetect) and e.writeTokens:
                if text is None:
                    commandIterator.put('unidentified')
                else:
                    command = text.strip().removeprefix("COM-").removesuffix("-COM").strip().replace('-', ' ')
                    command = command.replace('unidentified', e.userPrompt)
                    command = 'unidentified' if len(command.strip())==0 else command
                    command = 'unidentified' if canceled else command
                    command = self.commandExecutor(command)
                    commandIterator.put(command)

        ef.future.add_done_callback(on_done)
        return ef.future

    def _get_event_text(self, e: EventLlm) -> str:
        if isinstance(e.event, Chat | ChatStart | ChatStop): return e.event.__class__.__name__
        if isinstance(e.event, ChatProceed): return f"{e.event.__class__.__name__}({e.event.userPrompt})"
        return str(e.event)

class LlmNone(Llm):
    def __init__(self, speak: Tts, write: Writer, commandExecutor: Callable[[str], str]):
        super().__init__('LlmNone', 'cpu', write, speak)
        self.commandExecutor = commandExecutor

    def _loop(self):
        self._loaded = True
        while not self._stop:
            with self._loopProcessEvent() as ef:
                e, f = ef

                if isinstance(e, ChatReact):
                    f.set_result(e.fallback)
                elif isinstance(e, ChatIntentDetect):
                    f.set_result('COM-' + e.userPrompt + '-COM')
                    self.commandExecutor(text)
                    if e.writeTokens: self.write('COM-DET: ' + e.userPrompt)
                else:
                    f.set_exception(Exception("Illegal"))
        self._clear_queue()


# home: https://github.com/nomic-ai/gpt4all
# doc https://docs.gpt4all.io/gpt4all_python.html
class LlmGpt4All(Llm):

    def __init__(self, modelName: str, modelPath: str, speak: Tts, write: Writer, commandExecutor: Callable[[str], str], sysPrompt: str, maxTokens: int, temp: float, topp: float, topk: int):
        super().__init__('LlmGpt4All', "cpu", write, speak)
        self.commandExecutor = commandExecutor
        self.modelName = modelName
        self.modelPath = modelPath
        # gpt4all.gpt4all.DEFAULT_MODEL_DIRECTORY = chatDir
        self.sysPrompt = sysPrompt
        self.maxTokens = maxTokens
        self.temp = temp
        self.topp = topp
        self.topk = topk
        self.enabled = False # delayed load

    def _loop(self):
        # init
        llm = None
        # loop
        with (self._looping()):
            while not self._stop:
                ef = self.queue.get()
                x, ff = ef

                # load model lazily
                if llm is None: llm = GPT4All(model_name=self.modelName, model_path=self.modelPath, device="cpu", allow_download=True)
                self.enabled = True # delayed load

                if isinstance(x, ChatStart):
                    ff.set_result(None)
                    with llm.chat_session(self.sysPrompt):
                        while not self._stop:
                            with self._loopProcessEvent() as tf:
                                t, f = tf
                                if isinstance(t, ChatStart):
                                    f.set_result(None)
                                    pass
                                if isinstance(t, ChatStop):
                                    f.set_result(None)
                                    break
                                if isinstance(t, Chat | ChatProceed):
                                    isCommand = isinstance(t, ChatIntentDetect)
                                    isCommandWrite = isCommand and cast(ChatIntentDetect, t).writeTokens
                                    commandIterator = SingleLazyIterator(lambda: not self.generating)
                                    try:
                                        self.generating = True

                                        stop = "-COM" if isCommand else None,
                                        text = ''
                                        def process(token_id, token_string):
                                            text = text + token_string
                                            return not self._stop and self.generating and (not isCommand or stop in text)
                                        tokens = llm.generate(
                                            t.userPrompt, streaming=True,
                                            max_tokens=self.maxTokens, top_p=self.topp, top_k=self.topk, temp=self.temp,
                                            callback=process
                                        )
                                        consumer, tokensWrite, tokensSpeech, tokensAlt, tokensText = teeThreadSafeEager(tokens, 4)
                                        if not isCommand and t.writeTokens: self.write(chain([t.outStart], progress(consumer.hasStarted, tokensWrite)))
                                        if isCommandWrite: self.write(chain([t.outStart], progress(commandIterator.hasStarted, commandIterator)))
                                        if t.speakTokens: self.speak(tokensSpeech)
                                        t.processTokens(tokensAlt)
                                        consumer()
                                        canceled = self.generating is False
                                        text = ''.join(tokensText)
                                        f.set_result((text, canceled, commandIterator))
                                    except Exception as x:
                                        f.set_exception(x)
                                        raise x
                                    finally:
                                        self.generating = False
                else:
                    ff.set_result(None)

# home https://github.com/openai/openai-python
# howto https://cookbook.openai.com/examples/how_to_stream_completions
class LlmHttpOpenAi(Llm):

    def __init__(self, url: str, bearer: str, modelName: str, speak: Tts, write: Writer, commandExecutor: Callable[[str], str], sysPrompt: str, maxTokens: int, temp: float, topp: float, topk: int):
        super().__init__('LlmHttpOpenAi', 'http', write, speak)
        self.commandExecutor = commandExecutor
        self.url = url
        self.bearer = bearer
        self.modelName = modelName
        self.sysPrompt = sysPrompt
        self.maxTokens = maxTokens
        self.temp = temp
        self.topp = topp
        self.topk = topk

    def _loop(self):
        # init
        from httpx import Timeout
        import openai
        # init
        chat: ChatProceed | None = None
        client = openai.OpenAI(api_key=self.bearer, base_url=self.url)
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as ef:
                    e, f = ef

                    if isinstance(e, ChatStart):
                        if chat is not None: chat = ChatProceed.start(self.sysPrompt)
                        f.set_result(None)

                    if isinstance(e, ChatStop):
                        chat = None
                        f.set_result(None)

                    if isinstance(e, Chat | ChatProceed):
                        isCommand = isinstance(e, ChatIntentDetect)
                        isCommandWrite = isCommand and cast(ChatIntentDetect, e).writeTokens
                        commandIterator = SingleLazyIterator(lambda: not self.generating)
                        try:
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
                                    stream=True, timeout=Timeout(1.0),
                                    stop = ["-COM", "<|eot_id|>"] if isCommand else ["<|eot_id|>"],
                                )
                                try:
                                    for chunk in stream:
                                        if self._stop or not self.generating: break
                                        if chunk.choices[0].delta.content is not None: yield chunk.choices[0].delta.content
                                finally:
                                    stream.response.close()

                            consumer, tokensWrite, tokensSpeech, tokensAlt, tokensText = teeThreadSafeEager(process(), 4)
                            if not isCommand and e.writeTokens: self.write(chain([e.outStart], progress(consumer.hasStarted, tokensWrite)))
                            if isCommandWrite: self.write(chain([e.outStart], progress(commandIterator.hasStarted, commandIterator)))
                            if e.speakTokens: self.speak(tokensSpeech)
                            e.processTokens(tokensAlt)
                            consumer()
                            canceled = self.generating is False
                            text = ''.join(tokensText)
                            f.set_result((text, canceled, commandIterator))

                            if isinstance(e, Chat):
                                if len(text)==0: self.write("ERR: chat responded with empty message")
                                else: chat.messages.append({ "role": "assistant", "content": text })

                        except Exception as e:
                            f.set_exception(e)
                            if isinstance(e, openai.APIConnectionError): self.write(f"ERR: OpenAI server could not be reached: {e.__cause__}")
                            elif isinstance(e, openai.APIStatusError): self.write(f"ERR: OpenAI returned {e.status_code} status code with response {e.response}")
                            else: raise e
                        finally:
                            self.generating = False