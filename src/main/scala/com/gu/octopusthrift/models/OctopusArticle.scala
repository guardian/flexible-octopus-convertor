package com.gu.octopusthrift.models

import java.util.concurrent.TimeUnit

import com.gu.flexibleoctopus.model.thrift._
import org.joda.time.format.DateTimeFormat
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json._

import scala.util.{ Success, Try }

/** This model does not represent all fields sent by Octopus,
  * only those that will be relevant in converting to our Thrift model
  */
case class OctopusArticle(
    id: Int,
    filename: String,
    for_publication: String,
    lawyered: String,
    object_type: String,
    object_number: Int,
    last_modified: String,
    in_use_by: Option[String],
    ischeckedout: String,
    status: String,
    attached_to: Option[Int],
    on_pages: Option[String],
    last_user: String
) extends Ordered[OctopusArticle] {

  def lastModifiedEpoch: Long =
    TimeUnit.MILLISECONDS.toSeconds(
      DateTime
        .parse(last_modified, DateTimeFormat.forPattern("yyyyMMddHHmm").withZoneUTC())
        .withZone(DateTimeZone.UTC)
        .getMillis
    )

  def pageNumber: Option[Long] = {
    Try(on_pages.map(_.split(',')(0).split(';')(0).toLong)) match {
      case Success(number) => number
      case _               => None
    }
  }

  def as[T](implicit f: OctopusArticle => T): T = f(this)

  override def compare(that: OctopusArticle): Int = this.object_number compareTo that.object_number
}

object OctopusArticle {

  implicit val formats: Format[OctopusArticle] = Json.format[OctopusArticle]

  implicit def articleMapper: OctopusArticle => Article = (octopusArticle: OctopusArticle) => {

    val forPub = Map("w" -> ForPublication.Web, "b" -> ForPublication.Both, "p" -> ForPublication.Print)

    val lawyered = Map(
      "n" -> Lawyered.Notapplicable,
      "p" -> Lawyered.Pending,
      "c" -> Lawyered.Cleared,
      "r" -> Lawyered.Priority
    )

    val articleStatus = Map(
      "chief sub" -> ArticleStatus.Chiefsub,
      "desk" -> ArticleStatus.Desk,
      "finalled" -> ArticleStatus.Finalled,
      "hold" -> ArticleStatus.Hold,
      "killed" -> ArticleStatus.Killed,
      "revise sub" -> ArticleStatus.Revisesub,
      "subs" -> ArticleStatus.Subs,
      "writers" -> ArticleStatus.Writers
    )

    val isCheckedOut = Map("y" -> true, "n" -> false)

    Article(
      octopusArticle.id,
      forPub(octopusArticle.for_publication.toLowerCase),
      octopusArticle.in_use_by,
      isCheckedOut(octopusArticle.ischeckedout.toLowerCase),
      octopusArticle.lastModifiedEpoch,
      lawyered(octopusArticle.lawyered.toLowerCase),
      articleStatus(octopusArticle.status.toLowerCase),
      octopusArticle.filename,
      octopusArticle.last_user
    )
  }
}
