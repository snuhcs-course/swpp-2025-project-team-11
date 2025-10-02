from django.contrib import admin

from .models import Email


@admin.register(Email)
class EmailAdmin(admin.ModelAdmin):
    list_display = ["subject", "sender", "recipient", "received_at"]
    list_filter = ["received_at"]
    search_fields = ["subject", "sender", "recipient", "body"]
    readonly_fields = ["gmail_id", "thread_id", "created_at", "updated_at"]
    date_hierarchy = "received_at"
