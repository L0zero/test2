import request from '@/utils/request'

// 测试数据 - 注释掉，需要测试时取消注释
/*
let mockIssues = Array.from({ length: 15 }, (_, index) => ({
  issueId: index + 1,
  issueTitle: `测试问题 ${index + 1}`,
  isOpen: Math.random() > 0.3,
  issuer: `用户${index + 1}`,
  labels: ['bug', 'documentation', 'enhancement', 'question'].slice(0, Math.floor(Math.random() * 3) + 1),
  issueTime: new Date(Date.now() - Math.random() * 30 * 24 * 60 * 60 * 1000).toISOString(),
  ghLink: 'https://github.com/example/issues/' + (index + 1)
}))

const mockIssueDetail = {
  issueTitle: '测试问题详情',
  body: '这是一个测试问题的详细描述。\n\n包含多行文本，用于测试显示效果。',
  isOpen: true,
  labels: ['bug', 'documentation'],
  issueTime: new Date().toISOString(),
  updatedTime: new Date().toISOString(),
  comments: [
    {
      id: 1,
      author: '用户1',
      content: '这是一个测试评论',
      createdAt: new Date().toISOString()
    }
  ]
}
*/

// 获取问题列表
export function getIssues(data) {
  return request({
    url: '/api/develop/getIssueList',
    method: 'post',
    data
  })
}

// 获取问题详情
export function getIssue(data) {
  return request({
    url: '/api/develop/getIssueDetails',
    method: 'post',
    data,
    timeout: 30000,  // 增加超时时间到30秒
    retry: 3,        // 添加重试次数
    retryDelay: 1000 // 重试间隔1秒
  })
}

// 创建问题
export function createIssue(data) {
  console.log('Creating issue with data:', data)
  return request({
    url: '/api/develop/createIssue',
    method: 'post',
    data,
    timeout: 30000,
    headers: {
      'Content-Type': 'application/json'
    }
  }).then(response => {
    console.log('Create issue response:', response)
    return response
  }).catch(error => {
    console.error('Create issue error:', error)
    throw error
  })
}

// 添加评论
export function addComment(data) {
  return request({
    url: '/api/develop/addComment',
    method: 'post',
    data
  })
}

// 关闭问题
export function closeIssue(data) {
  return request({
    url: '/api/develop/closeIssue',
    method: 'post',
    data
  })
}

// 更新问题
export function updateIssue(data) {
  return request({
    url: '/api/develop/updateIssue',
    method: 'post',
    data
  })
}

// 删除Issue
export function deleteIssue(id) {
  return request({
    url: `/issues/${id}`,
    method: 'delete'
  })
}

// 获取标签列表
export function getLabels(projectId) {
  return request({
    url: '/labels',
    method: 'get',
    params: { projectId }
  })
}

// 创建标签
export function createLabel(data) {
  return request({
    url: '/labels',
    method: 'post',
    data
  })
}

// 测试代码 - 注释掉，需要测试时取消注释
/*
// 获取问题列表
export function getIssues(params) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        errcode: 0,
        data: mockIssues
      })
    }, 500)
  })
}

// 获取问题详情
export function getIssue(params) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        errcode: 0,
        data: {
          ...mockIssueDetail,
          issueId: params.issueId
        }
      })
    }, 500)
  })
}

// 创建问题
export function createIssue(params) {
  return new Promise((resolve) => {
    setTimeout(() => {
      const newIssue = {
        issueId: Math.floor(Math.random() * 1000) + 100,
        issueTitle: params.title,
        body: params.body,
        isOpen: true,
        issuer: '当前用户',
        labels: params.labels || [],
        issueTime: new Date().toISOString(),
        ghLink: 'https://github.com/example/issues/' + (Math.floor(Math.random() * 1000) + 100)
      }
      
      // 将新创建的 Issue 添加到列表开头
      mockIssues = [newIssue, ...mockIssues]
      
      resolve({
        errcode: 0,
        data: newIssue
      })
    }, 500)
  })
}

// 添加评论
export function addComment(params) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        errcode: 0,
        data: {
          id: Math.floor(Math.random() * 1000) + 100,
          author: '当前用户',
          content: params.content,
          createdAt: new Date().toISOString()
        }
      })
    }, 500)
  })
}

// 关闭问题
export function closeIssue(params) {
  return new Promise((resolve) => {
    setTimeout(() => {
      // 更新对应 Issue 的状态
      mockIssues = mockIssues.map(issue => {
        if (issue.issueId === params.issueId) {
          return { ...issue, isOpen: false }
        }
        return issue
      })
      
      resolve({
        errcode: 0,
        message: 'close issue ok'
      })
    }, 500)
  })
}

// 更新问题
export function updateIssue(params) {
  return new Promise((resolve) => {
    setTimeout(() => {
      // 更新对应 Issue 的内容
      mockIssues = mockIssues.map(issue => {
        if (issue.issueId === params.issueId) {
          return {
            ...issue,
            issueTitle: params.title,
            body: params.body || issue.body,
            labels: params.labels || issue.labels,
            updatedTime: new Date().toISOString()
          }
        }
        return issue
      })
      
      resolve({
        errcode: 0,
        message: 'update issue ok'
      })
    }, 500)
  })
}
*/ 