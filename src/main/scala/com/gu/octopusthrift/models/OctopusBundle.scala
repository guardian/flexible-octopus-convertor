package com.gu.octopusthrift.models

import java.util.concurrent.TimeUnit

import com.gu.flexibleoctopus.model.thrift._
import com.gu.octopusthrift.services.ArticleFinder
import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.{ Try, Success }

/** This model does not represent all fields sent by Octopus,
  * only those that will be relevant in converting to our Thrift model
  */
case class OctopusBundle(
    id: Int,
    info8: String,
    pubcode: String,
    pubdate: Option[String],
    sectioncode: String,
    articles: Array[OctopusArticle]
) {
  private val composerIdLocation = 7

  def composerId: Option[String] = {
    val i8 = info8.split(',')
    val possibleComposerId = Try(i8(composerIdLocation))
    (i8.length > 7, possibleComposerId) match {
      case (true, Success(id)) => if (id.trim.length > 0) Some(id.trim) else None
      case _                   => None
    }
  }

  def pubDateEpochDays: Option[Long] = {
    if (pubdate.exists(_.trim.nonEmpty)) {
      pubdate.map(date =>
        TimeUnit.MILLISECONDS.toDays(
          DateTime
            .parse(date, DateTimeFormat.forPattern("yyyyMMdd").withZoneUTC())
            .withZone(DateTimeZone.UTC)
            .getMillis
        )
      )
    } else { None }
  }

  def as[T](implicit f: OctopusBundle => T): T = f(this)
}

object OctopusBundle {

  implicit val formats: Format[OctopusBundle] = Json.format[OctopusBundle]

  implicit def bundleMapper: OctopusBundle => StoryBundle = (octopusBundle: OctopusBundle) => {

    val bodyText = ArticleFinder.findBodyText(octopusBundle)
    val article = bodyText.get.as[Article]
    val headline = None

    StoryBundle(
      octopusBundle.id,
      // the PayloadValidator has confirmed before this point there there _is_ a Composer ID, so it should not be possible for a None value to be returned here
      octopusBundle.composerId.getOrElse(""),
      octopusBundle.pubcode,
      octopusBundle.sectioncode,
      article,
      headline,
      bodyText.flatMap(_.pageNumber),
      octopusBundle.pubDateEpochDays,
      bodyText.flatMap(_.attached_to.map(_.toString))
    )
  }
}
