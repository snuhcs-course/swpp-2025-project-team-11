from django.db import models


# Create your models here.
class Users(models.Model):
    # id 필드: 기본으로 생성됨
    email = models.CharField(max_length=100)
    name = models.CharField(max_length=30)

    def __str__(self):
        return f"{self.name} <{self.email}>"
