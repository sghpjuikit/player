import unittest
from util_itr import *


class TestDirCache(unittest.TestCase):


    def test_lines(self):
        self.assertEqual(list(chunks('', '⏹')), [])
        self.assertEqual(list(chunks('a', '⏹')), ['a'])
        self.assertEqual(list(chunks('a⏹b', '⏹')), ['a', 'b'])

    def test_chunks(self):
        self.assertEqual(list(lines('')), [])
        self.assertEqual(list(lines('a')), ['a'])
        self.assertEqual(list(lines('a\nb')), ['a', 'b'])

    def test_python_code_chunks(self):
        # normal
        self.assertEqual(
            ['speak("a")', 'while True:\n  speak("b")\n  print("d")', 'speak("c")'],
            list(python_code_chunks('speak("a")\nwhile True:\n  speak("b")\n  print("d")\nspeak("c")'))
        )
        # normal with * prefix/suffix, which is common llm problem
        self.assertEqual(
            ['*speak("Oh, so dramatic")*', 'body("bat eyelashes")'],
            list(python_code_chunks('*speak("Oh, so dramatic")*\nbody("bat eyelashes")'))
        )
        # multiline string ''', """
        self.assertEqual(
            ['print("a")', 'print("""a\nb\nc""")', 'print("a")'],
            list(python_code_chunks('print("a")\nprint("""a\nb\nc""")\nprint("a")'))
        )
        self.assertEqual(
            ["print('a')", "print('''a\nb\nc''')", "print('a')"],
            list(python_code_chunks("print('a')\nprint('''a\nb\nc''')\nprint('a')"))
        )
        # unterminated ordinary strings should not cause complete corruption
        self.assertEqual(
            ['print("a)', 'print("a")'],
            list(python_code_chunks('print("a)\nprint("a")'))
        )
        self.assertEqual(
            ["print('a)", "print('a')"],
            list(python_code_chunks("print('a)\nprint('a')"))
        )


if __name__ == '__main__':
    unittest.main()
