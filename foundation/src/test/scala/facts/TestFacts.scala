package facts

import Fact._
import spray.json._
import testbase.UnitSpec
import org.joda.time.DateTime
import com.github.nscala_time.time.Imports._
import serializers.FactSerializers._
import serializers.CustomSerializers._

class TestFacts extends UnitSpec {
  
  "Facts group" should "support pulling children values" in {
    val lookup = testFact.children.pullValues[BigDecimal](Seq("us-gaap:DebtConversionOriginalDebtAmount1", "us-gaap:DeferredStateAndLocalIncomeTaxExpenseBenefit"))
    
    assert(lookup == Seq(1814000.0, 63400.0))
  }
  
  "Facts group" should "support pulling partial children values" in {
    val lookup = testFact.children.pullValues[BigDecimal](Seq("us-gaap:Nope", "us-gaap:DeferredStateAndLocalIncomeTaxExpenseBenefit"))
    
    assert(lookup == Seq(63400.0))
  }
  
  val testFact:Fact = ("""
   {
       "id": "0001419852::2014-01-28",
       "ftype": "10-K",
       "value": "2014-01-28",
       "prettyLabel": "MATTRESS FIRM HOLDING CORP.",
       "interest": 0,
       "children":
       [
           {
               "id": "us-gaap:DebtConversionOriginalDebtAmount1",
               "ftype": "xbrl",
               "value":
               {
                   "startDate": "2011-11-22T00:00:00.000-05:00",
                   "endDate": "2011-11-23T00:00:00.000-05:00",
                   "inner":
                   {
                       "valDouble": 1814000,
                       "currency": "USD",
                       "facttype": "monetary"
                   },
                   "facttype": "period"
               },
               "prettyLabel": "Debt converted to the Company's common stock",
               "interest": 0,
               "children":
               [
               ],
               "uuid": "09105af6-941b-496a-93af-b7b6fb8292fe"
           },
           {
               "id": "us-gaap:LongTermDebtNoncurrent",
               "ftype": "xbrl",
               "value":
               {
                   "valList":
                   [
                       {
                           "startDate": "2013-01-29T00:00:00.000-05:00",
                           "endDate": "2013-01-29T00:00:00.000-05:00",
                           "inner":
                           {
                               "valDouble": 7302300,
                               "currency": "USD",
                               "facttype": "monetary"
                           },
                           "facttype": "period"
                       },
                       {
                           "startDate": "2014-01-28T00:00:00.000-05:00",
                           "endDate": "2014-01-28T00:00:00.000-05:00",
                           "inner":
                           {
                               "valDouble": 7252900,
                               "currency": "USD",
                               "facttype": "monetary"
                           },
                           "facttype": "period"
                       }
                   ],
                   "facttype": "collection"
               },
               "prettyLabel": "Long-term debt, net of current maturities",
               "interest": 0,
               "children":
               [
               ],
               "uuid": "9e4c0287-95cb-47cf-885c-b2835fe40f78"
           },
           {
               "id": "us-gaap:DeferredStateAndLocalIncomeTaxExpenseBenefit",
               "ftype": "xbrl",
               "value":
               {
                   "valList":
                   [
                       {
                           "startDate": "2011-02-02T00:00:00.000-05:00",
                           "endDate": "2012-01-31T00:00:00.000-05:00",
                           "inner":
                           {
                               "valDouble": -11966,
                               "currency": "USD",
                               "facttype": "monetary"
                           },
                           "facttype": "period"
                       },
                       {
                           "startDate": "2012-02-01T00:00:00.000-05:00",
                           "endDate": "2013-01-29T00:00:00.000-05:00",
                           "inner":
                           {
                               "valDouble": 7100,
                               "currency": "USD",
                               "facttype": "monetary"
                           },
                           "facttype": "period"
                       },
                       {
                           "startDate": "2013-01-30T00:00:00.000-05:00",
                           "endDate": "2014-01-28T00:00:00.000-05:00",
                           "inner":
                           {
                               "valDouble": 63400,
                               "currency": "USD",
                               "facttype": "monetary"
                           },
                           "facttype": "period"
                       }
                   ],
                   "facttype": "collection"
               },
               "prettyLabel": "State",
               "interest": 0,
               "children":
               [
               ],
               "uuid": "ebdbeec3-c1b6-475d-bbf6-e21e3e358b5d"
           }
       ],
       "uuid": "ebdbeec3-c1b6-475d-bbf6-e21e3e358b5f"
     }
  """).parseJson.convertTo[Fact]
}