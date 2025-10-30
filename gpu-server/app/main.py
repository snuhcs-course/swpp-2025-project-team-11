from fastapi import FastAPI, BackgroundTasks, HTTPException
from app.models import PredictRequest
from app.llm import generate_and_publish

app = FastAPI(title="GPU Server for EXAONE")

@app.post("/predict")
async def predict(req: PredictRequest, bg: BackgroundTasks):
    try:
        bg.add_task(generate_and_publish, req.user_id, req.system_prompt, req.user_input, req.max_tokens)
        return {"status": "started"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
