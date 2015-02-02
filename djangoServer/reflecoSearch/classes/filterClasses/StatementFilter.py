from reflecoSearch.classes.boxClasses.TableBox import TableBox
from reflecoSearch.classes.filterClasses.Filter import Filter
import logging
devLogger = logging.getLogger('development')

class StatementFilter(Filter):
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

    statementTypes = {
        'balance' : [{'name': 'Balance Sheet' , 'keys':'balanceKeys'}],
        'cash': [{'name': 'Cash Flow Statement' , 'keys':'cashFlowKeys'}],
        "income": [{'name': 'Income Statement' , 'keys':'incomeKeys'}],
        "all": [
            {'name': 'Balance Sheet' , 'keys':'balanceKeys'},
            {'name': 'Cash Flow Statement' , 'keys':'cashFlowKeys'},
            {'name': 'Income Statement' , 'keys':'incomeKeys'}
        ]
    }

    keys = {
        'balanceKeys': [
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
        ],

        'cashFlowKeys': [
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
        ],
        'incomeKeys': [
            'Revenues Abstract',
            [
                'us-gaap:Revenues'
            ],
            'Costs And Expenses Abstract',
            [
                'us-gaap:CostOfRevenue',
                'us-gaap:ResearchAndDevelopmentExpense',
                'us-gaap:SellingAndMarketingExpense',
                'us-gaap:GeneralAndAdministrativeExpense',
                'us-gaap:ChargeRelatedToResolutionOfInvestigation',
                'us-gaap:CostsAndExpenses',
                'us-gaap:OperatingIncomeLoss',
                'us-gaap:NonoperatingIncomeExpense',
                'us-gaap:IncomeLossFromContinuingOperationsBeforeIncomeTaxesMinorityInterestAndIncomeLossFromEquityMethodInvestments',
                'us-gaap:IncomeTaxExpenseBenefit',
                'us-gaap:IncomeLossFromContinuingOperations',
                'us-gaap:IncomeLossFromDiscontinuedOperationsNetOfTax',
                'us-gaap:NetIncomeLoss'
            ],
            'Earnings Per Share Abstract',
            [
                'us-gaap:IncomeLossFromContinuingOperationsPerBasicShare',
                'us-gaap:IncomeLossFromDiscontinuedOperationsNetOfTaxPerBasicShare',
                'us-gaap:EarningsPerShareBasic'
            ],
            'Earnings Per Share Diluted Abstract',
            [
                'us-gaap:IncomeLossFromContinuingOperationsPerDilutedShare',
                'us-gaap:IncomeLossFromDiscontinuedOperationsNetOfTaxPerDilutedShare',
                'us-gaap:EarningsPerShareDiluted',
                'us-gaap:AllocatedShareBasedCompensationExpense',
                'us-gaap:CostOfSalesMember',
                'us-gaap:AllocatedShareBasedCompensationExpense',
                'us-gaap:AllocatedShareBasedCompensationExpense',
                'us-gaap:ResearchAndDevelopmentExpenseMember',
                'us-gaap:AllocatedShareBasedCompensationExpense',
                'us-gaap:SellingAndMarketingExpenseMember',
                'us-gaap:AllocatedShareBasedCompensationExpense',
                'us-gaap:GeneralAndAdministrativeExpenseMember',
                'us-gaap:AllocatedShareBasedCompensationExpense',
                'us-gaap:GoogleIncorporatedMember'
            ]
        ]
    }

    def createBoxList(self, filterArg='all'):
        """
        createBoxList creates a list of Box objects for a statement report

        Args:
            filterArg:
                string key to select a list of data fields from the 'keys' mapping

        Return:
            list of statement Box objects
        """
        boxList = []
        if len(self.dataSet):
            try:
                #get 10-k statement data
                factList = self.filterData(self.dataSet, lambda e: e[u'ftype'] == '10-K')[0]['children']
            except Exception as e:
                devLogger.error("could not get a fact list for statementFilter: " + str(e))
                factList = []

            for statement in self.statementTypes.get(filterArg, self.statementTypes['all']):
                statementTableData = []
                #get data for each statement key
                for item in self.keys.get(statement['keys'], []):
                    if isinstance(item, list):
                        for fact in self.filterData(factList, lambda e: e[u'id'] in item):
                            statementTableData.append(fact)
                    elif "Abstract" in item:
                        name = item.replace("Abstract", "")
                        statementTableData.append({u'prettyLabel': name, u'value': None})

                #create a tableBox for the statement
                boxList.append(TableBox.makeBox(statementTableData, statement['name'], []))

        return boxList


