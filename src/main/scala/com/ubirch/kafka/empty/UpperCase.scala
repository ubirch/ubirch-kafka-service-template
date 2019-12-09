package com.ubirch.kafka.empty

import com.ubirch.kafka.express.ExpressKafkaApp
import org.apache.kafka.common.serialization.{ Deserializer, Serializer, StringDeserializer, StringSerializer }

object UpperCase extends ExpressKafkaApp[String, String, Unit] {

  override def keyDeserializer: Deserializer[String] = new StringDeserializer

  override def valueDeserializer: Deserializer[String] = new StringDeserializer

  override def consumerTopics: Set[String] = conf.getString("eventLog.kafkaConsumer.topic").split(",").toSet.filter(_.nonEmpty)

  def producerTopics: Set[String] = conf.getString("eventLog.kafkaProducer.topic").split(",").toSet.filter(_.nonEmpty)

  override def consumerBootstrapServers: String = conf.getString("eventLog.kafkaConsumer.bootstrapServers")

  override def consumerGroupId: String = conf.getString("eventLog.kafkaConsumer.topic")

  override def consumerMaxPollRecords: Int = conf.getInt("eventLog.kafkaConsumer.maxPollRecords")

  override def consumerGracefulTimeout: Int = conf.getInt("eventLog.kafkaConsumer.gracefulTimeout")

  override def producerBootstrapServers: String = conf.getString("eventLog.kafkaProducer.bootstrapServers")

  override def keySerializer: Serializer[String] = new StringSerializer

  override def valueSerializer: Serializer[String] = new StringSerializer

  override def metricsSubNamespace: String = conf.getString("eventLog.kafkaConsumer.metricsSubNamespace")

  override def consumerReconnectBackoffMsConfig: Long = conf.getLong("eventLog.kafkaConsumer.reconnectBackoffMsConfig")

  override def consumerReconnectBackoffMaxMsConfig: Long = conf.getLong("eventLog.kafkaConsumer.reconnectBackoffMaxMsConfig")

  override def lingerMs: Int = conf.getInt("eventLog.kafkaProducer.lingerMS")

  override def process: UpperCase.Process = Process { consumerRecords =>
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
