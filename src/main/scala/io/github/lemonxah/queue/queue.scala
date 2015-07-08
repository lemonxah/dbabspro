package io.github.lemonxah.queue

import akka.actor.ActorRef
import io.github.lemonxah.framework.Readers.ReaderMonad

/**
 * Project: dbabspro
 * Created on 2015/06/09.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */
case class Publish[A](topic: String, message: A)
trait Subscribe[A, B] { def topic: String; def handler: A; def handle(m: B) }
case class ActorSubscribe[A](topic: String, handler: ActorRef) extends Subscribe[ActorRef, A] {
  def handle(m: A) = handler ! m
}

trait Publisher[A,B] { def publish(a: A): ReaderMonad[B,Unit] }
trait Subscriber[A,C] { def subscribe(a: A): ReaderMonad[C,Unit] }
trait MQueue[A <: Publish[_], B <: Subscribe[_,_]] {
  def publish(a: A)
  def subscribe(b: B)
}