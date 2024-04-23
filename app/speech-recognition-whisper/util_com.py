from typing import List

class CommandExecutor:
    def execute(self, text: str) -> str:
        pass

class CommandExecutorAsIs(CommandExecutor):
    def execute(self, text: str) -> str:
        return text

class CommandExecutorDoNothing(CommandExecutor):
    def execute(self, text: str) -> str:
        return "ignore"

class CommandExecutorDelegate(CommandExecutor):
    def __init__(self, commandExecutor: CommandExecutor):
        self.commandExecutor = commandExecutor

    def execute(self, text: str) -> str:
        return self.commandExecutor.execute(text)

def commands(text) -> List[str]:
    result = text.split(' and ')
    return result if len(result) > 1 else [text]