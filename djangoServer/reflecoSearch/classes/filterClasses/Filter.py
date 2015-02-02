from abc import ABCMeta, abstractmethod
import logging
devLogger = logging.getLogger('development')

class Filter(object):
    """
    Filter is the abstract class for filtering out specific values from
    data sets retrieved from the data server in order to create Box
    objects.

    Attributes:
        keys:
            A key mapping from keywords to data keys.

        dataSet:
            json data
    """
    __metaclass__ = ABCMeta

    def __init__(self):
        self.dataSet = "{}"

    @property
    def keys(self):
        """
        dict mapping for different lists of keys to filter by
        """
        pass

    @abstractmethod
    def createBoxList(self, *reportArgs):
        """
        Generates the list of appropriate box objects

        Args:
            filterArg:
                Optional filter for reports
        """
        pass

    def loadData(self, data):
        self.dataSet = data

    def filterData(self, dataSet, filterFunc, *filterArgs):
        """
        filters a data set for fields with given values

        Args:
            key:
                string field to filter by (i.e 'ID')

            values:
                list of acceptable string values for the given key

            dataSet:
                json data set to filter

        Return:
            returns a filtered json data set
        """
        return list(filter(filterFunc, dataSet))

