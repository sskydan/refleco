package ukrm

import facts._
import facts.Fact._
import spray.json.JsObject
import com.typesafe.scalalogging.StrictLogging
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.httpx.encoding.Gzip
import scala.concurrent.Future
import serializers.FactSerializers._
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
import Scalaz.{encode => _, _}
import scala.concurrent.ExecutionContext
import utilities.UConfig
import api.BaseParams

/**
 */
object FactResolver extends UConfig with StrictLogging {
	implicit val system: ActorSystem = ActorSystem()
	import system.dispatcher
	
	type LookupResult = (Seq[String], Long, Double)
	val HOST = config getString "dataServerHost"

  /** function to look up a fact containing as close a match to the provided label set
   *    as possible
   */
  def resolveLabels(labels: String*): Seq[Fact] = ??? 

  /** integrate a fractured fact candidate into the global dataset. Tries to match the
   *    data to an existing fact by label analysis, and integrates the facts. Otherwise, this
   *    will kick off the creation of a new fact to the hierarchy
   */
  def integrateFact(
    className: String,
    superName: String,
    labels: Seq[String] = Nil,
    value: FactVal = FactNone,
    children: Seq[Fact] = Nil,
    extra: Option[JsObject] = None
  ): Fact = {
    
    val normalizedLabels = labels.map(_.toLowerCase.trim).distinct
    val checkExisting = resolveLabels(normalizedLabels:_*)
    
    val classType = resolveLabels(className).headOption map (_.id.toString) getOrElse className 
    val superType = resolveLabels(superName).headOption map (_.id) orElse {
      logger error s"NOSUPER $superName"; None
    }
    
    if (checkExisting.isEmpty && (labels contains className)) {
      Fact.createSelfRef(
        normalizedLabels,
        value,
        children,
        extra
      )
    } else {
      val newInfo = Fact(
        classType,
        superType,
        normalizedLabels,
        value,
        children,
        extra
      )
      
      checkExisting.headOption map (_ combine newInfo) getOrElse newInfo.index()
    }
  }

// --------------------------------------------------------------------------------------------
    
  /** 
   */
  private def update(fact: Fact): Future[Boolean] = {
    logger info s"Updating fact ${fact.labels}"
    val sendpipe = encode(Gzip) ~> sendReceive
    
    sendpipe (
      Post(Uri(HOST) withQuery "cmd"->"overwrite", fact)
    ) map (
      _.status.isSuccess
    )
  }
  
// --------------------------------------------------------------------------------------------

  /** lookup a company name in a specific index
   *  FIXME cleanup types
   *  TODO default akka timeout kicks in if there is no explicit implicit timeout configured here
   */
  def identify(candidate: String, classType: String): ListT[Future, LookupResult] = {
  	logger info s"Disambiguating [$candidate] as [$classType]"
    implicit val timeout = Timeout(1.hour)
    
    val pipeline: HttpRequest => Future[Facts] = (
      sendReceive
      ~> decode(Gzip)
      ~> unmarshal[Facts]
    )
      
    val result = pipeline (
      buildNameRequest(candidate, classType)
    ) map (
      _.toList map parseNameRequest(classType)
    )
    
    ListT(result)
  }
  
  def identifyStrict(candidate: String, classType: String, cutoff: Double = 6.0)
  : ListT[Future, LookupResult] =
    identify(candidate, classType) filter (_._3 >= cutoff)
  
  /** TODO traverseM missing applicative for streams?
   */
  def identifyStrict(candidates: Seq[String], classType: String, cutoff: Double = 6.0)
  : StreamT[Future, LookupResult] = {
    
    val stream = candidates.toStream traverse (
      c => identifyStrict(c, classType, cutoff).run
    ) map (
      _.flatten
    )
    
    StreamT fromStream stream
  }
  
// --------------------------------------------------------------------------------------------
  
  /** FIXME centralize
   */
  def buildNameRequest(query: String, doctype: String): HttpRequest = {
    val params = doctype match {
      case "relation" | "entity" | "attribute" =>
        new BaseParams(
          queryRootFuncs = Some(""),
          queryRootKeys = Some("sform"),
          queryRootVals = Some(query),
          doctypeParam = Some(doctype),
          limParam = Some(50)
        )
      case _ =>
        new BaseParams(
          queryRootFuncs = Some(""),
          queryRootKeys = Some("prettyLabel"),
          queryRootVals = Some(query),
          postFilterFuncs =  Some("field;field;field"),
          postFilterKeys = Some("prettyLabel;interest;id"),
          postFilterVals = Some("NA;NA;NA"),
          //searchParam = Some(query), 
          //postFilter = Some("prettyLabel;interest;id"),
          limParam = Some(20)
        )
    }
        
    Get(Uri(HOST) withQuery params.toParamsMap)
  }
  
  /** TODO
   */
  private def parseNameRequest(doctype: String)(result: Fact): LookupResult = doctype match {
    case "relation" | "entity" | "attribute" =>
      logger info s"--> through [${result.labels}] got [${result.id}] with ${result.score}"
      (result.labels, result.id, result.score)

    case _ =>
      logger info s"--> [${result.labels}] with ${result.labels}"
      (result.labels, result.id, result.score)
  }
  
}