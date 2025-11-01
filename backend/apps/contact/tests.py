from types import SimpleNamespace
from unittest.mock import MagicMock

from django.http import Http404
from django.test import TestCase
from rest_framework import status
from rest_framework.exceptions import PermissionDenied
from rest_framework.test import APIRequestFactory, force_authenticate

from apps.contact.models import (
    Contact,
    ContactContext,
    Group,
    PromptOption,
    Template,
)
from apps.contact.serializers import (
    ContactSerializer,
    GroupSerializer,
    PromptOptionSerializer,
    TemplateSerializer,
)
from apps.contact.views import (
    ContactContextByContactView,
    ContactContextDetailView,
    ContactDetailView,
    ContactListCreateView,
    GroupDetailView,
    GroupListCreateView,
    PromptOptionDetailView,
    PromptOptionListCreateView,
    TemplateDetailView,
    TemplateListCreateView,
)
from apps.user.models import User


# -------------------------------------------------------------------
# 공통 기반
# -------------------------------------------------------------------
class BaseContactViewTest(TestCase):
    def setUp(self):
        self.factory = APIRequestFactory()
        self.user1 = User.objects.create(email="user1@example.com", name="User One")
        self.user2 = User.objects.create(email="user2@example.com", name="User Two")


# -------------------------------------------------------------------
# GroupListCreateView / GroupDetailView
# -------------------------------------------------------------------
class GroupViewTest(BaseContactViewTest):
    def test_group_list_only_returns_owner_groups(self):
        """
        List에서 OwnerQuerysetMixin이 현재 user의 Group만 반환하는지 확인
        """
        g1 = Group.objects.create(user=self.user1, name="Team A", description="A desc")
        Group.objects.create(user=self.user2, name="Team B", description="B desc")

        request = self.factory.get("/api/contact/groups/")
        force_authenticate(request, user=self.user1)

        response = GroupListCreateView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        returned_ids = [item["id"] for item in response.data]
        self.assertIn(g1.id, returned_ids)
        self.assertEqual(len(returned_ids), 1)

    def test_group_create_sets_user_automatically(self):
        """
        perform_create()가 serializer.save(user=...) 호출하는지 확인
        (필드 유효성은 serializer 내부 몫이므로 mock으로 충분)
        """
        request = self.factory.post(
            "/api/contact/groups/",
            {"name": "Friends", "description": "my people"},
            format="json",
        )
        force_authenticate(request, user=self.user1)

        mock_serializer = MagicMock(spec=GroupSerializer)
        mock_serializer.save = MagicMock()

        view = GroupListCreateView()
        # perform_create 안에서 self.request.user 사용하므로 최소 구조만 가진 네임스페이스 주입
        view.request = SimpleNamespace(user=self.user1)

        view.perform_create(mock_serializer)
        mock_serializer.save.assert_called_once_with(user=self.user1)

    def test_group_detail_only_owner_can_access(self):
        """
        RetrieveUpdateDestroyAPIView + OwnerQuerysetMixin 조합으로
        다른 유저 소유 리소스는 404 이어야 한다
        """
        g1 = Group.objects.create(user=self.user1, name="MyGroup", description="desc1")
        g2 = Group.objects.create(user=self.user2, name="OtherGroup", description="desc2")

        # user1 -> 자기 그룹 조회 OK
        req_ok = self.factory.get(f"/api/contact/groups/{g1.id}/")
        force_authenticate(req_ok, user=self.user1)
        resp_ok = GroupDetailView.as_view()(req_ok, pk=g1.id)
        self.assertEqual(resp_ok.status_code, status.HTTP_200_OK)
        self.assertEqual(resp_ok.data["id"], g1.id)

        # user1 -> user2 그룹 조회 404
        req_forbid = self.factory.get(f"/api/contact/groups/{g2.id}/")
        force_authenticate(req_forbid, user=self.user1)
        resp_forbid = GroupDetailView.as_view()(req_forbid, pk=g2.id)
        self.assertEqual(resp_forbid.status_code, status.HTTP_404_NOT_FOUND)


