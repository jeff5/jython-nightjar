import subprocess
import sys
import unittest
from test import test_support

class CodecsTestCase(unittest.TestCase):

    def test_print_sans_lib(self):
        # Encode and decode using utf-8 in an environment without the standard
        # library, to check that a utf-8 codec is always available. See:
        # http://bugs.jython.org/issue1458
        subprocess.call([sys.executable, "-J-Dpython.cachedir.skip=true",
            "-S", # No site module: avoid codec registry initialised too soon
            test_support.findfile('print_sans_lib.py')])

    def test_string_escape_1502(self):
        # http://bugs.jython.org/issue1502
        self.assertEqual('\\x00'.encode('string-escape'), '\\\\x00')
        self.assertEqual('\\x00'.encode('unicode-escape'), '\\\\x00')


def test_main():
    test_support.run_unittest(CodecsTestCase)


if __name__ == "__main__":
    test_main()
