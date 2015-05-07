package dbdriver.elasticsearch

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.elasticsearch.search.sort.SortOrder
import spray.json._
import serializers.FactSerializers._
import facts.Fact
import server.LibSearchRequest
import com.typesafe.scalalogging.StrictLogging
import scala.util.control.NonFatal

/** methods responsible for inserting and updating documents in elasticsearch
 */
trait ESUpdater extends StrictLogging { self: ESQueryer with ESBase =>

  /** 
   *  @note IMPORTANT: argument given to setSource() must be a string explicitly
   *  FIXME how should we match: id+classtype or uuid
   *  TODO parent-child mappings. should be limited to certain indices?
   */
  def update(facts: Iterator[Fact], overwrite: Boolean = false): Future[Boolean] = 
    try {
      logger info "ES uploading fact batch"
      val bulkReq = client prepareBulk ()

      // get the final facts to be written
      val factsToWrite = 
        if (overwrite) Future successful facts  
        else combineWithExisting(facts)
      
      // add each fact to the bulk request
      factsToWrite map (_ foreach { fact =>
        val req = client.prepareIndex(MAIN_INDEX, fact.classType, fact.id.toString)
                        .setSource(fact.toJson.toString())
    
        // child documents need to reference their parent doc
        if ((CHILD_TYPES contains fact.classType) && fact.superType.nonEmpty) 
          req setParent fact.superType.get.toString
          
        bulkReq add req
        
      // finally, execute the bulk request
      }) map { _ =>
    
        val rep = bulkReq execute () actionGet ()
        val repFailure = rep hasFailures ()
            
        logger info s"ES uploading result error: ${repFailure.toString}"
        if (repFailure) logger error "ES uploading error: "+rep.buildFailureMessage()
        !repFailure
      }
    
    } catch { case NonFatal(any) =>
      logger error any.getMessage
      logger error any.getStackTrace().mkString("\n")
      Future successful false
    }

  /** combine the given sequence of facts with their existing representation (if any)
   */
  def combineWithExisting(facts: Iterator[Fact]) = Future sequence (
    facts map (fact => 
      
      // perform the lookup
      lookup(
        LibSearchRequest(
          queryRoot = Seq(("", "id", fact.id.toString)), 
          doctype = Seq(fact.classType)
        )
        
      // combine the returned existing facts with the new facts
      ) map (
        _.toFacts.headOption map (_ combine fact) getOrElse fact
      )
    )
  )
}
