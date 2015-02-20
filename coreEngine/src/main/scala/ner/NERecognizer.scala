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

/** represents a recognized named entity
 */
case class NE(entity: String, genus: String, score: Double, raw: String)

/** methods for recognizing entities
 *  TODO pull values into ceconfig
 */
object NERecognizer extends CEConfig with StrictLogging {
  val PENT_BOOST = 1.6
  val ENT_BOOST = 1
  val ATTENT_BOOST = 2
  val RELENT_BOOST = 1.5
  val FIRST_CUTOFF = 4.0
  val SECOND_CUTOFF = 7.5
  
  type Words = Seq[String]
  implicit def toWords(str: String): Words = str.trim.split(" ").map(_.trim)

  def apply(chunk: String): Seq[NE] = {
    val words = toWords(chunk)
    
    val combinations = (1 to words.size) flatMap (words.combinations(_).toSeq)
    val combMatrix = QLMatrix.fromSparse(combinations, words, new NERNode(_))
    
    val x = combMatrix.solve()(NERDLX)
    println(x)
    
    Nil
  }
  
  /** custom DLX implementation for NER nodes
   *  - evaluate node before iterating over it
   */
  implicit val NERDLX: DLX[NERNode] = new DLX[NERNode] {
    
    override def search(root: QuadHeader, path: List[NERNode] = Nil): Seq[List[NERNode]] = {
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
  
//  def apply(chunk: String): Seq[NE] = {
//    logger.info(s"NER starting: $chunk")
//
//    val entities = findAllNE(chunk, Nil)
//    
//    logger.info(s"NER from $chunk complete: $entities")
//    entities
//  }

//  @tailrec
//  def findAllNE(tokens: Words, acc: List[NE]): List[NE] = 
//    
//    findFirstNE(tokens) match {
//    
//      case Some(ne) =>
//        val rawCount = toWords(ne.raw).length
//        findAllNE(tokens drop rawCount, ne :: acc)
//
//      case None if tokens.length > 1 => 
//        findAllNE(tokens drop 1, acc)
//        
//      case _ => acc
//    }

  /** FIXME smarter logic
   */
//  def findFirstNE(tokens: Words): Option[NE] = {
//    val candidates = tokens.scanLeft("")(_+" "+_).drop(1).reverse.map(_.trim)
//
//    val entities = candidates.toStream flatMap (
//      c => tryNEPass(c) filter (_.score >= SECOND_CUTOFF) sortBy (- _.score)
//    )
//    
//    entities.headOption
//  }

  /** @note blocking
   */
  def identifyChunk(candidate: String): List[NE] = {
    val finders = List(tryPENT _, tryENT _, tryRELENT _, tryATTENT _)

    val results = finders traverseM (_(candidate).run)
    
    Await.result(results, 10.seconds)
  }

  def tryPENT(candidate: String): ListT[Future, NE] =
    tryDisambiguation(candidate, "10-K", "company", PENT_BOOST)
   
  def tryENT(candidate: String): ListT[Future, NE] = 
    tryDisambiguation(candidate, "entity", "entity", ENT_BOOST)
    
  def tryATTENT(candidate: String): ListT[Future, NE] =
    tryDisambiguation(candidate, "attribute", "attribute", ATTENT_BOOST)
    
  def tryRELENT(candidate: String): ListT[Future, NE] = 
    tryDisambiguation(candidate, "relation", "relation", RELENT_BOOST)
    
  def tryDisambiguation(candidate: String, doctype: String, enttype: String, boost: Double = 1.0) =
    LibraryConnector.checkScored(candidate, doctype, FIRST_CUTOFF) map {
      case (name, _, weight) => NE(name, enttype, weight * boost, candidate) 
    }
  
}
