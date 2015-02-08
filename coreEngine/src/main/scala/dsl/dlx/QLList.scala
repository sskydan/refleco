package dsl.dlx

trait QLList {
  type Neighbour <: QLList
  var up,dn : QLList
  var l,r : Neighbour
  val c: QuadHeader
  
  /** TODO don't lose type information in traversal?
   */
  def traverseN[R](step: QLList => QLList)(fn: QLList => R): List[R] = {
    lazy val way: Stream[QLList] = step(this) #:: (way map step)
    
    (way takeWhile (_ != this) map fn).toList
  }

  def traverse[R](step: QLList => QLList)(fn: QLList => R): List[R] = fn(this) :: traverseN(step)(fn)
  
  def foreachN(step: QLList => QLList)(fn: QLList => Unit): Unit = {
    var i = step(this)
    while (i != this) {
      fn(i)
      i = step(i)
    }
  }
}

class QuadNode(val c: QuadHeader) extends QLList {
  type Neighbour = QuadNode
  
  var up: QLList = this; var dn: QLList = this
  var l = this; var r = this
  
  override def toString = "+"
}

class QuadHeader(val name: String) extends QLList {
  type Neighbour = QuadHeader

  var up: QLList = this; var dn: QLList = this
  var l = this; var r = this
  val c: QuadHeader = this
  
  var size: Int = 0
  
  def cover = {
    r.l = l
    l.r = r
          
    // loop down the column (instances of the element in different sets)
    foreachN(_.dn){ i =>
      
      // loop across the subset elements
      i.foreachN(_.r){ j =>
            
        j.dn.up = j.up
        j.up.dn = j.dn
        j.c.size = j.c.size - 1
      }
    }
  }
  
  def uncover = {
    foreachN(_.up){ i =>

      i.foreachN(_.l){ j =>
            
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

