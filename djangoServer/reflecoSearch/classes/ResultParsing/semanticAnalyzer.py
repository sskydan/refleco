import nltk
import nltk.tag
from pickle import load
import re
from reflecoSearch.classes.QueryParsing.POSTagger import POSTagger

def findIndsWithMatcher(matchers, tokens):
    inds = ()
    for mthrs in matchers:
        for tokenInd in range(len(tokens)):
            useInd = True 
            mthType = mthrs[0]
            for matchInd in range(len(mthrs[1])):
                mthr = mthrs[1][matchInd]
                if tokenInd + matchInd >= len(tokens) or not re.match(mthr, tokens[tokenInd + matchInd][0]):
                    useInd = False
            if useInd:
                inds = inds + ((tokenInd,mthType),)
    return inds

def findMergerInds(tokens):
    return findIndsWithMatcher((['MERGER', (r"merg.*",)],
                                ['PATENT', (r"patent.*",)],
                                ['ACQUIRE', (r"aqui[r + s].*",)] ,),tokens)

def findForwardInds(tokens):
    return findIndsWithMatcher((['FORWARD', (r"outlook.*",)],
                                ['FORWARD', (r"anticipate.*",)],
                                ['FORWARD', (r"demand.*",)],), tokens)

def findMarketInds(tokens):
    return findIndsWithMatcher((['INDICATION', (r"low.*", r"revenu.*")], 
                                ['INDICATION', (r"adver.*.*", r"impact.*")], 
                                ['INDICATION', (r"result.*", r"in")],), tokens)

def findMarketConditionInds(tokens):
    return findIndsWithMatcher((['CONDITION', (r"econ.*", r"condi.*")],
                                ['CONDITION', (r"market.*", r"condi.*")],
                                ['CONDITION', (r"cred.*", r"environ.*")],
                                ['CONDITION', (r"indic.*",)],
                                ['CONDITION', (r"credit", r"spread.*")],
                                ['CONDITION', (r"oil", r"spread.*")],
                                ['CONDITION', (r"commodity.*", r"pric.*")],), tokens)

def extractNounInfoInd(ind, tokens, posType):
    ret = ()
    idx = ind + 1
    take = False
    while idx > 0:
        if tokens[idx][1] == ".":
            idx += 1
            break
        idx -= 1
    while idx < len(tokens):
        if tokens[idx][1] == ".":
            break
        ret += (tokens[idx][0],)
        idx += 1
    ret = (" ".join([x for x in ret]), )
    if len(ret) > 0 and len(ret[0]) == 0:
        ret = ()
    return ret

NOUNS = ("NN", "NN$", "NNS", "NP", "NP$", "NPS", "NPS$", "NR", "NRS", "PN", "PN$", "PP$", "PP$$", "PPL", "PPLS", "PPO", "PPS", "PPSS")
VERBS = ("VBN", "VB", "VBD", "VBG", "VBZ")

def extractMergerInfo(tokens):
    ret = ()
    mergerInds = findMergerInds(tokens)
    for ind in mergerInds:
        ret = ret + ( (ind[1], extractNounInfoInd(ind[0], tokens, NOUNS)) ,)
    return ret

def extractForwardInfo(tokens):
    ret = ()
    mergerInds = findForwardInds(tokens)
    for ind in mergerInds:
        ret = ret + ( (ind[1], extractNounInfoInd(ind[0], tokens, VERBS)) ,)
    return ret

def extractMarketInfo(tokens):
    ret = ()
    mergerInds = findMarketInds(tokens)
    for ind in mergerInds:
        ret = ret + ( (ind[1], extractNounInfoInd(ind[0], tokens, NOUNS)) ,)
    return ret

def extractMarketConditionInfo(tokens):
    ret = ()
    mergerInds = findMarketConditionInds(tokens)
    for ind in mergerInds:
        ret = ret + ( (ind[1], extractNounInfoInd(ind[0], tokens, VERBS)) ,)
    return ret

def extractPredicates(queryString):
    posTokens = POSTagger.tagPOS(queryString)
    posTokens = [ (x[0].lower(), x[1]) for x in posTokens ]
    return extractMarketConditionInfo(posTokens) + extractMarketInfo(posTokens) + extractForwardInfo(posTokens) + extractMergerInfo(posTokens)
