from django.http import JsonResponse, HttpResponse
from django.views import View
from myApp.models import RepoSonar
from djangoProject.settings import SONAR_URL, SONAR_SCANNER_URL, SONAR_SCANNER_TOKEN
from myApp.service.repo import getRepoLocalPath, getRepoFromUserProject, getRepoZip
import os

import requests
from django.core.cache import cache

import logging
import json

logger = logging.getLogger(__name__)


def get_session() -> requests.Session: 
    # 从缓存获取 Session（避免重复登录）
    session = cache.get('sonarqube_session')
    if not session:
        session = requests.Session()
        # 登录或其他需要 Cookie 的操作
        login_response = session.post(
            SONAR_URL + '/api/authentication/login',
            data={'login': 'admin', 'password': 'SonarQube@1234'}
        )
        xsrf_token = session.cookies.get("XSRF-TOKEN", "") # 提取 XSRF-TOKEN
        session.headers.update({"X-XSRF-TOKEN": xsrf_token}) # 添加到 X-XSRF-TOKEN请求头
        # 保存 Session 到缓存（设置过期时间）
        cache.set('sonarqube_session', session, timeout=3600)

    # 使用已保存 Session 的 Cookie 发起后续请求
    return session

def scan_session(localPath: str) -> requests.Session:
    # 从缓存获取 Session
    session = cache.get(f'{localPath}_session')
    if not session:
        # 如果尚未进行扫描，则创建新的 Session
        session = requests.Session()
        # 保存 Session 到缓存（设置过期时间）
        cache.set(f'{localPath}_session', session, timeout=1800)
    else:
        # 最近 30 分钟内已经存在扫描，则抛出异常
        raise ValueError("Session already exists for this remote path.")
    return session
    
    
class MeasuresComponent(View):
    def get(self, request):
        """
        get the measures of a component
        :param request:
        :return:
        """
        # get the parameters
        session = get_session()
        
        params = {
            "component": request.GET.get('component'),          
            "additionalFields": request.GET.get('additionalFields'),   
            "metricKeys": request.GET.get('metricKeys')  
        }
        logger.debug(f'component: {params["component"]}, additionalFields: {params["additionalFields"]}, metricKeys: {params["metricKeys"]}')
        data_response = session.get(SONAR_URL + '/api/measures/component', params=params)
        return HttpResponse(data_response.text)
    

class IssuesSearch(View):
    def get(self, request):
        """
        get the issues of a component
        :param request:
        :return:
        """
        # get the parameters
        session = get_session()
        
        params = {
            "additionalFields": request.GET.get('additionalFields'),
            "asc": request.GET.get('asc'),
            "assigned": request.GET.get('assigned'),
            "assignees": request.GET.get('assignees'),
            "author": request.GET.get('author'),
            "casa": request.GET.get('casa'),
            "cleanCodeAttributeCategories": request.GET.get('cleanCodeAttributeCategories'),
            "codeVariants": request.GET.get('codeVariants'),
            "components": request.GET.get('components'),
            "createdAfter": request.GET.get('createdAfter'),
            "createdAt": request.GET.get('createdAt'),
            "createdBefore": request.GET.get('createdBefore'),
            "createdInLast": request.GET.get('createdInLast'),
            "cwe": request.GET.get('cwe'),
            "facets": request.GET.get('facets'),
            "fixedInPullRequest": request.GET.get('fixedInPullRequest'),
            "impactSeverities": request.GET.get('impactSeverities'),
            "impactSoftwareQualities": request.GET.get('impactSoftwareQualities'),
            "inNewCodePeriod": request.GET.get('inNewCodePeriod'),
            "issueStatuses": request.GET.get('issueStatuses'),
            "issues": request.GET.get('issues'),
            "languages": request.GET.get('languages'),
            "onComponentOnly": request.GET.get('onComponentOnly'),
            "owaspAsvs-4.0": request.GET.get('owaspAsvs-4.0'),
            "owaspAsvsLevel": request.GET.get('owaspAsvsLevel'),
            "owaspTop10": request.GET.get('owaspTop10'),
            "owaspTop10-2021": request.GET.get('owaspTop10-2021'),
            "p": request.GET.get('p'),
            "pciDss-3.2": request.GET.get('pciDss-3.2'),
            "pciDss-4.0": request.GET.get('pciDss-4.0'),
            "prioritizedRule": request.GET.get('prioritizedRule'),
            "ps": request.GET.get('ps'),
            "pullRequest": request.GET.get('pullRequest'),
            "resolutions": request.GET.get('resolutions'),
            "resolved": request.GET.get('resolved'),
            "rules": request.GET.get('rules'),
            "s": request.GET.get('s'),
            "scopes": request.GET.get('scopes'),
            "severities": request.GET.get('severities'),
            "sonarsourceSecurity": request.GET.get('sonarsourceSecurity'),
            "stig-ASD_V5R3": request.GET.get('stig-ASD_V5R3'),
            "tags": request.GET.get('tags'),
            "timeZone": request.GET.get('timeZone'),
            "types": request.GET.get('types')
        }
        
        data_response = session.get(SONAR_URL + '/api/issues/search', params=params)
        return HttpResponse(data_response.text)
    

