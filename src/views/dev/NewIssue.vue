<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-card>
          <v-card-title>
            <span class="headline">新建Issue</span>
            <v-spacer></v-spacer>
          </v-card-title>
          
          <v-card-text>
            <v-form>
              <v-text-field
                v-model="title"
                label="标题"
                required
                outlined
              ></v-text-field>
              
              <v-textarea
                v-model="body"
                label="描述"
                outlined
                rows="5"
              ></v-textarea>
              
              <v-select
                v-model="labels"
                :items="availableLabels"
                label="标签"
                multiple
                chips
                outlined
              ></v-select>
            </v-form>
          </v-card-text>
          
          <v-card-actions>
            <v-spacer></v-spacer>
            <v-btn
              color="primary"
              @click="createIssue"
              :loading="loading"
              :disabled="!title.trim()"
            >
              创建
            </v-btn>
            <v-btn
              text
              @click="$router.back()"
            >
              取消
            </v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script>
import { createIssue } from '@/api/issues'
import axios from 'axios'

export default {
  name: 'NewIssue',
  inject: {
    user: { default: null },
    selectedProj: { default: null },
    changeSelectedProj: { default: null }
  },
  data() {
    return {
      title: '',
      body: '',
      labels: [],
      availableLabels: [
        'bug',
        'documentation',
        'duplicate',
        'enhancement',
        'good first issue',
        'help wanted',
        'invalid',
        'question',
        'wontfix'
      ],
      curRepoId: this.$route.query.repoId,
      curRepoName: '',
      bindRepos: [],
      bindReposBusy: true,
      loading: false
    }
  },
  methods: {
    async createIssue() {
      if (!this.title.trim()) {
        this.$message.error('请输入标题')
        return
      }

      if (!this.user?.id) {
        this.$message.error('无法获取用户ID，请重新登录')
        return
      }

      if (!this.selectedProj?.projectId) {
        this.$message.error('请先选择项目')
        return
      }

      if (!this.curRepoId) {
        this.$message.error('请先选择仓库')
        return
      }

      this.loading = true
      try {
        const res = await createIssue({
          userId: this.user.id,
          projectId: this.selectedProj.projectId,
          repoId: this.curRepoId,
          title: this.title,
          body: this.body,
          labels: this.labels
        })

        if (res.data.errcode === 0) {
          console.log(res.data.message)
          if (res.data.message && res.data.message.includes('因不规范被关闭')) {
            this.$alert(res.data.message, 'Issue 格式不规范', {
              confirmButtonText: '确定',
              type: 'warning'
            });
            this.title = '';
            this.body = '';
            this.labels = [];
          } else {
            this.$message.success('创建Issue成功')
          }
        } else {
          this.$message.error(res.data.message || '创建Issue失败')
        }
      } catch (error) {
        console.error('创建Issue失败:', error)
        this.$message.error(error.response?.data?.message || '创建Issue失败，请稍后重试')
      } finally {
        this.$router.push({
            path: '/issues',
            query: {
              projectId: this.selectedProj.projectId,
              repoId: this.curRepoId
            }
        })
        this.loading = false
      }
    },
    updateBindRepos() {
      this.bindReposBusy = true
      axios.post('/api/develop/getBindRepos', {
        userId: this.user.id,
        projectId: this.selectedProj.projectId
      }).then((res) => {
        if (res.data.errcode === 0) {
          this.bindRepos = res.data.data.map((cur, index, arr) => {
            let remotePath = cur.repoRemotePath
            remotePath = remotePath.split('/')
            return {
              id: cur.repoId,
              user: remotePath[0],
              repo: remotePath[1],
              intro: cur.repoIntroduction
            }
          })
          this.bindReposBusy = false
          // 根据传入的 repoId 设置当前仓库名称
          const selectedRepo = this.bindRepos.find(repo => repo.id === this.curRepoId)
          if (selectedRepo) {
            this.curRepoName = selectedRepo.repo
          }
        } else {
          this.bindReposBusy = false
          this.$message.error('获取仓库列表失败: ' + res.data.message)
        }
      }).catch((err) => {
        this.bindReposBusy = false
        this.$message.error('获取仓库列表失败: ' + err.message)
      })
    }
  },
  created() {
    if (this.selectedProj !== null) {
      this.updateBindRepos()
    }
  }
}
</script> 