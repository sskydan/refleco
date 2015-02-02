package ranking.investigators

import facts._
import org.joda.time.DateTime
import scala.concurrent.duration._
import scala.concurrent.Future
import spray.http.HttpRequest
import spray.http.Uri
import spray.httpx.encoding.Gzip
import spray.httpx.SprayJsonSupport._
import spray.client.pipelining._
import spray.json.JsObject
import spray.json.JsString
import akka.actor.Actor
import akka.util.Timeout
import breeze.stats._
import utilities.CEConfig
import utilities.JsonUtil._
import scala.concurrent.Await
import serializers.FactSerializers._
import scala.reflect._

/**
 * This PI should first calculate the average historical variance of a particular fact,
 *   then compare it to the delta of this fact with its predecessor. The interest rank should
 *   be directly proportional to how much larger the current delta is.
 */
class HistoricalVariancePI extends Actor with PI with CEConfig {
  implicit val system = context.system
  implicit val dispatcher = system.dispatcher
  implicit val timeout = Timeout(10.hours)

  val dsHost = config.getString("dataServerHost")
  val searchToken = config.getString("dsSearchToken")
 
  val pipeline: HttpRequest => Future[Seq[Fact]] = (
    sendReceive
    ~> decode(Gzip)
    ~> unmarshal[Seq[Fact]]
  )


  override def rank(fact:Fact):Double = {
    val rank= getHistory(fact) map { history =>
      applicableIds map { id =>
        fact.children.find(_.id == id) map { workingFact =>
          
          val factHistory = history flatMap(_.children) filter(_.id == id) map(_.value.get)

          val valueHistory = factHistory match {
            case moneyList:Seq[FactMoney] => moneyList map (_.get) 
          }
      
          val deltas = (valueHistory, valueHistory drop 1).zipped map(_ - _) map(_.toDouble)
      
          if (deltas.length >= 2) {
            val dist = deltas.head / mean(deltas)
            println("HistoricalVariancePI:: id="+id+"\n\tstddev="+stddev(deltas)+"\n\tvalueHistory="+valueHistory+"\n\tdeltas="+deltas+"\n\tdist="+dist)
            
            workingFact.interest = dist
          } else println("HistoricalVariancePI:: not enough historical data: "+valueHistory.length)
          
        }
      }
    }
    
    Await.ready(rank, Duration.Inf)
    1.0
  }
  
  def companyName(fact:Fact) =
    fact.details.get.\\[JsObject]("dei:EntityRegistrantName").getFields("content") match {
      case Seq(JsString(name)) => name.split(" ").head
    }

  def getHistory(fact:Fact) = pipeline {
    Get(Uri(dsHost) withQuery (searchToken -> companyName(fact), "sort" -> "date"))
  }
  
