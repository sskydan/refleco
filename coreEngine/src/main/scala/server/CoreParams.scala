package server

import analytics.Analytic._
import api.SearchRequest
import analytics.Analytic
import api.URIParamsAdapter
import api.BaseParams

/** coreEngine params are a superset of the DS params
 */
case class CoreSearchRequest(
  override val queryRoot:Seq[(String,String,String)] = Nil,
  override val queryFilters:Seq[(String,String,String)] = Nil,
  override val postFilters:Seq[(String,String,String)] = Nil,
  override val sort: Option[String] = None,
  override val doctype: Seq[String] = Nil,
  override val lim: Option[Int] = None,
  override val page: Option[Int] = None,
  ranking:Option[String] = None,
  analytics:Seq[Analytic] = Nil
) extends SearchRequest

case class CoreParams(
  override val queryRootFuncs:Option[String] = None,
  override val queryRootKeys:Option[String] = None,
  override val queryRootVals:Option[String] = None,
  override val queryFilterFuncs:Option[String] = None,
  override val queryFilterKeys:Option[String] = None,
  override val queryFilterVals:Option[String] = None,
  override val postFilterFuncs:Option[String] = None,
  override val postFilterKeys:Option[String] = None,
  override val postFilterVals:Option[String] = None,
	override val sortParam: Option[String] = None,
	override val doctypeParam: Option[String] = None,
	override val limParam: Option[Int] = None,
  override val pageParam: Option[Int] = None,
	rankingParam: Option[String] = None,
	analyticsParam: Option[String] = None
) extends BaseParams with URIParamsAdapter[CoreSearchRequest] {
  
  override implicit def toRequest:CoreSearchRequest = {
  	val base = super.toRequest
    val analytics = analyticsParam match {
      case Some("all") => Analytic.ALL
      case _ => Nil
    }
    
    CoreSearchRequest(
      base.queryRoot,
      base.queryFilters,
      base.postFilters,
      base.sort,
      base.doctype,
      base.lim,
      base.page,
      rankingParam,
      analytics
    )
  }
}
