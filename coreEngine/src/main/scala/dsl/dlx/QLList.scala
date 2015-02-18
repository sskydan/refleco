package dsl.dlx


/** quad-linked list (like a doubly-linked list, but with up and down)
 *  FIXME proper/standardized type signatures for the QLList methods 
 *  @note because variance doesn't play well with vars (setters and getters), and self.type
 *    is a singleton type and had some issues pairing with abstract type parameters (in my exp)
 *    we resort to existential types for up/dn as a way to provide a LUB (least upper bound)
 */
sealed abstract class QLList[N <: QLList[N]] { self: N =>
  var up: T forSome { type T <: QLList[T] } = self
  var dn: T forSome { type T <: QLList[T] } = self
  var l: N = self
  var r: N = self
  val c: QuadHeader
  
  /** Traverse the matrix starting from (not including) this node according to step,
   *    while applying fn to every node visited.
   *  @tparam T the type of node we will be traversing
   *  @tparam R the type of the mapping fn
   *  @param step the traversal function
   *  @param fn the mapping fn
   *  @return the results of applying fn to every node visited
   */
  def traverseRem[T >: N <: QLList[_], R](step: T => T)(fn: T => R): List[R] = {
    lazy val way: Stream[T] = step(this) #:: (way map step)
    
    (way takeWhile (_ != this) map fn).toList
  }

  /** Like traverseRem, but accepts only traversals on generic QLList - exists only to 
   *    allow not specifying parameter type at the call site
   *  @see traverseRem
   *  @see todo on QLList
   */
  def traverseRemG[R](step: QLList[_<:QLList[_]] => QLList[_<:QLList[_]])(fn: QLList[_<:QLList[_]] => R): List[R] = 
	  traverseRem(step)(fn)

  /** Traverse the matrix starting from (including) this node according to step,
   *    while applying fn to every node visited.
   *  @tparam R the type of the mapping fn
   *  @param step the traversal function
   *  @param fn the mapping fn
   *  @return the results of applying fn to every node visited
   */
  def traverse[T >: N <: QLList[_], R](step: T => T)(fn: T => R): List[R] = 
    fn(this) :: traverseRem(step)(fn)

    
  /** Efficient implementation of matrix traversal; accepts only side-effecting functions
   *  Does not apply fn to the starting node
   *  @param step the traversal function
   *  @param fn the function to apply on every visited node
   */
  def foreachRem[T >: N <: QLList[_]](step: T => T)(fn: T => Unit): Unit = {
    var i = step(self)
    while (i != self) {
      fn(i)
      i = step(i)
    }
  }
  
  /** Efficient implementation of matrix traversal; accepts only side-effecting functions
   *  @param step the traversal function
   *  @param fn the function to apply on every visited node
   */
  def foreach[T >: N <: QLList[_]](step: T => T)(fn: T => Unit): Unit = {
    fn(this)
    foreachRem(step)(fn)
  }
}

/** interface for all normal (non-header) implementations of the QLList
 */
trait QuadNodeIntf[N <: QLList[N]] extends QLList[N] { self: N => }

/** quad-linked node
 */
class QuadNode(val c: QuadHeader) extends QuadNodeIntf[QuadNode] {
}

/** quad-linked node which has not been confirmed to be wanted in the matrix
 *  FIXME should be a subtype of QuadNode, or something.
 */
abstract class UntestedQuadNode[T,N <: QLList[N]](val c: QuadHeader) extends QuadNodeIntf[N] { self: N =>
  
  def evaluateFailed = foreach(_.r){ n =>
    n.dn.up = n.up
    n.up.dn = n.dn
    n.c.size = n.c.size - 1
  }
  
  val execute: this.type => T
  val evaluate: T => Boolean
  
  lazy val executionResults = execute(this)
  lazy val evaluationResults = evaluate(executionResults)
}

/** quad-linked node which is meant to act as the header of a column
 *  TODO shold cover/uncover be defined somewhere else?
 */
class QuadHeader(val name: String) extends QLList[QuadHeader] {
  val c: QuadHeader = this
  
  var size: Int = 0
  
  /** cover implementation from DLX paper - hide all the rows which have elements in this
   *    column by splicing nodes out of the linked lists
   */
  def cover = {
    r.l = l
    l.r = r
          
    // loop down the column (instances of the element in different sets)
    foreachRem((n:QLList[_<:QLList[_]]) => n.dn){ i =>
      
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
    foreachRem((n:QLList[_<:QLList[_]]) => n.up){ i =>

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

