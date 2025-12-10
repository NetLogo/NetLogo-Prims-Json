package org.nlogo.build

import java.io.File
import java.net.{ URL, URLClassLoader }
import java.nio.file.{ Files, Path, Paths }

import org.nlogo.api.PrimitiveManager
import org.nlogo.core.{ Primitive, Syntax }

import scala.collection.mutable.ArrayBuffer

import ujson.Obj

object PrimsJson {
  def main(args: Array[String]): Unit = {
    generate(args(0), args(1), new File(args(2)), Paths.get(args(3)))
  }

  def generate(extensionName: String, classManagerName: String, extensionJar: File, dest: Path): Unit = {
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

          Obj(
            "name" -> name,
            "argTypes" -> syntax.right.map(getTypeName).toBuffer,
            "returnType" -> getTypeName(syntax.ret)
          )
      }
    ), 2) + "\n")
  }

  private def getTypeName(tpe: Int): String = {
    tpe match {
      case Syntax.NumberType => "number"
      case Syntax.BooleanType => "boolean"
      case Syntax.StringType => "string"
      case Syntax.ListType => "list"
      case Syntax.TurtlesetType => "turtleset"
      case Syntax.PatchsetType => "patchset"
      case Syntax.LinksetType => "linkset"
      case Syntax.AgentsetType => "agentset"
      case Syntax.NobodyType => "nobody"
      case Syntax.TurtleType => "turtle"
      case Syntax.PatchType => "patch"
      case Syntax.LinkType => "link"
      case Syntax.CommandType => "command"
      case Syntax.ReporterType => "reporter"
      case Syntax.AgentType => "agent"
      case Syntax.ReadableType => "readable"
      case Syntax.WildcardType => "wildcard"
      case Syntax.ReferenceType => "reference"
      case Syntax.CommandBlockType => "commandblock"
      case Syntax.BooleanBlockType => "booleanblock"
      case Syntax.NumberBlockType => "numberblock"
      case Syntax.OtherBlockType => "otherblock"
      case Syntax.ReporterBlockType => "reporterblock"
      case Syntax.BracketedType => "bracketed"
      case Syntax.RepeatableType => "repeatable"
      case Syntax.OptionalType => "optional"
      case Syntax.CodeBlockType => "codeblock"
      case Syntax.SymbolType => "symbol"
      case unk => throw new Exception(s"Unknown type: $unk")
    }
  }
}
