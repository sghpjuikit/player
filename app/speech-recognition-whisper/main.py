import util_ctx
from imports import print_exc
from datetime import datetime
from threading import Timer
from itertools import chain
from util_http_handlers import *
from util_tts import Tts, TtsNone, TtsOs, TtsCoqui, TtsHttp, TtsTacotron2, TtsSpeechBrain, TtsFastPitch
from util_llm import ChatProceed, ChatIntentDetect, ChatReact, ChatPaste
from util_stt import SttNone, SttWhisper, SttFasterWhisper, SttWhisperS2T, SttNemo, SttHttp, SpeechText
from util_mic import Mic, Vad, MicVoiceDetectNone, MicVoiceDetectNvidia, SpeechStart, Speech
from util_llm import LlmNone, LlmGpt4All, LlmHttpOpenAi
from util_itr import teeThreadSafe, teeThreadSafeEager
from util_http import Http, HttpHandler
from util_wrt import Writer
from util_actor import Actor
from util_paste import *
from util_com import *
from util_now import *
from util_str import *
from util_num import *
from util_ctx import *
import faulthandler
import threading
import traceback
import base64
import psutil
import signal
import atexit
import time
import json
import sys
import os

# install_crash_logger
faulthandler.enable()

# print engine actor, non-blocking
write = Writer()
write.start()
write.busy_status.add(1)

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
    - recognized user command, in format `COM: $SPEAKER:$LOCATION:$command`

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

  main-speaker=$txt
    Speaker sent to assistant as context when none other available.
    Speaker is usually determined from name of the matched voice from verified voices.
    Default: 'User'

  main-location=$txt
    Location sent to assistant as context when none other available. Location is usually determined from microphone location.
    Default: 'PC'

  mic-enabled=$bool
    Optional bool whether microphone listening should be allowed.
    When false, speech recognition will receive no input and will not do anything.
    Interacting with the program is still fully possible through stdin `CALL: $command`.
    Default: True
    
  mics=$json
    Use to define multiple microphones.
    Overrides below microphone settins.
    Structure: `{ "exactMicNameOrEmptyForDefaultMic": { "energy": float, "location": "location name", "verbose": boolean }, ... }`    
    Default ''
  
  mic-energy=$int(0,32767)
    Microphone energy treshold. ANything above this volume is considered potential voice.
    Use `mic-verbose=True` to figure out what value to use, as default may cause speech recognition to not work.
    Default: 120
    
  mic-verbose=$bool
    Optional bool whether microphone should be printing real-time `mic-energy`.
    Use only to determine optimal `mic-energy`.
    Default: False

  mic-voice-detect=$bool
    Microphone voice detection.
    If true, detects voice from verified voices and ignores others.
    Verified voices must be 16000Hz wav in `voices-verified` directory.
    Use `mic-voice-detect-prop` to set up sensitivity.
    Default: False
    
  mic-voice-detect-device=$str
    Microphone voice detection torch device, e.g., cpu, cuda:0, cuda:1.
    Default "cpu"

  mic-voice-detect-prop=$int<0,1>
    Microphone voice detection treshold. Anything above this value is considered matched voice.
    Use `mic-voice-detect-debug` to determine optimal value.
    Default: 0.6

  mic-voice-detect-debug=$bool
    Optional bool whether microphone should be printing real-time `mic-voice-detect-prop`.
    Use only to determine optimal `mic-voice-detect-prop`.
    Default: False

  audio-out=$json
    Use to define multiple audio output devices
    Structure: `{ "exactMicNameOrEmptyForDefaultMic": "locationOfThisMictophone", ... }`
    Default ''

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

  stt-whispers2t-model=$model
    Whisper model for speech recognition
    Values: tiny.en, tiny, base.en, base, small.en, small, medium.en, medium, large, large-v1, large-v2, large-v3
    Default: small.en

  stt-whispers2t-device
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
(argWake, (name, wake_words)) = arg('wake-word', 'system', wake_words_and_name)
write(name + " booting up...")
(argSysParentProcess, sysParentProcess) = arg('parent-process', '-1', lambda it: int(it))
sysTerminating = False
sysCacheDir = "cache"
if not os.path.exists(sysCacheDir): os.makedirs(sysCacheDir)

