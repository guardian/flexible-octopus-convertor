package com.gu.octopusthrift.services

import com.gu.octopusthrift.models._
import play.api.libs.json._

import scala.util.{ Success, Try }

object PayloadValidator extends Logging {

  def validateSinglePayload(data: Array[Byte]): Option[OctopusSingleBundlePayload] = {
    Try(Json.parse(data)) match {
      case Success(json) =>
        json.validate[OctopusSingleBundlePayload] match {
          case JsSuccess(payload, _) => Some(payload)
          case _: JsError            => None
        }
      case _ => None
    }
  }

  def validateCachePayload(data: Array[Byte]): Option[OctopusBundleCachePayload] = {
    Try(Json.parse(data)) match {
      case Success(json) =>
        json.validate[OctopusBundleCachePayload] match {
          case JsSuccess(payload, _) => Some(payload)
          case _: JsError            => None
        }
      case _ => None
    }
  }

  def isValidBundle(bundle: OctopusBundle): Boolean = {
    logger.info(s"Validating bundle for ${bundle.composerId}: $bundle")
    logger.info(s"Body text for ${bundle.composerId}: ${ArticleFinder.findBodyText(bundle)}")
    val hasComposerId = bundle.composerId.isDefined
    val hasBodyText = ArticleFinder.findBodyText(bundle).isDefined
    hasComposerId && hasBodyText
  }
}
