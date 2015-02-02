package datasources.reports

import java.io.File
import spray.json._
import utilities._
import utilities.XMLUtil._
import extensions.ImplicitConversions._
import serializers.FactSerializers._
import datasources.Report
import datasources.Report._
import datasources.reports._
import datasources.XBRLReport

/** XBRL representation
 *  This is a memory-heavy object because of the size of xbrl reports and the multiple formats
 *    (xml, json, fact) that this object generates. Since the format conversions are quite expensive, 
 *    we will keep them around after they have been generated once. 
 *  @note need to be careful when dealing with XBRL objects
 *  FIXME would be nice to have a pattern for expensive object management
 *  FIXME check uid generation uniqueness (amendment docs + improper date fields + consolidated companies)
 *  TODO cleanup "details" Facts
 *  TODO Sometimes the root is TOP, sometimes xbrli:TOP. Make sure thats it
 *  TODO multiple pretty label support
 *  TODO deal with top-level strings in raw json, these are namespaces and such
 *  TODO deal with multiple DocumentPeriodEndDates in a single xml
 */
case class XBRL(override val file: File) extends XBRLReport(file) with XBRLToFact with XBRLToJson {
  val FORMS = R10K :: R10Q :: Nil

  /** xml is used in the uid and json generations
   */
  lazy val xml = openXML(file)

  /** xml file containing alternate labels
   */
  def labelXML = {
    val labelFile = new File(file.getParentFile()+"/"+file.getName().replace(".xml", "_lab.xml"))
    if (labelFile exists()) openXML(labelFile)
    else <empty/>
  }
  
  /** Shouldnt be used other than in factTransform. Used multiple times in one fact transform.
   */
  protected lazy val jsonTransform = xbrlToJson
}
