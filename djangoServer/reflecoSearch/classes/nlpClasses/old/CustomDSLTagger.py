#CustomDSLTagger is a post NLTK-tagging stop word and organization NE tagger

import logging

import requests

devLogger = logging.getLogger('development')

class CustomDSLTagger(object):

    @classmethod
    #adds NE and stopword tags to a list of tuples(result of NLTK tagging)
    def customDSLTagging(cls, taggedTokens):
        #orgNE is a triple (string name, tuple start and end indices of name in token list, dict backend result)
        orgNE = cls.identifyNE(taggedTokens)
        #tag identifiable organization NE
        if orgNE:
            taggedTokens[orgNE[1][0]:orgNE[1][1]+1] = [(orgNE[2][u'prettyLabel'], 'name')]

        return taggedTokens

    #temp NER: given a list of query tokens, find closest matching organization named entity
    #returns triple: (('search string', (start token index, end token index)), backend query result)
    @classmethod
    def identifyNE(cls, tokens):
        #seperate words from tags
        tokenWords = [token[0] for token in tokens]
        permutations = []
        #for each contiguous permutation of the query string
        for i in range(0, len(tokens)):
            for j in range(i, len(tokens)):
                orgString = ' '.join(tokenWords[i:j+1])
                try:
                    #query backend for organizations
                    companyResults = requests.get('http://localhost:7800/finbase?search=' + orgString + '&field=prettyLabel')
                    if companyResults.status_code == 200:
                        r = companyResults.json()
                        if len(r):
                            dataSet = r
                            #select closest match from results based on interest
                            bestMatch = max(dataSet, key=lambda item: item[u'interest'])
                            permutations.append((orgString, (i,j), bestMatch))
                except Exception as e:
                    devLogger.error("Error querying for org NE: " + str(e))

        #return the permutation with the highest match rating
        if not len(permutations):
            return False
        return max(permutations, key=lambda p: p[2][u'interest'])


