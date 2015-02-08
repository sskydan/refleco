from django.http import HttpResponseRedirect
from django.template import RequestContext
from django.shortcuts import render_to_response
from django.http import QueryDict
from django.core.mail import send_mail
from reflecoSearch.classes.ReportBuilder import ReportBuilder
from reflecoSearch.classes.QueryTagger import QueryTagger
from reflecoSearch.classes.QueryParser import QueryParser
import logging
devLogger = logging.getLogger('development')

def landingPage(request):
    args = {}
    return render_to_response('construction.html', args, context_instance=RequestContext(request))

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

    query = queryData.get('query')
    return HttpResponseRedirect('/results/'+query)

def results(request, query=""):
    if query:
        if "dsl::" == query[:5]:
            dslQuery = query[5:]
            devLogger.info('Received direct DSL query: ' + dslQuery)
        else:
            devLogger.info("Received query: " + query)
            try:
                taggedTokens = QueryTagger.tagQuery(query)
                queryParse = QueryParser(taggedTokens)
                dslList, filterObjects = queryParse.parseAST()
                #TODO - right now we only consider the first dsl query parsed.
                dslQuery = dslList[0]
            except Exception as e:
                dslQuery = ""
                filterObjects = []
                devLogger.error("could not parse query: " + str(e))
        try:
            boxes = ReportBuilder.buildReport(dslQuery, filterObjects)
        except Exception:
            boxes = []
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
