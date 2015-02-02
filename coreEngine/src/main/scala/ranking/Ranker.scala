package ranking

import akka.actor.Actor
import akka.pattern.ask
import akka.actor.Props
import akka.util.Timeout
import spray.client.pipelining._
import spray.httpx.SprayJsonSupport._
import scala.concurrent.duration._
import facts.Fact
import Ranker._
import ranking.investigators.HistoricalVariancePI
import ranking.investigators.PI
import utilities.CEConfig
import ranking.investigators.HistoricalVariancePI
import scala.concurrent.Future


/** Currently only ranks the first fact in a response
 */
class Ranker extends Actor with CEConfig {
  implicit val system = context.system
  implicit val dispatcher = system.dispatcher
  implicit val timeout = Timeout(10.hours)
  val receive = normal
 
  def normal: Receive = {

    case Rank(facts) =>
      // FIXME WTF????
      val original = sender
      val fact = facts.head
      val piActors = Ranker.investigators map system.actorOf

      val allRanks = piActors map (actor => (actor ? fact).mapTo[Double])
      
      Future.sequence(allRanks) map { ranks =>
        fact.interest = ranks.max
        original ! RankingComplete(facts) 
      }
      
  }
}

object Ranker {
  case class Rank(facts:Seq[Fact])
  case class RankingComplete(facts:Seq[Fact])
  
  val investigators = List(
    Props[HistoricalVariancePI](new HistoricalVariancePI())
  )
}
