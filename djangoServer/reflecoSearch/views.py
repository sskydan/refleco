import logging

from django.http import HttpResponseRedirect
from django.template import RequestContext
from django.shortcuts import render_to_response
from django.http import QueryDict

from reflecoSearch.classes.ReportBuilder import ReportBuilder

from reflecoSearch.classes.nlpClasses.QueryTagger import QueryTagger
from reflecoSearch.classes.nlpClasses.DSLParser import DSLParser

from django.core.mail import send_mail

from reflecoSearch.classes.filterClasses.DefaultDataFilter import DefaultDataFilter



devLogger = logging.getLogger('development')

def landingPage(request):
    args = {}
    return render_to_response('landingPage.html', args, context_instance=RequestContext(request))

def search(request):
    if request.method == 'POST':
        try:
            queryData = request.POST
        except Exception as e:
            queryData = QueryDict('')
            devLogger.warn('There was a problem handling the search POST data: ' + str(e))
    else:
        #this should never happen
        devLogger.critical("this should never happen: query data empty (not a post)")
        queryData = QueryDict('')
        return 0

    query = queryData.get('query')
    return HttpResponseRedirect('/results/'+query)

def results(request, query=""):
    #http://localhost:7800/finbase?search=mattress car&field=prettyLabel
    #http://localhost:7801/coreengine?reflask=1&search=name (string) [sortby (string attr) lim (num) filter (string attr , [arg]]
    if query:
        devLogger.info("views.results - query: " + query)
        boxes = []
        dslQuery = ""
        filterObjects = []
        if "dsl::" == query[:5]:
            dslQuery = query[5:]
            devLogger.info('Direct DSL give: ' + dslQuery)
        else:
            try:
                taggedTokens = QueryTagger.tagQuery(query)
                dslParse = DSLParser(taggedTokens)
                dslList, filterObjects = dslParse.parseAST()

                dslQuery = dslList[0]
                if 'company' not in dslQuery and 'entity' not in dslQuery:
                    dslQuery = "company *" + dslQuery

                devLogger.info('Parse query successfull.')
                devLogger.info('DSL query is: ' + str(dslQuery))
                devLogger.info('Filter objects are : ' + str(filterObjects))
            except Exception as e:
                devLogger.error("could not parse query: " + str(e))

        try:
            if len(filterObjects) < 1:
                filterObjects = [DefaultDataFilter]
            boxes = ReportBuilder.buildReport(dslQuery, filterObjects)
            devLogger.info('Report successfull.')
        except Exception as e:
            devLogger.error("could not build report: " + str(e))


    else:
        boxes = "empty"
        devLogger.warn("No query received")

    return render_to_response("resultBoxes.html", {'query': query, 'results': boxes},  context_instance=RequestContext(request))

def signup(request):
    if request.method == 'POST':
        try:
            queryData = request.POST
        except Exception as e:
            queryData = QueryDict('')
            devLogger.error('There was a problem handling the email POST data: ' + str(e))
    else:
        devLogger.error("SubmitEmail endpoint only supports POST method")
        return 0

    email = queryData.get('email')
    send_mail('refleco.com sign-up request', email, email, ['info@reflecho.com'], fail_silently=False)
    return HttpResponseRedirect('/')
