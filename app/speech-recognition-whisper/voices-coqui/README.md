Directory for TtsCoqui voices.

##### Voice cloning
- Create a voice sample recording (cleanest possible)
    - Make it into 3-30s `22050Hz` wav, `ffmpeg -i <input_file>.extension -ss 00:00:00 -t 00:00:15 -c:a pcm_s16le -ar 22050 <output_file>.wav`
        - using different sampling rate will degrade speech output quality
- Place it into this directory
- Change voice using assistant ui widget or simply by talking to the assistant.
    - No restart necessary
    - Voice loading is slower 1st time, the generated data are saved to disk.

### Loading voice
Use 3-20s wav or flac samples.
Loading a voice creates a json file, which speeds up subsequent loading.

### Voice speed
It is possible to add `"speed": float` property in the created voice.json file in range `<0.5, 1.5>`. Be sure to delete cached voices.