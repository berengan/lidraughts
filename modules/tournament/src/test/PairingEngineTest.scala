package lidraughts.tournament.itaswiss

import org.specs2.mutable.Specification

class PairingEngineTest extends Specification {

  private def zeroScores(n: Int) = (1 to n).map(_ -> 0d).toMap

  "FID Italian Swiss pairing engine" should {

    "pair the first ART2 round in initial competition-number order" in {
      val result = PairingEngine.generate(State(
        playerCount = 6,
        round = 1,
        roundCount = 4,
        scores = zeroScores(6),
        format = Format.Art2
      ))

      result.pairings must_== List(Pairing(1, 2), Pairing(3, 4), Pairing(5, 6))
      result.rests must beEmpty
      result.pairingStartNumber must_== 1
    }

    "apply the ART8 first-round rest to the last competition number" in {
      val result = PairingEngine.generate(State(
        playerCount = 5,
        round = 1,
        roundCount = 4,
        scores = zeroScores(5),
        format = Format.Art8
      ))

      result.pairings must_== List(Pairing(1, 2), Pairing(3, 4))
      result.rests must_== List(Rest(5, RestType.RM))
    }

    "apply the Kosmos/FID ART9 first-round RT block" in {
      val result = PairingEngine.generate(State(
        playerCount = 15,
        round = 1,
        roundCount = 7,
        scores = zeroScores(15),
        format = Format.Art9
      ))

      result.pairings must_== List(Pairing(1, 2), Pairing(3, 4), Pairing(5, 6))
      result.rests must_== (7 to 15).map(Rest(_, RestType.RT)).toList
    }

    "start the next rotation from the previous last Black without RM" in {
      val history = RoundHistory(
        pairings = List(Pairing(1, 2), Pairing(3, 4), Pairing(5, 6)),
        rests = Nil
      )
      val result = PairingEngine.generate(State(
        playerCount = 6,
        round = 2,
        roundCount = 4,
        scores = zeroScores(6),
        history = Map(1 -> history),
        format = Format.Art2
      ))

      result.pairingStartNumber must_== 6
    }

    "start the next Italian rotation from the previous RM" in {
      val history = RoundHistory(
        pairings = List(Pairing(1, 2), Pairing(3, 4)),
        rests = List(Rest(5, RestType.RM))
      )
      val result = PairingEngine.generate(State(
        playerCount = 5,
        round = 2,
        roundCount = 4,
        scores = Map(1 -> 2d, 2 -> 0d, 3 -> 2d, 4 -> 0d, 5 -> 2d),
        history = Map(1 -> history),
        format = Format.Art8
      ))

      result.pairingStartNumber must_== 5
    }

    "match Kosmos v15.81 ART8 round 2 including backtracking outcome" in {
      val history = RoundHistory(
        pairings = List(Pairing(1, 2), Pairing(3, 4)),
        rests = List(Rest(5, RestType.RM))
      )
      val result = PairingEngine.generate(State(
        playerCount = 5,
        round = 2,
        roundCount = 4,
        scores = Map(1 -> 2d, 2 -> 0d, 3 -> 1d, 4 -> 1d, 5 -> 2d),
        history = Map(1 -> history),
        format = Format.Art8
      ))

      result.pairings must_== List(Pairing(5, 3), Pairing(4, 1))
      result.rests must_== List(Rest(2, RestType.RM))
      result.pairingStartNumber must_== 5
    }

    "match Kosmos v15.81 ART9 round 2 RT and color rules" in {
      val history = RoundHistory(
        pairings = List(Pairing(1, 2), Pairing(3, 4), Pairing(5, 6)),
        rests = (7 to 15).map(Rest(_, RestType.RT)).toList
      )
      val result = PairingEngine.generate(State(
        playerCount = 15,
        round = 2,
        roundCount = 7,
        scores = Map(
          1 -> 2d, 2 -> 0d, 3 -> 1d, 4 -> 1d, 5 -> 0d, 6 -> 2d,
          7 -> 0d, 8 -> 0d, 9 -> 0d, 10 -> 0d, 11 -> 0d, 12 -> 0d,
          13 -> 0d, 14 -> 0d, 15 -> 0d
        ),
        history = Map(1 -> history),
        format = Format.Art9
      ))

      result.pairings must_== List(
        Pairing(6, 3), Pairing(4, 5), Pairing(7, 8), Pairing(9, 10),
        Pairing(11, 12), Pairing(13, 14), Pairing(2, 15)
      )
      result.rests must_== List(Rest(1, RestType.RT))
      result.pairingStartNumber must_== 6
    }

    "keep a player withdrawn during the current round in that round history and exclude them from the next pairing" in {
      val round1 = RoundHistory(
        pairings = List(Pairing(1, 2), Pairing(3, 4)),
        rests = Nil
      )
      val result = PairingEngine.generate(State(
        playerCount = 4,
        round = 2,
        roundCount = 3,
        scores = Map(1 -> 2d, 2 -> 0d, 3 -> 2d, 4 -> 0d),
        history = Map(1 -> round1),
        format = Format.Art2,
        retiredAt = Map(4 -> 1)
      ))

      result.retired must_== List(4)
      result.pairings.flatMap(p => List(p.white, p.black)) must not contain 4
    }

    "match Kosmos v15.81 with a withdrawn player and an RR" in {
      val history = RoundHistory(
        pairings = List(Pairing(1, 2), Pairing(3, 4), Pairing(5, 6)),
        rests = Nil
      )
      val result = PairingEngine.generate(State(
        playerCount = 6,
        round = 2,
        roundCount = 4,
        scores = Map(1 -> 2d, 2 -> 0d, 3 -> 1d, 4 -> 1d, 5 -> 2d, 6 -> 0d),
        history = Map(1 -> history),
        format = Format.Art2,
        retiredAt = Map(6 -> 1)
      ))

      result.pairings must_== List(Pairing(1, 3), Pairing(4, 5))
      result.rests must_== List(Rest(2, RestType.RR))
      result.retired must_== List(6)
      result.pairingStartNumber must_== 6
    }

    "classify a rest as RM when all withdrawn opponents were already played" in {
      val history = RoundHistory(
        pairings = List(Pairing(1, 2), Pairing(3, 4), Pairing(5, 6)),
        rests = Nil
      )
      val result = PairingEngine.generate(State(
        playerCount = 6,
        round = 2,
        roundCount = 4,
        scores = Map(1 -> 0d, 2 -> 2d, 3 -> 1d, 4 -> 1d, 5 -> 2d, 6 -> 0d),
        history = Map(1 -> history),
        format = Format.Art2,
        retiredAt = Map(2 -> 1)
      ))

      result.rests.forall { rest =>
        if (rest.player == 1) rest.restType == RestType.RM else true
      } must beTrue
    }

    "never assign a second RM to a player who already received RM or RR" in {
      val round1 = RoundHistory(
        pairings = List(Pairing(1, 2), Pairing(3, 4)),
        rests = List(Rest(5, RestType.RM))
      )
      val round2 = RoundHistory(
        pairings = List(Pairing(5, 3), Pairing(4, 1)),
        rests = List(Rest(2, RestType.RM))
      )
      val result = PairingEngine.generate(State(
        playerCount = 5,
        round = 3,
        roundCount = 4,
        scores = Map(1 -> 3d, 2 -> 2d, 3 -> 1d, 4 -> 2d, 5 -> 4d),
        history = Map(1 -> round1, 2 -> round2),
        format = Format.Art8
      ))

      result.pairings must_== List(Pairing(1, 5), Pairing(2, 4))
      result.rests must_== List(Rest(3, RestType.RM))
      result.pairingStartNumber must_== 2
    }
  }
}
