"""Offline regression tests for the Python <-> Scala Draughtsnet JSON contract.

No real network calls, engine execution, or moves sent to a running server.
"""
import io
import json
import sys
import unittest
from pathlib import Path
from unittest.mock import patch

import worker


class FakeResponse:
    def __init__(self, status, payload=None):
        self.status = status
        self.payload = payload

    def __enter__(self):
        return self

    def __exit__(self, *_):
        return False

    def read(self, *_):
        return json.dumps(self.payload).encode("utf-8")


class ProtocolContractTests(unittest.TestCase):
    def test_acquire_payload_uses_italian_engine_name(self):
        response = FakeResponse(204)
        with patch("worker.urllib.request.urlopen", return_value=response) as urlopen:
            result = worker.request("http://127.0.0.1:9001/", "/draughtsnet/acquire", {
                "draughtsnet": {"version": "1.0.0", "apikey": "test-only"},
                "scan": {"name": worker.ENGINE},
            })
        self.assertIsNone(result)
        req = urlopen.call_args.args[0]
        self.assertEqual(req.full_url, "http://127.0.0.1:9001/draughtsnet/acquire")
        self.assertEqual(req.get_method(), "POST")
        self.assertEqual(json.loads(req.data), {
            "draughtsnet": {"version": "1.0.0", "apikey": "test-only"},
            "scan": {"name": "LiFiDama-Italian"},
        })

    def test_acquired_work_preserves_current_fen_and_level(self):
        job = {
            "work": {"type": "move", "id": "abcdefgh", "level": 3, "clock": None},
            "game_id": "12345678", "variant": "italian",
            "position": "W:B1-12:W21-32", "moves": "",
            "currentFen": "W:W22:B18,10",
        }
        with patch("worker.urllib.request.urlopen", return_value=FakeResponse(202, job)):
            result = worker.request("http://127.0.0.1:9001", "/draughtsnet/acquire", {
                "draughtsnet": {"version": "1.0.0", "apikey": "test-only"},
                "scan": {"name": worker.ENGINE},
            })
        self.assertEqual(worker.validate_work(result), ("W:W22:B18,10", "intermediate"))

    def test_once_never_posts_a_move(self):
        job = {
            "work": {"type": "move", "id": "abcdefgh", "level": 1},
            "variant": "italian", "currentFen": "W:B1-12:W21-32",
        }
        argv = [
            "worker.py", "--once", "--key", "test-only",
            "--url", "http://127.0.0.1:9001",
            "--engine", str(Path(__file__)),
            "--config", str(Path(__file__)),
        ]
        with patch.object(sys, "argv", argv), \
             patch("worker.request", return_value=job) as request, \
             patch("worker.compute", return_value={"bestmove": "2118", "taken": ""}) as compute:
            worker.main()
        self.assertEqual(request.call_count, 1)
        self.assertEqual(request.call_args.args[1], "/draughtsnet/acquire")
        self.assertEqual(compute.call_count, 1)
        self.assertEqual(compute.call_args.args[2], "W:B1-12:W21-32")

    def test_post_is_rejected_with_once(self):
        argv = [
            "worker.py", "--once", "--post", "--key", "test-only",
            "--engine", str(Path(__file__)), "--config", str(Path(__file__)),
        ]
        with patch.object(sys, "argv", argv), self.assertRaises(SystemExit) as err:
            worker.main()
        self.assertEqual(err.exception.code, 2)


if __name__ == "__main__":
    unittest.main()
