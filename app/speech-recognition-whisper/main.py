import traceback
import base64
import psutil
import signal
import atexit
import time
import sys
import os
from imports import print_exc
from datetime import datetime
from threading import Timer
from itertools import chain
from util_http_handlers import HttpHandlerState, HttpHandlerStateActorEvents, HttpHandlerIntent, HttpHandlerStt, HttpHandlerSttReact
from util_tts import Tts, TtsNone, TtsOs, TtsCoqui, TtsHttp, TtsTacotron2, TtsSpeechBrain, TtsFastPitch
from util_llm import ChatProceed, ChatIntentDetect, ChatReact, ChatPaste
from util_stt import SttNone, SttWhisper, SttNemo, SttHttp, SpeechText
from util_mic import Mic, MicVoiceDetectNone, MicVoiceDetectNvidia
from util_llm import LlmNone, LlmGpt4All, LlmHttpOpenAi
from util_itr import teeThreadSafe, teeThreadSafeEager
from util_http import Http, HttpHandler
from util_play_engine import SdActor
from util_wrt import Writer
from util_actor import Actor
from util_paste import *
from util_com import *
from util_str import *


# print engine actor, non-blocking
write = Writer()
write.start()

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

  mic-enabled=$bool
    Optional bool whether microphone listening should be allowed.
    When false, speech recognition will receive no input and will not do anything.
    Interacting with the program is still fully possible through stdin `CALL: $command`.
    Default: True
    
  mic-energy=$int(0,inf)
    Microphone energy treshold. ANything above this volume is considered potential voice.
    Use `mic-energy-debug=True` to figure out what value to use, as default may cause speech recognition to not work.
    Default: 120
    
  mic-energy-debug=$bool
    Optional bool whether microphone should be printing real-time `mic-energy`.
    Use only to determine optimal `mic-energy`.
    Default: False

  mic-voice-detect=$bool
    Microphone voice detection.
    If true, detects voice from verified voices and ignores others.
    Verified voices must be 16000Hz wav in `voices-verified` directory.
    Use `mic-voice-detect-prop` to set up sensitivity.
    Default: False
    
  mic-voice-detect-prop=$int<0,1>
    Microphone voice detection treshold. Anything above this value is considered matched voice.
    Use `mic-voice-detect-debug` to determine optimal value.
    Default: 0.6
    
  mic-voice-detect-debug=$bool
    Optional bool whether microphone should be printing real-time `mic-voice-detect-prop`.
    Use only to determine optimal `mic-voice-detect-prop`.
    Default: False

  parent-process=$pid
    Optional parent process pid. This script terminates when the specified process terminates
    Default: None

  wake-word=$wake-word
    Optional wake words or phrases (separated by ',') that activate voice recognition. Case-insensitive.
    The first wake word will be used as name for the system
    Default: system

  stt-engine=$engine
    Speech-to-text engine for voice recognition
    - Use 'none' to disable voice recognition
    - Use 'whisper' to use OpenAI Whisper AI (https://github.com/openai/whisper), fully offline
      optionally specify stt-whisper-model, stt-whisper-device
    - Use 'nemo' to use Nvidia NEMO ASR AI (https://github.com/NVIDIA/NeMo), fully offline
      optionally specify stt-nemo-model, stt-nemo-device
    Default: whisper
    
  stt-whisper-model=$model
    Whisper model for speech recognition
    Values: tiny.en, tiny, base.en, base, small.en, small, medium.en, medium, large, large-v1, large-v2, large-v3
    Default: base.en

  stt-whisper-device
    Whisper torch device, e.g., cpu, cuda:0, cuda:1.
    Default: ''

  stt-nemo-model=$model
    Nemo model for speech recognition
    Values: nvidia/parakeet-tdt-1.1b, nvidia/parakeet-ctc-1.1b, nvidia/parakeet-ctc-0.6b
    Default: nvidia/parakeet-ctc-1.1b
    
  stt-nemo-device
    Nemo torch device, e.g., cpu, cuda:0, cuda:1.
    Default: ''

  speech-on=$bool
    Optional bool whether speech synthesis should be allowed.
    When false, speech synthesis will receive no input and will not do anything.
    Default: True

  speech-engine=$engine
    Engine for speaking.
    - Use 'none' for no text to speech
    - Use 'os' to use built-in OS text-to-speech (offline, high performance, low quality)
    - Use 'coqui' to use xttsv2 model (offline, low performance, realistic quality)
      optionally specify coqui-voice
    - Use 'tacotron2' to use tacotron2 (offline, realistic quality)
    - Use 'speechbrain' to use speechbrain (offline, realistic quality)
    - Use 'fastpitch' to use fastpitch (offline, fast, realistic quality)
    - Use 'http' to use another instance of this program to generate audio over http.
      requires the other instance to use coqui
      requires specifying the speech-server as the url of the other instance
    Default: os

  coqui-voice=$voice
    If speaking-engine=coqui is used, required name of voice file must be specified and exist in ./coqui-voices dir
    Default: Ann_Daniels.flac
  
  coqui-cuda-device=$device
    If speaking-engine=coqui is used, optionally cuda device index or empty string for auto.
    Default: ''
  
  speech-server=$host:$port
    If speaking-engine=http is used, host:port of the speech generation API of the other instance of this application
    Default: 'localhost:1236'

  llm-engine=$engine
    Llm engine for chat. Use 'none', 'gpt4all', 'openai'
    - Use 'none' to disable chat
    - Use 'gpt4all' to use Gpt4All (https://github.com/nomic-ai/gpt4all/tree/main/gpt4all-bindings/python) python bindings
      requires specifying llm-gpt4all-model
    - Use 'openai' to use OpenAI http client (https://github.com/openai/openai-python), requires access or custom local server is also possible, e.g. LmStudio )
      requires specifying llm-openai-url, llm-openai-bearer, llm-openai-model
    Default: none

  llm-gpt4all-model=$model
    Name of the model in ./models-gpt4all compatible with Gpt4All
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
    
  http-url=$host:port
    Url of the http API of the locally running AI executor
    Default: localhost:1236
    
  use-python-commands=bool
    Experimental flag to allow llm to repond with python commands. Much more powerful, but also somewhat unpredictable.
    Default: false
"""
    )
    quit()

# args
(name, wake_words) = wake_words_and_name(arg('wake-word', 'system'))
write(name + " booting up...")
sysParentProcess = int(arg('parent-process', -1))
sysTerminating = False
sysCacheDir = "cache"
if not os.path.exists(sysCacheDir): os.makedirs(sysCacheDir)

micEnabled = arg('mic-enabled', "true")=="true"
micName = arg('mic-name', '')
micEnergy = int(arg('mic-energy', "120"))
micVerbose = arg('mic-energy-debug', "false")=="true"
micVoiceDetect = arg('mic-voice-detect', "false")=="true"
micVoiceDetectTreshold = float(arg('mic-voice-detect-prop', "0.6"))
micVoiceDetectVerbose = arg('mic-voice-detect-debug', "false")=="true"

sttEngineType = arg('stt-engine', 'whisper')
sttWhisperDevice = arg('stt-whisper-device', '')
sttWhisperModel = arg('stt-whisper-model', 'base.en')
sttNemoDevice = arg('stt-nemo-device', '')
sttNemoModel = arg('stt-nemo-model', 'nvidia/parakeet-ctc-1.1b')
sttHttpUrl = arg('stt-http-url', 'localhost:1235')

ttsOn = arg('speech-on', "true") == "true"
ttsEngineType = arg('speech-engine', 'os')
ttsCoquiVoice = arg('coqui-voice', 'Ann_Daniels.flac')
ttsCoquiCudaDevice = arg('coqui-cuda-device', '')
ttsTacotron2Device = arg('tacotron2-cuda-device', '')
ttsHttpUrl = arg('speech-server', 'localhost:1236')

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

httpUrl = arg('http-url', 'localhost:1236')
usePythonCommands = arg('use-python-commands', 'false')=="true"


# speak engine actor, non-blocking
if ttsEngineType == 'none':
    speakEngine = TtsNone(write)
elif ttsEngineType == 'os':
    speakEngine = TtsOs(SdActor(write), write)
elif ttsEngineType == 'coqui':
    speakEngine = TtsCoqui(ttsCoquiVoice, "cuda" if len(ttsCoquiCudaDevice)==0 else ttsCoquiCudaDevice, SdActor(write), write)
elif ttsEngineType == 'tacotron2':
    speakEngine = TtsTacotron2("cuda" if len(ttsTacotron2Device)==0 else ttsTacotron2Device, SdActor(write), write)
elif ttsEngineType == 'speechbrain':
    speakEngine = TtsSpeechBrain("cuda" if len(ttsTacotron2Device)==0 else ttsTacotron2Device, SdActor(write), write)
elif ttsEngineType == 'http':
    if len(ttsHttpUrl)==0: raise AssertionError('speech-engine=http requires speech-server to be specified')
    ttsHttpUrl = ttsHttpUrl.removeprefix("http://").removeprefix("https://")
    if ':' not in ttsHttpUrl: raise AssertionError('speech-server must be in format host:port')
    host, _, port = ttsHttpUrl.partition(":")
    speakEngine = TtsHttp(host, int(port), SdActor(write), write)
elif ttsEngineType == 'fastpitch':
    speakEngine = TtsFastPitch("cuda" if len(ttsTacotron2Device)==0 else ttsTacotron2Device, SdActor(write), write)
else:
    speakEngine = TtsNone(write)
tts = Tts(ttsOn, speakEngine, write)

# commands
commandExecutor = CommandExecutorDelegate(CommandExecutorDoNothing)

# llm actor, non-blocking
llm = LlmNone(tts, write, commandExecutor.execute)
if llmEngine == 'none':
    pass
elif llmEngine == "gpt4all":
    llm = LlmGpt4All(
        llmGpt4AllModelName, "models-gpt4all",
        tts, write, commandExecutor.execute,
        llmSysPrompt, llmChatMaxTokens, llmChatTemp, llmChatTopp, llmChatTopk
    )
elif llmEngine == "openai":
    llm = LlmHttpOpenAi(
        llmOpenAiUrl, llmOpenAiBearer, llmOpenAiModelName,
        tts, write, commandExecutor.execute,
        llmSysPrompt, llmChatMaxTokens, llmChatTemp, llmChatTopp, llmChatTopk
    )
else:
    pass


# assist
class Assist:
    def __call__(self, text: str, textSanitized: str):
        pass


if isinstance(tts.tts, TtsCoqui):
    voices_dir = 'voices-coqui'
    voices = [f for f in os.listdir(voices_dir) if os.path.isfile(os.path.join(voices_dir, f)) and not f.endswith('.txt')]
else:
    voices = []


assist = Assist()

# commands
class CommandExecutorMain(CommandExecutor):

    def execute(self, text: str) -> str:
        global assist
        handled = "ignore"

        if text == "repeat last speech":
            tts.repeatLast()
            return handled
        if text.startswith("speak "):
            return text
        if text.startswith("do-speak "):
            tts(text.removeprefix("do speak "))
            return handled
        if text == "list available voices":
            tts("The available voices are: " + ', '.join(voices))
            return handled
        if text.startswith("greeting "):
            g = text.removeprefix("greeting ").capitalize()
            llm(ChatReact(llmSysPrompt, "User greeted you with " + g, g))
            return handled
        if text.startswith("change voice "):
            voice = text.removeprefix("change voice ")
            if isinstance(tts.tts, TtsCoqui):
                if voice in voices:
                    if tts.tts.voice != voice:
                        global ttsCoquiVoice
                        voiceOld = ttsCoquiVoice
                        ttsCoquiVoice = voice
                        tts.tts.voice = voice
                        voiceNew = ttsCoquiVoice
                        llm(ChatReact(llmSysPrompt, f"User changed your voice from:\n```\n{voiceOld}\n```\n\nto:\n```\n{voiceNew}\n```", name + " voice changed"))
                else:
                    tts(f"No voice {voice} available")
            return handled
        if text.startswith("generate from clipboard "):
            tts("Ok")
            llm(ChatPaste("generate " + text.removeprefix("generate from clipboard ") + "\nClipboard:\n```" + get_clipboard_text() + "\n```"))
            return handled
        if text.startswith("generate "):
            tts("Ok")
            llm(ChatPaste(text))
            return handled
        if text.startswith("do-describe "):
            llm(ChatProceed(llmSysPrompt, "Describe the following content:\n" + text.removeprefix("do-describe ")))
            return handled
        elif text == 'start conversation':
            assist.startChat()
            return handled
        elif text == 'restart conversation':
            assist.restartChat()
            return handled
        elif text == 'stop conversation':
            assist.stopChat()
            return handled
        else:
            return text

commandExecutor.commandExecutor = CommandExecutorMain()
executorPython = PythonExecutor(
    tts, llm,
    lambda sp, up, ms: llm(ChatIntentDetect.python(sp, up, ms)),
    lambda code: llm(ChatIntentDetect.pythonFix(code)),
    write, llmSysPrompt, ', '.join(voices)
)

class AssistBasic:
    def __init__(self):
        self.last_announcement_at = datetime.now()
        self.wake_word_delay = 5.0
        self.isChat = False
        self.activity_last_at = time.time()
        self.activity_last_diff = 0
        self.restartChatDelay = 5*60

        executorPython.onQuestion = self.onQuestion
        executorPython.llm = llm
        llm.api = executorPython

        Thread(name='Assist-Idle-Monitor', target=lambda: self.assistUpdateIdle(), daemon=True).start()

    def onQuestion(self):
        self.last_announcement_at = datetime.now()

    def assistUpdateIdle(self):
        while True:
            time.sleep(1.0)
            if self.restartChatDelay < (time.time() - self.activity_last_at) and len(executorPython.ms)>0:
                self.restartChat(react=False)

    def needsWakeWord(self, speech_start: datetime) -> bool:
        return self.isChat is False and (speech_start - self.last_announcement_at).total_seconds() > self.wake_word_delay

    def __call__(self, text: str, textSanitized: str):
        # announcement
        if len(text) == 0:
            self.last_announcement_at = datetime.now()
            if self.isChat: llm(ChatReact(llmSysPrompt, "User said your name - say you are still conversing. Say 2 words at most", "Yes, we are talking"))
            else: llm(ChatReact(llmSysPrompt, "User said your name - are you there? Say 2 words at most", "Yes"))
        # do greeting
        elif text == "hi" or text == "hello" or text == "greetings":
            commandExecutor.execute(f"greeting {text}")
        # cancel
        elif text == "ok" or text == "okey" or text == "whatever" or text == "stop":
            tts.skip()
            llm.generating = False
        # do help
        elif text == "help":
            if self.isChat:
                tts(
                    "Yes, we are in the middle of a conversation. Simply speak to me. "
                    "To end the conversation, say stop, or end conversation."
                    "To restart the conversation, say restart or reset conversation."
                    "To cancel ongoing reply, say okey, whatever or stop."
                )
            else:
                tts.skippable(
                    f'I am an AI assistant. Talk to me by calling {name}. ' +
                    f'Start conversation by saying, start conversation. ' +
                    f'Ask for help by saying, help. ' +
                    f'Run command by saying the command.'
                )
                commandExecutor.execute("help") # allows application to customize the help output
        elif text == "start conversation":
            commandExecutor.execute("start conversation")
        elif text == "restart conversation" or text == "reset conversation":
            commandExecutor.execute("restart conversation")
        elif text == "stop conversation":
            commandExecutor.execute("stop conversation")
        elif text.startswith("generate "):
            write('COM: ' + commandExecutor.execute(text))
        elif text.startswith("count "):
            write('COM: ' + commandExecutor.execute(text))
        # do command - python
        elif usePythonCommands:
            executorPython.generatePythonAndExecute(text)
        # do command
        else:
            write('COM: ' + commandExecutor.execute(text))

    def startChat(self, react: bool = True):
        if self.isChat: return
        self.isChat = True
        write("COM: start conversation")
        if (react): llm(ChatReact(llmSysPrompt, "User started conversation with you. Greet him", "Conversing"))
        mic.set_pause_threshold_talk()

    def restartChat(self, react: bool = True):
        tts.skip()
        llm.generating = False
        write("COM: restart conversation")
        if (react): llm(ChatReact(llmSysPrompt, "User erased his conversation with you from your memory.", "Ok"))
        executorPython.ms = []

    def stopChat(self, react: bool = True):
        if self.isChat is False: return
        self.isChat = False
        tts.skip()
        llm.generating = False
        write("COM: stop conversation")
        if (react): llm(ChatReact(llmSysPrompt, "User stopped conversation with you", "Ok"))
        mic.set_pause_threshold_normal()

assist = AssistBasic()

def skipWithoutSound():
    executorPython.skip()
    if llm.generating: llm.generating = False
    tts.skipWithoutSound()

def skip():
    executorPython.skip()
    if llm.generating: llm.generating = False
    tts.skip()

def callback(st: SpeechText):
    if sysTerminating: return

    # sanitize
    text = st.text
    textSanitized = text.rstrip(".").strip()
    text = text.lower().rstrip(".").strip()

    # ignore no input
    if len(text) == 0: return


    # ignore speech recognition noise
    if assist.needsWakeWord(st.start) and not starts_with_any(text, wake_words):
        write(f'USER-RAW: {text}')
        return

    # monitor activity time
    assist.activity_last_diff = time.time() - assist.activity_last_at
    assist.activity_last_at = time.time()

    # sanitize
    textSanitized = remove_any_prefix(textSanitized, wake_words).strip().lstrip(",").lstrip(".").rstrip(".").strip()
    text = remove_any_prefix(text, wake_words).strip().lstrip(",").lstrip(".").rstrip(".").strip().replace(' the ', ' ').replace(' a ', ' ')

    # cancel any ongoing activity
    skipWithoutSound()

    # handle by active assistant state
    try:
        write(f'USER: {name}' + (', ' + text if len(text)>0 else ''))
        if text == "repeat": commandExecutor.execute(text)
        else: assist(text, textSanitized)
    except Exception as e:
        write(f"ERR: {e}")
        print_exc()
        tts(name + " encountered an error. Please speak again or check logs for details.")

def start_exit_invoker():
    if sysParentProcess==-1: return

    def waitAndStopWithParentProcess():
        while not sysTerminating and psutil.pid_exists(sysParentProcess): time.sleep(1)
        stop()

    Thread(name='Process-Monitor', target=waitAndStopWithParentProcess, daemon=True).start()


def stop(*args):  # pylint: disable=unused-argument
    global sysTerminating
    if not sysTerminating:
        sysTerminating = True
        tts(name + ' offline')

        llm.stop()
        mic.stop()
        stt.stop()
        tts.stop()
        if http is not None: http.stop()
        write.stop()


def install_exit_handler():
    atexit.register(stop)
    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGBREAK, stop)
    signal.signal(signal.SIGABRT, stop)


stt = SttNone(micEnabled, write)
if sttEngineType == 'whisper':
    stt = SttWhisper(micEnabled, "cpu" if len(sttWhisperDevice)==0 else sttWhisperDevice, sttWhisperModel, write)
elif sttEngineType == 'nemo':
    stt = SttNemo(micEnabled, "cpu" if len(sttNemoDevice)==0 else sttNemoDevice, sttNemoModel, write)
elif sttEngineType == 'http':
    sttHttpUrl = sttHttpUrl.removeprefix("http://").removeprefix("https://")
    if ':' not in sttHttpUrl: raise AssertionError('stt-http-url must be in format host:port')
    host, _, port = sttHttpUrl.partition(":")
    stt = SttHttp(host, int(port), micEnabled, "cpu" if len(sttNemoDevice)==0 else sttNemoDevice, sttNemoModel, write)
else:
    pass
stt.onDone = callback

def micSpeakerDiarLoader():
    if not micVoiceDetect: return MicVoiceDetectNone()
    else: return MicVoiceDetectNvidia(micVoiceDetectTreshold, micVoiceDetectVerbose)
mic = Mic(None if len(micName)==0 else micName, micEnabled, stt.sample_rate, lambda e: skip(), lambda e: stt(e), tts, write, micEnergy, micVerbose, micSpeakerDiarLoader)
actors: [Actor] = list(filter(lambda x: x is not None, [write, mic, stt, llm, tts.tts, tts.tts.play if hasattr(tts.tts, 'play') else None]))

# http
http = None
if ':' not in httpUrl: raise AssertionError('http-url must be in format host:port')
host, _, port = httpUrl.partition(":")
http = Http(host, int(port), write)
http.handlers.append(HttpHandlerState(actors))
http.handlers.append(HttpHandlerStateActorEvents(actors))
http.handlers.append(HttpHandlerIntent(llm))
http.handlers.append(HttpHandlerStt(stt))
http.handlers.append(HttpHandlerSttReact(llm, llmSysPrompt))
if isinstance(tts.tts, TtsCoqui): http.handlers.append(tts.tts._httpHandler())

# start actors
if http is not None: http.start()
tts.start()
stt.start()
mic.start()
llm.start()
install_exit_handler()
start_exit_invoker()

def onBootup():
    while True:
        if all(actor.state() == "ACTIVE" for actor in actors): break
        else: sleep(0.1)
    llm(ChatReact(llmSysPrompt, "You booted up", f"{name} online"))

Thread(name='on-bootup', target=onBootup, daemon=True).start()

while not sysTerminating:
    try:
        m = input()

        # talk command
        if m.startswith("SAY-LINE: "):
            text = m[10:]
            tts.skippable(text)

        # talk command
        if m.startswith("SAY: "):
            text = base64.b64decode(m[5:]).decode('utf-8')
            tts.skippable(text)

        # chat command
        if m.startswith("CHAT: "):
            text = base64.b64decode(m[6:]).decode('utf-8')
            text = remove_any_prefix(text, wake_words)
            text = name + ' ' + text
            now = datetime.now()
            callback(SpeechText(now, None, now, text))

        if m.startswith("COM-PYT: "):
            text = base64.b64decode(m[9:]).decode('utf-8')
            executorPython.execute(text)

        if m.startswith("COM-PYT-INT: "):
            text = base64.b64decode(m[13:]).decode('utf-8')
            executorPython.generatePythonAndExecute(text)

        if m.startswith("CALL: "):
            text = m[6:]
            text = remove_any_prefix(text, wake_words)
            text = name + ' ' + text
            now = datetime.now()
            callback(SpeechText(now, None, now, text))

        if m.startswith("COM: "):
            text = base64.b64decode(m[5:]).decode('utf-8')
            commandExecutor.execute(text)

        # changing settings commands
        elif m.startswith("wake-word="):
            (name, wake_words) = wake_words_and_name(prop(m, 'wake-word', 'system'))

        elif m.startswith("mic-enabled="):
            e = prop(m, "mic-enabled", "true").lower() == "true"
            mic.enabled = e
            stt.enabled = e

        elif m.startswith("mic-energy="):
            micEnergy = int(prop(m, "mic-energy", "120"))
            mic.energy_threshold = micEnergy

        elif m.startswith("mic-energy-debug="):
            micVerbose = prop(m, "mic-energy-debug", "false").lower() == "true"
            mic.energy_debug = micVerbose

        elif m.startswith("mic-voice-detect-prop="):
            micVoiceDetectTreshold = float(prop(m, "mic-voice-detect-prop", "0.6"))
            if micVoiceDetect: mic.speaker_diar.speaker_treshold = micVoiceDetectTreshold

        elif m.startswith("mic-voice-detect-debug="):
            micVoiceDetectVerbose = prop(m, "mic-voice-detect-debug", "false").lower() == "true"
            if micVoiceDetect: mic.speaker_diar.verbose = micVoiceDetectVerbose

        elif m.startswith("speech-on="):
            tts.enabled = prop(m, "speech-on", "true").lower() == "true"

        elif m.startswith("coqui-voice=") and isinstance(tts.tts, TtsCoqui):
            commandExecutor.execute("change voice " + prop(m, "coqui-voice", ttsCoquiVoice))

        elif m.startswith("llm-chat-sys-prompt="):
            promptOld = llmSysPrompt
            llmSysPrompt = prop(m, 'llm-chat-sys-prompt', 'You are helpful voice assistant. You are voiced by tts, be extremly short.')
            llm.sysPrompt = llmSysPrompt
            if promptOld!=llmSysPrompt:
                llm(ChatReact(llmSysPrompt, f"User changed your system prompt from:\n```\n{promptOld}\n```\n\nto:\n```\n{llmSysPrompt}\n```", name + " prompt changed"))

        elif m.startswith("llm-chat-max-tokens="):
            llm.maxTokens = int(prop(m, "llm-chat-max-tokens", "300"))

        elif m.startswith("llm-chat-temp="):
            llm.temp = float(prop(m, "llm-chat-temp", "0.5"))

        elif m.startswith("llm-chat-topp="):
            llm.topp = float(prop(m, "llm-chat-topp", "0.95"))

        elif m.startswith("llm-chat-topk="):
            llm.topk = int(prop(m, "llm-chat-topk", "40"))

        elif m.startswith("use-python-commands="):
            usePythonCommandsOld = usePythonCommands
            usePythonCommands = prop(m, "use-python-commands", "false")=="true"
            if usePythonCommandsOld!=usePythonCommands:
                llm(ChatReact(llmSysPrompt, f"User {'increased' if usePythonCommands else 'decreased'} your output expressivity", name + " prompt changed"))


        # exit command
        elif m == "EXIT":
            stop()

    except EOFError as _:
        stop()
    except Exception as e:
        write("ERR: Error occurred:" + str(e))
