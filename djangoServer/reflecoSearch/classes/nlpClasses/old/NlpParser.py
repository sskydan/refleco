#NLTK tagging

import nltk
from pickle import dump,load
import nltk.tag
from nltk.corpus import wordnet as wn
from itertools import chain
from nltk.chunk import RegexpParser
from nltk import ChartParser


import logging
devLogger = logging.getLogger('development')

class NlpParser(object):

    with open ("/var/www/reflecho.com/djangoServer/static/nlp/dslGrammar.txt", "r") as grammarFile:
        dslGrammar = grammarFile.read()


    posMapping = {
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
    }

    tokenClassMapping = {
        u'limUpper': ['top','best','most','high',],
        u'limLower': ['bottom', 'worst', 'least', 'low']
    }

    ##
    ##This builds the base BUD tagger.
    ##
    #trainSents = brown.tagged_sents(categories='news)

    #t0 = nltk.DefaultTagger('NN')
    #t1 = nltk.UnigramTagger(trainSents, backoff=t0)
    #t2 = nltk.BigramTagger(trainSents, backoff=t1)


    ##
    ##This builds the Brill tagger.
    ##
    #templates = nltk.tag.brill.fntbl37()

    #trainer = nltk.tag.brill_trainer.BrillTaggerTrainer(budTagger, templates)
    #brillTagger = trainer.train(train_sents, max_rules=100, min_score=3)

    ## to improve performance the tagger has been saved to a file and
    ## loaded instead of creating and training a new tagger

    #load trained tagger
    try:
        input = open('/var/www/reflecho.com/djangoServer/static/nlp/brillTagger.pkl', 'rb')
        brillTagger = load(input)
        input.close
    except Exception as e:
        devLogger.error('Could not load brillTagger: ' + str(e))

    stemmer = nltk.PorterStemmer()
    lemmatizer = nltk.WordNetLemmatizer()

    @classmethod
    #given a query string, tag POS
    def tokenize(cls, queryString):
        tokens = nltk.word_tokenize(queryString)
        #stems all tokens. Works strangly is some cases. Avoid for now
        #tokens = [cls.stemmer.stem(t) for t in tokens]
        #tokens = [cls.lemmatizer.lemmatize(t) for t in tokens]

        brillTokens = cls.brillTagger.tag(tokens)

        classifiedTokens = [(lambda t: cls.tokenClassifier(t))(t) for t in brillTokens]
        return classifiedTokens

    @classmethod
    def chunk(cls, queryString):
        tokens = cls.tokenize(queryString)
        return(nltk.ne_chunk(tokens, binary=True))

    @classmethod
    def tokenClassifier(cls, token):
        if cls.posMapping.get(token[1], False):
            hyponyms = wn.synsets(token[0], cls.posMapping[token[1]])
            wordList = list(chain.from_iterable([(lambda s: s.lemma_names())(s) for s in hyponyms]))
            for k, v in cls.tokenClassMapping.iteritems():
                for i in wordList:
                    if i in v:
                        return (token[0], k)
        return token

    @classmethod
    def parseTokens(cls, tokens):
        grammar = cls.__getCFG(tokens)
        CFGParser = ChartParser(grammar)
        sentTokens = [(lambda t: t[0])(t) for t in tokens]
        syntaxTrees = CFGParser.parse(sentTokens)
        return syntaxTrees


    @classmethod
    def __getCFG(cls, tokens):
        grammar = cls.dslGrammar
        for t in tokens:
            grammar += "\n" + t[1] + ' -> ' + "'" + t[0] + "'"
        return nltk.CFG.fromstring(grammar)



