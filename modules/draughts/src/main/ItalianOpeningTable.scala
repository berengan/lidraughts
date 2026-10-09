package draughts

/** FID Italian draughts three-move opening tables. */
object ItalianOpeningTable {
  case class ItalianOpening(number: Int, moves: String, strength: String, moveTable: String) {
    lazy val fen = fenAfter(moves)
    lazy val position = StartingPosition(number.toString, fen, moves, s"FID opening $number ($strength)".some)
    def inTable(table: Char): Boolean = moveTable.contains(table)
  }

  private val raw = """1|21-17 9-13 25-21|1B|BC
2|21-17 9-13 26-21|1B|BC
3|21-17 9-13 22-18|3N|A
4|21-17 10-13 17-10|PARI|BC
5|21-17 10-14 26-21|3N|A
6|21-17 10-14 17-13|4N|D
7|21-17 10-14 22-18|3N|A
8|21-17 10-14 22-19|2N|B
9|21-17 10-14 23-19|4N|D
10|21-17 10-14 23-20|3N|A
11|21-17 10-14 24-20|2N|B
12|21-17 11-14 25-21|1N|BC
13|21-17 11-14 26-21|2N|B
14|21-17 11-14 22-18|3N|A
15|21-17 11-14 22-19|1N|BC
16|21-17 11-14 23-19|2N|B
17|21-17 11-14 23-20|3N|A
18|21-17 11-14 24-20|1N|BC
19|21-17 11-15 25-21|1N|BC
20|21-17 11-15 26-21|1N|BC
21|21-17 11-15 22-18|1N|BC
22|21-17 11-15 22-19|3N|A
23|21-17 11-15 23-20|1N|BC
24|21-17 12-15 25-21|3N|A
25|21-17 12-15 26-21|1N|BC
26|21-17 12-15 22-18|3N|A
27|21-17 12-15 22-19|3N|A
28|21-17 12-15 23-19|PARI|BC
29|21-17 12-15 23-20|2N|B
30|21-17 12-16 25-21|2N|B
31|21-17 12-16 26-21|2N|B
32|21-17 12-16 22-18|2N|B
33|21-17 12-16 22-19|2N|B
34|21-17 12-16 23-19|PARI|BC
35|21-17 12-16 23-20|3N|A
36|21-17 12-16 24-20|4N|D
37|21-18 10-13 25-21|2N|B
38|21-18 10-13 26-21|2N|B
39|21-18 10-13 22-19|3N|A
40|21-18 10-13 23-19|PARI|BC
41|21-18 10-13 23-20|PARI|BC
42|21-18 10-14 25-21|PARI|BC
43|21-18 10-14 22-19|2N|B
44|21-18 10-14 23-19|2N|B
45|21-18 10-14 23-20|1N|BC
46|21-18 11-14 18-11|3B|A
47|21-18 11-15 25-21|1B|BC
48|21-18 11-15 26-21|2N|B
49|21-18 11-15 23-20|PARI|BC
50|21-18 11-15 18-14|2N|B
51|21-18 11-15 22-19|3N|A
52|21-18 12-15 25-21|PARI|BC
53|21-18 12-15 23-19|1B|BC
54|21-18 12-15 23-20|1N|BC
55|21-18 12-15 22-19|4N|D
56|21-18 12-16 25-21|PARI|BC
57|21-18 12-16 22-19|1N|BC
58|21-18 12-16 23-19|1N|BC
59|21-18 12-16 23-20|1N|BC
60|22-18 10-13 27-22|1N|BC
61|22-18 10-13 18-14|3N|A
62|22-18 10-13 21-17|3N|A
63|22-18 10-13 23-19|3N|A
64|22-18 10-13 23-20|3N|A
65|22-18 10-14 26-22|2N|B
66|22-18 10-14 27-22|3N|A
67|22-18 10-14 23-19|1N|BC
68|22-18 10-14 23-20|2N|B
69|22-18 10-14 24-20|4N|D
70|22-18 11-14 18-11|3B|A
71|22-18 11-15 23-19|3N|A
72|22-18 11-15 23-20|1N|BC
73|22-18 11-15 18-14|1N|BC
74|22-18 11-15 27-22|2N|B
75|22-18 12-15 26-22|2N|B
76|22-18 12-15 27-22|2N|B
77|22-18 12-15 23-19|4N|D
78|22-18 12-15 23-20|2N|B
79|22-18 12-15 18-14|2N|B
80|22-18 12-16 26-22|PARI|BC
81|22-18 12-16 27-22|3N|A
82|22-18 12-16 23-19|PARI|BC
83|22-18 12-16 23-20|1N|BC
84|22-18 12-16 18-14|2N|B
85|22-19 9-13 26-22|2N|B
86|22-19 9-13 27-22|3N|A
87|22-19 9-13 21-17|3N|A
88|22-19 9-13 21-18|3N|A
89|22-19 9-13 23-20|1N|BC
90|22-19 9-13 19-14|2N|B
91|22-19 10-13 26-22|2N|B
92|22-19 10-13 27-22|2N|B
93|22-19 10-13 21-17|2N|B
94|22-19 10-13 23-20|PARI|BC
95|22-19 10-13 19-15|3N|A
96|22-19 10-14 19-10|1N|BC
97|22-19 11-14 26-22|2N|B
98|22-19 11-14 27-22|1N|BC
99|22-19 11-14 21-18|1N|BC
100|22-19 11-14 23-20|2N|B
101|22-19 11-14 24-20|PARI|BC
102|22-19 11-15 26-22|1N|BC
103|22-19 11-15 27-22|PARI|BC
104|22-19 11-15 23-20|3N|A
105|22-19 12-15 19-12|1B|BC
106|22-19 12-16 26-22|PARI|BC
107|22-19 12-16 27-22|PARI|BC
108|22-19 12-16 19-15|1B|BC
109|22-19 12-16 23-20|4N|D
110|22-19 12-16 24-20|3N|A
111|23-19 9-13 28-23|PARI|BC
112|23-19 9-13 21-17|2B|B
113|23-19 9-13 21-18|PARI|BC
114|23-19 9-13 22-18|3N|A
115|23-19 9-13 19-15|2N|B
116|23-19 10-13 28-23|PARI|BC
117|23-19 10-13 21-17|PARI|BC
118|23-19 10-13 19-14|1N|BC
119|23-19 10-13 19-15|1B|BC
120|23-19 10-14 19-10|1B|BC
121|23-19 11-14 28-23|1B|BC
122|23-19 11-14 21-18|PARI|BC
123|23-19 11-14 22-18|1N|BC
124|23-19 11-14 24-20|3N|A
125|23-19 11-14 19-15|1N|BC
126|23-19 11-15 28-23|1B|BC
127|23-19 11-15 21-17|PARI|BC
128|23-19 11-15 21-18|1B|BC
129|23-19 12-15 19-12|2B|B
130|23-19 12-16 28-23|1B|BC
131|23-19 12-16 24-20|3N|A
132|23-19 12-16 19-14|1N|BC
133|23-20 9-13 27-23|3N|A
134|23-20 9-13 28-23|2N|B
135|23-20 9-13 21-17|PARI|BC
136|23-20 9-13 21-18|PARI|BC
137|23-20 9-13 22-18|3N|A
138|23-20 9-13 20-16|1N|BC
139|23-20 10-13 21-17|3N|A
140|23-20 10-13 27-23|2N|B
141|23-20 10-13 28-23|1N|BC
142|23-20 10-13 20-16|2N|B
143|23-20 10-14 27-23|3N|A
144|23-20 10-14 28-23|1N|BC
145|23-20 10-14 20-15|1N|BC
146|23-20 10-14 20-16|2N|B
147|23-20 10-14 22-19|3N|A
148|23-20 11-14 27-23|1N|BC
149|23-20 11-14 28-23|1N|BC
150|23-20 11-14 21-18|1N|BC
151|23-20 11-14 22-18|2N|B
152|23-20 11-14 20-16|2N|B
153|23-20 11-15 20-11|3B|A
154|23-20 12-15 27-23|2N|B
155|23-20 12-15 28-23|1N|BC
156|23-20 12-15 20-16|2N|B
157|23-20 12-15 22-19|4N|D
158|23-20 12-16 20-15|1B|BC
159|23-20 12-16 27-23|2N|B
160|24-20 9-13 21-17|PARI|BC
161|24-20 9-13 21-18|PARI|BC
162|24-20 9-13 20-15|1N|BC
163|24-20 9-13 20-16|1N|BC
164|24-20 10-13 20-15|1N|BC
165|24-20 10-13 20-16|1N|BC
166|24-20 10-14 20-15|1N|BC
167|24-20 10-14 20-16|1N|BC
168|24-20 10-14 21-18|3N|A
169|24-20 11-14 20-15|1N|BC
170|24-20 11-14 20-16|1N|BC
171|24-20 11-14 21-18|3N|A
172|24-20 11-15 20-11|3B|A
173|24-20 12-15 20-16|1N|BC
174|24-20 12-16 28-24|2N|B"""

