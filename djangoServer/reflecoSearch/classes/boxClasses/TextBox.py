from reflecoSearch.classes.boxClasses.Box import Box
import logging
devLogger = logging.getLogger('development')

class TextBox(Box):
    """EXTENDS BOX
    TextBox is used to display text data
    """
    #constructor
    @staticmethod
    def makeBox(textData, textTitle):
        """
        makeTableBox is a constructor for TextBox. It creates
        a Box object with the appropriate args

        Attributes:
            TextData:
                json data used to build table entries

            tableTitle:
                string title of the table

            tableHeaders:
                list of string headers for the table columns

        Return:
            Box object
        """
        box = TextBox(textData, "textBox.html", title=textTitle)
        return box
