from django.db import models


# Create your models here.
class User(models.Model):  # 모델 명 User로 변경
    # id 필드: 기본으로 생성됨
    email = models.EmailField(max_length=255, unique=True)  # EmailField로 변경, unique 속성 설정
    name = models.CharField(max_length=30)

    def __str__(self):
        return f"{self.name} <{self.email}>"


class GoogleAccount(models.Model):  # 구글 API 접근을 위한 모델
    user = models.ForeignKey(
        User, on_delete=models.CASCADE, related_name="google_accounts", unique=True
    )  # 실제 필드명은 자동으로 user_id가 됨
    access_token = models.TextField()
    refresh_token = models.TextField()
    expires_at = models.DateTimeField()  # 토큰 만료 시간

    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f"GoogleAccount for {self.user.email}"
