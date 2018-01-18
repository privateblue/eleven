package graph

object Index {
  def apply(i: Int) = new Index(i)
}
class Index(val i: Int) extends AnyVal {
  override def toString = i.toString
}

object Direction {
  def apply(d: Int) = new Direction(d)
}
class Direction(val d: Int) extends AnyVal {
  override def toString = d.toString
}
