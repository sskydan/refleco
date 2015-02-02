package rr

import java.io.File
import scala.collection.immutable.HashMap
import scala.xml.XML
import utilities.LibConfig
import scala.xml.Node
import scala.util.Try
import facts.Fact
import com.typesafe.scalalogging.StrictLogging

/** representation of the calculation hierarchy between facts
 *  FIXME type alias for Relations
 *  TODO more efficient parent building?
 */
object FactRelations extends LibConfig with StrictLogging {
  val XBRL_TAX = new File(config.getString("taxonomyFiles")+"MasterCalc_calc.xml")

  val factHierarchy = fromXML

  def lookupFact(fname: String) = {
    val srch = factHierarchy get fname map (_._1 map (_.components))
    logger.info("Hierarchy fact lookup for: "+fname+" returned "+srch)
    srch
  }

  // constructions	
  type FFN = String => BigDecimal
  case class Relation(components: Seq[String] = Nil, fn: FFN => BigDecimal = _ => 0) {
    def apply(more: Seq[String], step: FFN => BigDecimal) =
      Relation(components ++ more, ffn => fn(ffn) + step(ffn))
  }
  //doen'st work?
  //  type Summation = Relation

  type Parents = Seq[String]

  // definition of the (directed) bidirectional graph/ association lists
  type DirectedBiAL = HashMap[String, (Seq[Relation], Parents)]

  // in the current iteration, a fact can have only one genesis
  private def fromXML: DirectedBiAL = {
    val xml = XML loadFile XBRL_TAX
    val calculations = xml \\ "calculationArc"
    def cleanAtt(parent: Node, key: String) =
      parent.attributes.asAttrMap get key map (_.replaceAll("lbl_", ""))

    val constructionMap =
      scala.collection.mutable.HashMap[String, Relation]() withDefault (_ => Relation())

    // parse the values out
    for {
      c <- calculations
      w <- cleanAtt(c, "weight") map (_.toDouble)
      from <- cleanAtt(c, "xlink:from")
      to <- cleanAtt(c, "xlink:to")
    } yield constructionMap += from -> constructionMap(from)(Seq(to), ffn => w * ffn(to))

    // build the parent lists
    val parentRelations = for {
      (k, _) <- constructionMap
      (p, components) <- constructionMap mapValues (_.components)
      if components contains k
    } yield List(k -> p)

    // group the relations by key
    val parentMap = parentRelations.flatten.groupBy(_._1) mapValues (_.map(_._2).toList)

    for {
      (k, r) <- HashMap(constructionMap.toSeq: _*)
      parents <- parentMap get k
    } yield k -> (Seq(r), parents)
  }
}