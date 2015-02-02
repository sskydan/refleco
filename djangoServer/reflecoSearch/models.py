from django.db import models


class dataMessage(models.Model):
    name = models.CharField(max_length=50)
    content = models.CharField(max_length=200)