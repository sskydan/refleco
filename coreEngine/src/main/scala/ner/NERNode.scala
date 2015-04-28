package ner

import dlx._
import scala.collection.mutable.HashMap


/** class representing named entity nodes
 *  FIXME handle row managment better (should be a set?)
 *  FIXME evaluate still returning positively when execute was empty?
 *  FIXME FIXME the equals checking has to specify c.name for some reason
 */
class NERNode(c: QuadHeader) extends UntestedQuadNode[Seq[NE], NERNode](c) {
  var row: Seq[NERNode] = Nil
	lazy val rowHeader = RowControl.getRowHeader(row)
			
	val execute = () => rowHeader.execute
	val evaluate: Seq[NE] => Boolean = _ => rowHeader.evaluate
  
  override def equals(obj: Any) = obj match {
    case obj:NERNode if row == obj.row && c.name == obj.c.name => true
    case _ => false
  }
}

class NERow(val elems: Seq[NERNode]) {
  lazy val execute = NERecognizer.identifyChunk(elems map (_.c.name) mkString " ")
  lazy val evaluate = !execute.isEmpty
}

/** FIXME hash hack assumes there are no duplicate rows
 *  FIXME use a proper threadsafe solution (scalaz memo)?
 */
object RowControl {
  val rowMap = HashMap[Double, NERow]()
  
  def getRowHeader(row: Seq[NERNode]): NERow = {
    def hash: Double = row.map(_.c.name.hashCode.toDouble).foldLeft(0.0)(_ + _)

    rowMap getOrElseUpdate (hash, new NERow(row))
  }  
}
