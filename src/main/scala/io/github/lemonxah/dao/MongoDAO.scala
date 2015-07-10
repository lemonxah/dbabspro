package io.github.lemonxah.dao

import com.mongodb.DBObject
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import io.github.lemonxah.macros.Mappable
import scala.language.implicitConversions
/**
 * Project: dbabspro
 * Created on 2015/05/11.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */

object MongoInterpreter {
  // Interpreter typeclasses
  def opmap: PartialFunction[Operand[_], String] = {
    case _: Equals[_] => "$eq"
    case _: GreaterThan[_] => "$gt"
    case _: GreaterOrEqual[_] => "$gte"
    case _: LessThan[_] => "$lt"
    case _: LessOrEqual[_] => "$lte"
    case _: NotEquals[_] => "$ne"
  }


  implicit val MongoOperandWriter = new Writer[Operand[_], DBObject] {
    override def write(o: Operand[_]): DBObject = o match {
      case Equals(f, v) => new BasicDBObject(f.name, v)
      case op: Operand[_] => new BasicDBObject(op.field.name, new BasicDBObject(opmap(op), op.value))
    }
  }

  implicit val MongoOperatorWriter = new Writer[Operator, DBObject] {
    override def write(o: Operator): DBObject = (o match
    { case _: Or => $or; case _: And => $and})(SqlQueryWriter.write(o.left),SqlQueryWriter.write(o.right))
  }

  implicit val SqlQueryWriter: Writer[Query, DBObject] = new Writer[Query, DBObject] {
    override def write(a: Query): DBObject = a match {
      case op: Operand[_] => MongoOperandWriter.write(op)
      case op: Operator   => MongoOperatorWriter.write(op)
    }
  }
}

class MongoDAO[A <: Model](coll: MongoCollection) extends DAO[A, DBObject, DBObject, DBObject] {
  import MongoInterpreter._

  def interpreter(q: Query, empty: DBObject = MongoDBObject()): Option[DBObject]  = {
    try { Some(implicitly[Writer[Query, DBObject]].write(q)) } catch { case e: Exception => None }
  }

  override def list(implicit m: Mappable[A, DBObject, DBObject]): Vector[A] = {
    coll.find().toVector.map(m.fromDBType)
  }

  override def filter(query: Query)(implicit m: Mappable[A, DBObject, DBObject]): Vector[A] = {
    interpreter(query) match {
      case Some(q) => coll.find(q).toVector.map(m.fromDBType)
      case None => Vector()
    }
  }

  override def headOption(query: Query)(implicit m: Mappable[A, DBObject, DBObject]): Option[A] = {
    interpreter(query) match {
      case Some(q) => coll.find(q).toVector.map(m.fromDBType).headOption
      case None => None
    }
  }

  override def insert(a: A)(implicit m: Mappable[A, DBObject, DBObject]) {
    coll.insert(m.toDBType(a))
  }

  override def update(a: A)(implicit m: Mappable[A, DBObject, DBObject]) {
    coll.update("id" $eq a.id.get, m.toDBType(a))
  }

  override def delete(a: A)(implicit m: Mappable[A, DBObject, DBObject]) {
    coll.findAndRemove("id" $eq a.id.get)
  }

  override def delete(query: Query)(implicit m: Mappable[A, DBObject, DBObject]): Unit = {
    interpreter(query) match {
      case Some(q) => coll.findAndRemove(q)
      case None =>
    }
  }
}

//object MongoDAO {
//  def apply[A <: Model](coll: MongoCollection): MongoDAO[A] = new MongoDAO[A](coll)
//  implicit val mapTicket: Mappable[ETicket, DBObject, DBObject] = implicitly[MongoMappable[ETicket]]
//  implicit val mapSeat: Mappable[ESeat, DBObject, DBObject] = implicitly[MongoMappable[ESeat]]
//  implicit val mapVenue: Mappable[EVenue, DBObject, DBObject] = implicitly[MongoMappable[EVenue]]
//  implicit val mapEvent: Mappable[EEvent, DBObject, DBObject] = implicitly[MongoMappable[EEvent]]
//}
