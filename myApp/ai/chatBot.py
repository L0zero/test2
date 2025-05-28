from myApp.ai.llm import LLM
from myApp.ai.rag import VectorDBManager
from myApp.models import DiscussionSummary, Group, Message, Project, User, UserProjectRepo
import base64
import hashlib
import requests

import logging

logger = logging.getLogger(__name__)

class ChatBot:
    def __init__(self, project_id: str, user_id: str, llm: LLM = LLM()):
        self.LLM = llm
        self.project_id = project_id
        self.user_id = user_id

    def analyze_question(self, question: str, depth: int, context: str) -> str:
        """
        分析问题并确定其类别。
        使用大语言模型进行分类。
        可能的类别包括：codebase, document, knowledge_base, general。
        """
        
        prompt = f"""你是一名技术专家，请根据以下问题内容判断其所属类别 (codebase, document, knowledge_base, general)。
        请注意：问题已被查询 {depth} 次，避免不必要的重复分类和查询，仅在必要时进行分类。
        如果问题内容不明确或查询次数过多，请直接返回 'general'。
        问题内容：{question}
        当前上下文：{context}
        请给出明确的分类结果："""
        return self.LLM.call_api(prompt,True).strip()

    def retrieve_from_codebase(self, question: str) -> str:
        """
        从代码库中检索相关信息。
        使用Github API进行检索。
        """
        search_result=CodeSearchUtil.search_code(
            project_id=self.project_id,
            query=question,
            user_id=self.user_id
        )
        if search_result['errcode'] != 0:
            logger.error(f"代码库检索失败: {search_result['message']}")
            return ""
        results=search_result.get('data', {}).get('results', [])
        if not results:
            return ""
        formatted=[]
        for item in results:
            formatted.append(
                f"文件路径: {item['file_path']}\n"
                f"仓库: {item['repository']}\n"
                f"内容:\n{item['content']}\n"
            )
        return "\n\n".join(formatted)

    def retrieve_from_documents(self, question: str) -> str:
        """
        从文档中检索相关信息。
        使用向量数据库进行检索。
        """
        db_manager = VectorDBManager(name=f"{self.project_id}_doc")
        results = db_manager.query_db(question, top_k=3)
        return "\n".join([f"Document: {result['content']}\nScore: {result['score']:.2f}\n" for result in results])

    def retrieve_from_knowledge_base(self, question: str) -> str:
        """
        从知识库中检索相关信息。
        使用向量数据库进行检索。
        """
        db_manager = VectorDBManager(name=f"{self.project_id}_knowledge")
        results = db_manager.query_db(question, top_k=3)
        return "\n".join([f"Knowledge Base: {result['content']}\nScore: {result['score']:.2f}\n" for result in results])

    def generate_answer(self, question: str, context: str):
        """
        使用大语言模型生成答案。
        """
        prompt = f"Question: {question}\n\nContext: {context}\n\nAnswer:"
        ans = self.LLM.call_api(prompt,True).strip()
        logger.debug('generate_answer: ' + ans)
        return ans

    def handle_request(self, question: str, depth: int = 0, context: str = '') -> str:
        """
        处理用户请求，分析问题，检索相关信息，并生成答案。
        :param question: 用户提问
        :return: 生成的答案
        """
        category = self.analyze_question(question, depth, context)
        logger.debug(f"问题分类: {category}")
        
        if depth > 3:
            logger.debug("查询次数过多，直接返回 'general'")
            return self.generate_answer(question, context)
        
        if "codebase" in category:
            logger.debug(f"代码库检索: {question}")
            s = self.retrieve_from_codebase(question)
            if s:
                logger.debug(f"代码库检索成功: {s}")
                context += "\n" + s 
            else:
                logger.debug("代码库检索失败")
        elif "document" in category:
            logger.debug(f"文档检索: {question}")
            s = self.retrieve_from_documents(question)
            if s:
                logger.debug(f"文档检索成功: {s}")
                context += "\n" + s
            else:
                logger.debug("文档检索失败")
        elif "knowledge_base" in category:
            logger.debug(f"知识库检索: {question}")
            s = self.retrieve_from_knowledge_base(question)
            if s:
                logger.debug(f"知识库检索成功: {s}")
                context += "\n" + s
            else:
                logger.debug("知识库检索失败")
        else:
            logger.debug(f"无需再次查询: {question}")
            return self.generate_answer(question, context)

        return self.handle_request(question, depth + 1, context)