(argMainSpeaker, mainSpeaker) = arg('main-speaker', "User")
CTX.speaker = mainSpeaker
(argMainLocation, mainLocation) = arg('main-location', "PC")
CTX.location = mainLocation
(argMicDef, micDef) = arg('mics', '')
(argMicEnabled, micEnabled) = arg('mic-enabled', "true", lambda it: it=="true")
(argMicName, micName) = arg('mic-name', '')
(argMicEnergy, micEnergy) = arg('mic-energy', "120", lambda it: int(it))
(argMicVerbose, micVerbose) = arg('mic-verbose', "false", lambda it: it=="true")
(argMicVoiceDetect, micVoiceDetect) = arg('mic-voice-detect', "false", lambda it: it=="true")
(argMicVoiceDetectDevice, micVoiceDetectDevice) = arg('mic-voice-detect-device', "cpu")
(argMicVoiceDetectTreshold, micVoiceDetectTreshold) = arg('mic-voice-detect-prop', "0.6", lambda it: float(it))
(argMicVoiceDetectVerbose, micVoiceDetectVerbose) = arg('mic-voice-detect-debug', "false", lambda it: it=="true")
(argAudioOutDef, audioOutDef) = arg('audio-out', '')

(argSttEngineType, sttEngineType) = arg('stt-engine', 'whisper')
(argSttWhisperDevice, sttWhisperDevice) = arg('stt-whisper-device', '')
(argSttWhisperModel, sttWhisperModel) = arg('stt-whisper-model', 'base.en')
(argSttFasterWhisperDevice, sttFasterWhisperDevice) = arg('stt-fasterwhisper-device', '')
(argSttFasterWhisperModel, sttFasterWhisperModel) = arg('stt-fasterwhisper-model', 'small.en')
(argSttWhispers2tDevice, sttWhispers2tDevice) = arg('stt-whispers2t-device', '')
(argSttWhispers2tModel, sttWhispers2tModel) = arg('stt-whispers2t-model', 'small.en')
(argSttNemoDevice, sttNemoDevice) = arg('stt-nemo-device', '')
(argSttNemoModel, sttNemoModel) = arg('stt-nemo-model', 'nvidia/parakeet-ctc-1.1b')
(argSttHttpUrl, sttHttpUrl) = arg('stt-http-url', 'localhost:1235')

(argTtsOn, ttsOn) = arg('speech-on', "true", lambda it: it=="true")
(argTtsEngineType, ttsEngineType) = arg('speech-engine', 'os')
(argTtsCoquiVoice, ttsCoquiVoice) = arg('coqui-voice', 'Ann_Daniels.flac')
(argTtsCoquiCudaDevice, ttsCoquiCudaDevice) = arg('coqui-cuda-device', '')
(argTtsTacotron2Device, ttsTacotron2Device) = arg('tacotron2-cuda-device', '')
(argTtsHttpUrl, ttsHttpUrl) = arg('speech-server', 'localhost:1236')

(argLlmEngine, llmEngine) = arg('llm-engine', 'none')
(argLlmGpt4AllModelName, llmGpt4AllModelName) = arg('llm-gpt4all-model', 'none')
(argLlmOpenAiUrl, llmOpenAiUrl) = arg('llm-openai-url', 'none')
(argLlmOpenAiBearer, llmOpenAiBearer) = arg('llm-openai-bearer', 'none')
(argLlmOpenAiModelName, llmOpenAiModelName) = arg('llm-openai-model', 'none')
(argLlmSysPrompt, llmSysPrompt) = arg('llm-chat-sys-prompt', 'You are helpful chat bot. You are voiced by text-to-speech, so you are extremly concise.')
(argLlmChatMaxTokens, llmChatMaxTokens) = arg('llm-chat-max-tokens', '400', lambda it: int(it))
(argLlmChatTemp, llmChatTemp) = arg('llm-chat-temp', '0.5', lambda it: float(it))
(argLlmChatTopp, llmChatTopp) = arg('llm-chat-topp', '0.95', lambda it: float(it))
(argLlmChatTopk, llmChatTopk) = arg('llm-chat-topk', '40', lambda it: int(it))

