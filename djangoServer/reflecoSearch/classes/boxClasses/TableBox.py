from reflecoSearch.classes.boxClasses.Box import Box
import logging
devLogger = logging.getLogger('development')

class TableBox(Box):
    """EXTENDS BOX
    TableBox is used to display data in a Table
    """
    #constructor
    @staticmethod
    def makeBox(tableData, tableTitle, tableHeaders):
        """
        makeTableBox is a constructor for TableBox. It creates
        a Box object with the appropriate args

        Attributes:
            TableData:
                json data used to build table entries

            tableTitle:
                string title of the table

            tableHeaders:
                list of string headers for the table columns

        Return:
            Box object
        """
        box = TableBox(tableData, "tableBox.html", title=tableTitle, headers=tableHeaders)
        return box
