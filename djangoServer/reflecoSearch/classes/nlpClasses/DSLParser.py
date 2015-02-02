from reflecoSearch.classes.filterClasses.CashFlowFilter import CashFlowFilter
from reflecoSearch.classes.filterClasses.BalanceSheetFilter import BalanceSheetFilter
from reflecoSearch.classes.filterClasses.IncomeStatementFilter import IncomeStatementFilter
from reflecoSearch.classes.filterClasses.StatementFilter import StatementFilter
from reflecoSearch.pyparsing.pyparsing import *
from reflecoSearch.classes.nlpClasses.Parser import Parser
import logging
devLogger = logging.getLogger('development')

class DSLParser(Parser):
    """ EXTENDS PARSER
        DSLParser uses the DSL grammar rules to parse a
        DSL query

        attributes:
            grammar:
                reads the nltk dsl grammar

        NOTE* THE PYPARSING GRAMMAR RULES ARE DEFINED HERE.
    """

    with open ("/var/www/reflecho.com/djangoServer/static/nlp/dslGrammar.txt", "r") as grammarFile:
        grammar = grammarFile.read()


    #PYPARSING - preterminal definitions
    LBRACE = Suppress(Literal('('))
    RBRACE = Suppress(Literal(')'))
    WRD = Regex("[0-9a-zA-Z_\-\,\.\?\!\>\<\=\/\:\&\{\}\+]+")
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
    PRETERM =  ABL ^ ABN ^ ABX ^ AP ^ AT ^ BE ^ BED ^ BEDZ ^ BEG ^ BEM ^ BEN ^ BER ^ BEZ ^ CC ^ CD ^ CS ^ DO ^ DOD ^ DOZ ^ DT ^ DTI ^ DTS ^ DTX ^ EX ^ FW ^ HL ^ HV ^ HVD ^ HVG ^ HVN ^ HVZ ^ IN ^ JJ ^ JJR ^ JJS ^ JJT ^ MD ^ NC ^ NN ^ NNS ^ NP ^ NPS ^ NR ^ NRS ^ OD ^ PN ^ PPL ^ PPLS ^ PPO ^ PPS ^ PPSS ^ QL ^ QLP ^ RB ^ RBR ^ RBT ^ RN ^ RP ^ TL ^ TO ^ UH ^ VB ^ VBD ^ VBG ^ VBN ^ VBZ ^ WDT ^ WPO ^ WPS ^ WQL ^ WRB
    UKWORD = Group(LBRACE + Literal('WORD') + PRETERM + RBRACE)

    #PYPARSING - DSL primary entity
    company = Group(LBRACE + Literal('company') + OneOrMore((WRD)) + RBRACE)
    entity = Group(LBRACE + Literal('entity') + OneOrMore((WRD)) + RBRACE)
    relation = LBRACE + Literal('relation') + OneOrMore((WRD)) + RBRACE
    attribute = LBRACE + Literal('attribute') + OneOrMore((WRD)) + RBRACE
    CASHFLOW = LBRACE + Literal('CASHFLOW') + OneOrMore((WRD)) + RBRACE
    BALANCESHEET = LBRACE + Literal('BALANCESHEET') + OneOrMore((WRD)) + RBRACE
    INCOMESTMT = LBRACE + Literal('INCOMESTMT') + OneOrMore((WRD)) + RBRACE
    REPORT = Group(LBRACE + Suppress(Literal('REPORT')) + (CASHFLOW ^ BALANCESHEET ^ INCOMESTMT) + RBRACE)
    DATE = Group(LBRACE + Literal('DATE') + WRD + RBRACE)
    RELATION = LBRACE + Suppress(Literal('RELATION')) + relation + RBRACE
    ATTRIBUTE = LBRACE + Suppress(Literal('ATTRIBUTE')) + attribute + RBRACE
    COMPANY = LBRACE + Suppress(Literal('COMPANY')) + company + RBRACE
    ENTITY = LBRACE + Suppress(Literal('ENTITY')) + entity + RBRACE
    GREATERTHAN =  LBRACE + Literal('GREATERTHAN') + Suppress(WRD) + RBRACE
    LESSTHAN =  LBRACE + Literal('LESSTHAN') + Suppress(WRD) + RBRACE
    EQUAL =  LBRACE + Literal('EQUAL') + Suppress(WRD) + RBRACE
    GTEQUAL =  LBRACE + Literal('GTEQUAL') + Suppress(WRD) + RBRACE
    LTEQUAL =  LBRACE + Literal('LTEQUAL') + Suppress(WRD) + RBRACE
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
    DSLPE = Group(LBRACE + Literal('DSLPE') + (SUBJECT ^ FUNCTION) + RBRACE)
    QBODY = Forward()
    QUERYOBJ = LBRACE + Suppress(Literal("QUERYOBJ")) + (DSLPE ^ FILTEROBJECT ^ UKWORD) + RBRACE
    QBODY << LBRACE + Suppress(Literal('QBODY')) + QUERYOBJ + Optional(QBODY) + RBRACE
    IS = LBRACE + Suppress(Literal('IS')) + (BE ^ BED ^ BEDZ ^ BER ^ BEZ) + RBRACE
    WHICHQ = LBRACE + Suppress(Literal('WHICHQ')) + WPS + IS + QBODY + RBRACE
    HOWQ = LBRACE + Suppress(Literal('WHICHQ')) + WRB + IS + QBODY + RBRACE
    WHATQ = LBRACE + Suppress(Literal('WHICHQ')) + WDT + IS + QBODY + RBRACE
    QUESTION = Group(LBRACE + Suppress(Literal('QUESTION')) + (WHICHQ ^ HOWQ ^ WHATQ ^ QBODY) + RBRACE)
    QUERY = LBRACE + Suppress(Literal('QUERY')) + OneOrMore(QUESTION) + RBRACE


    DSLOBJ = Suppress(SkipTo(company ^ FILTER)) + (company ^ FILTER)

    def parseAST(self):
        """
        parseAST parses the NLTK AST into a dsl qeury string

        Returns:
            string dsl query
        """
        ast = self._getAST()
        dslItems = []
        filterObjects = []

        # def processASTItem(x):
        #     return{
        #         'DSLPE' : dslItems.append(self.getDSLStrings(x)),
        #         'FILTEROBJECT' : filterObjects.append(self.getFilterObjects(x)),
        #     }.get(x[0], None)

        for tree in ast[:1]:
            parsedAST = self.QUERY.parseString(tree.pprint())
            devLogger.info("DSLParser.py - parsed AST: " + str(parsedAST))
            for parsed in parsedAST.asList():
                dslObj = dslString()
                for item in parsed:
                    if item[0] == 'DSLPE':
                        dslObj.addDSLE(item[1:])
                    if item[0] == 'FILTEROBJECT':
                        filterObjects.append(self.getFilterObjects(item))
                dslItems.append(dslObj.getString())
        devLogger.info('DSLParser - DSL query list is: ' + str(dslItems))
        devLogger.info('DSLParser - FilterObjects list is: ' + str(filterObjects))

        #return only first ast parse. we will have to rank and choose 'best' one later
        return dslItems, filterObjects


    def getFilterObjects(self, parsedItem):
        """
        getFilterObject gets the filter objects

        Args:
            parsedItems:
                list of parsed query items
        Returns:
            filter items
        """
        def filterSwitch(x):
            return {
                'CASHFLOW': CashFlowFilter,
                'BALANCESHEET': BalanceSheetFilter,
                'INCOMESTMT': IncomeStatementFilter,
            }.get(x, None)

        return filterSwitch(parsedItem[1][0])





