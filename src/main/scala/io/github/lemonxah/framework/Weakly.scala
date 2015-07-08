package io.github.lemonxah.framework

/**
 * Project: dbabspro
 * Created on 2015/07/02.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */
class Weakly[A](wr: java.lang.ref.WeakReference[A]) {
  def apply() = wr.get
  def get = Option(wr.get)
  def weak = wr
  override def hashCode = { val a = wr.get; if (a==null) wr.hashCode else a.hashCode }
  override def equals(a: Any) = a==wr.get
  override def toString = "~"+wr.get
}

object Weakly {
  def apply[A](a: A) = new Weakly(new java.lang.ref.WeakReference(a))
  def unapply[A](wa: Weakly[A]) = Option(wa())
}
