import nltk
from nltk import ChartParser
from reflecoSearch.classes.filterClasses.BalanceSheetFilter import BalanceSheetFilter
from reflecoSearch.classes.filterClasses.IncomeStatementFilter import IncomeStatementFilter
from reflecoSearch.classes.filterClasses.CashFlowFilter import CashFlowFilter
from reflecoSearch.classes.filterClasses.DefaultDataFilter import DefaultDataFilter
from reflecoSearch.classes.filterClasses.UnstructuredDataFilter import UnstructuredDataFilter
from reflecoSearch.pyparsing.pyparsing import *
from reflecoSearch.classes.DSLString import DSLString
from django.conf import settings
import logging
devLogger = logging.getLogger('development')

tokenGrammar = ""
try:
    with open(settings.GRAMMAR_DIR + "dslGrammar.txt", "r") as grammarFile:
        grammar = grammarFile.read()
    grammarFile.close()

    with open(settings.GRAMMAR_DIR + "POSPreterminals.txt", "r") as POSPreterminalsFile:
        POSpreterminals = POSPreterminalsFile.read()
    POSPreterminalsFile.close()

    with open(settings.GRAMMAR_DIR + "reflecoPreterminals.txt", "r") as reflecoPreterminalsFile:
        reflecoPreterminals = reflecoPreterminalsFile.read()
    reflecoPreterminalsFile.close()

    tokenGrammar = grammar
    tokenGrammar += "\n" + reflecoPreterminals
    tokenGrammar += "\n" + POSpreterminals
except Exception as e:
    devLogger.error("Could not load grammar: " + str(e))


