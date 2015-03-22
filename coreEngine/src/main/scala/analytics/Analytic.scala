package analytics

import facts.Fact
import facts.Fact._
import facts.FactNone
import facts.FactVal
import facts.FactBD
import com.typesafe.scalalogging.StrictLogging
import scala.util.Try
import scala.util.control.NonFatal

/** base signature for all analytics over facts
 */
class Analytic(
  val name: String, 
  val body: Facts => FactVal = _ => FactNone, 
  val aliases: Seq[String] = Nil
) extends StrictLogging {
  val ANA_FTYPE = "analytic"
  
  val subAnalytics:Seq[Analytic] = Nil
  
  def apply(data:Facts): Fact = Fact(
    name, 
    ANA_FTYPE, 
    body(data),
    aliases, 
    0, 
    subAnalytics map (_ apply data)
  )
  
  def simpleRatioFn(keys:Seq[String], fn:PartialFunction[Seq[BigDecimal],BigDecimal])(f:Facts) = {
	  val failed:PartialFunction[Seq[BigDecimal],FactVal] = {case other => 
	    logger info s"Ratio error $name, found $other from $keys"
	    FactNone
    }
  
    try {
     (fn andThen FactBD orElse failed) (f.pullValues[BigDecimal](keys))
     
    } catch {
      case NonFatal(e) =>
        logger error s"Ratio calculation error: $e"
        FactNone
    }
  }
}

/** utilities and common analytics methods
 */
object Analytic {
  
  val ALL = Seq(LeverageRatios, LiquidityRatios, ProfitabilityRatios, ValuationRatios, EfficiencyRatios)
  
  def generateRatios(analytics:Seq[Analytic], report:Fact) =
    new Fact(
      "analytics::"+report.id,
      "analytics", 
      report.value, 
      report.prettyLabel,
      0, 
      analytics map (_(report.children))
    )
}