from threading import Thread
from time import sleep
import pyperclip
import pyautogui
import pygetwindow

def pasteTokens(tokens):
    original_window = pygetwindow.getActiveWindow()
    tokensMissed = ''

    # end if no window active
    if original_window is None: return

    def paste(text):
        pyperclip.copy(text)  # Set the clipboard contents
        pyautogui.hotkey('ctrl', 'v')  # Simulate pressing CTRL+V

    def process_token(tokens):
        nonlocal tokensMissed

        for token in tokens:
            # paste if window active
            if original_window==pygetwindow.getActiveWindow():
                if len(tokensMissed)>0:
                    paste(tokensMissed)
                    tokensMissed = ''
                paste(token)
            # accumulate if not
            else:
                tokensMissed = tokensMissed + token

        if len(tokensMissed)>0:
            for _ in range(60):
                # watit second
                sleep(1)
                # end if window was closed
                if original_window.visible is False: break;
                # paste and end if window is active again
                if original_window==pygetwindow.getActiveWindow():
                    paste(tokensMissed)
                    break

    thread = Thread(name="paster", daemon=True, target=process_token, args=(tokens,))
    thread.start()