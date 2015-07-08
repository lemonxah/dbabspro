package io.github.lemonxah.macros

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros
import scala.reflect.macros._

/**
 * Project: dbabspro
 * Created on 2015/06/01.
 * ryno aka lemonxah -
 * https://github.com/lemonxah
 * http://stackoverflow.com/users/2919672/lemon-xah
 */
@compileTimeOnly("enable macro paradise to expand macro annotations")
class entity extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro entity.impl
}

object entity {
  def impl(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def extractCaseClassesParts(classDecl: ClassDef) = classDecl match {
      case q"case class $className(..$fields) extends ..$parents { ..$body }" =>
        (className, fields, parents, body)
    }

    def queryObjects(fields: List[ValDef]) = {
      fields.map(field => q"object ${field.name} extends Field[${field.tpt}](${field.name.decodedName.toString})")
    }

    def modifiedCompanion(compDeclOpt: Option[ModuleDef], fields: List[ValDef], className: TypeName) = {
      val queryObjs = queryObjects(fields)

      // leaving the json for converting DB data to web endpoint json
      val json =
        q"""
          object json {
            def toJson(o: $className): String ={
              grater[$className].toPrettyJSON(o)
            }
            def fromJson(s: String): $className = {
              grater[$className].fromJSON(s)
            }
            def toJson(o: Vector[$className]): String = {
              grater[$className].toPrettyJSONArray(o)
            }
            def fromJsonArray(s: String): Vector[$className] = {
              grater[$className].fromJSONArray(s).toVector
            }

          }
         """

      compDeclOpt map { compDecl =>
        val q"object $obj extends ..$bases { ..$body }" = compDecl
        q"""
          object $obj extends ..$bases {
            ..$body
            ..$queryObjs
           $json
          }
        """
      } getOrElse {
        q"""object ${className.toTermName} {
              ..$queryObjs
              $json
            }
        """
      }
    }

    def modifiedDeclaration(classDecl: ClassDef, compDeclOpt: Option[ModuleDef] = None) = {
      val (className, fields, parents, body) = extractCaseClassesParts(classDecl)

      val compDecl = modifiedCompanion(compDeclOpt, fields.asInstanceOf[List[ValDef]], className.asInstanceOf[TypeName])

      c.Expr(q"""
        $classDecl
        $compDecl
      """)
    }

    annottees.map(_.tree) match {
      case (classDecl: ClassDef) :: Nil => modifiedDeclaration(classDecl)
      case (classDecl: ClassDef) :: (compDecl: ModuleDef) :: Nil => modifiedDeclaration(classDecl, Some(compDecl))
      case _ => c.abort(c.enclosingPosition, "Invalid annottee")
    }
  }
}
