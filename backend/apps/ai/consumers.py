import asyncio
import json

import redis.asyncio as aioredis
import requests
from asgiref.sync import sync_to_async
from channels.generic.websocket import AsyncWebsocketConsumer
from django.conf import settings

from .services.utils import build_prompt_inputs, collect_prompt_context


class MailGenerateConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        if self.scope.get("token_invalid", False):
            await self.accept()
            await self.send(text_data=json.dumps({"type": "error", "message": "token_invalid"}))
            await self.close()
            return

        if not self.scope["user"].is_authenticated:
            await self.close()
            return

        self.user = self.scope["user"]
        self.room_group_name = f"user_{self.user.id}_mail"

        # WebSocket 연결 허용
        await self.accept()

        # Redis 구독 설정
        self.redis = await aioredis.from_url("redis://redis:6379")
        self.pubsub = self.redis.pubsub()
        await self.pubsub.subscribe(self.room_group_name)

        # Redis 메시지 수신 태스크 실행
        self.listener_task = asyncio.create_task(self.redis_listener())

    async def disconnect(self, close_code):
        # 연결 해제 시 자원 정리
        if hasattr(self, "listener_task"):
            self.listener_task.cancel()
        if hasattr(self, "pubsub"):
            await self.pubsub.unsubscribe(self.room_group_name)
        if hasattr(self, "redis"):
            await self.redis.close()

    async def receive(self, text_data):
        """
        프론트엔드에서 GPU 요청 전송 시 호출됨.
        Django는 이 요청을 GPU 서버에 중계함.
        """
        data = json.loads(text_data)
        # 1. 입력 내용의 길이가 20 이하이면 생성하지 않도록 함
        if len(data.get("text")) < 20:
            await self.send(text_data=json.dumps({"type": "noop"}))
        else:
            # json should include: to_emails, body, subject
            user = self.user

            to_emails = data.get("to_emails", [])  # 보내는 사람들 = list[str]
            ctx = await sync_to_async(collect_prompt_context)(user, to_emails)
            # async한 receive 함수 내부 / collect_prompt_context 함수 내부에서 ORM 호출(sync)이 존재함
            # Django ORM을 async context 내부에서 바로 호출 할 수 없음 -> sync_to_async로 변환하여 스레드에서 실행되게
            raw_inputs = build_prompt_inputs(ctx)
            raw_inputs["body"] = data.get("body", "")
            raw_inputs["subject"] = data.get("subject", "")

            system_prompt = """
            당신은 사용자가 작성 중인 메일을 이어서 완성하는 역할을 수행합니다.
            사용자가 작성한 내용에 자연스럽게 이어서 6단어 정도만 작성하세요.
            사용자가 이미 작성한 내용을 중복하여 출력하지 않습니다.
            출력은 반드시 JSON 형태로 아래와 같이 작성합니다.
            """

            if raw_inputs["recipients"]:
                recipients_str = ", ".join(raw_inputs["recipients"])
                system_prompt += f"\n사용자는 다음의 수신자에게 메일을 작성하고 있습니다:\n{recipients_str}"

            if raw_inputs["group_description"]:
                system_prompt += f"\n수신자들에 대한 설명은 다음과 같습니다:\n{raw_inputs['group_description']}"

            if raw_inputs["prompt_text"]:
                system_prompt += f"\n문장을 작성할 때 다음과 같은 스타일의 문체를 사용합니다:\n{raw_inputs['prompt_text']}"

            if raw_inputs["subject"]:
                system_prompt += f"\n사용자는 다음 제목의 메일을 작성하고 있습니다.\n{raw_inputs['subject']}"

            if raw_inputs["body"]:
                system_prompt += f"\n사용자는 다음 내용의 메일에 답장하고 있습니다:\n{raw_inputs["body"]}"

            if raw_inputs["analysis"]:
                system_prompt += f"\n사용자가 수신자들에게 작성한 메일을 분석한 내용은 다음과 같습니다:\n{raw_inputs["analysis"]}"

            system_prompt += """\n\n
            user: 안녕하세요 오늘 회의 진행을 맡은 홍길동 대리입니다.
            output: {"output": "오늘 회의 자료를 준비하면서"}

            user: 저는 내일 미팅을 준비하고 있고, 
            output: {"output": "미팅 준비를 위해 필요한 자료와"}
            """

            try:
                print("[DEBUG] Sending GPU request:", settings.GPU_SERVER_BASEURL + "predict")
                # GPU 서버로 직접 REST POST
                response = requests.post(
                    settings.GPU_SERVER_BASEURL + "predict",
                    json={
                        "user_id": self.user.id,
                        "system_prompt": system_prompt,
                        "user_input": data.get("text"),
                        "max_tokens": 30,  # fixed value
                    },
                    timeout=5,  # GPU 서버 응답 지연 시 타임아웃 방지
                )
                response.raise_for_status()
                print("[DEBUG] GPU Response:", response.status_code, response.text)
            except Exception as e:
                # GPU 서버 요청 실패 시 프론트로 에러 메시지 전달
                print("[ERROR] GPU Request Failed:", e)
                await self.send(text_data=json.dumps({"type": "error", "message": f"GPU 요청 실패: {str(e)}"}))

    async def redis_listener(self):
        """
        Redis Pub/Sub을 통해 GPU 서버에서 보내는 토큰 스트림을 수신하고,
        클라이언트(WebSocket)으로 전달함.
        """
        async for message in self.pubsub.listen():
            if message["type"] == "message":
                try:
                    event = json.loads(message["data"])
                    await self.send(text_data=json.dumps(event))
                except json.JSONDecodeError:
                    # 혹시 malformed JSON이 오면 무시
                    continue
