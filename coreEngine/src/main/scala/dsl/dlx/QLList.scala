package dsl.dlx


/** quad-linked list (like a doubly-linked list, but with up and down)
 *  TODO can this be structured better?
 *  FIXME Using "this.type"-annontation on vars (specifically) somehow stops the type 
 *    inferencer from properly understanding the var's type
 *    in the children classes. The current workaround is to use the SELF abstract type
 *    member, but it is kind of ugly to have to overwrite that in every subclass
 *  TODO the traverseRem alternatives should be under a single name
 */
sealed abstract class QLList {
  type SELF >: this.type <: QLList
  var up : QLList = this
  var dn : QLList = this
  var l: SELF = this
  var r: SELF = this
  val c: QuadHeader
  
  /** Traverse the matrix starting from (not including) this node according to step,
   *    while applying fn to every node visited.
   *  @tparam T the type of node we will be traversing
   *  @tparam R the type of the mapping fn
   *  @param step the traversal function
   *  @param fn the mapping fn
   *  @return the results of applying fn to every node visited
   */
  def traverseRem[T >: this.type <: QLList, R](step: T => T)(fn: T => R): List[R] = {
    lazy val way: Stream[T] = step(this) #:: (way map step)
    
    (way takeWhile (_ != this) map fn).toList
  }

  /** Like traverseRem, but accepts only traversals on generic QLList - exists only to 
   *    allow not specifying parameter type at the call site
   *  @see traverseRem
   *  @see todo on QLList
   */
  def traverseRemG[R](step: QLList => QLList)(fn: QLList => R): List[R] = 
	  traverseRem[QLList,R](step)(fn)

  /** Traverse the matrix starting from (including) this node according to step,
   *    while applying fn to every node visited.
   *  @tparam R the type of the mapping fn
   *  @param step the traversal function
   *  @param fn the mapping fn
   *  @return the results of applying fn to every node visited
   */
  def traverse[R](step: QLList => QLList)(fn: QLList => R): List[R] = 
    fn(this) :: traverseRem[QLList,R](step)(fn)

  /** Efficient implementation of matrix traversal; accepts only side-effecting functions
   *  @param step the traversal function
   *  @param fn the function to apply on every visited node
   */
  def foreachRem(step: QLList => QLList)(fn: QLList => Unit): Unit = {
    var i = step(this)
    while (i != this) {
      fn(i)
      i = step(i)
    }
  }
}

/** interface for all normal (non-header) implementations of the QLList
 */
trait QuadNodeIntf extends QLList

/** quad-linked node
 */
class QuadNode(val c: QuadHeader) extends QuadNodeIntf { type SELF = QuadNode }

/** quad-linked node which has not been confirmed to be wanted in the matrix
 */
class UntestedQuadNode(val c: QuadHeader, val rowInfo: String) extends QuadNodeIntf {
  type SELF = UntestedQuadNode
//  def evaluate(): Boolean
}

/** quad-linked node which is meant to act as the header of a column
 *  TODO shold cover/uncover be defined somewhere else?
 */
class QuadHeader(val name: String) extends QLList {
  type SELF = QuadHeader
  val c: QuadHeader = this
  
  var size: Int = 0
  
  /** cover implementation from DLX paper - hide all the rows which have elements in this
   *    column by splicing nodes out of the linked lists
   */
  def cover = {
    r.l = l
    l.r = r
          
    // loop down the column (instances of the element in different sets)
    foreachRem(_.dn){ i =>
      
      // loop across the subset elements
      i.foreachRem(_.r){ j =>
            
        j.dn.up = j.up
        j.up.dn = j.dn
        j.c.size = j.c.size - 1
      }
    }
  }
  
  /** uncover implementation from DLX paper - re-expose all the rows which have elements
   *    in this column by splicing them back into their original positions
   */
  def uncover = {
    foreachRem(_.up){ i =>

      i.foreachRem(_.l){ j =>
            
        j.dn.up = j
        j.up.dn = j
        j.c.size = j.c.size + 1
      }
    }

    l.r = this
    r.l = this
  }
  
  override def toString = s"[ $name - $size ]"
}

