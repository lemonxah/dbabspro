package io.github.lemonxah.dao


import io.github.lemonxah.macros.Mappable

import scala.language.higherKinds

/**
 * Project: dbabspro
 * Created on 2015/05/19.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */
trait Model { def id: Option[String] }
trait DAO[A <: Model, B, C, Q] {
  def interpreter(q: Query, empty: Q): Option[Q]
  def insert(a: A)(implicit m: Mappable[A, B, C])
  def list(implicit m: Mappable[A, B, C]): Vector[A]
  def filter(query: Query)(implicit m: Mappable[A, B, C]): Vector[A]
  def headOption(query: Query)(implicit m: Mappable[A, B, C]): Option[A]
  def update(a: A)(implicit m: Mappable[A, B, C])
  def delete(a: A)(implicit m: Mappable[A, B, C])
  def delete(query: Query)(implicit m: Mappable[A, B, C])
  // SHORTHAND NOTATION
  def -=(a: A)(implicit m: Mappable[A, B, C]) = delete(a)(m)
  def +=(a: A)(implicit m: Mappable[A, B, C]) = insert(a)(m)
  def :=(a: A)(implicit m: Mappable[A, B, C]) = update(a)(m)
}

trait Writer[A, B] { def write(a: A): B }
trait Cleaner[A, B] { def clean(a: A): B }

case class Field[A](name: String) {
  def ===(value: A): Query = Equals(this, value)
  def !==(value: A): Query = NotEquals(this, value)
  def <(value: A): Query = LessThan(this, value)
  def >(value: A): Query = GreaterThan(this, value)
  def >=(value: A): Query = GreaterOrEqual(this, value)
  def <=(value: A): Query = LessOrEqual(this, value)
}
sealed trait Query { self =>
  def &&(t: Query): Query = and(t)
  def and(t: Query): Query = And(self, t)
  def ||(t: Query): Query = or(t)
  def or(t: Query): Query = Or(self, t)
}
sealed trait Operator extends Query { def left: Query; def right: Query}
case class Or(left: Query, right: Query) extends Operator
case class And(left: Query, right: Query) extends Operator
sealed trait Operand[+A] extends Query { def field: Field[_]; def value: A }
case class GreaterOrEqual[A](field: Field[A], value: A) extends Operand[A]
case class GreaterThan[A](field: Field[A], value: A) extends Operand[A]
case class LessOrEqual[A](field: Field[A], value: A) extends Operand[A]
case class LessThan[A](field: Field[A], value: A) extends Operand[A]
case class Equals[A](field: Field[A], value: A) extends Operand[A]
case class NotEquals[A](field: Field[A], value: A) extends Operand[A]


