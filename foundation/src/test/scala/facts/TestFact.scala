package facts

import spray.json._
import testbase.UnitSpec
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import serializers.FactSerializers._
import serializers.CustomSerializers._

class TestFact extends UnitSpec {
  
  val startDate = "2004-02-23T00:00:00.000-05:00"
  val startDate2 = "2014-12-24T00:00:00.000-05:00"
  val endDate = "2005-12-02T00:00:00.000-05:00"
  val endDate2 = "2115-12-02T00:00:00.000-05:00"
  
  "FactVal initialization" should "support basic types" in {
    val fstring = new FactString("string")
    val fint = new FactInt(1)
    val fmoney = new FactMoney(10.323, "USD", -3)
    val fperiod1 = new Period(new DateTime(startDate), new DateTime(endDate), fint)
    val fperiod2 = new Period(new DateTime(startDate) + 2.days, new DateTime(endDate) + 1.day, fmoney)
    val fcol = new FactCol(List(fperiod1, fperiod2))

    assert(true)
  }
  
  "FactVal types" should "support json serialization" in {
    val fstring = new FactString("string")
    val fint = new FactInt(1)
    val fmoney = new FactMoney(1000.3763, "USD", -3)
    val fperiod1 = new Period(new DateTime(startDate), new DateTime(endDate), fint)
    val fperiod2 = new Period(new DateTime(startDate2), new DateTime(endDate2), fmoney)
    val fcol = new FactCol(List(fperiod1, fperiod2))


    val fstringJson = JsObject("valStr" -> JsString("string"))
    val fintJson = JsObject("valInt" -> JsNumber(1))
    val fmoneyJson = JsObject(
      "valDouble" -> JsNumber(1000.3763),
      "currency" -> JsString("USD"),
      "precision" -> JsNumber(-3),
      "facttype" -> JsString("monetary")
    )
    val fperiod1Json = JsObject(
      "startDate" -> JsString(startDate),
      "endDate" -> JsString(endDate),
      "inner" -> JsNumber(1),
      "facttype" -> JsString("period")
    )
    val fperiod2Json = JsObject(
      "startDate" -> JsString(startDate2),
      "endDate" -> JsString(endDate2),
      "inner" -> fmoneyJson,
      "facttype" -> JsString("period")
    )
    val fcolJson = JsObject(
      "valList" -> JsArray(
        fperiod1Json,
        fperiod2Json
      ),
      "facttype" -> JsString("collection")
    )
    
    assert(fstring.toJson == fstringJson)
    assert(fint.toJson == fintJson)
    assert(fmoney.toJson == fmoneyJson)
    assert(fperiod1.toJson == fperiod1Json)
    assert(fperiod2.toJson == fperiod2Json)
    assert(fcol.toJson == fcolJson)
  }
  
  "Facts" should "support json serialization" in {
    val fint = new FactInt(1)
    val fmoney = new FactMoney(10.2, "USD", -3)
    val fperiod1 = new Period(new DateTime(startDate), new DateTime(endDate), fint)
    val fperiod2 = new Period(new DateTime(startDate2), new DateTime(endDate2), fmoney)
    val fcol = new FactCol(List(fperiod1, fperiod2))
    
    val children = Seq(new Fact("child", "kind2", fcol, Seq("childLabel"), 42.99, Nil, None, "childUUID"))
    val fact = new Fact("name", "kind", fcol, Seq("prettyLabel"), 232.4, children, Some(JsObject("key"->JsString("details"))), "parentUUID")
    
    val factJson = ("""{
      "id":"name",
      "ftype":"kind",
      "value": {
        "valList": [
          {
            "startDate": """"+ startDate +"""",
            "endDate": """"+ endDate +"""",
            "inner":1,
            "facttype":"period"
          }, {
            "startDate": """"+ startDate2 +"""",
            "endDate": """"+ endDate2 +"""",
            "inner": {
              "valDouble":10.2,
              "currency":"USD",
              "precision":-3,
              "facttype":"monetary"
            },
            "facttype":"period"
          }
        ],
        "facttype":"collection"
      },
      "prettyLabel":["prettyLabel"],
      "interest":232.4,
      "children":[{
      "id":"child",
      "ftype":"kind2",
        "value": {
          "valList": [
            {
              "startDate": """"+ startDate +"""",
              "endDate": """"+ endDate +"""",
              "inner":1,
              "facttype":"period"
            }, {
              "startDate": """"+ startDate2 +"""",
              "endDate": """"+ endDate2 +"""",
              "inner": {
                "valDouble":10.2,
                "currency":"USD",
                "precision":-3,
                "facttype":"monetary"
              },
              "facttype":"period"
            }
          ],
          "facttype":"collection"
        },
        "prettyLabel":["childLabel"],
        "interest":42.99,
        "children":[],
        "uuid":"childUUID"
      }],
      "details":{
        "key":"details"
      },
      "uuid":"parentUUID"
    }""").parseJson

    assert(fact.toJson == factJson)
  }
  
  it should "support autogenerated uuids" in {
    val testfacts = (1 to 10000) map (index => new Fact(index.toString(), "kind"))
    val testUuids = testfacts map(_.uuid)
    
    // No duplicates
    assert(testUuids.toSet.size == testUuids.size)
  }
  
}

