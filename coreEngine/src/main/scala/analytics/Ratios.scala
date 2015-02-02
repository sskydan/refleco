package analytics

import facts.Fact._
import facts.FactMoney
import facts.FactBD
import Analytic._

/** collection of leverage ratio definitions
 */
object LeverageRatios extends Analytic("Leverage Ratios") {
	override val subAnalytics = Seq(
	    
	  new Analytic("financial leverage",
	    simpleRatioFn(
	      Seq("us-gaap:Assets", "us-gaap:StockholdersEquity"),
	      {case Seq(assets, equity) => assets / equity }
	    )
	  ),
	  new Analytic("debt to equity", 
	    simpleRatioFn(
	      Seq("us-gaap:DebtCurrent", "us-gaap:StockholdersEquity"),
        {case Seq(debt, equity) => debt / equity }
      )
    ),
	  new Analytic("current liabilities to net worth",
	    simpleRatioFn( 
	      Seq("us-gaap:LiabilitiesCurrent", "us-gaap:Assets", "us-gaap:Liabilities"),
        {case Seq(cliabilities, assets, liabilities) => assets / (assets - liabilities) }
	    )
	  ),
	  new Analytic("fixed assets to net worth",
	    simpleRatioFn(
	      Seq("us-gaap:PropertyPlantAndEquipmentNet", "us-gaap:Assets", "us-gaap:Liabilities"),
        {case Seq(ppen, assets, liabilities) => ppen / (assets - liabilities) }
      )
	  ),
	  new Analytic("net worth", 
	    simpleRatioFn(
	      Seq("us-gaap:Assets", "us-gaap:Liabilities"),
        {case Seq(assets, liabilities) => assets - liabilities }
      ), 
      Seq("owners equity")
	  )
  )
}

object LiquidityRatios extends Analytic("Liquidity Ratios") {
	override val subAnalytics = Seq(
	    	    
	  new Analytic("cash ratio",
	    simpleRatioFn(
	      Seq("us-gaap:CashAndCashEquivalentsAtCarryingValue", "us-gaap:LiabilitiesCurrent"),
	      {case Seq(cce, cliabilities) => cce / cliabilities }
	    )
	  ),
	  new Analytic("quick ratio", 
	    simpleRatioFn(
	      Seq("us-gaap:InventoryNet","us-gaap:AssetsCurrent", "us-gaap:LiabilitiesCurrent"),
        {case Seq(inventory, assets, liab) => (assets - inventory) / liab }
      )
    ),
	  new Analytic("current ratio",
	    simpleRatioFn( 
	      Seq("us-gaap:AssetsCurrent", "us-gaap:LiabilitiesCurrent"),
        {case Seq(assets, liab) => assets / liab }
	    )
	  ),
	  new Analytic("interest coverage ratio",
	    simpleRatioFn(
	      Seq("us-gaap:SalesRevenueNet", "us-gaap:NonoperatingIncomeExpense", "us-gaap:OperatingExpenses",
	          "us-gaap:InterestExpense"),
        {case Seq(revenue, noIncome, opExpense, inExpense) => (revenue - opExpense + noIncome) / inExpense }
      )
	  ),
	  new Analytic("cashflow to long-term debt", 
	    simpleRatioFn(
	      Seq("us-gaap:NetIncomeLoss", "us-gaap:DepreciationDepletionAndAmortization", "us-gaap:DebtCurrent"),
        {case Seq(netIncome, dda, debt) =>  (netIncome + dda) / debt }
      ), 
      Seq("owners equity")
	  )
  )
}

