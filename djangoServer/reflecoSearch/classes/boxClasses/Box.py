from abc import ABCMeta, abstractmethod
import logging
devLogger = logging.getLogger('development')

class Box(object):
    """
    Box is the abstract class for report boxes. It defines the internal
    data representation of a box

    Attributes:
        dataset:
            json data set received from data server in the form of a dictionary

        template:
            string reference to the django template a box uses for display

        **kwargs:
            a series of arguments used by the template
    """
    __metaclass__ = ABCMeta

    def __init__(self, dataSet, template, **kwargs):
        self.dataSet = dataSet
        self.template = template
        self.args = kwargs
        self.addArg('data', dataSet)


    @abstractmethod
    def makeBox(self):
        """
        makeBox is a constructor for a box. It creates
        a Box object with the appropriate args
        """
        pass

    def addArg(self, argName, argVal):
        """
        adds an argument / value to a boxes argument list

        Args:
            argName:
                string name reference for the arg

            argValue:
                string value of arg
        """
        self.args[argName] = argVal