class dslString(object):

    def __init__(self):
        self.string = "company "
        self.subject = "*"
        self.mods = []

    def getString(self):
        if len(self.mods):
            return self.string + " ".join(list(map(lambda e: self.subject + e, self.mods)))
        else:
            return self.string + self.subject

    def addSubject(self, DSLEntity):
        s = list(map(lambda e: e.replace('{', '(').replace('}', ')'), DSLEntity))
        self.subject = '"' + ' '.join(s[1:]) + '"'
        self.string = s[0].lower() + " "

    def addDSLE(self, DSLEntity):
        def getFilter(d):
            s = list(map(lambda e: e.replace('{', '(').replace('}', ')'), d))
            def filterSwitch(x):
                return {
                    'attribute': '@',
                    'relation': '.',
                }.get(x, False)

            m = filterSwitch(s[1])
            if m:
                return m + '"' + ' '.join(s[2:]) + '"'
            return ""

        def getModifier(d):
            def modifierSwitch(x):
                return {
                    'GREATERTHAN': '>',
                    'LESSTHAN': '<',
                    'EQUAL': '<>',
                    'GTEQUAL': '>>',
                    'LTEQUAL': '<<',
                }.get(x, False)

            m = modifierSwitch(d[1])
            if m:
                cleanNum = d[2].replace(",", "").split(".", 1)[0]
                return m + cleanNum
            return ""

        s = ""
        a = ""
        for d in DSLEntity:
            if d[0] == 'company' or d[0] == 'entity':
                self.addSubject(d)
            elif d[0] == 'FILTER':
                a = getFilter(d)
                s = s + a
            elif d[0] == 'MODIFIER':
                s = s + getModifier(d)

        if len(s):
            self.mods.append(s)
            self.mods.append(a)





