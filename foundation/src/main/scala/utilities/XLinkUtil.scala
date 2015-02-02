package utilities

import scala.xml.NodeSeq
import scala.xml.Node
import XMLUtil._

trait XMLNS {
  val XLINK_NS = "http://www.w3.org/1999/xlink"

  def xl(value:String) = "@{"+XLINK_NS+"}"+value
  def xl(root:Node) = <pre xmlns:xlink={XLINK_NS}>{root}</pre>

  val FROM = xl("from")
  val TO = xl("to")
  val HREF = xl("href")
  val LABEL = xl("label")
  val TYPE = xl("type")
  val TYPE_ARC = "arc"
  val TYPE_LOC = "locator"
  val TYPE_RESOURCE = "resource"
}

/**
  * NOTE: \\ will return all possible matches. Ie, a containing elem AND one of its chidren if both match
  * TODO its a bit ugly... better techniques? http://www.codecommit.com/blog/scala/working-with-scalas-xml-support
  */
object XLinkUtil extends XMLNS {

  private def keyAsString(key:String) = key replaceAll(":", "_")
  
  /**
   * Follow the xlink arc reference to the first matching node
   * @param key a String representing the key to look for
   * @param file the NodeSeq root node from which to lookup arcs
   * @return an Option[String] containing the value of the xlink
   */
  def resolveResource(rawKey:String, nroot:Node):Option[String] = resolveResources(rawKey, nroot).headOption
  
  /**
   * Follow the xlink arc reference to all matching nodes
   * @param key a String representing the key to look for
   * @param file the NodeSeq root node from which to lookup arcs
   * @return a sequence containing the value of the matching xlinks
   */
  def resolveResources(rawKey:String, nroot:Node): Seq[String] = {
    // Hack, otherwise the xlink attributes won't be searchable 
    // TODO reference the provided namespace
    val root = xl(nroot) \\ "_"
    val key = keyAsString(rawKey)
    
    resolveDirectResource(root)(key) ++ resolveLocatorLink(root)(key) map (_.text)
  }
  
  private def resolveDirectResource(root:NodeSeq)(key:String): Seq[Node] = 
    root filter attributeMatches(TYPE, TYPE_RESOURCE) filter attributeMatches(LABEL, key)
  
  private def resolveArcLink(root:NodeSeq)(key:String): Seq[Node] = {
    val arcs = root filter attributeMatches(TYPE, TYPE_ARC)
    val arcValue = arcs filter attributeMatches(FROM, key) map(_ \ TO) map (_.text)
    
    arcValue flatMap resolveDirectResource(root)
  }
  
  private def resolveLocatorLink(root:NodeSeq)(key:String): Seq[Node] = {
    val locs = root filter attributeMatches(TYPE, TYPE_LOC)
    val locValue = locs filter attributeFn(HREF, hrefMatcher(key)) map(_ \ LABEL) map (_.text)
    
    locValue flatMap resolveArcLink(root)
  }

  // TODO hacky locator matching
  private def hrefMatcher(key:String)(attVal:String):Boolean = attVal.endsWith("#"+key)
}