  val openings: List[ItalianOpening] = raw.lines.toList.filter(_.nonEmpty).map { line =>
    line.split('|').toList match {
      case number :: moves :: strength :: moveTable :: Nil => ItalianOpening(number.toInt, moves, strength, moveTable)
      case _ => sys.error("Invalid Italian opening row: " + line)
    }
  }

  private def positionsFor(table: Option[Char]) = openings.filter(o => table.forall(o.inTable)).map(_.position)
  private def table(key: String, name: String, membership: Option[Char]) = OpeningTable(
    key, name,
    "https://www.federdama.org/cms/index.php/federazione-1/statuti-e-regolamenti/regolamento-tecnico",
    List(StartingPosition.Category("FID", positionsFor(membership)))
  )

  val general = table("italianGeneral", "FID Italian Drawing Table - General", None)
  val tableA = table("italianA", "FID Italian Drawing Table - A", Some('A'))
  val tableB = table("italianB", "FID Italian Drawing Table - B", Some('B'))
  val tableC = table("italianC", "FID Italian Drawing Table - C", Some('C'))
  val tableD = table("italianD", "FID Italian Drawing Table - D", Some('D'))
  val allTables = List(general, tableA, tableB, tableC, tableD)

  def lidraughtsField(fidField: Int): Int =
    fidField

