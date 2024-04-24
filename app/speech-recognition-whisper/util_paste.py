from threading import Thread
from time import sleep
import pyperclip
import pyautogui
import pygetwindow

def pasteTokens(tokens):
    original_window = pygetwindow.getActiveWindow()
    tokensMissed = ''
    stop = False

    # end if no window active
    if original_window is None: return

    def paste(text):
        pyperclip.copy(text)  # Set the clipboard contents
        pyautogui.hotkey('ctrl', 'v')  # Simulate pressing CTRL+V

    def acc_tokens(tokens):
        nonlocal tokensMissed
        nonlocal stop
        for token in tokens: tokensMissed = tokensMissed + token
        stop = True
            
    def process_token():
        nonlocal tokensMissed

        while not stop:
            sleep(1/3.0) # 3 FPS pasting
            
            # paste if window active
            if original_window==pygetwindow.getActiveWindow():
                if len(tokensMissed)>0:
                    paste(tokensMissed)
                    tokensMissed = ''

            # finish paste if window becomes active
            if len(tokensMissed)>0:
                for _ in range(10*10):
                    # wait 10 s
                    sleep(0.1)
                    # end if window was closed
                    if original_window.visible is False: break;
                    # paste and end if window is active again
                    if original_window==pygetwindow.getActiveWindow():
                        paste(tokensMissed)
                        break

    thread1 = Thread(name="paster-acc", daemon=True, target=acc_tokens, args=(tokens,))
    thread2 = Thread(name="paster-cpy", daemon=True, target=process_token)
    thread1.start()
    thread2.start()