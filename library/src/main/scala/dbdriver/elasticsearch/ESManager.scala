package dbdriver.elasticsearch

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.elasticsearch.search.sort.SortOrder
import facts.Fact
import spray.json._
import spray.json.DefaultJsonProtocol._
import utilities.JsonUtil
import dbdriver.DataServerManager
import dbdriver.DataServerReply
import serializers.FactSerializers._
import scala.util.Try
import server.LibSearchRequest
import server.LibParams
import scala.util.control.NonFatal
import com.typesafe.scalalogging.StrictLogging
import org.elasticsearch.index.query.FilterBuilders
import api.EntityIndex
import facts.FactNone
import org.elasticsearch.index.query.QueryBuilder

/** Class for initializing and managing an elasticsearch node cluster
 *  FIXME make data-node creation explicit; otherwise use dataless-node
 *   or transport client to try to connect to existing nodes. + update shutdown
 *  @note uses elasticsearch.yml config in src/main/resources
 */
trait ESManager extends DataServerManager with StrictLogging {
  val MAIN_INDEX = "finbase"
  val DICT_INDEX = "dicts"
  val COMPANY = "company"
  val R10K = "10-K"
  val R10Q = "10-Q"
  val ANA = "analytics"
  val INV = "investments"
  
  val ALL_INDICES = List(MAIN_INDEX, DICT_INDEX)
  val CHILD_TYPES = List(R10K, R10Q, ANA, INV)

  // Clusters are composed of nodes; nodes can be either data-carrying or simply router-style
  private lazy val node = nodeBuilder node ()

  // client is our gateway to the node
  private lazy val client = {
    val c = node client ()
    c.admin().cluster().prepareHealth().setWaitForYellowStatus().execute().actionGet()
    c
  }

  initializeMappings()

  /** Initialize an elasticsearch node
   */
  override def initDS = client

  override def updateDS(facts: Iterator[Fact]): Future[Boolean] = try {
    logger info "ES updating fact batch"
    val bulkReq = client prepareBulk ()

    val futures = facts map { fact => 
      val checkExisting = lookupDS(LibSearchRequest(
        request = Map("id" -> fact.id), 
        doctype = Seq(fact.ftype))
      )
      val newDoc = checkExisting map (
        _.toFacts find (_.interest >= 6.0) map (_ integrateFacts fact) getOrElse fact
      )
      
      newDoc map { fact =>
        val req = client.prepareIndex(MAIN_INDEX, fact.ftype, fact.id)
                        .setSource(fact.toJson.toString())
  
        // child documents need to reference their parent doc
        if (CHILD_TYPES contains fact.ftype) req setParent fact.id.takeWhile(_ != ':')
        
        bulkReq add req
      }
    }
    
    Future sequence futures map { _ =>
    
      // query execution
      val rep = bulkReq execute () actionGet ()
      val repFailure = rep hasFailures ()
          
      logger info s"ES uploading result error: ${repFailure.toString}"
      if (repFailure) logger error "ES uploading error: "+rep.buildFailureMessage()
      !repFailure
    }

  } catch {
    case NonFatal(any) =>
      logger error any.getMessage
      logger error any.getStackTrace().mkString("\n")
      Future successful false
  }
  
  
  /** FIXME was using wrong parent attribute in last version
   *  @note IMPORTANT: argument given to setSource() must be a string explicitly
   */
  override def uploadDS(payload: Iterator[Fact]): Future[Boolean] = Future {
    try {
      logger info "ES uploading fact batch"

      val bulkReq = client prepareBulk ()

      payload foreach { fact =>
        val req = client.prepareIndex(MAIN_INDEX, fact.ftype, fact.id)
                        .setSource(fact.toJson.toString())

        // child documents need to reference their parent doc
        if (CHILD_TYPES contains fact.ftype) req setParent (fact.id.takeWhile(_ != ':'))

        bulkReq add req
      }

      // query execution
      val rep = bulkReq execute () actionGet ()
      val repFailure = rep hasFailures ()

      logger debug s"ES uploading result ${repFailure.toString}"
      if (repFailure) logger error s"ES uploading error: ${rep.buildFailureMessage()}"
      !repFailure

    } catch {
      case NonFatal(any) =>
        logger error any.getMessage
        logger error any.getStackTrace().mkString("\n")
        false
    }
  }

  
  /** build an appropriate es QueryBuilder instance from the key-value strings
   */
  def cleanQuery(k: String, v: String): QueryBuilder =
    // the double-colon case is a temporary workaround for lucene's punctuation handling
    if (v contains "::") 
      QueryBuilders.queryString(v replaceAll ("::", " AND ")).field(k)
      
    // covers the range filters
    else if ((v contains "<") || (v contains ">")) 
      cleanRangeQuery(k,v)

    // queries prefixed by == should be treated as "exact match" queries
    else if (v contains "==")
      QueryBuilders.matchPhraseQuery(k, v replaceAll ("=", ""))
    
