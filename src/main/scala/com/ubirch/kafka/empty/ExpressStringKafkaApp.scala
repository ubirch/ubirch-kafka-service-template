package com.ubirch.kafka.empty

import java.util.UUID
import java.util.concurrent.CountDownLatch

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import com.ubirch.kafka.consumer.{ ConsumerRecordsController, ConsumerRunner, ProcessResult, Configs => ConsumerConfigs }
import com.ubirch.kafka.producer.{ StringProducer, Configs => ProducerrConfigs }
import org.apache.kafka.clients.consumer.{ ConsumerRecord, OffsetResetStrategy }
import org.apache.kafka.common.serialization.Deserializer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ConsumerBasicConfigs[K, V] {

  def consumerTopics: Set[String]

  def consumerBootstrapServers: String

  def consumerGroupId: String

  def consumerMaxPollRecords: Int

  def consumerGracefulTimeout: Int

  def keyDeserializer: Deserializer[K]

  def valueDeserializer: Deserializer[V]

  def consumerConfigs = ConsumerConfigs(
    bootstrapServers = consumerBootstrapServers,
    groupId = consumerGroupId,
    enableAutoCommit = false,
    autoOffsetReset = OffsetResetStrategy.EARLIEST,
    maxPollRecords = consumerMaxPollRecords
  )

}

trait ProducerBasicConfigs {

  def producerBootstrapServers: String

  def producerConfigs = ProducerrConfigs(producerBootstrapServers)

}

trait Controller[K, V] {

  thizz =>

  val controller = new ConsumerRecordsController[K, V] {

    def simpleProcessResult(consumerRecord: Vector[ConsumerRecord[K, V]]) = new ProcessResult[K, V] {
      override val id: UUID = UUID.randomUUID()
      override val consumerRecords: Vector[ConsumerRecord[K, V]] = consumerRecord
    }

    override type A = ProcessResult[K, V]

    override def process(consumerRecord: Vector[ConsumerRecord[K, V]]): Future[ProcessResult[K, V]] = {
      thizz.process(consumerRecord)
      Future.successful(simpleProcessResult(consumerRecord))
    }
  }

  def process(consumerRecords: Vector[ConsumerRecord[K, V]]): Unit

}

trait Config {
  val config = ConfigFactory.load
}

trait ExpressConsumer[K, V] extends ConsumerBasicConfigs[K, V] with Controller[K, V] {
  lazy val consumption = {
    val consumerImp = ConsumerRunner.empty[K, V]
    consumerImp.setUseAutoCommit(false)
    consumerImp.setTopics(consumerTopics)
    consumerImp.setProps(consumerConfigs)
    consumerImp.setKeyDeserializer(Some(keyDeserializer))
    consumerImp.setValueDeserializer(Some(valueDeserializer))
    consumerImp.setConsumerRecordsController(Some(controller))
    consumerImp
  }
}

trait ExpressProducer[K, V] extends ProducerBasicConfigs {
  lazy val production = StringProducer(producerConfigs)
}

trait ExpressKafkaApp[K, V] extends ExpressConsumer[K, V] with ExpressProducer[K, V] with Config with LazyLogging {

  def main(args: Array[String]): Unit = {
    consumption.startPolling()
    val cd = new CountDownLatch(1)
    cd.await()
  }

  Runtime.getRuntime.addShutdownHook(new Thread() {
    override def run(): Unit = {

      logger.info("Shutting down Consumer: " + consumption.getName)
      consumption.shutdown(consumerGracefulTimeout, java.util.concurrent.TimeUnit.SECONDS)

      logger.info("Shutting down Producer")
      production.getProducerAsOpt.foreach(_.close())

      Thread.sleep(5000) //Waiting 5 secs
      logger.info("Bye bye, see you later...")
    }
  })

}

