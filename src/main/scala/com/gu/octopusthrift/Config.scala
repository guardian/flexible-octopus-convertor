package com.gu.octopusthrift

case class Config(thriftStreamName: String, deadLetterQueue: String, stage: String)

object Config {
  def apply: Config = {
    // TODO: add validation and fail fast if we don't have all config props
    Config(
      thriftStreamName = System.getenv("thriftStream"),
      deadLetterQueue = System.getenv("deadLetterQueue"),
      stage = System.getenv("stage")
    )
  }
}
