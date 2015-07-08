package io.github.lemonxah.dao

import com.fullfacing.ticketing.macros.{H2Mappable, Mappable}
import scalikejdbc._

/**
 * Project: dbabspro
 * Created on 2015/06/05.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */

object H2Interpreter {
  val opmap: PartialFunction[Operand[_], String] = {
    case _: Equals[_]         => "="
    case _: GreaterThan[_]    => ">"
    case _: GreaterOrEqual[_] => ">="
    case _: LessThan[_]       => "<"
    case _: LessOrEqual[_]    => "<"
    case _: NotEquals[_]      => "<"
  }

  val SqlOperandWriter = new Writer[Operand[_], String] {
    override def write(a: Operand[_]): String = a match {
      case op: Operand[_] => s"${op.field.name} ${opmap(op)} " + (op.value match {
        case v: String => s"'$v'"
        case v         => s"$v"
      })
    }
  }

  val SqlOperatorWriter = new Writer[Operator, String] {
    override def write(o: Operator): String = {
      s"(${SqlQueryWriter.write(o.left)} ${o match { case _: Or => "OR" case _: And => "AND" }} ${SqlQueryWriter.write(o.right)})"
    }
  }

  implicit val SqlQueryWriter: Writer[Query, String] = new Writer[Query, String] {
    override def write(a: Query): String = a match {
      case op: Operand[_] => SqlOperandWriter.write(op)
      case op: Operator   => SqlOperatorWriter.write(op)
    }
  }

  implicit val SqlCleaner = new Cleaner[String, String] {
    override def clean(a: String): String = if (a.head == '(' && a.last == ')') a.drop(1).dropRight(1) else a
  }
}

class H2DAO[A <: Model](session: DBSession, table: String) extends DAO[A, String, WrappedResultSet, String] {
  implicit val s = session
  import H2Interpreter._

  override def interpreter(q: Query, empty: String = ""): Option[String] = {
    try { Some(implicitly[Writer[Query,String]].write(q)) } catch { case e: Exception => None }
  }

  override def list(implicit m: Mappable[A, String, WrappedResultSet]): Vector[A] = {
    SQL(s"select * from $table;").map(m.fromDBType).list().apply().toVector
  }

  override def headOption(query: Query)(implicit m: Mappable[A, String, WrappedResultSet]): Option[A] = {
    SQL(s"select top 1 * from $table where ${interpreter(query)};").map(m.fromDBType).list().apply().headOption
  }

  override def filter(query: Query)(implicit m: Mappable[A, String, WrappedResultSet]): Vector[A] = {
    SQL(s"select * from $table where ${interpreter(query)};").map(m.fromDBType).list().apply().toVector
  }

  override def insert(a: A)(implicit m: Mappable[A, String, WrappedResultSet]): Unit = {
    SQL(m.toDBType(a)).update().apply()
  }

  override def update(a: A)(implicit m: Mappable[A, String, WrappedResultSet]): Unit = ???

  override def delete(a: A)(implicit m: Mappable[A, String, WrappedResultSet]): Unit = ???

  override def delete(query: Query)(implicit m: Mappable[A, String, WrappedResultSet]): Unit = {
    SQL(s"delete from $table where ${interpreter(query)};").update().apply()
  }
}
//
//object H2DAO {
//  def apply[A <: Model](session: DBSession, table: String): H2DAO[A] = new H2DAO[A](session, table)
//  implicit val mapTicket: Mappable[ETicket, String, WrappedResultSet] = implicitly[H2Mappable[ETicket]]
//  implicit val mapSeat: Mappable[ESeat, String, WrappedResultSet] = implicitly[H2Mappable[ESeat]]
//  implicit val mapVenue: Mappable[EVenue, String, WrappedResultSet] = implicitly[H2Mappable[EVenue]]
//  implicit val mapEvent: Mappable[EEvent, String, WrappedResultSet] = implicitly[H2Mappable[EEvent]]
//}
