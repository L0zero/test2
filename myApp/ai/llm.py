import requests
import json
import logging

logger = logging.getLogger(__name__)

class LLM:
    """
    LLM类，用于与大语言模型进行交互
    """
    def __init__(self, model: str = "glm-4-plus", temperature: float = 0.05, url: str = "https://open.bigmodel.cn/api/paas/v4/chat/completions"):
        """
        初始化LLM类
        :param model: 模型名称
        :param temperature: 温度参数，控制生成文本的随机性
        :param url: API请求地址
        """
        self.model = model
        self.temperature = temperature
        self.url = url
    
    def call_api(self, prompt, isAnswer):
        headers = {"Authorization": "Bearer 84e67deb292945428711e34beb999645.beRfuJzKAFOciVhM"}
        payload = {
            "model": self.model,
            "messages": [{"role": "user", "content": prompt}],
            "temperature": self.temperature,
            "response_format": {"type": "json_object"}
            }
                
        resp = requests.post(
            self.url,
            json=payload,
            headers=headers
            )
                
        if resp.status_code != 200:
            logger.error(f"API请求失败: {resp.status_code} - {resp.text}")
            raise Exception(f"API请求失败: {resp.text}")
                
        result = json.loads(resp.json()['choices'][0]['message']['content'])
        if isAnswer:

            logger.debug(result['answer'])
            return result['answer']
        else:
            return result
    
nor_llm = LLM()
# 直接实例化一个全局的LLM对象