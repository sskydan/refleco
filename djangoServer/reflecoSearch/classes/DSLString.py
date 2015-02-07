
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

    def addSubject(self, DSLEntity):
        """adds a subject (company or entity) to a DSL query
        :param DSLEntity: A chunk of a dsl string
        :return:
        """
        s = list(map(lambda e: e.replace('{', '(').replace('}', ')'), DSLEntity))
        self.subject = '"' + ' '.join(s[1:]) + '"'
        self.string = s[0].lower() + " "

    def addDSLE(self, DSLEntity):
        """adds a dsl chunk to a dls string
        :param DSLEntity: A chunk of a dsl string
        :return:
        """
        def getFilter(d):
            s = list(map(lambda e: e.replace('{', '(').replace('}', ')'), d))
            def filterSwitch(x):
                return {
                    'attribute': '@',
                    'relation': '.',
                }.get(x, False)

            m = filterSwitch(s[1])
            if m:
                return m + '"' + ' '.join(s[2:]) + '"'
            return ""

        def getModifier(d):
            def modifierSwitch(x):
                return {
                    'GREATERTHAN': '>',
                    'LESSTHAN': '<',
                    'EQUAL': '<>',
                    'GTEQUAL': '>>',
                    'LTEQUAL': '<<',
                }.get(x, False)

            m = modifierSwitch(d[1])
            if m:
                cleanNum = d[2].replace(",", "").split(".", 1)[0]
                return m + cleanNum
            return ""

        m = ""
        f = ""
        for d in DSLEntity:
            if d[0] == 'company' or d[0] == 'entity':
                self.addSubject(d)
            elif d[0] == 'FILTER':
                f = getFilter(d)
            elif d[0] == 'MODIFIER':
                m = m + getModifier(d)


        if len(m):
            self.filters.append(f + m)
            if not len(self.filterObjects):
                self.filters.append(f)
        elif len(f) and not len(self.filterObjects):
            self.filters.append(f)