(argHttpUrl, httpUrl) = arg('http-url', 'localhost:1236')
(argUsePythonCommands, usePythonCommands) = arg('use-python-commands', 'false', lambda it: it=="true")


# speak engine actor, non-blocking
if ttsEngineType == 'none':
    speakEngine = TtsNone(write)
elif ttsEngineType == 'os':
    speakEngine = TtsOs(write)
elif ttsEngineType == 'coqui':
    speakEngine = TtsCoqui(ttsCoquiVoice, "cuda" if len(ttsCoquiCudaDevice)==0 else ttsCoquiCudaDevice, write)
elif ttsEngineType == 'tacotron2':
    speakEngine = TtsTacotron2("cuda" if len(ttsTacotron2Device)==0 else ttsTacotron2Device, write)
elif ttsEngineType == 'speechbrain':
    speakEngine = TtsSpeechBrain("cuda" if len(ttsTacotron2Device)==0 else ttsTacotron2Device, write)
elif ttsEngineType == 'http':
    if len(ttsHttpUrl)==0: raise AssertionError('speech-engine=http requires speech-server to be specified')
    ttsHttpUrl = ttsHttpUrl.removeprefix("http://").removeprefix("https://")
    if ':' not in ttsHttpUrl: raise AssertionError('speech-server must be in format host:port')
    host, _, port = ttsHttpUrl.partition(":")
    speakEngine = TtsHttp(host, int(port), write)
elif ttsEngineType == 'fastpitch':
    speakEngine = TtsFastPitch("cuda" if len(ttsTacotron2Device)==0 else ttsTacotron2Device, write)
else:
    speakEngine = TtsNone(write)
tts = Tts(ttsOn, speakEngine, audioOutDef, write)


# llm actor, non-blocking
if llmEngine == 'none':
    llm = LlmNone(tts, write)
elif llmEngine == "gpt4all":
    llm = LlmGpt4All(
        llmGpt4AllModelName, "models-gpt4all",
        tts, write,
        llmSysPrompt, llmChatMaxTokens, llmChatTemp, llmChatTopp, llmChatTopk
    )
elif llmEngine == "openai":
    llm = LlmHttpOpenAi(
        llmOpenAiUrl, llmOpenAiBearer, llmOpenAiModelName,
        tts, write,
        llmSysPrompt, llmChatMaxTokens, llmChatTemp, llmChatTopp, llmChatTopk
    )
else:
    llm = LlmNone(tts, write)


# assist
class Assist:
    def onSpeech(self, speech: SpeechText):
        pass


if isinstance(tts.tts, TtsCoqui):
    voices_dir = 'voices-coqui'
    voices = [f for f in os.listdir(voices_dir) if os.path.isfile(os.path.join(voices_dir, f)) and not f.endswith('.txt')]
else:
    voices = []


assist = Assist()

# commands
class CommandExecutorMain(CommandExecutor):

    def execute(self, text: str, ctx: Ctx = CTX) -> str:
        global assist
        handled = "ignore"

        if text == "repeat last speech":
            tts.repeatLast()
            return handled
        if text.startswith("speak "):
            return text
        if text.startswith("do-speak "):
            tts(text.removeprefix("do speak "), ctx.location)
            return handled
        if text == "list available voices":
            tts("The available voices are: " + ', '.join(voices), ctx.location)
            return handled
        if text.startswith("greeting "):
            g = text.removeprefix("greeting ").capitalize()
            llm(ChatReact(llmSysPrompt, "User greeted you with " + g, g), ctx)
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
                        llm(ChatReact(llmSysPrompt, f"User changed your voice from:\n```\n{voiceOld}\n```\n\nto:\n```\n{voiceNew}\n```", name + " voice changed"), ctx)
                else: llm(ChatReact(llmSysPrompt, f"User tried to change voice to {voice}, but such voice is not available", f"No voice {voice} available"), ctx)
            else: llm(ChatReact(llmSysPrompt, f"User tried to change voice, but current voice generation does not support changing voice", "Current voice generation does not support changing voice"), ctx)
            return handled
        if text.startswith("generate from clipboard "):
            tts("Ok", ctx.location)
            llm(ChatPaste("generate " + text.removeprefix("generate from clipboard ") + "\nClipboard:\n```" + get_clipboard_text() + "\n```"))
            return handled
        if text.startswith("generate "):
            tts("Ok", ctx.location)
            llm(ChatPaste(text))
            return handled
        if text.startswith("do-describe "):
            llm(ChatProceed(llmSysPrompt, "Describe the following content:\n" + text.removeprefix("do-describe ")))
            return handled
        elif text == 'start conversation':
            assist.startChat(ctx)
            return handled
        elif text == 'restart conversation':
            assist.restartChat(ctx)
            return handled
        elif text == 'stop conversation':
            assist.stopChat(ctx)
            return handled
        else:
            return text

