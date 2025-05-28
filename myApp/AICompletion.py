from django.views.decorators.csrf import csrf_exempt
from django.http import JsonResponse
import json
from openai import OpenAI

client = OpenAI(
    base_url="https://ark.cn-beijing.volces.com/api/v3",
    api_key="8c55219f-5ca6-4c3f-a0e4-74c89eb263be"
)

@csrf_exempt
def ai_completion(request):
    if request.method == 'POST':
        body = json.loads(request.body)
        code = body.get('prompt', '')
        language = body.get('language', 'python')
        print(code)

        try:
            prompt = f"根据以下{language}代码补全接下来的内容：\n\n{code}\n\n# 继续补全："
            
            response = client.chat.completions.create(
                model="doubao-1-5-thinking-pro-250415",  # 你可以换你支持的模型，比如 gpt-3.5-turbo
                messages=[
                    {"role": "system", "content": "你是一个优秀的编程助手，帮我补全代码，不要解释。"},
                    {"role": "user", "content": prompt}
                ],
                temperature=0.2,
                max_tokens=100,
                stream=False
            )

            ai_text = response.choices[0].message.content.strip()
            print("AI 回复内容：", ai_text)

            # 简单处理一下，返回一个补全建议
            return JsonResponse({
                "suggestions": [
                    {
                        "label": ai_text.split('\n')[0][:30],  # 用第一行前30字符做 label
                        "insertText": ai_text,
                        "documentation": "AI自动补全"
                    }
                ]
            })

        except Exception as e:
            return JsonResponse({"error": str(e)}, status=500)

    else:
        return JsonResponse({"error": "Invalid request method"}, status=405)
