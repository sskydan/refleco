package ner

import dlx._
import scala.collection.mutable.HashMap


/** class representing named entity nodes
 */
class NERow(val elems: Seq[NERNode]) {
  
  lazy val execute = {
    val allWords = elems map (_.c.name) mkString " "
    NERecognizer.identifyChunk(allWords)
  }
  
  lazy val evaluate = !execute.isEmpty
}

/** FIXME hash hack assumes there are no duplicate rows
 *  FIXME use a proper threadsafe solution (scalaz memo)?
 */
object RowControl {
  val rowMap = HashMap[Int, NERow]()
  
	def getRowHeader(node: NERNode): NERow = {
	  val row = node.traverse[NERNode,NERNode](_.r)(x => x)

		def hash(node: NERNode): Int = 
      row.map(_.c.name.hashCode).foldLeft(0)(_ + _)

    rowMap getOrElseUpdate (hash(node), new NERow(row))
	}  
}

class NERNode(c: QuadHeader) extends UntestedQuadNode[Seq[NE], NERNode](c) {
  lazy val rowHeader = RowControl.getRowHeader(this)
  
  val execute = () => rowHeader.execute
  val evaluate: Seq[NE] => Boolean = _ => rowHeader.evaluate
}

