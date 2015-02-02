import requests
from reflecoSearch.classes.filterClasses.StatementFilter import StatementFilter
from reflecoSearch.classes.boxClasses.ListBox import ListBox
from reflecoSearch.classes.boxClasses.GraphBox import GraphBox
import logging
devLogger = logging.getLogger('development')


LOCAL_CORE_ENGINE_REFLASK = "http://localhost:7801/engine/reflask?search="
CORE_ENGINE_REFLASK = "http://54.148.120.55:8080/coreEngine/engine/reflask?search="

class ReportBuilder(object):
    """
    ReortBuilder takes in a queryObject tuple (dsl query string, list of Filter object)
    and creates a report consisting of a list of Box objects

    Attributes:
        defaultFilters:
            list of default filters to use if no filters are given.
            this is the default report.
    """



    defaultFilters = [
        ('cash', StatementFilter),
        ('balance', StatementFilter),
        ('income', StatementFilter),
    ]


    @classmethod
    def dataRequest(cls, dslString):
        """
        Gets json data from data server
        :param dslString: A dsl formated string
        :return: json data
        """

        data = '{}'
        if len(dslString) > 0:
            try:
                reply = requests.get(LOCAL_CORE_ENGINE_REFLASK + dslString)
                if reply.status_code == 200:
                    r = reply.json()
                    # Top level fact only describes the xbrl-report.
                    # NOTE "children" is an array
                    if len(r):
                        data = r
                    else:
                        devLogger.warn("No data received for query: " + dslString)

            except Exception as e:
                devLogger.error("There was a communicating with the data server" + str(e))
        else:
            devLogger.info("Empty DSL string was given")
        return data


    @classmethod
    def buildReport(cls, dslString, filterList):
        """
        buildReport creates a filter object and generates
        a box list.

        Args:
            data:
                json data from a data query

            reportList:
                 list of report classes to build off the data
            modifiers:
                additional report modifiers

        Return:
            list of Box objects
        """
        boxList = []
        data = cls.dataRequest(dslString)
        if data != "{}":
            for filter in filterList:
                filterObj = filter()
                filterObj.loadData(data)
                boxList.extend(filterObj.createBoxList())
        return boxList
