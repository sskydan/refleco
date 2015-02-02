package stickerboard.sources

import stickerboard.Sticker
import stickerboard.Alias
import scalaz.Scalaz._
import scalaz.Semigroup
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import Sticker._
import java.io.File
import org.apache.spark.broadcast.Broadcast
import utilities.LibraryConnector
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import java.util.concurrent.TimeUnit
import stickerboard.MergingSet
import org.apache.spark.rdd.RDD
import akka.actor.ActorSystem
import scala.concurrent.Future
import utilities.XMLUtil._
import com.typesafe.scalalogging.StrictLogging
import facts._
import org.apache.spark.storage.StorageLevel
import scala.util.Try
import scala.util.Success

/** FIXME cleanup
 */
object SparkFNsDBP {
  // TODO version that doesn't remove parenthesis
  def resourceNameFromURI(uri: String): String =
    uri.reverse.takeWhile(c => c != '/' && c != '#')
       .reverse.takeWhile(_ != '(')
       .replaceAll("_", " ").trim

  def resourceNameFromType(uri: String): String =
    uri.reverse.takeWhile(_ != '/')
       .reverse.takeWhile(_ != ':')
       .replaceAll(
         String.format("%s|%s|%s",
           "(?<=[A-Z])(?=[A-Z][a-z])",
           "(?<=[^A-Z])(?=[A-Z])",
           "(?<=[A-Za-z])(?=[^A-Za-z])"
         ), " ")
       .trim
       
  def uriToAlias(uri: String): Alias = Alias(resourceNameFromURI(uri), Seq(), None, Some(uri))
  def uriToSticker(uri: String): Sticker = Sticker(uriToAlias(uri))
}

object DBPediaGraph extends StrictLogging {
  import SparkFNsDBP._
  val DBP_STICKER_FILE = "refdata/dbpStickers"
  val DBP_VALUES_FILE = "refdata/dbpValues"
  
  val PROPERTIES_FILE = "dbpedia/mappingbased_properties_en.nt"
  val INFOBOX_FILE = "dbpedia/infobox_properties_en.nt"
  val TYPES_FILE = "dbpedia/instance_types_en.nt"
//  val PROPERTIES_FILE = "dbpedia/test_mappingbased_properties_en.nt"
//  val INFOBOX_FILE = "dbpedia/test_infobox_properties_en.nt"
//  val TYPES_FILE = "dbpedia/test_instance_types_en.nt"
  
  val OWL_FILE = "dbpedia/dbpedia_2014.owl"
  val RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  val RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#"
  val XML_NS = "http://www.w3.org/XML/1998/namespace"

  val SYMBOL_URI = "http://dbpedia.org/property/symbol"
  val LOCAL_SYMBOL_URI = resourceNameFromURI(SYMBOL_URI)
  val TYPE_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
  val LOCAL_TYPE_URI = resourceNameFromURI(TYPE_URI)
  val DBP_ORG_URI = "http://dbpedia.org/ontology/Organisation"
  val DBP_ORG_TYPE = resourceNameFromType(DBP_ORG_URI)
  val DBP_COMPANY_URI = "http://dbpedia.org/ontology/Company"
  val DBP_COMPANY_TYPE = resourceNameFromType(DBP_COMPANY_URI)
  val NAME_URI = "http://xmlns.com/foaf/0.1/name"
  
  val DBP_PREFIX = "http://dbpedia.org/ontology/"
  
  def getGraph(implicit spark: SparkContext): RDD[Sticker] = {
    val graph =
      if (!new File(DBP_STICKER_FILE).exists) {
        val graph = generateGraph
        saveGraph(graph)
        graph
      } else loadGraph

    graph
  }
  
  def generateGraph(implicit spark: SparkContext): RDD[Sticker] = {
    // parse all raw data files
    val (relations, _) = parseMappingFile
    val types = parseTypesFile
    val allRelations = types ++ relations
    
    // collect and transform the relation information
    val groupedRelations = groupTriples(allRelations)
    
    // choose only the entities which are marked as companies
    val groupedOrgRelations = groupedRelations filter {
      case (uri, rmap) => 
        (rmap get TYPE_URI flatMap (_.find (_.alias.id == DBP_COMPANY_TYPE))) != None
    } persist StorageLevel.MEMORY_AND_DISK_SER
    
    // generate the candidate names &
    // try to match any of the candidate names to the already-existing stickers/companies
    val relationsWithNames = buildNamesList(groupedOrgRelations)
    val canonicalNames = tryDereferenceNames(relationsWithNames.toSet)

    // join the grouped datasets with the list of true names for each object
    val stickers = canonicalNames map { trueNames =>
      val entityNamesRDD = spark parallelize trueNames
      val relationsWithNames = groupedOrgRelations join entityNamesRDD
      
      // generate simple stickers (with extracted relationship titles as values)
      val relationStickers = relationsWithNames map {
        case (url, (relations, (name, _))) =>
          
          url -> Sticker(Alias(name, Seq(), None, Some(url)), relations)
      }
      
      val symbolRDD = parseInfoboxFile map (t => t.subject -> t.obj)
      // complete the list of aliases for this type of sticker (companies)
      val stickersWithAliases = (relationStickers leftOuterJoin symbolRDD).values map {
        case (sticker, Some(symbol)) =>
          Sticker(
            Alias(sticker.alias.id, symbol +: sticker.alias.aliases, None, sticker.alias.semanticID),
            sticker.rels
          )
        case (sticker, None) => sticker
      }

      stickersWithAliases
    }

    groupedOrgRelations unpersist()
    
    // handle the new types of entities (don't do name lookup)
    val groupedOtherRelations = groupedRelations filter {
      case (uri, rmap) => 
        (rmap get TYPE_URI flatMap (_.find (_.alias.id == DBP_COMPANY_TYPE))) == None
    }
    val otherStickers = groupedOtherRelations map {
      case (url, relations) =>
        val name = resourceNameFromURI(url)
        Sticker(Alias(name, Seq(), None, Some(url)), relations)
    }
    
    Await.result(stickers, Duration.Inf) ++ otherStickers
  }
  
