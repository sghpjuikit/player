Directory for models

# Model support
Gpt4All has officially supported models and they can be downloaded (and moved here) through Gpt4All application Ui.
Any gguf model should however work.

## Prompt format
Officially supported models should have prompt format set up automatically.

To make sure the prompt format is correct, set it in the prompt file in this directory.
For each mode, you can create a text file with the prompt format as content and with the same name as the model file and adding suffix `.prompt.txt`

E.g. for `Meta-Llama-3-8B-Instruct.Q4_0.gguf` model create `Meta-Llama-3-8B-Instruct.Q4_0.gguf.prompt.txt` file with
```
<|start_header_id|>user<|end_header_id|>

%1<|eot_id|><|start_header_id|>assistant<|end_header_id|>

%2<|eot_id|>
```