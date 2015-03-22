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
            for matchInd in range(len(mthrs)):
                mthr = mthrs[matchInd]
                if tokenInd + matchInd >= len(tokens) or not re.match(mthr, tokens[tokenInd + matchInd][0]):
                    useInd = False
            if useInd:
                inds = inds + (tokenInd,)
    return inds

def findMergerInds(tokens):
    return findIndsWithMatcher(((r"merg.*",),
                                (r"aqui[r + s].*",),
                                (r"cease.*",),
                                (r"creat.*",),
                                (r"enter.*",),),tokens)

def findForwardInds(tokens):
    return findIndsWithMatcher(((r"outlook.*",),
                                (r"anticipate.*",),
                                (r"demand.*",),), tokens)

def findMarketInds(tokens):
    return findIndsWithMatcher(((r"low.*", r"revenu.*"), 
                                (r"adver.*.*", r"impact.*"), 
                                (r"result.*", r"in"),), tokens)

def findMarketConditionInds(tokens):
    return findIndsWithMatcher(((r"econ.*", r"condi.*"),
                                (r"market.*", r"condi.*"),
                                (r"cred.*", r"environ.*"),
                                (r"indic.*",),
                                (r"credit", r"spread.*"),
                                (r"oil", r"spread.*"),
                                (r"commodity.*", r"pric.*"),), tokens)

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
        ret = ret + extractNounInfoInd(ind, tokens, NOUNS)
    return ret

def extractForwardInfo(tokens):
    ret = ()
    mergerInds = findForwardInds(tokens)
    for ind in mergerInds:
        ret = ret + extractNounInfoInd(ind, tokens, VERBS)
    return ret

def extractMarketInfo(tokens):
    ret = ()
    mergerInds = findMarketInds(tokens)
    for ind in mergerInds:
        ret = ret + extractNounInfoInd(ind, tokens, NOUNS)
    return ret

def extractMarketConditionInfo(tokens):
    ret = ()
    mergerInds = findMarketConditionInds(tokens)
    for ind in mergerInds:
        ret = ret + extractNounInfoInd(ind, tokens, VERBS)
    return ret

def extractPredicates(queryString):
    posTokens = POSTagger.tagPOS(queryString)
    posTokens = [ (x[0].lower(), x[1]) for x in posTokens ]
    return extractMarketConditionInfo(posTokens)
