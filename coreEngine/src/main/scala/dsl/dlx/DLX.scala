package dsl.dlx

/** dancing links algo x typeclass interface
 */
abstract class DLX[T <: QuadNodeIntf[T]](root: QuadHeader) {
  def search(path: List[T] = Nil): Seq[List[T]]
  def chooseColumn: QuadHeader
  
  def getRowId(node: T): List[String] = node.traverse[T,String](_.r)(_.c.name)
  def solve: Seq[List[List[String]]] = search() map (_.reverse map getRowId)
}

object DLX {
  
  /** simple DLX implementation from the paper, no frills or whistles
   */
  implicit class SimpleDLX(root: QuadHeader) extends DLX[QuadNode](root) {
    def search(path: List[QuadNode] = Nil): Seq[List[QuadNode]] =
      if (root.r != root) {
        
        val c = chooseColumn
        c.cover
        
        val solutions = c.traverseRemG(_.dn) { 
          case r:QuadNode =>
            r.foreachRem(_.r)(_.c.cover)
            val subSolutions = search(r :: path)
            r.foreachRem(_.l)(_.c.uncover)
            
            subSolutions
            
          case _ => Nil
        }
        
        c.uncover
        solutions.flatten
        
      } else Seq(path) 
    
    def chooseColumn: QuadHeader = root.r
  }
}
