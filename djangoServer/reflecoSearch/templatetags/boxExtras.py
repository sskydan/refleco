from django import template
register = template.Library()
import logging
devLogger = logging.getLogger('development')


@register.filter(name='getBoxArg')
def getBoxArg(args, arg):
    return args.get(arg, False)

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

@register.filter(name='isPeriodicFact')
def isPeriodicFact(fact):
    if fact[u'ftype'] == "xbrl":
        value = fact.get(u'value', [])
        if u'valList' in value:
            return True
    return False

@register.filter(name='getPeriodicValues')
def getPeriodicValues(fact):
    value = fact.get(u'value', [])
    return value.get(u'valList', [])

@register.filter(name='getPeriodValue')
def getPeriodValue(period):
    try:
        return "${:,.2f}".format(period[u'inner'][u'valDouble'])
    except Exception as e:
        devLogger.error("Could not get period value: " + str(e))
        return ""

@register.filter(name='getPeriodDates')
def getPeriodDates(period):
    startDate = ""
    endDate = ""
    if u'startDate' in period:
        startDate = period[u'startDate'][:9].replace('-', '/')
    if u'endDate' in period:
        endDate = period[u'endDate'][:9].replace('-', '/')
    return startDate + " - " + endDate

@register.filter(name='getFactValue')
def getFactValue(fact):
    try:
        value = fact[u'value']
        if fact[u'ftype'] == "analytic":
            value = "{:,.4f}".format(value)
        if fact[u'ftype'] == "xbrl":
            # Monetary value?
            if u'valDouble' in value:
                value = value[u'valDouble']
            elif u'inner' in value:
                value = value[u'inner'][u'valDouble']
                value = "${:,.2f}".format(value)
            else:
                value = ""
        else:
            value = ""
    except Exception as e:
        devLogger.error("there was a problem getting a fact value " + e)
        value = ""
    return value

@register.filter(name='getFactChildren')
def getFactChildren(fact):
    return fact['children']