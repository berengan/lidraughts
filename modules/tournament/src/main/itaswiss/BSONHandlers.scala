package lidraughts.tournament.itaswiss

import reactivemongo.bson._
import draughts.StartingPosition

/** BSON representation kept separate from the legacy Tournament document.
  * It can be embedded under the optional "itaSwiss" field without changing
  * how existing Arena tournaments are decoded.
  */
private[tournament] object BSONHandlers {

  private def restTypeKey(restType: RestType) = restType match {
    case RestType.RM => "RM"
    case RestType.RT => "RT"
    case RestType.RR => "RR"
  }

  private def restType(key: String): Option[RestType] = key match {
    case "RM" => Some(RestType.RM)
    case "RT" => Some(RestType.RT)
    case "RR" => Some(RestType.RR)
    case _ => None
  }

  private def format(key: String): Format =
    Format.byKey(key).getOrElse(throw new IllegalArgumentException("Unknown ItaSwiss format: " + key))

  private implicit val pairingHandler = Macros.handler[Pairing]
  private implicit val competitionPlayerHandler = Macros.handler[CompetitionPlayer]

  private implicit val restHandler = new BSONHandler[BSONDocument, Rest] {
    def read(doc: BSONDocument) = Rest(
      doc.getAs[Int]("player").get,
      restType(doc.getAs[String]("type").get).get
    )
    def write(rest: Rest) = BSONDocument(
      "player" -> rest.player,
      "type" -> restTypeKey(rest.restType)
    )
  }

  private implicit val roundHandler = new BSONHandler[BSONDocument, Round] {
    def read(doc: BSONDocument) = Round(
      number = doc.getAs[Int]("number").get,
      pairingStartNumber = doc.getAs[Int]("pairingStartNumber").get,
      opening = doc.getAs[String]("openingFen").map { fen =>
        StartingPosition(
          code = doc.getAs[String]("openingCode").getOrElse(""),
          fen = fen,
          moves = doc.getAs[String]("openingMoves").getOrElse(""),
          name = doc.getAs[String]("openingName")
        )
      },
      pairings = doc.getAs[List[Pairing]]("pairings").getOrElse(Nil),
      rests = doc.getAs[List[Rest]]("rests").getOrElse(Nil),
      retired = doc.getAs[List[Int]]("retired").getOrElse(Nil),
      gameIds = doc.getAs[List[String]]("gameIds").getOrElse(Nil),
      complete = doc.getAs[Boolean]("complete").getOrElse(false)
    )
    def write(round: Round) = BSONDocument(
      "number" -> round.number,
      "pairingStartNumber" -> round.pairingStartNumber,
      "openingFen" -> round.opening.map(_.fen),
      "openingCode" -> round.opening.map(_.code),
      "openingMoves" -> round.opening.map(_.moves),
      "openingName" -> round.opening.flatMap(_.name),
      "pairings" -> round.pairings,
      "rests" -> round.rests,
      "retired" -> round.retired,
      "gameIds" -> round.gameIds,
      "complete" -> round.complete
    )
  }

  implicit val tournamentStateHandler = new BSONHandler[BSONDocument, TournamentState] {
    def read(doc: BSONDocument) = TournamentState(
      format = format(doc.getAs[String]("format").get),
      roundCount = doc.getAs[Int]("roundCount").get,
      rounds = doc.getAs[List[Round]]("rounds").getOrElse(Nil),
      usedOpeningCodes = doc.getAs[List[String]]("usedOpeningCodes").getOrElse(Nil).toSet,
      competitionPlayers = doc.getAs[List[CompetitionPlayer]]("competitionPlayers").getOrElse(Nil),
      retiredAt = doc.getAs[BSONDocument]("retiredAt").map(_.elements.flatMap { e =>
        Some(e.name.toInt -> e.value.asInstanceOf[BSONInteger].value)
      }.toMap).getOrElse(Map.empty)
    )
    def write(state: TournamentState) = BSONDocument(
      "format" -> state.format.key,
      "roundCount" -> state.roundCount,
      "rounds" -> state.rounds,
      "usedOpeningCodes" -> state.usedOpeningCodes.toList.sorted,
      "competitionPlayers" -> state.competitionPlayers,
      "retiredAt" -> BSONDocument(state.retiredAt.toList.map {
        case (number, round) => number.toString -> BSONInteger(round)
      })
    )
  }
}
