
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
from itertools import chain
from threading import Thread
from typing import cast
from util_tty_engines import Tty, TtyNone, TtyOs, TtyOsMac, TtyCharAi, TtyCoqui, VlcActor
from util_llm import Llm
from util_write_engine import Writer
from util_itr import teeThreadSafe

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
    write("This is a speech recognition python script using OpenAI Whisper.")
    write("Prints recognized user speech in format `USER: $speech` and system log in format`SYS: $message`.")
    write("")
    write("This script prints:")
    write("    - system log in format`SYS: $message`")
    write("    - recognized user speech in format `RAW: $speech`")
    write("    - recognized user speech, sanitized, in format `USER: $speech`")
    write("    - recognized user command, in format `COM: $command`")
    write("")
    write("This script takes (optional) input:")
    write("    - `SYS: $base64_encoded_text` and speaks it (if speech-engine is not `none`)")
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
    write("    Engine for speaking. Use 'none', 'os', 'character-ai' or 'coqui'")
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
    write("  coqui-voice=$voice")
    write("    If speaking-engine=coqui is used, required name of voice file must be specified and exist in ./coqui-voices dir")
    write("    Default: Ann_Daniels.flac")
    write("")
    write("  vlc-path=$path_to_vlc_dir")
    write("    If speaking-engine=character-ai is used, optional path to vlc player can be specified.")
    write("    If no path is specified and application does not find any vlc player installed, speaking will not function.")
    write("    Default: ''")
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

# args
parentProcess = int(arg('parent-process', -1))
wake_word = arg('wake-word', 'system')
name = wake_word[0].upper() + wake_word[1:]
speakEngineType = arg('speaking-engine', 'os')
speakUseCharAiToken = arg('character-ai-token', '')
speakUseCharAiVoice = int(arg('character-ai-voice', '22'))
speakUseCoquiVoice = arg('coqui-voice', 'Ann_Daniels.flac')
vlcPath = arg('vlc_path', '')
speechRecognitionModelName = arg('speech-recognition-model', 'base.en.pt')
chatModelName = arg('chat-model', 'none')

intention_prompt = base64.b64decode(arg('intent-prompt', '')).decode('utf-8')
listening_for_chat_prompt = False
listening_for_chat_generation = False
terminating = False

# speak engine actor, non-blocking
if speakEngineType == 'none':
    speakEngine = TtyNone()
elif speakEngineType == 'os' and sys.platform == 'darwin':
    speakEngine = TtyOsMac()
elif speakEngineType == 'os':
    speakEngine = TtyOs(write)
elif speakEngineType == 'character-ai':
    speakEngine = TtyCharAi(speakUseCharAiToken, speakUseCharAiVoice, VlcActor(vlcPath, write), write)
elif speakEngineType == 'coqui':
    speakEngine = TtyCoqui(speakUseCoquiVoice, VlcActor(vlcPath, write), write)

speak = Tty(speakEngine, write)


speak(name + " initializing")

cache_dir = "cache"
if not os.path.exists(cache_dir):
    os.makedirs(cache_dir)

whisperModelDir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models-whisper")
whisperModel = whisper.load_model(os.path.join(whisperModelDir, speechRecognitionModelName))
listening = None
warnings.filterwarnings("ignore", category=UserWarning, module='whisper.transcribe', lineno=114)


chatUseCharAi = False
chatModelDir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "models-llm")
chatModel = os.path.join(chatModelDir, chatModelName)
chat = None if chatModelName == "none" else Llm(chatModel)

# speak(name + " INTENT DETECTOR initializing.")

# https://huggingface.co/glaiveai/glaive-function-calling-v1
commandTokenizer = None  # AutoTokenizer.from_pretrained("glaiveai/glaive-function-calling-v1", revision="6ade959", trust_remote_code=True)
commandModel = None  # AutoModelForCausalLM.from_pretrained("glaiveai/glaive-function-calling-v1", revision="6ade959", trust_remote_code=True).half().cuda()


