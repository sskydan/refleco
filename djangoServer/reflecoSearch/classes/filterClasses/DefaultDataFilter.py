from reflecoSearch.classes.boxClasses.ListBox import ListBox
from reflecoSearch.classes.filterClasses.Filter import Filter
import logging
devLogger = logging.getLogger('development')

class DefaultDataFilter(Filter):
    """EXTENDS FILTER
    DefaultDataFilter is the class for displaying data when no filter
    has been specified
    """

    def createBoxList(self):
        """
        createBoxList creates a list of Box objects for default data

        Return:
            list of Box objects
        """
        boxList = []
        if len(self.dataSet):
            for fact in self.dataSet:
                factItems = []
                factTitle = ""
                try:
                    if fact[u'ftype'] == '10-K':
                        factItems = self.filterData(fact[u'children'], lambda e: "TextBlock" not in e[u'id'])
                        factTitle = fact[u'prettyLabel'] + " (" + fact[u'value'] + ")"
                    else:
                        factItems = [fact]
                except Exception as e:
                    devLogger.error("could not get a fact list for DefaultFilter: " + str(e))

                boxList.append(ListBox.makeBox(factItems, factTitle))

        return boxList


