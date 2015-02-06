package dsl.dlx


case class DLX(matrix: QLMatrix) {
  
  lazy val solutions = search() map (_.reverse map getRow)
  
  private def search(path: List[QLList] = Nil): Seq[List[QLList]] =
    if (matrix.root.r != matrix.root) {
      
      val c = chooseColumn
      c.cover
      
      val solutions = c.traverseN(_.dn){ r =>

        r.foreachN(_.r)(_.c.cover)
        val subSolutions = search(r :: path)
        r.foreachN(_.l)(_.c.uncover)
        
        subSolutions
      }
      
      c.uncover
      solutions.flatten
      
    } else Seq(path) 
  
  def chooseColumn: QuadHeader = matrix.root.r
  
  def getRow(node: QLList): List[String] = node.traverse(_.r)(_.c.name)
}
