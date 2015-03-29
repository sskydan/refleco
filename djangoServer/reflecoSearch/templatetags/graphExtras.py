from django import template
import json
register = template.Library()
import logging
devLogger = logging.getLogger('development')

@register.filter(name='getTitle')
def getTableTitle(args):
    return args['title']

@register.filter(name='getData')
def getTableTitle(args):
    return args['data']

@register.filter(name='getNames')
def getTableNames(args):
    return args.get('names', [])


@register.filter(name='getSeries')
def getSeries(args):
    series = {}
    for fact in args['data']:
        for child in fact['children']:
            s = series.get(child['prettyLabel'], [])
            s.append(getFactValue(child))
            series[child['prettyLabel']] = s
    hcSeries = []
    for name, data in series.iteritems():
        hcSeries.append({'name':name, 'data': data})
    return json.dumps(hcSeries)

@register.filter(name='getXLabels')
def getXLabels(args):
    xLabels = []
    for fact in args['data']:
        if not fact['value'] in xLabels:
            xLabels.append(getFactValue(fact))
    return json.dumps(xLabels)


@register.filter(name='getListItemChildren')
def getListItemChildren(fact):
    return fact['children']

@register.filter(name='getItemName')
def getFactName(item):
    try:
        name = item[u'prettyLabel']
        if not name:
            name = item[u'id']
    except Exception as e:
        devLogger.error("there was a problem getting a fact name: " + e)
        name = ""
    return name

@register.filter(name='getItemValue')
def getFactValue(fact):
    try:
        value = fact[u'value']
    except Exception as e:
        devLogger.error("there was a problem getting a fact value " + e)
        value = 0
    if value is not None:
        if fact[u'ftype'] == "analytic":
            value = value
            value = "{:,.4f}".format(value)
        if fact[u'ftype'] == "xbrl":
            # Monetary value?
            if u'valDouble' in value:
                value = value[u'valDouble']
            # Periodic value?
            elif u'valList' in value:
                arr = value[u'valList']
                value = arr[0][u'inner'][u'valDouble']
             #not sure whats up with the values...but so many random formats
            elif u'inner' in value:
                value = value[u'inner'][u'valDouble']
            #value = "${:,.2f}".format(value)
    else:
        value = ""
    return value

@register.filter(name='getStockSeries')
def getStockSeries(args):
    series = args.get('data', [])
    return series