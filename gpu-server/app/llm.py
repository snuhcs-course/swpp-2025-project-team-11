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

def generate_reply(system_prompt: str, user_input: str, max_tokens: int = 64):
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

    output = model.generate(
        input_ids,
        eos_token_id=tokenizer.eos_token_id,
        max_new_tokens=max_tokens,
        do_sample=True,
    )
    return tokenizer.decode(output[0], skip_special_tokens=True)