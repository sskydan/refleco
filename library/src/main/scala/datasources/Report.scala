package datasources

import java.io.File
import facts.Fact
import spray.json._
import extensions.ImplicitConversions._
import serializers.FactSerializers._
import utilities.XMLUtil._
import scala.io.Source
import utilities.LibConfig
import datasources.reports.XBRL
import datasources.reports.Company
import datasources.reports.Investments
import scala.util.Try
import scala.util.Failure
import scala.util.Success
import com.typesafe.scalalogging.StrictLogging
import scala.xml.Elem

/** generic file report class - represents all reports in the system as read from a file
 */
sealed abstract class Report(val file: File) {
  import Report._

  val FORMS: List[Form]
  lazy val cik = ""
  lazy val cname = ""
  lazy val date = ""
  lazy val doctype = ""
  // this field can be overriden in children
  lazy val uid = cik+"::"+date

  def factTransform: Fact
  override def toString() = factTransform.toJson.prettyPrint
}

object Report extends LibConfig {
  sealed abstract class Form(val str: String)
  case object R10K extends Form("10-K")
  case object R10Q extends Form("10-Q")
  case object S1 extends Form("S-1")
  case object R13FHR extends Form("13F-HR")
  case object UForm extends Form("???")
  object Form {
    def apply(str: String): Form = List(R10K, R10Q, S1, R13FHR) find (_.str == str) getOrElse UForm
  }

  val CIK_KEY = ""
  val NAME_KEY = ""
  val DOCTYPE_KEY = ""
  val PERIOD_KEY = ""
  val XBRLFOLDER = new File(config getString "xbrlFiles")
  val XBRL_DOCTYPES = Seq(R10K, R10Q)

  /** this function will try to identify the doctype of a file and parse it
   *  @param file The file to be parsed
   */
  def apply(file: File): Option[Report] = file match {

    // files ending in .txt can be either S-1 or 13F-HR forms
    case f if f.getName endsWith ".txt" => TextReport(f)

    // files matching this case are 10-K or 10-Q reports
    case f if f.getName.endsWith(".xml") && !f.getName.contains("_") && !f.getName.startsWith("defnref") =>
      XBRLReport(f)

    case _ => None
  }
}

/** representation of xbrl (xml) reports
 *  FIXME multiple cik/name fields from consolidated companies
 */
abstract class XBRLReport(file: File) extends Report(file) with StrictLogging {
  import XBRLReport._

  /** because these basic fields are calculated on initialization(strictly), and the xml
   *    representation may not be needed again, we will use a local xml variable here to avoid caching
   *  @throws XML exceptions if the xml file is not up to snuff
   */
  override lazy val (cik, cname, date, doctype) = {
    val localXML = openXML(file)
    val cik = (localXML \\ CIK_KEY) map (_.text.trim) reduce (_+","+_)
    val n = (localXML \\ NAME_KEY) map (_.text.trim) reduce (_+","+_)
    val d = (localXML \\ PERIOD_KEY).head
    val t = (localXML \\ DOCTYPE_KEY).head

    (cik, n, d.text.trim, t.text.trim)
  }
}

object XBRLReport {
  import Report._

  val CIK_KEY = "EntityCentralIndexKey"
  val NAME_KEY = "EntityRegistrantName"
  val DOCTYPE_KEY = "DocumentType"
  val PERIOD_KEY = "DocumentPeriodEndDate"

  val TOP_KEY = "xbrl"
  val ALT_TOP_KEY = "xbrli:"+TOP_KEY

  def apply(f: File): Option[XBRL] = Some(XBRL(f))
}

/** representation of plaintext reports
 */
abstract class TextReport(file: File) extends Report(file) {
  import TextReport._

  override lazy val (cik, cname, date, doctype) = {
    val lines = Source.fromFile(file).getLines

    val usefulLines = lines.collect {
      case line if line contains CIK_KEY => CIK_KEY -> line.replace(CIK_KEY, "").trim
      case line if line contains NAME_KEY => NAME_KEY -> line.replace(NAME_KEY, "").trim
      case line if line contains PERIOD_KEY => PERIOD_KEY -> line.replace(PERIOD_KEY, "").trim
      case line if line contains ALT_PERIOD_KEY => PERIOD_KEY -> line.replace(ALT_PERIOD_KEY, "").trim
      case line if line contains DOCTYPE_KEY => DOCTYPE_KEY -> line.replace(DOCTYPE_KEY, "").trim
    }.toMap withDefaultValue ("")

    (usefulLines(CIK_KEY), usefulLines(NAME_KEY), usefulLines(PERIOD_KEY), usefulLines(DOCTYPE_KEY))
  }
}

object TextReport {
  import Report._

  val CIK_KEY = "CENTRAL INDEX KEY:"
  val NAME_KEY = "COMPANY CONFORMED NAME:"
  val DOCTYPE_KEY = "CONFORMED SUBMISSION TYPE:"
  val PERIOD_KEY = "EFFECTIVENESS DATE:"
  val ALT_PERIOD_KEY = "DATE AS OF CHANGE:"

  def apply(f: File): Option[TextReport] = {
    val lines = Source.fromFile(f).getLines
    val doctype = (lines find (_ contains DOCTYPE_KEY) getOrElse "" replace (DOCTYPE_KEY, "")).trim
    Form(doctype) match {
      case S1 => Some(Company(f))
      case R13FHR => Some(Investments(f))
      case _ => None
    }
  }
}
