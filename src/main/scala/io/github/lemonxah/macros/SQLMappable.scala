package io.github.lemonxah.macros

import scalikejdbc.WrappedResultSet

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
 * Project: com.fullfacing.ticketing.macros
 * Created on 2015/06/08.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */

trait SQLMappable[A] extends Mappable[A, String, WrappedResultSet]
object H2Mappable {
  implicit def materializeMappable[T]: SQLMappable[T] = macro materializeMappableImpl[T]

  def materializeMappableImpl[T: c.WeakTypeTag](c: whitebox.Context): c.Expr[SQLMappable[T]] = {
    import c.universe._
    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.get.paramLists.head
    var i = 0
    val fromMapParams = fields.map { field ⇒
      i = i + 1
      val name = field.name.toTermName
      val returnType = tpe.decl(name).typeSignature
      q"map.any($i).asInstanceOf[$returnType]"
    }
    val values = fields.map(f => q"t.${f.name.toTermName}")
    val fb = s"insert into ${tpe.typeSymbol.name.decodedName.toString} (${fields.map(_.name.toTermName.toString).mkString(",")})"
    val toDBParams =
      q"""def mks(v: Any*) = $fb + " values (" + v.map("'" + _.toString + "'").mkString(",") + ")"
         mks(..$values)
       """

    c.Expr[SQLMappable[T]] { q"""
      new SQLMappable[$tpe] {
        def toDBType(t: $tpe): String = { $toDBParams }
        def fromDBType(map: WrappedResultSet): $tpe = $companion(..$fromMapParams)
      }
    """ }
  }
}