    // covers normal match queries 
    else QueryBuilders.matchQuery(k, v)
  
    
  /** initialise a RangeQueryBuilder from our custom value strings
   *  samples:
   *    ("debt and liabilities", "valList.inner.valDouble.>10,<40")
   */
  def cleanRangeQuery(k: String, v: String): QueryBuilder = {
    val PATH_SEPARATOR = '.'
    val INNER_VALUE_SEPARATOR = ','
    
    def extractValuePath(v: String) = {
      val (value, rawPrefix) = v.reverse span (_ != PATH_SEPARATOR)
      val prefix = if (rawPrefix.isEmpty) "" else s".${rawPrefix.drop(1).reverse}"
      prefix -> value.reverse
    }
    
    val (vPrefix, vValue) = extractValuePath(v)
    val rangeQ = QueryBuilders.rangeQuery("children.value"+vPrefix)
    
    vValue split INNER_VALUE_SEPARATOR map ( part =>
      if (part startsWith ">>") rangeQ.from((part drop 2).toDouble).includeLower(true)
      else if (part startsWith ">") rangeQ.from((part drop 1).toDouble).includeLower(false)
      else if (part startsWith "<<") rangeQ.to((part drop 2).toDouble).includeUpper(true)
      else if (part startsWith "<") rangeQ.to((part drop 1).toDouble).includeUpper(false)
    )
    
    val nameQ = cleanQuery("children.prettyLabel", "==" + (k replaceAll ("children.",""))) 
    
    QueryBuilders.boolQuery() must rangeQ must nameQ
  }

  
  /** TODO error handling
   *  TODO search type: req.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
   *  TODO escape lucene special chars
   *  FIXME lucene punctuation workaround
   *  FIXME use scrolling instead of paging
   *  FIXME multiple queries should be allowed with filters
   *  FIXME .asJson VS .toJson
   */
  override def lookupDS(params: LibSearchRequest): Future[ESReply] = Future {
    logger info s"ES lookup: $params"

    val req =
      client.prepareSearch(ALL_INDICES:_*)
        .setTypes(params.doctype:_*)
        .setSize(params.lim getOrElse 2)
        .setFrom(params.page getOrElse 0)

    //
    // query string handling
    //
    val (nested, normal) = params.request partition { case (k, v) => k startsWith "children." }
    
    // handle nested queries
    if (!nested.isEmpty) {
      val nestedQ = nested map { case (k, v) => cleanQuery(k, v) }

      if (normal.isEmpty && nestedQ.size < 2)
        req setQuery QueryBuilders.nestedQuery("children", nestedQ.head)
      else
        nestedQ map (q => req setPostFilter FilterBuilders.nestedFilter("children", q))
    }

    // handle regular queries
    if (normal.size > 1) logger error "Multiple regular(non-nested) queries not implemented"
    normal foreach { case (k, v) => req setQuery cleanQuery(k, v) }

    //
    // select fields to be returned
    //

    val (postReplyFilters, esFilters) = params.fields partition (_ startsWith "children.")
    val (ignoredFields, chosenFields) = esFilters partition (_ startsWith "-")
        
    val docFilters = postReplyFilters map { f =>
      val filterList = f split "="
      val k = filterList.head
      val v = filterList.tail.head
      FilterBuilders nestedFilter ("children", QueryBuilders.matchPhraseQuery(k, v))
    }
    if (docFilters.length > 0) req setPostFilter {
    	val boolF = FilterBuilders.boolFilter()
			docFilters foreach (boolF should _)
			boolF
    }
    
    if (chosenFields.length > 0)
      req addFields (chosenFields: _*)
    else if (ignoredFields.length > 0)
      req setFetchSource (
        Array("*"),
        ("details" +: ignoredFields).toArray map (_ replaceFirst ("-", ""))
      )
    else
      req setFetchSource ("*", "details")

    //
    // handle sorts
    //
    params.sort foreach (req addSort (_, SortOrder.DESC))

    //
    // query execution
    //
    logger info req.toString
    val rep = req.execute().actionGet()

    logger info s"ES lookup ${params.request} with fields ${params.fields}, results ${rep.status()}"
    ESReply(JsonParser(rep.toString()), postReplyFilters.toList)
  }

  /** Setup some initial mapping configurationa
   *  - Set CHILD_TYPES as children documents to the COMPANY doctype
   *  - mark company children facts as nested documents
   *  - mark children facts as nested documents
   *  FIXME non-hardcode "company" in the childMapping
   *  TODO properly mark all fact-children as "nested" types
   */
  def initializeMappings() = {
    val parentMapping = """{
      "properties": {
        "children": {
          "type": "nested"
        },
        "children.children": {
          "type": "nested"
        }
      }
    }""".parseJson.toString()

    val childMapping = """{
      "properties": {
        "children": {
          "type": "nested"
        },
        "children.children": {
          "type": "nested"
        }
      },
      "_parent": {
        "type": "company"
      }
    }""".parseJson.toString()

    // prepare the dict index
    val dIndexExists = client.admin().indices().prepareExists(DICT_INDEX).execute().actionGet().isExists()
    if (!dIndexExists) client.admin().indices().prepareCreate(DICT_INDEX).execute().actionGet()
    
    // prepare the main index
    val indexExists = client.admin().indices().prepareExists(MAIN_INDEX).execute().actionGet().isExists()
    if (!indexExists) client.admin().indices().prepareCreate(MAIN_INDEX).execute().actionGet()

    val clusterState = client.admin().cluster().prepareState().execute().actionGet()
    val mappings = clusterState.getState().metaData().index(MAIN_INDEX).mappings()

    def putMapping(name: String) = if (!mappings.containsKey(name))
      client.admin().indices()
        .preparePutMapping(MAIN_INDEX)
        .setType(name)
        .setSource(if (CHILD_TYPES contains name) childMapping else parentMapping)
        .execute.actionGet()

    COMPANY :: CHILD_TYPES map putMapping
  }

  override def shutdownDS() = if (node != null) node close ()
}


