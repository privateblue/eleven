package engine

object Value {
  val zero = Value(0)
  def max(v1: Value, v2: Value) = Value(math.max(v1.v, v2.v))
}

case class Value(v: Int) {
  def next = Value(v + 1)
  def prev = Value(v - 1)
}
