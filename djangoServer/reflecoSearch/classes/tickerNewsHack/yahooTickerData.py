import urllib.request

PASS = '\033[92m'
FAIL = '\033[91m'
NORM = '\033[0m'

base_url = "http://ichart.yahoo.com/table.csv?s="

def getTickerData(ticker):
    # Not having end date will give everything from start to now
    # Not having neither start and end dates gives everything\
    startMonth = "&a=1"
    startDay = "&b=1"
    startYear = "&c=2006"

    endMonth = "&d=0"
    endDay = "&e=31"
    endYear = "&f=2010"

    tradingInterval = "&g=d"

    staticPart = "&ignore=.csv"

    url = base_url + ticker + tradingInterval + staticPart

    response = urllib.request.urlopen(url)
    data = response.read()

    fileName = "static/tickerData/%s.csv" % (ticker)
    csvFile = open(fileName, 'w')
    csvFile.write(data.decode("utf-8"))
    csvFile.close()

    print("%sWrote to file: %s%s%s" % (PASS, FAIL,fileName, NORM))
    return fileName