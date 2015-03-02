package serializers

import spray.json._
import facts._
import org.joda.time.DateTime
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._
import api.EntityIndex

/** Serializers for common datatypes
 *  FIXME why is JsonFormat invariant?
 */
object CustomSerializers extends DefaultJsonProtocol {

  implicit def edFormat = jsonFormat2(EntityIndex)
  
  implicit def dtFormat = new JsonFormat[DateTime] {
    def write(dt: DateTime) = JsString(dt.toString())
    def read(js: JsValue) = js match {
      case JsString(dt) => new DateTime(dt)
      case other => throw new UnsupportedOperationException("Not a recognized DateTime value "+other)
    }
  }
}

/** Serializers for all fact-related types
 *  FIXME why is JsonFormat invariant? makes this very strange
 *  FIXME Group factval warning should be properly corrected
 *  TODO probably incorrect to have valFormat be parametrized - change to implicit classtag
 */
object FactSerializers extends DefaultJsonProtocol {
  import CustomSerializers._

  implicit def intFormat = jsonFormat1(FactInt)
  implicit def strFormat = jsonFormat1(FactString)
  implicit def bdFormat = jsonFormat1(FactBD)
  implicit def moneyFormat = jsonFormat4(FactMoney)
  implicit def periodFormat = jsonFormat4(Period)
  implicit def groupFormat[T <: FactVal : JsonFormat] = jsonFormat2(Group[T])
  implicit def holdingFormat = jsonFormat10(Holding)
  implicit def colFormat[T <: FactValSliced[T] : JsonFormat] = jsonFormat2(FactCol[T])

  implicit def valSlicedFormat[T <: FactValSliced[_]]: JsonFormat[T] = new JsonFormat[T] {
    def write(slice: T) = slice match {
      case period: Period => period.toJson
      case group: Group[FactVal] @unchecked => group.toJson
    }
    def read(json: JsValue) = json match {
      case obj: JsObject => obj.fields("facttype") match {
        // *hurl* 
        case JsString("period") => obj.convertTo[Period].asInstanceOf[T]
        case JsString("group") => obj.convertTo[Group[FactVal]].asInstanceOf[T]
        case other => throw new UnsupportedOperationException("This fact type does not have a marker value "+other)
      }
      case other => throw new UnsupportedOperationException("This fact type is not recognized "+other)
    }
  }

  implicit def valFormat[T <: FactValSliced[T]]: RootJsonFormat[FactVal] = new RootJsonFormat[FactVal] {
    def write(fv: FactVal) = fv match {
      case FactString(str) => str.toJson
      case FactInt(int) => int.toJson
      case FactBD(bd) => bd.toJson
      case money: FactMoney => money.toJson
      case col: FactCol[T] @unchecked => col.toJson
      case period: Period => period.toJson
      case group: Group[_] => group.toJson
      case holding: Holding => holding.toJson
      case FactNone => JsNull
      case other => throw new UnsupportedOperationException("Never supposed to happen "+other)
    }
    def read(json: JsValue) = json match {
      case JsString(str) => new FactString(str)
      case JsNumber(num) => new FactBD(num.toDouble)
      case JsNull => FactNone
      case obj: JsObject => obj.fields("facttype") match {
        case JsString("monetary") => obj.convertTo[FactMoney]
        case JsString("collection") => obj.convertTo[FactCol[T]]
        case JsString("period") => obj.convertTo[Period]
        case JsString("holding") => obj.convertTo[Holding]
        case JsString("group") => obj.convertTo[Group[_]]
        case other => throw new UnsupportedOperationException("This factval type does not have a marker value "+other)
      }
      case other => throw new UnsupportedOperationException("This factval type is not recognized "+other)
    }
  }

  implicit def factFormat: RootJsonFormat[Fact] = rootFormat(lazyFormat(jsonFormat8(Fact.apply)))
}

