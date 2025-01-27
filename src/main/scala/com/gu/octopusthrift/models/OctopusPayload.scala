package com.gu.octopusthrift.models

import play.api.libs.functional.syntax._
import play.api.libs.json._
case class OctopusBundleCacheData(
    bundles: Option[Array[OctopusBundle]],
    thismessageindex: Option[Int],
    totalmessages: Option[Int]
)
object OctopusBundleCacheData {
  implicit val bundleCacheDataFormat = Json.reads[OctopusBundleCacheData]
}

/** The following models represent the possible payload shapes we can receive from Octopus
  *  - the OctopusBundleCachePayload represents a daily snapshot of Octopus data, containing multiple story bundles
  *  - the OctopusSingleBundlePayload represents an update to a single story bundle
  */
case class OctopusBundleCachePayload(`type`: String, data: Option[OctopusBundleCacheData])
object OctopusBundleCachePayload {

  val typeReads = Reads.StringReads.filter(_.matches("bundlecache"))

  implicit val reads: Reads[OctopusBundleCachePayload] =
    ((__ \ "type").read[String](typeReads) and (__ \ "data")
      .readNullable[OctopusBundleCacheData])(OctopusBundleCachePayload.apply _)

}

case class OctopusSingleBundlePayload(`type`: String, data: Option[OctopusBundle])
object OctopusSingleBundlePayload {

  val typeReads = Reads.StringReads.filter(_.matches("bundle"))

  implicit val reads: Reads[OctopusSingleBundlePayload] =
    ((__ \ "type").read[String](typeReads) and (__ \ "data")
      .readNullable[OctopusBundle])(OctopusSingleBundlePayload.apply _)
}
