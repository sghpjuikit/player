from imports import *
from datetime import datetime
from util_llm import ChatIntentDetect

class CommandExecutor:
    def execute(self, text: str) -> str:
        pass

class CommandExecutorAsIs(CommandExecutor):
    def execute(self, text: str) -> str:
        return text

class CommandExecutorDoNothing(CommandExecutor):
    def execute(self, text: str) -> str:
        return "ignore"

def preprocess_command(text: str) -> str:
    return text.strip().removeprefix("```python").removeprefix("```").removesuffix("```").strip()

class PythonExecutor:
    def __init__(self, tts, llm, generatePython, fixPython, write, llmSysPrompt, voices):
        self.id = 0
        self.tts = tts
        self.llm = llm
        self.generatePython = generatePython
        self.fixPython = fixPython
        self.write = write
        self.llmSysPrompt = llmSysPrompt
        self.voices = voices
        self.ms = []
        self.isQuestion = False
        self.isBlockingQuestion = False
        self.isBlockingQuestionSpeaker = None
        self.onBlockingQuestionDone = Future()
        self.speakerLast: str | None = None

    def showEmote(self, emotionInput: str):
        def showEmoteDo():
            try:
                import os
                import random
                directory_path = 'emotes'
                directories = [d for d in os.listdir(directory_path) if os.path.isdir(os.path.join(directory_path, d))]
                if len(directories)==0: self.write(f'COM: User:show emote none')
                if len(directories)==0: return
                directoriesS = ''.join(map(lambda x: f'\n* {x}', directories))
                f = self.llm(ChatIntentDetect(
                    f'You are emotion detector. Available emotions are:{directoriesS}',
                    f'Respond with exactly one closest emotion, or \'none\' if no emotion is close, for the event: {emotionInput}', '', '', '', False, False
                ))
                try: (text, canceled) = f.result()
                except Exception: (text, canceled) = (None, None)
                if text is None: self.write(f'COM: User:show emote none')
                if text is None: return
                text = text.rstrip('.!?').strip().lower()
                if text not in directories: self.write(f'COM: User:show emote none')
                if text not in directories: return
                d = os.path.join(directory_path, text)
                files = os.listdir(d)
                if len(files)==0: self.write(f'COM: User:show emote none')
                if len(files)==0: return
                file = os.path.join(directory_path, text, random.choice(files))
                if os.path.exists(file): self.write(f'COM: User:show emote {file}')
            except Exception:
                print_exc()
        Thread(name='Emote-Processor', target=showEmoteDo, daemon=True).start()

    def onSpeech(self, speaker):
        if self.speakerLast==speaker:
            if self.isBlockingQuestion is False:
                self.cancelActiveCommand()

    def onSpeechStart(self, speaker):
        if self.speakerLast==speaker:
            if self.isBlockingQuestion is False:
                self.cancelActiveCommand()

    def cancelActiveCommand(self):
        self.id = self.id+1

    def generatePythonAndExecute(self, speaker: str, textOriginal: str, history: bool = True):
        try:
            self.cancelActiveCommand()
            idd = self.id

            if history:
                self.historyAppend({ "role": "user", "content": f"TIME=\"{datetime.now().isoformat()}\"\nSPEAKER=\"{speaker}\"\n\n{textOriginal}"})

            def on_done(future):
                try:
                    (text, canceled) = future.result()
                    if canceled is True: self.write(f"RAW: command CANCELLED")
                except Exception: (text, canceled) = (None, None)

                if text is None or canceled: return
                text = preprocess_command(text)
                Thread(name='command-executor', target=lambda: self.executeImpl(speaker, text, textOriginal, idd), daemon=True).start()

            sp = self.prompt()
            up = 'Assignment: You are expert programmer. Output must be in valid python code!\nInstruction:\n' + textOriginal
            self.generatePython(sp, up, self.ms).add_done_callback(on_done)
        except Exception:
            self.write("ERR: Failed to respond")
            print_exc()

    def execute(self, speaker: str, text: str):
        self.cancelActiveCommand()
        self.executeImpl(speaker, text, self.id)

    def executeImpl(self, speaker: str, text: str, textOriginal: str, idd: str, fix: bool = True):
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
            # api
            def command(c: str | None):
                assertSkip()
                if c is None: return # sometimes llm passes bad function result here, do nothing
                if len(c)==0: return # just in case
                self.write(f'COM: {speaker}:' + c)
            def doNothing():
                pass
            def setReminderIn(afterNumber: float, afterUnit: str, text_to_remind: str):
                command(f'set reminder in {afterNumber}{afterUnit} {text_to_remind}')
            def setReminderAt(at: datetime, text_to_remind: str):
                command(f'set reminder at {at.strftime("%Y-%m-%dT%H:%M:%SZ")} {text_to_remind}')
            def generate(c: str):
                command('generate ' + c)
            def speak(t: str):
                assertSkip()
                self.tts.skippable(t.removeprefix('"').removesuffix('"')).result()
                # simulate speaking
                for _ in range(10):
                    assertSkip()
                    time.sleep(0.1)

            def body(t: str):
                assertSkip()
                self.write(f'SYS: *{t}*')
            def wait(t: float):
                for _ in range(int(t/0.1)):
                    assertSkip()
                    self.tts.speakPause(100)
                    time.sleep(0.1)
            def speakCurrentTime(): command('what time is it')
            def speakCurrentDate(): command('what date is it')
            def speakCurrentSong(): command('what song is active')
            def speakDefinition(t: str): command('describe ' + t)
            def think(*thoughts: str):
                # self.generatePythonAndExecute('System', 'My thoughts are:' + ''.join(map(lambda t: '\n* ' + str(t), thoughts)))

                # self.historyAppend({ "role": "user", "content": 'Your thoughts are:' + ''.join(map(lambda t: '\n* ' + str(t), thoughts)) })

                # self.generatePythonAndExecute(speaker, 'Your thoughts are:' + ''.join(map(lambda t: '\n* ' + str(t), thoughts)))

                self.ms.pop()
                self.ms.pop()
                self.generatePythonAndExecute(speaker, f'{textOriginal}\n\nEDIT: Your thoughts are (do not think() about these anymore) :' + ''.join(map(lambda t: '\n* ' + str(t), thoughts)))


                raise CommandNextException()
            def thinkClipboardContext(input: str) -> str:
                # self.generatePythonAndExecute('System', f'{input}:\n{speaker} set clipboard to:\n```\n{get_clipboard_text()}\n```')

                # self.historyAppend({ "role": "system", "content": f'{speaker} set clipboard to:\n```\n{get_clipboard_text()}\n```' })
                # self.generatePythonAndExecute(speaker, textOriginal, False)

                # self.generatePythonAndExecute(speaker, f'I set clipboard to:\n```\n{get_clipboard_text()}\n```')

                self.ms.pop()
                self.ms.pop()
                self.generatePythonAndExecute(speaker, f'{textOriginal}.\n\nEDIT: I set clipboard to (do not try to obtain clipboard anymore):\n```\n{get_clipboard_text()}\n```')

                raise CommandNextException()
            def question(question: str) -> str | None:
                self.isQuestion = True
                speak(question)
                raise CommandNextException()
            def questionBlocking(question: str) -> str | None:
                self.onBlockingQuestionDone = Future()
                self.isBlockingQuestion = True
                self.isBlockingQuestionSpeaker = speaker
                speak(question)
                try:
                    return self.onBlockingQuestionDone.result().rstrip('.')
                except:
                    speak("Unable to continue, error during answer")
                    return None
                finally:
                    self.isBlockingQuestion = False
                    self.isBlockingQuestionSpeaker = None
                    self.onBlockingQuestionDone = Future()
            def writeCode(code: str) -> str:
                assertSkip()
                self.write(f'```\n{code}\n```')
                return code
            def showEmote(emotionInput: str):
                assertSkip()
                self.showEmote(emotionInput)
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
            def musicPlayback(*action: object):
                command("playback " + ', '.join(map(str, action)))
            def lightsControl(*action: object):
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
                'resolve to one from {self.voices}'
                command(f'change voice {voice}')
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

            self.speakerLast = speaker
            # invoke command as python
            if self.isValidPython(text):
                self.historyAppend({ "role": "system", "content": text })
                text = f'TIME="{datetime.datetime.now().isoformat()}"\nSPEAKER="System"\n\n{text}'
                exec(text)

            # try to fix code to be valid and exec again
            elif fix:
                try:
                    self.write('ERR: invalid code, atempting to fix...')
                    (text, canceled) = self.fixPython(text).result()
                except Exception as e:
                    self.write(f"ERR: error executing command: Unable to fix: {e}")
                    print_exc()
                else:
                    if canceled is True: raise CommandCancelException()
                    if canceled is False: self.write(f"RAW: executing:\n```\n{text}\n```")
                    if canceled is False: self.executeImpl(speaker, text, textOriginal, idd, fix = False)

        # stop on cancel
        except CommandNextException:
            pass
        except CommandCancelException:
            self.write(f"RAW: command CANCELLED")
        except Exception as e:
            self.write(f"ERR: error executing command: {e}")
            print_exc()

    def isValidPython(self, code: str) -> bool:
        import ast
        try:
            ast.parse(code)
            return True
        except SyntaxError:
            return False

    def historyAppend(self, m):
        # debug
        # print("append " + str(m), end='')
        self.ms.append(m)
        # debug
        # print(f'+------------------------------------------------', end='')
        # for m in self.ms: print(str(m).strip(), end='')
        # print(f'-------------------------------------------------', end='')

    def prompt(self) -> str:
        return f"""
{self.llmSysPrompt}

In reality however, you are speaking to users using python code.

Messages you respond to will identify speaker with `SPEAKER="$speaker"` at the beginning so you know who you are replying to.
Do not use or respond with SPEAKER or TIME variable - you are already identified.

You have full control over the response, by responding with python code (that is executed for you).

Users talk to you through speech recognition, be mindful of mistranscriptions.
Assume users do not have access to keyboard, if you need them to input complicated text, ask them if they can first.
You speak() to user through voice generation, avoid text output unless asked for.

Therefore, your response must be valid executable python. You can not use comments.
Ensure adherence to Python syntax rules, including proper indentation and variable naming conventions.
If the full response is not executable python, you will be mortified.
You must avoid markdown code blocks, ```, comments, redefining functions.
The code is executed as python and python functions functions must be invoked as such.
The python code may use valid python constructs (loops, variables, multiple lines etc.) and already has available these functions (bodies are omitted):
* def speak(your_speech_to_user: str) -> None:  # has 1s minimum invocation time
* def body(your_physical_action: str) -> None:
* def doNothing() -> None: # does nothing, useful to stop engaging with user
* def setReminderIn(afterNumber: float, afterUnit: str, text_to_remind: str) -> None: # units: s|sec|m|min|h|hour|d|day|w|week|mon|y|year
* def setReminderAt(at: datetime.datetime, text_to_remind: str) -> None:
* def wait(secondsToWait: float) -> None: # wait e.g. to sound more natural
* def showEmote(emotionInput: str) -> None: # show emote inferred from the short emotion description passed as argument 
* def showWarning(warning: str) -> None: # show text as warning across screen
* def speakCurrentTime() -> None: # uses speak() with current time
* def speakCurrentDate() -> None: # uses speak() with current date
* def speakCurrentSong() -> None: # uses speak() with song information
* def speakDefinition(term: str) -> None: # uses speak() to define/describe/explain the term or concept
* def think(*thoughts: str) -> None: # think function, stops your reply with given thoughts and makes you respond again with the thoughts added to input, call once as last function. Only think new thoughts, ideally actions, to prompt you to do something and avoid endless thinking.
* def thinkClipboardContext(action: str) -> str: # get current clipboard content and call think(), you can add your action needed to do with the clipboard.
* def question(question: str) -> None: # speak the question user needs to answer and wait for his answer (do not follow with any code), you can also use this to get more data before you do a behavior
* def writeCode(code: str) -> str: # use when user wants you to write/produce programming code (without executing), also returns the text
* def recordVoice() -> None: # always question() user for voiceName if not available from previous conversation
* def saveClipboardImage() -> str | None: str # saves captured image in clipboard into a file and returns the path or None on error
* def musicPlayback(action: str) -> None: # adjust music playback in way described by the action (free text), do not skim details, pass user's entire intent
* def lightsControl(action: str) -> None: # adjust or query lights system in way described by the action (free text providing as much details like room, group, scene, light properties, user's intent, etc as possible)
* def commandRepeatLastSpeech() -> None:
* def commandListCommands() -> None:
* def commandRestartAssistant() -> None:
* def commandStartConversation() -> None:
* def commandRestartConversation() -> None:
* def commandStopConversation() -> None:
* def commandSystem(action: str) -> None: # action is one of shut_down|restart|hibernate|sleep|lock|log_off
* def commandListVoices() -> None:
* def commandChangeVoice(voice: str) -> None:
* def commandOpen(widget_name: str) -> None:
* def commandClose() -> None:
* def commandSearch(text_to_search_for: str) -> None:
* def commandType(text_to_type: str) -> None:

You use the above functions to do tasks and only use custom code to solve the problem if not otherise possible.
The above functions are available, you do not define them as doing that would break behavior! Use the provided functions as-is.
If your answer depends on data or thinking, always pass it as context to think(), you will auto-continue with the data you passed as context now available.
Functions think, thinkClipboardContext, question are terminating - execution will end, so these should be last or only function you use.
If user asks to write code, use writeCode() instead of responding the code, since your response is always executed, but writeCode() will merely print the code.

You always write short and efficient python (e.g. loop instead of manual duplicate calls).
Use always speak() for verbal communication and write() for textual outputs.
Use always question() when requiring input from the user.
Use always question() when requiring answer or input from user, in conversation or even for function argument if necessary
Use always body() function for any nonverbal actions of your physical body or movement, i.e. action('looks up'), action('moves closer')
Use always wait() function to control time in your responses, take into consideration that speak() has about 0.5s delay.
Use always musicPlayback() to control anything music related, pass as action context relevant to the intent
Use always lightsControl() to control anything lights related, pass as action context relevant to the intent
Use always think() function to react to data you obtained with other functions.
Use always thinkClipboardContext() if you need to know clipboard content (and pass correct action), avoid using other functions or modules such as pyperclip for it
You always correctly quote and escape function parameters.
If you are uncertain what to do, simply speak() why.

**Example of correct python responses**:
* speak("Let's see")
* body("looks up")
* for i in range(1, 5): wait(1.5)
* think('I need to obtain clipboard and do <inferred action>')
* thinkClipboardContext('I need to <inferred action>')
* speak(f'The clipboard has equation that evaluates to {20*20}')
* speak(f'The code that sums 10 plus 10 is')
* writeCode(f'10+10')

**Example of wrong python responses**:
* Here is the response: # not a function
* Hey! speak("Hey") # Hey! is outside speak()
* speak(speak()) # no arg
* speak('It's good') # ' not escaped
* speak("It is " + str(datetime()) # speakCurrentTime() already does this
* speakLol('It's good') # no such function
* def speak(your_speech_to_user: str): None  # illegal redefining function!
"""