  private def fenAfter(moves: String): String = {
    var white = (21 to 32).toSet
    var black = (1 to 12).toSet
    var whiteToMove = true
    moves.split(' ').foreach { token =>
      val fields = token.split('-').map(_.toInt)
      val from = lidraughtsField(fields(0))
      val to = lidraughtsField(fields(1))
      val own = if (whiteToMove) white else black
      require(own(from), s"Invalid FID opening move $token in $moves")
      if (whiteToMove) white = white - from + to else black = black - from + to
      capturedBetween(from, to).foreach { captured =>
        if (whiteToMove) black = black - captured else white = white - captured
      }
      whiteToMove = !whiteToMove
    }
    val turn = if (whiteToMove) "W" else "B"
    s"$turn:W${white.toList.sorted.mkString(",")}:B${black.toList.sorted.mkString(",")}:H0:F1"
  }

  private def capturedBetween(from: Int, to: Int): Option[Int] =
    for {
      fromPos <- PosItalian.posAt(from)
      toPos <- Pos64.posAt(to)
      middle <- List(
        fromPos.moveUpLeft,
        fromPos.moveUpRight,
        fromPos.moveDownLeft,
        fromPos.moveDownRight
      ).flatten.find { pos =>
          List(pos.moveUpLeft, pos.moveUpRight, pos.moveDownLeft, pos.moveDownRight).flatten.contains(toPos)
        }
    } yield middle.fieldNumber
}
