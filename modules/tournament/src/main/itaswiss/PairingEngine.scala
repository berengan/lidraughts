package lidraughts.tournament.itaswiss

sealed abstract class Format(val key: String, val article: Int)
object Format {
  case object Art2 extends Format("ITA_SWISS_FID_ART2", 2)
  case object Art8 extends Format("ITA_SWISS_FID_ART8", 8)
  case object Art9 extends Format("ITA_SWISS_FID_ART9", 9)
  val all = List(Art2, Art8, Art9)
  def byKey(key: String): Option[Format] = all.find(_.key == key)
}

sealed trait RestType
object RestType {
  case object RM extends RestType
  case object RT extends RestType
  case object RR extends RestType
}
case class Pairing(white: Int, black: Int)
case class Rest(player: Int, restType: RestType)
case class RoundHistory(pairings: List[Pairing], rests: List[Rest])
case class State(
    playerCount: Int,
    round: Int,
    roundCount: Int,
    scores: Map[Int, Double],
    history: Map[Int, RoundHistory] = Map.empty,
    format: Format = Format.Art2,
    retiredAt: Map[Int, Int] = Map.empty)
case class Result(round: Int, pairings: List[Pairing], rests: List[Rest], retired: List[Int], pairingStartNumber: Int)

/** Pure FID Italian-Swiss pairing engine.
  * Scala port of the certified KosmosWeb ItaSwissFIDPairingEngine.
  * Player ids are initial competition numbers (1..N), hence circular rotation order.
  */
object PairingEngine {
  private sealed trait Flag
  private case object Free extends Flag
  private case object Paired extends Flag
  private case object Resting extends Flag
  private case object TurnRest extends Flag
  private case object Retired extends Flag
  private case object Considered extends Flag
  private case class OpenPair(first: Int, second: Option[Int], inhibited: List[Int])

