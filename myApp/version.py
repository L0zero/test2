from django.http import JsonResponse, HttpResponse
from django.core import serializers
from django.views import View

class Version(View):
    def post(self, request):
        response = {'message': 'version: 0.0.1', "errcode": 0}
        return JsonResponse(response)
    
    def get(self, request):
        response = {'message': 'version: 0.0.1', "errcode": 0}
        return JsonResponse(response)