from fastapi import FastAPI, HTTPException
from app.models import PredictRequest
from app.llm import generate_reply

app = FastAPI(title="GPU Server for EXAONE")

@app.post("/predict")
async def predict(req: PredictRequest):
    try:
        reply = generate_reply(req.system_prompt, req.user_input, req.max_tokens)
        return {"response": reply}
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail="예측 중 알 수 없는 오류가 발생했습니다")