package io.github.lemonxah.macros

import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
 * Project: dbabspro
 * Created on 2015/07/08.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */
trait GenericMappable[A] extends Mappable[A, Map[String, Any], Map[String, Any]]
object GenericMappable {
  implicit def materializeMappable[T]: GenericMappable[T] = macro materializeMappableImpl[T]

  def materializeMappableImpl[T: c.WeakTypeTag](c: whitebox.Context): c.Expr[GenericMappable[T]] = {
    import c.universe._
    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    val fields = tpe.decls.collectFirst {
      case m: MethodSymbol if m.isPrimaryConstructor ⇒ m
    }.get.paramLists.head

    val (toMapParams, fromMapParams) = fields.map { field ⇒
      val name = field.name.toTermName
      val decoded = name.decodedName.toString
      val returnType = tpe.decl(name).typeSignature

      (q"$decoded → t.$name", q"map($decoded).asInstanceOf[$returnType]")
    }.unzip

    c.Expr[GenericMappable[T]] { q"""
      new GenericMappable[$tpe] {
        def toDBType(t: $tpe): Map[String, Any] = Map(..$toMapParams)
        def fromDBType(map: Map[String, Any]): $tpe = $companion(..$fromMapParams)
      }
    """ }
  }
}
