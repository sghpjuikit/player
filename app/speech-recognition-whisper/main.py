
import sys
import atexit
import signal
import time
import os
import psutil
import base64
import traceback
from itertools import chain
from threading import Thread, Timer
from typing import cast
from util_play_engine import SdActor
from util_tty_engines import Tty, TtyNone, TtyOs, TtyOsMac, TtyCharAi, TtyCoqui
from util_llm import LlmNone, LlmGpt4All, LlmHttpOpenAi
from util_llm import ChatStart, Chat, ChatProceed, ChatIntentDetect, ChatStop
from util_mic import Mic, Whisper
from util_write_engine import Writer
from util_itr import teeThreadSafe, teeThreadSafeEager

# util: print engine actor, non-blocking
write = Writer()

# util: print ex with flush (avoids no console output)
def write_ex(text: str, exception):
    print(text, exception, flush=True)

# util: arg parsing
def arg(arg_name: str, fallback: str) -> str:
    a = next((x for x in sys.argv if x.startswith(arg_name + '=')), None)
    if a is None:
        return fallback
    else:
        return prop(a, arg_name, fallback)

# util: prop parsing
def prop(text: str, arg_name: str, fallback: str) -> str:
    if text.startswith(arg_name + '='):
        return text.split("=", 1)[-1]
    else:
        return fallback

# help
showHelp = '--help' in sys.argv or '-h' in sys.argv
if showHelp:
    write(
"""
This is a speech recognition python script using OpenAI Whisper.

This script outputs to stdout (token by token):
    - system log in format`SYS: $message`
    - recognized user speech in format `RAW: $speech`
    - recognized user speech, sanitized, in format `USER: $speech`
    - recognized user command, in format `COM: $command`

This script takes (optional) input:
    - `SAY-LINE: $line-of-text` and speaks it (if `speech-engine` is not `none`)
    - `SAY: $base64_encoded_text` and speaks it (if `speech-engine` is not `none`)
    - `CHAT: $base64_encoded_text` and send it to chat (if `llm-engine` is not `none`)

This scrip terminates:
    - upon `CTRL+C`
    - when parent process terminates, if launched with `parent-process=$pid` argument
    - when receives `EXIT` on input stream
It is possible to exit gracefully (on any platform) by writing 'EXIT' to stdin

Args:

  print-raw=$bool
    Optional bool whether RAW values should be print
    Default: True

  mic-on=$bool
    Optional bool whether microphone listening should be allowed.
    When false, speech recognition will receive no input and will not do anything.
    Interacting with the program is still fully possible through stdin `CHAT: ` command.
    Default: True
    
  mic-energy=$int(0,inf)
    Microphone energy treshold. ANything above this volume is considered potential voice.
    Use `mic-energy-debug=True` to figure out what value to use, as default may cause speech recognition to not work.
    Default: 120
    
  mic-energy-debug=$bool
    Optional bool whether microphone should be printing energy level.
    When true, microphone listening is not active. Use only to determine optimal energy treshold for voice detection.
    Default: False

  parent-process=$pid
    Optional parent process pid. This script terminates when the specified process terminates
    Default: None

  wake-word=$wake-word
    Optional wake word to interact with this script
    Default: system

  speech-recognition-model=$model
    Whisper model for speech recognition
    Values: tiny.en, tiny, base.en, base, small.en, small, medium.en, medium, large, large-v1, large-v2, large-v3
    Default: base.en

  speech-on=$bool
    Optional bool whether speech synthesis should be allowed.
    When false, speech synthesis will receive no input and will not do anything.
    Default: True

  speech-engine=$engine
    Engine for speaking.
    - Use 'none' for no text to speech
    - Use 'os' to use built-in OS text-to-speech (offline, high performance, low quality)
    - Use 'character-ai' to use character.ai online service client (requires login)
      requires specifying character-ai-token, optionally character-ai-voice
    - Use 'coqui' to use xttsv2 model (offline, low performance, realistic quality)
      optionally specify coqui-voice
    Default: os

  character-ai-token=$token
    If speaking-engine=character-ai is used, authentication token for character.ai is required
    Default: None

  character-ai-voice=$voice
    If speaking-engine=character-ai is used, optional voice id can be supplied
    Default: 22 (Anime Girl en-US)

  coqui-voice=$voice
    If speaking-engine=coqui is used, required name of voice file must be specified and exist in ./coqui-voices dir
    Default: Ann_Daniels.flac

  llm-engine=$engine
    Llm engine for chat. Use 'none', 'gpt4all', 'openai'
    - Use 'none' to disable chat
    - Use 'gpt4all' to use Gpt4All (https://github.com/nomic-ai/gpt4all/tree/main/gpt4all-bindings/python) python bindings
      requires specifying llm-gpt4all-model
    - Use 'openai' to use OpenAI http client (https://github.com/openai/openai-python), requires access or custom local server is also possible, e.g. LmStudio )
      requires specifying llm-openai-url, llm-openai-bearer, llm-openai-model
    Default: none

  llm-gpt4all-model=$model
    Name of the model in ./models-llm compatible with Gpt4All
    Default: none

  llm-openai-url=$url
    Url of the OpenAI or OpenAI-compatible server
    Default: none

  llm-openai-bearer=$bearer_token
    The user authorization of the OpenAI or OpenAI-compatible server
    Default: none

  llm-openai-model=$model
    The llm model of the OpenAI or OpenAI-compatible server
    Default: none

  llm-chat-sys-prompt=$prompt
    The llm model of the OpenAI or OpenAI-compatible server
    Default: 400
    
  llm-chat-max-tokens=$int
    The llm model of the OpenAI or OpenAI-compatible server
    Default: 400
    
  llm-chat-temp=$float(0.0-1.0)
    The llm model of the OpenAI or OpenAI-compatible server
    Default: 0.5

  llm-chat-topp=float(0.0-1.0)
    The llm model of the OpenAI or OpenAI-compatible server
    Default: 0.95

  llm-chat-topk=$int(1-inf)
    The llm model of the OpenAI or OpenAI-compatible server
    Default: 40
"""
    )
    quit()

