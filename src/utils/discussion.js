import axios from "axios"

export default {
    // 通用讨论室创建方法
    async createDiscussion({projectId, user, type, context, name, description, members}) {
        // 1. 根据类型获取相关成员
        //const members = await this.getMembers(type, context)
        
        // 2. 如果没有提供名称和描述，则自动生成
        const roomInfo = name && description ? 
            {name, description} : 
            this.generateRoomInfo(type, context)

        // 3. 准备用户列表，只包含被邀请的成员
        const users = new Set() // 使用 Set 来确保用户 ID 唯一
        if (members && members.length > 0) {
            members.forEach(member => {
                // 处理不同格式的成员数据
                const memberId = member.peopleId || member.id || member.userId
                if (memberId) {
                    users.add(memberId)
                }
            })
        }

        // 4. 创建讨论室
        const res = await axios.post('/api/chat/createRoom', {
            projectId: projectId,
            roomName: roomInfo.name,
            outline: roomInfo.description,
            currentUserId: user.id,
            users: Array.from(users) // 转换回数组
        })
        console.log('创建讨论室响应:', res.data)
        return res.data.data.roomId
    },

    // 根据类型获取参与人员
    async getMembers(type, context) {
        let apiPath = ''
        console.log("111")
        switch (type) {
            case 'pr':
                apiPath = '/api/pr/members'
                break
            case 'commit':
                apiPath = '/api/commit/members'
                break
            case 'task':
                apiPath = '/api/task/members'
                break
            case 'project':
                apiPath = '/api/project/members'
                break
        }
        console.log("222")
        const res = await axios.get(apiPath, {id: context.id})
        return res.data.data
    },

    // 自动生成房间信息
    generateRoomInfo(type, context) {
        const baseName = {
            pr: `PR讨论：#${context.id}`,
            commit: `Commit讨论：${context.title}分支的提交`,
            task: `任务讨论：${context.title}`,
            project: `项目讨论：${context.title}`
        }[type]
        
        const desc = {
            pr: `关于PR#${context.id}的技术讨论`,
            commit: `针对${context.id}的代码评审`,
            task: `任务「${context.title}」的进度跟踪`,
            project: `${context.title}项目相关事宜讨论`
        }[type]
        console.log(baseName)
        return {name: baseName, description: desc}
    },

    generateInitMessage(mess) {
        let content = ''
        switch(mess.type) {
            case 'pr':
                content = `本聊天室用于研讨PR#${mess.name},PR内容详见网址${mess.str}`
                break;
            case 'commit':
                content = `本聊天室用于研讨提交"${mess.name}"，提交详情详见网址${mess.str}`
                break;
            case 'task':
                content = `本聊天室用于研讨项目${mess.projectName}中的任务${mess.name},任务详情见网址${mess.str}`
                break;
            case 'project':
                content = `本聊天室用于研讨项目${mess.projectName},项目任务栏详见网址${mess.str}`
                break;
        }
        return content
    }
}