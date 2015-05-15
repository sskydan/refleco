package sources

import com.typesafe.scalalogging.StrictLogging
import utilities.UConfig
import utilities.XMLUtil._
import extensions.Extensions._
import facts.FactNone
import facts.FactVal
import spray.json.JsObject
import facts.Fact
import org.apache.spark.graphx.Edge
import ukrm.UKRM._
import scala.collection.mutable.ListBuffer
import org.joda.time.DateTime
import ukrm.FactCandidate
import scala.collection.immutable.HashMap
import ukrm.FactResolver


object RDFS extends FactResolver with UConfig {
  val RDFS_FILE = config getString "rdfSchema"

  var factCandidates: ListBuffer[FactCandidate] = ListBuffer.empty 
  var edgeCandidates: ListBuffer[(String, String, String, Option[DateTime])] = ListBuffer.empty
  var dependencies: HashMap[FactCandidate, Seq[FactCandidate]] = HashMap()
  
  /** main method to parse new information from this datasource and integrate it with our global corpus
   */
  def integrateDatasource() = {
    extractData
    buildDependencyMap
    val facts = resolveOrderedData

    // facts ready for posting
    ???
  }
  
  /** extract the fact candidates and edge candidates that we recognize in this datasource
   */
  def extractData() = {
    val rdfsXML = openXML(RDFS_FILE)

    val text = (rdfsXML \ "http://www.w3.org/2000/01/rdf-schema#").text
    val entryText = text split '\n' drop 40 map (_ replaceAll (";","")) map (_.trim)
    
    val entries = entryText.toList splitFilter (_.isEmpty)
    entries foreach { entry => 

      val name = entry.head takeWhile (_ != ' ')
      val classType = entry.head dropWhile (_ != ' ') drop 3 takeWhile (_ != ' ')
      val subClassOf = getByLabel(entry, "rdfs:subClassOf")
      factCandidates += FactCandidate(classType, subClassOf, Seq(name))

      val domain = getByLabel(entry, "rdfs:domain")
      val range = getByLabel(entry, "rdfs:range")
      domain foreach (d => edgeCandidates += ((name, "rdfs:domain", d, None)))
      range foreach (r => edgeCandidates += ((name, "rdfs:range", r, None)))
    }
  }
  
  /** build a dependency map from the list of fact candidates
   */
  def buildDependencyMap() =
    dependencies = factCandidates.foldLeft(HashMap[FactCandidate, Seq[FactCandidate]]()) {
      (dependencies, candidate) =>
        val classRef = factCandidates filter (_.labels contains candidate.className)
        val superRef = factCandidates filter (_.labels contains candidate.superName)
        
        dependencies + ((candidate, classRef++superRef))
      }

  /** iterates through a dependency map and tries to build a set of complete facts
   */
  def resolveOrderedData() = {
    val it = new Iterator[FactCandidate] { 
      def next() = 
        dependencies collectFirst { case (k,v) if v.isEmpty => k } getOrElse Iterator.empty.next()
      def hasNext = dependencies exists (_._2.isEmpty)
    }
    
    var localFacts = ListBuffer.empty[Fact]  
    it foreach { next =>
      
      val nextFact = integrateFact(next, localFacts)
      localFacts += nextFact
      dependencies ++= (dependencies mapValues (_ diff Seq(next)))
    }
    
    if (dependencies.nonEmpty) throw new Exception("Dependency parsing did not work fully")
    else localFacts
  }
  
  
  def getByLabel(entry: List[String], label: String): Option[String] = 
    entry find (_ startsWith label) map extractValue
    
  def extractValue(s: String) = 
    s.dropWhile(_ != " ").takeWhile(_ != " ")
    .replaceAll("<","").replaceAll(">","").replaceAll("\"","")
}
