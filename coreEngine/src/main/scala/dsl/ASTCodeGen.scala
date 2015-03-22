package dsl

import scala.concurrent.Future
import scala.concurrent.duration._
import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import facts.Fact
import server.CoreParams
import server.Reception
import server.Reception._
import com.typesafe.scalalogging.StrictLogging
import akka.actor.ActorSystem
import stickerboard.Board
import stickerboard.Clue
import stickerboard.Sticker._
import scala.util.Try

object ASTCodeGen extends StrictLogging {
  implicit val timeout = Timeout(10.hours)
  implicit def listToOpt(list: Iterable[String]): Option[String] = 
    if (!list.isEmpty) Some(list mkString ";")
    else None
  
  def generate(query: String)(implicit system: ActorSystem): Future[Seq[Fact]] = {
    implicit def ec = system.dispatcher

    val root = Reflask.parseQuestion(query)
    logger info s"DSL search tree: $root"
    
    val size = root.lim map (_.value)
    
    root.qtype.value match {
      case "company" =>
        val data = getQueryData(root.paths, size) :: Nil
        Future.reduce(data)(_ ++ _) 
        
      case "entity" => 
        val data = Board.find(root.paths.head.root.get.value).toList map s2fact
        Future.successful(data) 
        
      case _ => throw new Exception(s"Unknown arguments in DSL query $root")
    }
  }
    
     
    def getQueryData(paths: Seq[PathNode], lim: Option[Int] = None)(implicit system: ActorSystem) = {
    if (isQueryRequested(paths)) {
      implicit def ec = system.dispatcher
      def reception = system.actorOf(Reception.props())
      
      
      val (queryRootFuncs, queryRootKeys, queryRootVals) = getQueryRoot(paths).unzip3
      val (queryFilterFuncs, queryFilterKeys, queryFilterVals) = getQueryFilters(paths).unzip3
      val (postFilterFuncs, postFilterKeys, postFilterVals) = getPostFilters(paths).unzip3
      
      val reportReq = CoreParams(
                        queryRootFuncs, 
                        queryRootKeys, 
                        queryRootVals,
                        queryFilterFuncs, 
                        queryFilterKeys, 
                        queryFilterVals,
                        postFilterFuncs,
                        postFilterKeys,
                        postFilterVals,
                        None, 
                        Some("10-K"), 
                        lim, 
                        None, 
                        None)
      logger info s"DSL search generated (reports): ${reportReq.prettyPrint}"
      val reportRes = (reception ? SearchRequest(reportReq)).mapTo[Seq[Fact]]
      
      Future.reduce(reportRes :: Nil)(_ ++ _)
    }
    else Future.successful(Nil)
  }
    
    
  def isQueryRequested(paths: Seq[PathNode]) = paths exists {
    case PathNode(_, Some(AttributeSelectorNode(_,_))) => true
    case PathNode(_, Some(UnstructuredSelectorNode(_))) => true
    case PathNode(_, None) => true
    case _ => false
  }
    
  def getQueryRoot(paths: Seq[PathNode]): Seq[(String, String, String)] = paths.head.root match {
    case Some(all) if all == "*" => Nil
    case Some(name) => Seq(("==", "prettyLabel", name.value))
    case None => Nil
  }
  
  def getQueryFilters(paths: Seq[PathNode]): Seq[(String, String, String)] = paths.collect {
    case PathNode(_, Some(AttributeSelectorNode(field, fns))) if !fns.isEmpty =>
      val key = field.value.toString
      val values = fns.map(fn => (fn.fn.name, "children.prettyLabel." + key, fn.args.value.toString))
      values
    case PathNode(_, Some(AttributeSelectorNode(field, Nil))) => Seq(("==", "children.prettyLabel", field.value))
  }.flatten
  
