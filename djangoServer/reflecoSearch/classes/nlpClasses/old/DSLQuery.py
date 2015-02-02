
from nltk.tree import ParentedTree

import logging
devLogger = logging.getLogger('development')

class DSLQuery(object):

    def __init__(self, syntaxTree, tokens):
        self.syntaxTree = ParentedTree.convert(syntaxTree)
        self.tokens = tokens


    def getDSL(self):
        queries = []
        for tree in self.syntaxTree.subtrees(filter=lambda x: x.label()=='DSL'):
            query = {}
            for entity in tree.subtrees(filter=lambda x: x.label()=='ENTITY'):
                query['ENTITY'] = self.getEntity(entity)
            for filter in tree.subtrees(filter=lambda  x: x.label()=='FILTER'):
                query['FILTER'] = self.getFilter(filter)
            queries.append(query)
        return 0

    def getFilter(self, filter):
        return filter.leaves()

    def getEntity(self, entity):
        return entity.leaves()