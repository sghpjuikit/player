import re
import sys

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


def int_to_words(num: int):
    units = ("", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
    tens = ("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
    if num < 0:
        return "minus " + convert_to_words(-num)
    if num < 20:
        return units[num]
    if num < 100:
        return tens[num // 10] + (" " + units[num % 10] if num % 10 != 0 else "")
    if num < 1000:
        return units[num // 100] + " hundred" + (" and " + convert_to_words(num % 100) if num % 100 != 0 else "")
    if num < 1000000:
        return convert_to_words(num // 1000) + " thousand" + (" " + convert_to_words(num % 1000) if num % 1000 != 0 else "")
    if num < 1000000000:
        return convert_to_words(num // 1000000) + " million" + (" " + convert_to_words(num % 1000000) if num % 1000000 != 0 else "")
    else:
        return convert_to_words(num // 1000000000) + " billion" + (" " + convert_to_words(num % 1000000000) if num % 1000000000 != 0 else "")

def float_to_words(num: float):

    def floating_to_words(num: float):
        num_str = str(num)[2:]  # Remove "0." prefix
        result = []
        for digit in num_str: result.append(int_to_words(int(digit)))
        return ' '.join(result)

    integer_part = int(num)
    decimal_part = abs(num - integer_part)
    if decimal_part == 0: return int_to_words(integer_part)
    else: return f"{int_to_words(integer_part)} point {floating_to_words(decimal_part)}"

def num_to_words(num: str):
    return float_to_words(float(num))

def replace_numbers_with_words(text):
    import re
    # Regular expression to find all numbers, including negative and fractions
    pattern = r"[-+]?\d*\.\d+|[-+]?\d+"
    matches = re.findall(pattern, text)

    # Replace each match with its English word representation
    for match in matches:
        text = text.replace(match, num_to_words(match))

    return text