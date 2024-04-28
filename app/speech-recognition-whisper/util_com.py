
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

def commands(text) -> [str]:
    result = text.split(' and ')
    return result if len(result) > 1 else [text]

def preprocess_command(text: str) -> str:
    import re
    # Regular expression to match a method call without quotes around the method name or its arguments
    pattern = r"(\w+)\((.+?)\)"
    match = re.match(pattern, text)

    if match:
        method_name = match.group(1)
        arguments = match.group(2)
        # Check if arguments are already quoted
        if arguments.startswith("'") or arguments.startswith('"'):
            # If arguments are already correctly quoted, return the input as is
            return text
        else:
            # Escape single quotes within arguments
            arguments = arguments.replace('"', '\"')
            # Add quotes around arguments
            arguments = f'"{arguments}"'
            # Reconstruct the method call with escaped and quoted arguments
            return f"{method_name}({arguments})"
    else:
        # If the input does not match the pattern, return it as is
        return text

class PythonExecutor:
    def __init__(self, tts, write):
        self.id = 0
        self.tts = tts
        self.write = write

    def skip(self):
        self.id = self.id+1

    def __call__(self, text: str):
        self.skip()
        idd = self.id
        from util_paste import get_clipboard_text
        from time import sleep

        class CommandCancelException(Exception): pass
        def doOrSkip(block):
            if (idd!=self.id): raise CommandCancelException()
            return block()
        def clipboard(): return get_clipboard_text()
        def command(c: str): doOrSkip(lambda: self.write('COM: ' + c.replace("-", " ")))
        def generate(c: str): doOrSkip(lambda: command('generate ' + c))
        def speak(t: str): doOrSkip(lambda: self.tts(t).result())
        def wait(t: float): doOrSkip(lambda: sleep(t))

        try:
            exec(text)
        except CommandCancelException as ce: pass
        except Exception as e: self.write(f"ERR: error executing command: {e}")

