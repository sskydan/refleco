from django import template
register = template.Library()
import logging
devLogger = logging.getLogger('development')

@register.filter(name='getTableTitle')
def getTableTitle(args):
    return args['title']

@register.filter(name='getTableHeaders')
def getTableHeaders(args):
    return args['headers']

@register.filter(name='getTableData')
def getTableData(args):
    return args['data']

@register.filter(name='getFactName')
def getFactName(fact):
    try:
        name = fact[u'prettyLabel']
        if not name:
            name = fact[u'id']
    except Exception as e:
        devLogger.error("there was a problem getting a fact name: " + e)
        name = ""
    return name

@register.filter(name='getFactValue')
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
            value = "${:,.2f}".format(value)
    else:
        value = ""
    return value