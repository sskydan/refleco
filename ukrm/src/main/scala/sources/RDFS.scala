package sources

import com.typesafe.scalalogging.StrictLogging
import utilities.UConfig
import utilities.XMLUtil._
import extensions.Extensions._


object RDFS extends StrictLogging with UConfig {

  val RDFS_FILE = config getString "rdfSchema"

  def parseFile = {
    val rdfsXML = openXML(RDFS_FILE)

    val text = (rdfsXML \ "http://www.w3.org/2000/01/rdf-schema#").text
    val entryText = text split '\n' drop 40 map (_ replaceAll (";","")) map (_.trim)
    
    val entries = entryText.toList splitFilter (_.isEmpty)
    entries map { entry => 
      val name = entry.head takeWhile (_ != ' ')
      val classType = entry.head dropWhile (_ != ' ') drop 3 takeWhile (_ != ' ')
      val label = getByLabel(entry, "rdfs:label")
      val subClassOf = getByLabel(entry, "rdfs:subClassOf")
      val domain = getByLabel(entry, "rdfs:domain")
      val range = getByLabel(entry, "rdfs:range")
    
      
      
    }
    
    ???    
  }
  
  def getByLabel(entry: List[String], label: String): Option[String] = 
    entry find (_ startsWith label) map extractValue
    
  def extractValue(s: String) = 
    s.dropWhile(_ != " ").takeWhile(_ != " ")
    .replaceAll("<","").replaceAll(">","").replaceAll("\"","")
  
}