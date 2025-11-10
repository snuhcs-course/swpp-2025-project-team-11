from celery import Celery
from celery.schedules import crontab

from config.utils import set_environment

set_environment()

app = Celery("config")

app.config_from_object("django.conf:settings", namespace="CELERY")
app.autodiscover_tasks()
app.conf.update()

# Beat 스케줄 정의
app.conf.beat_schedule = {
    "integrating_analysis": {
        "task": "apps.ai.tasks.unified_analysis",
        "schedule": crontab(hour="*/3", minute=0),  # 매 3시간마다 정각에 실행
        "args": [10],
    },
    "cleanup_analysis": {
        "task": "apps.ai.tasks.delete_up_n",
        "schedule": crontab(hour="*/3", minute=30),  # 매 3시간마다 30분에 실행
        "args": [10],
    },
}
