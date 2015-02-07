from reflecoSearch.classes.boxClasses.Box import Box
import logging
devLogger = logging.getLogger('development')

class TextBox(Box):

    @staticmethod
    def makeBox(textData, textTitle):
        """Constructor for TextBox. It creates a Box object with the appropriate args
        :param textData: Json data for text box
        :param textTitle: String title for box
        :return: TextBox()
        """
        box = TextBox(textData, "textBox.html", title=textTitle)
        return box
