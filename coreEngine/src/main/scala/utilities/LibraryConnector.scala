package utilities

import com.typesafe.scalalogging.StrictLogging
import server.CoreParams
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding.Gzip
import scala.concurrent.Future
import serializers.FactSerializers._
import facts.Fact
import facts.Fact._
import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import scala.xml.NodeSeq
import akka.util.Timeout
import scala.concurrent.duration._
import api.EntityIndex
import scalaz.OptionT
import scalaz.OptionT._
import scalaz._
import scala.concurrent.ExecutionContext

/** Methods for interacting with the library
 */
object LibraryConnector extends CEConfig with StrictLogging {
  implicit val system: ActorSystem = ActorSystem()
  import system.dispatcher

  type NameLookupResult = (String, String, Double)
  
  val TIMEOUT = Duration(20, TimeUnit.SECONDS)
  val HOST = config getString "dataServerHost"
  
  /** TODO fix result value
   */
  def update(facts: Seq[Fact]): Future[Boolean] = {
    val sendpipe = (encode(Gzip) ~> sendReceive)
    logger info s"Updating fact ${facts.size}"
    
    sendpipe {
      Post(Uri(HOST) withQuery "cmd"->"put", facts)
    } map (_ => true)
  }
  
  /** FIXME centralize
   */
  //TODO Not sure what to do here? What does this function do?
  def getQueryParams(query: String, doctype: String): CoreParams = doctype match {
    case "relation" | "entity" | "attribute" =>
      CoreParams(
        queryRootFuncs = Some("="),
        queryRootKeys = Some("ftype"),
        queryRootVals = Some("sform"),
        //querFiltersearchParam = Some(query), 
        doctypeParam = Some(doctype),
        limParam = Some(10)
      )
    case _ =>
      CoreParams(
        //searchParam = Some(query), 
        //postFilter = Some("prettyLabel;interest;id"),
        limParam = Some(10)
      )
  }
  
  private def getNameLookupResult(doctype: String)(r: Fact): NameLookupResult = doctype match {
    case "relation" | "entity" | "attribute" =>
      logger.info(s"--> through [${r.prettyLabel}] got [${r.id}] with ${r.interest}")
      (r.id, r.id, r.interest)

    case _ =>
      logger.info(s"--> [${r.prettyLabel}] with ${r.interest}")
      (r.prettyLabel, r.id, r.interest)
  }
  
  /** lookup a company name in a specific index
   *  FIXME cleanup tyeps
   */
  def check(candidate: String, doctype: String = "10-K"): ListT[Future, NameLookupResult] = {
    // otherwise, the default akka timeout kicks in. TODO make configurable
    implicit val timeout = Timeout(1.hour)
    val pipeline: HttpRequest => Future[Facts] = (
      sendReceive
      ~> decode(Gzip)
      ~> unmarshal[Facts]
    )
      
    val result = pipeline {
      Get(Uri(HOST) withQuery getQueryParams(candidate, doctype).toParamsMap)
    } map {
      logger.info(s"Disambiguating [$candidate] as [$doctype]")
      
      _.toList map getNameLookupResult(doctype)
    }

    ListT(result)
  }
  
  def checkScored(candidate: String, doctype: String = "10-K", cutoff: Double = 6.0)
  : ListT[Future, NameLookupResult] = {
    import scalaz.std.scalaFuture._
    
    check(candidate, doctype) filter (_._3 >= cutoff)
  }
  
  /** TODO traverseM missing applicative for streams?
   *  TODO name conflict??
   */
  def checkScoredAll(candidates: Seq[String], doctype: String = "10-K", cutoff: Double = 6.0)
  : StreamT[Future,NameLookupResult] = {
    import Scalaz._
    
    val stream = candidates.toStream traverse (
      c => checkScored(c, doctype, cutoff).run
    ) map (
      _.flatten
    )
    
    StreamT fromStream stream
  }
    
  /** get a stream of all documents of a specific type from the library server
   *  TODO ability to exclude children
   */
  def streamDocs(doctype: String, lim: Int = 500): Stream[Facts] = {
    val libPipeline: HttpRequest => Future[Facts] = (
      sendReceive
      ~> decode(Gzip)
      ~> unmarshal[Facts]
    )
    def getPage(page: Int): Future[Facts] = libPipeline {
      logger.info(s"Retrieving results $page to ${page + lim} of $doctype")

      val params = CoreParams(
        doctypeParam = Some(doctype), 
        limParam = Some(lim), 
        pageParam = Some(page)
      )
      Get(Uri(HOST) withQuery params.toParamsMap)
    }

    Stream from (0, lim) map getPage map (Await.result(_, TIMEOUT)) takeWhile (!_.isEmpty)
  }
}