commandExecutor = CommandExecutorMain()
llm.commandExecutor = commandExecutor.execute
executorPython = PythonExecutor(
    tts, llm,
    lambda sp, up, ms, ctx: llm(ChatIntentDetect.python(sp, up, ms), ctx),
    lambda code: llm(ChatIntentDetect.pythonFix(code)),
    write, llmSysPrompt, commandExecutor, ', '.join(voices)
)

class AssistBasic:
    def __init__(self):
        self.last_announcement_at = datetime.now()
        self.wake_word_delay = 5.0
        self.isChat = False
        self.activity_last_at = time.time()
        self.activity_last_diff = 0
        self.restartChatDelay = 5*60

        executorPython.llm = llm
        llm.api = executorPython

        Thread(name='Assist-Idle-Monitor', target=lambda: self.assistUpdateIdle(), daemon=True).start()

    def assistUpdateIdle(self):
        while True:
            time.sleep(1.0)
            if self.restartChatDelay < (time.time() - self.activity_last_at) and len(executorPython.ms)>0:
                self.restartChat(CTX, react=False)

    def needsWakeWord(self, speech: SpeechText) -> bool:
        return self.isChat is False and executorPython.isQuestion is False and (speech.start - self.last_announcement_at).total_seconds() > self.wake_word_delay

    def onActivity(self, speech: SpeechText):
        self.activity_last_diff = time.time() - self.activity_last_at
        self.activity_last_at = time.time()

    def onSpeech(self, speech: SpeechText):
        text = speech.text
        # announcement
        if len(text) == 0:
            self.last_announcement_at = datetime.now()
            if self.isChat: llm(ChatReact(llmSysPrompt, "User said your name - say you are still conversing. Say 2 words at most", "Yes, we are talking"), speech.asCtx())
            else: llm(ChatReact(llmSysPrompt, "User said your name - are you there? Say 2 words at most", "Yes"), speech.asCtx())
        # cancel
        elif text == "ok" or text == "okey" or text == "whatever" or text == "stop":
            tts.skip()
            llm.generating = False
        # do greeting
        elif (text == "hi" or text == "hello" or text == "greetings") and not usePythonCommands:
            commandExecutor.execute(f"greeting {text}", speech.asCtx())
        # do help
        elif text == "help":
            if self.isChat:
                tts(
                    "Yes, we are in the middle of a conversation. Simply speak to me. "
                    "To end the conversation, say stop, or end conversation."
                    "To restart the conversation, say restart or reset conversation."
                    "To cancel ongoing reply, say okey, whatever or stop.",
                    speech.location
                )
            else:
                tts.skippable(
                    f'I am an AI assistant. Talk to me by calling {name}. ' +
                    f'Start conversation by saying, start conversation. ' +
                    f'Ask for help by saying, help. ' +
                    f'Run command by saying the command.',
                    speech.location
                )
                commandExecutor.execute("help", speech.asCtx()) # allows application to customize the help output

        elif text == "repeat":
            commandExecutor.execute(text, speech.asCtx())
        elif text == "start conversation":
            commandExecutor.execute("start conversation", speech.asCtx())
        elif text == "restart conversation" or text == "reset conversation":
            commandExecutor.execute("restart conversation", speech.asCtx())
        elif text == "stop conversation":
            commandExecutor.execute("stop conversation", speech.asCtx())
        elif text.startswith("generate "):
            write(f'COM: {speech.user}:{speech.location}:' + commandExecutor.execute(text, speech.asCtx()))
        elif text.startswith("count "):
            write(f'COM: {speech.user}:{speech.location}:' + commandExecutor.execute(text, speech.asCtx()))
        # do command - python
        elif usePythonCommands:
            executorPython.generatePythonAndExecute(speech.user, speech.location, speech.text)
        # do command
        else:
            write(f'COM: {speech.user}:{speech.location}:' + commandExecutor.execute(text, speech.asCtx()))

    def startChat(self, ctx: Ctx, react: bool = True):
        if self.isChat: return
        self.isChat = True
        write(f"COM: {ctx.speaker}:{ctx.location}:start conversation")
        if (react): llm(ChatReact(llmSysPrompt, "User started conversation with you. Greet him", "Conversing"), ctx)
        for mic in mics: mic.set_pause_threshold_talk()

    def restartChat(self, ctx: Ctx, react: bool = True):
        tts.skip()
        llm.generating = False
        write(f"COM: {ctx.speaker}:{ctx.location}:restart conversation")
        if (react): llm(ChatReact(llmSysPrompt, "User erased his conversation with you from your memory.", "Ok"), ctx)
        executorPython.ms = []

    def stopChat(self, ctx: Ctx, react: bool = True):
        if self.isChat is False: return
        self.isChat = False
        tts.skip()
        llm.generating = False
        write(f"COM: {ctx.speaker}:{ctx.location}:stop conversation")
        if (react): llm(ChatReact(llmSysPrompt, "User stopped conversation with you", "Ok"), ctx)
        for mic in mics: mic.set_pause_threshold_normal()

