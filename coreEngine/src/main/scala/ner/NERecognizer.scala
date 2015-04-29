package ner

import facts.Fact
import facts.Fact._
import scala.concurrent.Future
import serializers.FactSerializers._
import server.CoreParams
import utilities.CEConfig
import scala.xml.NodeSeq
import scala.concurrent.Await
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.Promise
import stickerboard.Board
import stickerboard.Sticker._
import stickerboard.Sticker
import com.typesafe.scalalogging.StrictLogging
import scala.concurrent.ExecutionContext.Implicits.global
import utilities.LibraryConnector
import scalaz._
import scalaz.Scalaz
import scalaz.Scalaz._
import scala.annotation.tailrec
import scalaz.OptionT._
import scalaz.OptionT
import dlx.QLMatrix
import dlx.DLX
import dlx.QuadHeader
import dlx.QLList
import extensions.Extensions._


/** handles named entity recognition from a phrase
 */
class NERecognizer(chunk: String) extends StrictLogging {

  /** represents the matrix of possible structural chunk combinations generated from the
   *    initial string
   */
  lazy val matrix: QLMatrix[NERNode] = {
    val words = chunk.trim.split(" ").map(_.trim).toSeq
    
    val combinations = for {
      i <- 0 to words.size
      j <- 1 to words.size - i
    } yield words slice(i, i+j) zip(i to i+j)
    
    val matrix = QLMatrix.fromSparse(combinations, words, new NERNode(_))
    
    // initialize the nodes with their row
    matrix.root.r.foreach[QuadHeader](_.r){ col =>
      col.foreachRem[QLList[_<:QLList[_]]](_.dn){
        
        case n: NERNode => 
          val row = 
            if (!n.l.row.isEmpty) n.l.row
            else n.traverse[NERNode,NERNode](_.r)(x => x)
            
          n.row = row
        case _ =>
      }
    }
    
    matrix
  }
  
  lazy val solutions: Seq[NESentence] = {
    //FIXME shoundn't be necessary
    val structuredSentences = matrix solve (_.executionResults) map (_ map (_.distinct))
    
    val sentences = structuredSentences flatMap (_.cartesianProduct map (NESentence(_, chunk).rank))
    
    val topResults = sentences sortBy (- _.score) take 50
    
    topResults
  }
  
  /** custom DLX implementation for NER nodes
   *  - evaluate node before iterating over it
   */
  implicit val NERDLX = new DLX[NERNode] {
  	override def search(root: QuadHeader, path: List[NERNode] = Nil): Seq[List[NERNode]] =
			if (root.r != root) {
				val c = chooseColumn(root)
				c.cover
				
				val solutions = c.traverseRemG(_.dn) { 
			
				  case r: NERNode if r.evaluationResults =>
						r.foreachRem(_.r)(_.c.cover)
						val subSolutions = search(root, r :: path)
						r.foreachRem(_.l)(_.c.uncover)
						
						subSolutions
				
				  case _ => Nil
				}
				
				c.uncover
				solutions.flatten
				
			} else Seq(path) 
  }
}

object NERecognizer extends CEConfig {
  val ATTENT_BOOST = config getDouble "attributeBoost"
  val FIRST_CUTOFF = config getDouble "firstCutoff"
  val SECOND_CUTOFF = config getDouble "secondCutoff"
  
  def identifyChunk(candidate: String): List[NE] =
    tryMatchChunk(candidate) filter (_.score >= SECOND_CUTOFF)
  
  /** @note blocking
   */
  def tryMatchChunk(candidate: String): List[NE] = {
    val finders = List(tryPENT _, tryENT _, tryRELENT _, tryATTENT _)

    val results = finders traverseM (_(candidate).run)
    
    Await.result(results, 10.seconds)
  }

  def tryPENT(candidate: String): ListT[Future, NE] =
    tryDisambiguation(candidate, "10-K", "company", 2.5)
   
  def tryENT(candidate: String): ListT[Future, NE] = 
    tryDisambiguation(candidate, "entity", "entity")
    
  def tryATTENT(candidate: String): ListT[Future, NE] =
    tryDisambiguation(candidate, "attribute", "attribute", ATTENT_BOOST)
    
  def tryRELENT(candidate: String): ListT[Future, NE] = 
    tryDisambiguation(candidate, "relation", "relation")
    
  def tryDisambiguation(candidate: String, doctype: String, enttype: String, boost: Double = 1.0) =
    LibraryConnector.checkScored(candidate, doctype, FIRST_CUTOFF) map {
      case (name, _, weight) => NE(name, enttype, weight*boost, candidate) 
    }
}
