from fastapi import FastAPI
from app.models import PredictRequest
from app.llm import generate_reply

app = FastAPI(title="GPU Server for EXAONE")

@app.post("/predict")
async def predict(req: PredictRequest):
    reply = generate_reply(req.system_prompt, req.user_input, req.max_tokens)
    return {"response": reply}