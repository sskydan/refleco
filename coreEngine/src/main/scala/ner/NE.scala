package ner

/** represents a recognized named entity.
 *  FIXME equals shouldn't be necessary once distinct check is gone
 */
case class NE(entity: String, genus: String, score: Double, raw: String) {
  
  override def equals(any: Any) = any match {
    case NE(e,g,_,r) => e==entity && g==genus && r==raw
    case _ => false
  }
}
