package org.nlogo.build

import java.io.File
import java.net.{ URL, URLClassLoader }
import java.nio.file.{ Files, Path, Paths }

import org.nlogo.api.PrimitiveManager
import org.nlogo.core.{ Primitive, Syntax }

import scala.collection.mutable.ArrayBuffer

import ujson.{ Arr, Bool, Num, Obj, Str, Value }

object PrimsJson {
  def main(args: Array[String]): Unit = {
    generate(args(0), args(1), new File(args(2)), Paths.get(args(3)))
  }

  private def generate(extensionName: String, classManagerName: String, extensionJar: File, dest: Path): Unit = {
    val loader = new URLClassLoader(Array(new URL(s"jar:file:${extensionJar.getAbsolutePath}!/")),
                                    getClass.getClassLoader)

    val prims = ArrayBuffer[(String, Primitive)]()

    val primManager = new PrimitiveManager {
      override def addPrimitive(name: String, prim: Primitive): Unit = {
        prims += ((name, prim))
      }

      def autoImportPrimitives: Boolean = false
      def autoImportPrimitives_=(value: Boolean): Unit = {}
    }

    val classManagerClass = loader.loadClass(classManagerName)
    val classManager = classManagerClass.getDeclaredConstructor().newInstance()
    val load = classManagerClass.getDeclaredMethod("load", classOf[PrimitiveManager])

    load.invoke(classManager, primManager)

    Files.writeString(dest, ujson.write(Obj(
      "name" -> extensionName,
      "prims" -> prims.map {
        case (name, prim) =>
          val syntax = prim.getSyntax

          val precOffset: Int = {
            if (syntax.ret == Syntax.VoidType) {
              syntax.precedence - Syntax.CommandPrecedence
            } else {
              syntax.precedence - Syntax.NormalPrecedence
            }
          }

          Obj.from(
            Map(
              "name" -> Str(name),
              "argTypes" -> Arr.from(syntax.allArgs.map(getTypeInfo)),
              "returnType" -> getTypeInfo(syntax.ret)
            ) ++ addIf("defaultArgCount", syntax.dfault, _.value != syntax.right.size)
              ++ addIf("minArgCount", syntax.minimum, _.value != syntax.dfault)
              ++ addIf("agentClassString", syntax.agentClassString, _.value != "OTPL")
              ++ addIf("blockAgentClassString", syntax.blockAgentClassString.getOrElse("OTPL"), _.value != "OTPL")
              ++ addIf("isInfix", syntax.isInfix, _.value)
              ++ addIf("precedenceOffset", precOffset, _.value != 0)
          )
      }
    ), 2) + "\n")
  }

  private def getTypeInfo(tpe: Int): Value = {
    val optional = matches(tpe, Syntax.OptionalType)
    val repeatable = matches(tpe, Syntax.RepeatableType)
    val names = getTypeNames(remove(tpe, Syntax.OptionalType | Syntax.RepeatableType))

    if (optional || repeatable || names.size > 1) {
      Obj.from(
        Map(
          if (names.size == 1) {
            "type" -> Str(names.head)
          } else {
            "types" -> Arr.from(names)
          }
        ) ++ addIf("isRepeatable", repeatable, _.value)
          ++ addIf("isOptional", optional, _.value)
      )
    } else {
      if (names.isEmpty) {
        "unit"
      } else {
        names.head
      }
    }
  }

