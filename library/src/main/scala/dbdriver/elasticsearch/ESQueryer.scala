package dbdriver.elasticsearch

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.node.Node
import org.elasticsearch.node.NodeBuilder.nodeBuilder
import org.elasticsearch.search.sort.SortOrder
import org.elasticsearch.index.query._
import org.elasticsearch.index.query.QueryBuilder._
import spray.json._
import facts.Fact
import server.LibSearchRequest
import server.LibParams

/** methods responsible for querying elasticsearch
 */
trait ESQueryer { self: ESBase =>

  //TODO sort out all these prefix
  //FIXME right now we only allow for one value path. We need a way
  //to specify a correct (or many) file path(s)
  val DEFAULT_PREFIX = "children.value.valList.inner"
  val REPORT_PREFIX = DEFAULT_PREFIX + ".valDouble"
  val COMPANY_STR_PREFIX = DEFAULT_PREFIX
  val COMPANY_NUM_PREFIX = "valDouble."

  /** TODO error handling
   *  TODO search type: req.setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
   *  TODO escape lucene special chars
   *  FIXME lucene punctuation workaround
   *  FIXME use scrolling instead of paging
   *  FIXME multiple queries should be allowed with filters
   *  FIXME .asJson VS .toJson
   */ 
  def lookup(params: LibSearchRequest): Future[ESReply] = Future {
    logger info s"ES lookup: $params"
        
    val req =
      client.prepareSearch(ALL_INDICES:_*)
        .setTypes(params.doctype:_*)
        .setSize(params.lim getOrElse 2)
        .setFrom(params.page getOrElse 0)

        
    val queryRoot = params.queryRoot
    val queryFilters = params.queryFilters
    val postFilters = params.postFilters
    
    val pf = PostFilters()
    val q = Query(queryRoot)
    
    //TODO We only allow for certain AND / OR combinations. Need a way to specify
    //  which sections are to be AND'd and which to be OR'd together. (i.e must and should cases)
    //TODO Currently we are using the REPORT_PREFIX for all queries. This gives problems when looking
    //  for fields with different value paths. 
    //FIXME is there a way to make this less redundant?
    val (nested, normal) = queryFilters partition { case (f, k, v) => k startsWith "children." }
    
    def buildQueryFilter(t: (String, String, String)): BoolQueryBuilder = {
      val boolQuery = QueryBuilders.boolQuery()
      val nameKey = t._2.reverse.takeWhile(_.toString != ".").reverse
      val namePath = t._2.reverse.dropWhile(_.toString != ".").reverse.dropRight(1)
      t match {
        case (">", k, v) => {
          boolQuery must QueryBuilders.rangeQuery(REPORT_PREFIX).gt(v)
          boolQuery must QueryBuilders.matchPhraseQuery(namePath, nameKey)
        }
        case ("<", k, v) => {
          boolQuery must QueryBuilders.rangeQuery(REPORT_PREFIX).lt(v)
          boolQuery must QueryBuilders.matchPhraseQuery(namePath, nameKey)
        }
        case ("<<", k, v) => {
          boolQuery must QueryBuilders.rangeQuery(REPORT_PREFIX).lte(v)
          boolQuery must QueryBuilders.matchPhraseQuery(namePath, nameKey)
        }
        case (">>", k, v) => {
          boolQuery must QueryBuilders.rangeQuery(REPORT_PREFIX).gte(v)
          boolQuery must QueryBuilders.matchPhraseQuery(namePath, nameKey)
        }
        case ("==", k, v) => {
          boolQuery must QueryBuilders.matchPhraseQuery(k, v)
        }
      }
      boolQuery
    }
    
    nested foreach { case (f,k,v) => {
        val boolQuery = buildQueryFilter((f,k,v)) 
        q.addQueryFilter((fb: BoolFilterBuilder) => fb must FilterBuilders.nestedFilter("children", boolQuery))
      }
    }
    
    normal foreach { case (f,k,v) => {
        val boolQuery = buildQueryFilter((f,k,v))
        q.addQueryFilter((fb: BoolFilterBuilder) => fb must FilterBuilders.queryFilter(boolQuery))
      }
    }
    
    postFilters foreach {
        case ("==", k, v) => pf.addQueryBuilder((qb: BoolQueryBuilder) => qb must QueryBuilders.matchPhraseQuery(k, v))
        case _ =>
    }
    
    val fields = postFilters collect {
      case ("field", k, v) => k
    }
    if (fields.length > 0) req addFields (fields: _*)
    else req setFetchSource ("*", "details")
    
    req setQuery q.buildQuery()
    params.sort foreach (req addSort (_, SortOrder.DESC))

    //
    // query execution
    //
    logger info req.toString
    val rep = req.execute().actionGet()

    //logger info s"ES lookup ${params.request} with fields ${params.fields}, results ${rep.status()}"
    ESReply(JsonParser(rep.toString()), postFilters.toList)
  }

  
  /** build an appropriate es QueryBuilder instance from the key-value strings
   */
  private def cleanQuery(k: String, v: String): QueryBuilder =
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
  private def cleanRangeQuery(k: String, v: String): QueryBuilder = {
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
}

//TODO only looking at first query root for now. No cases where there are more
case class Query(queryRoot: Seq[(String, String, String)]){
  var qfInitialized = false
  
  val qFilters = FilterBuilders.boolFilter()

  val qRoot =
    if (queryRoot.length > 0) {
      val rootBool = QueryBuilders.boolQuery()
      queryRoot foreach {
         case ("==", k, v) => rootBool must QueryBuilders.matchPhraseQuery(k, v) 
         case (_,k,v) => rootBool must QueryBuilders.matchQuery(k, v)
      }
      rootBool
    }
    else QueryBuilders.matchAllQuery()
  
  
  def addQueryFilter(fn: BoolFilterBuilder => BoolFilterBuilder) = {
    this.qfInitialized = true
    fn(this.qFilters)  
  }
  
  def buildQuery(): QueryBuilder = {
    if (this.qfInitialized)
      QueryBuilders.filteredQuery(qRoot, this.qFilters)
    else
      qRoot
  }

}

case class PostFilters(){
  var initialized = false 
  val postFilter = QueryBuilders.boolQuery()
  
  def addQueryBuilder(fn: BoolQueryBuilder => BoolQueryBuilder) = {
    fn(this.postFilter)
    this.initialized = true
  }
}
