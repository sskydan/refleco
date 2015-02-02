package stickerboard.sources

import facts.Fact
import facts.Fact._
import facts.FactString
import facts.FactNone
import serializers.FactSerializers._
import scala.concurrent.Future
import server.CoreParams
import utilities.CEConfig
import scalaz.Scalaz._
import scalaz.Semigroup
import com.typesafe.scalalogging.StrictLogging
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scala.collection.mutable.LinkedHashMap
import java.io.File
import stickerboard.Sticker
import stickerboard.Sticker._
import stickerboard.Alias
import utilities.LibraryConnector._
import org.joda.time.DateTime
import stickerboard.MergingSet
import org.apache.spark.rdd.RDD

/** FIXME clean up
 *  @note needs to be a simple object otherwise things go wrong
 *  @note need to be very careful with using external variables so that spark doesn't have to 
 *    serialize and load extra classes into partitions
 */
object SparkFNs10K {
  /** FIXME smarter serialization - from es (kryo?), or thru macros, or thru json?
   */
  def materialize10KFacts(m: LinkedHashMap[String, Any]): Fact = {
    val id = m("id").toString
    val ftype = m("ftype").toString
    val value = m("value").toString
    val prettyLabel = m("prettyLabel").toString
    val interest = m("interest").toString.toDouble
    val children = m("children")
      .asInstanceOf[Seq[LinkedHashMap[String, Any]]]
      .map (materialize10KFacts)
    val uuid = m("uuid").toString

    Fact(id, ftype, FactString(value), prettyLabel, interest, children, None, uuid)
  }
  
  def fact2relationmap(f: Fact) = f.prettyLabel -> fact2sticker(f)
  
  def wrap10KAsSticker(cname:String, rels:Seq[Sticker]) = 
    Sticker(
      Alias(cname), 
      Map("refleco:10-K" -> MergingSet(rels:_*))
    )
  
  def fact2sticker(fact: Fact): Sticker = fact.ftype match {
    case "10-K" => 
      Sticker(
        Alias(fact.prettyLabel, Seq(fact.id), Some(fact.uuid)),
        Map("refleco:xbrl" -> facts2stickers(fact.children)),
        Some(new DateTime(fact.value.get))
      )
      
    case "company" => 
      Sticker(
        Alias(fact.prettyLabel, Seq(fact.id), Some(fact.uuid)),
        Map("dbp:values" -> facts2stickers(fact.children))
      )
   
    case _ =>
      Sticker(
        Alias(fact.prettyLabel, Seq(fact.id), Some(fact.uuid))
      )
  }
  
  def facts2stickers(facts: Facts): Set[Sticker] = (facts map fact2sticker).toSet
}

object R10KGraph extends CEConfig with StrictLogging {
  val R10K_FILE = "../library/refdata/10-K"
  val R10K_STICKER_FILE = "refdata/10KStickers"
  
  def getGraph(implicit spark: SparkContext): RDD[Sticker] = {
    val graph =
      if (!new File(R10K_STICKER_FILE).exists) {
        val graph = generateGraph
        saveGraph(graph)
        graph
      } else loadGraph

    graph
  }
  
  def generateGraph(implicit spark: SparkContext): RDD[Sticker] = {
    // pull data from companies
    val companiesRDD = spark parallelize (streamDocs("S-1").flatten.toList) map SparkFNs10K.fact2sticker
    val companiesByName = companiesRDD map (s => s.alias.id -> s)

    // read raw 10-K facts from file 
    val reportRDD = spark.objectFile[Tuple2[String, LinkedHashMap[String, Any]]](R10K_FILE)
    val factsRDD = reportRDD map (_._2) map SparkFNs10K.materialize10KFacts
    
    val relationsByName = factsRDD map SparkFNs10K.fact2relationmap groupByKey() 
    
    val stickersByName = relationsByName map { 
      case (name, rels) => name -> SparkFNs10K.wrap10KAsSticker(name, rels.toSeq)
    }
    
    // join company and 10K data
    stickersByName.cogroup(companiesByName).values.map{
      case (l, r) => (l ++ r) reduce (_ |+| _)
    }
  }

  def saveGraph(graph: RDD[Sticker])(implicit spark: SparkContext) =
    graph map s2ss saveAsObjectFile R10K_STICKER_FILE

  def loadGraph(implicit spark: SparkContext): RDD[Sticker] =
    spark.objectFile[SSticker](R10K_STICKER_FILE) map ss2s
}