# -------------------------------------------------------------------
# ContactListCreateView / ContactDetailView
# -------------------------------------------------------------------
class ContactViewTest(BaseContactViewTest):
    def setUp(self):
        super().setUp()
        self.group_user1 = Group.objects.create(user=self.user1, name="U1 Group", description="g1")
        self.group_user2 = Group.objects.create(user=self.user2, name="U2 Group", description="g2")

        # Contact 필수 필드는 (user, name, email, group[nullable 가능]) 라고 가정
        self.c1 = Contact.objects.create(
            user=self.user1,
            name="Alice U1",
            email="a@u1.com",
            group=self.group_user1,
        )
        self.c2 = Contact.objects.create(
            user=self.user2,
            name="Bob U2",
            email="b@u2.com",
            group=self.group_user2,
        )
        self.c3_same_group = Contact.objects.create(
            user=self.user1,
            name="Charlie U1",
            email="c@u1.com",
            group=self.group_user1,
        )

    def test_contact_list_only_owner(self):
        """
        user1은 자신의 Contact만 조회해야 한다
        """
        request = self.factory.get("/api/contact/contacts/")
        force_authenticate(request, user=self.user1)

        response = ContactListCreateView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        emails = sorted([row["email"] for row in response.data])
        self.assertEqual(emails, sorted([self.c1.email, self.c3_same_group.email]))
        self.assertNotIn(self.c2.email, emails)

    def test_contact_list_filter_by_group_query_param(self):
        """
        /contacts/?group=<id> 로 특정 group 소속만 필터되는지 확인
        """
        request = self.factory.get(
            "/api/contact/contacts/",
            {"group": self.group_user1.id},
        )
        force_authenticate(request, user=self.user1)

        response = ContactListCreateView.as_view()(request)

        self.assertEqual(response.status_code, status.HTTP_200_OK)
        returned_emails = {row["email"] for row in response.data}
        self.assertSetEqual(
            returned_emails,
            {self.c1.email, self.c3_same_group.email},
        )

    def test_contact_create_calls_serializer_save_with_user(self):
        """
        perform_create() 가 serializer.save(user=request.user)를 호출하는지 확인
        """
        request = self.factory.post(
            "/api/contact/contacts/",
            {
                "name": "New Guy",
                "email": "new@u1.com",
                "group_id": self.group_user1.id,
                "context": {
                    "sender_role": "me",
                    "recipient_role": "professor",
                    "relationship_details": "I am their student",
                    "personal_prompt": "be polite",
                    "language_preference": "en",
                },
            },
            format="json",
        )
        force_authenticate(request, user=self.user1)

        mock_serializer = MagicMock(spec=ContactSerializer)
        mock_serializer.save = MagicMock()

        view = ContactListCreateView()
        # perform_create 내부에서 self.request.user 읽으니까 넣어준다
        view.request = SimpleNamespace(user=self.user1)

        view.perform_create(mock_serializer)
        mock_serializer.save.assert_called_once_with(user=self.user1)

    def test_contact_detail_only_owner_can_access(self):
        """
        다른 유저의 Contact는 404
        """
        # 자기 것 조회 OK
        req_ok = self.factory.get(f"/api/contact/contacts/{self.c1.id}/")
        force_authenticate(req_ok, user=self.user1)
        resp_ok = ContactDetailView.as_view()(req_ok, pk=self.c1.id)
        self.assertEqual(resp_ok.status_code, status.HTTP_200_OK)
        self.assertEqual(resp_ok.data["id"], self.c1.id)

        # 남의 것 조회 404
        req_forbid = self.factory.get(f"/api/contact/contacts/{self.c2.id}/")
        force_authenticate(req_forbid, user=self.user1)
        resp_forbid = ContactDetailView.as_view()(req_forbid, pk=self.c2.id)
        self.assertEqual(resp_forbid.status_code, status.HTTP_404_NOT_FOUND)

    def test_contact_detail_update_calls_serializer_save(self):
        """
        perform_update() 는 단순히 serializer.save() 만 호출.
        트랜잭션 atomic은 DB 롤백 테스트까지는 안 하고 호출 여부만 확인.
        """
        req = self.factory.patch(
            f"/api/contact/contacts/{self.c1.id}/",
            {"name": "Renamed"},
            format="json",
        )
        force_authenticate(req, user=self.user1)

        mock_serializer = MagicMock(spec=ContactSerializer)
        mock_serializer.save = MagicMock()

        view = ContactDetailView()
        view.request = SimpleNamespace(user=self.user1)

        view.perform_update(mock_serializer)
        mock_serializer.save.assert_called_once()


