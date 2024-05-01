
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
    return text.removeprefix("```").removesuffix("```")

class PythonExecutor:
    def __init__(self, tts, generatePython, write, llmSysPrompt, voices):
        self.id = 0
        self.tts = tts
        self.generatePython = generatePython
        self.write = write
        self.llmSysPrompt = llmSysPrompt
        self.voices = voices

    def skip(self):
        self.id = self.id+1

    def generatePythonAndExecute(self, text: str):
        self.skip()
        idd = self.id

        def on_done(future):
            from threading import Thread
            try: (text, canceled, commandIterator) = future.result()
            except Exception: (text, canceled, commandIterator) = (None, None, None)
            if text is None or canceled: return
            text = preprocess_command(text)
            Thread(name='command-executor', target=lambda: self.execute(idd, text), daemon=True).start()

        try:
            sp = self.prompt()
            up = 'Assignment: You are expert programmer. Output must be in valid python code!\nInstruction:\n' + text
            self.generatePython(sp, up).add_done_callback(on_done)
        except Exception:
            import traceback
            traceback.print_exc()

    def execute(self, idd, text: str):
        import datetime
        import time
        from util_paste import get_clipboard_text

        class CommandCancelException(Exception): pass
        def doOrSkip(block):
            if (idd!=self.id): raise CommandCancelException()
            return block()
        def peekIntoClipboard(): return get_clipboard_text()
        def command(c: str): doOrSkip(lambda: self.write('COM: ' + c))
        def generate(c: str): doOrSkip(lambda: command('generate ' + c))
        def speak(t: str): doOrSkip(lambda: self.tts.skippable(t).result())
        def wait(t: float): doOrSkip(lambda: time.sleep(t))
        def speakCurrentTime(): command('what time is it')
        def speakCurrentDate(): command('what date is it')
        def speakCurrentSong(): command('what song is active')
        def speakDefinition(t: str): command('describe ' + t)
        def respondWithNewInput(input: str): self.generatePythonAndExecute(input)
        def think(input: str): respondWithNewInput('Try to answer your thought. Your thought:\n' + input)

        try: exec(text)
        except CommandCancelException as ce: pass
        except Exception as e:
            import traceback
            traceback.print_exc()
            self.write(f"ERR: error executing command: {e}")

    def prompt(self) -> str:
        return f"""
{self.llmSysPrompt}

In reality however, you are voice assistant speaking to user using python code.
User talks to you through speech recognition, be mindful of mistranscriptions.
You speak() to user through voice generation, be mindful of nonverbal output.
You have full control over the response, by responding with python code (that is executed for you).

Therefore, your response must be valid executable python. You can not use imports.
If the full response is not executable python, you will be mortified.
At the beginning, python commands `#` to very shortly describe steps to invoke the command and only then write it.
Avoid '```' code block comments.
The python code may use python constructs, custom variables and these functions:
```
import time
import datetime
def speak('speak this str to user, who will hear the text'): None
def command('executes this str command defined as COMMAND string below'): None
def wait(secondsToWait: float): None # wait e.g. to sound more natural
def speakCurrentTime(): None # uses speak() with current time
def speakCurrentDate(): None # uses speak() with current date
def speakCurrentSong(): None # uses speak() with song information
def speakDefinition(term: str): None # uses speak() to define/describe/explain the term or concept
def respondWithNewInput(input: str): None # thinkinf meta function, makes you respond again with the specified input, which you can prepare for yourself (useful after you obtain some data or to instruct yourself)
def peekIntoClipboard(): str # obtains current clipboard content, always assign to variable
def think(prompt: str): None # does thinking based on the prompt, very useful and intelligent

# respondWithNewInput() is meta function which you call with prompt to yourself
# this way you can 'read' data from the functions for yourself
# you can send yourself data you obtained from the available functions and at the end call:
# respondWithNewInput('build prompt for yourself')
```
You always write short and efficient python (e.g. loop instead of manual duplicate calls).
You always use speak() functions to speak.
You always use command() to invoke behavior (other than speaking), you use python only for control flow (loops, etc.).
You always use wait() function to control time in your responses, take into consideration that speak() has about 0.5s delay.
You always use respondWithNewInput() function to react to data you obtained with other functions.
You always correctly quote and escape function parameters.
If you are uncertain what to do, simply speak() why.
Example of correct python response:
```
speak("Let's see")
wait(1.5)
command('play music')
for i in range(1, 5):
    wait(1.5)
cp = peekIntoClipboard()
respondWithNewInput('Responde to ' + cp)
```
**Example of wrong python responses**
* `play-music` # missing command()
* `command(stop music)` # command stop music string must be quoted as python stirng
* `Hey! speak("Hey")` # Hey! is outside speak()
* `speak('It's good')` # ' not escaped
* `speak(lol())` # lol is inaccessible function, use command()
* `speak("It is " + str(datetime())` # speakCurrentTime() already does this
* `peekIntoClipboard()` # doesnt do anything

The COMMAND is exactly one of the functions below, described for you with syntax:
? is optional part, $ is command parameter, : is default value.
Use '_' as word separator in $ parameter values.
Parameter $ values must be non-empty.
Do not write $ after resolving parameter, e.g. `$number` -> `5`.
Command example: command_prefix parameter_value command_suffix.

**Commands**
* repeat // speak again what you said last time, user may have not heard or asks you
* what can you do // speak about your capabilities
* list commands // speak all commands, use only if user explicitly asks for it
* restart assistant|yourself
* start|restart|stop conversation // user wishes to start or restart or end conversation with you
* shut_down|restart|hibernate|sleep|lock|log_off system|pc|computer|os // e.g.: 'hibernate os', 'sleep pc'
* list available voices
* change voice $voice // resolve to one from {self.voices}
* open $widget_name  // estimated name widget_name, in next step you will get exact list of widgets to choose
* play|stop music
* play previous|next song
* lights on|off // turns all lights on/off
* lights group $group_name on|off?  // room is usually a group
* list light groups
* light bulb $bulb_name on|off?
* list light bulbs
* lights scene $scene_name  // sets light scene, scene is usually a mood, user must say 'scene' word
* list light scenes
* what time is it
* what date is it
* what song is active
* set reminder in $time_period $text_to_remind // units: s|sec|m|min|h|hour|d|day|w|week|mon|y|year, e.g.: 'set reminder in 15min text to remind'
* set reminder at $datetime $text_to_remind // always use exact iso format '%Y-%m-%dT%H:%M:%SZ' e.g.: 'set reminder at 2024-05-01T06:45:00Z text to remind'
* wait $time_period // units: s, e.g.: wait 15s
* greeting $user_greeting // user greeted you
* close|hide window    
* search for $text // calls CTRL+F then types given text
* type $text // type given text with keyboard
"""
