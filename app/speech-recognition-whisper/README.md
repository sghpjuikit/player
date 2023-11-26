

## Introduction
This is a speech recognition python script using OpenAI Whisper. Features:
- configurable wake word
- speech feedback
    - configurable speech engine
- transcribes user speech
- conversation with LLM
- 3rd-party application integration
   - parsable output for convenient integration for 3rd-party applications
   - terminates automatically when parent terminates

## Installation

1. Install python >= `3.11`
2. Install python dependencies
    ```
    pip install openai-whisper
    pip install SpeechRecognition
    pip install --upgrade wheel
    pip install playsound
    pip install soundfile
    pip install PyAudio
    pip install pyttsx3
    pip install psutil
    pip install python-vlc
    pip install gpt4all
    ```
3. Install ffmpeg
    ```
    // Ubuntu | Debian
    sudo apt update && sudo apt install ffmpeg
    
    // Arch Linux
    sudo pacman -S ff[tos_agreed.txt](models-coqui%2Ftos_agreed.txt)mpeg
    
    // MaxOS
    brew install ffmpeg
    // Windows
    choco install ffmpeg
    scoop install ffmpeg
    ```
4. Install portaudio (macOS only)
    ```
    brew install portaudio
    ```
5. Download OpenAi Whisper [models](https://github.com/openai/whisper#available-models-and-languages) (optional)
   - model will be downloaded automatically, but you can do so manually
       - into the [models-whisper](models-whisper) directory
       - from [official source](https://github.com/openai/whisper/blob/f296bcd3fac41525f1c5ab467062776f8e13e4d0/whisper/__init__.py)
6. Download LLM model for [GPT4All](https://gpt4all.io/index.html) (optional)
   - model is required only for conversation feature 
       - into the [models-llm](models-llm) directory
       - for example from [official source](https://gpt4all.io/models/models.json)
7. Download XTTSv2 model (optional)
   - required only when using coqui speaking engine
       - the following files into [models-coqui](models-coqui) directory
           - `config.json`
           - `hash.md5`
           - `model.pth`
           - `vocab.json`
       - agree to the terms and conditions in [models-coqui](models-coqui) directory by
           - creating `tos_agreed.txt` file with content `I have read, understood and agreed to the Terms and Conditions.`

## Running

```python main.py```

For help invoke `-h` or `--help`

## Integration

### Wake word
Can be configured with a script argument

### Output format
This script prints:
- system log in format`SYS: $message`
- recognized user speech in format `RAW: $speech`
- recognized user speech, sanitized, in format `USER: $speech`
- recognized user command, in format `COM: $command`

### Input format (optional)
- `SYS: $base64_encoded_text` and speaks it (if speech-engine is not `none`)

### Whisper

### Speach
By default, system voice is used.

#### coqui
```
pip install torch
pip install TTS
```

#### character.ai
```
pip install PyCharacterAI
```

Supports [character.ai](https://beta.character.ai) voice generation (requires free account and access token)
using [PyCharacterAi](https://pypi.org/project/PyCharacterAI/), read details there.

### LLM
By default, disabled. GPT4All supported models can be provided to enable LLM chat functionality.

### Termination
This scrip terminates:
- upon `CTRL+C`
- when parent process terminates, if launched with `parent-process=$pid` argument
- when receives `EXIT` on input stream

## Hardware requirements
- RAM: 3GB
- GPU: ?

## Copyright
See [LICENCE](LICENCE)

## Acknowledgment
Loosely based on **AI-Austin**'s [GPT4ALL-Voice-Assistant](https://github.com/Ai-Austin/GPT4ALL-Voice-Assistant)