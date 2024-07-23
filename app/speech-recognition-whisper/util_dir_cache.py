import re
import os


def sanitize_filename(text):
    # Remove any characters that are not legal filename
    sanitized_text = re.sub(r'[\\/:*?"<>|]', '-', text).strip('-')
    # Convert to lowercase
    sanitized_text = sanitized_text.lower()

    return sanitized_text


def cache_file(text, cache_dir) -> (str, bool):
    sanitized_text = sanitize_filename(text)
    if len(sanitized_text)==0: raise Exception(f'cache file name sanitizaition error for {text} and {cache_dir}')
    cache_path = os.path.join(cache_dir, sanitized_text + ".wav")

    # Find the appropriate file
    return cache_path, os.path.exists(cache_path)
