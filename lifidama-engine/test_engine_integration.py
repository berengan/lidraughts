"""End-to-end native engine regression tests.

Opt in by setting LIFIDAMA_ENGINE to the compiled Linux executable.
These tests do not contact the web application or Draughtsnet.
"""
import os
import re
import subprocess
import unittest
from pathlib import Path

from worker import compute

ENGINE_PATH = os.environ.get("LIFIDAMA_ENGINE")
ENGINE = Path(ENGINE_PATH) if ENGINE_PATH else None
CONFIG = Path(os.environ.get("LIFIDAMA_CONFIG", str(Path(__file__).with_name("engine-levels.ini"))))
INITIAL = "W:B1-12:W21-32"


@unittest.skipUnless(ENGINE_PATH, "set LIFIDAMA_ENGINE to run native integration tests")
class NativeEngineTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not ENGINE.is_file():
            raise RuntimeError(f"native engine not found: {ENGINE}")
        if not CONFIG.is_file():
            raise RuntimeError(f"engine configuration not found: {CONFIG}")

    def invoke(self, *args, timeout=30):
        return subprocess.run([str(ENGINE), *args], text=True,
                              capture_output=True, timeout=timeout, check=True).stdout

    def test_initial_legal_moves(self):
        output = self.invoke("--list", "--fen", INITIAL)
        moves = [line.strip() for line in output.splitlines() if re.fullmatch(r"\d{1,2}[-x]\d{1,2}", line.strip())]
        self.assertEqual(moves, ["21-18", "21-17", "22-19", "22-18",
                                 "23-20", "23-19", "24-20"])
        self.assertIn("legal_moves=7", output)

    def test_perft_reference_depth_five(self):
        output = self.invoke("--perft", "--depth", "5", "--fen", INITIAL)
        results = dict((int(depth), int(count))
                       for depth, count in re.findall(r"perft\((\d+)\)=(\d+)", output))
        self.assertEqual(results, {1: 7, 2: 49, 3: 302, 4: 1469, 5: 7361})

    def test_mandatory_single_capture(self):
        output = self.invoke("--list", "--fen", "W:W22:B18")
        self.assertIn("legal_moves=1", output)
        self.assertRegex(output, r"(?m)^22x13$")

    def test_multiple_capture(self):
        output = self.invoke("--list", "--fen", "W:W22:B18,10")
        self.assertIn("legal_moves=1", output)
        self.assertRegex(output, r"(?m)^22x6$")
        # The native engine only emits endpoints, not the captured squares.
        # Never submit this move to Draughtsnet until 'taken' is verified.
        with self.assertRaisesRegex(ValueError, "capture"):
            compute(ENGINE, CONFIG, "W:W22:B18,10", 1)

    def test_king_capture(self):
        output = self.invoke("--list", "--fen", "W:WK22:B18,10")
        self.assertIn("legal_moves=1", output)
        self.assertRegex(output, r"(?m)^22x6$")

    def test_all_four_levels_produce_legal_initial_move(self):
        legal = {"2118", "2117", "2219", "2218", "2320", "2319", "2420"}
        for level in (1, 3, 6, 8):
            with self.subTest(level=level):
                result = compute(ENGINE, CONFIG, INITIAL, level)
                self.assertIn(result["bestmove"], legal)
                self.assertEqual(result["taken"], "")


if __name__ == "__main__":
    unittest.main()
