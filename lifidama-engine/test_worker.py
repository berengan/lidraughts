import unittest
from worker import normalize_fen, level_for, parse_bestmove, validate_work

class Tests(unittest.TestCase):
    def test_levels(self):
        self.assertEqual([level_for(i) for i in range(1,9)],
          ["beginner","beginner","intermediate","intermediate",
           "professional","professional","ultra","ultra"])
        for bad in (0,9,"1",True):
            with self.assertRaises(ValueError): level_for(bad)
    def test_fen(self):
        self.assertEqual(normalize_fen("W:B1-12:W21-32"),"W:B1-12:W21-32")
        for bad in ("W:B1-50:W21-32","W:W21-32",""):
            with self.assertRaises(ValueError): normalize_fen(bad)
    def test_moves(self):
        self.assertEqual(parse_bestmove("bestmove=23-19"),("2319","-"))
        self.assertEqual(parse_bestmove("bestmove=16x23"),("1623","x"))
    def test_foreign(self):
        job={"work":{"type":"move","id":"abcdefgh"},
             "variant":"standard","level":1,"currentFen":"W:B1-12:W21-32"}
        with self.assertRaises(ValueError): validate_work(job)
        job["variant"]="italian"
        self.assertEqual(validate_work(job)[1],"beginner")

if __name__=="__main__": unittest.main()
