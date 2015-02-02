from django import template
register = template.Library()
import logging
devLogger = logging.getLogger('development')

@register.filter(name='getCompanyName')
def getCompanyName(item):
    return item[u'prettyLabel']

@register.filter(name='companyInIndustry')
def companyInIndustry(item, args):
    try:
        if item[u'children'][0][u'prettyLabel'] == args['industry']:
            return True
    except Exception as e:
        devLogger.error("Could not check industry: " + e)
    return False
