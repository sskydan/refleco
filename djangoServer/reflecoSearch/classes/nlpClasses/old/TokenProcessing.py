#TokenProcessing is used for post query-tagging processing
#in the future we will want this class to consider phrases and context to
#get a better understanding of what the query is really looking for. As of now
#all we do is look for key words that correspond to filter classes.

from reflecoSearch.classes.filterClasses.RatioFilter import RatioFilter
from reflecoSearch.classes.filterClasses.StatementFilter import StatementFilter

import logging
devLogger = logging.getLogger('development')

class TokenProcessing(object):

    #a mapping from key words to filter classes
    mapping = {
        'cash': StatementFilter,
        'balance': StatementFilter,
        'ratio': StatementFilter,
        'income': StatementFilter,
        'ratios': RatioFilter,
        'leverage': RatioFilter
    }

    @classmethod
    #takes in tagged tokens as a list of tuples and maps phrases to report filter classes
    def postTagProcessing(cls, taggedTokens):
        #tag key words
        postTaggedTokens = [cls.isKeyWord(token) for token in taggedTokens]
        return postTaggedTokens


    @classmethod
    #if a token is a stop word, link it with the appropriate filter class
    def isKeyWord(cls, taggedToken):
        if taggedToken[0] in cls.mapping:
            return (taggedToken[0], cls.mapping[taggedToken[0]])
        return taggedToken

