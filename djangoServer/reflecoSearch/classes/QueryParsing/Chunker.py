import logging

devLogger = logging.getLogger('development')



class Chunker(object):
    """Chunks multiple Chunks together
    """
    #TODO this class should use dicts for faster lookups

    @classmethod
    def getPhraseIndex(cls, chunkList, phrase):
        """
        :param chunkList: List(Chunk) list of Chunks
        :param phrase: a String for a desired 'chunk'
        :return: List(List(Int)) List for each word in the phrase of the
                indices for which the word appears in the list of chunks
        """
        return [cls.getWordIndex(chunkList, word) for word in phrase.split(" ")]

    @classmethod
    def getWordIndex(cls, chunkList, word):
        """
        :param chunkList: List(Chunk) list of Chunks
        :param word: a String to look for in the chunkList
        :return: a list of indices where that word appears in chunkList
        """
        return [i for i, chunk in enumerate(chunkList) if chunk.getPhrase() == word]

    @classmethod
    def getContiguousIndex(cls, indexList, lastIndex, sol):
        """
        :param indexList: List(List(Int)) list of all indices for each word
        :param lastIndex: last index that was matched
        :param sol: solution so far
        :return: If a contiguous solution exists return it, else False
        """
        if len(indexList) < 1:
            return sol
        nextIndex = lastIndex + 1
        head, *tail = indexList
        if nextIndex in head:
            sol.append(nextIndex)
            return cls.getContiguousIndex(tail, nextIndex, sol)
        else:
            return False

    #TODO this does not check for overlapping index ranges, but i dont think it will be an issue
    @classmethod
    def getChunkIndex(cls, chunkList, phrase):
        """
        :param chunkList: List(Chunk) list of Chunks
        :param phrase: a String for a desired 'chunk'
        :return: List(List(Int)) List of contiguous indices where the given
                 phrase appears in the chunkList
        """
        indexList = cls.getPhraseIndex(chunkList, phrase)
        if len(indexList) < 1:
            return indexList
        else:
            head, *tail = indexList
            return [cls.getContiguousIndex(tail, x, list([x])) for x in head]


class Chunk(object):

    def __init__(self,phrase, tag):
        self.__phrase = phrase
        self.__tag = tag
        self.__recognized = False


    def setRecognized(self, bool):
        if bool:
            self.__recognized = True
        else:
            self.__recognized = False

    def setTag(self, tag):
        if tag:
            self.__tag = tag

    def setPhrase(self, phrase):
        if phrase:
            self.__phrase = phrase

    def getPhrase(self):
        return self.__phrase

    def getTag(self):
        return self.__tag

    def isRecognised(self):
        return self.__recognized

    def toTuple(self):
        tuple = (self.__phrase, self.__tag)
        return tuple

    def pprint(self):
        return "(" + self.__phrase + ", " + self.__tag + ")"
