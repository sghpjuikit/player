import re
import os


def sanitize_filename(text):
    # Remove any characters that are not alphanumeric, underscore, or whitespace
    sanitized_text = re.sub(r'[^\w\s]', '', text)
    # Split the text into words
    words = sanitized_text.split()
    # Join the words with hyphens
    sanitized_text = '-'.join(words)
    # Convert to lowercase
    sanitized_text = sanitized_text.lower()

    return sanitized_text


def cache_file(text, cache_dir):
    sanitized_text = sanitize_filename(text)
    cache_path = os.path.join(cache_dir, sanitized_text)

    # Check if the cache directory exists, create it if not
    if not os.path.exists(cache_dir):
        os.makedirs(cache_dir)

    # Find the appropriate file
    return cache_path, os.path.exists(cache_path)
