package server

import api.SearchRequest
import api.URIParamsAdapter
import api.BaseParams

/** coreEngine params are a superset of the DS params
 */
case class LibSearchRequest(
  override val request: Map[String,String] = Map(),
  override val sort: Option[String] = None,
  override val fields: Seq[String] = Nil,
  override val doctype: Seq[String] = Nil,
  override val lim: Option[Int] = None,
  override val page: Option[Int] = None
) extends SearchRequest

case class LibParams(
	override val keyParam: Option[String] = None,
	override val searchParam: Option[String] = None,
	override val sortParam: Option[String] = None,
	override val fieldParam: Option[String] = None,
	override val doctypeParam: Option[String] = None,
	override val limParam: Option[Int] = None,
  override val pageParam: Option[Int] = None
) extends BaseParams with URIParamsAdapter[LibSearchRequest] {
  
   override implicit def toRequest:LibSearchRequest = {
    val base = super.toRequest

    LibSearchRequest(
      base.request,
      base.sort,
      base.fields,
      base.doctype,
      base.lim,
      base.page
    )
  }
}
