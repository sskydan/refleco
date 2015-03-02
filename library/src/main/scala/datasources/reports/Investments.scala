package datasources.reports

import java.io.File
import datasources.Report
import datasources.Report._
import scala.io.Source
import com.typesafe.scalalogging.StrictLogging
import scala.xml.XML
import scala.xml.Elem
import scala.util.Try
import scala.xml.Node
import facts.Fact
import datasources.TextReport
import facts.FactNone
import facts.Holding
import facts.FactCol
import facts.Group

/** class to represent investment or holdings information
 *  TODO flatten instead of reduce strings problem in parseInfoTable
 *  TODO type inference issues in parseInfoEntry
 */
case class Investments(override val file:File) extends TextReport(file) with StrictLogging {
  val FORMS = R13FHR :: Nil
  val TABLE_START_KEY = "informationTable"
  val TABLE_END_KEY = "</XML>"
  
  def factTransform = {
    logger.info("Parsing 13F-HR form: "+file)
    val inv = parseInfoTable map (Group(_))
    
    Fact(
      uid,
      "investments",
      FactCol(inv.toList),
      Seq(cname),
      0
    )
  }
  
  def parseInfoTable = {
    val infoTable = Source.fromFile(file).getLines 
      .dropWhile (!_.contains(TABLE_START_KEY)) 
      .takeWhile (!_.startsWith(TABLE_END_KEY))
       
    val xmlTable = XML.loadString(infoTable reduce (_+_))
     
    xmlTable \\ "infoTable" map parseInfoEntry
  }
  
  def parseInfoEntry(entry:Node) = {
    val issuer = (entry \ "nameOfIssuer").text
    val tclass = (entry \ "titleOfClass").text
    val cusip = Try((entry \ "cusip").text.toDouble) getOrElse 0:Double
    val value = Try(BigDecimal((entry \ "value").text)) getOrElse 0:BigDecimal
    val spAmount = Try(BigDecimal((entry \\ "sshPrnamt").text)) getOrElse 0:BigDecimal
    val spType = (entry \\ "sshPrnamtType").text
    val discretion = (entry \ "investmentDiscretion").text
    val sole = Try((entry \\ "Sole").text.toDouble) getOrElse 0:Double
    val shared = Try((entry \\ "Shared").text.toDouble) getOrElse 0:Double
    val none = Try((entry \\ "None").text.toDouble) getOrElse 0:Double
    
    Holding(issuer, cusip, tclass, value, (spType, spAmount), discretion, sole, shared, none)
  }
}