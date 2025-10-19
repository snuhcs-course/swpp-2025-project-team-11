from pydantic import BaseModel

class PredictRequest(BaseModel):
    # 백엔드에서, 클라이언트의 입력으로부터 구성한 system prompt
    system_prompt: str
    user_input: str
    max_tokens: int = 10
