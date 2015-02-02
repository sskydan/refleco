package server

import akka.actor.{Props, ActorSystem}
import spray.servlet.WebBoot
import com.typesafe.config.ConfigFactory
import com.typesafe.config.Config
import java.io.File

/**
 * This class is instantiated by the servlet initializer
 */
class Boot extends WebBoot {
  val system = ActorSystem("CoreEngine")
  
  // the service actor replies to incoming HttpRequests
  val serviceActor = system.actorOf(ServiceActor.props())
}