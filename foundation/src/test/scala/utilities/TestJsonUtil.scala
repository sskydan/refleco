package utilities

import spray.json._
import spray.json.JsObject
import spray.json.JsNull
import testbase.UnitSpec
import utilities.JsonUtil._

object DummyData {
  val num = JsNumber(1)
  val part1 = JsString("part1")
  val part2 = JsString("part2")
  val bool = JsBoolean(false)
  val innerStr = JsString("bob")
  val obj = JsObject(Map("65days" -> JsString("ofstatic"), "obj" -> innerStr))
  val array = JsArray(JsBoolean(false), JsObject("innerObj" -> innerStr, "cat" -> JsString("mary")))
  
  val json:JsObject = JsObject(Map[String,JsValue](
    "first" -> JsObject(
      "num" -> num,
      "bool" -> bool,
      "part" -> part1,
      "varr" -> JsBoolean(true),
      "array" -> array,
      "type" -> num
    ),
    "nil" -> JsNull,
    "second" -> JsObject(
      "part" -> part2,
      "some" -> JsNumber(2),
      "type" -> bool
    ),
    "obj" -> obj,
    "simple" -> JsNumber(8)
  ))
    
  val jsonFilter = """{
    "xbrli:context": {
      "contentArray": [
        {
          "id": "D2009",
          "xbrli:entity": {
            "xbrli:identifier": {
              "content": 68505,
              "scheme": "http://www.sec.gov/CIK"
            }
          },
          "xbrli:period": {
            "xbrli:endDate": "2009-12-31",
            "xbrli:startDate": "2009-01-01"
          }
        },
        {
          "id": "I2009",
          "xbrli:entity": {
            "xbrli:identifier": {
              "content": 68505,
              "scheme": "CIKK"
            }
          },
          "xbrli:period": {
            "xbrli:instant": "2009-12-31"
          }
        },
        {
          "id": "I2009PF",
          "xbrli:entity": {
            "xbrli:identifier": {
              "content": 22222,
              "scheme": "http://www.sec.gov/CIK"
            }
          },
          "period": {
            "xbrli:instant": "2009-07-04"
          }
        }
      ]
    }
  }""".parseJson
  
  val simpleJson = """{
      "test": "t",
      "obj": {
        "1": "one",
        "2": "two",
        "inner": {
          "3": "three",
          "2": "two",
          "array": [
            "1",
            "2",
            "3",
            {
              "final": "f"
            }
          ]
        },
        "4": "four"
      }
    }""".parseJson
}

class TestJsonUtil extends UnitSpec {
  import DummyData._
  import JsonUtil._
  
  "Json parser" should "make json from string" in {
    val jsonString = json.toString
    val parsed = toJson(jsonString)

    assert(json == parsed)
  }
  
  it should "make json from stringable object" in {
    class Dummy(json:JsObject) {
      override def toString() = json.toString
    }
    val dummy = new Dummy(json)
    val parsed = toJson(dummy)

    assert(json == parsed)
  }
  
  "Json lookup" should "find without type" in {
    val fbool = json.\\~("bool")
    assert(Set(bool) == fbool)
  }
  
  it should "find single item" in {
    val fnum = json.\\~[JsNumber]("num")
    val fbool = json.\\~[JsBoolean]("bool")
    val farray = json.\\~[JsArray]("array")
    val fobj = json.\\~[JsObject]("obj")
    val finnerStr = json.\\~[JsString]("innerObj")
    val lookupMap = Map(num -> fnum, bool -> fbool, array -> farray, obj -> fobj, innerStr -> finnerStr)
    
    lookupMap foreach { case (k,v) => assert(Set(k) == v) }
  }
  
  it should "find multiple matches" in {
    val parts = json.\\~[JsString]("part")
    assert(Set(part1, part2) == parts)
  }
  
  it should "find recursively" in {
    val rec = json.\\~("obj")
    assert(Set(obj, innerStr) == rec)
  }
  
  it should "discriminate by type" in {
    val typeBool = json.\\~[JsBoolean]("type")
    assert(Set(bool) == typeBool)
    val typeNum = json.\\~[JsNumber]("type")
    assert(Set(num) == typeNum)
  }
  
  it should "not find missing elements" in {
    val missing = json.\\~[JsValue]("missing")
    assert(Set() == missing)
  }
  
