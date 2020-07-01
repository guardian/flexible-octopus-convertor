package com.gu.octopusthrift

import com.amazonaws.services.kinesis.model.Record
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.gu.flexibleoctopus.model.thrift._
import com.gu.octopusthrift.aws.{ CustomMetrics, Kinesis, Metrics, SQS }
import com.gu.octopusthrift.models._
import com.gu.octopusthrift.services.Logging
import com.gu.octopusthrift.services.PayloadValidator.{ isValidBundle, validatePayload }
import com.gu.octopusthrift.util.ThriftSerializer.serializeToBytes
import play.api.libs.json._

import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

object Lambda extends Logging with CustomMetrics {

  val deadLetterQueue = new SQS(Config.apply)

  def handler(lambdaInput: KinesisEvent, context: Context): Unit = {
    val records: List[Record] = lambdaInput.getRecords.asScala.map(_.getKinesis).toList

    records.foreach(record => {
      val sequenceNumber = record.getSequenceNumber
      logger.info(s"Received payload, sequence number: $sequenceNumber")

      val data = record.getData().array()
      val validatedPayload: Option[OctopusPayload] = validatePayload(data)

      validatedPayload.foreach(payload => {
        logger.info(s"Validated payload, sequence number: $sequenceNumber")
        if (payload.bundles.isDefined) {
          val messageIndex = payload.thismessageindex.getOrElse(0)
          val totalMessages = payload.totalmessages.getOrElse(0)
          logger.info(
            s"Processing daily snapshot, message $messageIndex of $totalMessages, sequence number: $sequenceNumber"
          )
          payload.bundles.get.foreach(bundle => processBundle(bundle, sequenceNumber))
        } else if (payload.data.isDefined) {
          logger.info(s"Processing single story bundle, sequence number: $sequenceNumber")
          processBundle(payload.data.get, sequenceNumber)
        } else {
          logger.info(s"Payload does not contain expected data, sequence number: $sequenceNumber")
          cloudWatch.publishMetricEvent(Metrics.MissingExpectedPayloadData)
          deadLetterQueue.sendMessage(Json.toJson(payload))
        }
        logger.info(s"Completed handling validated payload, sequence number: $sequenceNumber")
      })
    })
  }

  private def processBundle(octopusBundle: OctopusBundle, sequenceNumber: String): Unit = {
    val stream = new Kinesis(Config.apply)

    if (isValidBundle(octopusBundle)) {
      Try(octopusBundle.as[StoryBundle]) match {
        case Success(bundle) =>
          logger.info(s"Bundle passed validation, sequence number: $sequenceNumber")
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
