package io.github.lemonxah.dao

/**
 * Project: io.github.lemonxah.dao
 * Created on 2015/07/08.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */

object SQLInterpreter {
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