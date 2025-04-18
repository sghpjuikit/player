import os.path
import httpx
import uuid
from util_itr import teeThreadSafe, teeThreadSafeEager, chain, noThinking
from util_paste import pasteTokens
from util_actor import Actor
from util_wrt import Writer
from util_tts import Tts
from util_fut import *
from util_str import *
from util_ctx import *
from imports import *

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

    def http(self, outStart: str):
        self.outStart = outStart
        self.writeTokens = True
        return self

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
            "You are helpful intent detector. " +
            "From now on, identify user intent by returning exactly one command matching the right command matcher from the list given below. " +
            "For each user's message you construct and return single best command possible. " +
            "Only respond with exactly one command in format: `command` (without the surrounding quotes of course). " +
            "The resulting command will be matched against the command matchers in the list. The matchers have some rules, similar to regex, that help you infer how to construct the command. " +
            "\n" +
            "$ is command parameter (always resolve parameters into argument values, do so according to user input) " +
            ": is command parameter default parameter value (use if no value available). " +
            "| is 'or' group, i.e., all values in this group are syntactically valid, but only one is valid semantically. Always pick single best value. " +
            "? is optional part (do not include this part in the output unless it is 'or' group, but never write '?' character). " +
            "\n" +
            "Use '_' as word separator in $ parameter values. " +
            "Resolve all '|' groups into single value; all $ parameters, e.g. ` a|b $number` -> `a 5`." +
            "Do not write comments, do not give examples, do not respond in any other way than the command itself! " +
            "If no command is likely, respond with `unidentified` command. " +
            "Command example: `my command parameter1_value`. " +
            "For example:\n" +
            "* command 'xxx $x_name suffix?' with user intent 'Do xxx called yyy' would resolve to 'xxx yyy'. (Notice resolved $ and removed ?)\n"+
            "* command 'do xxx|yyy|ccc' with user intent 'Do the y thing' would resolve to 'do yyy'. (Notice resolved |)\n" +
            "\n" +
            "**Commands**:\n" +
            "" + assist_function_prompt,
            'Respond with exactly one resolved best-matching command for user intent. User intent:\n```\n' + userPrompt + "\n```",
            'RAW: ' if writeTokens else '',
            '',
            '',
            False,
            writeTokens
        )

    @classmethod
    def python(cls, sysPrompt: str, userPrompt: str, messages: []):
        a = cls(
            sysPrompt, userPrompt, 'RAW: ', '```\n', '\n```', False, True
        )
        for m in messages: a.messages.insert(len(a.messages)-1, m)
        return a

    @classmethod
    def pythonFix(cls, api:str, code: str):
        a = cls(
            'You are expert python programmer that fixes broken code to make it executable and do what was intended.\n' +
            'For each user message, you fix it to and return what seems to be intended python code.\n' +
            'Non-code parts like (unmarked) comments should be turned into speak(str) calls.\n' +
            'You fix code formatting, quoting, remove comments, invalid text, remove markdown code blocks and your response is always executable python code.\n' +
            'You never output comments. You never change used functions names or calls, only fix them. E.g. speak() is valid function.\n' +
            'You make sure str args are correctly escaped.\n' +
            'In addition to python API, this API is available (use it if possible):\n' + api + "\n" +
            'You remove re-definitions of api functions',
            userPrompt='Respond only with the executable code ad avoid any descriptions! The exact code to fix is below this line:\n' + code,
            outStart='', outCont='', outEnd='', speakTokens=False, writeTokens=False
        )
        # few-shot promting
        a.messages.insert(len(a.messages)-1, { "role": "user",      "content": "```\npython\nfunctionCall()\n```" })
        a.messages.insert(len(a.messages)-1, { "role": "assistant", "content": "functionCall()" })
        a.messages.insert(len(a.messages)-1, { "role": "user",      "content": "ok:\nfor i in range(1,5):\n  speak('I'm here)" })
        a.messages.insert(len(a.messages)-1, { "role": "assistant", "content": "for i in range(1,5):\n  speak('I\'m here')" })
        a.messages.insert(len(a.messages)-1, { "role": "user",      "content": "The response is:\n\nspeak('23')" })
        a.messages.insert(len(a.messages)-1, { "role": "assistant", "content": "speak('23')" })
        a.messages.insert(len(a.messages)-1, { "role": "user",      "content": "Here is the python code:\nprint('whatever text')" })
        a.messages.insert(len(a.messages)-1, { "role": "assistant", "content": "speak('Here is the python code')\nprint('whatever text')" })
        a.messages.insert(len(a.messages)-1, { "role": "user",      "content": "Here is your response:\nx = 10+10" })
        a.messages.insert(len(a.messages)-1, { "role": "assistant", "content": "x = 10+10" })
        a.messages.insert(len(a.messages)-1, { "role": "user",      "content": "*speak('I will') *body('bow')*" })
        a.messages.insert(len(a.messages)-1, { "role": "assistant", "content": "speak('I will')\nbody('bow')" })
        a.messages.insert(len(a.messages)-1, { "role": "user",      "content": "Hey! *pokes cheek*" })
        a.messages.insert(len(a.messages)-1, { "role": "assistant", "content": "speak('Hey!')\nbody('pokes cheek')" })
        a.messages.insert(len(a.messages)-1, { "role": "user",      "content": "13.1 / 88 = 0.148" })
        a.messages.insert(len(a.messages)-1, { "role": "assistant", "content": "speak(f'13.1 / 88 = 0.148')" })

        return a

