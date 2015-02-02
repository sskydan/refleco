from reflecoSearch.classes.boxClasses.Box import Box
import logging
devLogger = logging.getLogger('development')

class GraphBox(Box):
    """EXTENDS BOX
    ListBox is used to display data in a list
    """
    #constructor
    @staticmethod
    def makeBox(graphData, graphTitle):
        """
        makeBox is a constructor for GraphBox. It creates
        a Box object with the appropriate args

        Args:
            graphData:
                json data used to build list

            title:
                string title of the list

        Return:
            Box object
        """
        box = GraphBox(graphData, "minGraphBox.html", title=graphTitle)
        return box

