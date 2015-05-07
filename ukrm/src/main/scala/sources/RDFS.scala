package sources

import com.typesafe.scalalogging.StrictLogging
import utilities.UConfig
import utilities.XMLUtil._
import extensions.Extensions._
import facts.FactNone
import facts.FactVal
import spray.json.JsObject
import facts.Fact
import org.apache.spark.graphx.Edge
import ukrm.UKRM._
import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime
import ukrm.FactCandidate

object RDFS extends StrictLogging with UConfig {

  val RDFS_FILE = config getString "rdfSchema"

  var factCandidates: ListBuffer[FactCandidate] = ListBuffer.empty 
  var edgeCandidates: ListBuffer[(String, String, String, Option[DateTime])] = ListBuffer.empty

  /**
   */
  def extractData() = {
    val rdfsXML = openXML(RDFS_FILE)

    val text = (rdfsXML \ "http://www.w3.org/2000/01/rdf-schema#").text
    val entryText = text split '\n' drop 40 map (_ replaceAll (";","")) map (_.trim)
    
    val entries = entryText.toList splitFilter (_.isEmpty)
    entries foreach { entry => 

      val name = entry.head takeWhile (_ != ' ')
      val classType = entry.head dropWhile (_ != ' ') drop 3 takeWhile (_ != ' ')
      val subClassOf = getByLabel(entry, "rdfs:subClassOf")
      factCandidates += FactCandidate(classType, subClassOf, Seq(name))

      val domain = getByLabel(entry, "rdfs:domain")
      val range = getByLabel(entry, "rdfs:range")
      domain foreach (d => edgeCandidates += ((name, "rdfs:domain", d, None)))
      range foreach (r => edgeCandidates += ((name, "rdfs:range", r, None)))
    }
  }
  
  def integrateData() = {
    factCandidates map { candidate =>
      // prioritize name lookups in the local dataset, as the naming is assumed to be exact
      
      
      // else, try to dereference entities in the global dataset
    }
  }
  
  def getByLabel(entry: List[String], label: String): Option[String] = 
    entry find (_ startsWith label) map extractValue
    
  def extractValue(s: String) = 
    s.dropWhile(_ != " ").takeWhile(_ != " ")
    .replaceAll("<","").replaceAll(">","").replaceAll("\"","")
  
}
