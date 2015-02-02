
from abc import ABCMeta, abstractmethod
import nltk
from nltk import ChartParser

import logging
devLogger = logging.getLogger('development')

class Parser(object):
    """
        Parser is the abstract parsing class from which the DSLParser
        and ReflecoParser derive.

        Attributes:
            POSpreterminals:
                grammar rules for POS to words. Initially
                all POS tags match to empty, but each
                query dynamically defines rules for the query
                tokens.

            reflecoPreterminals:
                refleco specific tags used as preterminals

            grammar:
                the grammar rules for the parser

            tokens:
                list of tokens of the form (word, tag)

            CFGParser:
                CFG parser for the class. This is an NLTK chart parser.
    """

    __metaclass__ = ABCMeta
    try:
        with open ("/var/www/reflecho.com/djangoServer/static/nlp/POSPreterminals.txt", "r") as POSPreterminalsFile:
            POSpreterminals = POSPreterminalsFile.read()
        POSPreterminalsFile.close()

        with open ("/var/www/reflecho.com/djangoServer/static/nlp/reflecoPreterminals.txt", "r") as reflecoPreterminalsFile:
            reflecoPreterminals = reflecoPreterminalsFile.read()
        reflecoPreterminalsFile.close()
    except Exception as e:
        devLogger.error("Could not load terminal grammar rules: " + str(e))

    @property
    def grammar(self):
        """
        the CFG grammar rules
        """
        pass

    def __init__(self, tokens):
        """
        init parser with tokens and parser build from CFG
        """
        self.tokens = tokens
        self.CFGParser = ChartParser(self.__getCFG())
    @abstractmethod
    def parseAST(self):
        """
        crawls the resulting AST after a parse to determine
        the meaning
        """
        pass

    def _getAST(self):
        """
        Gets the words from the token list and passes them
        through the parser to build an AST

        Return:
            nltk tree
        """
        parseTokens = [(lambda t: t[0])(t) for t in self.tokens]
        syntaxTrees = self.CFGParser.parse(parseTokens)
        ASTs = []
        for tree in syntaxTrees:
            ASTs.append(tree)
            devLogger.info("Parser.py - AST generated: " + str(tree))
        if not(len(ASTs)):
            devLogger.warn("Parser.py - Did not generate any AST. AST list empty.")
        return ASTs

    def __getCFG(self):
        """
        Creates the CFG by combining the class defined rules,
        the standard preterminal rules for POS tags -> e, and
        finally the POS to word rules for the given query

        Retrun:
            nltk CFG
        """
        tokenGrammar = self.grammar
        tokenGrammar += "\n" + self.reflecoPreterminals
        tokenGrammar += "\n" + self.POSpreterminals
        for t in self.tokens:
            tokenGrammar += "\n" + t[1] + ' -> ' + "'" + t[0] + "'"
            devLogger.info("Parser.py - Preterminal added: " + str(t))
        return nltk.CFG.fromstring(tokenGrammar)