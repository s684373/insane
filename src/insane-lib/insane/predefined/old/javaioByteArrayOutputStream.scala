package insane
package predefined

import annotations._

@AbstractsClass("java.io.ByteArrayOutputStream")
class javaioByteArrayOutputStream {
  @AbstractsMethod("java.io.ByteArrayOutputStream.<init>((x$1: Int)java.io.ByteArrayOutputStream)")
  def ____init__(x1: Int): javaioByteArrayOutputStream = {
    this
  }
  @AbstractsMethod("java.io.ByteArrayOutputStream.toByteArray(()Array[Byte])")
  def __toByteArray(): Array[Byte] = {
    new Array[Byte](1)
  }
}
