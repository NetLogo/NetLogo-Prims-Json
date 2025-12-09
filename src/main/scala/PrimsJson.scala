package org.nlogo.build

import java.io.File
import java.net.{ URL, URLClassLoader }
import java.nio.file.{ Files, Path, Paths }

import org.nlogo.api.PrimitiveManager
import org.nlogo.core.{ Primitive, TypeNames }

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
            "argTypes" -> syntax.right.map(TypeNames.name).toBuffer,
            "returnType" -> TypeNames.name(syntax.ret)
          )
      }
    ), 2) + "\n")
  }
}
