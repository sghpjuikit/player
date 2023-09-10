from os import system
import atexit
import signal
import speech_recognition as sr
import sys
import whisper
import warnings
import time
import os
import psutil


# util: print with flush (avoids no console output)
def write(text):
    print(text, flush=True)


# util: print with flush (avoids no console output)
def writeEx(text, exception):
    print(text, exception, flush=True)


# util: arg parsing
def arg(name, fallback):
    a = next((x for x in sys.argv if x.startswith(name + '=')), None)
    if a is None:
        return fallback
    else:
        return a.split("=")[1]


showHelp = '--help' in sys.argv or '-h' in sys.argv
parentProcess = int(arg('parent-process', -1))
wake_word = arg('wake-word', 'mimi')
name = wake_word[0].upper() + wake_word[1:]
listening_for_wake_word = True


class _TTS:
    engine = None
    rate = None

    def __init__(self):
        import pyttsx3
        self.engine = pyttsx3.init()
        voices = self.engine.getProperty('voices')
        voiceZira = voices[1].id
        self.engine.setProperty('voice', voiceZira)

    def start(self, text_):
        self.engine.say(text_)
        self.engine.runAndWait()


def speak(text):
    if sys.platform == 'darwin':
        ALLOWED_CHARS = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,?!-_$:+-/ ")
        clean_text = ''.join(c for c in text if c in ALLOWED_CHARS)
        system(f"say '{clean_text}'")
    else:
        tts = _TTS()
        tts.start(text)
        del (tts)


if __name__ == '__main__':
    write('SYS: ' + name + ' initializing.')
    speak(name + " initializing.")

modelDir = os.path.join(os.path.dirname(os.path.abspath(__file__), "models"))
modelTiny = whisper.load_model(os.path.join(modelDir, 'tiny.en.pt'))
modelBase = whisper.load_model(os.path.join(modelDir, 'base.en.pt'))
r = sr.Recognizer()
source = sr.Microphone()
warnings.filterwarnings("ignore", category=UserWarning, module='whisper.transcribe', lineno=114)


def listen_for_wake_word(audio):
    global listening_for_wake_word
    with open("user_wake.wav", "wb") as f:
        f.write(audio.get_wav_data())
    result = modelTiny.transcribe('user_wake.wav')
    text_input = result['text']
    if wake_word in text_input.lower().strip():
        write("SYS: Yes. Please speak.")
        speak('Yes')
        listening_for_wake_word = False


def prompt_gpt(audio):
    global listening_for_wake_word
    try:
        with open("user_prompt.wav", "wb") as f:
            f.write(audio.get_wav_data())
        prompt_text = modelBase.transcribe('user_prompt.wav')['text']
        if prompt_text.strip().lower().replace('.', '') in ['never mind', 'stop', 'cancel', 'ignore']:
            write("SYS: Ok.")
            speak("Ok")
            listening_for_wake_word = True
        elif len(prompt_text.strip()) == 0:
            write("SYS: Empty prompt. Please speak again.")
            speak("Empty prompt. Please speak again.")
            listening_for_wake_word = True
        else:
            write('USER: ' + prompt_text)
            write('SYS: Say ' + wake_word + ' to wake me up.')
            listening_for_wake_word = True
    except Exception as e:
        writeEx("Error: ", e)


def callback(recognizer, audio):
    global listening_for_wake_word
    if listening_for_wake_word:
        listen_for_wake_word(audio)
    else:
        prompt_gpt(audio)


def start_listening():
    with source as s:
        r.adjust_for_ambient_noise(s, duration=2)

    write('SYS: ' + name + ' online. Say ' + wake_word + ' to wake me up.')
    speak(name + " online.")

    r.listen_in_background(source, callback)

    # wait until parent dies or listen forever
    while parentProcess == -1 or psutil.pid_exists(parentProcess):
        time.sleep(1)


def exit_handler(*args):
    write('SYS: ' + name + ' offline.')
    speak(name + ' offline')


def installExitHandler():
    atexit.register(exit_handler)
    signal.signal(signal.SIGTERM, exit_handler)
    signal.signal(signal.SIGINT, exit_handler)


if __name__ == '__main__':
    if showHelp:
        write("This is a speech recognition python script using OpenAI Whisper.")
        write("Prints recognized user speech in format `USER: $speech` and system log in format`SYS: $message`.")
        write("")
        write("Args:")
        write("  wake-word=$wake-word")
        write("    Optional wake word to interact with this script")
        write("  parent-process=$pid")
        write("    Optional parent process pid. This script terminates when the specified process terminates")
    else:
        installExitHandler()
        start_listening()
        exit_handler()
