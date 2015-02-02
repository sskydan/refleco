package datasources.reports

import scala.reflect.ClassTag
import facts._
import spray.json._
import extensions.ImplicitConversions._
import utilities.JsonUtil._
import utilities.XLinkUtil._
import utilities.XMLUtil._
import org.elasticsearch.common.joda.time.DateTime
import com.typesafe.scalalogging.StrictLogging
import datasources.XBRLReport._

/**
 * TODO this might not be necessary anymore since we implemented JSON to Fact
 * TODO some more testing on the default values
 * TODO footnote parsing (it contains html tags which screw with the json-format)
 * FIXME parse out encoded html in some variables (&gt; ...)
 */
trait XBRLToJson extends StrictLogging { self:XBRL =>
  
  protected def xbrlToJson:JsValue = {
    logger.info("XBRL parsing JSON from XML: "+file.getName())
    normalizeJSON( toJson(XMLtoJSON(xml)) )
  }
  
  /**
   *  - Normalize all top-level children to be JSON Objects (otherwise some are Arrays/Strings)
   *  - Normalizes root-level element name
   *  - Sets all default values to the correct types
   *  @param dirtyJson The xbrl's raw json
   *  @return The xbrl's clean json
   */
  protected def normalizeJSON(dirtyJson:JsValue) = {

    val root = dirtyJson.asJsObject.getFields(TOP_KEY, ALT_TOP_KEY).head.asJsObject
    val ffields = root.fields.map((formatTopLevelElem _).tupled)
    val formatted = JsObject(TOP_KEY -> JsObject(ffields))
    
    setDefaultElems(formatted)
  }
  
  private def formatTopLevelElem(key:String, elem:JsValue): (String,JsValue) = {
    // Make arrays into objects so that we can keep a "top"-level reference to their pretty name
    val newElem = elem match {
      case array:JsArray => Map("contentArray" -> array)
      case JsObject(fields) => fields
      case _ => Map("reflechoVal" -> elem)
    }

    key -> JsObject(newElem)
  }
  
  private def setDefaultElems(js:JsValue):JsValue = js match {
    case JsObject(fields) => JsObject(fields.map((checkDefault _).tupled))
    case JsArray(list) => JsArray(list map setDefaultElems)
    case _ => js
  }
  
  private def checkDefault(key:String, value:JsValue): (String,JsValue) = value match {
    case unchecked if !VALUE_DEFAULTS.contains(key) => key -> setDefaultElems(unchecked)
    case suspicious => key -> VALUE_DEFAULTS(key)(suspicious)
  }
  
  val VALUE_DEFAULTS:Map[String, JsValue => JsValue] = Map( 
    "decimals" -> zeroDefault, 
    "link:footnoteLink" -> blankOutObject,
    "LineOfCreditFacilityExpirationDate" -> zeroInnerDefault, 
    "LossContingencyPartiesJointlyAndSeverallyLiableInLitigation" -> zeroInnerDefault,
    "DerivativeLowerRemainingMaturityRange" -> zeroInnerDefault,
    "DebtInstrumentMaturityDateRangeEnd" -> zeroInnerDefault,
    "DerivativeHigherRemainingMaturityRange" -> zeroInnerDefault
  )
  
  private def blankOutObject(jsVal:JsValue) = JsObject("removed"->JsObject("removed"->JsString("removed during parsing")))
  
  private def zeroDefault(jsVal:JsValue) = jsVal match {
    case n:JsNumber => n
    case _ => JsNumber(0)
  }
  
  private def zeroInnerDefault(jsVal:JsValue):JsValue = jsVal match {
    case JsObject(fields) =>
      val checked = fields.map{ case (k,v) =>
        if (k=="content") k -> zeroDefault(v)
        else k -> zeroInnerDefault(v)
      }
      new JsObject(checked)
    case JsArray(l) => JsArray(l map zeroInnerDefault)
    case _ => jsVal
  }
  
}