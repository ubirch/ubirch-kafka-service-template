# ubirch-kafka-service-template
Simple service which could be used as a template for a Kafka based microservice

**Express Kafka Steps**

1. To create an Express Kafka App you have to implement ExpressKafkaApp. This component trait takes 
two type parameters, K and V, K stands for Key and V for Value. You have to pass these parameters in 
depending on the kind of consumer and producer you would like to have.

2. You have to implement a couple of configuration values:
    
    * keyDeserializer
    * valueDeserializer
    * consumerTopics
    * consumerBootstrapServers
    * consumerGroupId
    * consumerMaxPollRecords
    * consumerGracefulTimeout
    * producerBootstrapServers
    * keySerializer
    * valueSerializer

3. You have to implement the business logic in the method called *process*. This method is basically 
called every time the consumer polls for new data. That's to say that the vector of consumer records is 
the data arriving in this configure topic.

4. Optionally, you are able to send -publish- data to different topics if necessary. You can do this 
with the method *send*.

5. You need to have a Kafka Server running. For more information, check https://kafka.apache.org/

**Example**

```scala
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

```