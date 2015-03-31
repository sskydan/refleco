import csv
from reflecoSearch.classes.boxClasses.Box import Box

def makeStatsBox(dataFile):
	data = {}
	print(dataFile)
	with open(dataFile, 'r') as csvFile:
		reader = csv.DictReader(csvFile)
		lastCl = None
		lastDay = ('year', 'month', 'day')

		monthData = []
		for row in reader:
			op = float(row['Open'])
			cl = float(row['Close'])

			curDay = tuple(map(int, row['Date'].split('-')))
			tik = 100* (op-cl)/op

			if curDay[1] != lastDay[1]:
			    data[(lastDay[0], lastDay[1])] = monthData
			    monthData = []
			monthData = [tik] + monthData
			lastDay = curDay

	del data[('year', 'month')]
	for month in data:
		N = len(data[month])
		cnt = 0
		for day in data[month]:
		    cnt += day/N
		data[month] = cnt

	cnt = 0
	N = 0
	pr = 0

	stats = [{'cnt':0, 'N':0, 'pr':0} for i in range(12)]
	for month in data:
	    mth = month[1] - 1
	    stats[mth]['cnt'] += data[month]
	    stats[mth]['N'] += 1
	    if (data[month] > 0):
	        stats[mth]['pr'] += 1

	monthLbls = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"]
	statsList = [None for x in range(12)]
	for i in range(12):
		statsList[i] = {
			'month': monthLbls[i],
			'expected': stats[i]['cnt']/stats[i]['N'],
			'probability': stats[i]['pr']/stats[i]['N']
		}

	statBox = Box([], "statisticsBox.html", title="Summary Statistics", stats=statsList)
	return statBox