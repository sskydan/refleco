package stickerboard

import scalaz.Semigroup

/** FIXME aliases should be set
 */
case class Alias(
  id: String,
  aliases: Seq[String] = Nil, 
  factUUID: Option[String] = None, 
  semanticID: Option[String] = None
)

object Alias {
  implicit val aliasgroup: Semigroup[Alias] = new Semigroup[Alias] {
    def append(left: Alias, right: => Alias): Alias = 
      Alias(
        left.id,
        (left.aliases ++ right.aliases).distinct,
        left.factUUID orElse right.factUUID,
        left.semanticID orElse right.semanticID
      )
  }
}
