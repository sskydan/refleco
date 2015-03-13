from reflecoSearch.classes.boxClasses.TextBox import TextBox
from reflecoSearch.classes.filterClasses.Filter import Filter
import logging
devLogger = logging.getLogger('development')

class DefaultDataFilter(Filter):

    def createBoxList(self):
        """Creates a list of Box objects for a default report
        :return: List(TableBox())
        """
        boxList = []
        if len(self.dataSet):
            for fact in self.dataSet:
                factItems = []
                factTitle = ""
                try:
                    if fact[u'ftype'] == '10-K':
                        if len(fact[u'children']):
                            factItems = self.filterData(fact[u'children'], lambda e: "TextBlock" in e[u'id'])
                            factTitle = fact[u'prettyLabel'] + " (" + fact[u'value'] + ")"
                    elif fact[u'ftype'] == 'refleco:entity':
                        factItems = [fact]
                except Exception as e:
                    devLogger.error("could not get a fact list for DefaultFilter: " + str(e))

                if len(factItems):
                    for item in factItems:
                        itemTitle = factTitle + "(" + item[u'prettyLabel'] + ")"
                        boxList.append(TextBox.makeBox(item, itemTitle))
        return boxList


