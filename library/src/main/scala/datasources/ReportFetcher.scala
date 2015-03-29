package datasources

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.concurrent._
import scala.concurrent.duration._
import scala.io.Source
import com.typesafe.scalalogging.StrictLogging
import Report._
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import extensions.ImplicitConversions._
import utilities.FTPSimpleClient
import utilities.NetworkClient
import utilities.NetworkCompleted
import utilities.Zip
import utilities.LibConfig
import utilities.NetworkFailed

trait XBRLFetcherCommand
case object RetrieveIndex extends XBRLFetcherCommand
case class ParseIndex(loc: File) extends XBRLFetcherCommand
case class RetrieveDoc() extends XBRLFetcherCommand
case object DocRetrievedSuccess extends XBRLFetcherCommand
case object DocRetrievedFailure extends XBRLFetcherCommand
case class UnzipDoc(loc: File) extends XBRLFetcherCommand

/** classes to manage fetching forms from edgar's database
 *  TODO pull out folder settings
 *  TODO refactor the actor structure; indices/files should be separate
 */
object ReportFetcher extends LibConfig {
  // Do you want to retrieve and parse files even if they already exist locally?
  val OVERWRITE = false
  implicit val respTimeout: Timeout = Timeout(12.hours)

  val FULL_INDEX_BASE = "edgar/full-index/"
  val YEARS = "2014/" :: Nil //"2013/" :: "2012/" :: "2011/" :: "2010/" ::
  val QUARTERS = "QTR1/" :: "QTR2/" :: "QTR3/" :: "QTR4/" :: Nil
  val HOST = "ftp.sec.gov"
  val MASTER_INDEX_NAME = "master.gz"
  val XBRL_INDEX_NAME = "xbrl.gz"
  val INDEX_FOLDER = config getString "indexFiles"
  val REPORTS_FOLDER = config getString "xbrlFiles"
  
  /** fetch the specified form types from edgar
   *  TODO pull limit logic up?
   */
  def fetch(limit:Option[Int] = None, form:Form = R10K)(implicit system: ActorSystem) = {
    implicit val ec: ExecutionContext = system.dispatcher

    val workers = generateIndexUrls(form) flatMap { case url =>
      
      val indexWorker = system actorOf iprops(HOST, url, form) 
      Await.result((indexWorker ? RetrieveIndex).mapTo[List[String]], 10.seconds)
      
    } map { case url =>
      
      val reportWorker = system actorOf dprops(HOST, url, form)
      Await.result((reportWorker ? RetrieveDoc).mapTo[XBRLFetcherCommand], 1.minute)
    }
    
    val newReports = workers filter (_ == DocRetrievedSuccess)
    
    newReports.take(limit getOrElse 10).force
  }
    
  def generateIndexUrls(form:Form): Stream[String] =
	  for {
		  y <- YEARS.toStream
		  q <- QUARTERS
		  i = if (R13FHR == form) MASTER_INDEX_NAME else XBRL_INDEX_NAME
	  } yield FULL_INDEX_BASE + y + q + i

  def iprops(host: String, url: String, form: Form) = Props(new XBRLIndexFetcher(host, url, form) with FTPSimpleClient)
  def dprops(host: String, url: String, form: Form) = Props(new XBRLDocFetcher(host, url, form) with FTPSimpleClient)

  def unzippedTarget(zip: File): File = new File(zip getPath() replaceAll (".gz", ".txt") replaceAll (".zip", ""))
}

/** TODO appropriate dispatcher
 *  TODO check if we can/should use edgar's Archives through http
 *  @note stopping an actor stops all its children
 */
abstract class XBRLIndexFetcher(host: String, url: String, form: Form) extends Actor with NetworkClient with StrictLogging {
  import ReportFetcher._

  val CONNECTION_DELAY = 1
  val receive = normal
  if (!new File(INDEX_FOLDER).exists()) Files.createDirectory(Paths.get(INDEX_FOLDER))

  def normal: Receive = {

    case RetrieveIndex =>
      setup(host, CONNECTION_DELAY)
      val downloadName = new File(INDEX_FOLDER + url.replaceAll("/", "-"))

      if (!unzippedTarget(downloadName).exists || OVERWRITE) {
        retrieve(host, url, downloadName, Some(self))
        context become waitFileAndParse(context.sender)
      } else { 
        context.sender ! parseIndex(downloadName)
      }
  }
  