  def generate(s: State): Result = {
    require(s.playerCount > 1, "Italian Swiss requires at least two players")
    require(s.round >= 1 && s.round <= s.roundCount, "invalid round")
    require((1 to s.playerCount).forall(s.scores.contains), "missing player score")

    val n = s.playerCount
    val round = s.round
    val pairCount = c(s, round)
    val m = n - numRetired(s, round) - 2 * pairCount
    var flags = (1 to n).map(_ -> (Free: Flag)).toMap
    var restCandidates = Set.empty[Int]

    (1 to n).foreach(p => if (isRetiredBefore(s, p, round)) flags += p -> Retired)

    var art9Rests = 0
    if (m != 0) {
      if (s.format == Format.Art9) {
        if (round == 1) {
          (s.roundCount to n).foreach(p => flags += p -> TurnRest)
          art9Rests = n - s.roundCount + 1
        } else if (art9RestCount(s, round) != 0) {
          val eligible = (1 to n).filter(p => !hasRT(s, p, round - 1) && !isRetired(s, p))
          if (eligible.nonEmpty) {
            val bestScore = eligible.map(s.scores).max
            val best = eligible.find(s.scores(_) == bestScore).get
            flags += best -> TurnRest
            art9Rests = 1
          }
        }
      }

      if (m - art9Rests > 0) {
        if (round == 1) flags += n -> Resting
        else {
          val eligible = (1 to n).filter { p =>
            !isRetiredBefore(s, p, round) &&
            !hasRMR(s, p, round - 1) &&
            flags(p) != TurnRest &&
            (numRetired(s, round) == 0 || retiredPlayers(s, round).exists(r => !played(s, p, r, round - 1)))
          }
          if (eligible.size == 1) flags += eligible.head -> Resting
          else if (eligible.nonEmpty) {
            val minScore = eligible.map(s.scores).min
            val low = eligible.filter(s.scores(_) == minScore)
            if (low.size == 1) flags += low.head -> Resting
            else restCandidates ++= low
          }
        }
      }
    }

    val start = if (round == 1) 1 else rotationAnchor(s, round - 1)
    var stack = List.empty[OpenPair]
    var lastConsidered = start
    var dynamicRest: Option[(Int, Int)] = None
    var failed = false

    while (stack.size < pairCount && !failed) {
      flags = flags.map { case (p, Considered) => p -> (Free: Flag); case x => x }
      nextPlayer(flags, s.scores, lastConsidered, n) match {
        case None => failed = true
        case Some((selected, anchor)) =>
          var x = selected
          lastConsidered = anchor
          flags += x -> Paired
          stack = stack :+ OpenPair(x, None, Nil)
          var ok = false

          while (!ok && !failed) {
            nextPlayer(flags, s.scores, lastConsidered, n) match {
              case Some((y, nextAnchor)) =>
                lastConsidered = nextAnchor
                val current = stack.last
                if (played(s, x, y, round - 1) || current.inhibited.contains(y)) flags += y -> Considered
                else {
                  stack = stack.dropRight(1) :+ current.copy(second = Some(y))
                  flags += y -> Paired
                  ok = true

                  if (stack.size == pairCount) {
                    (1 to n).filter(_ != y).foreach { p =>
                      if (!Set[Flag](Paired, Resting, TurnRest, Retired).contains(flags(p))) {
                        if (hasRMR(s, p, round - 1)) {
                          flags += y -> Considered
                          stack = stack.dropRight(1) :+ stack.last.copy(second = None)
                          ok = false
                        } else if (restCandidates(p)) flags += p -> Resting
                        else {
                          flags += y -> Considered
                          stack = stack.dropRight(1) :+ stack.last.copy(second = None)
                          ok = false
                        }
                      }
                    }
                  }
                }

              case None =>
                val remaining = flags.values.count(f => f == Free || f == Considered) + 1
                if (remaining % 2 == 1 && restCandidates(x)) {
                  dynamicRest = Some(x -> stack.size)
                  flags += x -> Resting
                  stack = stack.dropRight(1)
                  lastConsidered = x
                  flags = flags.map { case (p, Considered) => p -> (Free: Flag); case z => z }
                  nextPlayer(flags, s.scores, lastConsidered, n) match {
                    case Some((next, nextAnchor)) =>
                      x = next
                      lastConsidered = nextAnchor
                      flags += x -> Paired
                      stack = stack :+ OpenPair(x, None, Nil)
                    case None => failed = true
                  }
                } else if (stack.size > 1) {
                  if (dynamicRest.exists(_._2 == stack.size)) {
                    flags += dynamicRest.get._1 -> Free
                    dynamicRest = None
                  }
                  stack = stack.dropRight(1)
                  val prev = stack.last
                  prev.second.foreach(p => flags += p -> Free)
                  stack = stack.dropRight(1) :+ prev.copy(second = None, inhibited = prev.second.toList ::: prev.inhibited)
                  flags += x -> Free
                  x = prev.first
                  lastConsidered = x
                  flags = flags.map { case (p, Considered) => p -> (Free: Flag); case z => z }
                } else failed = true
            }
          }
      }
    }

    if (failed) Result(round, Nil, rests(flags, s, round), retiredPlayers(s, round), start)
    else {
      val raw = stack.flatMap(p => p.second.map(p.first -> _))
      Result(round, allocateColors(s, round, raw), rests(flags, s, round), retiredPlayers(s, round), start)
    }
  }

  private def c(s: State, round: Int): Int = {
    val n = s.playerCount
    if (round == 1) {
      if (n % 2 == 0) n / 2
      else if (s.format == Format.Art9) (s.roundCount - 1) / 2
      else (n - 1) / 2
    } else {
      val remaining = n - numRetired(s, round)
      if (s.format == Format.Art9) {
        val activeWithoutRT = (1 to n).exists(p => !hasRT(s, p, round - 1) && !isRetired(s, p))
        (remaining - (if (activeWithoutRT) 1 else 0)) / 2
      } else remaining / 2
    }
  }

  private def nextPlayer(flags: Map[Int, Flag], scores: Map[Int, Double], anchor: Int, n: Int): Option[(Int, Int)] = {
    val free = (1 to n).filter(flags(_) == Free)
    if (free.isEmpty) None
    else {
      val max = free.map(scores).max
      (0 until n).map(i => ((anchor - 1 + i) % n) + 1)
        .find(p => flags(p) == Free && scores(p) == max).map(p => p -> p)
    }
  }

