package insane
package predefined

import annotations._

@AbstractsClass("java.util.WeakHashMap")
class javautilWeakHashMap {
  @AbstractsMethod("java.util.WeakHashMap.get((x$1:java.lang.Object)java.lang.Object)")
  def get(x1 : java.lang.Object) : java.lang.Object = { new java.lang.Object() }

  @AbstractsMethod("java.util.WeakHashMap.<init>(()java.util.WeakHashMap)")
  def PLOPINIT() : java.util.WeakHashMap = { new java.util.WeakHashMap() }

  @AbstractsMethod("java.util.WeakHashMap.put((x$1:java.lang.Object, x$2:java.lang.Object)java.lang.Object)")
  def put(x1 : java.lang.Object, x2 : java.lang.Object) : java.lang.Object = { new java.lang.Object() }

}