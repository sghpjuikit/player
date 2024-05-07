import unittest
from typing import Union
from util_str import float_to_words, replace_numbers_with_words
import re


class TestNumToWords(unittest.TestCase):

    def test_replace_numbers_with_words(self):
        self.assertEqual(replace_numbers_with_words("it is 0 cents"), "it is zero cents")
        self.assertEqual(replace_numbers_with_words("it is 0.0 cents"), "it is zero point zero cents")
        self.assertEqual(replace_numbers_with_words("it is -0 cents"), "it is minus zero cents")
        self.assertEqual(replace_numbers_with_words("it is +0 cents"), "it is plus zero cents")
        self.assertEqual(replace_numbers_with_words("it is -0.0 cents"), "it is minus zero point zero cents")
        self.assertEqual(replace_numbers_with_words("it is +0.0 cents"), "it is plus zero point zero cents")

    def test_zero(self):
        self.assertEqual(float_to_words("0"), "zero")
        self.assertEqual(float_to_words("0.0"), "zero point zero")
        self.assertEqual(float_to_words("-0"), "minus zero")
        self.assertEqual(float_to_words("+0"), "plus zero")
        self.assertEqual(float_to_words("-0.0"), "minus zero point zero")
        self.assertEqual(float_to_words("+0.0"), "plus zero point zero")

        self.assertEqual(float_to_words(0), "zero")
        self.assertEqual(float_to_words(0.0), "zero point zero")
        self.assertEqual(float_to_words(-0), "zero")
        self.assertEqual(float_to_words(+0), "zero")
        self.assertEqual(float_to_words(-0.0), "minus zero point zero")
        self.assertEqual(float_to_words(+0.0), "zero point zero")

    def test_positive_floats(self):
        self.assertEqual(float_to_words(123.456), "one hundred and twenty-three point four five six")
        self.assertEqual(float_to_words(0.5), "zero point five")

    def test_negative_floats(self):
        self.assertEqual(float_to_words(-123.456), "minus one hundred and twenty-three point four five six")
        self.assertEqual(float_to_words(-0.5), "minus zero point five")

    def test_decimal_part_only(self):
        self.assertEqual(float_to_words(0.123456), "zero point one two three four five six")

    def test_integer_part_only(self):
        self.assertEqual(float_to_words(123), "one hundred and twenty-three")

    def test_mixed_parts(self):
        self.assertEqual(float_to_words(123.456789),
                         "one hundred and twenty-three point four five six seven eight nine")
        self.assertEqual(float_to_words(0.987654321), "zero point nine eight seven six five four three two one")


if __name__ == '__main__':
    unittest.main()
