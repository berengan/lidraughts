package lidraughts.tournament

import org.specs2.mutable.Specification

class SystemTest extends Specification {

  "Tournament systems" should {

    "keep Arena as the backwards-compatible default" in {
      System.default must_== System.Arena
      System.orDefault(999) must_== System.Arena
    }

    "persist ItaSwiss with its own stable id" in {
      System(2) must beSome(System.ItaSwiss)
      System.ItaSwiss.default must beFalse
      System.ItaSwiss.berserkable must beFalse
      System.ItaSwiss.key must_== "itaSwiss"
      System.Arena.key must_== "arena"
    }
  }
}
