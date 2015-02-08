package server

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.util.Timeout
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding.Gzip
import facts.Fact
import facts.Fact._
import facts.FactNone
import ranking.Ranker
import ranking.Ranker._
import serializers.FactSerializers._
import utilities.CEConfig
import analytics.Analytic
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import java.io.StringWriter
import java.io.PrintWriter
import CoreParams._

object Reception extends CEConfig {
  sealed trait RequestType
  case class SearchRequest(params:CoreParams) extends RequestType
  case object ScanRequest extends RequestType
  
  val dsHost = config getString "dataServerHost"
  def props() = Props(new Reception())
}

/**
 * Receptionist for sorting requests
 * FIXME separate actorsystem for rankers
 * TODO standardize system/dispatcher import pattern
 *   import system.dispatcher
 * TODO send original sender as reference, or use the .tell method?
 * TODO shorten stack trace error printing code
 */
class Reception extends Actor with CEConfig with ActorLogging {
  import Reception._
  implicit val system = context.system
  implicit val dispatcher = system.dispatcher
  implicit val timeout = Timeout(10.hours)

  val receive = normal
 
  val libPipeline: HttpRequest => Future[Facts] = (
    sendReceive
    ~> decode(Gzip)
    ~> unmarshal[Facts]
  )
  val sendpipe = (encode(Gzip) ~> sendReceive)

  def normal: Receive = {

    case SearchRequest(params) =>
      log.info("Starting request processing")
      val server = sender
      
      // first, make the raw data request
      libPipeline {
      	Get(Uri(dsHost) withQuery params.toParamsMap)
      	
      } onSuccess { case facts => 
          
          // if ranking is enabled
    	    if (params.ranking != None) {
    	      log.info("Starting ranking of {}", params.request)
    	      
    	    	val ranker = system.actorOf(Props[Ranker](new Ranker()))
    	      ranker ! Rank(facts)
      		  context become rankWait(server)
      		  
      		// if analytics are enabled
    	    } else if (!params.analytics.isEmpty) {
    	      
    	      libPipeline {
    	        Get(Uri(dsHost) withQuery (
    	          "key" -> "id",
  	            "search" -> ("analytics::"+facts.head.id), 
  	            "type" -> "analytics"
              ))
    	      } map { 
    	        
    	        case Seq(head, _*) => 
    	          log.info("Using previous analytics")
    	          head
    	        
    	        case Seq() =>
        	      log.info("Calculating analytics of {}", params.request)
        	      
        	      Try(Analytic.generateRatios(params.analytics, facts.head)) match {
        	        case Success(analytics) => 
        	          sendpipe(Post(Uri(dsHost) withQuery ("cmd"->"put"), analytics))
      	        		log.info("Analytics completed")
      	        		analytics
      	        		
        	        case Failure(error) => 
        	          val sw = new StringWriter()
                    error.printStackTrace(new PrintWriter(sw))
                    log.error("ERROR: Could not calculate analytics\n"+sw.toString())
        	          FactNone
        	      }
        	      
    	      } onSuccess { case analytics:Fact => server ! (facts :+ analytics) }
    	      
    	    } else {
    	      log.info("Getting raw data for {}", params.request)
    	      server ! facts
    	    }
      }
      
    case ScanRequest =>
      sender ! libPipeline(Get(dsHost))
  }
  
  def rankWait(original:ActorRef): Receive = {
    case RankingComplete(facts) =>
      original ! facts
      context become normal
  }
}

