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
  }
}