assist = AssistBasic()

def skipWithoutSound(speech: Speech):
    executorPython.onSpeech(speech.user)
    if llm.generating: llm.generating = False
    tts.skipWithoutSound()

def skip(speech: SpeechStart):
    executorPython.onSpeechStart(speech.user)
    if llm.generating: llm.generating = False
    tts.skip()

def callback(speech: SpeechText):
    if sysTerminating: return

    # sanitize
    text = speech.text
    text = text.lower().rstrip(".").strip()

    # ignore no input
    if len(text) == 0: return

    # handle question
    if executorPython.isBlockingQuestion:
        if executorPython.isBlockingQuestion and executorPython.isBlockingQuestionSpeaker==speech.user: executorPython.onBlockingQuestionDone.set_result(speech.text)
        write(f'USER: {text}')
        # monitor activity time
        assist.onActivity(speech)
        return

    # ignore speech recognition noise
    if not starts_with_any(text, wake_words) and assist.needsWakeWord(speech):
        write(f'USER-RAW: {text}')
        return

    # monitor activity time
    assist.onActivity(speech)

    # sanitize
    text = remove_any_prefix(text, wake_words).strip().lstrip(",").lstrip(".").rstrip(".").strip().replace(' the ', ' ').replace(' a ', ' ')

    # cancel any ongoing activity
    skipWithoutSound(speech)

    # handle by active assistant state
    try:
        write(f'USER: {name}' + (', ' + text if len(text)>0 else ''))
        assist.onSpeech(SpeechText(speech.start, speech.audio, speech.stop, speech.user, speech.location, text))
    except Exception as e:
        write(f"ERR: {e}")
        print_exc()
        tts(name + " encountered an error. Please speak again or check logs for details.", speech.location)

def start_exit_invoker():
    if sysParentProcess==-1: return

    def waitAndStopWithParentProcess():
        while not sysTerminating and psutil.pid_exists(sysParentProcess): time.sleep(1)
        stop()

    Thread(name='Process-Monitor', target=waitAndStopWithParentProcess, daemon=True).start()

def install_dangling_thread_monitor():
    'Debug threads if application does not exit'
    def print_threads():
        time.sleep(5.0)
        for thread in threading.enumerate():
            if thread != threading.main_thread() and not thread.daemon:
                frame = sys._current_frames()[thread.ident]
                print(f"Thread ID: {thread.ident}")
                traceback.print_stack(frame)
                print("\n")

    if False:
        Thread(name='dangling-thread-monitor', target=print_threads, daemon=True).start()

