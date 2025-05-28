from openai import OpenAI
from django.views.decorators.csrf import csrf_exempt
from django.http import JsonResponse
import json

client = OpenAI(
    base_url="https://ark.cn-beijing.volces.com/api/v3",  # 修改为你的火山引擎代理地址
    api_key="8c55219f-5ca6-4c3f-a0e4-74c89eb263be"  # 替换为你的真实 key
)

@csrf_exempt
def chat_ai_view(request):
    if request.method != "POST":
        return JsonResponse({"error": "只支持 POST 请求"}, status=405)

    try:
        data = json.loads(request.body)
        prompt = data.get("message", "")

        if not prompt:
            return JsonResponse({"error": "缺少 message 参数"}, status=400)

        # ✅ 使用新版 SDK 接口
        response = client.chat.completions.create(
            model="doubao-1-5-thinking-pro-250415",  # 根据你自己的可用模型名称
            messages=[
                {"role": "system", "content": "你是一个友好的编程助手"},
                {"role": "user", "content": prompt}
            ]
        )

        ai_reply = response.choices[0].message.content
        print("AI 回复内容：", ai_reply)
        return JsonResponse({"response": ai_reply})

    except Exception as e:
        print("AI 调用失败:", str(e))
        return JsonResponse({"error": str(e)}, status=500)
