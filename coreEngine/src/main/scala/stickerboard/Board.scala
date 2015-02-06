package stickerboard

import utilities.CEConfig
import Sticker._
import com.typesafe.scalalogging.StrictLogging
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import stickerboard.sources.R10KGraph
import stickerboard.sources.DBPediaGraph
import scalaz.Scalaz._
import scalaz.Semigroup
import com.esotericsoftware.kryo.Kryo
import org.apache.spark.serializer.KryoRegistrator
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Output
import com.esotericsoftware.kryo.io.Input
import org.joda.time.DateTime
import com.esotericsoftware.kryo.serializers.JavaSerializer
import org.apache.spark.rdd.RDD
import java.io.File
import stickerboard.sources.SparkFNsDBP
import ner.KnownEntityManager
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import org.apache.spark.storage.StorageLevel
import org.apache.spark.HashPartitioner

object SparkFNs {
  /** TODO why map Map to iterable instead of list?
   */
  def getXBRLAttributes(root: Sticker): Set[String] = root.rels.flatMap{
    case (k, v) if k == "refleco:xbrl" => v map (_.alias.id)
    case (k, v) => v flatMap getXBRLAttributes
  }.toSet

  def getAllRelations(root: Sticker): Set[String] = root.rels.flatMap{
    case (k, v) => (v flatMap getAllRelations).toSet + SparkFNsDBP.resourceNameFromURI(k)
  }.toSet
}

/** TODO consistent actorsystem usage
 *  FIXME Await.result issues
 */
object Board extends CEConfig with StrictLogging {
  val sparkName = config getString "sparkAppName"
  val sparkMaster = config getString "sparkMaster"
  val sconf = new SparkConf() setAppName sparkName setMaster sparkMaster
  sconf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
  sconf.set("spark.kryo.registrator", "stickerboard.SBRegistrator")
  sconf.set("spark.akka.timeout", 10000)
  sconf.set("spark.akka.frameSize", 100)
  sconf.set("spark.akka.askTimeout", 300)
  implicit val spark = new SparkContext(sconf)

  val COMPANY = "refleco:company"
  val R10K = "refleco:10-K"
  val XBRL = "refleco:xbrl"
  val VALUE = "refleco:value"
  val GRAPH_STICKER_FILE = "refdata/graph"
  
  val rddGraph = getGraph persist StorageLevel.MEMORY_AND_DISK_SER

  def browse(str: String) = {
    val search = rddGraph filter (_._1 contains str) collect()
    search foreach ( s =>
      println(s._2.prettyPrint())
    )
    
    search map (_._1)
  }
  
  def find(id: String): Option[Sticker] =
    rddGraph.lookup(id).headOption
    
  def test = KnownEntityManager.createInverseIndex

  def getGraph: RDD[(String,Sticker)] = {
    val graph =
      if (!new File(GRAPH_STICKER_FILE).exists) {
        val graph = generateGraph
        saveGraph(graph)
        graph
      } else loadGraph
      
    graph keyBy (_.alias.id) partitionBy (new HashPartitioner(500))
  }
  
  /** setup the attribute graph
   *  @note printing the root graph is a bad idea
   */
  private def generateGraph: RDD[Sticker] = {
    val integrateValuesResult = Await.result(DBPediaGraph.generateValues, Duration.Inf)    
    
    val r10kGraph = R10KGraph.getGraph
    val r10kByName = r10kGraph map (s => s.alias.id -> s)
    
    val dbpGraph = DBPediaGraph.getGraph
    val dbpByName = dbpGraph map (s => s.alias.id -> s)
    
    val graph = r10kByName.cogroup(dbpByName).values map {
      case (reports, dbp) => (reports ++ dbp) reduce (_ |+| _)
    }

    graph
  }
  
  def saveGraph(graph: RDD[Sticker])(implicit spark: SparkContext) =
    graph map s2ss saveAsObjectFile GRAPH_STICKER_FILE

  def loadGraph(implicit spark: SparkContext): RDD[Sticker] =
    spark.objectFile[SSticker](GRAPH_STICKER_FILE) map ss2s
  
  implicit def it2set[A](it: Iterable[A]): Set[A] = it.toSet
  implicit def set2seq[A](set: Set[A]): Seq[A] = set.toSeq 
}


// FIXME LOL u srs m8
class SBRegistrator extends KryoRegistrator {
   override def registerClasses(kryo: Kryo) {
     kryo.register(classOf[Alias])
     kryo.register(classOf[Clue])
     kryo.register(classOf[DateTime])
//     kryo.register(classOf[Sticker])
//     kryo.register(classOf[SSticker], new JavaSerializer())
     
     kryo.register(classOf[Sticker], new Serializer[Sticker]() {
       def write(kryo:Kryo, output:Output, obj:Sticker) = {
         kryo.writeClassAndObject(output, obj.alias)
         kryo.writeClassAndObject(output, obj.birthday)
//         kryo.writeClassAndObject(output, obj.rels)
         
         output.writeInt(obj.rels.size)
         obj.rels map {
           case (k,v) =>
             kryo.writeClassAndObject(output, k)
             kryo.writeClassAndObject(output, v.backing)
         }
       }
       
       def read(kryo:Kryo, input:Input, tpe:Class[Sticker]) = {
         val a = kryo.readClassAndObject(input).asInstanceOf[Alias]
         val b = kryo.readClassAndObject(input).asInstanceOf[Option[DateTime]]
//         val r = kryo.readClassAndObject(input).asInstanceOf[RelationMap]
//         val mergingR = r mapValues (s => MergingSet(s.toSeq:_*))
         
         val size = input.readInt()
         val loaded = (1 to size).toSeq map { i =>
            val k = kryo.readClassAndObject(input).asInstanceOf[String]
            val v = kryo.readClassAndObject(input).asInstanceOf[Set[Sticker]]
            k -> MergingSet(v.toSeq:_*)
         }
         
         val r = 
           if (!loaded.isEmpty) loaded.toMap[String,MergingSet[Sticker]]
           else Map[String,MergingSet[Sticker]]()
         
         
         Sticker(a,r,b)
       }
     })
     
     kryo.register(classOf[MergingSet[Sticker]], new Serializer[MergingSet[Sticker]]() {
       def write(kryo:Kryo, output:Output, obj:MergingSet[Sticker]) = {
         kryo.writeClassAndObject(output, obj.backing)
//         output.writeInt(obj.backing.size)
//         obj.backing map (kryo.writeClassAndObject(output, _))
       }
    
       def read(kryo:Kryo, input:Input, tpe:Class[MergingSet[Sticker]]) = {
//         val len = input.readInt()
//         val backing = (1 to len) map (_ => kryo.readClassAndObject(input).asInstanceOf[Sticker])
         val backing = kryo.readClassAndObject(input).asInstanceOf[Set[Sticker]]
         MergingSet(backing.toSeq:_*)
       }
     })
  }
}

