from django.db import models


# Create your models here.
class User(models.Model):  # 모델 명 User로 변경
    # id 필드: 기본으로 생성됨
    email = models.EmailField(max_length=255, unique=True)  # EmailField로 변경, unique 속성 설정
    name = models.CharField(max_length=30)
    refresh_token = models.CharField(max_length=512, null=True, blank=True)

    def __str__(self):
        return f"{self.name} <{self.email}>"