# noinspection PyUnusedLocal
def callback(recognizer, audio_data):
    global listening_for_chat_prompt
    global listening_for_chat_generation

    if not terminating:
        try:
            import io
            import numpy as np
            import soundfile as sf
            import torch
            wav_bytes = audio_data.get_wav_data(convert_rate=16000) # 16 kHz https://github.com/openai/whisper/blob/28769fcfe50755a817ab922a7bc83483159600a9/whisper/audio.py#L98-L99
            wav_stream = io.BytesIO(wav_bytes)
            audio_array, sampling_rate = sf.read(wav_stream)
            audio_array = audio_array.astype(np.float32)
            text = whisperModel.transcribe(audio_array, language=None, task=None, fp16=torch.cuda.is_available())['text']


            # import numpy as np
            # result = modelWhisper.transcribe(np.frombuffer(audio.get_raw_data(), dtype=np.int16).astype(np.float32) / 32768.0)
            # text = result['text']

            chatPrompt = text.rstrip(".").strip()
            text = text.lower().rstrip(".").strip()

            if len(text) > 0:
                write('RAW: ' + text)

            # ignore speech recognition noise
            if not text.startswith(wake_word):
                return

            # cancel any ongoing chat activity
            if listening_for_chat_generation:
                if not text.startswith("system end conversation") and not text.startswith("system stop conversation"):
                    listening_for_chat_generation = False
                    return

            # sanitize
            chatPrompt = chatPrompt.lstrip(wake_word).lstrip(",").rstrip(".").strip()
            text = text.lstrip(wake_word).lstrip(",").rstrip(".").strip().replace(' the ', ' ').replace(' a ', ' ')
            write('USER: ' + text)

            # announcement
            if len(text) == 0:
                if listening_for_chat_prompt:
                    speak('Yes, conversation is ongoing')
                else:
                    speak('Yes')

            # start LLM conversation (fail)
            elif listening_for_chat_prompt is False and "start conversation" in text and chat is None:
                speak('No conversation model is loaded')

            # start LLM conversation
            elif listening_for_chat_prompt is False and "start conversation" in text:
                chat.chatStart()
                listening_for_chat_prompt = True
                speak('Conversing')

            # end LLM conversation
            elif listening_for_chat_prompt is True and (text.startswith("end conversation") or text.startswith("stop conversation")):
                listening_for_chat_prompt = False
                if not chat.generating:
                    chat.chatStop()
                    speak.iterableSkip()
                    speak("Ok")

            # do LLM conversation
            elif listening_for_chat_prompt is True:
                # noinspection PyUnusedLocal
                def stop_on_token_callback(token_id, token_string):
                    return listening_for_chat_generation

                def generate(txt):
                    global listening_for_chat_generation
                    listening_for_chat_generation = True
                    chat.generating = True

                    # generate & stream response
                    tokens = chat.llm.generate(txt, streaming=True, n_batch=16, max_tokens=1000, callback=stop_on_token_callback)
                    tokensWrite, tokensSpeech, tokensText = teeThreadSafe(tokens, 3)
                    write(chain(['CHAT: '], tokensWrite))
                    speak(tokensSpeech, False)
                    text_all = ''.join(tokensText)
                    chat.generating = False

                    # end LLM conversation (if cancelled by user)
                    if listening_for_chat_prompt is False:
                        chat.chatStop()
                        speak.iterableSkip()
                        speak("Ok")
                    listening_for_chat_generation = False

                Thread(target=generate, args=(chatPrompt,), daemon=True).start()

            # do help
            elif text == "help":
                speak('I am an AI assistant. Talk to me by calling ' + wake_word + '.')
                speak('You can start conversation by saying ' + wake_word + ' start conversation.')
                speak('You can stop active conversation by saying ' + wake_word + 'stop or end conversation.')
                speak('You can ask for help by saying ' + wake_word + ' help.')
                speak('You can run command by saying ' + wake_word + ' and saying the command.')
                write('COM: help')  # allows application to customize the help output

            # do command
            else:
                # command_prompt = intention_prompt + text
                # command_input = commandTokenizer(command_prompt, return_tensors="pt").to(commandModel.device)
                # command_output = commandModel.generate(**command_input, do_sample=True, temperature=0.1, top_p=0.1, max_new_tokens=1000)
                # text = commandTokenizer.decode(command_output[0], skip_special_tokens=True)
                #
                write('COM: ' + text.strip())
                #
                # if "ASSISTANT: <functioncall>" in text:
                #     text = text.split("ASSISTANT: <functioncall>")[1]
                #     write('USER: ' + text)
                # else:
                #     write('SYS: No command detected')

        except Exception as e:
            traceback.print_exc()
            write_ex("Error: ", e)
            speak(name + " encountered an error. Please speak again.")


def start_listening():
    global listening
    r = speech_recognition.Recognizer()
    r.pause_threshold = 0.8
    r.phrase_threshold = 0.3
    r.energy_threshold = 120
    r.dynamic_energy_threshold = False
    try:
        source = speech_recognition.Microphone()
        # with source:
        #     r.adjust_for_ambient_noise(source, duration=3)
        listening = r.listen_in_background(source, callback)
    except Exception:
        speak(name + " partially online. Voice recognition disabled. No microphone found.")
    speak(name + " online")


# noinspection PyUnusedLocal
def stop(*args):
    global terminating
    if not terminating:
        terminating = True
        speak(name + ' offline')

        if listening is not None:
            cast(callable, listening)(False)

        speak.stop()


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
                text = base64.b64decode(m[5:]).decode('utf-8')
                speak(map(lambda x: x + ' ', text.split(' ')), False)
            elif m.startswith("coqui-voice=") and isinstance(speak.tty, TtyCoqui):
                cast(speak.tty, TtyCoqui).voice = prop(m, "coqui-voice=", speakUseCoquiVoice)
                speak(name + " voice changed", use_cache=False)
            elif m == "EXIT":
                sys.exit(0)
        except EOFError as _:
            sys.exit(0)


install_exit_handler()
Thread(target=start_exit_invoker, daemon=True).start()
Thread(target=start_listening, daemon=True).start()
Thread(target=start_input_handler, daemon=False).start()
