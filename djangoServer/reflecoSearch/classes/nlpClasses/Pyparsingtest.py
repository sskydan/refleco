
from reflecoSearch.pyparsing.pyparsing import *
from itertools import groupby

class Pyparsingtest(object):

    dataTypes = ['DSL', 'FILTER', 'ENTITY']

    LBRACE = Suppress(Literal('('))
    RBRACE = Suppress(Literal(')'))
    ABL = LBRACE + Suppress(Literal('ABL')) + Word(alphanums) + RBRACE
    ABN = LBRACE + Suppress(Literal('ABN')) + Word(alphanums) + RBRACE
    ABX = LBRACE + Suppress(Literal('ABX')) + Word(alphanums) + RBRACE
    AP = LBRACE + Suppress(Literal('AP')) + Word(alphanums) + RBRACE
    AT = LBRACE + Suppress(Literal('AT')) + Word(alphanums) + RBRACE
    BE = LBRACE + Suppress(Literal('BE')) + Word(alphanums) + RBRACE
    BED = LBRACE + Suppress(Literal('BED')) + Word(alphanums) + RBRACE
    BEDZ = LBRACE + Suppress(Literal('BEDZ')) + Word(alphanums) + RBRACE
    BEG = LBRACE + Suppress(Literal('BEG')) + Word(alphanums) + RBRACE
    BEM = LBRACE + Suppress(Literal('BEM')) + Word(alphanums) + RBRACE
    BEN = LBRACE + Suppress(Literal('BEN')) + Word(alphanums) + RBRACE
    BER = LBRACE + Suppress(Literal('BER')) + Word(alphanums) + RBRACE
    BEZ = LBRACE + Suppress(Literal('BEZ')) + Word(alphanums) + RBRACE
    CC = LBRACE + Suppress(Literal('CC')) + Word(alphanums) + RBRACE
    CD = LBRACE + Suppress(Literal('CD')) + Word(alphanums) + RBRACE
    CS = LBRACE + Suppress(Literal('CS')) + Word(alphanums) + RBRACE
    DO = LBRACE + Suppress(Literal('DO')) + Word(alphanums) + RBRACE
    DOD = LBRACE + Suppress(Literal('DOD')) + Word(alphanums) + RBRACE
    DOZ = LBRACE + Suppress(Literal('DOZ')) + Word(alphanums) + RBRACE
    DT = LBRACE + Suppress(Literal('DT')) + Word(alphanums) + RBRACE
    DTI = LBRACE + Suppress(Literal('DTI')) + Word(alphanums) + RBRACE
    DTS = LBRACE + Suppress(Literal('DTS')) + Word(alphanums) + RBRACE
    DTX = LBRACE + Suppress(Literal('DTX')) + Word(alphanums) + RBRACE
    EX = LBRACE + Suppress(Literal('EX')) + Word(alphanums) + RBRACE
    FW = LBRACE + Suppress(Literal('FW')) + Word(alphanums) + RBRACE
    HL = LBRACE + Suppress(Literal('HL')) + Word(alphanums) + RBRACE
    HV = LBRACE + Suppress(Literal('HV')) + Word(alphanums) + RBRACE
    HVD = LBRACE + Suppress(Literal('HVD')) + Word(alphanums) + RBRACE
    HVG = LBRACE + Suppress(Literal('HVG')) + Word(alphanums) + RBRACE
    HVN = LBRACE + Suppress(Literal('HVN')) + Word(alphanums) + RBRACE
    HVZ = LBRACE + Suppress(Literal('HVZ')) + Word(alphanums) + RBRACE
    IN = LBRACE + Suppress(Literal('IN')) + Word(alphanums) + RBRACE
    JJ = LBRACE + Suppress(Literal('JJ')) + Word(alphanums) + RBRACE
    JJR = LBRACE + Suppress(Literal('JJR')) + Word(alphanums) + RBRACE
    JJS = LBRACE + Suppress(Literal('JJS')) + Word(alphanums) + RBRACE
    JJT = LBRACE + Suppress(Literal('JJT')) + Word(alphanums) + RBRACE
    MD = LBRACE + Suppress(Literal('MD')) + Word(alphanums) + RBRACE
    NC = LBRACE + Suppress(Literal('NC')) + Word(alphanums) + RBRACE
    NN = LBRACE + Suppress(Literal('NN')) + Word(alphanums) + RBRACE
    NNS = LBRACE + Suppress(Literal('NNS')) + Word(alphanums) + RBRACE
    NP = LBRACE + Suppress(Literal('NP')) + Word(alphanums) + RBRACE
    NPS = LBRACE + Suppress(Literal('NPS')) + Word(alphanums) + RBRACE
    NR = LBRACE + Suppress(Literal('NR')) + Word(alphanums) + RBRACE
    NRS = LBRACE + Suppress(Literal('NRS')) + Word(alphanums) + RBRACE
    OD = LBRACE + Suppress(Literal('OD')) + Word(alphanums) + RBRACE
    PN = LBRACE + Suppress(Literal('PN')) + Word(alphanums) + RBRACE
    PPL = LBRACE + Suppress(Literal('PPL')) + Word(alphanums) + RBRACE
    PPLS = LBRACE + Suppress(Literal('PPLS')) + Word(alphanums) + RBRACE
    PPO = LBRACE + Suppress(Literal('PPO')) + Word(alphanums) + RBRACE
    PPS = LBRACE + Suppress(Literal('PPS')) + Word(alphanums) + RBRACE
    PPSS = LBRACE + Suppress(Literal('PPSS')) + Word(alphanums) + RBRACE
    QL = LBRACE + Suppress(Literal('QL')) + Word(alphanums) + RBRACE
    QLP = LBRACE + Suppress(Literal('QLP')) + Word(alphanums) + RBRACE
    RB = LBRACE + Suppress(Literal('RB')) + Word(alphanums) + RBRACE
    RBR = LBRACE + Suppress(Literal('RBR')) + Word(alphanums) + RBRACE
    RBT = LBRACE + Suppress(Literal('RBT')) + Word(alphanums) + RBRACE
    RN = LBRACE + Suppress(Literal('RN')) + Word(alphanums) + RBRACE
    RP = LBRACE + Suppress(Literal('RP')) + Word(alphanums) + RBRACE
    TL = LBRACE + Suppress(Literal('TL')) + Word(alphanums) + RBRACE
    TO = LBRACE + Suppress(Literal('TO')) + Word(alphanums) + RBRACE
    UH = LBRACE + Suppress(Literal('UH')) + Word(alphanums) + RBRACE
    VB = LBRACE + Suppress(Literal('VB')) + Word(alphanums) + RBRACE
    VBD = LBRACE + Suppress(Literal('VBD')) + Word(alphanums) + RBRACE
    VBG = LBRACE + Suppress(Literal('VBG')) + Word(alphanums) + RBRACE
    VBN = LBRACE + Suppress(Literal('VBN')) + Word(alphanums) + RBRACE
    VBZ = LBRACE + Suppress(Literal('VBZ')) + Word(alphanums) + RBRACE
    WDT = LBRACE + Suppress(Literal('WDT')) + Word(alphanums) + RBRACE
    WPO = LBRACE + Suppress(Literal('WPO')) + Word(alphanums) + RBRACE
    WPS = LBRACE + Suppress(Literal('WPS')) + Word(alphanums) + RBRACE
    WQL = LBRACE + Suppress(Literal('WQL')) + Word(alphanums) + RBRACE
    WRB = LBRACE + Suppress(Literal('WRB')) + Word(alphanums) + RBRACE
    PRETERM =  ABL ^ ABN ^ ABX ^ AP ^ AT ^ BE ^ BED ^ BEDZ ^ BEG ^ BEM ^ BEN ^ BER ^ BEZ ^ CC ^ CD ^ CS ^ DO ^ DOD ^ DOZ ^ DT ^ DTI ^ DTS ^ DTX ^ EX ^ FW ^ HL ^ HV ^ HVD ^ HVG ^ HVN ^ HVZ ^ IN ^ JJ ^ JJR ^ JJS ^ JJT ^ MD ^ NC ^ NN ^ NNS ^ NP ^ NPS ^ NR ^ NRS ^ OD ^ PN ^ PPL ^ PPLS ^ PPO ^ PPS ^ PPSS ^ QL ^ QLP ^ RB ^ RBR ^ RBT ^ RN ^ RP ^ TL ^ TO ^ UH ^ VB ^ VBD ^ VBG ^ VBN ^ VBZ ^ WDT ^ WPO ^ WPS ^ WQL ^ WRB

    ATTRIBUTE = LBRACE + Suppress(Literal('attribute')) + OneOrMore(Word(alphanums)) + RBRACE
    COMPANY = LBRACE + Literal('company') + OneOrMore(Word(alphanums)) + RBRACE

    ATTR = LBRACE + Suppress(Literal('ATTR')) + (ATTRIBUTE) + RBRACE
    EXPR = LBRACE + Literal('FILTER') + ATTR + RBRACE
    FUNCTION = LBRACE + Suppress(Literal('FUNCTION')) + EXPR + RBRACE
    ENTITY = LBRACE + Suppress(Literal('ENTITY')) + (COMPANY) + RBRACE
    FUNCTIONLIST = Forward()
    FUNCTIONLIST << LBRACE + Suppress("FUNCTIONLIST") + FUNCTION + Optional(FUNCTIONLIST) + RBRACE

    DSL = Group(LBRACE + Suppress(Literal('DSL')) + Group(ENTITY) + Group(FUNCTIONLIST) + RBRACE)
    QUERY = LBRACE + Suppress(Literal('QUERY')) + OneOrMore(DSL) + RBRACE

    @classmethod
    def parseAST(cls, ast):
        for tree in ast:
            test = cls.QUERY.parseString(tree.pprint())
            dslStringList = []
            for dsl in test.asList():
                dslString = ""
                for e in dsl:
                    dslString += e[0]
                    dslString += ('("' + ' '.join(e[1:]) + '")')
                    dslString += ' '
                dslStringList.append(dslString)
        return dslStringList