  def getPostFilters(paths: Seq[PathNode]): Seq[(String, String, String)] = paths collect {
    case PathNode(_, Some(AttributeSelectorNode(field, Nil))) => ("==", "children.prettyLabel", field.value)
    case PathNode(_, Some(UnstructuredSelectorNode(field))) => ("==", "children.value", field.value)
  }
  /*
    //--------------------------------------------------------------------------------------------
  
  def getUnstructuredData(paths: Seq[PathNode], lim: Option[Int] = None)(implicit system: ActorSystem) = 
    if (isUnstructuredRequested(paths)) {
      implicit def ec = system.dispatcher
      def reception = system.actorOf(Reception.props())
      
      val fieldParam = getUnstructuredFieldFilters(paths)
      
      val (keyParamUS, searchParamUS) = (getNameFilter(paths) ++ getUnstructuredFilters(paths)).unzip
      val unstructuredReq = CoreParams(keyParamUS, searchParamUS, None, fieldParam, Some("10-K"), lim, None, None)
      logger info s"DSL search generated (Unstructured): ${unstructuredReq.prettyPrint}"
      val unstructuredRes = (reception ? SearchRequest(unstructuredReq)).mapTo[Seq[Fact]]
      
      Future.reduce(unstructuredRes :: Nil)(_ ++ _)
    } else
      Future.successful(Nil)
  
  def isUnstructuredRequested(paths: Seq[PathNode]) = paths exists {
    case PathNode(_, Some(UnstructuredSelectorNode(_))) => true
    case _ => false
  }
   
  def getUnstructuredFieldFilters(paths: Seq[PathNode]) = paths collect {
    case PathNode(_, Some(UnstructuredSelectorNode(field))) => "children.children.value="+field.value
  }
  
  //TODO Fix this shit
  def getUnstructuredFilters(paths: Seq[PathNode]) = paths.collect {
    case PathNode(_, Some(UnstructuredSelectorNode(field))) => 
      val typeKey = "children.children.ftype"
      val typeValue = "xbrl:unstructured"
      val unstructKey = "children.children.value"
      val unstructValue = field.value
      List(typeKey -> typeValue, unstructKey -> unstructValue)
  }.flatten.toMap
  
  //--------------------------------------------------------------------------------------------
 
  val DEFAULT_PREFIX = "valList.inner."
  val REPORT_PREFIX = DEFAULT_PREFIX + "valDouble."
  val COMPANY_STR_PREFIX = DEFAULT_PREFIX
  val COMPANY_NUM_PREFIX = "valDouble."
  
  def getReportData(paths: Seq[PathNode], lim: Option[Int] = None)(implicit system: ActorSystem) = 
    if (isReportRequested(paths)) {
      implicit def ec = system.dispatcher
      def reception = system.actorOf(Reception.props())
      
      val fieldParam = getFieldFilters(paths)
            
      val (keyParamR, searchParamR) = (getNameFilter(paths) ++ getSearchFilters(paths)).unzip
      val reportReq = CoreParams(keyParamR, searchParamR, None, fieldParam, Some("10-K"), lim, None, None)
      logger info s"DSL search generated (reports): ${reportReq.prettyPrint}"
      val reportRes = (reception ? SearchRequest(reportReq)).mapTo[Seq[Fact]]
      
      val (keyParamC, searchParamC) = (getNameFilter(paths) ++ getSearchFiltersCompany(paths)).unzip
      val companyReq = CoreParams(keyParamC, searchParamC, None, fieldParam, Some("company"), lim, None, None)
      logger info s"DSL search generated (companies): ${companyReq.prettyPrint}"
      val companyRes = (reception ? SearchRequest(companyReq)).mapTo[Seq[Fact]]
      
      Future.reduce(reportRes :: companyRes :: Nil)(_ ++ _)
    } else
      Future.successful(Nil)
  
  def isReportRequested(paths: Seq[PathNode]) = paths exists {
    case PathNode(_, Some(AttributeSelectorNode(_,_))) => true
    case PathNode(_, None) => true
    case _ => false
  }
  
  def getNameFilter(paths: Seq[PathNode]) = paths.head.root match {
    case Some(all) if all == "*" => Map()
    case Some(name) => Map(("prettyLabel", "=="+name.value))
    case None => Map()
  }
  
  def getFieldFilters(paths: Seq[PathNode]) = paths collect {
    case PathNode(_, Some(AttributeSelectorNode(field, Nil))) => "children.prettyLabel="+field.value
  }
  
  def getSearchFilters(paths: Seq[PathNode]) = paths.collect {
    case PathNode(_, Some(AttributeSelectorNode(field, fns))) if !fns.isEmpty =>
      val key = "children."+field.value
      val values = fns.map(fn => (fn.fn.name, fn.args.value)).mkString(",") 
      
      key -> values

  }.toMap
  
  def getSearchFiltersCompany(paths: Seq[PathNode]) = paths.collect {
    case PathNode(_, Some(AttributeSelectorNode(field, fns))) if !fns.isEmpty =>
      val prefix = 
        if (Try(BigDecimal(fns.head.args.value.toString)).isSuccess) COMPANY_NUM_PREFIX
        else COMPANY_STR_PREFIX
      
      val key = "children."+field.value 
      val values = fns.map(fn => fn.fn.name + fn.args.value).mkString(",")
      
      key -> (prefix + values)
      
  }.toMap
  
  //--------------------------------------------------------------------------------------------
  
  def getRelationshipData(paths: Seq[PathNode]) = 
    paths.head.root map { rootName =>
      val entityFilters = getRelationshipFilters(paths)
      
      val res = 
        if (entityFilters.size > 0)
          Board.find(rootName.value) map { root =>
            entityFilters flatMap (root \~ _) map s2fact
          }
        else None
      
      Future.successful(res getOrElse Nil)
    } getOrElse Future.successful(Nil)
  
  def getRelationshipFilters(paths: Seq[PathNode]) = paths collect {
    case PathNode(_, Some(RelationSelectorNode(field))) => Clue(field.value)
  }   
    
  //-------------------------------------------------------------------------------------------- 
  */

   
    
}
