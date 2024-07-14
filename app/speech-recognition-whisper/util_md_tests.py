import unittest
from util_md import *


class TestDirCache(unittest.TestCase):

    def test_mixed_parts(self):
        self.assertEqual(
            index_md_text('## Introduction\nA\n## Integration\nB\n### Mic\nC\n#### Sensitivity\nD'),
            {'Integration': 'B', 'Integration > Mic': 'C', 'Integration > Mic > Sensitivity': 'D', 'Introduction': 'A'}
        )

if __name__ == '__main__':
    unittest.main()