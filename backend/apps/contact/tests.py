from django.contrib.auth import get_user_model
from django.urls import reverse
from rest_framework.test import APIClient, APITestCase

from apps.contact.models import (
    Contact,
    ContactContext,
    Group,
    GroupOptionMap,
    PromptOption,
    Template,
)

User = get_user_model()


class ContactAppTests(APITestCase):
    def setUp(self):
        # ===== Users (커스텀 모델) =====
        self.user = User.objects.create(email="test@example.com", name="Test User")
        self.other_user = User.objects.create(email="other@example.com", name="Other User")

        # ===== DRF APIClient 인증 =====
        self.client = APIClient()
        self.client.force_authenticate(user=self.user)

        # ===== Groups =====
        self.group = Group.objects.create(name="Group1", user=self.user)
        self.other_group = Group.objects.create(name="OtherGroup", user=self.other_user)

        # ===== Contacts =====
        self.contact = Contact.objects.create(name="Contact1", email="contact1@example.com", group=self.group, user=self.user)
        self.contact_context = ContactContext.objects.create(
            contact=self.contact,
            relationship_role="Friend",
            relationship_details="Details",
            personal_prompt="Hello",
            language_preference="en",
        )

        # ===== Prompt Options =====
        self.prompt_option = PromptOption.objects.create(name="Option1", key="tone", prompt="Prompt text", created_by=self.user)
        self.system_option = PromptOption.objects.create(name="SystemOption", key="format", prompt="System prompt", created_by=None)
        # 새 옵션 추가 (GroupOptionMap 테스트용)
        self.new_option = PromptOption.objects.create(name="Option2", key="tone", prompt="New prompt", created_by=self.user)

        # ===== Group-Option Map =====
        self.map = GroupOptionMap.objects.create(group=self.group, option=self.prompt_option)

        # ===== Template =====
        self.template = Template.objects.create(name="Template1", content="T1", user=self.user)

    # ===== Group Tests =====
    def test_group_list_and_create(self):
        url = reverse("group-list")
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 1)

        response = self.client.post(url, {"name": "NewGroup"})
        self.assertEqual(response.status_code, 201)
        self.assertEqual(Group.objects.filter(user=self.user).count(), 2)

    def test_group_detail_retrieve_update_delete(self):
        url = reverse("group-detail", args=[self.group.id])
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)

        response = self.client.patch(url, {"name": "UpdatedGroup"})
        self.assertEqual(response.status_code, 200)
        self.group.refresh_from_db()
        self.assertEqual(self.group.name, "UpdatedGroup")

        response = self.client.delete(url)
        self.assertEqual(response.status_code, 204)
        self.assertFalse(Group.objects.filter(id=self.group.id).exists())

    # ===== Contact Tests =====
    def test_contact_list_filter_by_group(self):
        url = reverse("contact-list")
        response = self.client.get(url, {"group": self.group.id})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 1)

    def test_contact_create_and_update(self):
        url = reverse("contact-list")
        response = self.client.post(
            url,
            {
                "name": "NewContact",
                "email": "new@example.com",
                "group": self.group.id,
            },  # noqa: E501
        )
        self.assertEqual(response.status_code, 201)
        contact_id = response.data["id"]

        url_detail = reverse("contact-detail", args=[contact_id])
        response = self.client.patch(url_detail, {"name": "UpdatedContact"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(Contact.objects.get(id=contact_id).name, "UpdatedContact")

    # ===== ContactContext Nested =====
    def test_contact_context_by_contact_view(self):
        url = reverse("contact-context-by-contact", args=[self.contact.id])
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["relationship_role"], "Friend")

        response = self.client.patch(url, {"relationship_role": "UpdatedRole"})
        self.assertEqual(response.status_code, 200)
        self.contact_context.refresh_from_db()
        self.assertEqual(self.contact_context.relationship_role, "UpdatedRole")

    # ===== PromptOption Tests =====
    def test_prompt_option_list_and_create(self):
        url = reverse("prompt-option-list")
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        names = [o["name"] for o in response.data]
        self.assertIn(self.prompt_option.name, names)
        self.assertIn(self.system_option.name, names)

        response = self.client.post(url, {"name": "NewOption", "key": "style", "prompt": "New prompt"})  # noqa: E501
        self.assertEqual(response.status_code, 201)
        self.assertEqual(PromptOption.objects.filter(created_by=self.user).count(), 3)  # Option1 + NewOption + new_option

    def test_prompt_option_update_delete_permission(self):
        # system-defined
        url = reverse("prompt-option-detail", args=[self.system_option.id])
        response = self.client.patch(url, {"name": "FailUpdate"})
        self.assertEqual(response.status_code, 403)
        response = self.client.delete(url)
        self.assertEqual(response.status_code, 403)

        # user-defined
        url_user_option = reverse("prompt-option-detail", args=[self.prompt_option.id])
        response = self.client.patch(url_user_option, {"name": "UpdatedOption"})
        self.assertEqual(response.status_code, 200)
        self.prompt_option.refresh_from_db()
        self.assertEqual(self.prompt_option.name, "UpdatedOption")

    # ===== GroupOptionMap Tests =====
    def test_group_option_map_list_create(self):
        url = reverse("group-option-map-list")
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(len(response.data), 1)

        # 중복 아닌 새로운 매핑 생성
        response = self.client.post(url, {"group": self.group.id, "option": self.new_option.id})  # noqa: E501
        self.assertEqual(response.status_code, 201)
        self.assertEqual(GroupOptionMap.objects.filter(group=self.group).count(), 2)

        # 다른 사용자의 그룹 매핑 시도 → 400
        response = self.client.post(url, {"group": self.other_group.id, "option": self.prompt_option.id})
        self.assertEqual(response.status_code, 400)

    def test_group_option_map_update_delete(self):
        url_detail = reverse("group-option-map-detail", args=[self.map.id])
        response = self.client.patch(url_detail, {"option": self.prompt_option.id})
        self.assertEqual(response.status_code, 200)

        response = self.client.delete(url_detail)
        self.assertEqual(response.status_code, 204)
        self.assertFalse(GroupOptionMap.objects.filter(id=self.map.id).exists())

    # ===== Template Tests =====
    def test_template_crud(self):
        url = reverse("template-list")
        response = self.client.get(url)
        self.assertEqual(response.status_code, 200)

        response = self.client.post(url, {"name": "NewTemplate", "content": "C"})
        self.assertEqual(response.status_code, 201)
        template_id = response.data["id"]

        url_detail = reverse("template-detail", args=[template_id])
        response = self.client.patch(url_detail, {"content": "UpdatedC"})
        self.assertEqual(response.status_code, 200)
        self.assertEqual(Template.objects.get(id=template_id).content, "UpdatedC")

        response = self.client.delete(url_detail)
        self.assertEqual(response.status_code, 204)
        self.assertFalse(Template.objects.filter(id=template_id).exists())