  private def getTypeNames(tpe: Int): Seq[String] = {
    if (tpe == 0) {
      Seq()
    } else if (matches(tpe, Syntax.SymbolType)) {
      "symbol" +: getTypeNames(remove(tpe, Syntax.SymbolType))
    } else if (matches(tpe, Syntax.CodeBlockType)) {
      "codeblock" +: getTypeNames(remove(tpe, Syntax.CodeBlockType))
    } else if (matches(tpe, Syntax.BracketedType)) {
      "bracketed" +: getTypeNames(remove(tpe, Syntax.BracketedType))
    } else if (matches(tpe, Syntax.ReporterBlockType)) {
      "reporterblock" +: getTypeNames(remove(tpe, Syntax.ReporterBlockType))
    } else if (matches(tpe, Syntax.OtherBlockType)) {
      "otherblock" +: getTypeNames(remove(tpe, Syntax.OtherBlockType))
    } else if (matches(tpe, Syntax.NumberBlockType)) {
      "numberblock" +: getTypeNames(remove(tpe, Syntax.NumberBlockType))
    } else if (matches(tpe, Syntax.BooleanBlockType)) {
      "booleanblock" +: getTypeNames(remove(tpe, Syntax.BooleanBlockType))
    } else if (matches(tpe, Syntax.CommandBlockType)) {
      "commandblock" +: getTypeNames(remove(tpe, Syntax.CommandBlockType))
    } else if (matches(tpe, Syntax.ReferenceType)) {
      "reference" +: getTypeNames(remove(tpe, Syntax.ReferenceType))
    } else if (matches(tpe, Syntax.WildcardType)) {
      "wildcard" +: getTypeNames(remove(tpe, Syntax.WildcardType))
    } else if (matches(tpe, Syntax.ReadableType)) {
      "readable" +: getTypeNames(remove(tpe, Syntax.ReadableType))
    } else if (matches(tpe, Syntax.AgentType)) {
      "agent" +: getTypeNames(remove(tpe, Syntax.AgentType))
    } else if (matches(tpe, Syntax.ReporterType)) {
      "reporter" +: getTypeNames(remove(tpe, Syntax.ReporterType))
    } else if (matches(tpe, Syntax.CommandType)) {
      "command" +: getTypeNames(remove(tpe, Syntax.CommandType))
    } else if (matches(tpe, Syntax.LinkType)) {
      "link" +: getTypeNames(remove(tpe, Syntax.LinkType))
    } else if (matches(tpe, Syntax.PatchType)) {
      "patch" +: getTypeNames(remove(tpe, Syntax.PatchType))
    } else if (matches(tpe, Syntax.TurtleType)) {
      "turtle" +: getTypeNames(remove(tpe, Syntax.TurtleType))
    } else if (matches(tpe, Syntax.NobodyType)) {
      "nobody" +: getTypeNames(remove(tpe, Syntax.NobodyType))
    } else if (matches(tpe, Syntax.AgentsetType)) {
      "agentset" +: getTypeNames(remove(tpe, Syntax.AgentsetType))
    } else if (matches(tpe, Syntax.LinksetType)) {
      "linkset" +: getTypeNames(remove(tpe, Syntax.LinksetType))
    } else if (matches(tpe, Syntax.PatchsetType)) {
      "patchset" +: getTypeNames(remove(tpe, Syntax.PatchsetType))
    } else if (matches(tpe, Syntax.TurtlesetType)) {
      "turtleset" +: getTypeNames(remove(tpe, Syntax.TurtlesetType))
    } else if (matches(tpe, Syntax.ListType)) {
      "list" +: getTypeNames(remove(tpe, Syntax.ListType))
    } else if (matches(tpe, Syntax.StringType)) {
      "string" +: getTypeNames(remove(tpe, Syntax.StringType))
    } else if (matches(tpe, Syntax.BooleanType)) {
      "boolean" +: getTypeNames(remove(tpe, Syntax.BooleanType))
    } else if (matches(tpe, Syntax.NumberType)) {
      "number" +: getTypeNames(remove(tpe, Syntax.NumberType))
    } else {
      throw new Exception(s"Unknown type constant: $tpe.")
    }
  }

  private def addIf[T <: Value](key: String, value: T, condition: T => Boolean): Option[(String, Value)] = {
    if (condition(value)) {
      Some(key -> value)
    } else {
      None
    }
  }

  private def matches(a: Int, b: Int): Boolean =
    (a & b) == b

  private def remove(a: Int, b: Int): Int =
    a & ~b
}
