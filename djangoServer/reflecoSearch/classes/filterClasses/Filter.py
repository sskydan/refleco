from abc import ABCMeta, abstractmethod
import logging
devLogger = logging.getLogger('development')

class Filter(object):
    """Abstract class for filtering fact data
    """
    __metaclass__ = ABCMeta

    def __init__(self):
        self.dataSet = "{}"

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

    def filterData(self, dataSet, filterFunc, *filterArgs):
        """filters a data set given a predicate
        :param dataSet: Json data set
        :param filterFunc: Boolean predicate function
        :param *filterArgs: optional filter arguments
        :return: a filtered json data set
        """
        try:
            filtered = list(filter(filterFunc, dataSet))
        except Exception as e:
            filtered = list()
            devLogger.error("Could not filter data: " + str(e))
        return filtered


