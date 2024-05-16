from imports import *
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

class CommandExecutorDelegate(CommandExecutor):
    def __init__(self, commandExecutor: CommandExecutor):
        self.commandExecutor = commandExecutor

    def execute(self, text: str) -> str:
        return self.commandExecutor.execute(text)

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
        self.onQuestion = None

    def showEmote(self, emotionInput: str):
        def showEmoteDo():
            try:
                import os
                import random
                directory_path = 'emotes'
                directories = [d for d in os.listdir(directory_path) if os.path.isdir(os.path.join(directory_path, d))]
                if len(directories)==0: self.write(f'COM: show emote none')
                if len(directories)==0: return
                directoriesS = ''.join(map(lambda x: f'\n* {x}', directories))
                f = self.llm(ChatIntentDetect(
                    f'You detect closest emotion for input to one from list of emotions:{directoriesS}',
                    f'Respond with exactly one closest emotion from the list, as is, or \'none\' if no emotion is close, for the event: {emotionInput}', '', '', '', False, False
                ))
                try: (text, canceled, commandIterator) = f.result()
                except Exception: (text, canceled, commandIterator) = (None, None, None)
                if text is None: self.write(f'COM: show emote none')
                if text is None: return
                text = text.rstrip('.!?').strip().lower()
                if text not in directories: self.write(f'COM: show emote none')
                if text not in directories: return
                d = os.path.join(directory_path, text)
                files = os.listdir(d)
                if len(files)==0: self.write(f'COM: show emote none')
                if len(files)==0: return
                file = os.path.join(directory_path, text, random.choice(files))
                if os.path.exists(file): self.write(f'COM: show emote {file}')
            except Exception:
                print_exc()
        Thread(name='Emote-Processor', target=showEmoteDo, daemon=True).start()

    def skip(self):
        self.id = self.id+1

    def generatePythonAndExecute(self, text: str):
        try:
            self.skip()
            idd = self.id

            self.historyAppend({ "role": "user", "content": text })

            def on_done(future):
                try: (text, canceled, commandIterator) = future.result()
                except Exception: (text, canceled, commandIterator) = (None, None, None)
                if text is None or canceled: return
                text = preprocess_command(text)
                Thread(name='command-executor', target=lambda: self.executeImpl(text, idd), daemon=True).start()

            sp = self.prompt()
            up = 'Assignment: You are expert programmer. Output must be in valid python code!\nInstruction:\n' + text
            self.generatePython(sp, up, self.ms).add_done_callback(on_done)
        except Exception:
            print_exc()

    def execute(self, text: str):
        self.skip()
        self.executeImpl(text, self.id)

    def executeImpl(self, text: str, idd: str, fix: bool = True):
        try:
            import ast
            import datetime
            import time
            from util_paste import get_clipboard_text

            # plumbing
            class CommandCancelException(Exception): pass
            def assertSkip():
                if (idd!=self.id): raise CommandCancelException()
            def execCode(filename: str, **kwargs):
                try:
                    speak("Ok")
                    with open(f'{filename}.py', 'r') as file:
                        code = file.read() # expand code
                        for key, value in kwargs.items(): code = f'{key} = {repr(value)}\n{code}' # expand variables
                        exec(code) # exec code
                    speak("Done")
                except:
                    speak("Error")

            # api
            def command(c: str):
                assertSkip()
                self.write('COM: ' + c)
            def commandDoNothing():
                pass
            def commandSetReminderIn(afterNumber: float, afterUnit: str, text_to_remind: str):
                command(f'set reminder in {afterNumber}{afterUnit} {text_to_remind}')
            def commandSetReminderAt(at: datetime, text_to_remind: str):
                command(f'set reminder at {at.strftime("%Y-%m-%dT%H:%M:%SZ")} {text_to_remind}')
            def generate(c: str):
                command('generate ' + c)
            def speak(t: str):
                assertSkip()
                self.tts.skippable(t.removeprefix('"').removesuffix('"')).result()
                wait(1.0) # simulate speaking
            def body(t: str):
                assertSkip()
                self.write(f'SYS: *{t}*')
            def wait(t: float):
                for _ in range(int(t/0.1)):
                    assertSkip()
                    time.sleep(0.1)
            def speakCurrentTime(): command('what time is it')
            def speakCurrentDate(): command('what date is it')
            def speakCurrentSong(): command('what song is active')
            def speakDefinition(t: str): command('describe ' + t)
            def continueWithContext(input: str):
                self.generatePythonAndExecute(f'{input}')
                raise CommandCancelException()
            def continueWithClipboardContext(input: str) -> str:
                return continueWithContext(f'{input}:\n```{get_clipboard_text()}```')
            def continueWithQuestion(question: str):
                speak(question)
                self.onQuestion()
                raise CommandCancelException()
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
            def recordVoice(voiceName: str):
                speak('Recording will start in 3 seconds. Speak anything for 20 seconds.')
                wait(3.0)
                speak('Recording started')
                execCode('com_record', WAVE_OUTPUT_FILENAME_RAW=voiceName)
                speak('Recording finished')

            # invoke command as python
            if self.isValidPython(text):
                self.historyAppend({ "role": "system", "content": text })
                exec(text)
            # try to fix code to be valid and exec again
            elif fix:
                try:
                    self.write('ERR: invalid code, atempting to fix...')
                    (text, canceled, commandIterator) = self.fixPython(text).result()
                except Exception as e:
                    self.write(f"ERR: error executing command: Unable to fix: {e}")
                    print_exc()
                else:
                    if canceled is True: raise CommandCancelException()
                    if canceled is False: self.write(f"RAW: executing:\n```\n{text}\n```")
                    if canceled is False: self.executeImpl(text, idd, fix = False)

        # stop on cancel
        except CommandCancelException as ce:
            self.write(f"ERR: error executing command: CANCELLED")
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
        # print("append\n" + str(m))
        self.ms.append(m)
        # debug
        # print(f'+------------------------------------------------')
        # for m in self.ms: print(str(m).strip())
        # print(f'-------------------------------------------------')

    def prompt(self) -> str:
        return f"""
{self.llmSysPrompt}

In reality however, you are speaking to user using python code.
User talks to you through speech recognition, be mindful of mistranscriptions.
You speak() to user through voice generation, be mindful of nonverbal output.
You have full control over the response, by responding with python code (that is executed for you).

Therefore, your response must be valid executable python. You can not use your own imports or comments.
If the full response is not executable python, you will be mortified.
You must avoid markdown code blocks, ```, comments, redefining functions.
The python code may use valid python constructs (loops, variables, multiple lines etc.) and has available these functions:
* import datetime
* import time
* def speak(your_speech_to_user: str): None  # has 1s minimum invocation time
* def body(your_physical_action: str): None
* def command(COMMAND_below_to_execute: str): None
* def commandDoNothing(): None # does nothing, useful to stop engaging with user
* def commandSetReminderIn(afterNumber: float, afterUnit: str, text_to_remind: str): None # units: s|sec|m|min|h|hour|d|day|w|week|mon|y|year
* def commandSetReminderAt(at: datetime, text_to_remind: str): None
* def wait(secondsToWait: float): None # wait e.g. to sound more natural
* def showEmote(emotionInput: str): None # show emote inferred from the short emotion description passed as argument 
* def showWarning(warning: str): None # show text as warning across screen
* def speakCurrentTime(): None # uses speak() with current time
* def speakCurrentDate(): None # uses speak() with current date
* def speakCurrentSong(): None # uses speak() with song information
* def speakDefinition(term: str): None # uses speak() to define/describe/explain the term or concept
* def continueWithContext(context: str): None # think/meta/pipe function, stops current reply and makes you respond again with added context, call once as last function
* def continueWithClipboardContext(explanation: str): str # get current clipboard content and call continueWithContext(), you can add your explanation
* def continueWithQuestion(question: str): None # ask user for additional data
* def writeCode(code: str): str # use when user wants you to write/produce programming code (without executing), also returns the text
* def recordVoice(voiceName: str): None # continueWithQuestion() user for voiceName if not available from previous conversation

You always continueWithQuestion() user for arguments before calling function with parameters, unless you know the argument already.
If your answer depends on data or thinking, always pass it as context to continueWithContext(), you will auto-continue with the data you passed as context now available.

You always write short and efficient python (e.g. loop instead of manual duplicate calls).
You always use write() functions to write.
You always use speak() functions to speak.
You always use body() function for any nonverbal actions of your physical body or movement, i.e. action('looks up'), action('moves closer')
You always use command() to invoke behavior (other than speaking), you use python only for control flow (loops, etc.).
You always use wait() function to control time in your responses, take into consideration that speak() has about 0.5s delay.
You always use continueWithContext() function to react to data you obtained with other functions.
You always correctly quote and escape function parameters.
If you are uncertain what to do, simply speak() why.

**Example of correct python responses**
* speak("Let's see")
* command('play music')
* for i in range(1, 5): wait(1.5)
* continueWithClipboardContext('The clipboard is')
* speak(f'20 times 20 is {20 * 20}')

**Example of wrong python responses**
* play-music # missing command(), not a function
* command(stop music) # command stop music string must be quoted as python stirng
* Hey! speak("Hey") # Hey! is outside speak()
* speak(speak()) # no arg
* speak('It's good') # ' not escaped
* speak("It is " + str(datetime()) # speakCurrentTime() already does this
* speakLol('It's good') # no such function

The COMMAND is exactly one of the functions below, described for you with syntax:
? is optional part, $ is command parameter, : is default value.
Use '_' as word separator in $ parameter values.
Parameter $ values must be non-empty.
Do not write $ after resolving parameter, e.g. `$number` -> `5`.
Command example: command prefix parameter_value command suffix.

**Commands**
* 'repeat last speech' // speak again what you said last time, user may have not heard or asks you
* 'what can you do' // speak about your capabilities
* 'list commands' // speak all commands, use only if user specifically asks for all commands
* 'restart assistant|yourself'
* 'start|restart|stop conversation' // user wishes to start or restart or end conversation with you
* 'shut_down|restart|hibernate|sleep|lock|log_off system|pc|computer|os' // e.g.: 'hibernate os', 'sleep pc'
* 'list available voices'
* 'change voice $voice' // resolve to one from {self.voices}
* 'open $widget_name'  // estimated name widget_name, in next step you will get exact list of widgets to choose
* 'play|stop music'
* 'play previous|next|first|last song' // changes playback to previous|next song
* 'lights on|off' // turns all lights on/off
* 'lights group $group_name on|off?'  // room is usually a group
* 'list light groups'
* 'light bulb $bulb_name on|off?'
* 'list light bulbs'
* 'lights scene $scene_name'  // sets light scene, scene is usually a mood, user must say 'scene' word
* 'list light scenes'
* 'close|hide window'
* 'search for $text' // calls CTRL+F then types given text
* 'type $text' // type given text with keyboard
"""