class QueryParser(object):
    #PYPARSING preterminal definitions
    LBRACE = Suppress(Literal('('))
    RBRACE = Suppress(Literal(')'))
    WRD = Regex("[0-9a-zA-Z_\-\—\,\.\?\!\>\<\=\/\:\;\&\{\}\+\$]+")
    ABL = LBRACE + Suppress(Literal('ABL')) + WRD + RBRACE
    ABN = LBRACE + Suppress(Literal('ABN')) + WRD + RBRACE
    ABX = LBRACE + Suppress(Literal('ABX')) + WRD + RBRACE
    AP = LBRACE + Suppress(Literal('AP')) + WRD + RBRACE
    AT = LBRACE + Suppress(Literal('AT')) + WRD + RBRACE
    BE = LBRACE + Suppress(Literal('BE')) + WRD + RBRACE
    BED = LBRACE + Suppress(Literal('BED')) + WRD + RBRACE
    BEDZ = LBRACE + Suppress(Literal('BEDZ')) + WRD + RBRACE
    BEG = LBRACE + Suppress(Literal('BEG')) + WRD + RBRACE
    BEM = LBRACE + Suppress(Literal('BEM')) + WRD + RBRACE
    BEN = LBRACE + Suppress(Literal('BEN')) + WRD + RBRACE
    BER = LBRACE + Suppress(Literal('BER')) + WRD + RBRACE
    BEZ = LBRACE + Suppress(Literal('BEZ')) + WRD + RBRACE
    CC = LBRACE + Suppress(Literal('CC')) + WRD + RBRACE
    CD = LBRACE + Suppress(Literal('CD')) + WRD + RBRACE
    CS = LBRACE + Suppress(Literal('CS')) + WRD + RBRACE
    DO = LBRACE + Suppress(Literal('DO')) + WRD + RBRACE
    DOD = LBRACE + Suppress(Literal('DOD')) + WRD + RBRACE
    DOZ = LBRACE + Suppress(Literal('DOZ')) + WRD + RBRACE
    DT = LBRACE + Suppress(Literal('DT')) + WRD + RBRACE
    DTI = LBRACE + Suppress(Literal('DTI')) + WRD + RBRACE
    DTS = LBRACE + Suppress(Literal('DTS')) + WRD + RBRACE
    DTX = LBRACE + Suppress(Literal('DTX')) + WRD + RBRACE
    EX = LBRACE + Suppress(Literal('EX')) + WRD + RBRACE
    FW = LBRACE + Suppress(Literal('FW')) + WRD + RBRACE
    HL = LBRACE + Suppress(Literal('HL')) + WRD + RBRACE
    HV = LBRACE + Suppress(Literal('HV')) + WRD + RBRACE
    HVD = LBRACE + Suppress(Literal('HVD')) + WRD + RBRACE
    HVG = LBRACE + Suppress(Literal('HVG')) + WRD + RBRACE
    HVN = LBRACE + Suppress(Literal('HVN')) + WRD + RBRACE
    HVZ = LBRACE + Suppress(Literal('HVZ')) + WRD + RBRACE
    IN = LBRACE + Suppress(Literal('IN')) + WRD + RBRACE
    JJ = LBRACE + Suppress(Literal('JJ')) + WRD + RBRACE
    JJR = LBRACE + Suppress(Literal('JJR')) + WRD + RBRACE
    JJS = LBRACE + Suppress(Literal('JJS')) + WRD + RBRACE
    JJT = LBRACE + Suppress(Literal('JJT')) + WRD + RBRACE
    MD = LBRACE + Suppress(Literal('MD')) + WRD + RBRACE
    NC = LBRACE + Suppress(Literal('NC')) + WRD + RBRACE
    NN = LBRACE + Suppress(Literal('NN')) + WRD + RBRACE
    NNS = LBRACE + Suppress(Literal('NNS')) + WRD + RBRACE
    NP = LBRACE + Suppress(Literal('NP')) + WRD + RBRACE
    NPS = LBRACE + Suppress(Literal('NPS')) + WRD + RBRACE
    NR = LBRACE + Suppress(Literal('NR')) + WRD + RBRACE
    NRS = LBRACE + Suppress(Literal('NRS')) + WRD + RBRACE
    OD = LBRACE + Suppress(Literal('OD')) + WRD + RBRACE
    PN = LBRACE + Suppress(Literal('PN')) + WRD + RBRACE
    PPL = LBRACE + Suppress(Literal('PPL')) + WRD + RBRACE
    PPLS = LBRACE + Suppress(Literal('PPLS')) + WRD + RBRACE
    PPO = LBRACE + Suppress(Literal('PPO')) + WRD + RBRACE
    PPS = LBRACE + Suppress(Literal('PPS')) + WRD + RBRACE
    PPSS = LBRACE + Suppress(Literal('PPSS')) + WRD + RBRACE
    QL = LBRACE + Suppress(Literal('QL')) + WRD + RBRACE
    QLP = LBRACE + Suppress(Literal('QLP')) + WRD + RBRACE
    RB = LBRACE + Suppress(Literal('RB')) + WRD + RBRACE
    RBR = LBRACE + Suppress(Literal('RBR')) + WRD + RBRACE
    RBT = LBRACE + Suppress(Literal('RBT')) + WRD + RBRACE
    RN = LBRACE + Suppress(Literal('RN')) + WRD + RBRACE
    RP = LBRACE + Suppress(Literal('RP')) + WRD + RBRACE
    TL = LBRACE + Suppress(Literal('TL')) + WRD + RBRACE
    TO = LBRACE + Suppress(Literal('TO')) + WRD + RBRACE
    UH = LBRACE + Suppress(Literal('UH')) + WRD + RBRACE
    VB = LBRACE + Suppress(Literal('VB')) + WRD + RBRACE
    VBD = LBRACE + Suppress(Literal('VBD')) + WRD + RBRACE
    VBG = LBRACE + Suppress(Literal('VBG')) + WRD + RBRACE
    VBN = LBRACE + Suppress(Literal('VBN')) + WRD + RBRACE
    VBZ = LBRACE + Suppress(Literal('VBZ')) + WRD + RBRACE
    WDT = LBRACE + Suppress(Literal('WDT')) + WRD + RBRACE
    WPO = LBRACE + Suppress(Literal('WPO')) + WRD + RBRACE
    WPS = LBRACE + Suppress(Literal('WPS')) + WRD + RBRACE
    WQL = LBRACE + Suppress(Literal('WQL')) + WRD + RBRACE
    WRB = LBRACE + Suppress(Literal('WRB')) + WRD + RBRACE
    PRETERM = ABL ^ ABN ^ ABX ^ AP ^ AT ^ BE ^ BED ^ BEDZ ^ BEG ^ BEM ^ BEN ^ BER ^ BEZ ^ CC ^ CD ^ CS ^ DO ^ DOD ^ DOZ ^ DT ^ DTI ^ DTS ^ DTX ^ EX ^ FW ^ HL ^ HV ^ HVD ^ HVG ^ HVN ^ HVZ ^ IN ^ JJ ^ JJR ^ JJS ^ JJT ^ MD ^ NC ^ NN ^ NNS ^ NP ^ NPS ^ NR ^ NRS ^ OD ^ PN ^ PPL ^ PPLS ^ PPO ^ PPS ^ PPSS ^ QL ^ QLP ^ RB ^ RBR ^ RBT ^ RN ^ RP ^ TL ^ TO ^ UH ^ VB ^ VBD ^ VBG ^ VBN ^ VBZ ^ WDT ^ WPO ^ WPS ^ WQL ^ WRB
    UKWORD = Group(LBRACE + Literal('WORD') + PRETERM + RBRACE)

    #PYPARSING - DSL primary entity
    company = Group(LBRACE + Literal('company') + OneOrMore(WRD) + RBRACE)
    entity = Group(LBRACE + Literal('entity') + OneOrMore(WRD) + RBRACE)
    relation = LBRACE + Literal('relation') + OneOrMore(WRD) + RBRACE
    attribute = LBRACE + Literal('attribute') + OneOrMore(WRD) + RBRACE
    CASHFLOW = LBRACE + Literal('CASHFLOW') + OneOrMore(WRD) + RBRACE

    ACQUIRE = LBRACE + Literal('ACQUIRE') + OneOrMore(WRD) + RBRACE
    MERGER = LBRACE + Literal('MERGER') + OneOrMore(WRD) + RBRACE
    FORWARD = LBRACE + Literal('FORWARD') + OneOrMore(WRD) + RBRACE
    INDICATION = LBRACE + Literal('INDICATION') + OneOrMore(WRD) + RBRACE
    CONDITION = LBRACE + Literal('CONDITION') + OneOrMore(WRD) + RBRACE
    PATENT = LBRACE + Literal('PATENT') + OneOrMore(WRD) + RBRACE

    CASHFLOW = LBRACE + Literal('') + OneOrMore(WRD) + RBRACE
    BALANCESHEET = LBRACE + Literal('BALANCESHEET') + OneOrMore(WRD) + RBRACE
    INCOMESTMT = LBRACE + Literal('INCOMESTMT') + OneOrMore(WRD) + RBRACE
    REPORT = Group(LBRACE + Suppress(Literal('REPORT')) + (CASHFLOW ^ BALANCESHEET ^ INCOMESTMT ^ MERGER ^ ACQUIRE ^ FORWARD ^ INDICATION ^ CONDITION ^ PATENT) + RBRACE)
    DATE = Group(LBRACE + Literal('DATE') + WRD + RBRACE)
    RELATION = LBRACE + Suppress(Literal('RELATION')) + relation + RBRACE
    ATTRIBUTE = LBRACE + Suppress(Literal('ATTRIBUTE')) + attribute + RBRACE
    COMPANY = LBRACE + Suppress(Literal('COMPANY')) + company + RBRACE
    ENTITY = LBRACE + Suppress(Literal('ENTITY')) + entity + RBRACE
    GREATERTHAN = LBRACE + Literal('GREATERTHAN') + Suppress(WRD) + RBRACE
    LESSTHAN = LBRACE + Literal('LESSTHAN') + Suppress(WRD) + RBRACE
    EQUAL = LBRACE + Literal('EQUAL') + Suppress(WRD) + RBRACE
    GTEQUAL = LBRACE + Literal('GTEQUAL') + Suppress(WRD) + RBRACE
    LTEQUAL = LBRACE + Literal('LTEQUAL') + Suppress(WRD) + RBRACE
    USD = LBRACE + Literal('USD') + Suppress(Regex("[$]+")) + RBRACE
    UNIT = LBRACE + Literal('UNIT') + USD + RBRACE
    EQUALITY = LBRACE + Suppress(Literal('EQUALITY')) + (GREATERTHAN ^ LESSTHAN ^ EQUAL ^ GTEQUAL ^ LTEQUAL) + RBRACE
    QUANTITY = LBRACE + Suppress(Literal('QUANTITY')) + Optional(UNIT) + CD + RBRACE
    QUANTIFIER = LBRACE + Suppress(Literal('QUANTIFIER')) + EQUALITY + QUANTITY + RBRACE

    #PYPARSING - AST parsing rules
    FILTER = Group(LBRACE + Literal('FILTER') + (ATTRIBUTE ^ RELATION) + RBRACE)
    MODIFIER = Group(LBRACE + Literal('MODIFIER') + (DATE ^ QUANTIFIER) + RBRACE)
    FUNCTIONLIST = Forward()
    FUNCTION = LBRACE + Suppress(Literal('FUNCTION')) + FILTER + Optional(MODIFIER) + RBRACE
    FUNCTIONLIST << LBRACE + Suppress('FUNCTIONLIST') + FUNCTION + Optional(FUNCTIONLIST) + RBRACE
    SUBJECT = LBRACE + Suppress(Literal('SUBJECT')) + (ENTITY ^ COMPANY) + RBRACE
    FILTEROBJECT = Group(LBRACE + Literal('FILTEROBJECT') + REPORT + RBRACE)
    DSLI = Group(LBRACE + Literal('DSLI') + (SUBJECT ^ FUNCTION) + RBRACE)
    QBODY = Forward()
    QUERYOBJ = LBRACE + Suppress(Literal("QUERYOBJ")) + (DSLI ^ FILTEROBJECT ^ UKWORD) + RBRACE
    QBODY << LBRACE + Suppress(Literal('QBODY')) + QUERYOBJ + Optional(QBODY) + RBRACE
    IS = LBRACE + Suppress(Literal('IS')) + (BE ^ BED ^ BEDZ ^ BER ^ BEZ) + RBRACE
    WHICHQ = LBRACE + Suppress(Literal('WHICHQ')) + WPS + IS + QBODY + RBRACE
    HOWQ = LBRACE + Suppress(Literal('WHICHQ')) + WRB + IS + QBODY + RBRACE
    WHATQ = LBRACE + Suppress(Literal('WHICHQ')) + WDT + IS + QBODY + RBRACE
    QUESTION = Group(LBRACE + Suppress(Literal('QUESTION')) + (WHICHQ ^ HOWQ ^ WHATQ ^ QBODY) + RBRACE)
    QUERY = LBRACE + Suppress(Literal('QUERY')) + OneOrMore(QUESTION) + RBRACE

    DSLOBJ = Suppress(SkipTo(company ^ FILTER)) + (company ^ FILTER)

    def __init__(self, tokens):
        """init parser with tokens and parser build from CFG
        :param tokens: tagged query tokens
        """
        self.tokens = tokens
        self.CFGParser = ChartParser(self.__getCFG())

    def _getAST(self):
        """Gets the words from the token list and passes them
        through the parser to build an AST
        :return nltk AST
        """
        parseTokens = [t[0] for t in self.tokens]
        ASTs = []
        try:
            syntaxTrees = self.CFGParser.parse(parseTokens)
            for tree in syntaxTrees:
                ASTs.append(tree)
                devLogger.info("AST generated: " + str(tree))
            if not(len(ASTs)):
                devLogger.warn("Did not generate any AST. AST list empty.")
        except Exception as e:
            devLogger.error("Could not parse tokens into AST: " + str(e))
        return ASTs

    def __getCFG(self):
        """Creates the CFG by combining the class defined rules,
        the standard preterminal rules for POS tags -> e, and
        finally the POS to word rules for the given query
        :return nltk CFG
        """
        tg = tokenGrammar
        for t in self.tokens:
            tg += "\n" + t[1] + ' -> ' + "'" + t[0] + "'"
            devLogger.info("Preterminal added to grammar: " + str(t))
        return nltk.CFG.fromstring(tg)

    def parseAST(self):
        """Parses the NLTK AST into a DSL string and view filters
        :return (List(DSL String),List(Filter references))
        """
        ast = self._getAST()
        dslItems = []
        filterObjects = []

        #TODO right now only consider the first AST. In furutre we will have to pick best AST
        if len(ast) >= 1:
            astLimmited = ast[0]
        else:
            astLimmited = False

        if astLimmited:
            try:
                parsedAST = self.QUERY.parseString(astLimmited.pprint())
                devLogger.info("Parsed AST: " + str(parsedAST))
            except Exception as e:
                parsedAST = []
                devLogger.error("Could not parse AST: " + str(e))
            for parsed in parsedAST.asList():
                filterObjects = [self.getFilterObjects(item) for item in parsed if item[0] == 'FILTEROBJECT']
                dslStr = DSLString(filterObjects)
                for item in parsed:
                    if item[0] == 'DSLI':
                        dslStr.addDSLI(item[1:])
                dslItems.append(dslStr)
        if len(filterObjects) < 1:
                filterObjects = [DefaultDataFilter()]

        devLogger.info('DSL query list is: ' + str(dslItems))
        devLogger.info('Filter reference list is: ' + str(filterObjects))
        return dslItems, filterObjects


    def getFilterObjects(self, parsedItem):
        """Links to the appropriate filter class
        :param parsedItems: List(List()) of parsed query items
        :return Filter reference
        """
        def filterSwitch(x):
            return {
                'CASHFLOW': CashFlowFilter(),
                'BALANCESHEET': BalanceSheetFilter(),
                'INCOMESTMT': IncomeStatementFilter(),
                'ACQUIRE': UnstructuredDataFilter(pred='ACQUIRE'),
                'MERGER': UnstructuredDataFilter(pred='MERGER'),
                'FORWARD': UnstructuredDataFilter(pred='FORWARD'),
                'CONDITION': UnstructuredDataFilter(pred='CONDITION'),
                'INDICATION': UnstructuredDataFilter(pred='INDICATION'),
                'PATENT': UnstructuredDataFilter(pred='PATENT')
            }.get(x, False)

        return filterSwitch(parsedItem[1][0])
