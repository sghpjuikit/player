
import sys
import atexit
import signal
import speech_recognition  # https://github.com/Uberi/speech_recognition
import whisper  # https://github.com/openai/whisper
import warnings
import time
import os
import psutil
import base64
import traceback
from threading import Thread
from typing import cast
from gpt4all import GPT4All  # https://docs.gpt4all.io/index.html
from util_tty_engines import Tty, TtyCharAi


# util: print with flush (avoids no console output)
def write(text):
    print(text, flush=True)


# util: print with flush (avoids no console output)
def write_ex(text, exception):
    print(text, exception, flush=True)


# util: arg parsing
def arg(arg_name, fallback):
    a = next((x for x in sys.argv if x.startswith(arg_name + '=')), None)
    if a is None:
        return fallback
    else:
        return a.split("=", 1)[-1]


showHelp = '--help' in sys.argv or '-h' in sys.argv
if showHelp:
    write("This is a speech recognition python script using OpenAI Whisper.")
    write("Prints recognized user speech in format `USER: $speech` and system log in format`SYS: $message`.")
    write("")
    write("This script prints:")
    write("    - system log in format`SYS: $message`")
    write("    - recognized user speech in format `RAW: $speech`")
    write("    - recognized user speech, sanitized, in format `USER: $speech`")
    write("")
    write("This scrip terminates:")
    write("    - upon `CTRL+C`")
    write("    - when parent process terminates, if launched with `parent-process=$pid` argument")
    write("    - when receives `EXIT` on input stream")
    write("It is possible to exit gracefully (on any platform) by writing 'EXIT' to stdin")
    write("")
    write("Args:")
    write("")
    write("  parent-process=$pid")
    write("    Optional parent process pid. This script terminates when the specified process terminates")
    write("    Default: None")
    write("")
    write("  wake-word=$wake-word")
    write("    Optional wake word to interact with this script")
    write("    Default: system")
    write("")
    write("  speaking-engine=$engine")
    write("    Engine for speaking. Use 'system' or 'character-ai'")
    write("    Default: system")
    write("")
    write("  character-ai-token=$token")
    write("    If speaking-engine=character-ai is used, authentication token for character.ai is required")
    write("    Default: None")
    write("")
    write("  character-ai-voice=$voice")
    write("    If speaking-engine=character-ai is used, optional voice id can be supplied")
    write("    Default: 22 (Anime Girl en-US)")
    write("")
    write("  speech-recognition-model=$model")
    write("    Whisper model for speech recognition")
    write("    Default: base.en.pt")
    write("")
    write("  chat-model=$model")
    write("    LLM model for chat or none for no LLM chat functionality")
    write("    Default: none")
    write("")
    quit()

parentProcess = int(arg('parent-process', -1))
wake_word = arg('wake-word', 'system')
name = wake_word[0].upper() + wake_word[1:]
speakUseCharAi = arg('speaking-engine', 'os')
speakUseCharAiToken = arg('character-ai-token', '')
speakUseCharAiVoice = int(arg('character-ai-voice', '22'))
speechRecognitionModelName = arg('speech-recognition-model', 'base.en.pt')
chatModelName = arg('chat-model', 'none')

intention_prompt = base64.b64decode(arg('intent-prompt', '')).decode('utf-8')
listening_for_chat_prompt = False
listening_for_chat_generation = False
terminating = False

# speaking actor, non-blocking
speaking = TtyCharAi(speakUseCharAiToken, speakUseCharAiVoice) if speakUseCharAi == 'character-ai' else Tty()


# noinspection SpellCheckingInspection
def speak(text, use_cache=True):
    if sys.platform == 'darwin':
        allowed_chars = set("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.,?!-_$:+-/ ")
        clean_text = ''.join(c for c in text if c in allowed_chars)
        os.system(f"say '{clean_text}'")
    else:
        speaking.speak(text, use_cache)


write('SYS: ' + name + ' initializing...')
speak(name + " initializing")


whisperModelDir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models-whisper")
whisperModel = whisper.load_model(os.path.join(whisperModelDir, speechRecognitionModelName))
r = speech_recognition.Recognizer()
listening = None
source = speech_recognition.Microphone()
warnings.filterwarnings("ignore", category=UserWarning, module='whisper.transcribe', lineno=114)


chatUseCharAi = False
chatModelDir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models-llm")
chatModel = os.path.join(chatModelDir, chatModelName)
chat = None if chatModelName == "none" else GPT4All(chatModel)
chatSession = None

# if __name__ == '__main__':
#     write('SYS: ' + name + ' INTENT DETECTOR initializing.')
#     speak(name + " INTENT DETECTOR initializing.")

# https://huggingface.co/glaiveai/glaive-function-calling-v1
commandTokenizer = None  # AutoTokenizer.from_pretrained("glaiveai/glaive-function-calling-v1", revision="6ade959", trust_remote_code=True)
commandModel = None  # AutoModelForCausalLM.from_pretrained("glaiveai/glaive-function-calling-v1", revision="6ade959", trust_remote_code=True).half().cuda()


