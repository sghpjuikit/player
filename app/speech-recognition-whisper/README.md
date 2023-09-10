

## Introduction
This is a speech recognition python script using OpenAI Whisper. Features:
- configurable wake word
- speech feedback
- provides user speech as console output
- 3rd-party application integration
   - parsable output for convenient integration from 3rd-party applications
   - terminates automatically when parent terminates

## Installation

1. Install python
2. Install python dependencies
    ```
    pip install openai-whisper
    pip install SpeechRecognition
    pip install playsound
    pip install soundfile
    pip install PyAudio
    pip install pyttsx3
    pip install psutil
    ```
3. Install ffmpeg
    ```
    // Ubuntu | Debian
    sudo apt update && sudo apt install ffmpeg
    
    // Arch Linux
    sudo pacman -S ffmpeg
    
    // MaxOS
    brew install ffmpeg
    // Windows
    choco install ffmpeg
    scoop install ffmpeg
    ```
4. Install portaudio (MacOS only)
    ```
    brew install portaudio
    ```
5. Download OpenAi Whisper [models](https://github.com/openai/whisper#available-models-and-languages)
   - `tiny-en`, `base-en`
   - into the [models](models) directory
   - from [official source](https://github.com/openai/whisper/blob/f296bcd3fac41525f1c5ab467062776f8e13e4d0/whisper/__init__.py)

## Running

```python main.py```

For help invoke `-h` or `--help`

## Integration

### Wake word
Can be configured with a script argument

### Output format
This script prints system log in format`SYS: $message` and recognized user speech in format `USER: $speech`.

### Termination
If this script is launched normally, terminate with `CTRL+C`.

If this script is launched from some parent process that passed `parent-process=$pid` argument,
this script terminates when the specified process terminates.

## Hardware requirements
- RAM: 3GB
- GPU: ?

## Copyright
See [LICENCE](LICENCE)

## Acknowledgment
Loosely based on **AI-Austin**'s [GPT4ALL-Voice-Assistant](https://github.com/Ai-Austin/GPT4ALL-Voice-Assistant)