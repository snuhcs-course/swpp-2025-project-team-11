import torch
from transformers import AutoModelForCausalLM, AutoTokenizer
import redis.asyncio as aioredis
import json
import asyncio

MODEL_NAME = "LGAI-EXAONE/EXAONE-3.5-7.8B-Instruct"

device = "cuda" if torch.cuda.is_available() else "cpu"

tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
model = AutoModelForCausalLM.from_pretrained(
    MODEL_NAME,
    torch_dtype=torch.bfloat16 if device == "cuda" else torch.float32,
    trust_remote_code=True,
    device_map=None
).to(device)

async def stream_generate_reply(system_prompt: str, user_input: str, max_tokens: int = 10):
    retry = 3 # 최대 3번 재시도
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

    for attempt in range(1, 4):
        try:
            output = model.generate(
                input_ids,
                eos_token_id=tokenizer.eos_token_id,
                max_new_tokens=max_tokens,
                do_sample=True,
            )

            generated_ids = output[0][input_length:]
            generated_text = tokenizer.decode(generated_ids, skip_special_tokens=True)

            print(f"[DEBUG] Attempt {attempt}: {generated_text}")

            # JSON 파싱 시도
            clean_text = generated_text.strip("`json\n ")
            try:
                data = json.loads(clean_text)
                output_text = data.get("output", "")
                print(f"[DEBUG] Parsed JSON successfully on attempt {attempt}")
                break
            except json.JSONDecodeError:
                if attempt < retry:
                    print(f"[WARN] JSON 파싱 실패 (시도 {attempt}/{retry}) → 재시도")
                    await asyncio.sleep(0.3)
                    continue
                else:
                    print(f"[ERROR] JSON 파싱 실패 (최대 재시도 {retry}회 초과)")
                    output_text = generated_text
            break

        except torch.cuda.OutOfMemoryError:
            raise RuntimeError("GPU 메모리 부족")
        except Exception as e:
            raise RuntimeError(f"모델 생성 중 오류 발생: {str(e)}")

    print(f"[DEBUG] 최종 출력: {output_text}")
    for token in output_text.split():
        yield token
        await asyncio.sleep(0.01)

async def generate_and_publish(user_id: int, system_prompt: str, user_input: str, max_tokens: int = 10):
    redis = await aioredis.from_url("redis://xend-fiveis-dev.duckdns.org:6379")
    channel = f"user_{user_id}_mail"

    async for token in stream_generate_reply(system_prompt, user_input, max_tokens):
        message = json.dumps({"type": "gpu.message", "data": {"text": token}})
        await redis.publish(channel, message)

    await redis.publish(channel, json.dumps({"type": "gpu.done"}))
    await redis.close()