def stop(*args):  # pylint: disable=unused-argument
    global sysTerminating
    if not sysTerminating:
        sysTerminating = True
        tts.skip()
        tts(name + ' offline', CTX.location).exception()
        llm.stop()
        for mic in mics: mic.stop()
        stt.stop()
        tts.stop()
        http.stop()
        write.stop_with_app()
        install_dangling_thread_monitor()

def install_exit_handler():
    atexit.register(stop)
    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGBREAK, stop)
    signal.signal(signal.SIGABRT, stop)

def install_on_bootup_invoke():
    wait_until(lambda: all(actor.state_active() for actor in actors))
    write.busy_status.remove(1)
    llm(ChatReact(llmSysPrompt, "You booted up. Use 4 words or less.", f"{name} online"))

def install_on_bootup():
    Thread(name='on-bootup', target=install_on_bootup_invoke, daemon=True).start()


try:

    stt = SttNone(micEnabled, write)
    if sttEngineType == 'whisper':
        stt = SttWhisper(micEnabled, "cpu" if len(sttWhisperDevice)==0 else sttWhisperDevice, sttWhisperModel, write)
    if sttEngineType == 'faster-whisper':
        stt = SttFasterWhisper(micEnabled, "cpu" if len(sttFasterWhisperDevice)==0 else sttFasterWhisperDevice, sttFasterWhisperModel, write)
    if sttEngineType == 'whispers2t':
        stt = SttWhisperS2T(micEnabled, "cpu" if len(sttWhispers2tDevice)==0 else sttWhispers2tDevice, sttWhispers2tModel, write)
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

    micVad = Vad(0.5, stt.sample_rate)
    if micVoiceDetect: micSpeakerDetector = MicVoiceDetectNvidia(micVoiceDetectTreshold, micVoiceDetectVerbose, micVoiceDetectDevice)
    else: micSpeakerDetector = MicVoiceDetectNone()


    mics: [Mic] = []
    if len(micDef)>0:
        for micDefName, att in json.loads(micDef).items():
            mics.append(
                Mic(None if len(micDefName)==0 else micDefName, micEnabled, att['location'], stt.sample_rate, lambda e: skip(e), lambda e: stt(e), tts, write, att['energy'], att['verbose'], micVad, micSpeakerDetector)
            )
    else:
        mics.append(
            Mic(None if len(micName)==0 else micName, micEnabled, "Livingroom", stt.sample_rate, lambda e: skip(e), lambda e: stt(e), tts, write, micEnergy, micVerbose, micVad, micSpeakerDetector)
        )

    actors: [Actor] = [write, *mics, stt, llm, tts.tts, tts.play]

    # http
    if ':' not in httpUrl: raise AssertionError('http-url must be in format host:port')
    host, _, port = httpUrl.partition(":")
    http = Http(host, int(port), write)
    http.handlers.append(HttpHandlerState(actors))
    http.handlers.append(HttpHandlerStateActorEvents(actors))
    http.handlers.append(HttpHandlerStateActorEventsAll(actors))
    http.handlers.append(HttpHandlerIntent(llm))
    http.handlers.append(HttpHandlerStt(stt))
    http.handlers.append(HttpHandlerTts(tts.tts))
    http.handlers.append(HttpHandlerMicState(mics))
    http.handlers.append(HttpHandlerTtsReact(llm, llmSysPrompt))

    # start actors
    http.start()
    tts.start()
    stt.start()

    for mic in mics: mic.start()
    llm.start()
    install_exit_handler()
    start_exit_invoker()
    install_on_bootup()

except Exception:
    write("ERR: Failed to start")
    sysTerminating = True
    print_exc()

