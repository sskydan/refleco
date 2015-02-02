package serializers

import spray.json._
import ner.NE
import stickerboard.Sticker
import stickerboard.Alias
import serializers.CustomSerializers._
import scalaz.Semigroup
import stickerboard.MergingSet

object CESerializers extends DefaultJsonProtocol {
  
  implicit def neFormat = jsonFormat4(NE)
  implicit def aliasFormat = jsonFormat4(Alias.apply)
  implicit def mergesetFormat[A:Semigroup:JsonFormat] = jsonFormat1(MergingSet[A])
  implicit def stickerFormat: RootJsonFormat[Sticker] = jsonFormat3(Sticker.apply)
}