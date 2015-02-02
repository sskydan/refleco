package utilities

import java.io.File
import java.io.FileOutputStream
import java.net.ConnectException
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPConnectionClosedException
import org.apache.commons.net.ftp.FTPReply
import com.typesafe.config.ConfigFactory
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.routing.FromConfig
import akka.contrib.throttle.Throttler._
import akka.contrib.throttle.TimerBasedThrottler
import scala.compat.Platform
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
import utilities._
import com.typesafe.scalalogging.StrictLogging

/**
 * TODO how does spray do it
 */
class FTPSimpleActor extends Actor with StrictLogging {
  val receive = normal

  def normal: Receive = {

    case NetworkRetrieve(host, fileName, outputFile, origin) =>
      var client: FTPClient = null
      var success = false
      val output = new FileOutputStream(outputFile)

      try {
        client = setupClientEdgar(host)
        success = client.retrieveFile(fileName, output)
        client.logout()
        
      } catch {
        case connectEx: ConnectException =>
          println(s"Connection Failed -- $fileName")
          throw connectEx
        case closed: FTPConnectionClosedException =>
          println(s"Connection Closed -- $fileName")
          throw closed

      } finally {
        output.close()
        if (client != null && client.isConnected()) client.disconnect()

        if (!success) origin match {
          case Some(o) => o ! NetworkFailed(outputFile)
          case _ => throw new ConnectException(s"File retrieval failed -- $fileName")
          
        } else {
          //Note: not simply "sender" since that is the router
          origin map (_ ! NetworkCompleted(outputFile))
          logger.info(s"Downloaded from $host file $fileName")
        }
      }
  }

  // TODO add options other than edgar ones
  def setupClientEdgar(host: String): FTPClient = {
    val ftpClient = new FTPClient()
    ftpClient.connect(host)

    // Check connection
    if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
      ftpClient.disconnect()
      throw new ConnectException("FTP server refused connection")
    }

    // Set connection options
    ftpClient.login("anonymous", "snowball3_@hotmail.com")
    ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
    ftpClient.enterLocalPassiveMode()

    ftpClient
  }
}

/**
 * TODO try one client per host
 * Note the special actor system is not strictly needed anymore since the throttler was
 *   added, but leave it in for now as reference (and it's still the limiting factor if a
 *   connection delay is not provided)
 * FIXME automatically call setupConnectionEnv in ftpRetrieve if it was not
 *   called externally?
 */
trait FTPSimpleClient extends NetworkClient {
  /**
   * Filename is a URI relative to host
   * TODO handle lookup failure
   */
  override def retrieve(host: String, fileName: String, outputName: File, sender: Option[ActorRef] = None) = {
    FTPSimpleClient.hostToRouterMap(host) ! NetworkRetrieve(host, fileName, outputName, sender)
  }

  override def teardown(host: String) = FTPSimpleClient.removeHost(host)

  def props() = Props[FTPSimpleActor](new FTPSimpleActor())

  /**
   * This method should be idempotent
   * @param host String url of the host
   * @param connectionDelay (optional) parameter is in seconds
   * TODO remove fallback AS
   * TODO cleanup
   */
  override def setup(host: String, connectionDelay: Int = 0) =
    if (!FTPSimpleClient.hostToRouterMap.contains(host)) {

      val config = ConfigFactory.load()
      val system = ActorSystem("ftpSystem", config.getConfig("cappedSystem").withFallback(config))
      val router = system.actorOf(Props[FTPSimpleActor].withRouter(FromConfig()), "smRouter")

      if (connectionDelay > 0) {
        val throttler = FTPSimpleClient.GENERIC_AS.actorOf(Props(
            new TimerBasedThrottler(1 msgsPer connectionDelay.seconds)))

        throttler ! SetTarget(Some(router))
        FTPSimpleClient.addHost(host, throttler)

      } else FTPSimpleClient.addHost(host, router)
    }
}

object FTPSimpleClient {
  var hostToRouterMap = Map[String, ActorRef]()
  lazy val GENERIC_AS = ActorSystem("GenericSystem")

  def addHost(host: String, router: ActorRef) = hostToRouterMap = hostToRouterMap + ((host, router))
  def removeHost(host: String) = hostToRouterMap = hostToRouterMap - host

}