object ProfitabilityRatios extends Analytic("Profitability Ratios") {
	override val subAnalytics = Seq(
	    	    
	  new Analytic("earnings per share",
	    simpleRatioFn(
	      Seq("us-gaap:EarningsPerShareBasic"),
	      {case Seq(earningsPshare) => earningsPshare }
	    )
	  ),
	  new Analytic("net profit margin", 
	    simpleRatioFn(
	      Seq("us-gaap:NetIncomeLoss","us-gaap:Revenues"),
        {case Seq(incomeloss, revenues) => incomeloss / revenues }
      ),
      Seq("return on revenue", "ROR")
    ),
	  new Analytic("current ratio",
	    simpleRatioFn( 
	      Seq("us-gaap:SalesRevenueNet", "us-gaap:NonoperatingIncomeExpense", "us-gaap:OperatingExpenses",
	          "us-gaap:Depreciation", "us-gaap:AdjustmentForAmortization"),
        {case Seq(revenue, noIncome, opExpense, depreciation, amortAdj) => 
           revenue - opExpense + noIncome + depreciation + amortAdj }
      )
	  ),
	  new Analytic("operating income loss",
	    simpleRatioFn(
	      Seq("us-gaap:OperatingIncomeLoss"),
        {case Seq(opIncome) => opIncome }
      )
	  ),
	  new Analytic("return on assets", 
	    simpleRatioFn(
	      Seq("us-gaap:NetIncomeLoss", "us-gaap:Assets"),
        {case Seq(netIncome, assets) =>  netIncome / assets }
      ), 
      Seq("ROA")
	  ),
    // return on average assets 
	  // return on average equity (ROAE)
	  new Analytic("return on debt", 
	    simpleRatioFn(
	      Seq("us-gaap:NetIncomeLoss", "us-gaap:LongTermDebtNoncurrent"),
        {case Seq(netIncome, debt) => netIncome / debt }
      ), 
      Seq("ROD")
	  ),
	  new Analytic("return on invested capital", 
	    simpleRatioFn(
	      Seq("us-gaap:NetIncomeLoss", "us-gaap:Dividends", "us-gaap:ShortTermBorrowings", "us-gaap:DebtCurrent",
	          "us-gaap:LongTermDebtNoncurrent", "us-gaap:StockholdersEquity", "us-gaap:LiabilitiesCurrent"),
        {case Seq(netIncome, dividends, borrowings, debtcurrent, longdebt, equity, cliab) => 
           (netIncome - dividends) / (borrowings + debtcurrent + longdebt + equity + cliab) }
      ), 
      Seq("ROIC")
	  ),
	  new Analytic("return on net assets", 
	    simpleRatioFn(
	      Seq("us-gaap:NetIncomeLoss", "us-gaap:PropertyPlantAndEquipmentNet", "us-gaap:AssetsCurrent", 
	          "us-gaap:LiabilitiesCurrent"),
        {case Seq(netIncome, ppen, cassets, cliab) =>  netIncome / (ppen + cassets - cliab) }
      ), 
      Seq("RONA")
	  ),
	  // return on research capital
	  new Analytic("return on sales", 
	    simpleRatioFn(
	      Seq("us-gaap:SalesRevenueNet", "us-gaap:NonoperatingIncomeExpense", "us-gaap:OperatingExpenses", 
	          "us-gaap:Revenues"),
        {case Seq(sales, noIncome, opExpense, revenues) => (sales - opExpense + noIncome) / revenues }
      ),
      Seq("ROS")
    ),
	  new Analytic("dupont formula", 
	    simpleRatioFn(
	      // NetProfitMargin * AssetTurnover * FinancialLeverage 
	      Seq("us-gaap:NetIncomeLoss", "us-gaap:Revenues", "us-gaap:SalesRevenueGoodsNet", "us-gaap:Assets",
	          "us-gaap:StockholdersEquity"),
        {case Seq(netIncome, revenue, sales, assets, equity) => 
           (netIncome / revenue) * (sales / assets) * (assets / equity) }
      )
    ),
    new Analytic("earnings retention", 
	    simpleRatioFn(
	      Seq("us-gaap:NetIncomeLoss", "us-gaap:Dividends"),
        {case Seq(netIncome, dividends) => (netIncome - dividends) / netIncome }
      )
    )
  )
}

object ValuationRatios extends Analytic("Valuation Ratios") {
	override val subAnalytics = Seq(
	  new Analytic("fixed assets turnover",
	    simpleRatioFn(
	      Seq("us-gaap:SalesRevenueGoodsNet", "us-gaap:PropertyPlantAndEquipmentNet"),
        {case Seq(salesGoods, ppen) => salesGoods / ppen }
      )
	  ),
	  new Analytic("total assets turnover", 
	    simpleRatioFn(
	      Seq("us-gaap:Assets", "us-gaap:SalesRevenueGoodsNet"),
        {case Seq(assets, salesGoods) => salesGoods / assets }
      ), 
      Seq("assets to sales")
	  ),
	  new Analytic("equity turnover",
	    simpleRatioFn( 
	      Seq("us-gaap:SalesRevenueGoodsNet", "us-gaap:StockholdersEquity"),
        {case Seq(salesGoods, equity) => salesGoods / equity }
      )
	  ),
	  new Analytic("working capital turnover", 
	    simpleRatioFn(
	      Seq("us-gaap:SalesRevenueGoodsNet", "us-gaap:AssetsCurrent", "us-gaap:LiabilitiesCurrent"),
        {case Seq(salesGoods, cassets, cliab) => salesGoods / (cassets - cliab) }
      ) 
	  ),
	  new Analytic("sales to net working capital", 
	    simpleRatioFn(
	      Seq("us-gaap:SalesRevenueGoodsNet", "us-gaap:AccountsReceivableNetCurrent", "us-gaap:InventoryNet",
	          "us-gaap:AccountsPayableCurrent"),
        {case Seq(salesGoods, receivables, inventory, payables) =>
          salesGoods / (receivables + inventory + payables )}
      ) 
	  )
  )
}

object EfficiencyRatios extends Analytic("Efficiency Ratios") {
	override val subAnalytics = Seq(
    new Analytic("cashflow per share", 
	    simpleRatioFn(
	      Seq("us-gaap:NetCashProvidedByUsedInOperatingActivities", "us-gaap:DividendsPreferredStock",
	          "us-gaap:SharesOutstanding"),
        {case Seq(opCash, prefStockDividends, outShares) => (opCash - prefStockDividends) / outShares }
      )
	  )
  )
}
