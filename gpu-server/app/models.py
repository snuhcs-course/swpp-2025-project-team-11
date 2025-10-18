from pydantic import BaseModel

class PromptRequest(BaseModel):
    prompt: str
    max_tokens: int = 10

class PredictionResponse(BaseModel):
    prediction: str