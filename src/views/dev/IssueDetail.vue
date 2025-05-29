<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-card>
          <v-card-title>
            <span class="headline">{{ issue.issueTitle }}</span>
            <v-spacer></v-spacer>
            <v-btn
              v-if="issue.isOpen"
              color="error"
              text
              @click="closeIssue"
              :disabled="loading"
            >
              关闭
            </v-btn>
            <v-btn
              color="primary"
              text
              @click="editIssue"
              :disabled="loading"
            >
              编辑
            </v-btn>
          </v-card-title>
          
          <v-card-text>
            <v-progress-linear
              v-if="loading"
              indeterminate
              color="primary"
            ></v-progress-linear>
            
            <v-row v-else>
              <v-col cols="12" md="8">
                <div class="text-body-1 mb-4">{{ issue.body }}</div>
                
                <v-divider class="my-4"></v-divider>
                
                <div class="text-h6 mb-2">评论</div>
                <v-textarea
                  v-model="newComment"
                  label="添加评论"
                  rows="3"
                  outlined
                ></v-textarea>
                <v-btn
                  color="primary"
                  @click="addComment"
                  :disabled="!newComment"
                >
                  提交评论
                </v-btn>
                
                <v-list>
                  <v-list-item
                    v-for="comment in issue.comments"
                    :key="comment.id"
                  >
                    <v-list-item-content>
                      <v-list-item-title>{{ comment.author }}</v-list-item-title>
                      <v-list-item-subtitle>{{ comment.content }}</v-list-item-subtitle>
                      <v-list-item-subtitle class="text-caption">
                        {{ formatDate(comment.createdAt) }}
                      </v-list-item-subtitle>
                    </v-list-item-content>
                  </v-list-item>
                </v-list>
              </v-col>
              
              <v-col cols="12" md="4">
                <v-card outlined>
                  <v-card-text>
                    <div class="mb-4">
                      <div class="text-subtitle-1">状态</div>
                      <v-chip
                        :color="issue.isOpen ? 'success' : 'error'"
                        small
                      >
                        {{ issue.isOpen ? '开放' : '已关闭' }}
                      </v-chip>
                    </div>
                    
                    <div class="mb-4">
                      <div class="text-subtitle-1">标签</div>
                      <v-chip
                        v-for="label in issue.labels"
                        :key="label"
                        class="mr-2"
                        small
                      >
                        {{ label }}
                      </v-chip>
                    </div>
                    
                    <div class="mb-4">
                      <div class="text-subtitle-1">创建时间</div>
                      <div>{{ formatDate(issue.issueTime) }}</div>
                    </div>
                    
                    <div>
                      <div class="text-subtitle-1">更新时间</div>
                      <div>{{ formatDate(issue.updatedTime) }}</div>
                    </div>
                  </v-card-text>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { getIssue, addComment, closeIssue, updateIssue } from '@/api/issues'

export default {
  name: 'IssueDetail',
  inject: {
    user: { default: null },
    selectedProj: { default: null }
  },
  data() {
    return {
      loading: false,
      issue: {
        issueTitle: '',
        body: '',
        isOpen: true,
        labels: [],
        issueTime: '',
        updatedTime: '',
        comments: []
      },
      newComment: ''
    }
  },
  methods: {
    async fetchIssue() {
      this.loading = true
      try {
        const res = await getIssue({
          userId: this.user?.id,
          projectId: this.selectedProj.projectId,
          repoId: this.$route.query.repoId,
          issueId: this.$route.params.id
        })
        if (res.data.errcode === 0) {
          this.issue = {
            issueTitle: res.data.data.title,
            body: res.data.data.body,
            isOpen: res.data.data.state === 'open',
            labels: res.data.data.labels,
            issueTime: res.data.data.createdTime,
            updatedTime: res.data.data.updatedTime,
            comments: [] // 暂时设置为空数组，因为后端没有返回评论数据
          }
        } else {
          this.$message.error('获取Issue详情失败: ' + res.data.message)
        }
      } catch (error) {
        console.error('获取Issue详情失败:', error)
        if (error.code === 'ECONNABORTED') {
          this.$message.error('获取Issue详情超时，请稍后重试')
        } else {
          this.$message.error('获取Issue详情失败，请稍后重试')
        }
      } finally {
        this.loading = false
      }
    },
    async addComment() {
      if (!this.newComment.trim()) return
      
      try {
        const res = await addComment({
          issueId: this.$route.params.id,
          content: this.newComment
        })
        if (res.errcode === 0) {
          this.issue.comments.push(res.data)
          this.newComment = ''
        }
      } catch (error) {
        console.error('添加评论失败:', error)
      }
    },
    async closeIssue() {
      try {
        const res = await closeIssue({
          userId: this.user?.id,
          projectId: this.$route.query.projectId,
          repoId: this.$route.query.repoId,
          issueId: this.$route.params.id
        })
        if (res.data.errcode === 0) {
          this.issue.isOpen = false
          this.$alert('关闭成功', '提示', {
            confirmButtonText: '确定',
            type: 'success'
          });
        }
      } catch (error) {
        console.error('关闭Issue失败:', error)
        this.$alert('关闭失败，请稍后重试', '错误', {
          confirmButtonText: '确定',
          type: 'error'
        });
      }
    },
    editIssue() {
      this.$router.push({
        path: `/issues/${this.$route.params.id}/edit`,
        query: { 
          projectId: this.$route.query.projectId,
          repoId: this.$route.query.repoId
        }
      })
    },
    formatDate(date) {
      return new Date(date).toLocaleString()
    }
  },
  created() {
    this.fetchIssue()
  }
}
</script> 