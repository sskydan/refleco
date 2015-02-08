from abc import ABCMeta, abstractmethod
import logging
devLogger = logging.getLogger('development')

class Box(object):

    __metaclass__ = ABCMeta

    def __init__(self, dataSet, template, **kwargs):
        """Abstract class for report boxes.
        :param dataset: json data set received from data server in the form of a dictionary
        :param template: string reference to the django template a box uses for display
        :param **kwargs: a series of arguments used by the template
        """
        self.dataSet = dataSet
        self.template = template
        self.args = kwargs
        self.addArg('data', dataSet)


    @abstractmethod
    def makeBox(self):
        """MakeBox is a constructor for a box.
        """
        pass

    def addArg(self, argName, argVal):
        """Adds an argument / value to a boxes argument list
        :param argName: String argument name
        :param argVal: String argument value
        """
        self.args[argName] = argVal
