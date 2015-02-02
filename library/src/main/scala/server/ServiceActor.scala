package server

import scala.concurrent._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.Props
import spray.http.MediaTypes._
import spray.routing._
import spray.http._
import spray.httpx.SprayJsonSupport._
import dbdriver.BufferedDSManager
import dbdriver.DataServerManager
import dbdriver.elasticsearch.ESManager
import facts.Fact
import serializers.FactSerializers._
import datasources.ReportFetcher
import datasources.Report._
import rr.FactRelations
import datasources.ReportManager
import api.URIParamsAdapter._
import api.URIParamsAdapter
import api.SearchRequest
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.elasticsearch.spark._
import spray.json.JsValue
import api.EntityIndex

object ServiceActor {
  def props() = Props(new ServiceActor())
}

class ServiceActor extends ESManager with BufferedDSManager with DatabaseService {
  //TODO why not with val?
  override def system = context.system

  def actorRefFactory = context
  def receive = runRoute(route)
}

/** TODO limit fetch & upload to one-at-a-time
 *  TODO company name search should use parent-child relationship
 *  TODO correct pattern?
 *    see http://stackoverflow.com/questions/20168677/violation-of-the-left-identity-law-for-future-monads-in-scalaz
 *  TODO use Forms instead of strings
 */
trait DatabaseService extends Actor with HttpService with DataServerManager with ActorLogging {
  def system:ActorSystem
  protected implicit val excon = system.dispatcher

  //FIXME what a hack...
  system registerOnTermination shutdownDS
  
  val route =
    path("finbase") {
      post {
        parameters('cmd, 'lim.as[Int].?, 'type.?) { (cmd, lim, doctype) => cmd match {
            
          case "node" =>
            log.info("Http request for new node")
            initDS
            complete("New server node started\n")
            
          case "fetch" =>
            log.info(s"Http request for fetching $lim new $doctype reports")
            Future(ReportFetcher.fetch(lim, Form(doctype getOrElse "10-K"))(system))
            complete("XBRL retrieval started\n")
            
          case "load" =>
            log.info(s"Http request for loading $lim $doctype files")
            val form = Form(doctype getOrElse "10-K")
            
            val existingIds = lookupDS(LibParams(fieldParam=Some("_id"), doctypeParam=doctype, limParam=Some(1000000)).toRequest) map {
              _.toFacts map(_.id)
            } recover {
              case _ => List()
            }
            
            existingIds map (ReportManager.parse(form, lim, _)) foreach uploadDS
            
            complete("Loading started\n")
            
          case "put" =>
            decompressRequest() { entity(as[Seq[Fact]]) { case docs =>
              log.info(s"Http request for putting to db: ${docs.size}")

              //FIXME difference between updateDS and uploadDS
              updateDS(docs.iterator)
            	complete("Update started\n")
            }}
            
          case "spark" => doctype match {
            case Some("dict") =>
              val conf = new SparkConf().setAppName("lib").setMaster("local[*]")
              conf.set("es.index.auto.create", "true")
              val spark = new SparkContext(conf)
              
              // load relations index
              val relindex = spark.objectFile[(String,String)]("refdata/relindex") map { 
                case (l,r) => Map("sform" -> l, "uri" -> r) 
              }
              relindex.saveToEs("dicts/relation")
              
              // load attribute index
              val attindex = spark.objectFile[(String,String)]("refdata/attindex") map { 
                case (l,r) => Map("sform" -> l, "uri" -> r) 
              }
              attindex.saveToEs("dicts/attribute")
              
              // load entity index
              val entindex = spark.objectFile[(String,String)]("refdata/entindex") map { 
                case (l,r) => Map("sform" -> l, "uri" -> r) 
              }
              entindex.saveToEs("dicts/entity")
              
              complete("Loading dicts")

            case _ =>
              log.info(s"Dumping 10K reports to spark file")
              val conf = new SparkConf().setAppName("lib").setMaster("local[*]")
              conf.set("es.index.auto.create", "true")
              val sc = new SparkContext(conf)         

              val rdd = sc.esRDD("finbase/10-K") 
              rdd.saveAsObjectFile("refdata/10-K")
              
              complete("10-K Reports dumped as spark file\n")
          }
        }}
      } ~
      get {
    	  parameters('search, 'type ! "hierarchy") { srch =>
    	    complete (FactRelations.lookupFact(srch))
    	  } ~
        // Search reports by company name
        parameters(
            'key.?, 
            'search.?, 
            'sort.?, 
            'field.?, 
            'type.?, 
            'lim.as[Int].?,
            'page.as[Int].?
          ).as(LibParams) { sparams =>
          log.info(s"Http request for search: ${sparams.prettyPrint}")
          
          compressResponse() (complete {
            lookupDS(sparams.toRequest)
          })
        } ~
        // Get list of all company names
        // TODO cache
        compressResponse() (complete {
      	  log.info("Http request for company listing")
          lookupDS(LibParams(doctypeParam=Some("company"),limParam=Some(1000)).toRequest)
        })
      }
    } ~
    // ES-Head forwarding workaround
    pathPrefix("dist") {
      getFromResourceDirectory("elasticsearch-head-master/dist")
    } ~
    path("eshead") {
      getFromResource("elasticsearch-head-master/index.html")
    } ~
    path("") {
      get {
        respondWithMediaType(`text/html`) {
          complete {
            <html>
              <body>
                <h1>Nothing to see here currently!</h1>
              </body>
            </html>
          }
        }
      }
    }
}
