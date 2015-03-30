package sources

import utilities.UConfig
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import scalaz.Scalaz._
import scalaz.Semigroup
import utilities.XMLUtil._
import com.typesafe.scalalogging.StrictLogging


object DBPedia extends StrictLogging with UConfig {
  // file locations
  val DBP_STICKER_FILE = config getString "dbpStickerFile"
  val DBP_VALUES_FILE = config getString "dbpValuesFile"
  val PROPERTIES_FILE = config getString "dbpPropertiesFile"
  val INFOBOX_FILE = config getString "dbpInfoboxFile"
  val TYPES_FILE = config getString "dbpTypesFile"
  val OWL_FILE = config getString "dbpOWLFile"
  // namespaces
  val RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  val RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#"
  val XML_NS = "http://www.w3.org/XML/1998/namespace"
  // dbp ontology prefix  
  val DBP_PREFIX = "http://dbpedia.org/ontology/"
  // prefixes and special properties 
//  val SYMBOL_URI = "http://dbpedia.org/property/symbol"
//  val LOCAL_SYMBOL_URI = resourceNameFromURI(SYMBOL_URI)
//  val TYPE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
//  val LOCAL_TYPE_URI = resourceNameFromURI(TYPE_URI)
//  val DBP_ORG_URI = "http://dbpedia.org/ontology/Organisation"
//  val DBP_ORG_TYPE = resourceNameFromType(DBP_ORG_URI)
//  val DBP_COMPANY_URI = "http://dbpedia.org/ontology/Company"
//  val DBP_COMPANY_TYPE = resourceNameFromType(DBP_COMPANY_URI)
//  val NAME_URI = "http://xmlns.com/foaf/0.1/name"
  
  implicit val spark: SparkContext = ???
  
  //FIXME parenthesis
//  def resourceNameFromURI(uri: String): String =
//    uri.reverse.takeWhile(c => c != '/' && c != '#')
//       .reverse.takeWhile(_ != '(')
//       .replaceAll("_", " ").trim

//  def resourceNameFromType(uri: String): String =
//    uri.reverse.takeWhile(_ != '/')
//       .reverse.takeWhile(_ != ':')
//       .replaceAll(
//         String.format("%s|%s|%s",
//           "(?<=[A-Z])(?=[A-Z][a-z])",
//           "(?<=[^A-Z])(?=[A-Z])",
//           "(?<=[A-Za-z])(?=[^A-Za-z])"
//         ), " ")
//       .trim
   
  
  
  /** parse the main, semi-cleaned dbp properties list
   */
  def parseMappingProperties(implicit spark: SparkContext): (RDD[Quad], RDD[Quad]) = {
    val propertiesRDD = spark textFile PROPERTIES_FILE map Quad.fromString
    
    val chosedProperties = propertiesRDD filter (t => 
      !(mpropertyBlacklist contains t.predicate)
    )
    val relations = chosedProperties filter (_.unit.isEmpty)
    val directValues = chosedProperties filter (_.unit.nonEmpty)

    relations -> directValues 
  }
  
  def parseInfoboxProperties(implicit spark: SparkContext): RDD[Quad] = {
    val propertiesRDD = spark textFile INFOBOX_FILE map Quad.fromString
    propertiesRDD filter (ipropertyWhitelist contains _.predicate)
  }
  
  def parseOwl: Seq[(String,String)] = {
    val ontologyXML = openXML(OWL_FILE)
        
    ontologyXML \ "_" flatMap { case elem => 
      val uri = elem \ s"@{$RDF_NS}about"
      
      elem \ "_" find (elem => 
        elem.label == "label" &&
        (elem \ s"@{$XML_NS}lang").toString() == "en"
        
      ) map (uri.text -> _.text) orElse {
        logger error s"Could not parse owl entry: ${elem.toString}"
        None
      }
    }
  }
  
  val mpropertyBlacklist = Seq(
    "http://dbpedia.org/ontology/revenue",
    "http://dbpedia.org/ontology/operatingIncome",
    "http://dbpedia.org/ontology/netIncome",
    "http://dbpedia.org/ontology/assets",
    "http://dbpedia.org/ontology/equity"
  )
  
  val ipropertyWhitelist = Seq(
    "http://dbpedia.org/property/name",
    "http://dbpedia.org/property/symbol",
    
    "http://dbpedia.org/property/owner",
    "http://dbpedia.org/property/parent",
    
    "http://dbpedia.org/property/manufacturer",
    "http://dbpedia.org/property/operators",
    
    "http://dbpedia.org/property/employer",
    
    "http://dbpedia.org/property/workplaces",
    "http://dbpedia.org/property/workInstitution",
    "http://dbpedia.org/property/workInstitutions",
    
    "http://dbpedia.org/property/developer",
    
    "http://dbpedia.org/property/currentTenants",
    
    "http://dbpedia.org/property/author",
    "http://dbpedia.org/property/publisher"
  )
  
}

/** Triples supporting both values-with-unit and references as objects
 */
case class Quad(subject: String, predicate: String, obj: String, unit: Option[String])
object Quad {
  def fromString(str: String): Quad = {
    val (subject, rem) = str span (_ != ' ') map (_.trim)
    val (predicate, rem2) = rem span (_ != ' ') map (_.trim)
    
    // Variant 1 ---> "Google Inc."@en .
    // Variant 2 ---> "0384"^^<http://www.w3.org/2001/XMLSchema#gYear> .
    // Variant 3 ---> "F84.0".
    // Variant 4 ---> <http://dbpedia.org/resource/Western_philosophy> .
    val (obj, unit) =
      // Variant 4
      if (rem2 startsWith "<") 
        (rem2 replaceAll ("<","") replaceAll ("> .", "")) -> None
      // Variant 2
      else if (rem2 contains "<") 
        (rem2 drop 1 takeWhile (_ != '"')) -> 
        Some(rem2 dropWhile (_ != '<') drop 1 takeWhile (_ != '>'))
      // Variant 1,3
      else 
        (rem2 drop 1 takeWhile (_ != '"')) -> None
    
    val cleanS = subject replaceAll ("<", "") replaceAll (">", "")
    val cleanP = predicate replaceAll ("<", "") replaceAll (">", "")
    
    Quad(cleanS.trim, cleanP.trim, obj.trim, unit)
  }
}
