from reflecoSearch.classes.boxClasses.Box import Box
import logging
devLogger = logging.getLogger('development')

class ListBox(Box):
    """EXTENDS BOX
    ListBox is used to display data in a list
    """
    #constructor
    @staticmethod
    def makeBox(listData, ListTitle):
        """
        makeBox is a constructor for ListBox. It creates
        a Box object with the appropriate args

        Args:
            listData:
                json data used to build list

            title:
                string title of the list

        Return:
            Box object
        """
        box = ListBox(listData, "listBox.html", title=ListTitle)
        return box

