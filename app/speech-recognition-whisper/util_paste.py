from time import sleep
import pyperclip
import pyautogui
import pygetwindow

def get_clipboard_text() -> str:
    return pyperclip.paste()

def pasteTokens(tokens):
    original_window = pygetwindow.getActiveWindow()
    tokens_queue = Queue()
    stop = False

    # end if no window active
    if original_window is None: return

    def paste(text):
        pyperclip.copy(text)  # Set the clipboard contents
        pyautogui.hotkey('ctrl', 'v')  # Simulate pressing CTRL+V

    def pasteAccumulated():
        t = ''
        while not tokens_queue.empty(): t = t + tokens_queue.get()
        paste(t)

    def acc_tokens(tokens):
        nonlocal stop
        for token in tokens: tokens_queue.put(token)
        stop = True
            
    def process_token():
        nonlocal stop

        while not stop:
            sleep(1/10.0) # 10 FPS pasting
            
            # paste if window active
            if original_window.isActive:
                pasteAccumulated()
                continue

            # finish paste if window becomes active
            if len(queue)>0:
                for _ in range(10*10):
                    # wait 10 s
                    sleep(0.1)
                    # end if window was closed
                    if original_window.visible is False: break;
                    # paste and end if window is active again
                    if original_window.isActive:
                        pasteAccumulated()
                        break

    thread1 = Thread(name="paster-acc", daemon=True, target=acc_tokens, args=(tokens,))
    thread2 = Thread(name="paster-cpy", daemon=True, target=process_token)
    thread1.start()
    thread2.start()