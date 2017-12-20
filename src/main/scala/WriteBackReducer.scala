object WriteBackReducer {
  type Out = Map[Int, Int]

  val empty = Map.empty[Int, Int]
  def get(out: Out, i: Int): Int = out.get(i).getOrElse(0)
  def set(out: Out, i: Int, v: Int): Out = out + (i -> v)
  def add(out1: Out, out2: Out): Out =
    out1.keySet ++ out2.keySet map (k => k -> math.max(get(out1, k), get(out2, k))) toMap

  def reduce(dir: Int, graph: Graph[Int]): Graph[Int] = {
    val dag = graph.direction(dir)
    def reduce0(i: Int, out: Out, path: Vector[Int], focus: Int): Out = {
      val mergeb = nextValues(dag, i).size > 1
      val splitb =
        dag.to(i)
          .filterNot(path.contains)
          .exists(c => nextValues(dag, c).size > 1 || dag.ends.contains(c))
      val p = path :+ i
      val f = if (splitb) p.size - 1 else focus
      val focused = p(f)
      val v = dag.at(i)
      val fv = get(out, focused)
      if (v == 0) {
        val cs = dag.from(i).map(c => reduce0(c, out, p, if (mergeb) p.size else f))
        resolve(out, cs)
      } else if (fv == 0) {
        val updated = set(out, focused, v)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, if (mergeb) p.size else f))
        resolve(updated, cs)
      } else if (fv == v) {
        val updated = set(out, focused, v + 1)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, if (mergeb) p.size else f + 1))
        resolve(updated, cs)
      } else {
        val nf = p.drop(f + 1).headOption.getOrElse(i)
        val updated = set(out, nf, v)
        val cs = dag.from(i).map(c => reduce0(c, updated, p, if (mergeb) p.size else f + 1))
        resolve(updated, cs)
      }
    }
    val cs = dag.ends.map { r => reduce0(r, empty, Vector.empty, 0) }
    val blank = DAG(IndexedSeq.fill(dag.values.size)(0), dag.edges)
    val updated = resolve(empty, cs).foldLeft(blank)((d, vs) => d.set(vs._1, vs._2))
    graph.update(dir, updated)
  }

  def nextValues(dag: DAG[Int], i: Int): Set[Int] =
    dag.from(i).map {
      case c if dag.at(c) == 0 =>
        val cvs = nextValues(dag, c)
        if (cvs.size == 1) cvs.head else 0
      case c => dag.at(c)
    }.filterNot(_ == 0)

  def resolve(parent: Out, cs: Set[Out]): Out = cs.foldLeft(parent)(add)
}