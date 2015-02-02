import string
import re
import requests
import nltk
from pickle import dump,load
import nltk.tag
from dateutil.parser import _timelex, parser
from dateutil.tz import *
from datetime import *
from nltk.corpus import wordnet as wn
from itertools import chain
from nltk.chunk import RegexpParser
from nltk import ChartParser
import logging
devLogger = logging.getLogger('development')

"""
posWordnetMapping:
    maps pos tagger tags to wordnet tags (a - adjective, n - noun, v - verb, r - adverb).
    This helps with synonym tagging

keyTokenTags:
    used during the post tagging process to tag refleco specific terms

brillTagger:
    tagger used for POS tagging.

    *NOTE* this tagger is built of an initial BUD tagger

stemmer:
    NOT USED - NLTK word stemmer

lemmatizer:
    NOTE USED - NLTK lemmatizer

LIBRARY_HOST:
    root url for library api

CORE_HOST:
    root url for core api
"""

#LIBRARY_HOST = "http://localhost:7800/finbase/"
#CORE_HOST = "http://54.148.120.55:8080/coreEngine/engine/"
#LOCAL_CORE_HOST = "http://localhost:7801/engine/"
LOCAL_CORE_HOST = "http://localhost:8080/coreEngine/engine/"
#LOCAL_LIBRARY_HOST = "http://localhost:7800/finbase/"

posWordnetMapping = {
    'JJ': 'a',
    'JJS': 'a',
    'JJR': 'a',
    'JJT': 'a',
    'NN': 'n',
    'NNS': 'n',
    'NP': 'n',
    'NPS': 'n',
    'NR': 'n',
    'NRS': 'n',
    'PN': 'n',
    'PP': 'n',
    'PPL': 'n',
    'PPLS': 'n',
    'PPO': 'n',
    'PPS': 'n',
    'PPSS': 'n',
    'RB': 'r',
    'RBR': 'r',
    'RBT': 'r',
    'RN': 'r',
    'RP': 'r',
    'VB': 'v',
    'VBD': 'v',
    'VBG': 'v',
    'VBN': 'v',
    'VBZ': 'v',
    'ABL': '',
    'ABN': '',
    'ABX': '',
    'AP': '',
    'AT': '',
    'BE': '',
    'BED': '',
    'BEDZ': '',
    'BEG': '',
    'BEM': '',
    'BEN': '',
    'BER': '',
    'BEZ': '',
    'CC': '',
    'CD': '',
    'CS': '',
    'DO': '',
    'DOD': '',
    'DOZ': '',
    'DT': '',
    'DTI': '',
    'DTS': '',
    'DTX': '',
    'EX': '',
    'FW': '',
    'HL': '',
    'HV': '',
    'HVD': '',
    'HVG': '',
    'HVN': '',
    'HVZ': '',
    'IN': '',
    'MD': '',
    'NC': '',
    'OD': '',
    'QL': '',
    'QLP': '',
    'RP': '',
    'TL': '',
    'TO': '',
    'UH': '',
    'WDT': '',
    'WPO': '',
    'WPS': '',
    'WQL': '',
    'WRB': '',
}


# #
# #This builds the base BUD tagger.
# #
# trainSents = nltk.corpus.brown.tagged_sents(categories='news')
# model = {'>': 'GREATERTHAN',
#         '<': 'LESSTHAN',
#         '>=': 'GTEQUAL',
#         '<=': 'LTEQUAL',
#         '=': 'EQUAL',
#         '$': 'USD',
#         }
# t0 = nltk.DefaultTagger('NN')
# t1 = nltk.UnigramTagger(model=model, backoff=t0)
# t2 = nltk.UnigramTagger(trainSents, backoff=t1)
# t3 = nltk.BigramTagger(trainSents, backoff=t2)
#
# budTagger = t3
#
# #
# #This builds the Brill tagger.
# #
# templates = nltk.tag.brill.fntbl37()
#
# trainer = nltk.tag.brill_trainer.BrillTaggerTrainer(budTagger, templates)
# brillTagger = trainer.train(trainSents, max_rules=100, min_score=3)
#
# #Save tagger
# try:
#     output = open('static/nlp/brillTaggerWithModel.pkl', 'wb')
#     dump(brillTagger, output, -1)
#     output.close
#     devLogger.info("Saved tagger to file: static/nlp/brillTaggerWithModel.pkl")
# except Exception as e:
#     devLogger.error("Could not save tagger: " + str(e))

