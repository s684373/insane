package insane
package predefined

import annotations._

@AbstractsClass("java.lang.Class")
class javalangClass {
  @AbstractsMethod("java.lang.ClassCastException.<init>((x$1:java.lang.String)java.lang.ClassCastException)")
  def PLOPINIT(x1 : java.lang.String) : java.lang.ClassCastException = { new java.lang.ClassCastException() }

  @AbstractsMethod("java.lang.Class.forName((x$1:java.lang.String)java.lang.Class)")
  def forName(x1 : java.lang.String) : java.lang.Class = { new java.lang.Class() }

  @AbstractsMethod("java.lang.Class.getComponentType(()java.lang.Class)")
  def getComponentType() : java.lang.Class = { new java.lang.Class() }

  @AbstractsMethod("java.lang.Class.getField((x$1:java.lang.String)java.lang.reflect.Field)")
  def getField(x1 : java.lang.String) : java.lang.reflect.Field = { new java.lang.reflect.Field() }

  @AbstractsMethod("java.lang.Class.getInterfaces(()Array[java.lang.Class])")
  def getInterfaces() : Array[java.lang.Class] = { new Array[java.lang.Class]() }

  @AbstractsMethod("java.lang.Class.getMethods(()Array[java.lang.reflect.Method])")
  def getMethods() : Array[java.lang.reflect.Method] = { new Array[java.lang.reflect.Method]() }

  @AbstractsMethod("java.lang.Class.getMethod((x$1:java.lang.String, x$2:Array[java.lang.Class])java.lang.reflect.Method)")
  def getMethod(x1 : java.lang.String, x2 : Array[java.lang.Class]) : java.lang.reflect.Method = { new java.lang.reflect.Method() }

  @AbstractsMethod("java.lang.Class.getName(()java.lang.String)")
  def getName() : java.lang.String = { "" }

  @AbstractsMethod("java.lang.Class.getPackage(()java.lang.Package)")
  def getPackage() : java.lang.Package = { new java.lang.Package() }

  @AbstractsMethod("java.lang.Class.getResourceAsStream((x$1:java.lang.String)java.io.InputStream)")
  def getResourceAsStream(x1 : java.lang.String) : java.io.InputStream = { new java.io.InputStream() }

  @AbstractsMethod("java.lang.Class.getSimpleName(()java.lang.String)")
  def getSimpleName() : java.lang.String = { "" }

  @AbstractsMethod("java.lang.Class.getSuperclass(()java.lang.Class)")
  def getSuperclass() : java.lang.Class = { new java.lang.Class() }

  @AbstractsMethod("java.lang.Class.isArray(()Boolean)")
  def isArray() : Boolean = { true }

  @AbstractsMethod("java.lang.Class.isAssignableFrom((x$1:java.lang.Class)Boolean)")
  def isAssignableFrom(x1 : java.lang.Class) : Boolean = { true }

  @AbstractsMethod("java.lang.Class.isPrimitive(()Boolean)")
  def isPrimitive() : Boolean = { true }

  @AbstractsMethod("java.lang.Class.newInstance(()java.lang.Object)")
  def newInstance() : java.lang.Object = { new java.lang.Object() }

  @AbstractsMethod("java.lang.Class.toString(()java.lang.String)")
  def toString() : java.lang.String = { "" }

}