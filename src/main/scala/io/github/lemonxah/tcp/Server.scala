package io.github.lemonxah.tcp

import java.io.BufferedOutputStream
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

import io.github.lemonxah.framework.Weakly

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
 * Project: dbabspro
 * Created on 2015/06/29.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */
sealed trait ServerType
case object Response extends ServerType
case object Broadcast extends ServerType
object Server {
  def apply(port: Int, f: (Array[Byte], Int) => Array[Byte], packetLength: Int) = new Server(port, f, packetLength)
}
object BroadcastServer {
  def apply(port: Int, packetLength: Int, f: (Array[Byte], Int) => Array[Byte] = (bytes,pid) => bytes) =
    new Server(port, f, packetLength, Broadcast)
}
class Server(port: Int, f: (Array[Byte], Int) => Array[Byte], receivePacketLength: Int, serverType: ServerType = Response) {
  import Imports._
  var clients = mutable.HashMap[Int,Weakly[BufferedOutputStream]]()
  val actualPacketLength = receivePacketLength + 4 // adding packetId of 4 bytes(Int value)
  var cid: AtomicInteger = new AtomicInteger(0)

  private def bcast(bytes: Array[Byte]): PartialFunction[(Int, Weakly[BufferedOutputStream]), Unit] = {
    case (ncid, Weakly(out)) => out.synchronized { out.write(bytes); out.flush() }
    case (ncid, _) => clients.synchronized(clients = clients - ncid) // remove the weak reference
  }

  def broadcast(bytes: Array[Byte], pid: Array[Byte], id: Int): Unit = {
    clients.filterNot { case (ncid, weak) => ncid != id }.foreach(bcast(bytes ++ pid))
  }

  def broadcast(bytes: Array[Byte]): Unit = {
    clients.foreach(bcast(bytes))
  }

  private def reply(bytes: Array[Byte], pid: Array[Byte], id: Int): Unit = {
    clients(id) match {
      case Weakly(out) => out.synchronized { out.write(bytes ++ pid); out.flush() }
      case _ => clients - id // remove the weak reference
    }
  }

  private def handshake(id: Int) = {
    clients(id) match {
      case Weakly(out) => out.synchronized {
        out.write(ByteBuffer.allocate(4).putInt(id).array)
        out.flush()
      }
      case _ => clients - id // remove the weak reference
    }
  }

  @tailrec private def acceptConnection(s: ServerSocket): Unit = {
    val socket = s.accept()
    Future {
      val id = cid.incrementAndGet()
      clients.synchronized(clients += id -> Weakly(new BufferedOutputStream(socket.getOutputStream)))
      handshake(id)
      socket.toByteArrayStream(actualPacketLength).map(b => (Try(f(b.dropRight(4), id)), b.takeRight(4))).foreach {
        case (Success(b), pid) => serverType match {
          case Response => reply(b, pid, id)
          case Broadcast => broadcast(b, pid, id)
        }
        case (Failure(e), pid) => serverType match {
          case Response => reply(Array(), pid, id)
          case Broadcast => broadcast(Array(), pid, id)
        }
      }
    }
    if (s.isBound) acceptConnection(s)
  }
  acceptConnection(new ServerSocket(port))
}
