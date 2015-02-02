from reflecoSearch.classes.boxClasses.TableBox import TableBox
from reflecoSearch.classes.filterClasses.Filter import Filter
import logging
devLogger = logging.getLogger('development')

class RatioFilter(Filter):
    """EXTENDS FILTER
    RatioFilter is the class for filtering out ratio data to create
    Box objects for ratio reports

    Attributes:
        keys:
            dict key mapping from ratios keywords to lists of data key values
    """
    keys = {
        'leverage': ['Leverage Ratios'],
        'liquidity': ['Liquidity Ratios'],
        'profitability': ['Profitability Ratios'],
        'valuation': ['Valuation Ratios'],
        'efficiency': ['Efficiency Ratios'],
        'all': ['Leverage Ratios', 'Liquidity Ratios', 'Profitability Ratios', 'Valuation Ratios', 'Efficiency Ratios']
    }

    def createBoxList(self, filterArg='all'):
        """
        createBoxList creates a list of Box objects for a ratio report

        Args:
            filterArg:
                string key to select a list of data fields from the 'keys' mapping

        Return:
            list of ratio Box objects
        """
        boxList = []
        if len(self.dataSet):
            #defaut filter is all ratios
            if filterArg not in self.keys:
                filterArg = 'all'
            #for all available ratios from dataSet
            for analytics in self.filterData(self.dataSet, lambda e: e[u'ftype'] == 'analytics'):
                #for each ratio in the filter
                for ratioGroup in self.filterData(analytics[u'children'], lambda e: e[u'id'] in self.keys[filterArg]):
                    ratioTableData = []
                    #for each available ratio gound in data, get the values
                    for ratio in ratioGroup[u'children']:
                        if ratio[u'value'] is not None:
                            ratioTableData.append(ratio)
                    #create a tableBox for the given ratios
                    boxList.append(TableBox.makeBox(ratioTableData, ratioGroup[u'id'], []))

        return boxList

