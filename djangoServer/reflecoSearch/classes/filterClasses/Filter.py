from abc import ABCMeta, abstractmethod
from itertools import filterfalse, tee
import logging
devLogger = logging.getLogger('development')

class Filter(object):
    """Abstract class for filtering fact data
    """
    __metaclass__ = ABCMeta

    def __init__(self, **kargs):
        self.dataSet = '{}'
        self.args = kargs

    @property
    def keys(self):
        """dict mapping for different lists of keys to filter by
        """
        pass

    @abstractmethod
    def createBoxList(self, *reportArgs):
        """Generates the list of appropriate box objects
        :param *reportArgs: Optional filter args
        """
        pass

    def loadData(self, data):
        """Loads a filter with a json data set
        :param data: Json data to be loaded
        """
        self.dataSet = data

    def filterData(self, iterable, predicate):
        """filters an interable set given a predicate
        :param iterable: Json data set
        :param predicate: Boolean predicate function
        :return: a filtered iterable
        """
        try:
            filtered = list(filter(predicate, iterable))
        except Exception as e:
            filtered = list()
            devLogger.error("Could not filter data: " + str(e))
        return filtered


    def partitionData(self, iterable, predicate):
        """partitions an interable given a predicate
        :param iterable: Json data set
        :param predicate: Boolean predicate function
        :return: Two iterables (matches, misses)
        """
        match, miss = list(), list()
        try:
            itr1, itr2 = tee(iterable)
            match, miss = list(filter(predicate, itr1)), list(filterfalse(predicate, itr2))
        except Exception as e:
            devLogger.error("Could not partition data: " + str(e))
        return match, miss