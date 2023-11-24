from gpt4all import GPT4All  # https://docs.gpt4all.io/index.html
from gpt4all.gpt4all import empty_chat_session


class Llm:
    generating = False
    llm: GPT4All

    def __init__(self, chatModel: str):
        self.llm = GPT4All(chatModel)

    def chatStart(self):
        self.chatSession = self.llm.chat_session()

    def chatStop(self):
        self.chat._is_chat_session_activated = False
        self.chat.current_chat_session = empty_chat_session("")
        self.chat._current_prompt_template = "{0}"
        self.chatSession = None