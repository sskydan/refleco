package ner

import api.EntityIndex
import stickerboard._
import scalaz.Scalaz._
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import java.io.File
import scala.io.Source
import utilities.XMLUtil._
import com.typesafe.scalalogging.StrictLogging
import stickerboard.sources.DBPediaGraph

object EDTransforms {
  // all nodes in the graph which are semantically meaningful
  def indexEntities(root: Sticker): Map[String, String] = { 
    val aliasMap = 
      if (root.alias.factUUID.isEmpty)
        (root.alias.id +: root.alias.aliases) map (_ -> root.alias.id)
      else 
        Map()
    
    val childrenAliases = root.rels flatMap { case (k, v) => v flatMap indexEntities }

    childrenAliases ++ aliasMap
  }
  
  // leaves which are associated with direct values (ie, numbers)
  def indexAttributes(root: Sticker): Map[String, String] = {
    val aliasMap = 
      if (root.alias.semanticID.isEmpty && root.rels.isEmpty)
        (root.alias.id +: root.alias.aliases) map (_ -> root.alias.id)
      else 
        Map()
    
    val childrenAliases = root.rels flatMap { case (k, v) => v flatMap indexAttributes }

    childrenAliases ++ aliasMap
  }
}

/** handles creation and persistence of inverse indices of surface forms
 */
object KnownEntityManager extends StrictLogging {
  val REL_INDEX = "refdata/relindex"
  val ENT_INDEX = "refdata/entindex"
  val ATT_INDEX = "refdata/attindex"
  
  val OWL_FILE = "dbpedia/dbpedia_2014.owl"
  val RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  val RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#"
  val XML_NS = "http://www.w3.org/XML/1998/namespace"

  /** generic entry point
   */
  def createInverseIndex = {
//    implicit val spark = Board.spark
//    
//	  createRelationsIndex
//    createEntityIndex
//    createAttributeIndex
  }
  
  private def createEntityIndex(implicit spark: SparkContext) =
    if (!new File(ENT_INDEX).exists) {
      val graphEntities = Board.rddGraph map (_._2) flatMap EDTransforms.indexEntities
      graphEntities.distinct() saveAsObjectFile ENT_INDEX
    }

  private def createAttributeIndex(implicit spark: SparkContext) = 
	  if (!new File(ATT_INDEX).exists) {
		  val graphAttributes = Board.rddGraph map (_._2) flatMap EDTransforms.indexAttributes
		  graphAttributes.distinct() saveAsObjectFile ATT_INDEX
	  }
  
  /** the relations index is special because its surface forms have to be read
   *    from the dbpedia owl file
   */
  private def createRelationsIndex(implicit spark: SparkContext) =
    if (!new File(REL_INDEX).exists) {
      val ontologyXML = openXML(OWL_FILE)
    		  
      val namesToUris = ontologyXML \ "_" flatMap { elem => 
    	  val uri = elem \ s"@{$RDF_NS}about"
    	  
    	  elem \ "_" find (elem => 
      	  elem.label == "label" &&
      	  (elem \ s"@{$XML_NS}lang").toString() == "en"
          
    	  ) map (_.text -> uri.text) orElse {
    		  logger error s"Could not parse owl entry: ${elem.toString}"
    		  None
    	  }
      }
      
      val rdd = spark parallelize namesToUris
      rdd saveAsObjectFile REL_INDEX
    }
}