class ChatReact(ChatProceed):
    def __init__(self, sys_prompt: str, event_to_react_to: str, fallback: str):
        super().__init__(
            f"{sys_prompt}",
            f"React to provided event with single short sentence. React in such a way that user will understand what event happened.\n" +
            f"To prevent same response, here is a random seed: {uuid.uuid4()}\n" +
            f"Standard response would be: ```\n{fallback}\n```\n" +
            f"Event:\n {event_to_react_to}."
        )
        self.userPromptRaw = event_to_react_to
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
    streamFuture: Future[str]

    def __iter__(self):
        yield self.event
        yield self.future
        yield self.streamFuture

class Llm(Actor):
    def __init__(self, name: str, deviceName: str, write: Writer, speak: Tts):
        super().__init__("llm", name,  deviceName, write, True)
        self._daemon = False
        self._stop_event = EventLlm(None, None, None)
        self.speak = speak
        self.generating = False
        self.commandExecutor: Callable[[str, Ctx], str] = None

    @contextmanager
    def _active(self):
        try:
            self.generating = True
            yield
        finally:
            self.generating = False

    def __call__(self, e: ChatProceed, ctx: Ctx = CTX, stream = False) -> Future[str | Iterator[str]]:

        def on_done_stream(future):
            try: (text, canceled) = future.result()
            except Exception: (text, canceled) = (None, None)
            if text is not None: e.processTokens(text)

        def on_done(future):
            try: (text, canceled) = future.result()
            except Exception: (text, canceled) = (None, None)

            # speak generated text or fallback if error
            if isinstance(e, ChatReact):
                self.speak(e.fallback if text is None else text.strip('\'" '), ctx.location)
                self.commandExecutor(f'show-emote {e.userPromptRaw}', ctx)

            # run generated command or unidentified if error
            if isinstance(e, ChatIntentDetect) and e.writeTokens:
                if text is None:
                    self.commandExecutor('unidentified', ctx)
                else:
                    command = text.strip()
                    command = command.replace('unidentified', e.userPrompt)
                    command = 'unidentified' if len(command.strip())==0 else command
                    command = 'unidentified' if canceled else command
                    self.commandExecutor(command, ctx)

        if self._stop:
            f = futureFailed(Exception(f"{self.group} stopped"))
            futureOnDone(f, on_done)
            futureOnDone(f, on_done_stream)
            return f
        else:
            ef = EventLlm(e, Future(), Future())
            self.queue.put(ef)
            futureOnDone(ef.future, on_done)
            futureOnDone(ef.streamFuture, on_done_stream)
            return ef.streamFuture if stream else ef.future

    def _get_event_text(self, e: EventLlm) -> str:
        return f"{e.event.__class__.__name__}:{e.event.userPrompt}"


class LlmNone(Llm):
    def __init__(self, speak: Tts, write: Writer):
        super().__init__('LlmNone', 'cpu', write, speak)

    def _loop(self):
        self._loaded = True

        while not self._stop:
            with self._loopProcessEvent() as (e, f, sf):
                if e is None: break
                f.set_exception(Exception(f"{self.group} disabled"))
                sf.set_exception(Exception(f"{self.group} disabled"))

        while not self.queue.empty():
            e, f, sf = self.queue.get()
            f.set_exception(Exception(f"{self.group} stopped"))
            sf.set_exception(Exception(f"{self.group} stopped"))


