package io.github.lemonxah.tcp

import java.io.BufferedInputStream
import java.net.Socket
import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

/**
 * Project: dbabspro
 * Created on 2015/06/29.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */
object Imports {
  implicit val ec = new ExecutionContext {
    val threadPool = Executors.newFixedThreadPool(1000)
    def execute(runnable: Runnable) {
      threadPool.submit(runnable)
    }
    def reportFailure(t: Throwable) {}
  }

  implicit class socketToByteArrayStream(s: Socket) {
    def toByteArrayStream(packetLength: Int): Stream[Array[Byte]] = {
      s.setKeepAlive(true)
      val in = new BufferedInputStream(s.getInputStream)
      val buffer = Array.ofDim[Byte](packetLength * 1000)
      def loop(c: Int, over: Array[Byte]): Stream[Array[Byte]] = {
        if (c > 0) {
          val c1 = if (over.length > 0) {
            val b = Array.concat(over, buffer.take(c))
            Array.copy(b, 0, buffer, 0, b.length)
            c + over.length
          } else c
          val odd = c1 % packetLength
          val (c2: Int, over1: Array[Byte]) = if (odd != 0) {
            (c1 - odd, buffer.slice(c1 - odd, c1))
          } else (c1, Array[Byte]())
          if (c2 % packetLength == 0) {
            (0 to (c2 / packetLength) - 1).map(i =>
              buffer.slice(0 + (i * packetLength), packetLength + (i * packetLength))).toStream #::: loop(in.read(buffer), over1)
          } else loop(in.read(buffer), over1)
        } else Stream.empty[Array[Byte]]
      }
      loop(in.read(buffer), Array[Byte]())
    }
  }
}
