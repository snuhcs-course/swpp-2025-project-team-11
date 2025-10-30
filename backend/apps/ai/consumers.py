import asyncio
import json

import redis.asyncio as aioredis
import requests
from channels.generic.websocket import AsyncWebsocketConsumer
from django.conf import settings


class MailGenerateConsumer(AsyncWebsocketConsumer):
    async def connect(self):
        if self.scope.get("token_invalid", False):
            await self.accept()
            await self.send_json({"type": "error", "message": "token_invalid"})
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

        try:
            print("[DEBUG] Sending GPU request:", settings.GPU_SERVER_BASEURL + "predict")
            # GPU 서버로 직접 REST POST
            response = requests.post(
                settings.GPU_SERVER_BASEURL + "predict",
                json={
                    "user_id": self.user.id,
                    "system_prompt": data.get("system_prompt"),
                    "user_input": data.get("text"),
                    "max_tokens": data.get("max_tokens", 10),
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
