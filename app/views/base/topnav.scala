package views.html.base

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object topnav {

  // Optional Play config: lifidama.menu.<key> = true/false.
  // Missing keys preserve the current menu defaults.
  private object MenuDisplay {
    private def enabled(key: String, default: Boolean): Boolean =
      play.api.Play.current.configuration.getBoolean(s"lifidama.menu.$key").getOrElse(default)

    def showPuzzleTraining = enabled("learn.puzzleTraining", false)
    def showPractice = enabled("learn.practice", false)
    def showCoordinates = enabled("learn.coordinates", false)
    def showStudies = enabled("learn.studies", true)

    def showWatch = enabled("watch.enabled", true)
    def showTv = enabled("watch.tv", true)
    def showCurrentGames = enabled("watch.currentGames", true)
    def showStreamers = enabled("watch.streamers", true)
    def showBroadcasts = enabled("watch.broadcasts", true)

    def showCommunity = enabled("community.enabled", true)
    def showPlayers = enabled("community.players", true)
    def showTeams = enabled("community.teams", true)
    def showForum = enabled("community.forum", true)
    def showFaq = enabled("community.faq", false)
    def showDonation = enabled("community.donation", false)
  }

  private def linkTitle(url: String, name: Frag)(implicit ctx: Context) =
    if (ctx.blind) h3(name) else a(href := url)(name)

  def apply()(implicit ctx: Context) = st.nav(id := "topnav", cls := "hover")(
    st.section(
      linkTitle("/", frag(
        span(cls := "play")(trans.play()),
        span(cls := "home")("LiFiDama")
      )),
      div(role := "group")(
        if (ctx.noBot) a(href := "/?any#hook")(trans.createAGame())
        else a(href := "/?any#friend")(trans.playWithAFriend()),
        ctx.noBot option frag(
          a(href := routes.Tournament.home())(trans.arena.arenaTournaments()),
          a(href := routes.Swiss.home())(trans.swiss.swissTournaments()),
          a(href := routes.Simul.home)(trans.simultaneousExhibitions())
        )
      )
    ),
    st.section(
      linkTitle(routes.Page.variantHome.toString, trans.learnMenu()),
      div(role := "group")(
        ctx.noBot option frag(
          //a(href := routes.Learn.index)(trans.draughtsBasics()),
          MenuDisplay.showPuzzleTraining option a(href := routes.Puzzle.home)(trans.training()),
          MenuDisplay.showPractice option a(href := routes.Practice.index)(trans.practice()),
          MenuDisplay.showCoordinates option a(href := routes.Coordinate.home)(trans.coordinates.coordinates())
        ),
        MenuDisplay.showStudies option a(href := routes.Study.allDefault(1))(trans.studyMenu()),
        a(href := routes.Page.variantHome)(trans.rulesAndVariants())
      //a(href := routes.Coach.allDefault(1))(trans.coaches())
      )
    ),
    MenuDisplay.showWatch option st.section(
      linkTitle(routes.Tv.index.toString, trans.watch()),
      div(role := "group")(
        MenuDisplay.showTv option a(href := routes.Tv.index)("LiFiDama TV"),
        MenuDisplay.showCurrentGames option a(href := routes.Tv.games)(trans.currentGames()),
        MenuDisplay.showStreamers option a(href := routes.Streamer.index())(trans.streamersMenu()),
        MenuDisplay.showBroadcasts option a(href := routes.Relay.index())(trans.broadcast.broadcasts())
      )
    ),
    MenuDisplay.showCommunity option st.section(
      linkTitle(routes.User.list.toString, trans.community()),
      div(role := "group")(
        MenuDisplay.showPlayers option a(href := routes.User.list)(trans.players()),
        MenuDisplay.showTeams option a(href := routes.Team.home())(trans.team.teams()),
        MenuDisplay.showForum option NotForKids(a(href := routes.ForumCateg.index)(trans.forum())),
        MenuDisplay.showFaq option a(href := routes.Main.faq)(trans.faqMenu()),
        (MenuDisplay.showDonation && ctx.me.exists(!_.kid)) option
          a(cls := "community-patron", href := routes.Plan.index)(trans.patron.donate())
      )
    ),
    st.section(
      linkTitle(routes.UserAnalysis.index.toString, trans.tools()),
      div(role := "group")(
        a(href := routes.UserAnalysis.index)(trans.analysis()),
        isGranted(_.CreatePuzzles) option a(href := routes.UserAnalysis.puzzleEditor)("Puzzle editor"),
        //a(href := s"${routes.UserAnalysis.index}#explorer")(trans.openingExplorer()),
        a(href := routes.Editor.parse("italian"))("Editor damiera"),
        a(href := routes.Importer.importGame)(trans.importGame()),
        a(href := routes.Search.index())(trans.search.advancedSearch())
      )
    )
  )
}