  /** integrate the new values mined from the dbp files, so that we can
   *    reference those values by fact-id when we create the sticker representations
   *  FIXME get proper relation names from the owl file
   *  TODO useful return value
   *  FIXME get values for non-company types
   */
  def generateValues(implicit spark: SparkContext): Future[Boolean] = 
    if (!new File(DBP_VALUES_FILE).exists) {
      val (_, mappingValues) = parseMappingFile
      val types = parseTypesFile map { case Triple(s,p,o) => Triple(s, p, resourceNameFromType(o)) }
      val allValues = mappingValues ++ types
      
      // collect and transform the relation information
      val groupedValues = groupTriplesPretty(allValues)
      
      // choose only the entities which are marked as companies
      val groupedOrgValues = groupedValues filter {
        case (uri, rmap) => 
          (rmap get LOCAL_TYPE_URI flatMap (_.find (_.alias.id == DBP_COMPANY_TYPE))) != None
      } persist StorageLevel.MEMORY_AND_DISK_SER
      
      // generate the candidate names &
      // try to match any of the candidate names to the already-existing stickers/companies
      val relationsWithNames = buildNamesList(groupedOrgValues)
      val canonicalNames = tryDereferenceNames(relationsWithNames.toSet)
  
      // collect all the info necessary to insert the facts into lib
      val facts = canonicalNames map { trueNames =>
        val entityNamesRDD = spark parallelize trueNames
        val valuesWithNames = groupedOrgValues join entityNamesRDD
        
        // transform the named values to be updated into our fact format
        val facts = valuesWithNames map { 
          case (url, (values, (name, id))) =>
            
            // transform each relation-values pair into a fact
            val valueChildren = valuesToFacts(values)
            
            // concatenate all prettynames for this entity

            val companyID = id takeWhile (_ != ':')
            val cik = if (companyID == "") FactNone else FactString(companyID)
            
            Fact(name, "company", cik, name, 0, valueChildren)
        }
        
        // save a local copy of the changes to be made
        facts saveAsObjectFile DBP_VALUES_FILE
        
        facts.collect().toSeq
      }
      
      groupedOrgValues unpersist ()
      
      // actually persist the new facts 
      val results = facts flatMap (fs => Future.sequence(fs grouped(1000) map LibraryConnector.update))
      val globalResult = results map (_.foldLeft(true)(_ && _))
      globalResult
      
  } else Future.successful(true)

  /** from a set of relation-values, generate a seq of fact represenations of each relation-value
   */
  def valuesToFacts(rs: RelationMap): Seq[Fact] = 
    rs.toSeq.collect { 
      case (relation, values) if Try(BigDecimal(values.head.alias.id)).isSuccess => 
        val factVals = values.headOption map (v => FactMoney(BigDecimal(v.alias.id), ""))
        Fact(relation, "dbp:value", factVals.getOrElse(FactNone), relation)
      
      case (relation, values) =>
        val factVals = values map (v => Group(FactString(v.alias.id)))
        Fact(relation, "dbp:value", FactCol(factVals.toList), relation)
    }
//    case (relation, values) if values.size > 1 => 
//    case (relation, values) => 
//      Fact(relation, "dbp:value", FactString(values.head.alias.id), relation)
    
  /** parse the dbp ontology file, to get a mapping of relation pretty labels
   *    to relation URIs
   */
  def parseOwlFile: Seq[(String,String)] = {
    val ontologyXML = openXML(OWL_FILE)
        
    ontologyXML \ "_" flatMap { case elem => 
      val uri = elem \ s"@{$RDF_NS}about"
      
      elem \ "_" find (elem => 
        elem.label == "label" &&
        (elem \ s"@{$XML_NS}lang").toString() == "en"
        
      ) map (uri.text -> _.text) orElse {
        logger.error(s"Could not parse owl entry: ${elem.toString}")
        None
      }
    }
  }
  