# home: https://github.com/nomic-ai/gpt4all
# doc https://docs.gpt4all.io/gpt4all_python.html
class LlmGpt4All(Llm):

    def __init__(self, modelName: str, modelPath: str, speak: Tts, write: Writer, sysPrompt: str, maxTokens: int, temp: float, topp: float, topk: int):
        super().__init__('LlmGpt4All', "cpu", write, speak)
        self.modelName = modelName
        self.modelPath = modelPath
        self.sysPrompt = sysPrompt
        self.maxTokens = maxTokens
        self.temp = temp
        self.topp = topp
        self.topk = topk

    def _loop(self):
        # init
        import gpt4all.gpt4all
        from gpt4all import GPT4All  # https://docs.gpt4all.io/index.html
        # init model
        model_file = os.path.join(self.modelPath, self.modelName)
        if os.path.exists(model_file) is False: raise Exception(f"Model= {model_file} not found")
        llm = GPT4All(model_name=self.modelName, model_path=self.modelPath, device="cpu", allow_download=True, verbose=False)
        # init prompt format
        llm_prompt_template_file = model_file + '.prompt.txt'
        llm_prompt_template = None
        if os.path.exists(llm_prompt_template_file):
            with open(llm_prompt_template_file) as f:
                llm_prompt_template = f.read()

        # loop
        with self._looping():
            while not self._stop:
                with self._loopProcessEvent() as (e, f, sf):
                    if e is None: break
                    try:
                        with self._active():
                            with self.write.active():
                                with llm.chat_session():
                                    try:
                                        llm._history = e.messages   # overwrite system prompt & history
                                        self.generating = True
                                        stop = []
                                        text = ''
                                        def process(token_id, token_string):
                                            nonlocal text, stop
                                            text = text + token_string
                                            return not self._stop and self.generating and not contains_any(text, stop, False)
                                        tokens = llm.generate(
                                            e.userPrompt, streaming=True,
                                            max_tokens=self.maxTokens, top_p=self.topp, top_k=self.topk, temp=self.temp,
                                            callback=process
                                        )
                                        consumer, tokensWrite, tokensSpeech, tokensAlt, tokensText = teeThreadSafeEager(noThinking(tokens), 4)
                                        sf.set_result((tokensAlt, self.generating is False))
                                    except Exception as x:
                                        sf.set_exception(x)
                                        raise x
                                    if e.writeTokens: self.write(chain([e.outStart], [e.outCont], tokensWrite, [e.outEnd]))
                                    if e.speakTokens: self.speak(tokensSpeech, CTX.location)
                                    consumer()
                                    text = ''.join(tokensText)
                                    f.set_result((text, self.generating is False))
                    except Exception as x:
                        f.set_exception(x)
                        raise x


# home https://github.com/openai/openai-python
# howto https://cookbook.openai.com/examples/how_to_stream_completions
class LlmHttpOpenAi(Llm):

    def __init__(self, url: str, bearer: str, modelName: str, speak: Tts, write: Writer, sysPrompt: str, maxTokens: int, temp: float, topp: float, topk: int):
        super().__init__('LlmHttpOpenAi', 'http', write, speak)
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
                with self._loopProcessEvent() as (e, f, sf):
                    if e is None: break
                    try:
                        with self._active():
                            with self.write.active():

                                def process():
                                    stream = client.chat.completions.create(
                                        model=self.modelName, messages=e.messages, max_tokens=self.maxTokens, temperature=self.temp, top_p=self.topp,
                                        stream=True, timeout=Timeout(10.0),
                                        stop = [],
                                    )
                                    try:
                                        for chunk in stream:
                                            if self._stop or not self.generating: break
                                            if chunk is None or chunk.choices is None: continue
                                            if chunk.choices[0].delta.content is not None: yield chunk.choices[0].delta.content
                                    finally:
                                        stream.response.close()
                                try:
                                    consumer, tokensWrite, tokensSpeech, tokensAlt, tokensText = teeThreadSafeEager(noThinking(process()), 4)
                                    sf.set_result((tokensAlt, self.generating is False))
                                except Exception as x:
                                    sf.set_exception(e)
                                    raise x

                                if e.writeTokens: self.write(chain([e.outStart], [e.outCont], tokensWrite, [e.outEnd]))
                                if e.speakTokens: self.speak(tokensSpeech, CTX.location)
                                consumer()
                                text = ''.join(tokensText)
                                f.set_result((text, self.generating is False))

                    except Exception as e:
                        f.set_exception(e)
                        if isinstance(e, openai.APIConnectionError):
                            self.write(f"ERR: {self.name} event processing error: server could not be reached: {e.__cause__}")
                        elif isinstance(e, openai.APIStatusError):
                            self.write(f"ERR: {self.name} event processing error: server returned {e.status_code}: {e.message}")
                        elif isinstance(e, httpx.ReadTimeout):
                            self.write(f"ERR: {self.name} event processing error: server timed out")
                        else:
                            raise e