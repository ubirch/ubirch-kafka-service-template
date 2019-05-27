package com.ubirch.kafka.empty

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.{ Deserializer, Serializer, StringDeserializer, StringSerializer }

object UpperCase extends ExpressKafkaApp[String, String] {

  override def keyDeserializer: Deserializer[String] = new StringDeserializer

  override def valueDeserializer: Deserializer[String] = new StringDeserializer

  override def consumerTopics: Set[String] = config.getString("eventLog.kafkaConsumer.topic").split(",").toSet.filter(_.nonEmpty)

  def producerTopics: Set[String] = config.getString("eventLog.kafkaProducer.topic").split(",").toSet.filter(_.nonEmpty)

  override def consumerBootstrapServers: String = config.getString("eventLog.kafkaConsumer.bootstrapServers")

  override def consumerGroupId: String = config.getString("eventLog.kafkaConsumer.topic")

  override def consumerMaxPollRecords: Int = config.getInt("eventLog.kafkaConsumer.maxPollRecords")

  override def consumerGracefulTimeout: Int = config.getInt("eventLog.kafkaConsumer.gracefulTimeout")

  override def producerBootstrapServers: String = config.getString("eventLog.kafkaProducer.bootstrapServers")

  override def keySerializer: Serializer[String] = new StringSerializer

  override def valueSerializer: Serializer[String] = new StringSerializer

  override def process(consumerRecords: Vector[ConsumerRecord[String, String]]): Unit = {
    consumerRecords.foreach { x =>

      val upperCased = x.value().toUpperCase()
      println("I got this: " + x.value())
      println("I turned it into this: " + upperCased)

      producerTopics.map { topic =>
        send(topic, upperCased)
      }

    }
  }

}
