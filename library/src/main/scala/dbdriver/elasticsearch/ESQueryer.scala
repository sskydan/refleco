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
    
    val pf = PostFilters()
    
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

      if (normal.isEmpty && nestedQ.size < 2){
        req setQuery QueryBuilders.nestedQuery("children", nestedQ.head)
      }
      else
        nestedQ foreach (q => pf.addQueryBuilder((qb: BoolQueryBuilder) => qb must q ))
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
      QueryBuilders.matchPhraseQuery(k, v)
    }
    
    if (normal.isEmpty & nested.isEmpty) 
      req setQuery (QueryBuilders.nestedQuery("children", docFilters.head))
    
    docFilters foreach (df => pf.addQueryBuilder((qb: BoolQueryBuilder) => qb should df))
    
    if (pf.initialized)  req setPostFilter FilterBuilders.nestedFilter("children", pf.postFilter)
    
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

  case class PostFilters() {
    var initialized = false 
    val postFilter = QueryBuilders.boolQuery()
    
    def addQueryBuilder(fn: BoolQueryBuilder => BoolQueryBuilder) = {
      fn(this.postFilter)
      this.initialized = true
    }
  }
  
}