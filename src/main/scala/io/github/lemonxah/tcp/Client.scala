package io.github.lemonxah.tcp

import java.io.BufferedOutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue}

import scala.annotation.tailrec
import scala.concurrent.{Future, Promise}

/**
 * Project: dbabspro
 * Created on 2015/06/29.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */
object Client {
  def apply(host: String, port: Int, packetLength: Int) = new Client(host, port, packetLength)
}

class Client(host: String, port: Int, receivePacketLength: Int) {
  import Imports._
  var packetId = 0
  val queue = new LinkedBlockingQueue[Array[Byte]]()
  lazy val actualPacketLength = receivePacketLength + 4 // adding the packetId
  var promiseCache = new ConcurrentHashMap[Int, Promise[Array[Byte]]]()
  val socket = new Socket(host, port)
  socket.setKeepAlive(true)
  val clientId = {
    val b = Array.ofDim[Byte](4)
    socket.getInputStream.read(b)
    val id = ByteBuffer.wrap(b).getInt
    println(s"handshake complete client id: $id")
    id
  }
  val out = new BufferedOutputStream(socket.getOutputStream)
  @tailrec private def readQueue(m: Array[Byte]): Unit = {
    out.write(m)
    out.flush()
    readQueue(queue.take)
  }
  def write(bytes: Array[Byte]): Future[Array[Byte]] = {
    val p = Promise[Array[Byte]]()
    packetId += 1
    promiseCache.put(packetId, p)
    queue.put(bytes ++ ByteBuffer.allocate(4).putInt(packetId).array)
    p.future
  }
  Future { socket.toByteArrayStream(actualPacketLength).foreach { bytes =>
    val key = ByteBuffer.wrap(bytes.takeRight(4)).getInt
    promiseCache.get(key).success(bytes.dropRight(4))
    promiseCache.remove(key)
  }} onFailure {
    case ex => println(ex.getMessage)
  }
  // single thread writer
  Future { readQueue(queue.take) } onFailure {
    case ex => println(ex.getMessage)
  }
}

object BroadcastClient { def apply (h: String, p: Int, l: Int, f: Array[Byte] => Unit) = new BroadcastClient(h,p,l,f) }

class BroadcastClient(host: String, port: Int, receivePacketLength: Int, f: Array[Byte] => Unit) {
  import Imports._
  val socket = new Socket(host, port)
  socket.setKeepAlive(true)
  val clientId = {
    val b = Array.ofDim[Byte](4)
    socket.getInputStream.read(b)
    val id = ByteBuffer.wrap(b).getInt
    println(s"handshake complete client id: $id")
    id
  }
  Future { socket.toByteArrayStream(receivePacketLength).foreach(f) } onFailure {
    case ex => println(ex.getMessage)
  }
}