  def waitFileAndParse(origin:ActorRef): Receive = {
    
    case NetworkCompleted(file) =>
      logger.info(s"Index fetched: $file")
      origin ! parseIndex(file)
      teardown(host)
      context become normal
      
    case NetworkFailed(file) =>
      logger.error(s"Error retrieving: $file")
      origin ! List()
      context become normal
  }
  
  /** Parses the index into a map of uris by formtype
   */
  def parseIndex(zipFile: File): List[String] = {
    // get the file (unzip it if it has just been downloaded)
    val file = unzippedTarget(zipFile)
    if (zipFile.exists()) {
      Zip.unzipGzip(zipFile getPath(), file getPath())
      zipFile.delete
    }

    // jump to the report list
    val lines = Source.fromFile(file).getLines() dropWhile (!_.startsWith("CIK"))
    // get the column headers, then get the rows
    val entryColumns: List[String] = lines next() split ('|')
    val entries = lines drop 1 map (line => entryColumns zip (line split ('|'))) map (_.toMap)

    val reportByForm = entries map (entry => entry("Form Type") -> entry("Filename"))
    
    // filter the entries based on known form types
    reportByForm collect {
      case (form, r) if Form(form) == this.form => r
    }
  }
}

/** handle downloading and parsing all forms from edgar
 *  TODO s-1 (and amendment) forms don't need to be nested in folders as the seem
 *    to be duplicated in the root cik folder
 */
abstract class XBRLDocFetcher(host: String, rawUrl: String, form: Form) extends Actor with NetworkClient with StrictLogging {
  import ReportFetcher._

  val receive = normal
  val connectionDelay = 1

  val url = parseUrl
  val cik = url stripPrefix("edgar/data/") takeWhile (_ != '/')
  val LOCAL_FOLDER = REPORTS_FOLDER + cik + "/"
  
  /** standard receive logic
   */
  def normal: Receive = {

    case RetrieveDoc =>
      setup(host, connectionDelay)

      if (!new File(LOCAL_FOLDER).exists()) Files.createDirectories(Paths.get(LOCAL_FOLDER))
      val downloadName = new File(LOCAL_FOLDER + url.replaceAll("/", "-"))
      
      form match {
        // these reports are in (zipped) xbrl format
        case R10K | R10Q  if !unzippedTarget(downloadName).exists || OVERWRITE =>
          retrieve(host, url, downloadName, Some(self))
          context become waitFileAndThen(UnzipDoc, context.sender)
          
        // these reports are other types of filings (text/xml)
        case S1 | R13FHR if !downloadName.exists || OVERWRITE =>
          retrieve(host, url, downloadName, Some(self)) 
          context become waitFileEnd(context.sender)
          
        case _ => shutdown(context.sender)(false)
      }
      
    case (UnzipDoc(zipFile), origin:ActorRef) =>
      val file = unzippedTarget(zipFile)
      Zip.unzipZip(zipFile getPath(), file getPath())

      zipFile.delete
      shutdown(origin)(true)
  }

  /** wait and respond to network call
   */
  def waitFileAndThen(next: File => XBRLFetcherCommand, origin:ActorRef): Receive = {
    
    case NetworkCompleted(file) =>
  	  logger.info(s"Report fetched: ${file.getName()}")
      self ! (next(file), origin)
      context become normal
    case NetworkFailed(file) =>
      logger.error(s"Error retrieving: $file")
      shutdown(origin)(false)
  }

  /** wait and shutdown after network call
   */
  def waitFileEnd(origin:ActorRef):Receive = {
    
    case NetworkCompleted(file) =>
      logger.info(s"Report fetched: ${file.getName()}")
      shutdown(origin)(true)
    case NetworkFailed(file) =>
      logger.error(s"Error retrieving: $file")
      shutdown(origin)(false)
  }

  def shutdown(origin:ActorRef)(success: Boolean = false) = {
  	origin ! (if (success) DocRetrievedSuccess else DocRetrievedFailure)
  	context stop self
  }
  
  /** Method to transform the "filename" for an edgar report type (from the index file) into actual
   *    urls for the files in edgar
   */
  def parseUrl: String = form match {
    case R10K | R10Q =>
      val filename = rawUrl.reverse.takeWhile(_ != '/').reverse
      val fileParent = filename replace("-", "") replace(".txt", "")
      rawUrl replace (filename, fileParent + "/" + filename) replace (".txt", "-xbrl.zip")
        
    case S1 | R13FHR =>
      val filename = rawUrl.reverse.takeWhile(_ != '/').reverse
      val fileParent = filename replace("-", "") replace(".txt", "")
      rawUrl replace (filename, fileParent + "/" + filename)
      
    case _ => throw new Exception(s"Unknown form type $form")
  }
}

