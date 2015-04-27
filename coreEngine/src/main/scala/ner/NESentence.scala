package ner


/** represents a chunk (sentence) formed out of recognized entities
 */
case class NESentence(val row: Seq[NE], val whole: String, sc: Option[Double] = None)
extends SentenceRanker {

  // the raw sum of scores of all entities in this sentence
  val scoreSum = sc getOrElse row.foldLeft(0.0)(_ + _.score)
  
  // the total, refleco-adjusted score for the likelihood of this sentence
  lazy val rank: NESentence = rankers.foldLeft(this)( (s, ranker) => ranker(s) )    

  def updateScore(boost: Double => Double) = NESentence(row, whole, Some(boost(scoreSum)))
  override def toString() = s"ROW ($scoreSum) --\n    ${row.mkString(",\n    ")}"
}
