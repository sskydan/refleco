package utilities

import java.io.File
import akka.actor.ActorRef

trait NetworkClient {
  def setup(host:String, connectionDelay:Int = 0)
  def retrieve(host:String, fileName:String, outputName:File, sender:Option[ActorRef] = None)
  def teardown(host:String) 
}

sealed trait NetworkClientMessage
case class NetworkRetrieve(host: String, fileName: String, outputFile: File, sender: Option[ActorRef] = None) extends NetworkClientMessage
case class NetworkCompleted(outputName: File) extends NetworkClientMessage
case class NetworkFailed(outputName: File) extends NetworkClientMessage
