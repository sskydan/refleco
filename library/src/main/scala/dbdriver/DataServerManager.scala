package dbdriver

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import facts.Fact
import serializers.FactSerializers.factFormat
import spray.json.DefaultJsonProtocol
import spray.json.JsValue
import spray.json.RootJsonFormat
import spray.json.pimpAny
import server.LibSearchRequest
import server.LibParams
import api.URIParamsAdapter
import api.SearchRequest
import api.EntityIndex

/** Interface for data-server communications
 */
trait DataServerManager {
  def initDS()
  def updateDS(facts: Iterator[Fact]): Future[Boolean]
  def uploadDS(payload: Iterator[Fact]): Future[Boolean]
  def shutdownDS()
  def lookupDS(params: LibSearchRequest): Future[DataServerReply]
}

trait BufferedDSManager extends DataServerManager {
  val IDEAL_REQUEST_SIZE = 50

  /** Buffered upload method that batches the payload and minimizes upload operations
   *  TODO why on earth is GroupedIterator not an Iterator??
   */
  abstract override def uploadDS(payload: Iterator[Fact]): Future[Boolean] = {
    val parts = payload grouped(IDEAL_REQUEST_SIZE) map(x => super.uploadDS(x.toIterator))
    Future.reduce(parts)(_ && _)
  }
}

/** An interface for different data server types. All their responses should be
 *   collectable into Facts.
 *  TODO instead of Fact, expect Fact-likes (implicit conversion)
 *  TODO implicit chaining?
 */
trait DataServerReply {
  def toFacts: Seq[Fact]
}
object DataServerReply extends DefaultJsonProtocol {

  implicit def jsonFormat: RootJsonFormat[DataServerReply] = new RootJsonFormat[DataServerReply] {
    def write(dsr: DataServerReply) = dsr.toFacts.toJson
    def read(json: JsValue) = throw new UnsupportedOperationException("DataServerReply is not meant to be read")
  }
}

