package io.github.lemonxah.macros

import com.mongodb.DBObject

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
 * Project: dbabspro
 * Created on 2015/05/26.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */

trait MongoMappable[A] extends Mappable[A, DBObject, DBObject]
object MongoMappable {
  implicit def materializeMappable[A]: MongoMappable[A] = macro materializeMappableImpl[A]

  def materializeMappableImpl[A: c.WeakTypeTag](c: whitebox.Context): c.Expr[MongoMappable[A]] = {
    import c.universe._
    val tpe = weakTypeOf[A]
    val companion = tpe.typeSymbol.companion

    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.get.paramLists.head

    c.Expr[MongoMappable[A]] { q"""
      new MongoMappable[$tpe] {
        def toDBType(t: $tpe): DBObject = grater[$tpe].asDBObject(t)
        def fromDBType(dbo: DBObject): $tpe = grater[$tpe].asObject(dbo)
      }
    """ }
  }
}

