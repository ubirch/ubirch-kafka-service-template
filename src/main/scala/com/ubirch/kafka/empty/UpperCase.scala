package com.ubirch.kafka.empty

import java.util.concurrent.atomic.AtomicInteger

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ Deserializer, StringDeserializer }

object UpperCase extends ExpressKafkaApp[String, String] {

  override def keyDeserializer: Deserializer[String] = new StringDeserializer
  override def valueDeserializer: Deserializer[String] = new StringDeserializer
  override def consumerTopics: Set[String] = config.getString("eventLog.kafkaConsumer.topic").split(",").toSet.filter(_.nonEmpty)
  override def consumerBootstrapServers: String = config.getString("eventLog.kafkaConsumer.bootstrapServers")
  override def consumerGroupId: String = config.getString("eventLog.kafkaConsumer.topic")
  override def consumerMaxPollRecords: Int = config.getInt("eventLog.kafkaConsumer.maxPollRecords")
  override def consumerGracefulTimeout: Int = config.getInt("eventLog.kafkaConsumer.gracefulTimeout")

  override def producerBootstrapServers: String = config.getString("eventLog.kafkaProducer.bootstrapServers")

  val counter = new AtomicInteger(0)
  val max = 100

  override def process(consumerRecords: Vector[ConsumerRecord[String, String]]): Unit = {
    consumerRecords.foreach { x =>

      if (counter.incrementAndGet() < max) {
        val upperCased = x.value().toUpperCase()
        println("I got this: " + x.value())
        println("I turned it into this: " + upperCased)
        consumerTopics.map { topic =>
          production.getProducerOrCreate.send(new ProducerRecord[String, String](topic, x.value()))
        }
      } else {
        counter.set(0)
      }

    }
  }

}
