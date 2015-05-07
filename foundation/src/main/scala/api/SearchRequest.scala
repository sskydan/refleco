package api

import shapeless._
import poly._
import syntax.std.tuple._

/** representation of an internal search request + parameters
 *  FIXME doctype should be a list of concrete forms
 *  TODO sorting
 */
class SearchRequest(

   /** sequence of triples(func, key, value) where key is a variable to test and value is value to test against.
   *   func is the type of test (i.e. =, <, >, etc). Applies to main query parameters.
   */
  val queryRoot: Seq[(String,String,String)] = Nil,
  
  /** sequence of triples(func, key, value) where key is a variable to test and value is value to test against.
   *   func is the type of test (i.e. =, <, >, etc). Applies to main query filter parameters.
   */
  val queryFilters: Seq[(String,String,String)] = Nil,
   
  /** sequence of triples(func, key, value) where key is a variable to test and value is value to test against.
   *   func is the type of test (i.e. =, <, >, etc). Applies to post query filter parameters.
   */
  val postFilters: Seq[(String,String,String)] = Nil,
  
  /** key on which to sort.
   *  TODO
   */
  val sort: Option[String] = None,
  
  /** doctype to search. maps directly to es document type
   */
  val doctype: Seq[String] = Nil,
  
  /** max number of results to return (per page)
   */
  val lim: Option[Int] = None,
  
  /** page to fetch
   */
  val page: Option[Int] = None
) {
  
  def prettyPrint = s"Request for $queryRoot filtered on $queryFilters in $doctype limited to $postFilters sorted by $sort with limit $lim"
}

/** typeclass for defining the transformation to requests
 *  FIXME something not quite right with this pattern
 *  TODO move paramMap to BaseParams?
 */
trait URIParamsAdapter[+T <: SearchRequest] {
  implicit def toRequest: T
  def toParamsMap: Map[String,String]
}
object URIParamsAdapter {
  implicit def toRequest[T <: SearchRequest](params: URIParamsAdapter[T]): T = 
    params.toRequest
}

/** Formatting and preparation of supported parameters to search ops.
 *  @note parameter defaults in the method signature will be overwritten by spray-routing
 *  FIXME doctype should be Form not String
 */
class BaseParams(
  val queryRootFuncs: Option[String] = None,
  val queryRootKeys: Option[String] = None,
  val queryRootVals: Option[String] = None,
  val queryFilterFuncs: Option[String] = None,
  val queryFilterKeys: Option[String] = None,
  val queryFilterVals: Option[String] = None,
  val postFilterFuncs: Option[String] = None,
  val postFilterKeys: Option[String] = None,
  val postFilterVals: Option[String] = None,
  val sortParam: Option[String] = None,
  val fieldParam: Option[String] = None,
  val doctypeParam: Option[String] = None,
  val limParam: Option[Int] = None,
  val pageParam: Option[Int] = None
) extends URIParamsAdapter[SearchRequest] {
  
  val SEPARATOR = ';'

  def toRequest = {
    def combine(fns: Option[String], keys: Option[String], vals: Option[String]): Seq[(String,String,String)] = {
      object splitOnSep extends (Option[String] -> Seq[String])(_ map (_.split(SEPARATOR).toSeq) getOrElse Nil)
      ((fns, keys, vals) map splitOnSep).zipped.toSeq
      
      /*
      val f = fns map(_ split SEPARATOR)
      val k = keys map(_ split SEPARATOR) 
      val v = vals map(_ split SEPARATOR)
      
      //TODO do some scala magic to make this if nice
      if (f.isDefined && k.isDefined && v.isDefined) {
        (f.get, k.get, v.get).zipped.toList 
      }
      else Nil*/
    } 
    
    val queryRoot: Seq[(String,String,String)] = combine(queryRootFuncs, queryRootKeys, queryRootVals) 
    val queryFilters: Seq[(String,String,String)] = combine(queryFilterFuncs, queryFilterKeys, queryFilterVals)
    val postFilters: Seq[(String,String,String)] = combine(postFilterFuncs, postFilterKeys, postFilterVals)     
  
    val sort = sortParam map { case "date" => "value" }
    val doctype = doctypeParam map (_.split(SEPARATOR).toList) getOrElse Seq("10-K") map {
      case "S-1" => "company"
      case "13F-HR" => "investments"
      case other => other
    }

    new SearchRequest(queryRoot, queryFilters, postFilters, sort, doctype, limParam, pageParam)
  }
  
  /** method to convert this request into a url-parameters (key-value pairs) string
   *  @note the keys here need to match exactly with the keys expected in the various
   *    ServiceActor APIs
   */
  def toParamsMap: Map[String,String] = Map(
    "rootFunc" -> queryRootFuncs,
    "rootKey" -> queryRootKeys,
    "rootVal" -> queryRootVals,
    "qFilterFuncs" -> queryFilterFuncs,
    "qFilterKeys" -> queryFilterKeys,
    "qFilterVals" -> queryFilterVals,
    "pFilterFuncs" -> postFilterFuncs,
    "pFilterKeys" -> postFilterKeys,
    "pFilterVals" -> postFilterVals,
	  "sort" -> sortParam,
	  "field" -> fieldParam, 
	  "type" -> doctypeParam,
	  "lim" -> limParam,
    "page" -> pageParam
  ) collect { case (k, Some(v)) => (k, v.toString) } 
}
