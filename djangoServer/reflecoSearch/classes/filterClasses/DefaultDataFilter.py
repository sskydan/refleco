from reflecoSearch.classes.boxClasses.TableBox import TableBox
from reflecoSearch.classes.boxClasses.TextBox import TextBox
from reflecoSearch.classes.filterClasses.Filter import Filter
from reflecoSearch.classes.ResultParsing.semanticAnalyzer import *
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
                unstructuredItems = []
                factTitle = ""
                try:
                    if fact[u'ftype'] == '10-K':
                        if len(fact[u'children']):
                            factItems, unstructuredItems = self.partitionData(fact[u'children'], lambda e: "TextBlock" not in e[u'id'])
                            factTitle = fact[u'prettyLabel'][0] + " (" + fact[u'value'] + ")"
                    elif fact[u'ftype'] == 'refleco:entity':
                        factItems = [fact]
                except Exception as e:
                    devLogger.error("could not get a fact list for DefaultFilter: " + str(e))

                if len(factItems):
                    boxList.append(TableBox.makeBox(factItems, factTitle, []))

                if len(unstructuredItems):
                    for unstructured in unstructuredItems:
                        children = list()
                        include = False
                        for textBlock in unstructured[u'children']:
                            if textBlock[u'ftype'] == 'xbrl:unstructured:text':
                                textBlock[u'value'] = set([y[0] for x,y in extractPredicates(textBlock[u'value']) if x == self.args.get('pred', False)])
                                itemTitle = factTitle + "(" + unstructured[u'prettyLabel'][0] + ")"
                                if len(textBlock[u'value']) > 0:
                                    include = True
                                    children.append(textBlock)
                            elif textBlock[u'ftype'] == 'xbrl:unstructured:table':
                                include = True
                                children.append(textBlock)
                        unstructured[u'children'] = children
                        if (include):
                            boxList.append(TextBox.makeBox(unstructured, itemTitle))

        return boxList


