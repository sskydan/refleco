package serializers

import spray.json._
import ner.NE
import stickerboard.Sticker
import stickerboard.Alias
import serializers.CustomSerializers._
import scalaz.Semigroup
import stickerboard.MergingSet
import ner.NE
import ner.NESentence

object CESerializers extends DefaultJsonProtocol {
  
  implicit def neFormat = jsonFormat4(NE)
  implicit def aliasFormat = jsonFormat4(Alias.apply)
  implicit def mergesetFormat[A:Semigroup:JsonFormat] = jsonFormat1(MergingSet[A])
  implicit def stickerFormat: RootJsonFormat[Sticker] = jsonFormat3(Sticker.apply)
  
  implicit def neSentenceFormat: RootJsonFormat[NESentence] = new RootJsonFormat[NESentence] {
    def write(s: NESentence) = 
      JsObject("score" -> JsNumber(s.score), "entities" -> s.row.toJson)
      
    def read(json: JsValue) = json match {
      case obj: JsObject => obj.getFields("score","entities") match {
        case Seq(JsNumber(score), js) => 
          NESentence(js.convertTo[Seq[NE]], "", Some(score.toDouble))
      }
      case _ => throw new UnsupportedOperationException("This NESentence can't be parsed "+json)
    }
  }
}