import unittest
from util_dir_cache import *


class TestDirCache(unittest.TestCase):


    def test_mixed_parts(self):
        self.assertEqual(sanitize_filename(''), '')
        self.assertEqual(sanitize_filename('aAbBcC09'), 'aabbcc09')
        self.assertEqual(sanitize_filename('?!:'), '')
        self.assertEqual(sanitize_filename("I'm not sure that's appropriate."), 'i-m-not-sure-that-s-appropriate')
        self.assertEqual(sanitize_filename('a?!:a'), 'a---a')


if __name__ == '__main__':
    unittest.main()
