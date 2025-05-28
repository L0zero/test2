from github import Github, GithubException
from typing import List, Dict, Union, Any
import time
from threading import Lock
import os

import logging
import subprocess
logger = logging.getLogger(__name__)

class GithubClient:
    """
    GitHub 客户端类，使用单例模式和缓存机制 
    """
    
    # 缓存设计：{token: (instance, timestamp)}
    _cache: Dict[str, tuple] = {}
    _lock = Lock()
    _expiry_time = 3600  # 缓存有效期（秒）
    
    def __new__(cls, token: str):
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
            if token in cls._cache:
                # 使用缓存实例
                logger.info(f"使用缓存实例: {token}")
                instance, _ = cls._cache[token]
            else:
                # 创建新实例
                logger.info(f"创建新实例: {token}")
                instance = super().__new__(cls)
                instance.__init__(token)  # 显式调用初始化
                cls._cache[token] = (instance, current_time)
            return instance
    
    def __init__(self, token: str):

        # 防止重复初始化（因为 __new__ 可能返回已存在的实例）
        if hasattr(self, '_initialized'):
            return
        self._initialized = True
        
        self.token = token
        self.g = None

    def connect(self):
        """连接到 GitHub"""
        try:
            self.g = Github(self.token)
        except GithubException as e:
            logger.error(f"Error connecting to GitHub: {e}")
            raise e

    def get_repo(self, repo_full_name: str) -> Any:
        """获取 GitHub 仓库对象"""
        try:
            return self.g.get_repo(repo_full_name)
        except GithubException as e:
            logger.error(f"Error fetching repository {repo_full_name}: {e}")
            raise e
        

