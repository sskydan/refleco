from django import template
register = template.Library()
import logging
devLogger = logging.getLogger('development')

@register.filter(name='getRatioList')
def getRatioList(report):
    return report[1]['children']

@register.filter(name='getRatioChildren')
def getRatioChildren(ratio):
    try:
        children = ratio[u'children']
    except Exception as e:
        devLogger.error("Could not get ratio children: " + e)
        children = []
    return children

@register.filter(name='getRatioName')
def getRatioName(ratio):
    try:
        name = ratio[u'id']
    except Exception as e:
        devLogger.error("there was a problem getting the ratio name: " + e)
        name = ""
    return name

@register.filter(name='getRatioValue')
def getRatioValue(ratio):
    try:
        value = ratio[u'value']
    except Exception as e:
        devLogger.error("there was a problem getting the ratio name " + e)
        value = 0
    return value
