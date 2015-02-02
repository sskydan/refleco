import inspect
import logging
devLogger = logging.getLogger('development')

class QueryGenerator(object):
    """
    QueryGenerator creates data server query strings

    Attributes:
        LIBRARY_HOST:
            root http string for library server
        CORE_HOST:
            root http string for core server
        REFLASK:
            root reflask string for reflask queries
    """

    LIBRARY_HOST = "http://localhost:7800/finbase"
    CORE_HOST = "http://54.148.120.55:8080/coreEngine/engine/"
    REFLASK = "reflask=1&search="

    @classmethod
    def getSearchCriteria(cls, processedTokens):
        #get valid filter tokens
        keyWords = [token for token in processedTokens if inspect.isclass(token[1])]
        #http://localhost:7801/coreengine?reflask=1&search=name (string) [sortby (string attr) lim (num) filter (string attr , [arg]]
        #query string for data server
        query = cls.CORE_HOST + "?" + cls.REFLASK + cls.getDLSQuery(processedTokens)
        #a query object is a tuple of a query for the data server and a list of filters for the filter classes
        queryResults = {'query': query, 'filters': keyWords}

        return queryResults

    @classmethod
    #Creates a reflask DSL string
    def getDLSQuery(cls, searchTokens):
        dslString = ''
        #get NE and reflask filters
        NEs = [token for token in searchTokens if token[1] == 'name']
        filters = [token for token in searchTokens if token[1] == 'filter']

        for NE in NEs:
            dslString += cls.getDSLString(NE)

        for filter in filters:
            dslString += cls.getDSLString(filter)

        return dslString

    @classmethod
    #given a tuple (word, type) create the reflask string
    def getDSLString(cls, token):
        return token[1] + ' ' + '"' + token[0] + '"'
