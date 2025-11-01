from fastapi import FastAPI, BackgroundTasks, HTTPException, Request
from app.models import PredictRequest
from app.llm import generate_and_publish
import json

app = FastAPI(title="GPU Server for EXAONE")

@app.post("/predict")
async def predict(bg: BackgroundTasks, request: Request):
    try:
        # 요청 body 원시 확인
        body_bytes = await request.body()
        print("Raw body:", body_bytes.decode())

        # JSON으로 파싱
        body_json = await request.json()
        print("JSON body:", body_json)

        # Pydantic 모델로 안전하게 변환
        req = PredictRequest(**body_json)

        # 기존 작업 실행
        bg.add_task(generate_and_publish, req.user_id, req.system_prompt, req.user_input, req.max_tokens)
        return {"status": "started"}
    except Exception as e:
        print(f"Exception: {e}")
        raise HTTPException(status_code=500, detail=str(e))