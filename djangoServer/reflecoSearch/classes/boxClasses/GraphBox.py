from reflecoSearch.classes.boxClasses.Box import Box
import logging
devLogger = logging.getLogger('development')

class GraphBox(Box):


    @staticmethod
    def makeBox(graphData, graphTitle):
        """Constructor for GraphBox. It creates a Box object with the appropriate args
        :param graphData: Json data for graph box
        :param graphTitle: String title for box
        :return: GraphBox()
        """
        box = GraphBox(graphData, "minGraphBox.html", title=graphTitle)
        return box

