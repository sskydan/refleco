from django.db import models

class Article(models.Model):
    date = models.IntegerField()
    pageRank = models.IntegerField()
    link = models.TextField()

    def __str__(self):
        return self.link

    class Meta:
        db_tablespace = "article"