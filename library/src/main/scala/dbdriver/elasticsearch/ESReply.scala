package dbdriver.elasticsearch

import scala.collection.Seq
import spray.json.JsArray
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import dbdriver.DataServerReply
import facts.Fact
import facts.FactNone
import utilities.JsonUtil
import serializers.FactSerializers._
import utilities.JsonUtil._
import scala.util.Try
import scala.util.{Success, Failure}
import spray.json.JsNumber
import spray.json.JsNull

/** Wrapper class for elasticsearch server responses
 * Implements filtering features since ES does not currently support search/filtering on child docs
 * FIXME properly structure the anonymous facts generated from es-data responses
 * TODO filters
 * FIXME class is a mess
 */
case class ESReply(raw: JsValue, childFilter: List[String] = Nil) extends DataServerReply {
  // this is the json array that represents the actual results returned
  private val results = raw.\\[JsArray]("hits").elements
  
  override def toFacts = Try(raw.\\[JsString]("_index").value) match {
    case Success("finbase") => 
      results map parseFinbaseResult
    case Success("dicts") =>
      results map parseIndexResult
    case Failure(_) if results == Nil =>
      Nil
    case _ =>
      throw new Exception("Reply from an unknown index")
  }
  
  /** Does not support post-reply filtering
   */
  private def parseIndexResult(entry: JsValue) =
    Fact(
      entry.\\[JsString]("uri").value,
      "refleco:result",
      FactNone,
      entry.\\[JsArray]("sform").elements map (_.toString),
      entry.\\[JsNumber]("_score").value.toDouble
    )
  
  /** only one of "fields" or "_source" may be in the response
   *  FIXME interest/rank extraction in es-provided responses
   *  FIXME smarter childFiltering
   */
  private def parseFinbaseResult(entry: JsValue) = (Try(entry \\ "_source"), Try(entry \\ "fields")) match {
    
    // This case is for whole, direct fact responses
    case (Success(doc: JsObject), _) if (doc \\~ "children") != Set() => 
      val fact = doc.convertTo[Fact]
        
      val filterPaths = childFilter map (filter => filter splitAt (filter indexOf "=")) // replaceFirst ("children.", ""))
      
      if (filterPaths.isEmpty) fact
      else
        fact innerFilter (f =>
          filterPaths exists { case (path, value) =>
            
            if (path startsWith "children.prettylabel") 
              f.prettyLabel map (_.toLowerCase) contains value.toLowerCase
              
            else if (path startsWith "children.children.value")
              f.children exists (
                _.value.toString.toLowerCase contains value.toLowerCase
              )
            
            else false
          }
        )
    
    // as above, but case when children was ignored. dummy fields need to be added for the Fact
    //   conversion to work properly
    case (Success(JsObject(fields)), _) => 
      JsObject(
        fields + ("children" -> JsArray())
      ).convertTo[Fact]
      
    // This case is for es-provided (meta)data responses
    case (_, Success(JsObject(fields))) =>
      
      val id = entry.\\[JsString]("_id").value
      
      val esScore = entry.\\[JsNumber]("_score").value.toDouble
      val reflechoScore =
        entry.\\~[JsNumber]("interest").headOption.map(_.value.toDouble)
      
      val prettyLabel = (fields get "prettyLabel") orElse (fields get "uri") collect {
        case JsArray(e) => e.toSeq map (_.toString)
      } getOrElse Nil
      
      val cleanFields = JsObject(fields - ("prettyLabel", "interest"))
      val filteredFields = cleanFields filterAll childFilter
        
      Fact(
        id,
        "refleco:result",
        FactNone,
        prettyLabel,
        reflechoScore getOrElse esScore,
        Nil,
        Some(filteredFields.asJsObject)
      )
      
    // TODO this is a temp dummy fallback for consolidated companies
    case _ => Fact(entry.\\[JsString]("_id").value, "refleco:?")
  }
  
}

