import settings
import requests
import logging

from reflecoSearch.classes.QueryParsing.Chunker import Chunk, Chunker

devLogger = logging.getLogger('development')

class NERTagger(object):

    @classmethod
    def getNERChunks(cls, chunkList):
        """
        :param chunkList: List(Chunk) to check with NER
        :return:List(Chunk) tagged with NER chunks
        """
        nerText = " ".join([chunk.getPhrase() for chunk in chunkList if not chunk.isRecognised()])
        NERPermutations = []
        try:
            nerItems = requests.get(settings.CORE_HOST + 'ner?search=' + nerText.replace('&', '%26'))
            if nerItems.status_code == 200:
                r = nerItems.json()
                if len(r):
                    NERPermutations = r
                else:
                    devLogger.warn("No NER results received!")
        except Exception as e:
            devLogger.error("Error querying for NER: " + str(e))

        NERChunkOptions = []
        for NERP in NERPermutations:
            if len([e for e in NERP.get('entities', []) if e.get('genus', "") == 'entity']) < 1:
                NERChunks = list(chunkList)
                for entity in NERP.get('entities', []):
                    try:
                        chunkIndex = Chunker.getChunkIndex(NERChunks, entity.get('raw'))
                        for range in chunkIndex:
                            if range:
                                newChunk = Chunk(entity.get('entity'), entity.get('genus'))
                                newChunk.setRecognized(True)
                                NERChunks[range[0]:range[-1]+1] = [newChunk]
                    except Exception as e:
                        devLogger.error("There was a problem chunking a NER result: " + str(e))
                NERChunkOptions.append(NERChunks)
        return NERChunkOptions