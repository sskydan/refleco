from reflecoSearch.classes.boxClasses.TableBox import TableBox
from reflecoSearch.classes.filterClasses.Filter import Filter
import logging
devLogger = logging.getLogger('development')

class CashFlowFilter(Filter):
    """EXTENDS FILTER
    StatementFilter is the class for filtering out statement data to create
    Box objects for financial statement reports

    Attributes:
        statementTypes:
            dict mapping statement keywords to statement key sets

        keys:
            dict key mapping from ratios keywords to lists of
            appropriate statement names / key sets
    """
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
        """
        createBoxList creates a list of Box objects for a cash flow report

        Return:
            cash flow Box object
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

                    boxList.append(TableBox.makeBox(statementData, "Cash Flow" + " (" + f[u'value'] + ")", []))
        return boxList


