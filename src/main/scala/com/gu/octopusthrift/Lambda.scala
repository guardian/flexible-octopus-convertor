package com.gu.octopusthrift

import com.amazonaws.services.lambda.runtime.events.KinesisEvent.Record
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.gu.flexibleoctopus.model.thrift._
import com.gu.octopusthrift.aws.{ CustomMetrics, DeadLetterQueue, Kinesis, Metrics }
import com.gu.octopusthrift.models._
import com.gu.octopusthrift.services.Logging
import com.gu.octopusthrift.services.PayloadValidator.{
  isValidBundle,
  validateCachePayload,
  validateSinglePayload
}
import com.gu.octopusthrift.util.ThriftSerializer.serializeToBytes
import play.api.libs.json._

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

object Lambda extends Logging with CustomMetrics with DeadLetterQueue {

  def handler(lambdaInput: KinesisEvent, context: Context): Unit = {
    val records: List[Record] = lambdaInput.getRecords.asScala.map(_.getKinesis).toList

    records.foreach(record => {
      val sequenceNumber = record.getSequenceNumber
      logger.info(s"Received payload, sequence number: $sequenceNumber")
      val data = record.getData().array()

      (validateSinglePayload(data), validateCachePayload(data)) match {
        case (Some(singleBundle), _) =>
          logger.info(s"Processing single story bundle, sequence number: $sequenceNumber")
          processBundle(singleBundle.data.get, sequenceNumber)
        case (None, Some(cache)) => {
          val cacheData = cache.data.get
          val messageIndex = cacheData.thismessageindex.getOrElse(0)
          val totalMessages = cacheData.totalmessages.getOrElse(0)
          logger.info(
            s"Processing daily snapshot, message $messageIndex of $totalMessages, sequence number: $sequenceNumber"
          )
          cacheData.bundles.get.foreach(bundle => processBundle(bundle, sequenceNumber))
        }
        case _ => {
          logger.info(s"Payload does not match OctopusPayload model, sequence number: $sequenceNumber")
          deadLetterQueue.sendMessage(Json.toJson(data))
          cloudWatch.publishMetricEvent(Metrics.InvalidOctopusPayload)
        }
      }
    })
  }

  private def processBundle(octopusBundle: OctopusBundle, sequenceNumber: String): Unit = {
    val stream = new Kinesis(Config.apply)

    if (isValidBundle(octopusBundle)) {
      Try(octopusBundle.as[StoryBundle]) match {
        case Success(bundle) =>
          logger.info(
            s"Bundle passed validation, sequence number: $sequenceNumber, composer ID: ${bundle.composerId}"
          )
          val serializedThriftBundle = serializeToBytes(bundle)
          stream.publish(serializedThriftBundle)
        case Failure(e) =>
          logger.info(
            s"Bundle failed validation as StoryBundle, sequence number: $sequenceNumber, with error: $e"
          )
          cloudWatch.publishMetricEvent(Metrics.FailedThriftConversion)
          deadLetterQueue.sendMessage(Json.toJson(octopusBundle))
      }
    } else {
      logger.info(s"Bundle failed validation, sequence number: $sequenceNumber")
      cloudWatch.publishMetricEvent(Metrics.MissingMandatoryBundleData)
    }
  }

}
