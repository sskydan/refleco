package dlx

import scala.reflect.ClassTag


/** typeclass providing the DLX interface, and  simple DLX implementation
 *    from the paper, no frills or whistles
 *  TODO do we really need to pass the classtag around
 */
class DLX[N <: QuadNodeIntf[N] : ClassTag] {
  
  /** algo to choose the column (particular element) when doing a search pass
   */
	def chooseColumn(root: QuadHeader): QuadHeader = root.r
  
  /** this is the traversal method that actually performs the search steps
   */
  def search(root: QuadHeader, path: List[N] = Nil): Seq[List[N]] = 
    if (root.r != root) {
      val c = chooseColumn(root)
      c.cover
      
      val solutions = c.traverseRemG(_.dn) { 
        case r: N =>
          r.foreachRem(_.r)(_.c.cover)
          val subSolutions = search(root, r :: path)
          r.foreachRem(_.l)(_.c.uncover)
          
          subSolutions
          
        case _ => Nil
      }
      
      c.uncover
      solutions.flatten
      
    } else Seq(path)
}

object DLX {
  
  /** low-priority dlx implementation
   *  @note the basic functionality is put into the DLX class itself so that this implicit can be
   *    defined in terms of T<:QuadNodeIntf[T] - although this forever restricts the default
   *    implementation that is provided, it was not possible (TODO) to provide a defalt implementation
   *    (for T<:QuadNodeIntf[T]) that worked propertly 
   */
  implicit def defaultDLX[T <: QuadNodeIntf[T] : ClassTag] = new DLX[T]
}
