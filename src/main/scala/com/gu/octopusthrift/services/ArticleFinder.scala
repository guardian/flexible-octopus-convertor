package com.gu.octopusthrift.services

import com.gu.octopusthrift.models._
import scala.util.{ Failure, Success, Try }

object ArticleFinder extends Logging {

  private val BodyText = "Body Text"
  private val PanelText = "Panel Text"
  private val TabularText = "Tabular Text"

  private val ForBoth = "b"
  private val ForWeb = "w"
  private val ForPrint = "p"

  // There are other possible destinations, 'Y' and 'N', which we don't care about
  private val validPublicationDestinations = Set(ForBoth, ForWeb, ForPrint)

  // Order of precedence for object types: body over panel over tabular
  private val bodyTextObjects = List(BodyText, PanelText, TabularText)

  // Sometimes, object types look like Body Text [Ruled] and we don't care about the part in brackets
  private def cleanObjectType(objectType: String): String = objectType.split("\\[").head.trim

  // Prefer articles that are labelled both, then web, then print
  private def articlesForPreferredDestination(
      articles: Map[String, Array[OctopusArticle]]
  ): Array[OctopusArticle] = {
    val forBoth = Try(articles(ForBoth))
    val forWeb = Try(articles(ForWeb))
    val forPrint = Try(articles(ForPrint))

    (forBoth, forWeb, forPrint) match {
      case (Success(both), _, _)                    => both
      case (Failure(_), Success(web), _)            => web
      case (Failure(_), Failure(_), Success(print)) => print
      case _                                        => Array.empty[OctopusArticle]
    }
  }

  // Prefer Body Text, then Panel Text, then Tabular Text
  private def articlesOfPreferredObjectType(
      articles: Map[String, Array[OctopusArticle]]
  ): Array[OctopusArticle] = {
    val bodyText = Try(articles(BodyText))
    val panelText = Try(articles(PanelText))
    val tabularText = Try(articles(TabularText))

    (bodyText, panelText, tabularText) match {
      case (Success(bodyTexts), _, _)                      => bodyTexts
      case (Failure(_), Success(panelTexts), _)            => panelTexts
      case (Failure(_), Failure(_), Success(tabularTexts)) => tabularTexts
      case _                                               => Array.empty[OctopusArticle]
    }
  }

  /*  Given a bundle with a series of articles, we want to find the 'primary' body text article
   1. If some are for publication in print and some are web/both, discard the print ones
   2. If we still have more than one and the object types are mixed discard all tabular components
   3. If we still have more than one and the object types are mixed discard all panel components
   4. If we still have more than one, pick the one with the lowest object_number.
   */
  def findBodyText(bundle: OctopusBundle): Option[OctopusArticle] = {

    // Given a bundles worth of articles, pick only the ones that are Body Text, Panel Text or Tabular Text
    val onlyBodyTextArticles: Array[OctopusArticle] =
      bundle.articles
        .map(a => a.copy(object_type = cleanObjectType(a.object_type)))
        .filter(a =>
          bodyTextObjects.contains(a.object_type) && validPublicationDestinations
            .contains(a.for_publication.toLowerCase())
        )

    if (onlyBodyTextArticles.isEmpty)
      // There are no suitable articles
      None
    else if (onlyBodyTextArticles.length == 1)
      // There's only one suitable article
      onlyBodyTextArticles.headOption
    else {
      // Given a number of suitable articles, group them by their publication destination
      val groupedByDestination: Map[String, Array[OctopusArticle]] =
        onlyBodyTextArticles.groupBy(_.for_publication.toLowerCase)

      // Pick the available articles for the destination we prefer the most, then group them by object type
      val forPreferredDestinationAndGroupedByType: Map[String, Array[OctopusArticle]] =
        articlesForPreferredDestination(groupedByDestination).groupBy(_.object_type)

      // Pick the article with the most preferred object type and the lowest object number
      articlesOfPreferredObjectType(forPreferredDestinationAndGroupedByType).sorted.headOption
    }
  }

}
