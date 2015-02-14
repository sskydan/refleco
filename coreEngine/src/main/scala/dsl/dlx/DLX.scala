package dsl.dlx

/** dancing links algo x typeclass interface
 */
abstract class DLX[T <: QuadNode](root: QuadHeader) {
  def search(path: List[QLList] = Nil): Seq[List[QLList]]
  def chooseColumn: QuadHeader
  
  def getRowId(node: QLList): List[String] = node.traverse(_.r)(_.c.name)
  def solve: Seq[List[List[String]]] = search() map (_.reverse map getRowId)
}

object DLX {
  
  /** simple DLX implementation from the paper, no frills or whistles
   */
  implicit class SimpleDLX(root: QuadHeader) extends DLX[QuadNode](root) {
    def search(path: List[QLList] = Nil): Seq[List[QLList]] =
      if (root.r != root) {
        
        val c = chooseColumn
        c.cover
        
        val solutions = c.traverseRemG(_.dn){ r =>
  
          r.foreachRem(_.r)(_.c.cover)
          val subSolutions = search(r :: path)
          r.foreachRem(_.l)(_.c.uncover)
          
          subSolutions
        }
        
        c.uncover
        solutions.flatten
        
      } else Seq(path) 
    
    def chooseColumn: QuadHeader = root.r
  }
}
