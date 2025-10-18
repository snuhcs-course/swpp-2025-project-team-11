from fastapi import FastAPI
from .models import PromptRequest, PredictionResponse
from .llm import generate_response

app = FastAPI(title="EXAONE LLM GPU Server")

@app.post("/predict", response_model=PredictionResponse)
def predict(req: PromptRequest):
    try:
        prediction = generate_response(req.prompt, req.max_tokens)
        return {"prediction": prediction}
    except Exception as e:
        return {"prediction": f"Error: {str(e)}"}
