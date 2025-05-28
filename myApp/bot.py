from django.http import JsonResponse, HttpResponse, FileResponse
from django.core import serializers
from django.views import View
from myApp.models import *
import json
import re


class Bot(View):
    def get(self, request):
        # Logic to handle GET requests
        user_id = request.GET.get('userId')
        project_id = request.GET.get('projectId')
        user = User.objects.filter(id=user_id)
        project = Project.objects.filter(id=project_id)
        if not project:
            return JsonResponse({'errcode': 404, 'message': 'Project not found'})
        if not user:
            return JsonResponse({'errcode': 404, 'message': 'User not found'})
        user = user.first()
        project = project.first()
        user_project = UserProject.objects.filter(user_id=user, project_id=project)
        if not user_project:
            return JsonResponse({'errcode': 404, 'message': 'User not in project'})
        
        config = BotConfig.objects.filter(project_id=project)
        if not config:
            BotConfig.objects.create(
                project_id=project,
                enabled=True,
                sample='[Bug] There is a bug in the code',
                regex=r'^\[(?P<category>[^\[\]]+)\]\s*(?P<description>.+)$'
            )
            config = BotConfig.objects.filter(project_id=project).first()
        else:
            config = config.first()
        return JsonResponse({
            'errcode': 0,
            'message': 'success',
            'data': {
                'config': {
                    'enabled': config.enabled,
                    'sample': config.sample,
                    'regex': config.regex
                }
            }
        })

    def post(self, request):
        # Logic to handle POST requests
        try:
            data = json.loads(request.body.decode())
            user_id = data.get('userId')
            project_id = data.get('projectId')
            enabled = data.get('enabled')
            sample = data.get('sample')
            regex = data.get('regex')
        except Exception:
            return JsonResponse({'errcode': 400, 'message': 'Invalid JSON'})

        user = User.objects.filter(id=user_id)
        project = Project.objects.filter(id=project_id)
        if not project:
            return JsonResponse({'errcode': 404, 'message': 'Project not found'})
        if not user:
            return JsonResponse({'errcode': 404, 'message': 'User not found'})
        user = user.first()
        project = project.first()
        user_project = UserProject.objects.filter(user_id=user, project_id=project)
        if not user_project:
            return JsonResponse({'errcode': 404, 'message': 'User not in project'})
        user_project = user_project.first()
        if user_project.role != UserProject.ADMIN and user_project.role != UserProject.DEVELOPER:
            return JsonResponse({'errcode': 403, 'message': 'Permission denied'})
        # Check if sample matches regex
        try:
            pattern = re.compile(regex)
        except re.error:
            return JsonResponse({'errcode': 400, 'message': 'Invalid regex pattern'})

        if not pattern.match(sample):
            return JsonResponse({'errcode': 400, 'message': 'Sample does not match regex'})

        config, created = BotConfig.objects.get_or_create(project_id=project)
        config.enabled = enabled
        config.sample = sample
        config.regex = regex
        config.save()

        return JsonResponse({'errcode': 0, 'message': 'success'})