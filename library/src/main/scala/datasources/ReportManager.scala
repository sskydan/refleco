package datasources

import datasources.Report._
import datasources.reports._
import java.io.File
import scala.io.Source
import utilities.FSUtil
import utilities.LibConfig
import dbdriver.DataServerManager
import java.io.FileWriter
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.scalalogging.StrictLogging
import extensions.Extensions._
import facts.Fact
import scala.util.Try
import scala.util.control.NonFatal

/** TODO exceptions
 */
object ReportManager extends LibConfig with StrictLogging {
  val REPORT_INDEX_FILE = new File(config getString "reportIndexFile")
  if (!REPORT_INDEX_FILE.exists()) REPORT_INDEX_FILE.createNewFile()
  
  // the index of all reports on the filesystem. keeps track of the type & all other details of the reports
  val index = updateIndexFromFS
  def indexLookup(doctype: Report.Form) = index filter (_.doctype == doctype.str)
  
  /** generate the fact reports which are candidates for uploading
   */
  def parse(form:Form = R10K, lim:Option[Int] = None, except:Seq[String] = Nil): Iterator[Fact] = try {
    logger.info(s"Total reports on filesystem: ${index.size}")
    logger.info(s"Total reports of $form on filesystem: ${indexLookup(form).size}")
    logger.info(s"Exception (already loaded) reports size: ${except.size}")

    // intersect uids of the reports index with the exception list
    val reports = indexLookup(form) filter (doc => !except.contains(doc.uid)) map (_.toReport)
    logger.info(s"New available matching reports size: ${reports.size}")
    
    val batch = reports.take(lim getOrElse reports.size).force
    
    // XBRL doctypes are computationally intensive to process, so do them in parallel
    if (Report.XBRL_DOCTYPES contains form) generateFactsPar(batch)
    else batch.toIterator map (_.factTransform)
    
  } catch {
    case NonFatal(any) => 
      any.printStackTrace()
      Iterator.empty
  }
  
  /** Transform XBRL instances into their fact forms in parallel (because the transformation is expensive,
   *   and facts are memory intensive)
   * TODO Still open question as to how the multicore transforms will impact disk io and caching performance.
   *   Might be worth minimizing the thread count
   */
  def generateFactsPar(stream:Stream[Report]) = stream.toIterator mapp (_.factTransform) 
  
  /** synchronize the reports present on the local filesystem with the index. the index is a
   *    single file containing the (serialized) known report summaries
   */
  def updateIndexFromFS:Stream[ReportSummary] = {
    logger.info("Updating file index from FS")
    // first, read the current index for known files
    val index = Source.fromFile(REPORT_INDEX_FILE).getLines().toSet[String] map (ReportSummary(_))
    val indexPaths = index map (_.path)
    
    // then, of the stream of all FS reports, take only those not on the index list
    val newFiles = FSUtil.getFileTree(XBRLFOLDER) filter ( f =>
      !indexPaths.contains(f.getPath())
    )
    
    // turn these new reports into ReportSummarys
    val newReports = newFiles.toList flatMap (Report(_)) flatMap (ReportSummary(_))
    
    // append the new entries to the file. lazy collections won't be evaluated properly here
  	val fw = new FileWriter(REPORT_INDEX_FILE, true)
  	try {
  		newReports map (_.toString()) map fw.write
  	} finally fw.close() 
    
    (index ++ newReports).toStream
  }
}

/** short class representing the serialized/packed represntation of reports
 */
case class ReportSummary(doctype:String, uid:String, date:String, cik:String, cname:String, path:String) {
  def toReport:Report = Report(new File(path)) getOrElse (throw new Exception("Unknown Report type from ReportSummary"))
  override def toString() = Seq(doctype, uid, date, cik, cname, path).reduce(_+" ~ "+_) +"\n"
}

object ReportSummary {
  def apply(report:Report):Option[ReportSummary] = 
    Try(ReportSummary(report.doctype, report.uid, report.date, report.cik, report.cname, report.file.getPath())).toOption
    
  def apply(str:String):ReportSummary = str.split(" ~ ").toSeq match {
    case Seq(dtype, uid, date, cik, cname, path) => ReportSummary(dtype, uid, date, cik, cname, path)
    case _ => throw new Exception("Couldn't parse the ReportSummary "+str)
  }
}