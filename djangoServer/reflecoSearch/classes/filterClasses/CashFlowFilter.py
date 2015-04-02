from reflecoSearch.classes.boxClasses.TableBox import TableBox
from reflecoSearch.classes.filterClasses.Filter import Filter
import logging
devLogger = logging.getLogger('development')

class CashFlowFilter(Filter):

    #Keys that create a cash flow statement
    keys = [
        'Net Operating Cash Abstract',
        [
            'us-gaap:NetIncomeLoss'
        ],
        'Adjustments Abstract',
        [
            'us-gaap:Depreciation',
            'us-gaap:AmortizationOfIntangibleAssets',
            'us-gaap:ShareBasedCompensation',
            'us-gaap:ExcessTaxBenefitFromShareBasedCompensationOperatingActivities',
            'us-gaap:DeferredIncomeTaxExpenseBenefit',
            'us-gaap:ImpairmentOfInvestments',
            'us-gaap:GainLossonSaleofBusinessIncludingDiscontinuedOperations',
            'us-gaap:OtherNoncashIncomeExpense'
        ],
        'Change In Operating Capital Abstract',
        [
            'us-gaap:IncreaseDecreaseInAccountsReceivable',
            'us-gaap:IncreaseDecreaseInIncomeTaxes',
            'us-gaap:IncreaseDecreaseInInventories',
            'us-gaap:IncreaseDecreaseInPrepaidRevenueShareExpensesAndOtherAssets',
            'us-gaap:IncreaseDecreaseInAccountsPayable',
            'us-gaap:IncreaseDecreaseInAccruedLiabilities',
            'us-gaap:IncreaseDecreaseInAccruedRevenueShare',
            'us-gaap:IncreaseDecreaseInDeferredRevenue',
            'us-gaap:NetCashProvidedByUsedInOperatingActivities'
        ],
        'Net Investing Cash Abstract',
        [
            'us-gaap:PaymentsToAcquirePropertyPlantAndEquipment',
            'us-gaap:PaymentsToAcquireMarketableSecurities',
            'us-gaap:ProceedsFromSaleAndMaturityOfMarketableSecurities',
            'us-gaap:PaymentsToAcquireOtherInvestments',
            'us-gaap:IncreaseDecreaseInCollateralHeldUnderSecuritiesLending',
            'us-gaap:InvestmentsInMaturitiesOfReverseRepurchaseAgreements',
            'us-gaap:ProceedsFromDivestitureOfBusinesses',
            'us-gaap:AcquisitionsNetofCashAcquiredAndPurchasesOfIntangibleandOtherAssets',
            'us-gaap:NetCashProvidedByUsedInInvestingActivities'
        ],
        'Net Financing Cash Abstract',
        [
            'us-gaap:NetProceedsPaymentsRelatedToStockBasedAwardActivities',
            'us-gaap:ExcessTaxBenefitFromShareBasedCompensationFinancingActivities',
            'us-gaap:ProceedsFromDebtNetOfIssuanceCosts',
            'us-gaap:RepaymentsOfDebt',
            'us-gaap:NetCashProvidedByUsedInFinancingActivities',
            'us-gaap:EffectOfExchangeRateOnCashAndCashEquivalents',
            'us-gaap:CashAndCashEquivalentsPeriodIncreaseDecrease',
            'us-gaap:CashAndCashEquivalentsAtCarryingValue',
            'us-gaap:CashAndCashEquivalentsAtCarryingValue'
        ],
        'Supplemental Cash Flow Abstract',
        [
            'us-gaap:IncomeTaxesPaid',
            'us-gaap:InterestPaid'
        ],
        'Noncash Disclosure Abstract',
        [
            'us-gaap:NoncashOrPartNoncashDivestitureAmountOfConsiderationReceived1',
            'us-gaap:FairValueStockBasedAwardsAssumedInConnectionWithAcquisition',
            'us-gaap:CapitalLeaseObligationsIncurred'
        ]
    ]

    def createBoxList(self):
        """Creates a list of Box objects for a cash flow report
        :return: List(TableBox())
        """
        boxList = []
        if len(self.dataSet):
            for f in self.dataSet:
                statementData = []
                statementTitle = ""
                try:
                    if f[u'ftype'] == '10-K':
                        for item in self.keys:
                            if isinstance(item, list):
                                for fact in self.filterData(f[u'children'], lambda e: e[u'id'] in item):
                                    statementData.append(fact)
                            elif "Abstract" in item:
                                name = item.replace("Abstract", "")
                                statementData.append({
                                    u'prettyLabel': [name],
                                    u'value': None,
                                    u'ftype': "blah",
                                    u'id': "",
                                })
                except Exception as e:
                    devLogger.error("could not get a fact list for CashFlowFilter: " + str(e))

                if len(statementData):
                    statementTitle =  f[u'prettyLabel'][0] + " Cash Flow" + " (" + f[u'value'] + ")"
                    boxList.append(TableBox.makeBox(statementData, statementTitle, []))
        return boxList


