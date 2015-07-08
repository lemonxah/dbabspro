package io.github.lemonxah.queue

import java.io._
import java.util.Properties
import java.util.zip.{GZIPOutputStream, GZIPInputStream}
import javax.naming.ConfigurationException

import io.github.lemonxah.framework.Readers.ReaderMonad
import kafka.consumer.{ConsumerConfig, Consumer}
import kafka.producer.{ProducerConfig, KeyedMessage, Producer}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * Project: dbabspro
 * Created on 2015/06/09.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */

object KafkaImplicits {
  var DEBUG = false
  type KafkaConfigReader = ReaderMonad[Properties, Unit]
  type ProducerReader = ReaderMonad[Producer[String,Array[Byte]], Unit]

  def inflate(bytes: Array[Byte]): Array[Byte] = {
    val in = new GZIPInputStream(new ByteArrayInputStream(bytes))
    val out = new ByteArrayOutputStream()
    val buf = new Array[Byte](1024)
    def loop(count: Int): Array[Byte] = count match {
      case n if n >= 0 => out.write(buf, 0, n); loop(in.read(buf))
      case _ => in.close(); out.close(); out.toByteArray
    }
    loop(in.read(buf))
  }

  def deflate(message: Array[Byte]): Array[Byte] = {
    val outStream = new ByteArrayOutputStream(message.length)
    val gzip = new GZIPOutputStream(outStream)
    gzip.write(message)
    gzip.close()
    outStream.close()
    outStream.toByteArray
  }

  implicit val kafkaSubscribe = new Subscriber[ActorSubscribe[Array[Byte]], Properties] {
    override def subscribe(a: ActorSubscribe[Array[Byte]]): KafkaConfigReader = new KafkaConfigReader({c =>
      val consumer = Consumer.create(new ConsumerConfig(c))
      val consumerMap = consumer.createMessageStreams(Map(a.topic -> 1))
      val stream = consumerMap.getOrElse(a.topic, List()).head
      def read(): Stream[Array[Byte]] = stream.head.message() #:: read()
      Future { read().foreach(bytes => try {
        if (DEBUG) {
          println(s"bytes received on topic ${a.topic}, byte length: ${bytes.length}")
          println(s"infalted byte length: ${inflate(bytes).length}")
        }
        a.handle(inflate(bytes))
      } catch { case e: Exception => if (DEBUG) println(s"Subscribe Future Failed! message:${e.getMessage}")})  }
    })
  }

  implicit val kafkaPublish = new Publisher[Publish[Array[Byte]], Producer[String,Array[Byte]]] {
    override def publish(a: Publish[Array[Byte]]): ProducerReader = new ProducerReader( { producer =>
      producer.send(new KeyedMessage(a.topic, deflate(a.message)))
    })
  }
}

class KafkaQueue extends MQueue[Publish[Array[Byte]], ActorSubscribe[Array[Byte]]] {
  import KafkaImplicits._
  val conf = new Properties()
  try { conf.load(new FileInputStream("conf/kafka.properties"))}
  catch { case e: IOException => throw new ConfigurationException("conf/kafka.properties file does not exist or is stupid") }
  lazy val producer: Producer[String, Array[Byte]] = new Producer[String,Array[Byte]](new ProducerConfig(conf))
  override def publish(a: Publish[Array[Byte]]): Unit =
    implicitly[Publisher[Publish[Array[Byte]],Producer[String,Array[Byte]]]].publish(a).run(producer)
  override def subscribe(b: ActorSubscribe[Array[Byte]]): Unit =
    implicitly[Subscriber[ActorSubscribe[Array[Byte]], Properties]].subscribe(b).run(conf)
}