# noinspection PyUnusedLocal
def callback(recognizer, audio):
    global chatSession
    global listening_for_chat_prompt
    global listening_for_chat_generation

    if not terminating:
        try:
            import uuid
            audio_file = os.path.join("cache", "user_" + str(uuid.uuid4()) + ".wav")
            text = ''
            try:
                with open(audio_file, "wb") as f:
                    f.write(audio.get_wav_data())
                    f.flush()
                text = whisperModel.transcribe(audio_file)['text']
            finally:
                os.remove(audio_file)

            # import numpy as np
            # result = modelWhisper.transcribe(np.frombuffer(audio.get_raw_data(), dtype=np.int16).astype(np.float32) / 32768.0)
            # text = result['text']

            text = text.lower().rstrip(".").strip()

            if len(text) > 0:
                write('')  # helps log separate event loops & sometimes previous loop is still running/writing
                write('RAW: ' + text)

            # ignore speech recognition noise
            if not text.startswith(wake_word):
                return

            # cancel any ongoing chat activity
            if listening_for_chat_generation:
                listening_for_chat_generation = False
                return

            # sanitize
            text = text.lstrip(wake_word).lstrip(",").rstrip(".").strip().replace(' the ', '').replace(' a ', '')
            write('USER: ' + text)

            # announcement
            if len(text) == 0:
                write("SYS: Yes. Please speak.")
                speak('Yes')

            # start LLM conversation
            elif not listening_for_chat_prompt and chat is not None and "start conversation" in text:

                # initialize chat
                chatSession = chat.chat_session()

                listening_for_chat_prompt = True
                write("SYS: Conversing. Please speak.")
                speak('Conversing')

            # if no speech, report to user
            elif listening_for_chat_prompt is True:

                # end LLM conversation
                if text.startswith("end conversation") or text.startswith("stop conversation"):
                    listening_for_chat_prompt = False
                    write("SYS: Ok")
                    speak("Ok")

                # do LLM conversation
                else:

                    # noinspection PyUnusedLocal
                    def stop_on_token_callback(token_id, token_string):
                        return listening_for_chat_generation

                    def generate(txt):
                        global listening_for_chat_generation
                        listening_for_chat_generation = True
                        text_tokens = chat.generate(
                            txt, streaming=True, n_batch=16, max_tokens=1000, callback=stop_on_token_callback
                        )

                        # generate & stream response
                        text_all = ''
                        print('CHAT: ', end='', flush=True)
                        for token in text_tokens:
                            print(token, end='', flush=True)
                            text_all = text_all + token
                        print('\n', flush=True)
                        txt = text_all

                        # if finished, speak
                        if listening_for_chat_generation:
                            speak(txt, False)
                        else:
                            chatSession.__exit__(None, None, None)

                    Thread(target=generate, args=(text,), daemon=True).start()

            # do command
            elif text.startswith("command"):
                # command_prompt = intention_prompt + text
                # command_input = commandTokenizer(command_prompt, return_tensors="pt").to(commandModel.device)
                # command_output = commandModel.generate(**command_input, do_sample=True, temperature=0.1, top_p=0.1, max_new_tokens=1000)
                # text = commandTokenizer.decode(command_output[0], skip_special_tokens=True)
                #
                write('SYS: ' + text.lstrip("command").strip())
                #
                # if "ASSISTANT: <functioncall>" in text:
                #     text = text.split("ASSISTANT: <functioncall>")[1]
                #     write('USER: ' + text)
                # else:
                #     write('SYS: No command detected')

            # do help
            elif text == "help":
                speak('I am an AI assistant. Talk to me by calling ' + wake_word + '.')
                speak('You can start conversation by saying ' + wake_word + ' start conversation.')
                speak('You can stop active conversation by saying ' + wake_word + 'stop or end conversation.')
                speak('You can ask for help by saying ' + wake_word + ' help.')
                speak('You can run command by saying ' + wake_word + ' command and saying the command.')

            # generic response
            else:
                write("SYS: Yes. Please speak.")
                speak('Yes')

        except Exception as e:
            traceback.print_exc()
            write_ex("Error: ", e)
            speak(name + " encountered an error. Please speak again.")


def start_listening():
    with source as s:
        r.adjust_for_ambient_noise(s, duration=1)

    global listening
    listening = r.listen_in_background(source, callback)

    write('SYS: ' + name + ' online.')
    speak(name + " online.")


# noinspection PyUnusedLocal
def stop(*args):
    global terminating
    if not terminating:
        terminating = True
        write('SYS: ' + name + ' offline.')
        speak(name + ' offline')

        if listening is not None:
            cast(callable, listening)(False)

        speaking.stop()


def start_exit_invoker():
    # wait until parent dies or listen forever
    while not terminating and (parentProcess == -1 or psutil.pid_exists(parentProcess)):
        time.sleep(1)
    sys.exit(0)


def install_exit_handler():
    atexit.register(stop)
    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGBREAK, stop)
    signal.signal(signal.SIGABRT, stop)


def start_input_handler():
    while True:
        try:
            m = input()
            if m.startswith("SAY: "):
                speak(base64.b64decode(m[5:]).decode('utf-8'))
            elif m == "EXIT":
                sys.exit(0)
        except EOFError as _:
            sys.exit(0)


install_exit_handler()
Thread(target=start_exit_invoker, daemon=True).start()
Thread(target=start_listening, daemon=True).start()
Thread(target=start_input_handler, daemon=False).start()