# args
wake_word = arg('wake-word', 'system')
name = wake_word[0].upper() + wake_word[1:]
printRaw = arg('printRaw', "true")=="true"
parentProcess = int(arg('parent-process', -1))

micOn = arg('mic-on', "true")=="true"
micName = arg('mic-name', '')
micEnergy = int(arg('mic-energy', "120"))
micEnergyDebug = arg('mic-energy-debug', "false")=="true"

speakOn = arg('speech-on', "true")=="true"
speakEngineType = arg('speech-engine', 'os')
speakUseCharAiToken = arg('character-ai-token', '')
speakUseCharAiVoice = int(arg('character-ai-voice', '22'))
speakUseCoquiVoice = arg('coqui-voice', 'Ann_Daniels.flac')
speechRecognitionModelName = arg('speech-recognition-model', 'base.en')

llmEngine = arg('llm-engine', 'none')
llmGpt4AllModelName = arg('llm-gpt4all-model', 'none')
llmOpenAiUrl = arg('llm-openai-url', 'none')
llmOpenAiBearer = arg('llm-openai-bearer', 'none')
llmOpenAiModelName = arg('llm-openai-model', 'none')
llmSysPrompt = arg('llm-chat-sys-prompt', 'You are helpful chat bot. You are voiced by text-to-speech, so you are extremly concise.')
llmChatMaxTokens = int(arg('llm-chat-max-tokens', '400'))
llmChatTemp = float(arg('llm-chat-temp', '0.5'))
llmChatTopp = float(arg('llm-chat-topp', '0.95'))
llmChatTopk = int(arg('llm-chat-topk', '40'))

terminating = False
cache_dir = "cache"
if not os.path.exists(cache_dir): os.makedirs(cache_dir)

# speak engine actor, non-blocking
if speakEngineType == 'none':
    speakEngine = TtyNone()
elif speakEngineType == 'os' and sys.platform == 'darwin':
    speakEngine = TtyOsMac()
elif speakEngineType == 'os':
    speakEngine = TtyOs(write)
elif speakEngineType == 'character-ai':
    speakEngine = TtyCharAi(speakUseCharAiToken, speakUseCharAiVoice, SdActor(), write)
elif speakEngineType == 'coqui':
    speakEngine = TtyCoqui(speakUseCoquiVoice, SdActor(), write)
else:
    speakEngine = TtyNone()
speak = Tty(speakOn, speakEngine, write)

# llm actor, non-blocking
llm = LlmNone(speak, write)
if llmEngine == 'none':
    pass
