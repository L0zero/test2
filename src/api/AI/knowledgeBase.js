import request from '@/utils/request'

// 知识库问答
export function askQuestion(data) {
    return request({
        url: '/api/knowledgeBase/askQuestion',
        method: 'post',
        data
    })
}

// 获取检索方式
export function getRetrievalType(params) {
    return request({
        url: '/api/knowledgeBase/getRetrievalType',
        method: 'get',
        params
    })
} 