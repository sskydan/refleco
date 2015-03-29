from reflecoSearch.classes.boxClasses.Box import Box
import datetime
import logging
devLogger = logging.getLogger('development')

class GraphBox(Box):


    @staticmethod
    def makeBox(dataFile, graphTitle):
        """Constructor for GraphBox. It creates a Box object with the appropriate args
        :param graphData: Json data for graph box
        :param graphTitle: String title for box
        :return: GraphBox()
        """
        import csv
        with open(dataFile, 'rt') as f:
            reader = csv.reader(f)
            s1 = []
            s2 = []
            s3 = []
            s4 = []
            s5 = []
            s6 = []
            names = next(reader)
            for row in reader:
                s1.append([int(datetime.datetime.strptime(row[0], "%Y-%m-%d").strftime('%s')) * 1000, float(row[1])])
                s2.append([int(datetime.datetime.strptime(row[0], "%Y-%m-%d").strftime('%s')) * 1000, float(row[2])])
                s3.append([int(datetime.datetime.strptime(row[0], "%Y-%m-%d").strftime('%s')) * 1000, float(row[3])])
                s4.append([int(datetime.datetime.strptime(row[0], "%Y-%m-%d").strftime('%s')) * 1000, float(row[4])])
                s5.append([int(datetime.datetime.strptime(row[0], "%Y-%m-%d").strftime('%s')) * 1000, float(row[5])])
                s6.append([int(datetime.datetime.strptime(row[0], "%Y-%m-%d").strftime('%s')) * 1000, float(row[6])])

        graphData = [s1[::-1], s2[::-1], s3[::-1], s4[::-1], s5[::-1], s6[::-1]]
        box = GraphBox(graphData, "graphBox.html", title=graphTitle, names=names[1:])
        return box