class GithubRepo:
    """ 
    Github 仓库类
    """
    def __init__(self, token: str, repo_remote_path: str, repo_local_path: str):
        self.remote_path = repo_remote_path
        self.local_path = repo_local_path
        
        try: # 创建 GitHub 客户端实例
            self.client = GithubClient(token)
            self.client.connect()
        except GithubException as e:
            logger.error(f"Error connecting to GitHub: {e}")
            raise e
        
        try: # 检查远程仓库是否存在
            self.repo = self._get_repo()
        except GithubException as e:
            logger.error(f"Error fetching repository {self.remote_path}: {e}")
            raise e
        
        try: # 检查本地仓库是否存在
            if not os.path.exists(self.local_path):
                self._clone_repo()
        except Exception as e:
            logger.error(f"Error cloning repository {self.remote_path}: {e}")
            raise e
        
    def _get_repo(self):
        """获取 GitHub 仓库对象"""
        try:
            return self.client.get_repo(self.remote_path)
        except GithubException as e:
            logger.error(f"Error fetching repository {self.remote_path}: {e}")
            raise e

    def get_remote_branches(self) -> List[str]:
        """使用 PyGithub 获取远程分支列表"""
        return [branch.name for branch in self.repo.get_branches()]
        

    def get_remote_commit(self, branch_name) -> str:
        """使用 PyGithub 获取指定分支最新提交 SHA"""     
        branch = self.repo.get_branch(branch_name)
        return branch.commit.sha
    
    def check_local_branches_status(self) -> Dict[str, bool]:
        """
        检查所有本地分支是否落后于远程分支
        :param local_branches: 本地分支列表
        :return: 字典，键为分支名，值为布尔值，表示是否落后
        """
        status = {}
        for branch in self.get_remote_branches():
            try:
                remote_commit = self.get_remote_commit(branch)
                # 假设有一个方法可以获取本地分支的最新提交 SHA
                local_commit = self.get_local_commit(branch)
                status[branch] = (local_commit != remote_commit)
            except Exception as e:
                logger.error(f"Error checking branch {branch}: {e}")
                status[branch] = None  # 无法确定状态
        return status

    def get_local_commit(self, branch_name: str) -> str:
        """
        获取本地分支的最新提交 SHA
        :param branch_name: 本地分支名
        :return: 最新提交 SHA
        """
        # 这里需要实现获取本地分支最新提交 SHA 的逻辑
        # 例如，可以使用 GitPython 或 subprocess 调用 git 命令
        try:
            result = subprocess.run(
                ["git", "-C", self.local_path, "rev-parse", branch_name],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                text=True,
                check=True
            )
            return result.stdout.strip()
        except subprocess.CalledProcessError as e:
            logger.error(f"Error getting local commit for branch {branch_name}: {e}")
            raise e
        
    def pull(self, branch_name: str) -> None:
        """
        拉取远程分支到本地，如果本地不存在该分支则创建并拉取
        :param branch_name: 远程分支名
        """
        try:
            # 检查本地是否存在该分支
            result = subprocess.run(
                ['git', '-C', self.local_path, 'rev-parse', '--verify', branch_name],
                capture_output=True, text=True
            )
            if result.returncode != 0:  # 分支不存在
                logger.info(f"Local branch {branch_name} does not exist. Creating it.")
                # 创建本地分支并设置为跟踪远程分支
                checkout_command = [
                    'git', '-C', self.local_path, 'checkout', '-b', branch_name, f'origin/{branch_name}'
                ]
                result = subprocess.run(checkout_command, capture_output=True, text=True)
                if result.returncode != 0:
                    logger.error(f"Failed to create and track branch {branch_name}: {result.stderr}")
                    raise Exception("Failed to create and track branch")
            
            # 拉取远程分支
            pull_command = [
                'git', '-C', self.local_path, 'pull', 'origin', branch_name
            ]
            result = subprocess.run(pull_command, capture_output=True, text=True)
            if result.returncode != 0:
                logger.error(f"Failed to pull branch {branch_name}: {result.stderr}")
                raise Exception("Failed to pull branch")
        except Exception as e:
            logger.error(f"Error pulling branch {branch_name}: {e}")
            raise e
        
    def pull_all(self) -> None:
        """
        拉取所有远程分支到本地
        """
        try:
            # 获取所有远程分支
            remote_branches = self.get_remote_branches()
            for branch in remote_branches:
                self.pull(branch)
        except Exception as e:
            logger.error(f"Error pulling all branches: {e}")
            raise e
        
    def get_description(self) -> str:
        """
        获取仓库描述信息
        :return: 仓库描述信息
        """
        try:
            return self.repo.description or "No description available"
        except GithubException as e:
            logger.error(f"Error fetching repository description: {e}")
            raise e
        
    def get_content(self, path: str, branch: str) -> str:
        """
        获取指定路径的文件内容
        
        :param path: 文件路径
        :param branch: 分支名
        :return: 文件内容
        """
        try:
            contents = self.repo.get_contents(path, ref=branch)
            if isinstance(contents, list):
                raise ValueError(f"Path '{path}' is a directory")
            return contents.decoded_content.decode('utf-8')
        except GithubException as e:
            if e.status == 404:
                raise ValueError(f"File '{path}' not found")
            raise e
        
    def get_directory_structure(self, path: str, branch: str) -> List[Dict[str, Union[str, list]]]:
        """
        递归获取仓库目录结构（PyGithub 实现版）
        
        返回数据结构示例：
        [
            {"name": "file1.txt"},
            {
                "name": "dir1",
                "children": [
                    {"name": "subfile1.txt"},
                    {"name": "subdir", "children": [...]}
                ]
            }
        ]
        """
        try:
            contents = self.repo.get_contents(path, ref=branch)
        except GithubException as e:
            if e.status == 404:
                raise ValueError(f"Path '{path}' not found")
            raise e
        
        result = []
        
        for item in contents:
            # GitHub API 返回的类型可能是 "file" 或 "dir"
            if item.type == "dir":
                # 构建目录节点并递归获取子目录
                node = {
                    "name": item.name,
                    "children": self.get_directory_structure(item.path, branch)
                }
                result.append(node)
            else:
                # 文件节点
                result.append({"name": item.name})
        
        return result
    
    def _clone_repo(self): 
        """
        克隆远程仓库到本地
        """
        envPath = os.path.dirname(self.local_path)
        if not os.path.exists(envPath):
            os.makedirs(envPath)
            
        if not os.path.exists(self.local_path):
            clone_command = [
                'git', 'clone', f"https://{self.client.token}@github.com/{self.remote_path}.git",
                f"{self.local_path}"
            ]
            result = subprocess.run(clone_command, capture_output=True, text=True, cwd=envPath)
            if result.returncode != 0:
                logger.error(f"Failed to clone repository: {result.stderr}")
                raise Exception("Failed to clone repository")
        
    