import subprocess
import unittest
from pathlib import Path
from unittest.mock import patch

from worker import compute, normalize_fen, level_for, parse_bestmove, validate_work


class ProtocolTests(unittest.TestCase):
    def test_all_eight_numeric_levels_map_to_four_presets(self):
        self.assertEqual([level_for(i) for i in range(1, 9)],
            ["beginner", "beginner", "intermediate", "intermediate",
             "professional", "professional", "ultra", "ultra"])
        for bad in (0, 9, "1", True, None):
            with self.assertRaises(ValueError):
                level_for(bad)

    def test_fen_validation(self):
        for valid in ("W:B1-12:W21-32", "B:W21-32:B1-12",
                      "W:WK12:B3", "W:B:W"):
            self.assertEqual(normalize_fen(valid), valid)
        for bad in ("W:B1-50:W21-32", "W:WK33:B1",
                    "W:W21-32", "", "W:B1-12:W21-32:EXTRA"):
            with self.subTest(fen=bad), self.assertRaises(ValueError):
                normalize_fen(bad)

    def test_move_notation(self):
        self.assertEqual(parse_bestmove("bestmove=23-19"), ("2319", "-"))
        self.assertEqual(parse_bestmove("bestmove=16x23"), ("1623", "x"))
        for bad in ("bestmove=33-01", "bestmove=03-03", "bestmove=(none)"):
            with self.subTest(move=bad), self.assertRaises(ValueError):
                parse_bestmove(bad)

    def test_realistic_draughtsnet_work_json(self):
        job = {"work": {"type": "move", "id": "abcdefgh", "level": 6,
                        "clock": None},
               "variant": "italian", "currentFen": "W:B1-12:W21-32",
               "game_id": "game1234", "position": "W:B1-12:W21-32",
               "moves": ""}
        self.assertEqual(validate_work(job),
                         ("W:B1-12:W21-32", "professional"))
        job["variant"] = "standard"
        with self.assertRaises(ValueError):
            validate_work(job)
        job["variant"] = "italian"
        job["work"]["level"] = 9
        with self.assertRaises(ValueError):
            validate_work(job)

    @patch("worker.subprocess.run")
    def test_quiet_move_adapter(self, run):
        run.return_value = subprocess.CompletedProcess(
            args=[], returncode=0, stdout="bestmove=21-18\n", stderr="")
        result = compute(Path("/tmp/dama-linux"), Path("/tmp/engine-levels.ini"),
                         "W:B1-12:W21-32", 1)
        self.assertEqual(result, {"bestmove": "2118", "taken": ""})
        argv = run.call_args.args[0]
        self.assertIn("beginner", argv)
        self.assertIn("W:B1-12:W21-32", argv)
        self.assertEqual(run.call_args.kwargs["timeout"], 8)

    @patch("worker.subprocess.run")
    def test_capture_not_posted_without_taken_protocol(self, run):
        run.return_value = subprocess.CompletedProcess(
            args=[], returncode=0, stdout="bestmove=16x23\n", stderr="")
        with self.assertRaisesRegex(ValueError, "capture"):
            compute(Path("/tmp/dama-linux"), Path("/tmp/engine-levels.ini"),
                    "W:B1-12:W21-32", 3)


if __name__ == "__main__":
    unittest.main()
