import glob
import os
import unittest

REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SQL_ROOT = os.path.join(REPO_ROOT, "support-files", "sql")
UTF8_BOM = b"\xef\xbb\xbf"


class SqlMigrationEncodingTest(unittest.TestCase):

    def test_migration_files_have_no_utf8_bom(self):
        """mysql CLI fails with a syntax error when a migration file starts with a UTF-8 BOM."""
        files = glob.glob(os.path.join(SQL_ROOT, "**", "*.sql"), recursive=True)
        self.assertTrue(files, "no sql migration files found")
        offenders = []
        for path in files:
            with open(path, "rb") as f:
                if f.read(3) == UTF8_BOM:
                    offenders.append(os.path.relpath(path, REPO_ROOT).replace(os.sep, "/"))
        self.assertEqual(
            [], offenders,
            "SQL migration files must not start with a UTF-8 BOM: %s" % ", ".join(offenders)
        )


if __name__ == "__main__":
    unittest.main()
