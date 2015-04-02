package sources

import com.typesafe.scalalogging.StrictLogging
import utilities.UConfig
import utilities.XMLUtil._
import extensions.Extensions._


object OWL2 extends StrictLogging with UConfig {

  val OWL_FILE = config getString "dbpOWLFile"

  def owlParser = {
    val owlXML = openXML(OWL_FILE)

    val text = (owlXML \ "http://www.w3.org/2002/07/owl").text
    val entryText = text split '\n' drop 40 map (_ replaceAll (";","")) map (_.trim)
    
    val entries = entryText.toList splitFilter (_.isEmpty)
    entries map { entry => 
      val name = entry.head takeWhile (_ != ' ')
      val classOf = entry.head dropWhile (_ != ' ') drop 3 
      val label = getByLabel(entry, "rdfs:label")
      val superClass = getByLabel(entry, "rdfs:subClassOf")
      val domain = getByLabel(entry, "rdfs:domain")
      val range = getByLabel(entry, "rdfs:range")
    
      Fact(classOf, superClass)
      
    }
    
    ???    
  }
  
  def getByLabel(entry: List[String], label: String) = 
    entry find (_ startsWith label) map extractValue
    
  def extractValue(s: String) = 
    s.dropWhile(_ != " ").takeWhile(_ != " ")
    .replaceAll("<","").replaceAll(">","").replaceAll("\"","")
  
}