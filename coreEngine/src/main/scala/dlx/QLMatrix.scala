package dlx

import scala.collection.immutable.ListMap


/** class representing a quad-linked matrix
 *  TODO getting row ids feels a bit awkward
 */
class QLMatrix[T <: QuadNodeIntf[T]](val root: QuadHeader) {
  
  def solve[R](getRow: T => R = (_:T).c.name)(implicit ev: DLX[T]) = 
    ev.search(root) map (_.reverse map (node => node.traverse[T,R](_.r)(getRow(_))))
}

object QLMatrix {
  
  def fromSparse[T <: QuadNodeIntf[T]](
    rows: Seq[Seq[String]],
    names: Seq[String],
    nodeBuilder: QuadHeader => T
  ): QLMatrix[T] = {
    
    // we keep the name ordering so that element adjacency is preserved
    val init = ListMap[String,Vector[Int]](names.map(_ -> Vector()): _*)
    
    val matrix = rows.foldLeft(init) {
      case (matrix, row) => matrix map { 
        case (k,v) if row contains k => k -> (v :+ 1)
        case (k,v) => k -> (v :+ 0)
      }
    }

    QLMatrix(matrix.values.toList, names, nodeBuilder)
  }
  
  def apply[T <: QuadNodeIntf[T]](
    cols: Seq[Seq[Int]],
    names: Seq[String],
    nodeBuilder: QuadHeader => T
  ): QLMatrix[T] = {
    
		type GenericNode = S forSome { type S <: QLList[S] }
    val root = new QuadHeader("root")
    
    // link the columns (vertically)
    val matrix = (cols zip names) map { case (col, name) =>
      val header = new QuadHeader(name)
      val contents = col map (_ -> nodeBuilder(header))
      
      contents.foldLeft[GenericNode](header){
        case (l, (1, r)) =>
          l.dn = r
          r.up = l
          header.size = header.size + 1
          r
        case (l, _) => l
      }
      val last = contents.reverse find (_._1 == 1) map (_._2:GenericNode) getOrElse header
      header.up = last
      last.dn = header
      
      contents
    }
    
    // link the rows
    matrix.transpose foreach { row =>
      row.tail.foldLeft(row.head._2){
        case (l, (1, r)) =>
          l.r = r
          r.l = l
          r
        case (l, _) => l
      }
      val first = row.find(_._1 == 1).get._2
      val last = row.reverse.find(_._1 == 1).get._2
      first.l = last
      last.r = first
      val x = first.l
      val y = last.r
    }
    
    // link the special headers row
    matrix.transpose.head.foldLeft(root){
      case (l, (_, r)) =>
        l.r = r.c
        r.c.l = l
        r.c
    }
    val last = matrix.transpose.head.last._2.c
    root.l = last
    last.r = root
    
    new QLMatrix(root)
  }
}

