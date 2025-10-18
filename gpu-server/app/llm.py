from transformers import AutoTokenizer, AutoModelForCausalLM, pipeline
import torch

DEVICE = "cuda" if torch.cuda.is_available() else "cpu"

MODEL_NAME = "LGAI-EXAONE/EXAONE-3.5-7.8B-Instruct"

tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForCausalLM.from_pretrained(
    MODEL_NAME,
    torch_dtype=torch.float16 if DEVICE == "cuda" else torch.float32,
    low_cpu_mem_usage=True,
    device_map="auto"
)

generator = pipeline(
    "text-generation",
    model=model,
    tokenizer=tokenizer,
    device=0 if DEVICE == "cuda" else -1
)

def format_prompt(user_input: str) -> str:
    # TODO: user context에 맞게 prompt format 
    return f"USER: {user_input}\nASSISTANT:"

def generate_response(prompt: str, max_tokens: int = 10) -> str:
    formatted_prompt = format_prompt(prompt)
    outputs = generator(
        formatted_prompt,
        max_new_tokens=max_tokens,
        num_return_sequences=1,
        temperature=0.7,
        top_p=0.9,
        do_sample=True,
        pad_token_id=tokenizer.eos_token_id
    )
    response = outputs[0]["generated_text"]
    
    # TODO: prompt format에 맞게 수정 필요
    assistant_response = response.split("ASSISTANT:")[-1].strip()
    return assistant_response