  it should "find first element" in {
    val fnum = json.\\[JsNumber]("num")
    val fbool = json.\\[JsBoolean]("bool")
    val farray = json.\\[JsArray]("array")
    val fobj = json.\\[JsObject]("obj")
    val finnerStr = json.\\[JsString]("innerObj")
    val lookupMap = Map(num -> fnum, bool -> fbool, array -> farray, obj -> fobj, innerStr -> finnerStr)
    
    lookupMap foreach { case (k,v) => assert(k == v) }
  }
  
  it should "error if first element is not found" in {
    intercept[java.util.NoSuchElementException] {
      json.\\[JsValue]("missing")
    }
  }
  
  "Json multiple lookup" should "find all elements" in {
    val fmult = json.\\~[JsBoolean]("bool", "varr")
    
    assert(Set(bool, JsBoolean(true)) == fmult)
  }
  
  "Json multiple lookup" should "find first from all elements" in {
    val fmult = json.\\~[JsBoolean]("ricky", "varr")
    
    assert(Set(JsBoolean(true)) == fmult)
  }
  
  "Json filter" should "filter correct single value" in {
    val period = """{
      "id": "D2009",
      "xbrli:entity": {
          "xbrli:identifier": {
              "content": 68505,
              "scheme": "http://www.sec.gov/CIK"
          }
      },
      "xbrli:period": {
          "xbrli:endDate": "2009-12-31",
          "xbrli:startDate": "2009-01-01"
      }
    }""".parseJson

    val periodActual = jsonFilter.childrenWith("id", JsString("D2009"))
    
    assert(Set(period) == periodActual)
  }
  
  it should "filter correct multiple values" in {
    val cik1 = """{
      "content": 68505,
      "scheme": "http://www.sec.gov/CIK"
    }""".parseJson
    val cik2 = """{
      "content": 68505,
      "scheme": "CIKK"
    }""".parseJson
  
    val cikActual = jsonFilter.childrenWith("content", JsNumber(68505))
    
    assert(Set(cik1,cik2) == cikActual)
  }
  
  it should "filter by JsObjects" in {
    val fobj = """{"xbrli:instant": "2009-07-04"}""".parseJson
    
    val obj = """{
      "id": "I2009PF",
      "xbrli:entity": {
          "xbrli:identifier": {
              "content": 22222,
              "scheme": "http://www.sec.gov/CIK"
          }
      },
      "period": {
          "xbrli:instant": "2009-07-04"
      }
    }""".parseJson
    
    val objectActual = jsonFilter.childrenWith("period", fobj)
    
    assert(Set(obj) == objectActual)
  }
  
  it should "not filter by missing values" in {
    val fobj = """{"xbrli:instant": "2009-12-31"}""".parseJson
    
    val objectActual = jsonFilter.childrenWith("period", fobj)
    
    assert(Set() == objectActual)
  }
  
  "Json concatenation" should "concatenate properly" in {
    val other = """{
      "test": "tt",
      "obj": {
        "inner": {
          "33": "three",
          "22": "two",
          "array": [
            "4",
            "3"
          ]
        }
      }
    }""".parseJson
    
    val union = """{
      "test": "tt",
      "obj": {
        "1": "one",
        "2": "two",
        "inner": {
          "3": "three",
          "2": "two",
          "array": [
            "1",
            "2",
            "3",
            {
              "final": "f"
            },
            "4",
            "3"
          ],
          "33": "three",
          "22": "two"
        },
        "4": "four"
      }
    }""".parseJson
    
    assert(union == (simpleJson ++ other))
  }
  
  "Json path filter" should "work for single path" in {
    val result = """{
      "obj": {
        "inner": {
          "array": [
            {
              "final": "f"
            }
          ]
        }
      }
    }""".parseJson 
    
    assert(result == (simpleJson filter "obj.inner.array.final"))
  }
  
  it should "work for multiple paths" in {
    val result = """{
      "test": "t",
      "obj": {
        "2": "two",
        "inner": {
          "array": [
            "1",
            "2",
            "3",
            {
              "final": "f"
            }
          ]
        }
      }
    }""".parseJson
    
    val queries = Seq(
      "obj.inner.array",
      "test",
      "obj.2"
    )
    
    assert(result == (simpleJson filterAll queries))
  }
  
}
