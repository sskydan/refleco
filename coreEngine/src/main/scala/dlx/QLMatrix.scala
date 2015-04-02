package dlx

import scala.collection.immutable.ListMap


/** class representing a quad-linked matrix
 *  TODO qlmatrix type parameter doesn't actually enforce the type of node linked to the 
 *    root quadheader
 *  TODO getting row ids feels a bit awkward
 */
class QLMatrix[T <: QuadNodeIntf[T]](val root: QuadHeader) {
  
  val allNames = root.traverseRem((_:QuadHeader).r)(_.name)  
  val rowNames = (node: T) => node.traverse[T,String](_.r)(_.c.name)
  
  def solve[R](getRow: T => R = rowNames)(implicit ev: DLX[T]): Seq[Seq[R]] = 
    ev.search(root) map (_.reverse map getRow)
}

object QLMatrix {
  
  def fromSparse[T <: QuadNodeIntf[T]](
    rows: Seq[Seq[(String,Int)]],
    names: Seq[String],
    nodeBuilder: QuadHeader => T
  ): QLMatrix[T] = {
    
    val matrix = for {
      r <- rows
      leftPadding = (1 to r.head._2) map (_ => 0)
      rightPadding = (r.last._2 to names.size-2) map (_ => 0)
    } yield leftPadding ++ (r map (_ => 1)) ++ rightPadding
    
    QLMatrix(matrix.transpose, names, nodeBuilder)
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
      val nodeCol = col map (_ -> nodeBuilder(header))
      
      nodeCol.foldLeft[GenericNode](header){
        case (l, (1, r)) =>
          l.dn = r
          r.up = l
          header.size = header.size + 1
          r
        case (l, _) => l
      }
      val last = nodeCol.reverse find (_._1 == 1) map (_._2:GenericNode) getOrElse header
      header.up = last
      last.dn = header
      
      nodeCol
    }
    
    val matrixT = matrix.transpose
    
    // link the rows
    matrixT foreach { row =>
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
    }
    
    // link the special headers row
    matrixT.head.foldLeft(root){
      case (l, (_, r)) =>
        l.r = r.c
        r.c.l = l
        r.c
    }
    val last = matrixT.head.last._2.c
    root.l = last
    last.r = root
    
    new QLMatrix(root)
  }
}

