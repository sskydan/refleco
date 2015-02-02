from reflecoSearch.classes.boxClasses.TableBox import TableBox
from reflecoSearch.classes.filterClasses.Filter import Filter
import logging
devLogger = logging.getLogger('development')

class IncomeStatementFilter(Filter):
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

    def createBoxList(self):
        """
        createBoxList creates a list of Box objects for a income statement report

        Return:
            income statement Box object
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

                    boxList.append(TableBox.makeBox(statementData, "Income Statement" + " (" + f[u'value'] + ")", []))
        return boxList


__author__ = 'lboileau'