  /** parse some whitelisted properties out of the infobox list
   */
  def parseInfoboxFile(implicit spark: SparkContext): RDD[Triple] = {
    val propertiesRDD = spark textFile INFOBOX_FILE map Triple.str2triple
    propertiesRDD filter (_.predicate == SYMBOL_URI)
  }

  /** parse the main, semi-cleaned dbp mappings list
   *  FIXME smarter value vs relationship discrimination
   */
  def parseMappingFile(implicit spark: SparkContext): (RDD[Triple], RDD[Triple]) = {
    val propertiesRDD = spark textFile PROPERTIES_FILE map Triple.str2triple
    
    val relations = propertiesRDD filter (_.obj.contains("http://"))
    val directValues = propertiesRDD filter (! _.obj.contains("http://"))

    relations -> directValues 
  }

  /** parse the type/class information
   */
  def parseTypesFile(implicit spark: SparkContext): RDD[Triple] = 
    spark textFile TYPES_FILE map Triple.str2triple filter (_.obj startsWith DBP_PREFIX)
  
  def typesPretty(triples: RDD[Triple]): RDD[Triple] =
    triples map { case Triple(s,p,o) => Triple(s, p, resourceNameFromType(o))}
      
  /** group the flat list of triples by company, then for each
   *    company group by relationtype
   */
  def groupTriples(triples: RDD[Triple]): RDD[(String, RelationMap)] = {
    val properties = triples map (
      t => t.subject -> (t.predicate -> uriToSticker(t.obj))
    )

    properties.aggregateByKey(Map[String, MergingSet[Sticker]]())(
      (a: RelationMap, b: (String, Sticker)) => 
        a |+| Map(b._1 -> MergingSet(b._2)),
      (a: RelationMap, b: RelationMap) => 
        a |+| b
    )
  }
  
  /** group triples as in groupTriples, but replace relationship URI's with their
   *    prettyLabeled version 
   */
  def groupTriplesPretty(triples: RDD[Triple])(implicit spark: SparkContext): RDD[(String, RelationMap)] = {
    val relationNames = spark parallelize parseOwlFile
    val byRelation = triples map (t => t.predicate -> t)
    val propertiesPretty = (byRelation leftOuterJoin relationNames).values map {
      case (Triple(s,p,o), prettyRelation) =>
        Triple(s, prettyRelation getOrElse resourceNameFromURI(p), o)
    }
    
    groupTriples(propertiesPretty)
  }
  
  /** for each top-level entity, generate the complete list of its names/aliases
   *  FIXME pass in name attribute instead 
   *  FIXME this was working on spark, once
   */
  def buildNamesList(entitiesGrouped: RDD[(String, RelationMap)]): Array[(String, Set[String], String)] =
    entitiesGrouped map {
      case (url, rels) => 
        val cleanName = resourceNameFromURI(url)
        val names = rels get NAME_URI map (_ map (_.alias.id))
        val namesToCheck = names getOrElse Set(cleanName)

        (url, namesToCheck, cleanName)
    } collect()
 
  /** lookup and find best match for a resource based on a list of provided aliases
   *  @note parallellized
   */
  def tryDereferenceNames(entities: Set[(String, Set[String], String)]): Future[Seq[(String, (String, String))]] = 
    Future.traverse(entities.toSeq) {
      case (k, names, originalName) =>
        LibraryConnector.checkScoredAll(names.toSeq).headOption map {
          case Some((trueName, cik, _)) => k -> (trueName, cik) 
          case _ => k -> (originalName, "")
        }
    }
  
  def saveGraph(graph: RDD[Sticker])(implicit spark: SparkContext) =
    graph map s2ss saveAsObjectFile DBP_STICKER_FILE

  def loadGraph(implicit spark: SparkContext): RDD[Sticker] =
    spark.objectFile[SSticker](DBP_STICKER_FILE) map ss2s
}

/** obj can be either a URI-reference or a straigt up value string TODO awkwarrrd
 *  TODO value calss
 */
case class Triple(subject: String, predicate: String, obj: String)
object Triple {
  implicit def str2triple(str: String): Triple = {
    val (subject, rem) = str span (_ != ' ') map (_.trim)
    val (predicate, rem2) = rem span (_ != ' ') map (_.trim)
    // Variant 1 ---> "Google Inc."@en .
    // Variant 2 ---> "0384"^^<http://www.w3.org/2001/XMLSchema#gYear> .
    // Variant 3 ---> "F84.0".
    // Variant 4 ---> <http://dbpedia.org/resource/Western_philosophy> .
    val obj =
      // Variant 4
      if (rem2 startsWith "<") rem2 replaceAll ("<","") replaceAll ("> .", "")
      // Variant 2
//    else if (rem2 contains "<") rem2 dropWhile (_ != '<') drop 1 takeWhile (_ != '>')
      // Variant 1,3
      else rem2 drop 1 takeWhile (_ != '"')
    
    val cleanS = subject.trim replaceAll ("<", "") replaceAll (">", "")
    val cleanP = predicate.trim replaceAll ("<", "") replaceAll (">", "")
    
    Triple(cleanS.trim, cleanP.trim, obj.trim)
  }
}