elif llmEngine == "gpt4all":
    llmModelDir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models-llm")
    llmModel = os.path.join(llmModelDir, llmModelName)
    llm = LlmGpt4All(llmGpt4AllModelName, speak, write, llmSysPrompt, llmChatMaxTokens, llmChatTemp, llmChatTopp, llmChatTopk)
elif llmEngine == "openai":
    llm = LlmHttpOpenAi(llmOpenAiUrl, llmOpenAiBearer, llmOpenAiModelName, speak, write, llmSysPrompt, llmChatMaxTokens, llmChatTemp, llmChatTopp, llmChatTopk)
else:
    pass


class Assist:
    def __call__(self, text: str, textSanitized: str):
        pass

assist_last_at = time.time()
assist_last_diff = 0
assist = Assist()


class AssistChat:
    def __init__(self):
        # start
        llm(ChatStart())
        speak('Conversing')
    def __call__(self, text: str, textSanitized: str):
        # announcement
        if len(text) == 0: speak('Yes, conversation is ongoing')
        # do help
        elif text == "help":
            speak("Yes, we are in the middle of a conversation. Simply speak to me. To end the conversation, say stop, or end.")
        # end
        elif text.startswith("stop") or text.startswith("end"):
            llm.generating = False
            llm(ChatStop())
            speak.skip()
            speak("Ok")
            global assist
            assist = assistStand
        # normal
        else: llm(Chat(textSanitized))


class AssistEnVocab:
    def __init__(self):
        # start
        speak("Certainly. Say a word and explain what it means. I'll check if you understand it correctly. To end the excersize, say stop, or end.")
    def __call__(self, text: str, textSanitized: str):
        # announcement
        if len(text) == 0:
            speak("Yes, I'm testing your English vocabulary.")
        # end
        elif text.startswith("stop") or text.startswith("end"):
            speak("Ok")
            global assist
            assist = assistStand
        # do help
        elif text == "help":
            speak("Yes, say a word and explain what it means. I'll check if you understand it correctly. To end the excersize, say stop, or end.")
        # normal
        else:
            llm(ChatProceed("You are English Teacher evaluating user's understanding of a word. Be short. Criticize, but improve.", text))


class AssistStandard:
    def __call__(self, text: str, textSanitized: str):
        global assist
        # announcement
        if len(text) == 0:
            speak('Yes')

        # do greeting
        elif text == "hi" or text == "hello" or text == "greetings":
            speak(text.capitalize())

        # do help
        elif text == "help":
            speak.skippable(
                f'I am an AI assistant. Talk to me by calling {wake_word}. ' +
                f'Start conversation by saying, start conversation. ' +
                f'Stop active conversation by saying, stop or end conversation. ' +
                f'Ask for help by saying, help. ' +
                f'Run command by saying the command.'
            )
            write('COM: help')  # allows application to customize the help output

        # start LLM conversation
        elif "start conversation" in text:
            if isinstance(llm, LlmNone): speak('No conversation model is loaded')
            else: assist = AssistChat()

        # start en vocab excersize
        elif text == "start english vocabulary excersize":
            if isinstance(llm, LlmHttpOpenAi): assist = AssistEnVocab()
            else: speak('No supporting conversation model is loaded. Use OpenAI chat engine.')

        # do command
        else:
            write('COM: ' + text)

        # experimental:
        # do random activity
        # import random
        # import string
        # if assist_last_diff>5*60 and random.random() <= 0.1 and isinstance(llm, LlmHttpOpenAi):
        # if isinstance(llm, LlmHttpOpenAi):
        #     Timer(
        #         1, lambda: llm(ChatProceed(
        #             "You are pretending to be a character.",
        #             f"Say single short sentence, choose between:\n" +
        #             f"- Complain angrily that user haven't needed anything (if too long, last response was {assist_last_diff} seconds ago)" +
        #             f"- Mention passionately short trivia or interesting fact about random topic containing letter {random.choice(string.ascii_uppercase)}"
        #         ))
        #    ).start()


assistStand = AssistStandard()
assist = assistStand


