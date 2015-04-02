import nltk
from pickle import load
import nltk.tag
import logging
from django.conf import settings

devLogger = logging.getLogger('development')

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
    with open(settings.GRAMMAR_DIR + 'brillTaggerWithModel.pkl', 'rb') as brillFile:
        brillTagger = load(brillFile)
        brillFile.close
except Exception as e:
    devLogger.error('Could not load brillTagger: ' + str(e))

#stemmer = nltk.PorterStemmer()
#lemmatizer = nltk.WordNetLemmatizer()

class POSTagger(object):

    @classmethod
    def tagPOS(cls, queryString):
        """
        :param queryString: String to tag
        :return: List((word,tag)) list of tokens from base POS tagging
        """
        posTokens = list()
        try:
            tokens = nltk.word_tokenize(queryString)
            #stems all tokens. Works strangly is some cases. Avoid for now
            #tokens = [cls.stemmer.stem(t) for t in tokens]
            #tokens = [cls.lemmatizer.lemmatize(t) for t in tokens]
            posTokens = brillTagger.tag(tokens)
            #devLogger.info("POS tagged tokens are: " + str(posTokens))
        except Exception as e:
            devLogger.error("Could not tag POS: " + str(e))
        return posTokens
