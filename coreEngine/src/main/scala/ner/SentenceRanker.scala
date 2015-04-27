package ner

import extensions.Extensions._

/** represents scoring rules on named entity sentences
 */
trait BoostRule extends (NESentence => NESentence)

/** boost a sentence based on if it matches a criteria
 */
case class BooleanBoostRule(matcher: NESentence => Boolean, boost: Double => Double)
extends BoostRule {
  
  def apply(sentence: NESentence) =
    if (matcher(sentence)) sentence updateScore boost
    else sentence
}
/** boost a sentence based on how many times it matches a criteria
 */
case class FindingBoostRule(matcher: NESentence => Int, boost: Double => Double => Double)
extends BoostRule {
  
  def apply(sentence: NESentence) = {
    val matchCount = matcher(sentence)
    
    if (matchCount > 0) sentence updateScore boost(matchCount)
    else sentence
  }
}

/** class that handles scoring of ne sentence possibilities
 */
trait SentenceRanker { self: NESentence =>
  
  val rankers: Seq[BoostRule] = Seq(
    // normalize the multi-word chunks scores
    FindingBoostRule( 
      sentence => sentence.row.foldLeft(0) { case (sum, chunk) => 
        val chunkSizeBuff = chunk.raw.count(_ == ' ')
        sum + chunkSizeBuff*chunkSizeBuff
      },
      count => _ * count * 1.5
    ),
    // boost full sentences
    BooleanBoostRule(_.row.map(_.raw).mkString(" ") == whole, _ * 1.4),
    // first entity likelihoods
    BooleanBoostRule(_.row.head.genus == "company", _ * 1.3),
    BooleanBoostRule(_.row.head.genus == "entity", _ - 5),
    // companies are preferred?
    BooleanBoostRule(_.row.exists(_.genus == "company"), _ + 5),
    // companies are likely to be followed by
    FindingBoostRule(sentence => 
      sentence.row.countSlicesLike(Seq(
        _.genus == "company", 
        _.genus == "attribute")),
      count => _ + count*10
    ),
    FindingBoostRule(sentence => 
      sentence.row.countSlicesLike(Seq(
        _.genus == "company", 
        _.genus == "relation")),
      count => _ + count*10
    )
  )
}