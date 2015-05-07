package ukrm

import org.apache.spark._
import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime
import facts.Fact


/** interface for the vertex property to be used in the graph
 */
trait Vertex
/** vertex that represents a simple reference
 */
case class VRef(val id: Long) extends Vertex


/** interface for our graph implementation
 */
trait UKRM[C[_], G[_,_], E[_]] {
  import UKRM._
  
  val entities: C[(VertexId, Vertex)]
  val relationships: C[E[Relation]]
  
  val graph: G[Vertex, Relation]
}
object UKRM {
  type Relation = (Long, Option[DateTime])
}


/** implementation of the UKRM on spark's graphX
 */
class UKRMX(
  val entities: RDD[(VertexId, Vertex)],
  val relationships: RDD[Edge[(Long, Option[DateTime])]]
) extends UKRM[RDD, Graph, Edge] {
  import UKRM._
  
  val sconf = new SparkConf()
  implicit val spark = new SparkContext(sconf)
  
  val defaultEntity = VRef(0)
  val graph: Graph[Vertex, Relation] = Graph(entities, relationships, defaultEntity)
}


object UKRMXBuilders {
  
  implicit def fromTriples(triples: Seq[(String, String, String)]): UKRMX = {
    
    ???
  }
  
}