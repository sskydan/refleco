from django.conf import settings
import re
import requests
import nltk
from pickle import dump,load
import nltk.tag
from dateutil.parser import _timelex, parser
import logging
devLogger = logging.getLogger('development')

#TODO include wordet for fuzzy string matching
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

#stemmer = nltk.PorterStemmer()
#lemmatizer = nltk.WordNetLemmatizer()

class QueryTagger(object):
    """Creates a list of (word, tag) pairs
    """

    @classmethod
    def tagQuery(cls, queryString):
        """
        :param queryString: String query to tag
        :return: List((word, tag))
        """
        posTags = cls.tagPOS(queryString)
        FilterTags = cls.tagFilters(posTags)
        #dateTags = cls.tagDates(reportTags)
        #TODO right now we don't have a reliable list of all base pos tag. NER needs base tags
        pos = [t for w,t in posTags[:]]
        nerTags = cls.tagNER(FilterTags, pos)
        taggedTokens = nerTags

        devLogger.info("Fully tagged tokens are: " + str(taggedTokens))
        #TODO we have to replace brackets because DSL parser used brackets as stop chars
        return list(map(lambda e: (e[0].replace('(', '{').replace(')', '}'), e[1]), taggedTokens))

    @classmethod
    def tagPOS(cls, queryString):
        """
        :param queryString: String to tag
        :return: List((word,tag)) base pos tagging
        """
        tokens = nltk.word_tokenize(queryString)
        #stems all tokens. Works strangly is some cases. Avoid for now
        #tokens = [cls.stemmer.stem(t) for t in tokens]
        #tokens = [cls.lemmatizer.lemmatize(t) for t in tokens]
        classifiedTokens = brillTagger.tag(tokens)
        devLogger.info("POS tagged tokens are: " + str(classifiedTokens))
        return classifiedTokens

    @classmethod
    def tagNER(cls, tokenList, pos):
        """Tag a given sting input with NER tags
        :param tokenList: List((word,tag)) of POS tagged tokens
        :param pos: temp list of pos tags. in future we will have a defined list
        :return: List((word,tag)) NER tagged tokens
        """

        def NERSplit(inputTokens):
            """Walkes a list of tokens and finds groups of bas pos tagged tokens i.e non refleco specific
            :param inputTokens: list of taged tokens
            :return: List((word, tag)) base pos tagged tokens
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
            """Gets NER from backend
            :param tokens: List((word,tag)) tokens
            """
            #get the original string
            tokenString = re.sub(r' (?=\W)', '', " ".join([w for w,t in tokens]))

            NERTokens = []
            try:
                nerItems = requests.get(settings.CORE_HOST + 'ner?search=' + tokenString)
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

        devLogger.info("NER tagged tokens are :" + str(tokenList))
        return tokenList

    @classmethod
    def tagDates(cls, classifiedTokens):
        """tags date tokens while changing the date string into and datetime object
        :param classifiedTokens: List((word,tag)) tagged tokens
        :return: List((word,tag)) tagged tokens with dates tagged
        """
        p = parser()
        info = p.info
        def timetoken(token):
            """true if a given token could be part of a datetime string
            :param token: string
            :return: Boolean
            """
            try:
                if 1900 < float(token) < 3000:
                    return True
            except ValueError:
                pass
            return any(f(token) for f in (info.jump,info.weekday,info.month,info.hms,info.ampm,info.pertain,info.utczone,info.tzoffset))

        def timesplit(inputString):
            """Walkes a string and finds groups of datetime strings
            :param inputString: string
            :return: list of datetime strings
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

        devLogger.info("Date tagged tokens are: " + str(classifiedTokens))
        return classifiedTokens

    @classmethod
    def tagFilters(cls, classifiedTokens):
        """finds and tags known Filter tokens
        :param classifiedTokens: List((word,tag)) tagged tokens
        :return: List((word,tag)) tagged tokens with filters tagged
        """
        query = " ".join([w for w,t in classifiedTokens])
        filters = [('cash flow','CASHFLOW'), ('balance sheet','BALANCESHEET'), ('income statement', 'INCOMESTMT')]
        for f in filters:
            if f[0].lower() in query.lower():
                replaceTokens = cls.__getTokensFromRaw(classifiedTokens, f[0])
                cls.__replaceTokens(classifiedTokens, replaceTokens, [f])

        devLogger.info("Filter tagged tokens are: " + str(classifiedTokens))
        return classifiedTokens


    @classmethod
    def __replaceTokens(cls, tokenList, oldTokens, newTokens):
        """replace a set of tokens with new tokens
        :param tokenList: List((word, tag)) which will have tokens replaced
        :param oldTokens: List((word, tag)) tokens to be replaced
        :param newTokens: List((word, tag)) tokens to replace with
        """
        try:
            startIndex = tokenList.index(oldTokens[0])
            endIndex = tokenList.index(oldTokens[-1])
            tokenList[startIndex:endIndex+1] = newTokens
            devLogger.info("Replaced token: " + str(oldTokens) + " WITH " + str(newTokens))
        except Exception as e:
            devLogger.warn("Could not replace " + str(oldTokens) + ":" + str(e))

    @classmethod
    def __getTokensFromRaw(cls, tokenList, rawText):
        """gets a list of tokens given a raw string
        :param tokenList: List((word,tag)) to get from
        :param rawText: String text for which we want tokens for
        :return: List((word,tag)) tokens for the raw string
        """
        NERWords = [x for x in re.split('(\W+)',rawText) if x]
        startIndex = [i for i, t in enumerate(tokenList) if t[0] == NERWords[0]]
        endIndex = [i for i, t in enumerate(tokenList) if t[0] == NERWords[-1]]
        return  tokenList[startIndex[0]: endIndex[0]+1]