from time import sleep
from imports import *
import pyperclip
import pyautogui
import pygetwindow

def get_clipboard_text() -> str | None:
    'Returns text in clipboard or None if no text available'
    return pyperclip.paste()

def pasteTokens(tokens: Iterator[str]):
    '''
    Pastes the specified text (to active window) as if user pressed CTRL+V. Does not block thread..
    Supports streaming (uses 10FPS to maintain interactivity and performance.
    If active window loses focus, pasting is interrupted until focus is retained or 10s passes (remainer is thrown away)
    '''

    tokens_queue = Queue()
    stop = False

    def paste(text):
        pyperclip.copy(text)  # Set the clipboard contents
        pyautogui.hotkey('ctrl', 'v')  # Simulate pressing CTRL+V

    def pasteAccumulated():
        t = ''
        while not tokens_queue.empty(): t = t + tokens_queue.get()
        paste(t)

    def acc_tokens(tokens):
        nonlocal stop
        try:
            for token in tokens: tokens_queue.put(token)
            stop = True
        except Exception as e:
            print_exc()

    def process_token():
        nonlocal stop
        try:
            w = pygetwindow.getActiveWindow()
            # end if no window active
            if w is None: return

            while not stop:
                sleep(1/10.0) # 10 FPS pasting

                # paste if window active
                if w.isActive:
                    pasteAccumulated()
                    continue

                # finish paste if window becomes active
                if len(tokens_queue)>0:
                    for _ in range(10*10):
                        # wait 10 s
                        sleep(0.1)
                        # end if window was closed
                        if w.visible is False: break;
                        # paste and end if window is active again
                        if w.isActive:
                            pasteAccumulated()
                            break
        except Exception as e:
            print_exc()

    Thread(name="paster-acc", daemon=True, target=acc_tokens, args=(tokens,)).start()
    Thread(name="paster-cpy", daemon=True, target=process_token).start()