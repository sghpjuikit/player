
ignored_chars = set(".,?!-: ")

print(str(len(''.join(c for c in '' if c not in ignored_chars))>0), flush=True)
print(str(len(''.join(c for c in ' ' if c not in ignored_chars))>0), flush=True)
print(str(len(''.join(c for c in '  ' if c not in ignored_chars))>0), flush=True)
print(str(len(''.join(c for c in '_' if c not in ignored_chars))>0), flush=True)
print(str(len(''.join(c for c in '__' if c not in ignored_chars))>0), flush=True)
print(str(len(''.join(c for c in '.' if c not in ignored_chars))>0), flush=True)
print(str(len(''.join(c for c in '..' if c not in ignored_chars))>0), flush=True)
print(str(len(''.join(c for c in '. ' if c not in ignored_chars))>0), flush=True)
print(str(len(''.join(c for c in ' ? ' if c not in ignored_chars))>0), flush=True)