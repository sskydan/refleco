package server

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect.ClassTag
import Reception._
import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import facts.Fact
import spray.httpx.SprayJsonSupport._
import spray.json._
import spray.json.DefaultJsonProtocol._
import spray.routing.HttpService
import spray.http.MediaTypes._
import serializers.FactSerializers.factFormat
import serializers.CESerializers.neFormat
import dsl.Reflask
import dsl.ASTCodeGen
import ner.NERecognizer
import stickerboard.Board
import ner.KnownEntityManager

object ServiceActor {
  def props() = Props(new ServiceActor())
}

class ServiceActor extends EngineService {
  // TODO fix this
  override def system = context.system
  
  def actorRefFactory = context
  def receive = runRoute(route)
}

/**
 * Spray routing
 * TODO fix timeout
 * TODO wtf is up with the factserializer import
 */
trait EngineService extends Actor with HttpService with ActorLogging {
  def system:ActorSystem
  protected implicit val excon = system.dispatcher
  implicit val timeout = Timeout(10.hours)

  // Create a new receptionist
  def reception = system.actorOf(Reception.props())
  
  val route =
    path("engine" / "graph") {
      get {
        parameters('search) { question =>
          complete {
            log info s"Browsing relation graph: $question"
            Future{
              Board.browse(question)
            }
          }
        }
      }
    } ~
    path("engine" / "reflask") {
      get {
        parameters('search) { question =>
          complete {
            log info s"Parsing reflask dsl query: $question"
            ASTCodeGen.generate(question)(system)
          }
        }
      }
    } ~
    path("engine" / "ner") {
      get {
        parameters('search) { chunk =>
          complete {
            log info s"Performing NER on chunk: $chunk"
            NERecognizer(chunk)
          }
        }
      }
    } ~
    path("engine" / "dict") {
      get {
        KnownEntityManager.createInverseIndex
        complete("Starting dump of inverse indices")
      }
    }
    path("engine") {
      get {
        parameters(
            'key.?, 
            'search.?,
            'sort.?, 
            'field.?, 
            'type.?, 
            'lim.as[Int].?,
            'page.as[Int].?,
            'interest.?, 
            'analytics.?
          ).as(CoreParams) { sparams =>
          compressResponse() (complete {
            log info s"Http request for search: ${sparams.prettyPrint}"
            (reception ? SearchRequest(sparams)).mapTo[Seq[Fact]]
          })
        } ~
        compressResponse() (complete {
        	log info "Http request for company listing"
          (reception ? ScanRequest).mapTo[Future[Seq[Fact]]]
        })
      }
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
