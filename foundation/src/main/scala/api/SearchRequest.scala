package api

/** representation of an internal search request + parameters
 *  FIXME doctype should be a list of concrete forms
 *  TODO sorting
 */
class SearchRequest(
	/** key-value pair. "key" is name of variable to test, "value" is the value
	 *     it should have. maps directly to elasticsearch fields (which are flat)
	 */
  val request: Map[String,String] = Map(),
  /** key on which to sort.
   */
  val sort: Option[String] = None,
  /** field(s) which should be included in the response
   */
  val fields: Seq[String] = Nil,
  /** doctype to search
   */
  val doctype: Seq[String] = Nil,
  /** max number of results to return (per page)
   */
  val lim: Option[Int] = None,
  /** page to fetch
   */
  val page: Option[Int] = None
) {
  
  def prettyPrint = s"Request for $request on $doctype limited to $fields sorted by $sort with limit $lim"
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
 */
class BaseParams(
  val keyParam: Option[String] = None,
  val searchParam: Option[String] = None,
  val sortParam: Option[String] = None,
  val fieldParam: Option[String] = None,
  val doctypeParam: Option[String] = None,
  val limParam: Option[Int] = None,
  val pageParam: Option[Int] = None
) extends URIParamsAdapter[SearchRequest] {
  
  val SEPARATOR = ';'
  val DEFAULT_QUERY_FIELD = "prettyLabel"
  
  /** transform the url-parameter pairs into a request object
   */
  def toRequest = {
	  def splitBySep(field: Option[String]) = field.toList flatMap (_ split SEPARATOR)
    
    // parse the separate keys and values representing the search request into a
    //   map of queries
    val request: Map[String, String] = {
      val keys = keyParam getOrElse DEFAULT_QUERY_FIELD split SEPARATOR 
      val values = splitBySep(searchParam)
      (keys zip values).toMap
    }
    
    val doctype = splitBySep(doctypeParam) map {
      case "S-1" => "company"
      case "13F-HR" => "investments"
      case other => other
    }
    
    val fields = splitBySep(fieldParam)
  
    new SearchRequest(request, None, fields, doctype, limParam, pageParam)
  }
  
  /** method to convert this request into a url-parameters (key-value pairs) string
   *  @note the keys here need to match exactly with the keys expected in the various
   *    ServiceActor APIs
   */
  def toParamsMap: Map[String,String] = Map(
	  "key" -> keyParam,
	  "search" -> searchParam,
	  "sort" -> sortParam,
	  "field" -> fieldParam, 
	  "type" -> doctypeParam,
	  "lim" -> limParam,
    "page" -> pageParam
  ) collect { case (k, Some(v)) => (k, v.toString) } 
}