while not sysTerminating:
    try:
        m = input()
        def speakerAndLocAndText(text: str) -> (str, str, str):
            speaker = CTX.speaker if ":" not in text else text.split(":")[0]
            location = CTX.location if ":" not in text else text.split(":")[1]
            text = text if ":" not in text else text.split(":")[2]
            text = base64.b64decode(text).decode('utf-8')
            text = remove_any_prefix(text, wake_words)
            print(location)
            return (speaker, location, text)

        # talk command
        if m.startswith("SAY-LINE: "):
            text = m[10:]
            tts.skippable(text, CTX.location)

        # talk command
        if m.startswith("SAY: "):
            text = base64.b64decode(m[5:]).decode('utf-8')
            tts.skippable(text, CTX.location)

        # chat command
        if m.startswith("CHAT: "):
            speaker, location, text = speakerAndLocAndText(m[6:])
            now = datetime.now()
            callback(SpeechText(now, None, now, speaker, location, name + ' ' + text))

        if m.startswith("COM-PYT: "):
            speaker, location, text = speakerAndLocAndText(m[9:])
            executorPython.execute(speaker, location, text, text)

        if m.startswith("COM-PYT-INT: "):
            speaker, location, text = speakerAndLocAndText(m[13:])
            executorPython.generatePythonAndExecute(speaker, location, text)

        if m.startswith("COM: "):
            text = m[5:]
            text = base64.b64decode(text).decode('utf-8')
            commandExecutor.execute(text)

        # changing settings commands
        elif argWake.isArg(m):
            (name, (name, wake_words)) = argWake(m)

        elif argMainSpeaker.isArg(m):
            mainSpeaker = argMainSpeaker(m)
            CTX.speaker = mainSpeaker

        elif argMainLocation.isArg(m):
            mainLocation = argMainLocation(m)
            CTX.location = mainLocation

        elif argMicEnabled.isArg(m):
            e = argMicEnabled(m)
            micEnabledOld = micEnabled
            micEnabled = e
            for mic in mics: mic.enabled = e
            stt.enabled = e
            if micEnabledOld!=e:
                llm(ChatReact(llmSysPrompt, f"User turned {'on' if e else 'off'} microphone input", "Microphone {'on' if e else 'off'}"))

        elif argMicEnergy.isArg(m):
            micEnergy = argMicEnergy(m)
            for mic in mics: mic.energy_threshold = micEnergy

        elif argMicVerbose.isArg(m):
            micVerbose = argMicVerbose(m)
            for mic in mics: mic.energy_debug = micVerbose

        elif argMicVoiceDetectTreshold.isArg(m):
            micVoiceDetectTreshold = argMicVoiceDetectTreshold(m)
            for mic in mics:
                if micVoiceDetect: mic.speaker_diar.speaker_treshold = micVoiceDetectTreshold

        elif argMicVoiceDetectVerbose.isArg(m):
            micVoiceDetectVerbose = argMicVoiceDetectVerbose(m)
            for mic in mics:
                if micVoiceDetect: mic.speaker_diar.verbose = micVoiceDetectVerbose

        elif argTtsOn.isArg(m):
            ttsOn = argTtsOn(m)
            tts.enabled = ttsOn

        elif argTtsCoquiVoice.isArg(m):
            commandExecutor.execute("change voice " + argTtsCoquiVoice(m))

        elif argLlmSysPrompt.isArg(m):
            promptOld = llmSysPrompt
            llmSysPrompt = argLlmSysPrompt(m)
            llm.sysPrompt = llmSysPrompt
            if promptOld!=llmSysPrompt:
                llm(ChatReact(llmSysPrompt, f"User changed your system prompt from:\n```\n{promptOld}\n```\n\nto:\n```\n{llmSysPrompt}\n```", name + " prompt changed"))

        elif argLlmChatMaxTokens.isArg(m):
            llm.maxTokens = argLlmChatMaxTokens(m)

        elif argLlmChatTemp.isArg(m):
            llm.temp = argLlmChatTemp(m)

        elif argLlmChatTopp.isArg(m):
            llm.topp = argLlmChatTopp(m)

        elif argLlmChatTopk.isArg(m):
            llm.topk = argLlmChatTopk(m)

        elif argUsePythonCommands.isArg(m):
            usePythonCommandsOld = usePythonCommands
            usePythonCommands = argUsePythonCommands(m)
            if usePythonCommandsOld!=usePythonCommands:
                llm(ChatReact(llmSysPrompt, f"User {'increased' if usePythonCommands else 'decreased'} your output expressivity", name + " prompt changed"))


        # exit command
        elif m == "EXIT":
            stop()

    except EOFError as _:
        stop()
    except Exception as e:
        write("ERR: Error occurred:" + str(e))
        print_exc()