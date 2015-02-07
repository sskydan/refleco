import requests
from django.conf import settings
import logging
devLogger = logging.getLogger('development')

class ReportBuilder(object):
    """
    Builds a report of Box() objects to display as the result
    of a query
    """

    @classmethod
    def buildReport(cls, dslString, filterList):
        """Generates a list of boxes
        :param dslString: DSL string for data query
        :param filterList: List(Filter()) to set data views
        :return List(Box()) resulting display boxes
        """
        boxList = []
        data = cls.__dataRequest(dslString)
        if data != "{}":
            for filter in filterList:
                try:
                    if filter:
                        filterObj = filter()
                        filterObj.loadData(data)
                        boxList.extend(filterObj.createBoxList())
                except Exception as e:
                    devLogger.error("Could not create Filter object: " + str(e))
        return boxList

    @classmethod
    def __dataRequest(cls, dslString):
        """Gets json data from data server
        :param dslString: DSL string for data query
        :return: json data
        """
        if len(dslString) > 0:
            try:
                reply = requests.get(settings.CORE_HOST + "reflask?search=" + dslString)
                if reply.status_code == 200:
                    r = reply.json()
                    if len(r):
                        data = r
                    else:
                        data = '{}'
                        devLogger.warn("No data received for query: " + dslString)
            except Exception as e:
                devLogger.error("There was a problem communicating with the data server" + str(e))
        else:
            data = '{}'
            devLogger.info("Empty DSL string was given")
        return data