def callback(text):
    if terminating: return

    textSanitized = text.rstrip(".").strip()
    text = text.lower().rstrip(".").strip()

    if len(text) > 0 and printRaw:
        write('RAW: ' + text)

    # ignore speech recognition noise
    if not text.startswith(wake_word): return

    # monitor activity time
    global assist_last_at
    assist_last_diff = time.time() - assist_last_at
    assist_last_at = time.time()

    # sanitize
    textSanitized = textSanitized.lstrip(wake_word).lstrip(",").rstrip(".").strip()
    text = text.lstrip(wake_word).lstrip(",").rstrip(".").strip().replace(' the ', ' ').replace(' a ', ' ')

    # cancel any ongoing activity
    if llm.generating: llm.generating = False
    speak.skip()

    # handle by active assistant state
    try:
        write(f'USER: {name}, ' + text)
        assist(text, textSanitized)
    except Exception as e:
        traceback.print_exc()
        write_ex("ERR: ", e)
        speak(name + " encountered an error. Please speak again.")


def start_exit_invoker():
    if parentProcess==-1:
        return

    def monitor():
        # wait until parent dies or listen forever
        while not terminating and psutil.pid_exists(parentProcess):
            time.sleep(1)
        sys.exit(0)

    Thread(name='Process-Monitor', target=monitor, daemon=True).start()


def stop(*args):  # pylint: disable=unused-argument
    global terminating
    if not terminating:
        terminating = True
        speak(name + ' offline')

        llm.stop()
        mic.stop()
        whisper.stop()
        speak.stop()
        write.stop()


def install_exit_handler():
    atexit.register(stop)
    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGBREAK, stop)
    signal.signal(signal.SIGABRT, stop)


whisper = Whisper(callback, micOn, speechRecognitionModelName)
mic = Mic(None if len(micName)==0 else micName, micOn, whisper.queue, speak, write, micEnergy, micEnergyDebug)
speak.start()
whisper.start()
mic.start()
llm.start()
install_exit_handler()
start_exit_invoker()
speak(name + " online")

while True:
    try:
        m = input()

        # talk command
        if m.startswith("SAY-LINE: "):
            text = m[11:]
            speak.skippable(text)

        # talk command
        if m.startswith("SAY: "):
            text = base64.b64decode(m[5:]).decode('utf-8')
            speak.skippable(text)

        # chat command
        if m.startswith("CHAT: "):
            text = base64.b64decode(m[6:]).decode('utf-8')
            llm(ChatStart)
            llm(Chat(text))

        if m.startswith("COM-DET: "):
            text = base64.b64decode(m[9:]).decode('utf-8')
            if isinstance(llm, LlmHttpOpenAi): llm(ChatIntentDetect(text))
            else: write('COM-DET: ' + text)

        if m.startswith("CALL: "):
            text = m[6:]
            callback(text)

        # changing settings commands
        elif m.startswith("mic-on="):
            mic.micOn = prop(m, "mic-on", "true").lower() == "true"
            whisper.whisperOn = mic.micOn

        elif m.startswith("mic-energy"):
            mic.micEnergy = int(prop(m, "mic-energy", "120"))

        elif m.startswith("mic-energy-debug"):
            mic.micEnergyDebug = prop(m, "mic-energy-debug", "false").lower() == "true"

        elif m.startswith("speech-on="):
            speak.speakOn = prop(m, "speech-on", "true").lower() == "true"

        elif m.startswith("print-raw="):
            printRaw = prop(m, "print-raw", "true").lower() == "true"

        elif m.startswith("coqui-voice=") and isinstance(speak.tty, TtyCoqui):
            speak.tty.voice = prop(m, "coqui-voice", speakUseCoquiVoice)
            speak(name + " voice changed")

        elif m.startswith("llm-chat-sys-prompt="):
            llm.sysPrompt = arg('llm-chat-sys-prompt', 'You are helpful voice assistant. You are voiced by tts, be extremly short.')

        elif m.startswith("llm-chat-max-tokens="):
            llm.maxTokens = int(prop(m, "llm-chat-max-tokens", "300"))

        elif m.startswith("llm-chat-temp="):
            llm.temp = float(prop(m, "llm-chat-temp", "0.5"))

        elif m.startswith("llm-chat-topp="):
            llm.topp = float(prop(m, "llm-chat-topp", "0.95"))

        elif m.startswith("llm-chat-topk="):
            llm.topk = int(prop(m, "llm-chat-topk", "40"))

        # exit command
        elif m == "EXIT":
            sys.exit(0)

    except EOFError as _:
        sys.exit(0)
    except Exception as e:
        write("ERR: Error occurred:" + str(e))
