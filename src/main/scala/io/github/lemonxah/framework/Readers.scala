package io.github.lemonxah.framework

import scala.language.higherKinds

/**
 * Project: dbabspro
 * Created on 2015/06/09.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */

object Readers {
  case class ReaderMonad[A, B](f: A => B) {
    def map[C](g: B => C): ReaderMonad[A, C] = ReaderMonad(a => g(f(a)))
    def flatMap[C](g: B => ReaderMonad[A, C]): ReaderMonad[A, C] = ReaderMonad(a => g(f(a)).f(a))
    def run(a: A) = f(a)
    def apply[D, E](e: E): ReaderMonad[D, E] = ReaderMonad(d => e)
    def ask[D]: ReaderMonad[D, D] = ReaderMonad(identity)
  }
}