## to improve performance the tagger has been saved to a file and
## loaded instead of creating and training a new tagger

#load trained tagger
try:
    input = open('static/nlp/brillTaggerWithModel.pkl', 'rb')
    brillTagger = load(input)
    input.close
except Exception as e:
    devLogger.error('Could not load brillTagger: ' + str(e))

stemmer = nltk.PorterStemmer()
lemmatizer = nltk.WordNetLemmatizer()

class QueryTagger(object):
    """
    QueryTagger inserts POS tags into a given text query.
    It is responsible for the POS tagger as well as post
    processing. Post processing furth tags refleco specific
    terms to be used during parsing.
    """

    @classmethod
    def tagQuery(cls, queryString):
        """
        tagQuery identifies all known entities in a query
        Args:
            queryString:
                string query
        Return:
            list tagged Query
        """
        posTags = cls.tagPOS(queryString)
        #temp pass pos to tagNER until we have a true list of all tags
        pos = [t for w,t in posTags[:]]

        reportTags = cls.tagReports(posTags)
        dateTags = cls.tagDates(reportTags)
        #temp pass pos tags until we have a true list of all tags
        nerTags = cls.tagNER(dateTags, pos)

        taggedTokens = nerTags
        devLogger.info("QueryTagger - Fully tagged tokens are: " + str(taggedTokens))

        return list(map(lambda e: (e[0].replace('(', '{').replace(')', '}'), e[1]), taggedTokens))


    @classmethod
    def tagPOS(cls, queryString):
        """
        Tag a given sting input with POS tags.

        Args:
            quertString: a given string. This should be a query sentence.

        Returns:
            A list of tuples, where each tuple is a (word, tag) pair.
        """

        tokens = nltk.word_tokenize(queryString)
        #stems all tokens. Works strangly is some cases. Avoid for now
        #tokens = [cls.stemmer.stem(t) for t in tokens]
        #tokens = [cls.lemmatizer.lemmatize(t) for t in tokens]

        classifiedTokens = brillTagger.tag(tokens)

        #classifiedTokens = [(lambda t: (t[0], keyTokenTags.get(t[0], t[1])))(t) for t in brillTokens]
        devLogger.info("QueryTagger - POS tagged tokens are: " + str(classifiedTokens))
        return classifiedTokens

    @classmethod
    def tagNER(cls, tokenList, pos):
        """
        Tag a given sting input with POS tags while using NER.

        Args:
            tokenList: a list of POS tagged tokens
            pos: temp list of pos tags. in future we will have a global
        Returns:
            A list of tuples, where each tuple is a (word, tag) pair.
        """

        def NERSplit(inputTokens):
            """
            NERSplit walkes a list of tokens and finds groups
            of unrecognized tokens
            Args: inputTokens: list of taged tokens
            Returns: list of unrecognized tokens
            """
            batch = []
            regexp = re.compile(r'[0-9\<\>]')
            for token in inputTokens:
                if token[1] in pos and regexp.search(token[0]) is None:
                    batch.append(token)
                else:
                    if batch:
                        yield batch
                        batch = []
            if batch:
                yield batch

        def getNER(tokens):
            tokenString = re.sub(r' (?=\W)', '', " ".join([w for w,t in tokens]))
            #get ner tags from dataserver
            NERTokens = []
            try:
                nerItems = requests.get(LOCAL_CORE_HOST + 'ner?search=' + tokenString)
                if nerItems.status_code == 200:
                    r = nerItems.json()
                    if len(r):
                        NERTokens = r
                    else:
                        devLogger.info("No NER results received!")
            except Exception as e:
                devLogger.error("Error querying for NER: " + str(e))

            for NE in NERTokens:
                replaceTokens = cls.__getTokensFromRaw(tokens, NE['raw'])
                cls.__replaceTokens(tokens, replaceTokens, [(NE['entity'], NE['genus'])])
            return tokens

        for ur in NERSplit(tokenList):
            NERTokens = getNER(ur[:])
            cls.__replaceTokens(tokenList, ur, NERTokens)

        return tokenList

    @classmethod
    def tagDates(cls, classifiedTokens):
        """
        tagDates finds and tags date tokens while changing the
        date string into and datetime object

        Args:
            classifiedTokens:
                list of tuples of classified tokens

        Returns:
            list of tuples of classified tokens with dates
            tagged and datetime objects created
        """
        p = parser()
        info = p.info
        def timetoken(token):
            """
            timeToken is true if a given token could be
            part of a datetime string
            Args:
                token: string
            Return: Boolean
            """
            try:
                if 1900 < float(token) < 3000:
                    return True
            except ValueError:
                pass
            return any(f(token) for f in (info.jump,info.weekday,info.month,info.hms,info.ampm,info.pertain,info.utczone,info.tzoffset))

        def timesplit(inputString):
            """
            timeSplit walkes a string and finds groups
            of datetime strings
            Args: inputString: string
            Returns: list of datetime strings
            """
            batch = []
            for token in _timelex(inputString):
                if token == "and":
                    if batch:
                        yield " ".join(batch)
                        batch = []
                if timetoken(token):
                    if info.jump(token):
                        continue
                    batch.append(token)
                else:
                    if batch:
                        yield " ".join(batch)
                        batch = []
            if batch:
                yield " ".join(batch)

        tokenWords = [w for w,t in classifiedTokens]
        for date in timesplit(" ".join(tokenWords)):
            replaceTokens = cls.__getTokensFromRaw(classifiedTokens, date)
            cls.__replaceTokens(classifiedTokens, replaceTokens, [(p.parse(date).strftime("%Y-%m-%d"), 'DATE')])
        return classifiedTokens

    @classmethod
    def tagReports(cls, classifiedTokens):
        """
        tagReports finds and tags report tokens

        Args:
            classifiedTokens:
                list of tuples of classified tokens

        Returns:
            list of tuples of classified tokens with reports
            tagged
        """
        query = " ".join([w for w,t in classifiedTokens])
        reports = [('cash flow','CASHFLOW'), ('balance sheet','BALANCESHEET'), ('income statement', 'INCOMESTMT')]
        for r in reports:
            if r[0] in query:
                replaceTokens = cls.__getTokensFromRaw(classifiedTokens, r[0])
                cls.__replaceTokens(classifiedTokens, replaceTokens, [r])
        return classifiedTokens


    @classmethod
    def __replaceTokens(cls, tokenList, oldTokens, newTokens):
        try:
            startIndex = tokenList.index(oldTokens[0])
            endIndex = tokenList.index(oldTokens[-1])
            tokenList[startIndex:endIndex+1] = newTokens
            devLogger.info("QueryTagger - Replaced token: " + str(oldTokens) + " WITH " + str(newTokens))
        except Exception as e:
            devLogger.warn("QueryTagger - Could not replace " + str(oldTokens) + ":" + str(e))

    @classmethod
    def __getTokensFromRaw(cls, tokenList, rawText):
        NERWords = [x for x in re.split('(\W+)',rawText) if x]
        startIndex = [i for i, t in enumerate(tokenList) if t[0] == NERWords[0]]
        endIndex = [i for i, t in enumerate(tokenList) if t[0] == NERWords[-1]]
        return  tokenList[startIndex[0]: endIndex[0]+1]