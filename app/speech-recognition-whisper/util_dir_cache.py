import re
import os


def sanitize_filename(text):
    # Remove any characters that are not alphanumeric
    sanitized_text = re.sub(r'[^a-zA-Z0-9]', '-', text).strip('-')
    # Convert to lowercase
    sanitized_text = sanitized_text.lower()

    return sanitized_text


def cache_file(text, cache_dir) -> (str, bool):
    sanitized_text = sanitize_filename(text)
    cache_path = os.path.join(cache_dir, sanitized_text + ".wav")

    # Find the appropriate file
    return cache_path, os.path.exists(cache_path)
