from typing import List, Dict, Any
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.embeddings import HuggingFaceBgeEmbeddings
from langchain.vectorstores import Chroma
from langchain.schema import Document
import time
from threading import Lock
import logging

logger = logging.getLogger(__name__)

class VectorDBManager:
    # 缓存设计：{name: (instance, timestamp)}
    _cache: Dict[str, tuple] = {}
    _lock = Lock()
    _expiry_time = 3600  # 缓存有效期（秒）

    def __new__(cls, name: str):
        """
        重写 __new__ 实现带缓存的实例化
        """
        with cls._lock:
            # 清理过期缓存
            current_time = time.time()
            expired_keys = [
                # 得到过期的缓存键
                k for k, (_, ts) in cls._cache.items()
                # 如果当前时间减去缓存时间戳大于过期时间，则认为缓存过期
                if current_time - ts > cls._expiry_time
            ]
            for k in expired_keys:
                logger.info(f"清理过期缓存: {k}")
                del cls._cache[k]

            # 返回缓存实例或创建新实例
            if name in cls._cache:
                # 使用缓存实例
                logger.info(f"使用缓存实例: {name}")
                instance, _ = cls._cache[name]
            else:
                # 创建新实例
                logger.info(f"创建新实例: {name}")
                instance = super().__new__(cls)
                instance.__init__(name)  # 显式调用初始化
                cls._cache[name] = (instance, current_time)
            return instance
    
    def __init__(self, name: str):
        """
        向量数据库管理器
        :param name: 数据库持久化目录， 由项目id组成, id_doc表示共享文档数据库, id_knowledge表示知识库数据库
        """
        # 防止重复初始化（因为 __new__ 可能返回已存在的实例）
        if hasattr(self, '_initialized'):
            return
        self._initialized = True
        
        # 初始化配置
        self.embedding_model = self._init_embedding_model()
        self.text_splitter = self._init_text_splitter()
        
        # 初始化时自动加载已有数据库（如果存在）
        self.persist_directory = name + ".db"  # 保存路径
        self.db = self._init_db(self.persist_directory)

    def _init_embedding_model(self) -> HuggingFaceBgeEmbeddings:
        """初始化嵌入模型"""
        return HuggingFaceBgeEmbeddings(
            model_name="moka-ai/m3e-base",
            model_kwargs={'device': 'cpu'},
            encode_kwargs={'normalize_embeddings': True},
            query_instruction="为文本生成向量表示用于文本检索"
        )

    def _init_text_splitter(self) -> RecursiveCharacterTextSplitter:
        """初始化文本分割器"""
        return RecursiveCharacterTextSplitter(
            chunk_size=128,
            chunk_overlap=0,
            separators=["\n\n", "\n", "。", "，", " ", ""]
        )

    def _init_db(self, persist_dir: str) -> Chroma:
        """初始化/加载数据库"""
        return Chroma(
            persist_directory=persist_dir,
            embedding_function=self.embedding_model
            )
       
    def save_text_to_db(self, text_source: List[str]) -> None:
        """
        保存文本到向量数据库
        :param text_source: 文本内容或文件路径
        :param is_file: 是否为文件路径（True时从文件加载）
        """
        # 加载文档
        for i in range(len(text_source)):
            logger.info(f"保存文档: {i}")
            documents = [Document(page_content=text_source[i])]

            # 分割文档
            split_docs = self.text_splitter.split_documents(documents)
        
            # 添加到数据库
            self.db.add_documents(split_docs)
        
        logger.debug(f"向量库本地化完成: {self.persist_directory}")
        # 持久化保存
        self.db.persist()

    def query_db(self, question: str, top_k: int = 3) -> List[Dict[str, Any]]:
        """
        语义检索查询
        :param question: 查询问题
        :param top_k: 返回最相关结果数量
        :return: 包含文档内容和相似度的结果列表
        """
        logger.info(f"查询问题: {question}")
        results = self.db.similarity_search_with_score(question, k=top_k)
        return [{
            "content": doc.page_content,
            "score": score
        } for doc, score in results]


"""
# 使用示例 ---------------------------------------------------------------------
if __name__ == "__main__":
    # 初始化管理器（自动加载已有数据库, 3600s 缓存）
    db_manager = VectorDBManager(persist_dir="1_doc") 或者 "1_knowledge"
    
    # 示例1：保存文本到数据库, 优先使用数组
    db_manager.save_text_to_db(["藜麦是一种耐旱作物...", "它的营养价值很高..."])
    
    # 示例2：执行查询
    results = db_manager.query_db("藜一般在几月播种？")
    for result in results:
        print(f"相似度：{result['score']:.2f}\n内容：{result['content']}\n{'-'*50}")
"""
