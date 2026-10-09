package views
package html.site

import controllers.routes
import lidraughts.api.Context
import lidraughts.common.FederationConfig
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

object variant {

  def show(
    doc: io.prismic.Document,
    resolver: io.prismic.DocumentLinkResolver,
    variant: draughts.variant.Variant,
    perfType: lidraughts.rating.PerfType
  )(implicit ctx: Context) = layout(
    active = perfType.some,
    title = s"${variant.name} • ${variantTitle(variant)}",
    klass = "box-pad page variant"
  )(
    h1(cls := "text", dataIcon := perfType.iconChar)(variant.name),
    h2(cls := "headline")(variantTitle(variant)),
    div(cls := "body")(raw(~doc.getHtml("variant.content", resolver)))
  )

  // Dedicated FID rules page. Keep this separate from the generic variant page.
  def italian()(implicit ctx: Context) = layout(
    title = "Dama Italiana",
    klass = "box-pad page variant",
    italianActive = true
  )(
    h1("Dama Italiana"),
    h2(cls := "headline")("La variante italiana"),
    div(cls := "body")(
      p("La Dama Italiana si gioca su una damiera 8×8 con dodici pedine per giocatore."),
      p("Le catture sono obbligatorie e seguono specifiche regole di precedenza. " +
        "Per la normativa completa si fa riferimento al Regolamento Tecnico della Federazione Italiana Dama."),
      p(a(href := "https://www.federdama.org")("Federazione Italiana Dama - regolamenti ufficiali")),
      p(a(cls := "button button-green", href := "/?any#hook")("Gioca a Dama Italiana"))
    )
  )

  def home()(implicit ctx: Context) = layout(
    title = trans.rulesAndVariants.txt(),
    klass = "variants"
  )(
      h1(trans.rulesAndVariants()),
      div(cls := "body box__pad")(
        if (FederationConfig.current.enabled && FederationConfig.current.defaultVariant == draughts.variant.Italian)
          p("Regole e varianti abilitate per questa federazione.")
        else p(trans.standardFmjdRegulationsWithDrawingRules())
      ),
      div(cls := "variants")(
        FederationConfig.current.allowsVariant(draughts.variant.Italian) option
          a(cls := "variant text box__pad", href := routes.Page.variant("italian"), dataIcon := "g")(
            span(h2("Dama Italiana"), h3(cls := "headline")("Regole italiane 8×8"))
          ),
        lidraughts.rating.PerfType.variantsPlus.filter(pt =>
          pt != lidraughts.rating.PerfType.Italian &&
          FederationConfig.current.allowsVariant(lidraughts.rating.PerfType.variantOf(pt))
        ) map { pt =>
          val variant = lidraughts.rating.PerfType variantOf pt
          a(cls := "variant text box__pad", href := routes.Page.variant(pt.key), dataIcon := pt.iconChar)(
            span(
              h2(variant.name),
              h3(cls := "headline")(variantTitle(variant))
            )
          )
        }
      )
    )

  private def layout(
    title: String,
    klass: String,
    active: Option[lidraughts.rating.PerfType] = None,
    openGraph: Option[lidraughts.app.ui.OpenGraph] = None,
    italianActive: Boolean = false
  )(body: Modifier*)(implicit ctx: Context) = views.html.base.layout(
    title = title,
    moreCss = cssTag("variant"),
    openGraph = openGraph
  )(
      main(cls := "page-menu")(
        st.aside(cls := "page-menu__menu subnav")(
          FederationConfig.current.allowsVariant(draughts.variant.Italian) option a(
            cls := List("text" -> true, "active" -> italianActive),
            href := routes.Page.variant("italian"),
            dataIcon := "g"
          )("Dama Italiana"),
          lidraughts.rating.PerfType.variantsPlus.filter(pt =>
            pt != lidraughts.rating.PerfType.Italian &&
            FederationConfig.current.allowsVariant(lidraughts.rating.PerfType.variantOf(pt))
          ) map { pt =>
            a(
              cls := List("text" -> true, "active" -> active.has(pt)),
              href := routes.Page.variant(pt.key),
              dataIcon := pt.iconChar
            )(pt.name)
          }
        ),
        div(cls := s"page-menu__content box $klass")(body)
      )
    )
}
