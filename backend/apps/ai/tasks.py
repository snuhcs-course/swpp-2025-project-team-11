from datetime import timedelta

from celery import shared_task
from django.utils import timezone

from apps.mail.models import AttachmentAnalysis


@shared_task
def task_example():
    pass


@shared_task
def purge_old_attachment_analysis():
    cutoff = timezone.now() - timedelta(days=1)
    AttachmentAnalysis.objects.filter(created_at__lt=cutoff).delete()
