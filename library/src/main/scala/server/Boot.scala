package server

import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.actor.Props
import spray.servlet.WebBoot
import java.io.File
import com.typesafe.config.Config

/**
 * This class is instantiated by the servlet initializer
 */
class Boot extends WebBoot {
  val system = ActorSystem("DataServerSystem")

  // the service actor replies to incoming HttpRequests
  val serviceActor = system.actorOf(ServiceActor.props())
}