from django import template
register = template.Library()
import logging
devLogger = logging.getLogger('development')

@register.filter(name='getTemplate')
def getTemplate(reportBox):
	return "boxTemplates/"+reportBox.template

@register.filter(name='getArgs')
def getArgs(reportBox):
    return reportBox.args

@register.filter(name='resultsAreEmpty')
def resultsAreEmpty(results):
    if len(results) == 0:
        return True
    return False

@register.filter(name='emptyQuery')
def resultsAreEmpty(results):
    if results == "empty":
        return True
    return False