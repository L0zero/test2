import base64
import hashlib
import requests
from django.views import View
from django.http import JsonResponse
from django.core.paginator import Paginator
import json
from myApp.ai.chatBot import ChatBot
from myApp.ai.llm import nor_llm as llm

from myApp.ai.rag import VectorDBManager
from myApp.models import DiscussionSummary, Group, Message, Project, User, UserProjectRepo


SCORING_PROMPT_TEMPLATE = """作为技术知识管理专家，请严格按以下步骤处理：

【任务要求】
1. 分析讨论室的对话记录，将对话记录转换为问答对
2. 按以下标准评分（1-100分）：
   - 领域相关性（20%）：是否与软件开发、项目管理、技术方案相关
   - 知识复用性（40%）：是否具有长期参考价值，能否帮助其他团队成员
   - 信息有效性（40%）：是否准确解决问题（不要求步骤完整，但需切中要害）
3. 输出严格遵循JSON格式：
{{
  "qas": [
    {{
      "question": "问题文本",
      "answer": "回答文本",
      "score": 评分值,
      "reason": "评分理由"
    }}
  ]
}}

【示例输出】
{{
  "qas": [
    {{
      "question": "如何配置ESLint？",
      "answer": "安装eslint包，创建.eslintrc配置文件",
      "score": 85,
      "reason": "涉及开发工具配置，具有长期参考价值"
    }}
  ]
}}

【对话记录】
{conversation}
""" 

QA_PROMPT_TEMPLATE = """你是一个技术问答助手，请根据以下知识库回答问题。
            
【知识库】
{knowledge_base}
            
【用户问题】
{question}
            
【回答要求】
1. 基于知识库内容，结合你对于问题的理解，给出一个答案
2. 如果问题明显超出知识范围，与项目毫无关系，请回答"该问题与项目无关，无法回答您的问题"
3. 保持专业性，语句通顺，意思明确，不需要解释来源
"""

def create_hash(question, answer):
    return hashlib.md5(f"{question}||{answer}".encode('utf-8')).hexdigest()

class GenerateDiscussionSummary(View):
    def post(self, request):
        response = {'errcode': 0, 'message': 'success'}
        try:
            kwargs = json.loads(request.body) 
            room_id = kwargs['roomId']
            project_id = kwargs['projectId']
            try:
                group = Group.objects.get(id=room_id)
                project = Project.objects.get(id=project_id)
            except (Group.DoesNotExist, Project.DoesNotExist):
                response.update({'errcode': 404, 'message': '资源不存在'})
                return JsonResponse(response)
            #获取已有知识库问题的哈希集合（用于去重）
            existing_hashes = set(
                DiscussionSummary.objects.filter(
                    group=group,
                    project=project
                ).values_list('question_hash', flat=True)
            )
            # 获取聊天记录（保持不变）
            messages = Message.objects.filter(group_id=group).order_by('time')
            conversation="\n".join(
                    [f"{msg.send_user.name} ({msg.time.strftime('%Y-%m-%d %H:%M')}): {msg.content}" 
                     for msg in messages]
                     )
# 提示词（保持原有逻辑）
             # 使用之前的完整提示词
            prompt=SCORING_PROMPT_TEMPLATE.format(conversation=conversation)
            # 调用大模型API
            result = llm.call_api(prompt,False)

            #knowledge_db = VectorDBManager(name=f"{project_id}_knowledge")
            try:
                qa_data = result
                # 修改数据结构：字典列表替代元组
                valid_qas = [
                    {"question": qa['question'], "answer": qa['answer']}
                    for qa in qa_data['qas']
                    if qa['score'] >= 70  # 保持质量筛选
                ]
            except (KeyError, json.JSONDecodeError) as e:
                raise ValueError("大模型返回格式异常") from e
            knowledge_text=[]
            for qa in qa_data['qas']:
                if qa['score'] < 70:
                    continue
                 # 生成唯一哈希值用于去重
                question_hash = create_hash(qa['question'], qa['answer'])
                if question_hash not in existing_hashes:
                    DiscussionSummary.objects.create(
                    group=group,
                    project=project,
                    question=qa['question'],
                    answer=qa['answer'],
                    question_hash=question_hash
                    )
                    existing_hashes.add(question_hash)
                    knowledge_text.append(f"Q: {qa['question']}\nA: {qa['answer']}")
            #knowledge_db.save_text_to_db(knowledge_text)
            # 严格遵循API响应格式
            response['data'] = {
                'summary': valid_qas  # 直接返回字典列表
            }
            # 移除其他统计字段以符合示例要求
            
        except Group.DoesNotExist:
            response.update({'errcode': 404, 'message': '讨论室不存在'})
        except ValueError as e:
            response.update({'errcode': 502, 'message': str(e)})
        except Exception as e:
            response.update({'errcode': 500, 'message': str(e)})
        
        return JsonResponse(response)
    
class AskQuestion(View):
    def post(self, request):
        response = {'errcode': 0, 'message': 'success'}
        try:
            kwargs = json.loads(request.body)
            project_id = kwargs['projectId']
            question = kwargs['question']
            user_id = kwargs['userId']
            try:
                project = Project.objects.get(id=project_id)
            except (Project.DoesNotExist):
                response.update({'errcode': 404, 'message': '资源不存在'})
                return JsonResponse(response)
            chatbot=ChatBot(project_id=project_id, user_id=user_id)
            answer=chatbot.handle_request(question)
            response['data'] = {
                'answer': answer
            }
            # qas=DiscussionSummary.objects.filter(
            #     project=project
            # ).values('question', 'answer')
            # if not qas.exists():
            #     response.update({'errcode': 404, 'message': '该项目暂无知识库数据'})
            #     return JsonResponse(response)
            
            # knowledge_text = "\n".join(
            #     [f"Q: {qa['question']}\nA: {qa['answer']}" for qa in qas]
            # )
            # existing_hashes = set(
            #     DiscussionSummary.objects.filter(
            #         project=project
            #     ).values_list('question_hash', flat=True)
            # )
            # prompt = QA_PROMPT_TEMPLATE.format(
            #     knowledge_base=knowledge_text,
            #     question=question
            # )
            # result = llm.call_api(prompt)
            # answer = result['answer']
            # if answer == "该问题与项目无关，无法回答您的问题":
            #     response['data'] = {
            #     'answer': answer
            #     }
            #     return JsonResponse(response)
            # question_hash = create_hash(question, answer)
            # if question_hash not in existing_hashes:
            #         DiscussionSummary.objects.create(
            #         project=project,
            #         question=question,
            #         answer=answer,
            #         question_hash=question_hash
            #         )
            # response['data'] = {
            #     'answer': answer
            # }
        except KeyError as e:
            response.update({'errcode': 400, 'message': f'缺少必要参数: {str(e)}'})
        except Exception as e:
            response.update({'errcode': 500, 'message': str(e)})
        return JsonResponse(response)

