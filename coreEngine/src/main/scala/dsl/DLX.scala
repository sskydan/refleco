package dsl

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
  
  override def toString = s"+"
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


case class QLMatrix(root: QuadHeader)

object QLMatrix {
  
  def construct(cols: Seq[Seq[Int]], names: Seq[String]) = {
    val root = new QuadHeader("root")
    
    // link the columns (vertically)
    val matrix = (cols zip names) map { case (col, name) =>
      val header = new QuadHeader(name)
      val contents = col map (_ -> new QuadNode(header))
      
      contents.foldLeft(header: QLList){
        case (l, (1, r)) =>
          l.dn = r
          r.up = l
          header.size = header.size + 1
          r
        case (l, _) => l
      }
      val last = contents.reverse find (_._1 == 1) map (_._2) getOrElse header
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
    
    QLMatrix(root)
  }
  
}