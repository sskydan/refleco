import re
import logging
from reflecoSearch.classes.QueryParsing.Chunker import Chunker, Chunk

devLogger = logging.getLogger('development')

class KnownEntityTagger(object):

    #These are the known entities
    knownEntities = {'cash flow': 'CASHFLOW',
                     'balance sheet': 'BALANCESHEET',
                     'income statement': 'INCOMESTMT'}

    @classmethod
    def getKnownEntities(cls, chunkList):
        """
        :param chunkList: List(Chunk) to check for known entities
        :return: List(Chunk) with known entities chunked
        """
        try:
            for entity in cls.knownEntities.keys():
                chunkIndex = Chunker.getChunkIndex(chunkList, entity)
                for range in chunkIndex:
                    if range:
                        newChunk = Chunk(entity, cls.knownEntities.get(entity, "NN"))
                        newChunk.setRecognized(True)
                        chunkList[range[0]:range[-1]+1] = [newChunk]

            numre = re.compile(r'\d{1,3}(,?\d{3})*(\.\d*)?')
            stopre = re.compile(r'[\<\>]')
            for i,chunk in enumerate(chunkList):
                phrase = chunk.getPhrase()
                if numre.search(phrase):
                    chunkList[i].setTag("CD")
                    chunkList[i].setRecognized(True)
                elif stopre.search(phrase):
                    chunkList[i].setRecognized(True)

            return chunkList
        except Exception as e:
            devLogger.error("Could not tag known entities: " + str(e))
