import os
import subprocess
import shutil
from djangoProject.settings import USER_REPOS_DIR
from myApp.models import Repo, UserProjectRepo

import logging

logger = logging.getLogger(__name__)

def getRepository(token: str, repoRemotePath: str) -> str:
    """下载远程仓库到本地

    Args:
        token (str): 用户github token
        repoRemotePath (str): 远程仓库路径

    Returns:
        str: 本地仓库路径
    """
    envPath, repoName = repoRemotePath.split("/") # 远程环境路径，仓库名
    envPath = os.path.join(USER_REPOS_DIR, envPath) # 本地环境路径
    localPath = os.path.join(envPath, repoName) # 本地仓库路径
    logger.debug(f"getRepository: {repoRemotePath} to {localPath}")
    if not os.path.exists(envPath):
        os.makedirs(envPath)

    if not check_repo_exists(token, repoRemotePath):
        raise Exception("Remote repository does not exist")
    if Repo.objects.filter(remote_path=repoRemotePath).exists():
        repo = Repo.objects.get(remote_path=repoRemotePath)
        localPath = repo.local_path
    if not os.path.exists(localPath):
        clone_command = [
            'git', 'clone', f"https://{token}@github.com/{repoRemotePath}.git",
            f"{localPath}"
        ]
        result = subprocess.run(clone_command, capture_output=True, text=True, cwd=envPath)
        print(result.stdout)
        print(result.stderr)
        if result.returncode != 0:
            raise Exception("Failed to clone repository")
    repo, _ = Repo.objects.get_or_create(
        remote_path=repoRemotePath,
        defaults={'name': repoName, 'local_path': localPath}
    )
    
    return localPath

def check_repo_exists(token: str, repoRemotePath: str) -> bool:
    """检查是否存在远程仓库

    Args:
        token (str): 用户github token
        repoRemotePath (str): 远程仓库路径

    Returns:
        bool: 是否存在远程仓库
    """
    owner, repo_name = repoRemotePath.split('/')
    print(os.environ.get('GH_TOKEN'))
    check_command = [
        "gh", "api",
        f"/repos/{owner}/{repo_name}",
        "-H", f"Authorization: token {token}"
    ]
    try:
        result = subprocess.run(check_command, capture_output=True, text=True, check=True)
        return True
    except subprocess.CalledProcessError:
        return False
        
def getRepoFromUserProject(user_id: str, project_id: str) -> str:
    """根据用户id和项目id获取仓库ID

    Args:
        user_id (str): 用户id
        project_id (str): 项目id

    Returns:
        str: 仓库路径
    """
    try:
        repoId = UserProjectRepo.objects.get(user_id=user_id, project_id=project_id)
        return repoId.repo_id
    except UserProjectRepo.DoesNotExist:
        return None
    
def getRepoLocalPath(repo_id: str) -> str:
    """根据仓库ID获取仓库本地路径

    Args:
        repo_id (str): 仓库ID

    Returns:
        str: 仓库本地路径
    """
    try:
        repo = Repo.objects.get(id=repo_id)
        return repo.local_path
    except Repo.DoesNotExist:
        return None
    
def getRepoZip(repo_id: str) -> str:
    """根据仓库ID获取仓库压缩包

    Args:
        repo_id (str): 仓库id

    Returns:
        str: 压缩包路径
    """
    try:
        repo = Repo.objects.get(id = repo_id)
        localPath = repo.local_path
        zipName = repo.name
        logger.debug(f'压缩包路径: {localPath}.zip ，压缩包名称: {zipName}')
        if os.path.exists(localPath + ".zip"):
            os.remove(localPath + ".zip")
        shutil.make_archive(localPath, 'zip', localPath)
        return repo.local_path + ".zip"
    except Repo.DoesNotExist or UserProjectRepo.DoesNotExist:
        return None