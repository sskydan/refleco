
class DSLString(object):

    def __init__(self, filterObjects):
        """a dslString manages and combines DSL chunks into a query string
        :param filterObjects: List(Filter) to specify view filters
        """
        self.string = "company "
        self.subject = "*"
        self.filters = []
        self.filterObjects = filterObjects

    def getString(self):
        """builds the dsl string from the available chunks
        :return: dsl String
        """
        if len(self.filters):
            return self.string + " ".join(list(map(lambda e: self.subject + e, self.filters)))
        else:
            return self.string + self.subject

    def addSubject(self, DSLItem):
        """adds a subject (company or entity) to a DSL query
        :param DSLItem: A chunk of a dsl string
        :return:
        """
        s = list(map(lambda e: e.replace('{', '(').replace('}', ')'), DSLItem))
        self.subject = '"' + ' '.join(s[1:]) + '"'
        self.string = s[0].lower() + " "

    def addDSLI(self, DSLItem):
        """adds a dsl chunk to a dls string
        :param DSLItem: A chunk of a dsl string
        :return:
        """
        def getFilter(DSLI):
            """converts a DSL filter from List() to String
            :param DSLI: List() DSL item
            :return: String formatted for DSL
            """
            def filterSwitch(x):
                """switch on different filter types
                :param x: switch input String
                :return: switch result String
                """
                return {
                    'attribute': '@',
                    'relation': '.',
                }.get(x, False)

            itemString = list(map(lambda e: e.replace('{', '(').replace('}', ')'), DSLI))
            itemFilter = filterSwitch(itemString[1])
            if itemFilter:
                return itemFilter + '"' + ' '.join(itemString[2:]) + '"'
            return ""

        def getModifier(DSLI):
            """converts a DSL modifier from List() to String
            :param DSLI: List() DSL item
            :return: String formatted for DSL
            """
            def modifierSwitch(x):
                """switch on different modifier types
                :param x: switch input String
                :return: switch result String
                """
                return {
                    'GREATERTHAN': '>',
                    'LESSTHAN': '<',
                    'EQUAL': '<>',
                    'GTEQUAL': '>>',
                    'LTEQUAL': '<<',
                }.get(x, False)

            modString = modifierSwitch(DSLI[1])
            if modString:
                cleanNum = DSLI[2].replace(",", "").split(".", 1)[0]
                return modString + cleanNum
            return ""

        modString = ""
        filterString = ""
        for item in DSLItem:
            if item[0] == 'company' or item[0] == 'entity':
                self.addSubject(item)
            elif item[0] == 'FILTER':
                filterString = getFilter(item)
            elif item[0] == 'MODIFIER':
                modString = modString + getModifier(item)


        if len(modString):
            self.filters.append(filterString + modString)
            if not len(self.filterObjects):
                self.filters.append(filterString)
        elif len(filterString) and not len(self.filterObjects):
            self.filters.append(filterString)

    def pprint(self):
        print("string: " + self.string)
        print("subject: " + self.subject)
        print("filters: " + self.filters)
        print("filterObjects: " + self.filterObjects)