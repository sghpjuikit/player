from PIL import ImageGrab
import uuid

# Capture the image data from the clipboard
image = ImageGrab.grabclipboard()

def filenamize(str):
    invalid_chars = ['/', '\\', ':', '*', '?', '<', '>', '|']
    for char in invalid_chars:
        str = str.replace(char, '_')
    return str

if image:
    file_path = str(filenamize(in_name) if in_name!='random' else uuid.uuid4()) + '.png'
    image.save(file_path)
    speak('Done')
    out_result = file_path
else:
    speak('No image data found.')
    out_result = None
