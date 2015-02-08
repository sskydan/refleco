from reflecoSearch.classes.boxClasses.Box import Box
import logging
devLogger = logging.getLogger('development')

class TableBox(Box):

    @staticmethod
    def makeBox(tableData, tableTitle, tableHeaders):
        """Constructor for TableBox. It creates a Box object with the appropriate args
        :param tableData: Json data for table box
        :param tableTitle: String title for box
        :param tableHeaders: List(String) table headers
        :return: TableBox()
        """
        box = TableBox(tableData, "tableBox.html", title=tableTitle, headers=tableHeaders)
        return box
