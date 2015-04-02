package facts

import spray.json.JsObject
import java.util.concurrent.atomic.AtomicLong
import Fact._

/** Simple representation of a "fact" that provides some information about an entity
 *  FIXME FactVal should be a monad?
 *  FIXME lenses
 *  TODO should score be here?
 *  @param classType The fact's type or class. Technically, should always refer to a class
 *    in the refleco ontology (ie, a fact) but currently leaving as a generic string to
 *    allow for generic url "classes" for now
 *  @param superType The fact's parent or superclass
 *  @param labels Set of surface forms
 *  @param value The fact's direct value
 *  @param children Details and properties of this entity
 *  @param extra Other non-standardized stuff about this fact
 *  @param id The Long id of this entity (for internal use)
 *  @param uuid Universally unique identifier
 *  @param interest Interest rank of this fact
 */
case class Fact(
  classType: String,
  superType: Option[Long],
  labels: Seq[String] = Nil,
  value: FactVal = FactNone,
  children: Seq[Fact] = Nil,
  extra: Option[JsObject] = None,
  id: Long = idSeq.incrementAndGet(),
  uuid: String = java.util.UUID.randomUUID().toString(),
  var score: Double = -1
) {
  
  /** convenience method to filter the direct children of a fact
   */
  def innerFilter(p: Fact => Boolean): Fact =
    Fact(classType, superType, labels, value, children filter p, extra, id, uuid, score)
  
  /** compose two facts. -1 is the magic number to not override interest
   *  FIXME deep concatenation of children + distinctness
   *  FIXME updates with details
   */
  def combine(newf: Fact): Fact = {
    val newValue = if (newf.value != FactNone) newf.value else value
    val newLabels = (labels ++ newf.labels).distinct 
    val newScore = if (newf.score != -1) newf.score else score 
    val newChildren = children ++ newf.children
    
    Fact(classType, superType, newLabels, newValue, newChildren, extra, id, uuid, newScore)
  }
}

/** some extra utilities
 *  TODO unify pimp & type alias?
 */
object Fact {
  type Facts = Seq[Fact]
  
  private val idSeq = new AtomicLong()
  def updateLongSeed(newFrom: Long) = idSeq set newFrom
  
  def createSelfRef(
    labels: Seq[String] = Nil,
    value: FactVal = FactNone,
    children: Seq[Fact] = Nil,
    extra: Option[JsObject] = None,
    uuid: String = java.util.UUID.randomUUID().toString(),
    score: Double = -1
  ) {
    val id = idSeq.incrementAndGet()
    
    Fact(id.toString(), None, labels, value, children, extra, id, uuid, score)
  }
  
//  class FactsPimp(facts: Facts) {
//    def pull(ids: Seq[String]) = ids flatMap (id => facts find (_.uuid == uuid))
//    def pullValues[T: ClassTag](ids: Seq[String]): Seq[T] = pull(ids) map (_.value.get) collect { case s: T => s }
//  }

//  implicit def factsPimp(f: Facts) = new FactsPimp(f)
}
