package ranking.investigators

import facts.Fact
import akka.actor.Actor

/**
 * PIs are supposed to capture a single facet of interestingness of a fact
 */
trait PI extends Actor {
  val receive = normal
  
  def rank(fact:Fact):Double
  val applicableIds:List[String]
  
  def normal:Receive = {
    case fact:Fact => sender ! rank(fact)
  }
}