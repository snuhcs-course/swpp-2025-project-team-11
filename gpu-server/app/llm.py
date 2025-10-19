import torch
from transformers import AutoModelForCausalLM, AutoTokenizer

MODEL_NAME = "LGAI-EXAONE/EXAONE-3.5-7.8B-Instruct"

device = "cuda" if torch.cuda.is_available() else "cpu"

tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForCausalLM.from_pretrained(
    MODEL_NAME,
    torch_dtype=torch.bfloat16 if device == "cuda" else torch.float32,
    trust_remote_code=True,
    device_map=None
).to(device)

def generate_reply(system_prompt: str, user_input: str, max_tokens: int = 10):
    try:
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_input},
        ]
        input_ids = tokenizer.apply_chat_template(
            messages,
            tokenize=True,
            add_generation_prompt=True,
            return_tensors="pt"
        ).to(device)

        input_length = input_ids.shape[1]
        
        output = model.generate(
            input_ids,
            eos_token_id=tokenizer.eos_token_id,
            max_new_tokens=max_tokens,
            do_sample=True,
        )

        generated_ids = output[0][input_length:]
        
        return tokenizer.decode(generated_ids, skip_special_tokens=True)
    except torch.cuda.OutOfMemoryError:
        raise RuntimeError("GPU 메모리 부족")
    except Exception as e:
        raise RuntimeError(f"모델 생성 중 오류 발생: {str(e)}")