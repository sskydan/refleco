package datasources.reports

import scala.util.Failure
import scala.util.Success
import scala.util.Try
import org.joda.time.DateTime
import extensions.ImplicitConversions._
import facts._
import spray.json._
import utilities.JsonUtil._
import utilities.XLinkUtil._
import utilities.XMLUtil._
import datasources.XBRLReport._
import scala.util.control.NonFatal
import com.typesafe.scalalogging.StrictLogging
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import scala.collection.JavaConversions._

/** Transformers for XBRL data
 *  TODO more robust error-catching
 */
trait XBRLToFact extends StrictLogging { self: XBRL =>

  /** main method for initiating fact conversion
   */
  def factTransform: Fact = {
    logger info s"Turning xbrl into fact $uid"
    val defaultFact = Fact(uid, "error", FactNone, "error")

    val elems = jsonTransform.\\[JsObject](TOP_KEY).fields
    val (metaElems, dataElems) = elems partition {
      case (k, v) =>
        k.startsWith("xbrli") ||
          k == "reflechoVal" ||
          k.startsWith("xmlns") ||
          k.startsWith("link") ||
          k.startsWith("dei") ||
          k.startsWith("context") ||
          k.startsWith("unit")
    }
    val processedElems = Try(dataElems map ((elemToFact _).tupled))

    processedElems match {
      case Success(data) =>
        Fact(
          uid, //uid
          doctype, //type
          FactString(date), //val
          cname, //prettyLabel   
          0,
          data,
          None //Some(JsObject(metaElems)))
          )
      case Failure(ex) =>
        ex.printStackTrace
        defaultFact
    }
  }

  private def elemToFact(key: String, elem: JsValue): Fact = {
    Fact(key, "xbrl", parseEntry(key, elem), resolveLabel(key), 0, parseEntryChildren(key, elem))
  }
    

  /** Remember, all top-level entries in the normalized json are JsObjects
   */
  private def parseEntry(key: String, elem: JsValue): FactVal = elem match {
    case JsObject(fields) if fields contains "contextRef" => entryToPeriodic(elem, key)
    case _ => entryToFactVal(elem, key)
  }

  private def parseEntryChildren(key: String, elem: JsValue): List[Fact] =
    if (key contains "TextBlock") unstructuredToFactChildren(key, elem)
    else Nil

  private def entryToFactVal(elem: JsValue, key: String): FactVal =
    if (key contains "TextBlock") unstructuredToFactVal(key, elem)
    else structuredToFactVal(elem)

  private def unstructuredToFactVal(key: String, elem: JsValue): FactVal = getDummyFactVal("stephan is an error")

  private def unstructuredToFactChildren(key: String, elem: JsValue): List[Fact] = {
    def groupPrefix[T](xs: List[T], p: T => Boolean): List[List[T]] = xs match {
      case List()         => List()
      case h :: t if p(h) => groupPrefix(t, p)
      case h :: t =>
        val (y, z) = t span (!p(_))
        (h :: y) :: groupPrefix(z, p)
    }

    def parseHTML(htmlString: String): List[String] = {
      val doc = Jsoup.parse(htmlString)
      doc.select("table").remove();
      val htmlText = doc.body.select("*").iterator().map(_.ownText.trim).filter(_.length > 0)
      groupPrefix(htmlText, (x: String) => x == "\u00a0").map(_.mkString(" "))
    }

    elem match {
      case JsObject(fields) => fields.get("content") match {
        case Some(JsString(unstructured)) => {
          parseHTML(unstructured)
          .filter(_.matches(".*\\."))
          .map(s => Fact(key, "xbrl", FactString(s)))
        }
        case other => List()
      }
    }
  }  

  private def structuredToFactVal(elem: JsValue): FactVal = elem match {
    // Match periodic facts => FactPeriod
    case JsObject(fields) if fields contains "contentArray" => fields("contentArray") match {
      case JsArray(items) => FactCol(items.toList map (i => entryToPeriodic(i)))
      case other          => getDummyFactVal(other)
    }

    // Match monetary facts => FactMoney
    case obj @ JsObject(fields) if fields contains "decimals" => obj.getFields("content", "decimals", "unitRef") match {
      case Seq(JsNumber(con), JsNumber(dec), JsString(uref)) =>
        val doublemoney =
          if (dec != 0) con.toInt / (10 * Math.abs(dec.toInt))
          else con.toInt

        FactMoney(doublemoney, uref)

      case other => getDummyFactVal(other)
    }

    // Match facts which are zeroed => FactMoney
    case obj: JsObject => obj.getFields("xsi:nil", "unitRef") match {
      case Seq(_: JsBoolean, JsString(uref)) => FactMoney(0, uref)

      case other                             => getDummyFactVal(other)
    }
  }

  private def entryToPeriodic(elem: JsValue, key: String = ""): Period = elem \\ "contextRef" match {
    case JsString(context) =>
      val (start, end) = resolveContext(context)
      Period(start, end, entryToFactVal(elem, key))

    case _ => throw new Exception("Periodic fact value did not have a context")
  }

  /** Resolves the "context" keys in the xbrl
   *  This function should not throw exceptions
   */
  private def resolveContext(key: String): (DateTime, DateTime) = {
    val nss = "" :: "xbrli:" :: Nil
    val defaultContext = new DateTime("1001-01-01") -> new DateTime("1001-01-01")

    def checkDates(ns: String) =
      try {
        val contextName = ns + "context"
        val periodName = ns + "period"
        val instantName = ns + "instant"
        val startName = ns + "startDate"
        val endName = ns + "endDate"

        val context = jsonTransform.\\[JsObject](contextName) fields ("contentArray")
        val date = context.childrenWith("id", JsString(key)).head
        val period = date.\\[JsObject](periodName)

        period.getFields(instantName, startName, endName) match {
          case Seq(JsString(inst))                 => Success(new DateTime(inst) -> new DateTime(inst))
          case Seq(JsString(start), JsString(end)) => Success(new DateTime(start) -> new DateTime(end))
        }
      } catch { case NonFatal(any) => Failure(any) }

    (nss map checkDates collect { case Success(context) => context }).headOption getOrElse defaultContext
  }

  /**
   * prettyLabel represents the user-facing name of the element
   */
  private def resolveLabel(key: String) = {
    val prettyLabel = resolveResource(key, labelXML)
    if (!prettyLabel.isDefined) logger debug s"Pretty label not found for $key in ${file.getName()}"

    prettyLabel getOrElse ""
  }

  /**
   * TODO better error-reporting. lots of problems with ArrayBuffer?
   */
  private def getDummyFactVal(errored: Any) = {
    logger debug s"Unknown object fact type\n${errored.toString}"
    FactMoney(0, "???")
  }
}
