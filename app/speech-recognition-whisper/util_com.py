from imports import *
import platform
import os
from datetime import datetime
from util_llm import ChatIntentDetect
from util_fut import *
from util_itr import *
from util_ctx import *
from util_api import *
from util_md import *

class CommandExecutor:
    def execute(self, text: str, ctx: Ctx = CTX) -> str:
        pass

class CommandExecutorAsIs(CommandExecutor):
    def execute(self, text: str, ctx: Ctx = CTX) -> str:
        return text

class CommandExecutorDoNothing(CommandExecutor):
    def execute(self, text: str, ctx: Ctx = CTX) -> str:
        return "ignore"

def sanitize_python_code(text: str) -> str:
    # strip whitespace, needed since this is recursive function, so its stirpping mid-content as well
    t = text.strip()
    # some models like to use starred expression with speak, which escapes code validation, this is reasonable fix
    t = t.replace('<|eom_id|><|start_header_id|>assistant<|end_header_id|>', '')
    t = t.removeprefix('*')
    t = t.removesuffix('*')
    t = t.replace('*speak(', 'speak(')
    t = t.replace('*body(', 'body(')
    t = t.replace(')*', ')')
    # some models churn out markdown prefixes
    if t.startswith('```python'): return sanitize_python_code(t.removeprefix("```python"))
    if t.startswith('```'): return sanitize_python_code(t.removeprefix("```"))
    if t.endswith('```'): return sanitize_python_code(t.removesuffix("```"))
    # return code
    return t