  /**
   * FIXME should be read from config to allow dynamic reconfiguration
   */
  override val applicableIds = List(
    "us-gaap:Goodwill",
    "us-gaap:Revenues",
    "us-gaap:DebtCurrent",
    "us-gaap:Depreciation",
    "us-gaap:InterestPaid",
    "us-gaap:InventoryNet",
    "us-gaap:SharesIssued",
    "us-gaap:NetIncomeLoss",
    "us-gaap:CostOfRevenue",
    "us-gaap:AssetsCurrent",
    "us-gaap:IncomeTaxesPaid",
    "us-gaap:RepaymentsOfDebt",
    "us-gaap:CostsAndExpenses",
    "us-gaap:CapitalStockValue",
    "us-gaap:CostOfSalesMember",
    "us-gaap:StockholdersEquity",
    "us-gaap:LiabilitiesCurrent",
    "us-gaap:StockholdersEquity",
    "us-gaap:AccruedRevenueShare",
    "us-gaap:OperatingIncomeLoss",
    "us-gaap:EarningsPerShareBasic",
    "us-gaap:IncomeTaxesReceivable",
    "us-gaap:AccountsPayableCurrent",
    "us-gaap:DeferredRevenueCurrent",
    "us-gaap:ShareBasedCompensation",
    "us-gaap:RetainedEarningsMember",
    "us-gaap:ImpairmentOfInvestments",
    "us-gaap:IncomeTaxExpenseBenefit",
    "us-gaap:EarningsPerShareDiluted",
    "us-gaap:OtherLongTermInvestments",
    "us-gaap:AccruedLiabilitiesCurrent",
    "us-gaap:AccruedIncomeTaxesCurrent",
    "us-gaap:DeferredRevenueNoncurrent",
    "us-gaap:OtherNoncashIncomeExpense",
    "us-gaap:NonoperatingIncomeExpense",
    "us-gaap:OtherLiabilitiesNoncurrent",
    "us-gaap:SellingAndMarketingExpense",
    "us-gaap:DeferredTaxAssetsNetCurrent",
    "us-gaap:PropertyPlantAndEquipmentNet",
    "us-gaap:AccountsReceivableNetCurrent",
    "us-gaap:IncreaseDecreaseInIncomeTaxes",
    "us-gaap:IncreaseDecreaseInInventories",
    "us-gaap:ResearchAndDevelopmentExpense",
    "us-gaap:AmortizationOfIntangibleAssets",
    "us-gaap:DeferredIncomeTaxExpenseBenefit",
    "us-gaap:CapitalLeaseObligationsIncurred",
    "us-gaap:GeneralAndAdministrativeExpense",
    "us-gaap:DeferredTaxLiabilitiesNoncurrent",
    "us-gaap:SellingAndMarketingExpenseMember",
    "us-gaap:AvailableForSaleSecuritiesCurrent",
    "us-gaap:EmployeeRelatedLiabilitiesCurrent",
    "us-gaap:IncreaseDecreaseInAccountsPayable",
    "us-gaap:IncreaseDecreaseInDeferredRevenue",
    "us-gaap:PaymentsToAcquireOtherInvestments",
    "us-gaap:MotorolaMobilityHoldingsIncMember",
    "us-gaap:RetainedEarningsAccumulatedDeficit",
    "us-gaap:ProceedsFromDebtNetOfIssuanceCosts",
    "us-gaap:IncomeLossFromContinuingOperations",
    "us-gaap:ProceedsFromDivestitureOfBusinesses",
    "us-gaap:ResearchAndDevelopmentExpenseMember",
    "us-gaap:IncreaseDecreaseInAccountsReceivable",
    "us-gaap:IntangibleAssetsNetExcludingGoodwill",
    "us-gaap:IncreaseDecreaseInAccruedLiabilities",
    "us-gaap:OtherComprehensiveIncomeLossNetOfTax",
    "us-gaap:IncreaseDecreaseInAccruedRevenueShare",
    "us-gaap:PaymentsToAcquireMarketableSecurities",
    "us-gaap:CashAndCashEquivalentsAtCarryingValue",
    "us-gaap:GeneralAndAdministrativeExpenseMember",
    "us-gaap:StockIssuedDuringPeriodValueNewIssues",
    "us-gaap:LongTermDebtAndCapitalLeaseObligations",
    "us-gaap:AllocatedShareBasedCompensationExpense",
    "us-gaap:StockIssuedDuringPeriodSharesNewIssues",
    "us-gaap:ChargeRelatedToResolutionOfInvestigation",
    "us-gaap:AccumulatedOtherComprehensiveIncomeMember",
    "us-gaap:IncreaseDecreaseInOperatingCapitalAbstract",
    "us-gaap:NetCashProvidedByUsedInOperatingActivities",
    "us-gaap:PaymentsToAcquirePropertyPlantAndEquipment",
    "us-gaap:NetCashProvidedByUsedInInvestingActivities",
    "us-gaap:NetCashProvidedByUsedInFinancingActivities",
    "us-gaap:CashCashEquivalentsAndShortTermInvestments",
    "us-gaap:ReceivableUnderReverseRepurchaseAgreements",
    "us-gaap:LiabilityForUncertainTaxPositionsNoncurrent",
    "us-gaap:CommonStocksIncludingAdditionalPaidInCapital",
    "us-gaap:EffectOfExchangeRateOnCashAndCashEquivalents",
    "us-gaap:CashAndCashEquivalentsPeriodIncreaseDecrease",
    "us-gaap:IncomeLossFromDiscontinuedOperationsNetOfTax",
    "us-gaap:AccumulatedOtherComprehensiveIncomeLossNetOfTax",
    "us-gaap:IncomeLossFromContinuingOperationsPerBasicShare",
    "us-gaap:PrepaidRevenueShareExpensesAndOtherAssetsCurrent",
    "us-gaap:ProceedsFromSaleAndMaturityOfMarketableSecurities",
    "us-gaap:IncomeLossFromContinuingOperationsPerDilutedShare",
    "us-gaap:CommonStockIncludingAdditionalPaidInCapitalMember",
    "us-gaap:DepositsReceivedForSecuritiesLoanedAtCarryingValue",
    "us-gaap:PrepaidRevenueShareExpensesAndOtherAssetsNoncurrent",
    "us-gaap:InvestmentsInMaturitiesOfReverseRepurchaseAgreements",
    "us-gaap:TaxWithholdingRelatedToVestingOfRestrictedStockUnits",
    "us-gaap:NetProceedsPaymentsRelatedToStockBasedAwardActivities",
    "us-gaap:IncreaseDecreaseInCollateralHeldUnderSecuritiesLending",
    "us-gaap:GainLossonSaleofBusinessIncludingDiscontinuedOperations",
    "us-gaap:IncomeLossFromDiscontinuedOperationsNetOfTaxPerBasicShare",
    "us-gaap:IncreaseDecreaseInPrepaidRevenueShareExpensesAndOtherAssets",
    "us-gaap:FairValueStockBasedAwardsAssumedInConnectionWithAcquisition",
    "us-gaap:IncomeLossFromDiscontinuedOperationsNetOfTaxPerDilutedShare",
    "us-gaap:ExcessTaxBenefitFromShareBasedCompensationOperatingActivities",
    "us-gaap:ExcessTaxBenefitFromShareBasedCompensationFinancingActivities",
    "us-gaap:NoncashOrPartNoncashDivestitureAmountOfConsiderationReceived1",
    "us-gaap:ConvertiblePreferredStockNonredeemableOrRedeemableIssuerOptionValue",
    "us-gaap:AcquisitionsNetofCashAcquiredAndPurchasesOfIntangibleandOtherAssets",
    "us-gaap:AdjustmentsToAdditionalPaidInCapitalTaxEffectFromShareBasedCompensation",
    "us-gaap:AdjustmentsToReconcileNetIncomeLossToCashProvidedByUsedInOperatingActivitiesAbstract",
    "us-gaap:AdjustmentsToAdditionalPaidInCapitalSharebasedCompensationRequisiteServicePeriodRecognitionValue",
    "us-gaap:IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments)"
  )
}