# -------------------------------------------------------------------
# ContactContextDetailView / ContactContextByContactView
# -------------------------------------------------------------------
class ContactContextViewTest(BaseContactViewTest):
    def setUp(self):
        super().setUp()

        # 그룹/연락처 생성
        g1 = Group.objects.create(user=self.user1, name="G1", description="g1")
        g2 = Group.objects.create(user=self.user2, name="G2", description="g2")

        self.c_u1 = Contact.objects.create(
            user=self.user1,
            name="Alice U1",
            email="a@u1.com",
            group=g1,
        )
        self.c_u2 = Contact.objects.create(
            user=self.user2,
            name="Bob U2",
            email="b@u2.com",
            group=g2,
        )

        # ContactContext 필드는 serializer와 모델 정의에 맞춰 생성
        # sender_role / recipient_role / relationship_details / personal_prompt / language_preference
        self.ctx_u1 = ContactContext.objects.create(
            contact=self.c_u1,
            sender_role="student",
            recipient_role="professor",
            relationship_details="CS101 TA",
            personal_prompt="Be respectful and concise",
            language_preference="en",
        )
        self.ctx_u2 = ContactContext.objects.create(
            contact=self.c_u2,
            sender_role="subordinate",
            recipient_role="manager",
            relationship_details="Team lead",
            personal_prompt="Formal tone",
            language_preference="en",
        )

    def test_contact_context_detail_queryset_filters_by_owner(self):
        """
        ContactContextDetailView.get_queryset() 는
        contact__user=self.request.user 로 제한되어야 함
        """
        req = self.factory.get("/api/contact/contact-contexts/")
        force_authenticate(req, user=self.user1)

        view = ContactContextDetailView()
        view.request = SimpleNamespace(user=self.user1)

        qs = view.get_queryset()
        self.assertIn(self.ctx_u1, list(qs))
        self.assertNotIn(self.ctx_u2, list(qs))

    def test_contact_context_by_contact_view_get_object_owner_ok(self):
        """
        ContactContextByContactView.get_object():
        내가 소유한 contact_id면 해당 ContactContext를 리턴
        """
        req = self.factory.get(f"/api/contact/{self.c_u1.id}/context/")
        force_authenticate(req, user=self.user1)

        view = ContactContextByContactView()
        view.request = SimpleNamespace(user=self.user1)
        view.kwargs = {"contact_id": self.c_u1.id}

        obj = view.get_object()
        self.assertEqual(obj.id, self.ctx_u1.id)
        self.assertEqual(obj.contact, self.c_u1)

    def test_contact_context_by_contact_view_get_object_forbidden_other_user(self):
        """
        다른 사용자의 contact_id면 get_object_or_404에서 404 (Http404 예외)
        """
        req = self.factory.get(f"/api/contact/{self.c_u2.id}/context/")
        force_authenticate(req, user=self.user1)

        view = ContactContextByContactView()
        view.request = SimpleNamespace(user=self.user1)
        view.kwargs = {"contact_id": self.c_u2.id}

        with self.assertRaises(Http404):
            _ = view.get_object()


# -------------------------------------------------------------------
# PromptOptionListCreateView / PromptOptionDetailView
# -------------------------------------------------------------------
class PromptOptionViewTest(BaseContactViewTest):
    def setUp(self):
        super().setUp()
        # PromptOption 모델 실제 필드: key, name, prompt, created_by
        # 시스템 프롬프트: created_by=None
        self.sys_opt = PromptOption.objects.create(
            key="sys_polite",
            name="System Default Polite",
            prompt="Please respond politely.",
            created_by=None,
        )
        self.u1_opt = PromptOption.objects.create(
            key="u1_casual",
            name="Casual tone",
            prompt="Keep it friendly, short.",
            created_by=self.user1,
        )
        self.u2_opt = PromptOption.objects.create(
            key="u2_formal",
            name="Serious tone",
            prompt="Keep it formal and structured.",
            created_by=self.user2,
        )

    def test_promptoption_list_includes_system_and_own(self):
        """
        ListCreateView.get_queryset():
        Q(created_by=user) | Q(created_by__isnull=True)
        -> 시스템 + 내 것만 보여야 함
        """
        req = self.factory.get("/api/contact/prompt-options/")
        force_authenticate(req, user=self.user1)

        resp = PromptOptionListCreateView.as_view()(req)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)

        names = {item["name"] for item in resp.data}
        # user1은 sys_opt, u1_opt만 봐야 하고 u2_opt는 나오면 안 됨
        self.assertIn(self.sys_opt.name, names)
        self.assertIn(self.u1_opt.name, names)
        self.assertNotIn(self.u2_opt.name, names)

    def test_promptoption_create_sets_created_by(self):
        """
        perform_create() -> serializer.save(created_by=request.user)
        """
        req = self.factory.post(
            "/api/contact/prompt-options/",
            {"key": "new_key", "name": "MyStyle", "prompt": "Be super kind"},
            format="json",
        )
        force_authenticate(req, user=self.user1)

        mock_serializer = MagicMock(spec=PromptOptionSerializer)
        mock_serializer.save = MagicMock()

        view = PromptOptionListCreateView()
        view.request = SimpleNamespace(user=self.user1)

        view.perform_create(mock_serializer)
        mock_serializer.save.assert_called_once_with(created_by=self.user1)

    def test_promptoption_detail_queryset_contains_system_and_own(self):
        """
        DetailView.get_queryset() 도 시스템 + 내 옵션만 포함하는 QuerySet 을 돌려줘야 함
        (OR 조건)
        """
        req = self.factory.get(f"/api/contact/prompt-options/{self.sys_opt.id}/")
        force_authenticate(req, user=self.user1)

        view = PromptOptionDetailView()
        view.request = SimpleNamespace(user=self.user1)

        qs = view.get_queryset()
        ids = list(qs.values_list("id", flat=True))

        self.assertIn(self.sys_opt.id, ids)
        self.assertIn(self.u1_opt.id, ids)
        self.assertNotIn(self.u2_opt.id, ids)

    def test_promptoption_detail_update_forbidden_if_system_defined(self):
        """
        perform_update():
        - created_by is None이면 PermissionDenied
        - 아니면 serializer.save()
        """
        req = self.factory.patch(
            f"/api/contact/prompt-options/{self.sys_opt.id}/",
            {"name": "hack"},
            format="json",
        )
        force_authenticate(req, user=self.user1)

        view = PromptOptionDetailView()
        view.request = SimpleNamespace(user=self.user1)

        # 시스템 옵션이면 PermissionDenied
        view.get_object = MagicMock(return_value=self.sys_opt)
        mock_ser = MagicMock(spec=PromptOptionSerializer)

        with self.assertRaises(PermissionDenied):
            view.perform_update(mock_ser)

        # 사용자 옵션이면 허용
        view.get_object = MagicMock(return_value=self.u1_opt)
        view.perform_update(mock_ser)
        mock_ser.save.assert_called_once()

    def test_promptoption_detail_destroy_forbidden_if_system_defined(self):
        """
        perform_destroy():
        - created_by is None -> PermissionDenied
        - 아니면 instance.delete()
        """
        req = self.factory.delete(f"/api/contact/prompt-options/{self.sys_opt.id}/")
        force_authenticate(req, user=self.user1)

        view = PromptOptionDetailView()
        view.request = SimpleNamespace(user=self.user1)

        # 시스템 옵션 삭제 시도
        with self.assertRaises(PermissionDenied):
            view.perform_destroy(self.sys_opt)

        # 개인 옵션 삭제 시도 -> delete() 호출
        mock_instance = MagicMock()
        mock_instance.created_by = self.user1
        view.perform_destroy(mock_instance)
        mock_instance.delete.assert_called_once()


