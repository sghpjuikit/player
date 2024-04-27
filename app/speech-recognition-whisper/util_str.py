import re

def starts_with_any(t: str, prefixes: [str]):
    for prefix in prefixes:
        if t.startswith(prefix):
            return True
    return False

def ends_with_any(t: str, suffixes: [str]):
    for prefix in suffixes:
        if t.endswith(prefix):
            return True
    return False

def remove_any_prefix(t: str, prefixes: [str]):
    for prefix in prefixes:
        if t.startswith(prefix):
            t = t[len(prefix):]
    return t

def remove_any_suffix(t, suffixes: [str]):
    for suffix in suffixes:
        if t.endswith(suffix):
            t = t[:len(t) - len(suffix)]
    return t

def wake_words_and_name(s: str) -> (str, [str]):
    s = re.sub(',\s*', ',', original_string).lower().split(",")
    wake_words = list(filter(None, s.split(',')))
    name = wake_words[0][0].upper() + wake_words[0][1:]
    return (name, wake_words)

def arg(arg_name: str, fallback: str) -> str:
    a = next((x for x in sys.argv if x.startswith(arg_name + '=')), None)
    if a is None:
        return fallback
    else:
        return prop(a, arg_name, fallback)

def prop(text: str, arg_name: str, fallback: str) -> str:
    if text.startswith(arg_name + '='):
        return text.split("=", 1)[-1]
    else:
        return fallback