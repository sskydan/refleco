from reflecoSearch.classes.boxClasses.TableBox import TableBox
from reflecoSearch.classes.filterClasses.Filter import Filter
import logging
devLogger = logging.getLogger('development')

class BalanceSheetFilter(Filter):

    #Keys that build a balance sheet report
    keys = [
            'Assets Abstract',
            'Current Assets Abstract',
            [
                'us-gaap:CashAndCashEquivalentsAtCarryingValue',
                'us-gaap:AvailableForSaleSecuritiesCurrent',
                'us-gaap:CashCashEquivalentsAndShortTermInvestments',
                'us-gaap:AccountsReceivableNetCurrent',
                'us-gaap:InventoryNet',
                'us-gaap:ReceivableUnderReverseRepurchaseAgreements',
                'us-gaap:DeferredTaxAssetsNetCurrent',
                'us-gaap:IncomeTaxesReceivable',
                'us-gaap:PrepaidRevenueShareExpensesAndOtherAssetsCurrent',
                'us-gaap:AssetsCurrent',
                'us-gaap:PrepaidRevenueShareExpensesAndOtherAssetsNoncurrent',
                'us-gaap:OtherLongTermInvestments',
                'us-gaap:PropertyPlantAndEquipmentNet',
                'us-gaap:IntangibleAssetsNetExcludingGoodwill',
                'us-gaap:Goodwill',
                'us-gaap:Assets'
            ],
            'Liabilities Abstract',
            'Current Liabilities Abstract',
            [
                'us-gaap:AccountsPayableCurrent',
                'us-gaap:DebtCurrent',
                'us-gaap:EmployeeRelatedLiabilitiesCurrent',
                'us-gaap:AccruedLiabilitiesCurrent',
                'us-gaap:AccruedRevenueShare',
                'us-gaap:DepositsReceivedForSecuritiesLoanedAtCarryingValue',
                'us-gaap:DeferredRevenueCurrent',
                'us-gaap:AccruedIncomeTaxesCurrent',
                'us-gaap:LiabilitiesCurrent',
                'us-gaap:LongTermDebtAndCapitalLeaseObligations',
                'us-gaap:DeferredRevenueNoncurrent',
                'us-gaap:LiabilityForUncertainTaxPositionsNoncurrent',
                'us-gaap:DeferredTaxLiabilitiesNoncurrent',
                'us-gaap:OtherLiabilitiesNoncurrent'
            ],
            'Stockholders Equity Abstract',
            [
                'us-gaap:ConvertiblePreferredStockNonredeemableOrRedeemableIssuerOptionValue',
                'us-gaap:CommonStocksIncludingAdditionalPaidInCapital',
                'us-gaap:CapitalStockValue',
                'us-gaap:AccumulatedOtherComprehensiveIncomeLossNetOfTax',
                'us-gaap:RetainedEarningsAccumulatedDeficit',
                'us-gaap:StockholdersEquity'
            ]
    ]

    def createBoxList(self):
        """Creates a list of Box objects for a balance sheet report
        :return: List(TableBox())
        """
        boxList = []
        if len(self.dataSet):
            for f in self.dataSet:
                if f[u'ftype'] == '10-K':
                    statementData = []
                    for item in self.keys:
                        if isinstance(item, list):
                            for fact in self.filterData(f[u'children'], lambda e: e[u'id'] in item):
                                statementData.append(fact)
                        elif "Abstract" in item:
                            name = item.replace("Abstract", "")
                            statementData.append({u'prettyLabel': name, u'value': None})

                    if len(statementData):
                        boxList.append(TableBox.makeBox(statementData, f[u'prettyLabel'][0] + " Balance Sheet" + " (" + f[u'value'] + ")", []))
        return boxList
