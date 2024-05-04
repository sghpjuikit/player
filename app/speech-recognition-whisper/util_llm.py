from concurrent.futures import Future
from util_paste import pasteTokens
from typing import Callable, cast
from dataclasses import dataclass
from gpt4all import GPT4All  # https://docs.gpt4all.io/index.html
import gpt4all.gpt4all
from util_itr import teeThreadSafe, teeThreadSafeEager, progress, chain, SingleLazyIterator
from util_actor import Actor
from util_wrt import Writer
from util_tts import Tts
from util_str import *

class ChatProceed:
    def __init__(self, sysPrompt: str, userPrompt: str | None):
        self.outStart = 'SYS: '
        self.outCont = ''
        self.outEnd = ''
        self.sysPrompt = sysPrompt
        self.userPrompt = userPrompt
        self.messages = [ ]
        self.messages.append({ "role": "system", "content": self.sysPrompt })
        self.speakTokens = True
        self.writeTokens = True
        self.processTokens = lambda tokens: None
        if (userPrompt is not None and len(userPrompt)>0): self.messages.append({ "role": "user", "content": self.userPrompt })

class ChatWhatCanYouDo(ChatProceed):
    def __init__(self, assist_function_prompt):
        super().__init__(
            "You are voice assistant capable of these functions. "
            "If user askes you about what you can do, you give him overview of your functions. "
            "Funs: \n" + assist_function_prompt,
            "Give me summary of your capabilities"
        )

class ChatIntentDetect(ChatProceed):
    def __init__(self, sysPrompt: str, userPrompt: str, outStart: str, outCont: str, outEnd: str, speakTokens: bool, writeTokens: bool):
        super().__init__(sysPrompt, userPrompt)
        self.outStart = outStart
        self.outCont = outCont
        self.outEnd = outEnd
        self.speakTokens = speakTokens
        self.writeTokens = writeTokens

    @classmethod
    def normal(cls, assist_function_prompt: str, userPrompt: str, writeTokens: bool = True):
        return cls(
            "From now on, identify user intent by returning one of following commands. " +
            "Only respond with command in format: `COM command COM`. " +
            "? is optional, $ is command parameter, : is default value. " +
            "Use '_' as word separator in $ parameter values. " +
            "Do not write $ after resolving parameter, e.g. `$number` -> `5`. " +
            "You can respond with multiple commands, each on new line. " +
            "Command example: COM command prefix parameter_value command suffix COM. " +
            "Commands: \n" + assist_function_prompt,
            userPrompt,
            'COM-DET: ' if writeTokens else '',
            '',
            '',
            False,
            writeTokens
        )

    @classmethod
    def python(cls, sysPrompt: str, userPrompt: str, messages: []):
        a = cls(
            sysPrompt, userPrompt, 'RAW: ', 'executing:\n```\n', '\n```', False, True
        )
        for m in messages: a.messages.insert(len(a.messages)-1, m)
        return a


class ChatReact(ChatProceed):
    def __init__(self, sys_prompt: str, event_to_react_to: str, fallback: str):
        super().__init__(
            f"{sys_prompt}",
            f"React to provided event with single short sentence. Event:\n {event_to_react_to}."
        )
        self.outStart = 'SYS: '
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
        self.outStart = 'SYS: '
        self.speakTokens = False
        self.processTokens = pasteTokens

@dataclass
class EventLlm:
    event: ChatProceed
    future: Future[str]

    def __iter__(self):
        yield self.event
        yield self.future

class Llm(Actor):
    def __init__(self, name: str, deviceName: str, write: Writer, speak: Tts):
        super().__init__("llm", name,  deviceName, write, True)
        self.speak = speak
        self.generating = False

    def __call__(self, e: ChatProceed) -> Future[str]:
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
                    command = text.strip().removeprefix("COM ").removesuffix(" COM").strip()
                    command = command.replace('unidentified', e.userPrompt)
                    command = 'unidentified' if len(command.strip())==0 else command
                    command = 'unidentified' if canceled else command
                    command = self.commandExecutor(command)
                    commandIterator.put(command)

        ef.future.add_done_callback(on_done)
        return ef.future

    def _get_event_text(self, e: EventLlm) -> str:
        return f"{e.event.__class__.__name__}({e.event.userPrompt})"

