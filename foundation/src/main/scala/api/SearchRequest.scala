package api

/** representation of an internal search request + parameters
 */
class SearchRequest(
	/** key-value pair. "key" is name of variable to test, "value" is the value it should have. maps directly
	 *    to elasticsearch fields (which are flat)
	 */
  val request: Map[String,String] = Map(),
  /** key on which to sort.
   *  TODO
   */
  val sort: Option[String] = None,
  /** field(s) which should be included in the response
   *  TODO support more than one field
   */
  val fields: Seq[String] = Nil,
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
  def prettyPrint = s"Request for $request on $doctype limited to $fields sorted by $sort with limit $lim"
}

/** typeclass for defining the transformation to requests
 *  FIXME something not quite right with this pattern
 *  TODO move paramMap to BaseParams?
 */
trait URIParamsAdapter[+T<:SearchRequest] {
  implicit def toRequest: T
  def toParamsMap: Map[String,String]
}
object URIParamsAdapter {
  implicit def toRequest[T<:SearchRequest](params: URIParamsAdapter[T]): T = 
    params.toRequest
}

/** Formatting and preparation of supported parameters to search ops.
 *  @note parameter defaults in the method signature will be overwritten by spray-routing
 *  FIXME doctype should be Form not String
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
  
  def toRequest = {
    val request: Map[String, String] = {
      val keys = keyParam getOrElse "prettyLabel" split SEPARATOR 
      val values = searchParam.toList flatMap (_ split SEPARATOR)
      (keys zip values).toMap
    }
    val sort = sortParam map { case "date" => "value" }
    val doctype = doctypeParam map (_.split(SEPARATOR).toList) getOrElse Seq("10-K") map {
      case "S-1" => "company"
      case "13F-HR" => "investments"
      case other => other
    }
    val fields = fieldParam map (_.split(SEPARATOR).toSeq) getOrElse Nil
  
    new SearchRequest(request, sort, fields, doctype, limParam, pageParam)
  }
  
  def toParamsMap: Map[String,String] = Map(
	  "key" -> keyParam,
	  "search" -> searchParam,
	  "sort" -> sortParam,
	  "field" -> fieldParam, 
	  "type" -> doctypeParam,
	  "lim" -> limParam,
    "page" -> pageParam
  ) collect {case (k, Some(v)) => (k, v.toString)} 
}
