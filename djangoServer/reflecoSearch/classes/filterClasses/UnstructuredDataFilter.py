from reflecoSearch.classes.boxClasses.TextBox import TextBox
from reflecoSearch.classes.filterClasses.Filter import Filter
from reflecoSearch.classes.ResultParsing.semanticAnalyzer import *
import logging
devLogger = logging.getLogger('development')

class UnstructuredDataFilter(Filter):

    def createBoxList(self):
        """Creates a list of Box objects for a default report
        :return: List(TableBox())
        """
        boxList = []
        if len(self.dataSet):
            for fact in [self.dataSet]:
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
                    devLogger.error("could not get a fact list for UnstructuredDataFilter: " + str(e))

                if len(factItems):
                    for item in factItems:
                        children = list()
                        include = False
                        for textBlock in item[u'children']:
                            if textBlock[u'ftype'] == 'xbrl:unstructured:text':
                                textBlock[u'value'] = [if x == self.args.get('pred', False) y for x,y in extractPredicates(textBlock[u'value'])
                                itemTitle = factTitle + "(" + item[u'prettyLabel'][0] + ")"
                                if len(textBlock[u'value']) > 0:
                                    include = True
                                    children.append(textBlock)
                            elif textBlock[u'ftype'] == 'xbrl:unstructured:table':
                                children.append(textBlock)
                        item[u'children'] = children
                        if (include):
                            boxList.append(TextBox.makeBox(item, itemTitle))
        return boxList


