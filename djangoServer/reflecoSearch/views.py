from django.http import HttpResponseRedirect, HttpResponse
from django.template import RequestContext
from django.shortcuts import render_to_response
from django.http import QueryDict
from django.core.mail import send_mail
from reflecoSearch.classes.ReportBuilder import ReportBuilder
from reflecoSearch.classes.QueryParser import QueryParser
from reflecoSearch.classes.QueryParsing.QueryTagger import QueryTagger
from reflecoSearch.models import Article
from django.core import serializers
import json
import logging
devLogger = logging.getLogger('development')
queryLogger = logging.getLogger("query")

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
    boxes = []
    if len(query) < 6 and query.isupper():
        return HttpResponseRedirect('/sandbox/'+query)

    if query:
        #logging queries
        queryLogger.info(QueryTagger.getClientIp(request) + " - " + query)

        if "dsl::" == query[:5]:
            dslQuery = query[5:]
            devLogger.info('Received direct DSL query: ' + dslQuery)
        else:
            devLogger.info("Received query: " + query)
            queryList = list()
            try:
                qt = QueryTagger(query)
                queryOptions = qt.splitOnNer()
                for opt in queryOptions:
                    queryParse = QueryParser([c.toTuple() for c in opt])
                    dsl, filter = queryParse.parseAST()
                    newQuery = (dsl, filter)
                    queryList.append(newQuery)
            except Exception as e:
                devLogger.error("could not parse query: " + str(e))
        try:
            boxes = ReportBuilder.buildReport(queryList)
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

def sandbox(request, query=""):
    if not query.isupper():
        return HttpResponseRedirect('/results/'+query)

    from reflecoSearch.classes.tickerNewsHack.yahooTickerData import getTickerData
    from reflecoSearch.classes.boxClasses.GraphBox import GraphBox
    from reflecoSearch.classes.boxClasses.StatisticBox import makeStatsBox 

    fileName = getTickerData(query)
    boxes = [GraphBox.makeBox(fileName, "Test"), makeStatsBox(fileName)]
    return render_to_response("resultBoxes.html", {'query': query, 'results': boxes},  context_instance=RequestContext(request))

def getArticlesByDate(request):
    articles = []
    if request.method == 'POST':
        try:
            queryData = request.POST
            startDate = queryData.get('startDate', 9999999999999)
            endDate = queryData.get('endDate', 9999999999999)
            #distArticles = Article.objects.values_list('link',flat = True).distinct()[:10]
            #rankedArticles = Article.objects.order_by('-pageRank')
            articles = Article.objects.filter(date__range=[startDate, endDate]).filter(link__startswith="http" ).order_by('-pageRank').values_list('link',flat = True).distinct()

        except:
            print("failed article lookup")
        data = {}
        data['data'] = list(articles)
        response = json.dumps(data)
    return HttpResponse(response)