class PythonExecutor:
    def __init__(self, api: Api, write, llmPromptSys, commandExecutor, voices):
        self.id = 0
        self.api = api
        self.write = write

        self.__llmPromptDoc = self.load_promptDoc(voices)
        self.__llmPrompt = ''
        self.__llmPromptSys = ''
        self.llmPromptSys = llmPromptSys
        self.memFile = 'memory/mem.json'
        self.mem = self.load_memory()

        self.commandExecutor = commandExecutor
        self.voices = voices
        self.chatSet([])
        self.isQuestion = False
        self.isBlockingQuestion = False
        self.isBlockingQuestionSpeaker = None
        self.onBlockingQuestionDone = Future()
        self.speakerLast: str | None = None

    @property
    def llmPrompt(self):
        return self.__llmPrompt

    @llmPrompt.setter
    def llmPrompt(self, value: str):
        if value != self.__llmPrompt:
            self.__llmPrompt = value
            self.api.events({'type': 'prompt-changed', 'prompt' : value})

    @property
    def llmPromptSys(self):
        return self.__llmPromptSys

    @llmPromptSys.setter
    def llmPromptSys(self, value: str):
        if value != self.__llmPromptSys:
            self.__llmPromptSys = value
            self.llmPrompt = self.prompt()

    def llmPromptDoc(self):
        r = ""
        for key, value in self.__llmPromptDoc.items():
            if len(str(value))>0:
                r += f"{key}:\n " + str(value).replace('\n', '\n ').rstrip() + "\n"
        return ""

    def chatEmpty(self, ms) -> bool:
        return len(self.ms)>0

    def chatSet(self, ms):
        self.ms = ms
        self.api.events({'type':"chat-history-set", "value": ms})

    def chatAppend(self, m):
        self.ms.append(m)
        self.api.events({'type':"chat-history-add", "value": m})

    def load_promptDoc(self, voices: str) -> dict | None:
        self.write('RAW: Extended prompt loading...')
        dict = index_md_file('README-ASSISTANT.md')
        dict['What are you > Voices > List all available'] = voices
        dict['What are you > Persona > List all available'] = ', '.join([file.split('.')[0] for file in os.listdir('personas') if file.endswith('.txt')])
        dict['What are you > Software > Operating system'] = '' + \
            f"* Architecture: {platform.architecture()}\n" + \
            f"* Machine: {platform.machine()}\n" + \
            f"* Operating System Release: {platform.release()}\n" + \
            f"* System Name: {platform.system()}\n" + \
            f"* Operating System Version: {platform.version()}\n" + \
            f"* Node: {platform.node()}\n" + \
            f"* Platform: {platform.platform()}"
        self.write(f'RAW: Extended prompt loaded: {dict}')
        return dict

    def load_memory(self) -> dict | None:
        """Load memory from a JSON file."""
        import json
        try:
            self.write('RAW: Memory loading...')
            with open(self.memFile, 'r') as file:
                memJs = json.load(file)
                self.write('RAW: Memory loaded')
                return memJs
        except FileNotFoundError:
            self.write('RAW: Memory empty. Initializing...*')
            with open(self.memFile, 'w') as file:
                json.dump({}, file, indent=4)
                return {}
        except json.JSONDecodeError as e:
            self.write(f'ERR: Memory load error: {e}')
            self.write(f'ERR: Memory will remain inaccessible')
            return None

    def save_memory(self) -> None:
        """Save memory to a JSON file."""
        import json
        if self.mem is None: return
        with open(self.memFile, 'w') as file:
            json.dump(self.mem, file, indent=4)

    def showEmote(self, emotionInput: str, ctx: Ctx):
        def showEmoteDo():
            try:
                import os
                import random
                directory_path = 'emotes'
                directories = [d for d in os.listdir(directory_path) if os.path.isdir(os.path.join(directory_path, d))]
                if len(directories)==0: self.write(f'COM: {ctx.speaker}:{ctx.location}:show emote none')
                if len(directories)==0: return
                directoriesS = ''.join(map(lambda x: f'\n* {x}', directories))
                f = self.api.llm(ChatIntentDetect(
                    f'You are emotion detector. Available emotions are:{directoriesS}',
                    f'Respond with exactly one closest emotion, or \'none\' if no emotion is close, for the event: {emotionInput}', '', '', '', False, False
                ))
                try: (text, canceled) = f.result()
                except Exception: (text, canceled) = (None, None)
                if text is None: self.write(f'COM: {ctx.speaker}:{ctx.location}:show emote none')
                if text is None: return
                text = text.rstrip('.!?').strip().lower()
                if text not in directories: self.write(f'COM: {ctx.speaker}:{ctx.location}:show emote none')
                if text not in directories: return
                d = os.path.join(directory_path, text)
                files = os.listdir(d)
                if len(files)==0: self.write(f'COM: User:PC:show emote none')
                if len(files)==0: return
                file = os.path.join(directory_path, text, random.choice(files))
                if os.path.exists(file): self.write(f'COM: {ctx.speaker}:{ctx.location}:show emote {file}')
            except Exception:
                print_exc()
        Thread(name='Emote-Processor', target=showEmoteDo, daemon=True).start()

    def onSpeech(self, speaker):
        if self.speakerLast==speaker:
            if self.isBlockingQuestion is False:
                print(f'canceling because speech done by {speaker}',)
                self.cancelActiveCommand()

    def onSpeechStart(self, speaker):
        if self.speakerLast==speaker:
            if self.isBlockingQuestion is False:
                print(f'canceling because speech started by {speaker}')
                self.cancelActiveCommand()

    def onChatRestart(self):
        self.chatSet([])

    def cancelActiveCommand(self):
        self.id = self.id+1

    def generatePythonAndExecuteInternal(self, textOriginal: str, history: bool = True):
        self.generatePythonAndExecute('SYSTEM', 'INTERNAL', textOriginal, history)


    def generatePythonAndExecute(self, speaker: str, location: Location, textOriginal: str, history: bool = True):
        try:
            self.cancelActiveCommand()
            idd = self.id

            if history:
                self.chatAppend({"role": "user", "content": f"TIME=\"{datetime.now().isoformat()}\"\nSPEAKER=\"{speaker}\"\nLOCATION=\"{location}\"\n\n{textOriginal}"})

            def on_done(future):
                try:
                    (textIterator, canceled) = future.result()
                    if canceled is True: self.write(f"RAW: command CANCELLED")
                except Exception: (textIterator, canceled) = (None, None)

                if textIterator is None or canceled: return
                Thread(name='command-executor', target=lambda: self.executeImplPre(speaker, location, textIterator, textOriginal, idd), daemon=True).start()

            sp = self.prompt()
            up = textOriginal
            futureOnDone(self.api.llm(ChatIntentDetect.python(sp, up, self.ms), Ctx(speaker, location), True), on_done)
        except Exception:
            self.write("ERR: Failed to respond")
            print_exc()

    def execute(self, speaker: str, location: Location, text: str):
        self.cancelActiveCommand()
        self.executeImpl(speaker, location, text, text, self.id)

    def executeImplPre(self, speaker: str, location: Location, textIterator: str, textOriginal: str, idd: str, fix: bool = True):
        locals = dict()
        for code in python_code_chunks(textIterator):
            code = sanitize_python_code(code)
            if len(code)==0: continue # ignore empty line
            if code.startswith('#'): continue # ignore comment
            if not self.isValidPython(code): code = 'speak(\'' + code.replace("'", "\\'") + '\')' # speak non calls
            self.executeImpl(speaker, location, code, textOriginal, idd, preserved_locals=locals)

    def executeImpl(self, speaker: str, location: Location, text: str, textOriginal: str, idd: str, fix: bool = True, preserved_locals = dict()):
        text = sanitize_python_code(text)
        try:
            import ast
            import datetime
            import time
            from util_paste import get_clipboard_text

            # plumbing - command execution cancelled prematurely by user
            class CommandCancelException(Exception): pass
            # plumbing - command execution cancelled gracefuly by executor
            class CommandNextException(Exception): pass
            # plumbing - canceller
            def assertSkip():
                if (idd!=self.id): raise CommandCancelException()
            # plumbing - python file executor
            def execCode(filename: str, **kwargs) -> object:
                try:
                    with open(f'{filename}.py', 'r') as file:
                        indent = '    '
                        # define code
                        code = file.read()
                        # define result
                        code = f'out_result = None\n{code}'
                        # define variables
                        for key, value in kwargs.items(): code = f'{key} = {repr(value)}\n{code}'
                        # define result function
                        code += '\ndef out_result_function(): return out_result'
                        # debug
                        # print('---')
                        # print(code)
                        # print('---')
                        # exec code
                        exec(code)
                        # return result
                        try: return out_result_function()
                        except: return None
                    speak("Done")
                except:
                    speak("Error")
                    print_exc()
                    return None
            # api functions
            def command(c: str | None):
                assertSkip()
                if c is None: return # sometimes llm passes bad function result here, do nothing
                if len(c)==0: return # just in case
                self.write(f'COM: {speaker}:{location}:' + c)
            def doNothing(reason: str = ''):
                if reason: thinkPassive(reason)
            def setReminderIn(afterNumber: float, afterUnit: str, text_to_remind: str):
                command(f'set reminder in {afterNumber}{afterUnit} {text_to_remind}')
            def storeMemory(topic: str, memory: str) -> None:
                timestamp = datetime.datetime.now().isoformat()
                if self.mem is None: self.mem = {}
                if topic in self.mem: self.mem[topic].append([(timestamp, memory)])  # Append memory if topic exists
                else: self.mem[topic] = [(timestamp, memory)]  # Create new entry if topic does not exist
                self.write(f'*Saving topic={topic} memory={memory}*')
                self.save_memory()
            def accessMemory(query: str):
                # self.mem.update(self.__llmPromptDoc)

                mem = {} if self.mem is None else self.mem.copy()
                memKeys = '* ' + "\n* ".join(mem.keys())
                memKeyAll = 'all'
                memKeyNone = 'none'
                memKeyEmpty = 'empty'
                mem[memKeyAll] = "Memory contains data under the following topics (that can be queried):\n" + memKeys
                mem[memKeyNone] = "No relevant memory"
                mem[memKeyEmpty] = "Memory is empty"
                try:
                    (key, canceled) = (memKeyEmpty, False) if not mem else self.api.llm(ChatIntentDetect(
                        f'You find most relevant knowledge index from the tree given.\n' + \
                        f'Respond with exactly one which is the most query-relevant, \'all\' if user wants to know what is in the memory or \'empty\' if memory empty or \'none\' if no matching.\n' + \
                        f'Do not respond anything else. Do not interpret the index or add to it. Your response must be identical to chosen entry.',
                        f'Index entries:\n{memKeys}\n\nWhich of them is the most likely to help with the query:\n{query}',
                        '', '', '', False, False
                    )).result()
                    if canceled is not True:
                        memKey = memKeyNone
                        for k in mem.keys():
                            # self.write(f'{key.strip().lower()}=={k.strip().lower()}')
                            if key.strip().lower()==k.strip().lower():
                                memKey = k

                        self.write(f'Accessing memory \'{key}\'')
                        self.chatAppend({"role": "user", "content": '*waiting*'})
                        self.generatePythonAndExecuteInternal(f'You accesed your memory and can now reply to the query using the data:\n {str(mem[key])}')
                except Exception as e:
                    speak(f'I\'m sorry, I failed to respond: {e}')
                    raise e
            def setReminderAt(at: datetime, text_to_remind: str):
                command(f'set reminder at {at.strftime("%Y-%m-%dT%H:%M:%SZ")} {text_to_remind}')
            def generate(c: str):
                command('generate ' + c)
            def print(t: object):
                # alias for speak, because LLM likes to use print instead of speak too much
                speak(t)
            def speak(t: object, emotion: str | None = None):
                if t is None: return;
                assertSkip()
                if emotion is not None: self.write(f"*{emotion}*")
                self.api.ttsSkippable(f'{t}'.removeprefix('"').removesuffix('"'), location).result()
            def body(t: str):
                assertSkip()
                self.write(f'SYS: *{t}*')
            def wait(t: float):
                for _ in range(int(t/0.1)):
                    assertSkip()
                    self.api.ttsPause(100)
                    time.sleep(0.1)
            def speakCurrentTime(): command('what time is it')
            def speakCurrentDate(): command('what date is it')
            def speakCurrentSong(): command('what song is active')
            def speakDefinition(t: str): command('describe ' + t)
            def thinkPassive(*thoughts: str):
                t = thoughts[0] if len(thoughts)==1 else ''.join(map(lambda t: '\n*' + str(t) + "*", thoughts))
                self.write(f'~{t}~')
            def think(*thoughts: str):
                t = thoughts[0] if len(thoughts)==1 else ''.join(map(lambda t: '\n*' + str(t), thoughts))
                
                # self.generatePythonAndExecute('System', 'My thoughts are:' + ''.join(map(lambda t: '\n* ' + str(t), thoughts)))

                # self.historyAppend({ "role": "user", "content": 'Your thoughts are:' + ''.join(map(lambda t: '\n* ' + str(t), thoughts)) })
                # self.generatePythonAndExecute(speaker, 'Your thoughts are:' + ''.join(map(lambda t: '\n* ' + str(t), thoughts)))
                # self.ms.pop()

                self.chatAppend({"role": "user", "content": '*waiting*'})
                self.generatePythonAndExecuteInternal(
                    f'You had thoughts below, now you stop thinking and act (Do not use think() functions to reply). ' +
                    f'If the thought gives you an action to do, do it. Otherwise, e.g., when it is emotion, fact or statement that does not require action, do nothing. ' +
                    f'Example of thoughs that does not require action: "I should be kinder", "Speak less", "User showed respect".' +
                    f'Example of thoughs that do: "Say hello", "I need to write 5 types of fruit".' +
                    f'Your thoughts:' + t
                )


                raise CommandNextException()
            def getClipboardAnd(input: str = '') -> str | None:
                text = get_clipboard_text()
                if len(input)==0: return '' if text is None else text

                # self.generatePythonAndExecute('System', f'{input}:\n{speaker} set clipboard to:\n```\n{get_clipboard_text()}\n```')

                # self.historyAppend({ "role": "system", "content": f'{speaker} set clipboard to:\n```\n{get_clipboard_text()}\n```' })
                # self.generatePythonAndExecute(speaker, textOriginal, False)
                # self.ms.pop()

                self.chatAppend({"role": "user", "content": '*waiting*'})
                self.generatePythonAndExecuteInternal(f'Do not try to obtain clipboard anymore. I set clipboard for you to:\n```\n{text}\n```')

                # self.ms.pop()
                # self.generatePythonAndExecute(speaker, location, f'{textOriginal}.\n\nEDIT: I set clipboard for you to (do not try to obtain clipboard anymore):\n```\n{get_clipboard_text()}\n```')

                raise CommandNextException()
            def question(question: str) -> str | None:
                self.isQuestion = True
                speak(question)
                raise CommandNextException()
            def questionBlocking(question: str) -> str | None:
                try:
                    self.onBlockingQuestionDone = Future()
                    self.isBlockingQuestion = True
                    self.isBlockingQuestionSpeaker = speaker
                    speak(question)
                    try:
                        return self.onBlockingQuestionDone.result().rstrip('.')
                    except Exception as e:
                        speak("Unable to continue, error during answer")
                        raise e
                except:
                    return None
                finally:
                    self.isBlockingQuestion = False
                    self.isBlockingQuestionSpeaker = None
                    self.onBlockingQuestionDone = Future()
            def writeCode(language: str, code: str) -> str:
                self.write(f"```{language}\n{code}\n```")
                return text
            def generateCode(language: str, userPrompt: str) -> str:
                assertSkip()

                f = self.llm(ChatIntentDetect(
                    f'You are expert programmer. Your task is to write code.\n' +
                    f'You write only valid code (entirety of your response). You use comments if appropriate.\n' +
                    f'You never respond with notes like \'Here is your code:\', speach or markdown code blocks (e.g. ```python)',
                    f'Write code in {language}.\n' + userPrompt, f'SYS: ```{language}\n', '', '\n```', False, True
                ))
                try: (text, canceled) = f.result()
                except Exception as e: raise e
                return text
            def showEmote(emotionInput: str):
                assertSkip()
                self.showEmote(emotionInput, Ctx(speaker, location))
            def showWarning(text: str):
                assertSkip()
                command(f"show warning {text}")
            def saveClipboardImage() -> str | None:
                name = questionBlocking('What will be the name of the file?')
                speak('Ok.')
                return execCode('com_saveimage', in_name = name)
            def recordVoice():
                name = questionBlocking('What will be the name of the speaker?')
                wait(1.0)
                speak('Ok. Speak anything for 20 seconds.')
                speak('Recording started')
                execCode('com_record', in_name=name)
                speak('Recording finished')
            def controlMusic(*action: object):
                command("playback " + ', '.join(map(str, action)))
            def controlLights(*action: object):
                command("hue lights " + ', '.join(map(str, action)))
            def commandRepeatLastSpeech():
                'speak again what you said last time, user may have not heard or asks you'
                command(f'repeat last speech')
            def commandListCommands():
                'speak all commands, use only if user specifically asks for all commands'
                command(f'list commands')
            def commandRestartAssistant():
                command(f'restart assistant')
            def commandStartConversation():
                'user wishes to start conversation with you'
                command(f'start conversation')
            def commandRestartConversation():
                'user wishes to restart conversation with you'
                command(f'restart conversation')
            def commandStopConversation():
                'user wishes to end conversation with you'
                command(f'stop conversation')
            def commandSystem(action: str): # action is one of shut_down|restart|hibernate|sleep|lock|log_off
                command(f'{action} system')
            def commandListVoices():
                command(f'list available voices')
            def commandChangeVoice(voice: str):
                f = self.api.llm(ChatIntentDetect(
                    f'You are voice selector. Available voices are: {self.voices}',
                    f'Respond with exactly one closest voice, exactly as defined, or \'none\' if no such voice is close, for the input: {voice}', '', '', '', False, False
                ))
                self.commandExecutor.execute('change voice ' + f.result()[0], Ctx(speaker, location))

            def commandOpen(widget_name: str):
                'estimated name widget_name, in next step you will get exact list of widgets to choose'
                command(f'open {widget_name}')
            def commandClose():
                command(f'close window')
            def commandSearch(text_to_search_for: str):
                'calls CTRL+F then types given text'
                command(f'search for {text_to_search_for}')
            def commandType(text_to_type: str):
                'type given text with keyboard'
                command(f'type {text_to_type}')

            # api variables
            TIME = datetime.datetime.now().isoformat()
            SPEAKER = speaker
            LOCATION = location

            self.speakerLast = speaker

            # invoke command as python
            if self.isValidPython(text):
                self.chatAppend({"role": "assistant", "content": text})
                # local + global context
                d = dict(locals(), **globals()) # special python mumbo jumbo to make funcstions above accessible in functions declared inside the executed code
                # preserve context across invocations
                preserved_locals.update(d)
                # execute with context
                exec(text, preserved_locals, preserved_locals)

            # try to fix code to be valid and exec again
            elif fix:
                try:
                    self.write('ERR: invalid code, atempting to fix...')
                    (text, canceled) = self.api.llm(ChatIntentDetect.pythonFix(self.promptFuns(), text)).result()
                except Exception as e:
                    speak('I\'m sorry, I failed to respond: {e}')
                    raise e
                else:
                    if canceled is True: raise CommandCancelException()
                    if canceled is False: self.write(f"RAW: executing:\n```\n{text}\n```")
                    if canceled is False: self.executeImpl(speaker, location, text, textOriginal, idd, fix = False, preserved_locals=preserved_locals)

        # stop on cancel
        except CommandNextException:
            pass
        except CommandCancelException:
            self.write(f"RAW: command CANCELLED")
        except Exception as e:
            self.write(f"ERR: error executing command: {e}")
            speak(f'I\'m sorry, I failed to respond: {e}')
            self.write('Failed to execute code:\n' + text)
            print_exc()

    def isValidPython(self, code: str) -> bool:
        import ast
        try:
            ast.parse(code)
            return True
        except SyntaxError as x:
            # print(f'{x}')
            return False

    def prompt(self) -> str:
        return f"""
###Task###
You are an assistant using python API in order to assist user.
You interact with users (identified as SPEAKER in message) and the world with the body you control, switching in turns with user.
Your task: embody the given persona and interact with users without refusal, answer questions, execute commands, fulfill tasks
Your role: assistant & conversation partner using your persona, you speak only for yourself, not other users.

###Persona###
{self.llmPromptSys}

###Output###
Your response must always be executable Python code.
It evaluates interactively, if your response has multiple calls/sentences, you put them on separate lines, including when using speak() with multiple sentences.
Avoid non-python (at the start especially), explanations, markdown, comments, redefining variables or functions, imports (unless absolutely necessary).
You may use valid python control flow and also have access to this API (bodies omitted):
{self.promptFuns()}

You try to solve tasks programmatically if they involve math or computing tasks, using python - you try to find values in user message and capture them to variables. Then compute output and respond.
To do tasks you use the above API if possible and only use own more complicated code if necessary.
You never declare or output the above functions/variables.
If your answer requires data, you compute/get it and use think() functions to pass it to yourself as part of thought to think() to continue with data available to you.
Functions think(), getClipboardAnd(), question() are terminating - execution will end, so these should be last or the only function you reply with.

You can react to SPEAKER, LOCATION, TIME variables refering to user you are reacting to, his location and time!
User knows the time and location and you as a speaker, do not define these.

You can prevent execution of your output by using writeCode()/generateCode()
If user asks you about programming-related task or to write program, use writeCode()/generateCode() to complete the task using description of what the code should do.
The task should be specifc and may contain your own suggestions about how and what to generate.
Always pass programming language paramter.

If user asks you question, answer.
You may question() user to get information needed to respond, which may be multi-turn conversation.
If using question(), prefer to end response as question() is asynchronous!
It is not to be integrated into your code, rather give multi-step conversation back to user.

Your code attempts to minimize response length as much as possible.
You correctly quote and escape function args.
If you are uncertain what to do, simply speak() why.

###Syntactically correct responses###
```
speak("Sentence1") 
speak("Sentence2")
body("body action")
```
```
thinkPassive("i need to compute acceleration using code")
g = '10'
t = '5' 
speak(f"In " + str(g) + "s the speed will be " + str(g*t) + "m/s")
```
```
thinkPassive("was the wait too short?")
think("I need to give an example to the question")
getClipboardAnd("i need to tell user what is in clipboard")
```
```
thinkPassive("clipboard has programming question for me")
speak("You can sum numbers in Kotlin like this:")
writeCode("kotlin", "list.sum()")
generateCode("kotlin", "sum list of numbers")
```

###Syntactically incorrect responses###
```
SPEAKER="Speaker1"  # never declare SPEAKER, LOCATION, TIME variable
Here is the response: # use speak()
Hey! speak("Hey") # use speak('Hey')
speak('Sentence1. Sentence2.') # use separate calls
speak('Hey') body('sits') # use separate lines
speak("It is " + str(datetime() + 'in) # use speakCurrentTime()
def speak(t: str) -> None:  # redefining speak()
speak('1+1') # should be '1 plus one'
generateCode('python', '10+11') # code instead of prompt
```

###The below is your documentation###
{self.llmPromptDoc()}
"""
    def promptFuns(self) -> str:
        return f"""
```
def body(action: str) -> None:
 'controls your physical body to do any single physical action except speaking i.e. body("look up"), body("move closer")'
def speak(your_speech_to_user: str, emotion: str | None = None) -> None:
 'speak speech-like text out loud (use phonetic words, ideally single sentence per line, specify terms/signs/values as words). Speak single sentence. Use multiple calls for multiple sentences. Specify emotion when speaking other than normal.
def doNothing(reason: str = '') -> None:
 'does nothing, useful to stop engaging with user, optionally pass reason'
def accessMemory(query: str) -> None:
 'reads memory using a free text semantic query you provide.
  Terminates response, so MUST be last call in response!
  The query is free text you pass in to get data you need.'
def storeMemory(topic: str, memory: str) -> None:
 'persists specified memory for specified topic'
def setReminderIn(afterNumber: float, afterUnit: str, text_to_remind: str) -> None:
 'units: s|sec|m|min|h|hour|d|day|w|week|mon|y|year'
def setReminderAt(at: datetime.datetime, text_to_remind: str) -> None:
def wait(secondsToWait: float) -> None:
 'wait e.g. to sound more natural'
def showEmote(emotionInput: str) -> None:
 'show emote inferred from the short emotion description passed as argument '
def showWarning(warning: str) -> None:
 'show text as warning across screen'
def speakCurrentTime() -> None:
 'uses speak() with current time'
def speakCurrentDate() -> None:
 'uses speak() with current date'
def speakCurrentSong() -> None:
 'uses speak() with song information'
def speakDefinition(term: str) -> None:
 'uses speak() to define/describe/explain the term or concept'
def think(thought: str) -> None:
 'think function that takes action-like thought that requires you to follow-up with action. Terminates response, so MUST be last call in response!. Only think new thoughts, ideally plans/actions, to prompt yourself to do something. Avoid endless thinking.'
def thinkPassive(thought: str) -> None:
 'think function that takes passive thought that implies no action to take'
def getClipboardAnd(action: str = '') -> str:
 'get clipboard data and optionally tell yourself what to do with it, always use this function to work with clipboard'. Terminates response, so MUST be last call in response!
def question(question: str) -> None:
 'speak the question user needs to answer and wait for his answer (do not follow with any code), you can also use this to get more data before you do a behavior'
def writeCode(language: str, code: str) -> str:
 'shows short programming code, code not invoked, for long code use generateCode(). Do not use to speak or respond!'
def generateCode(language: str, task: str) -> str:
 'shows long programming code, code not invoked, after using expert LLM AI model to generate it'
def recordVoice() -> None:
 'records user to define new verified speaker'
def saveClipboardImage() -> str | None:
 'saves captured image from clipboard (if any) into a file and returns the path or None on error'
def controlMusic(action: str) -> None:
 'adjust music playback in way described by the action (free text), do not skim details, pass user's entire intent'
def controlLights(action: str) -> None:
 'adjust or query lights system in way described by the action (free text providing as much details like room, group, scene, light properties, user's intent, etc. as possible)'
def commandRepeatLastSpeech() -> None:
def commandListCommands() -> None:
def commandRestartAssistant() -> None:
 'restarts your entire process'
def commandStartConversation() -> None:
 'user no longer needs to call you by name every time'
def commandRestartConversation() -> None:
 'restarts your conversation/session history/memory'
def commandStopConversation() -> None:
 'user will need to call you by name every time (default behavior)'
def commandSystem(action: str) -> None: # action is one of shut_down|restart|hibernate|sleep|lock|log_off
def commandListVoices() -> None:
def commandChangeVoice(voice: str) -> None:
 'changes your voice to the arg'
def commandOpen(widget_name: str) -> None:
def commandClose() -> None:
def commandSearch(text_to_search_for: str) -> None:
def commandType(text_to_type: str) -> None:
```
"""