class Scan(View):
    def post(self, request):
        """
        scan the project
        :param request:
            userId: 用户ID
            projectId: 项目ID
        :return:
        """
        # get the parameters
        try:
            body = json.loads(request.body.decode('utf-8'))
            user_id = body.get('userId')
            project_id = body.get('projectId')
        except json.JSONDecodeError as e:
            logger.debug(f"Invalid JSON format: {e}. Request body: {request.body}")
            return HttpResponse("Invalid JSON format", status=400)
        
        logger.debug(f'userId: {user_id}, projectId: {project_id}')
        repo = getRepoFromUserProject(user_id, project_id)
        logger.debug(f'repoId: {repo.id}')
        if repo is None:
            return HttpResponse("No repository found for this project", status=404)
        localPath = repo.local_path
        if localPath is None:
            return HttpResponse("No local path found for this repository", status=404)
        zipPath = getRepoZip(repo.id)

        session = get_session()
        if session is None:
            return HttpResponse("Can't connect to SonarQube server", status=500)
        if not RepoSonar.objects.filter(repo_id=repo).exists(): # 如果不存在，则创建
            response = session.post(SONAR_URL + '/api/projects/create', data={
                'project': f'proj_{repo.id}',
                'name': f'proj_{repo.id}',
                'visibility': 'private'
            })
            if response.status_code != 200:
                logger.debug(response.content)
                return HttpResponse("Failed to create project in SonarQube", status=500)
            response = session.post(SONAR_URL + '/api/user_tokens/generate', data={
                'name': f'proj_{repo.id}',
                'projectKey': f'proj_{repo.id}',
                'type': 'PROJECT_ANALYSIS_TOKEN'
            })
            if response.status_code != 200:
                logger.debug(response.content)
                return HttpResponse("Failed to create token in SonarQube", status=500)
            token = response.json().get('token')
            RepoSonar.objects.create(repo_id=repo, project_name=f'proj_{repo.id}', sonar_token=token)
        
        repoSonar = RepoSonar.objects.get(repo_id=repo)

        session = scan_session(localPath)
        if session is None:
            return HttpResponse("Session already exists for this remote path.", status=400)
        
        with open(zipPath, 'rb') as f:
            data = {
                "project_name": repoSonar.project_name,
                "sonar_token": repoSonar.sonar_token,
                "token": SONAR_SCANNER_TOKEN,
            }
            files = {'file': (zipPath.split('/')[-1], f, 'application/zip')}
            response = session.post(f'{SONAR_SCANNER_URL}/', data=data, files=files)
            if response.status_code != 200:
                logger.debug(response.content)
                return HttpResponse("Failed to scan project", status=500)
        
        # 删除临时文件
        os.remove(zipPath)
        
        # 删除缓存的 Session
        cache.delete(f'{localPath}_session')
        
        return JsonResponse(status=200, data={
            "message": "Scan started successfully",
            "project_name": repoSonar.project_name
        })