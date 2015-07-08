package io.github.lemonxah.macros

/**
 * Project: dbabspro
 * Created on 2015/06/08.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */
trait Mappable[A,B,C] {
  def toDBType(a: A): B
  def fromDBType(c: C): A
}