class CodeSearchUtil:
    """
    代码库检索工具类，提供静态方法用于代码搜索
    """
    @staticmethod
    def search_code(project_id, query, user_id):
        """
        搜索代码库中的代码
        
        Args:
            project_id: 项目ID
            query: 搜索查询
            user_id: 用户ID
            
        Returns:
            dict: 包含搜索结果的字典，格式为:
                {
                    'errcode': 0,  # 0表示成功，其他值表示错误
                    'message': 'success',  # 成功或错误消息
                    'data': {
                        'results': [  # 搜索结果列表
                            {
                                'file_path': '文件路径',
                                'file_name': '文件名',
                                'repository': '仓库名',
                                'url': 'GitHub URL',
                                'content': '文件内容',
                                'line_number': 行号
                            },
                            ...
                        ]
                    }
                }
        """
        response = {'errcode': 0, 'message': 'success'}
        try:
            try:
                project = Project.objects.get(id=project_id)
            except Project.DoesNotExist:
                response.update({'errcode': 404, 'message': '项目不存在'})
                return response

            # 获取用户对象
            try:
                user = User.objects.get(id=user_id)
            except User.DoesNotExist:
                response.update({'errcode': 404, 'message': '用户不存在'})
                return response

            # 获取项目关联的仓库
            userProjectRepos = UserProjectRepo.objects.filter(project_id=project)
            repos = []
            for userProjectRepo in userProjectRepos:
                repos.append(userProjectRepo.repo_id)
            
            if not repos:
                response.update({'errcode': 404, 'message': '项目未关联任何代码仓库'})
                return response

            search_results = []
            for repo in repos:
                # 构建 GitHub API 搜索 URL
                search_url = f"https://api.github.com/search/code"
                headers = {
                    "Accept": "application/vnd.github.v3+json",
                    "Authorization": f"token {user.token}"  # 使用用户的 token
                }
                params = {
                    "q": f"repo:{repo.remote_path} {query}",
                    "per_page": 10  # 限制返回结果数量
                }

                try:
                    # 调用 GitHub API
                    resp = requests.get(search_url, headers=headers, params=params)
                    if resp.status_code == 200:
                        results = resp.json()
                        for item in results.get('items', []):
                            # 获取文件内容
                            content_url = item['url']
                            content_resp = requests.get(content_url, headers=headers)
                            if content_resp.status_code == 200:
                                content_data = content_resp.json()
                                file_content = content_data.get('content', '')
                                if file_content:
                                    # 如果是 base64 编码的内容，需要解码
                                    file_content = base64.b64decode(file_content).decode('utf-8')

                                search_results.append({
                                    'file_path': item['path'],
                                    'file_name': item['name'],
                                    'repository': f"{repo.owner}/{repo.name}",
                                    'url': item['html_url'],
                                    'content': file_content,
                                    'line_number': item.get('line_number', None)
                                })
                    elif resp.status_code == 403:
                        response.update({'errcode': 403, 'message': 'GitHub API 访问限制，请稍后再试'})
                        return response
                    else:
                        print(f"GitHub API 错误: {resp.status_code} - {resp.text}")
                except Exception as e:
                    print(f"搜索仓库 {repo.name} 时出错: {str(e)}")
                    continue

            if not search_results:
                response.update({'errcode': 404, 'message': '未找到匹配的代码'})
                return response

            response['data'] = {
                'results': search_results
            }
            
        except KeyError as e:
            response.update({'errcode': 400, 'message': f'缺少必要参数: {str(e)}'})
        except Exception as e:
            response.update({'errcode': 500, 'message': str(e)})
        
        return response