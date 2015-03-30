package datastructures

case class Entity(
  uuid: String = java.util.UUID.randomUUID().toString(),
  aliases: Seq[String]
) {
  
}