package ner


/** represents a chunk (sentence) formed out of recognized entities
 */
case class NESentence(val row: Seq[NE], val whole: String, sc: Option[Double] = None)
extends NESRanker {
  
	// the base score of this sentence
  val score = sc getOrElse 0.0
  
  // the total, refleco-adjusted score for the likelihood of this sentence
  lazy val rank: NESentence = rankers.foldLeft(this)( (s, ranker) => ranker(s) )    

  def updateScore(boost: Double => Double) = NESentence(row, whole, Some(boost(score)))
  override def toString() = s"ROW ($score) --\n    ${row.mkString(",\n    ")}"
}
