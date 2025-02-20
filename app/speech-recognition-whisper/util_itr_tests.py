import unittest
from util_itr import *


class TestDirCache(unittest.TestCase):


    def test_chunks(self):
        self.assertEqual(list(chunks('', '⏹')), [])
        self.assertEqual(list(chunks('a', '⏹')), ['a'])
        self.assertEqual(list(chunks('a⏹b', '⏹')), ['a', 'b'])

        self.assertEqual(list(chunks([''], '⏹')), [])
        self.assertEqual(list(chunks(['a'], '⏹')), ['a'])
        self.assertEqual(list(chunks(['a', '⏹', 'b'], '⏹')), ['a', 'b'])

    def test_lines(self):
        self.assertEqual(list(lines('')), [])
        self.assertEqual(list(lines('a')), ['a'])
        self.assertEqual(list(lines('a\nb')), ['a', 'b'])

        self.assertEqual(list(lines([''])), [])
        self.assertEqual(list(lines(['a'])), ['a'])
        self.assertEqual(list(lines(['a', '\n', 'b'])), ['a', 'b'])

    def test_linesWithNewline(self):
        self.assertEqual(list(linesWithNewline('')), [])
        self.assertEqual(list(linesWithNewline('a')), ['a'])
        self.assertEqual(list(linesWithNewline('a\nb')), ['a', '\n','b'])

        self.assertEqual(list(linesWithNewline([''])), [])
        self.assertEqual(list(linesWithNewline(['a'])), ['a'])
        self.assertEqual(list(linesWithNewline(['a', '\n', 'b'])), ['a', '\n', 'b'])

    def test_skipThinking(self):
        self.assertEqual(list(skipThinking('')), [])

        self.assertEqual(list(skipThinking([''])), [])
        self.assertEqual(list(skipThinking(['<think>'])), [])
        self.assertEqual(list(skipThinking(['<think>', '\n', 'a', '\n', '</think>', '\n'])), ['thinkPassive("a")'])
        self.assertEqual(list(skipThinking(['<think>', '\n', 'a', '\n', '</think>', '\n', 'b'])), ['thinkPassive("a")', '\n', 'b'])
        self.assertEqual(list(skipThinking(['<think>', '\na\n', '</think>', '\n'])), ['thinkPassive("a")'])
        self.assertEqual(list(skipThinking(['<think>', '\na\n', '</think>', '\nb\nc'])), ['thinkPassive("a")', '\n', 'b', '\n', 'c'])

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


    def test_noThinking(self):

        # test_empty
        data = []
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), [])

        # test_no_think_tag
        data = ["no think tag"]
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), data)

        # test_simple_think_tag
        data = ["<think>\n", "</think>"]
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), [])

        # test_think_tag_with_content
        data = ["<think>","first line\n", "second line\n","</think>","after think"]
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), ["after think"])

        # test_think_tag_with_prefix
        data = ["prefix","<think>","first line\n", "second line\n","</think>","after think"]
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), ["prefix<think>","first line\n", "second line\n","</think>","after think"])

        # test_split_think_tag
        data = ["pre","fi","x","<think>","first line\n", "second line\n","</think>","after think"]
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), ["prefix<think>","first line\n", "second line\n","</think>","after think"])

        # test_multiple_chunks
        data = ["Some text before", "<think>", "This is a ", "thought", " that spans", " multiple chunks.\n", "</think>", "Some text after"]
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), data)

        # test_no_newline_before_end_tag
        #Test that without newline before closing tag function does not filter
        data = ["<think>","first line", "</think>","after think"]
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), ["after think"])

        # test_end_tag_in_same_chunk
        data = ["<think>test\n</think>after"]
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), ["after"])

        # test_prefix_and_end_tag_same_chunk
        data = ["prefix<think>test\n</think>after"]
        filtered_generator = noThinking(data)
        self.assertEqual(list(filtered_generator), data)


if __name__ == '__main__':
    unittest.main()
