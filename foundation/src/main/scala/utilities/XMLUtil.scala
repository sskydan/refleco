package utilities

import java.io.File
import scala.xml.Attribute
import scala.xml.Elem
import scala.xml.MetaData
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.Text
import scala.xml.XML
import scala.xml.transform.RewriteRule
import scala.xml.transform.RuleTransformer
import scala.xml.Null

/** Central XML processing core, so that we don't get confused about the
 *  imports or multiple ways to do things.
 *
 *  This class does not support pull parsing or SAX patterns
 *  and just loads the whole thing into memory. This is because we are likely to need
 *  the whole file in all current use cases.
 */
object XMLUtil {
  def XMLtoJSON(file: File) = org.json.XML.toJSONObject(FSUtil readFile file)
  def XMLtoJSON(xml: Node) = org.json.XML.toJSONObject(xml toString ())

  def openXML(filename: String) = XML loadFile filename
  def openXML(file: File) = XML loadFile file

  /** TODO finish
   */
  def updateAttributes(root: Node, defaults: List[(String, _)]) = batchUpdates(root, defaults, updateAttrFn)

  /** TODO finish
   */
  def updateElements(root: Node, defaults: List[(String, _)]) = batchUpdates(root, defaults, updateElemFn)

  /** Match against a specific attribute's value
   *  @param att the name of the attribute
   *  @param attVal the value to try matching
   *  @param node the owner node of the attribute
   *  @return a Boolean representing the match result
   */
  def attributeMatches(att: String, attVal: String)(node: Node) = (node \ att).text == attVal

  private[utilities] def attributeFn(att: String, fn: String => Boolean)(node: Node) = fn((node \ att).text)

  private def batchUpdates(root: Node, defaults: List[(String, _)], updateFn: (String, String) => Node => Seq[Node]) = {
    val rules = defaults map { case (key, newVal) => new UpdateRule(updateFn(key, newVal.toString())) }
    object ruleBatch extends RuleTransformer(rules: _*)

    ruleBatch(root)
  }

  private class UpdateRule(transformer: Node => Seq[Node]) extends RewriteRule {
    override def transform(n: Node): Seq[Node] = transformer(n)
  }

  /** TODO finish
   */
  private def updateElemFn(key: String, newVal: String)(n: Node) = n match {
    case Elem(prefix, name, atts, scope, _*) if (name == key) =>
      Elem(prefix, name, atts, scope, true, Text(newVal))
    case other => other
  }
  /** TODO finish
   */
  private def updateAttrFn(key: String, newVal: String)(n: Node) = n match {
    case Elem(prefix, name, atts, scope, nodes @ _*) if atts.get(key).isDefined =>
      val newAtt = createAttributes(atts.asAttrMap.updated(key, newVal))
      Elem(prefix, name, newAtt, scope, true, nodes: _*)
    case other => other
  }

  /** TODO support prefixes on attribute copying
   */
  private def createAttributes(attMap: Map[String, String]): MetaData = attMap.headOption match {
    case None => Null
    case Some((key, value)) => Attribute(null, key, value, createAttributes(attMap.tail))
  }
}