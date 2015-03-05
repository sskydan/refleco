from reflecoSearch.classes.QueryParsing.POSTagger import POSTagger
from reflecoSearch.classes.QueryParsing.KnownEntityTagger import KnownEntityTagger
from reflecoSearch.classes.QueryParsing.Chunker import Chunk
from reflecoSearch.classes.QueryParsing.NERTagger import NERTagger
import logging

devLogger = logging.getLogger('development')

class QueryTagger(object):
    """Tags a query
    """
    def __init__(self, text):
        self.rawText = text
        self.chunks = []

    def getPOSChunks(self):
        """
        :return: List(Chunks) POS tagged word chunks
        """
        POSTokens = POSTagger.tagPOS(self.rawText)
        chunks = []
        for token in POSTokens:
            wrd,tag = token
            chunks.append(Chunk(wrd,tag))
        return chunks

    def getKnownEntityChunks(self):
        """
        :return: List(Chunks) Known entity tagged chunks
        """
        if len(self.chunks) < 1:
            self.chunks = self.getPOSChunks()
        return KnownEntityTagger.getKnownEntities(self.chunks)


    def splitOnNer(self):
        """
        :return:List of all possible permutations of the NER tagged chunks
        """
        if len(self.chunks) < 1:
            self.chunks = self.getKnownEntityChunks()
        return NERTagger.getNERChunks(self.chunks)


