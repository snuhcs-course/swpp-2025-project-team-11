from celery import Celery

from config.utils import set_environment

set_environment()

app = Celery("config")

app.config_from_object("django.conf:settings", namespace="CELERY")
app.autodiscover_tasks()
app.conf.update()