# -------------------------------------------------------------------
# TemplateListCreateView / TemplateDetailView
# -------------------------------------------------------------------
class TemplateViewTest(BaseContactViewTest):
    def setUp(self):
        super().setUp()
        # Template 모델 필드: user(FK), name, content
        self.t1 = Template.objects.create(
            user=self.user1,
            name="Friendly Reply",
            content="Thanks for reaching out...",
        )
        self.t2 = Template.objects.create(
            user=self.user2,
            name="Formal Reply",
            content="Dear {{name}}, ...",
        )

    def test_template_list_owner_only(self):
        """
        OwnerQuerysetMixin: user1은 자기 Template만 조회해야 한다
        """
        req = self.factory.get("/api/contact/templates/")
        force_authenticate(req, user=self.user1)

        resp = TemplateListCreateView.as_view()(req)
        self.assertEqual(resp.status_code, status.HTTP_200_OK)

        names = {row["name"] for row in resp.data}
        self.assertIn(self.t1.name, names)
        self.assertNotIn(self.t2.name, names)

    def test_template_create_sets_user(self):
        """
        perform_create() -> serializer.save(user=request.user)
        """
        req = self.factory.post(
            "/api/contact/templates/",
            {
                "name": "Short Ping",
                "content": "Just checking in.",
            },
            format="json",
        )
        force_authenticate(req, user=self.user1)

        mock_serializer = MagicMock(spec=TemplateSerializer)
        mock_serializer.save = MagicMock()

        view = TemplateListCreateView()
        view.request = SimpleNamespace(user=self.user1)

        view.perform_create(mock_serializer)
        mock_serializer.save.assert_called_once_with(user=self.user1)

    def test_template_detail_only_owner_can_access(self):
        """
        RetrieveUpdateDestroyAPIView + OwnerQuerysetMixin:
        - 내 템플릿은 200
        - 남의 템플릿은 404
        """
        req_ok = self.factory.get(f"/api/contact/templates/{self.t1.id}/")
        force_authenticate(req_ok, user=self.user1)
        resp_ok = TemplateDetailView.as_view()(req_ok, pk=self.t1.id)
        self.assertEqual(resp_ok.status_code, status.HTTP_200_OK)
        self.assertEqual(resp_ok.data["id"], self.t1.id)

        req_forbid = self.factory.get(f"/api/contact/templates/{self.t2.id}/")
        force_authenticate(req_forbid, user=self.user1)
        resp_forbid = TemplateDetailView.as_view()(req_forbid, pk=self.t2.id)
        self.assertEqual(resp_forbid.status_code, status.HTTP_404_NOT_FOUND)
