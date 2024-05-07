import re
import sys

def contains_any(text: str, fragments: [str], ignore_case: bool = False) -> bool:
    def sanitize(s): return s.lower() if ignore_case else s
    text = sanitize(text)
    return any(sanitize(fragment) in text for fragment in fragments)

def starts_with_any(t: str, prefixes: [str]) -> bool:
    for prefix in prefixes:
        if t.lower().startswith(prefix):
            return True
    return False

def ends_with_any(t: str, suffixes: [str]) -> bool:
    for prefix in suffixes:
        if t.lower().endswith(prefix):
            return True
    return False

def remove_any_prefix(t: str, prefixes: [str]) -> str:
    for prefix in prefixes:
        if t.lower().startswith(prefix):
            t = t[len(prefix):]
    return t

def remove_any_suffix(t, suffixes: [str]) -> str:
    for suffix in suffixes:
        if t.lower.endswith(suffix):
            t = t[:len(t) - len(suffix)]
    return t

def wake_words_and_name(s: str) -> (str, [str]):
    s = re.sub(',\s*', ',', s).lower()
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


def int_to_words(num: int) -> str:
    units = ("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
    tens = ("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
    if num < 0:
        return "minus " + int_to_words(-num)
    if num == 0:
        return "zero"
    if num < 20:
        return units[num]
    if num < 100:
        return tens[num // 10] + ("-" + units[num % 10] if num % 10 != 0 else "")
    if num < 1000:
        return units[num // 100] + " hundred" + (" and " + int_to_words(num % 100) if num % 100 != 0 else "")
    if num < 1000000:
        return int_to_words(num // 1000) + " thousand" + (" " + int_to_words(num % 1000) if num % 1000 != 0 else "")
    if num < 1000000000:
        return int_to_words(num // 1000000) + " million" + (" " + int_to_words(num % 1000000) if num % 1000000 != 0 else "")
    if num < 1000000000000:
        return int_to_words(num // 1000000000) + " billion" + (" " + int_to_words(num % 1000000000) if num % 1000000000 != 0 else "")
    else:
        return int_to_words(num // 1000000000000) + " trillion" + (" " + int_to_words(num % 1000000000000) if num % 1000000000000 != 0 else "")

def float_to_words(num: float):
    return floatstr_to_words(str(num))

def floatstr_to_words(numstr: str):
    numstr = numstr.strip()

    if numstr.startswith('+'):
        return "plus " + floatstr_to_words(numstr.removeprefix('+'))
    if numstr.startswith('-'):
        return "minus " + floatstr_to_words(numstr.removeprefix('-'))

    def floating_to_words(num_str: float):
        result = []
        for digit in num_str: result.append(int_to_words(int(digit)))
        return ' '.join(result)

    num = float(numstr)
    integer_part = int(num)
    if '.' not in numstr: return int_to_words(integer_part)
    else: return f"{int_to_words(integer_part)} point {floating_to_words(numstr.split('.')[1])}"

def num_to_words(num: int | float) -> str:
    if isinstance(num, float):
        return float_to_words(num)
    else:
        return int_to_words(int(num))

def replace_numbers_with_words(text) -> str:
    import re
    pattern = r"[-+]?\d*\.\d+|[-+]?\d+" # find all numbers, including negative and fractions
    matches = re.findall(pattern, text)
    t = text
    for match in matches: t = t.replace(match, floatstr_to_words(match)) # Replace each match with its English words
    return t