  private def allocateColors(s: State, round: Int, pairs: List[(Int, Int)]): List[Pairing] = pairs.map {
    case (a, b) if round == 1 => Pairing(a, b)
    case (a, b) if s.format == Format.Art9 && round == 2 && (restedRound1ByArt9(s, a) || restedRound1ByArt9(s, b)) =>
      val ar = restedRound1ByArt9(s, a)
      val br = restedRound1ByArt9(s, b)
      if (ar && br) if (a < b) Pairing(a, b) else Pairing(b, a)
      else if (ar) if (historicalColor(s, b, 1).contains('W')) Pairing(a, b) else Pairing(b, a)
      else if (historicalColor(s, a, 1).contains('W')) Pairing(b, a) else Pairing(a, b)
    case (a, b) =>
      var wa = 0
      var wb = 0
      var recentBlack = 0
      (round - 1 to 1 by -1).foreach { r =>
        val ca = historicalColor(s, a, r)
        val cb = historicalColor(s, b, r)
        if (ca.contains('W')) wa += 1
        if (cb.contains('W')) wb += 1
        if (recentBlack == 0) {
          if (ca.contains('B') && cb.contains('W')) recentBlack = 1
          else if (ca.contains('W') && cb.contains('B')) recentBlack = 2
        }
      }
      if (wa < wb) Pairing(a, b)
      else if (wa > wb) Pairing(b, a)
      else if (recentBlack == 1) Pairing(a, b)
      else if (recentBlack == 2) Pairing(b, a)
      else if (s.scores(a) > s.scores(b)) Pairing(a, b)
      else if (s.scores(b) > s.scores(a)) Pairing(b, a)
      else if (a < b) Pairing(a, b) else Pairing(b, a)
  }

  private def historicalColor(s: State, player: Int, round: Int): Option[Char] =
    s.history.get(round).flatMap { h =>
      h.pairings.collectFirst {
        case Pairing(`player`, _) => 'W'
        case Pairing(_, `player`) => 'B'
      }.orElse(if (h.rests.exists(_.player == player)) Some('B') else None)
    }

  private def restedRound1ByArt9(s: State, p: Int): Boolean = p > 2 * c(s, 1)

  private def rests(flags: Map[Int, Flag], s: State, round: Int): List[Rest] =
    flags.toList.sortBy(_._1).collect {
      case (p, TurnRest) => Rest(p, RestType.RT)
      case (p, Resting) => Rest(p, restType(s, p, round))
    }

  private def restType(s: State, player: Int, round: Int): RestType =
    if (retiredPlayers(s, round).exists(retired => !played(s, player, retired, round - 1))) RestType.RR
    else RestType.RM

  private def played(s: State, a: Int, b: Int, before: Int): Boolean =
    s.history.exists { case (r, h) =>
      r <= before && h.pairings.exists(p => (p.white == a && p.black == b) || (p.white == b && p.black == a))
    }

  private def hasRT(s: State, p: Int, before: Int): Boolean =
    s.history.exists { case (r, h) => r <= before && h.rests.exists(x => x.player == p && x.restType == RestType.RT) }

  private def hasRMR(s: State, p: Int, before: Int): Boolean =
    s.history.exists { case (r, h) =>
      r <= before && h.rests.exists(x => x.player == p && (x.restType == RestType.RM || x.restType == RestType.RR))
    }

  private def art9RestCount(s: State, round: Int): Int =
    if (s.format != Format.Art9) 0
    else if (round == 1) if (s.playerCount % 2 == 0) 0 else s.playerCount - (s.roundCount - 1)
    else if ((1 to s.playerCount).exists(p => !hasRT(s, p, round - 1) && !isRetired(s, p))) 1 else 0

  private def rotationAnchor(s: State, previousRound: Int): Int = {
    val restOrRetired = s.playerCount - 2 * c(s, previousRound)
    val rm = restOrRetired - art9RestCount(s, previousRound) - numRetired(s, previousRound)
    val prev = s.history.get(previousRound)
    if (rm != 0)
      prev.flatMap(h => h.rests.find(_.restType == RestType.RM).map(_.player).orElse(h.rests.lastOption.map(_.player))).getOrElse(1)
    else prev.flatMap(_.pairings.lastOption.map(_.black)).getOrElse(1)
  }

  private def isRetired(s: State, p: Int): Boolean = s.retiredAt.getOrElse(p, 0) != 0
  private def isRetiredBefore(s: State, p: Int, round: Int): Boolean = {
    val r = s.retiredAt.getOrElse(p, 0)
    r != 0 && r <= round - 1
  }
  private def numRetired(s: State, round: Int): Int = (1 to s.playerCount).count(p => isRetiredBefore(s, p, round))
  private def retiredPlayers(s: State, round: Int): List[Int] =
    (1 to s.playerCount).filter(p => isRetiredBefore(s, p, round)).toList
}