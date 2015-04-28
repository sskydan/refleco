import datetime
import json


def getTotalsByDates(fact):
    dateBuckets = list()
    totals = list()
    values = fact.get(u'value', [])
    values = values.get(u'valList', [])
    for val in values:
        startDate = datetime.datetime.strptime(val.get(u'startDate')[:10], "%Y-%m-%d")
        endDate = datetime.datetime.strptime(val.get(u'endDate')[:10], "%Y-%m-%d")
        value = val.get(u'inner', "").get(u'valDouble', False)
        if value:
            inBucket = False
            for db in dateBuckets:
                if (db.dateInRange(startDate) and db.dateInRange(endDate)):
                    inBucket = True
                    db.values.append(value)
                if db.rangeIncludesBucket(startDate, endDate):
                    inBucket = True
                    db.startDate = startDate
                    db.endDate = endDate
                    db.values.append(value)
            if not inBucket:
                dateBuckets.append(DateBucket(startDate, endDate, value))
    for db in dateBuckets:
        fact[u'value'][u'valList'].append(db.toJson())
    return fact


class DateBucket(object):

    def __init__(self, start, end, val):
        self.startDate = start
        self.endDate = end
        self.values = [val]

    def dateInRange(self, date):
        if self.startDate <= date <= self.endDate:
            return True
        else:
            return False

    def rangeIncludesBucket(self, start, end):
        if start < self.startDate and end > self.endDate:
            return True
        return False

    def getTotalVal(self):
        if len(self.values) == 1:
            return self.values[0]
        elif len(self.values) < 1:
            return 0
        else:
            sumOfVals = sum(self.values)
            for v in self.values:
                if v == (sumOfVals / 2):
                    return v

        return max(map(abs, self.values))

    def toJson(self):
        data = {}
        data[u'startDate'] = self.startDate.strftime("%Y-%m-%dT%H:%M:%S.%f%z")
        data[u'endDate'] = self.endDate.strftime("%Y-%m-%dT%H:%M:%S.%f%z")
        data[u'facttype'] = "period:total"
        data[u'inner'] = {}
        data[u'inner'][u'valDouble'] = self.getTotalVal()
        data[u'inner'][u'currency'] = "usd"
        data[u'inner'][u'facttype'] = "monetary"
        return data

