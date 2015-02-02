from reflecoSearch.pyparsing.pyparsing import *
from reflecoSearch.classes.nlpClasses.Parser import Parser
import logging
devLogger = logging.getLogger('development')

class ReflecoParser(Parser):
    """ EXTENDS PARSER
        RelfechoParser uses the Refleco grammar rules to parse
        the appropriate report type
    """

    with open ("/var/www/reflecho.com/djangoServer/static/nlp/reportGrammar.txt", "r") as grammarFile:
        grammar = grammarFile.read()

    #PYPARSING - DSL primary entity
    LBRACE = Suppress(Literal('('))
    RBRACE = Suppress(Literal(')'))

    #PYPARSING - report key phrases
    CASHFLOW = LBRACE + Literal('CASHFLOW') + Literal('cash flow') + RBRACE
    BALANCESHEET = LBRACE + Literal('BALANCESHEET') + Literal('balance sheet') + RBRACE
    INCOMESTATEMENT = LBRACE + Literal('INCOMESTATEMENT') + Literal('income statement') + RBRACE

    #PYPARSING - AST parsing rules


    def parseAST(self):
        """
        parseAST parses the NLTK AST into a refleco report object

        Returns:
            object refleco report
        """
        ast = self._getAST()
        reflechoReportList = []
        #for tree in ast:
            #parsedAST = self.QUERY.parseString(tree.pprint())
            #for dsl in parsedAST.asList():
            #    reflechoReport = []
            #    for e in dsl:
            #        dslString = ""
            #        dslString += e[0]
            #        dslString += ('("' + ' '.join(e[1:]) + '")')
            #        reflechoReport.append(dslString)
            #   reflechoReportList.append(' '.join(reflechoReport))
        return reflechoReportList