class LlmNone(Llm):
    def __init__(self, speak: Tts, write: Writer, commandExecutor: Callable[[str], str]):
        super().__init__('LlmNone', 'cpu', write, speak)
        self.commandExecutor = commandExecutor

    def _loop(self):
        self._loaded = True
        while not self._stop:
            with self._loopProcessEvent() as (e, f):
                if isinstance(e, ChatReact):
                    f.set_result(e.fallback)
                elif isinstance(e, ChatIntentDetect):
                    f.set_result('COM ' + e.userPrompt + ' COM')
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

    def _loop(self):
        # init
        llm = GPT4All(model_name=self.modelName, model_path=self.modelPath, device="cpu", allow_download=True, verbose=False)
        # loop
        with (self._looping()):
            while not self._stop:
                ef = self.queue.get()
                x, ff = ef

                while not self._stop:
                    with self._loopProcessEvent() as (e, f):
                        isCommand = isinstance(e, ChatIntentDetect)
                        isCommandWrite = isCommand and cast(ChatIntentDetect, e).writeTokens
                        commandIterator = SingleLazyIterator(lambda: not self.generating)
                        try:
                            self.generating = True
                            llm._current_prompt_template = llm.config["promptTemplate"]
                            llm._history = e.messages
                            stop = [" COM", "<|eot_id|>"] if isCommand else ["<|eot_id|>"]
                            text = ''
                            def process(token_id, token_string):
                                text = text + token_string
                                return not self._stop and self.generating and not contains_any(text, stop)
                            tokens = llm.generate(
                                e.userPrompt, streaming=True,
                                max_tokens=self.maxTokens, top_p=self.topp, top_k=self.topk, temp=self.temp,
                                callback=process
                            )
                            consumer, tokensWrite, tokensSpeech, tokensAlt, tokensText = teeThreadSafeEager(tokens, 4)
                            if not isCommand and e.writeTokens: self.write(chain([e.outStart], progress(consumer.hasStarted, chain([e.outCont], tokensWrite)), [e.outEnd]))
                            if isCommandWrite: self.write(chain([e.outStart], progress(commandIterator.hasStarted, chain([e.outCont], commandIterator)), [e.outEnd]))
                            if e.speakTokens: self.speak(tokensSpeech)
                            e.processTokens(tokensAlt)
                            consumer()
                            canceled = self.generating is False
                            text = ''.join(tokensText)
                            f.set_result((text, canceled, commandIterator))
                        except Exception as x:
                            f.set_exception(x)
                            raise x
                        finally:
                            self.generating = False


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
        client = openai.OpenAI(api_key=self.bearer, base_url=self.url)
        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as (e, f):
                    isCommand = isinstance(e, ChatIntentDetect)
                    isCommandWrite = isCommand and cast(ChatIntentDetect, e).writeTokens
                    commandIterator = SingleLazyIterator(lambda: not self.generating)
                    try:
                        self.generating = True

                        def process():
                            stream = client.chat.completions.create(
                                model=self.modelName, messages=e.messages, max_tokens=self.maxTokens, temperature=self.temp, top_p=self.topp,
                                stream=True, timeout=Timeout(5.0),
                                stop = [" COM", "<|eot_id|>"] if isCommand else ["<|eot_id|>"],
                            )
                            try:
                                for chunk in stream:
                                    if self._stop or not self.generating: break
                                    if chunk.choices[0].delta.content is not None: yield chunk.choices[0].delta.content
                            finally:
                                stream.response.close()

                        consumer, tokensWrite, tokensSpeech, tokensAlt, tokensText = teeThreadSafeEager(process(), 4)
                        if not isCommand and e.writeTokens: self.write(chain([e.outStart], progress(consumer.hasStarted, chain([e.outCont], tokensWrite)), [e.outEnd]))
                        if isCommandWrite: self.write(chain([e.outStart], progress(commandIterator.hasStarted, chain([e.outCont], commandIterator)), [e.outEnd]))
                        if e.speakTokens: self.speak(tokensSpeech)
                        e.processTokens(tokensAlt)
                        consumer()
                        canceled = self.generating is False
                        text = ''.join(tokensText)
                        f.set_result((text, canceled, commandIterator))

                    except Exception as e:
                        f.set_exception(e)
                        if isinstance(e, openai.APIConnectionError): self.write(f"ERR: OpenAI server could not be reached: {e.__cause__}")
                        elif isinstance(e, openai.APIStatusError): self.write(f"ERR: OpenAI returned {e.status_code} status code with response {e.response}")
                        else: raise e
                    finally:
                        self.generating = False