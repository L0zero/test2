<template>
  <v-container>
    <v-row>
      <v-col cols="12">
        <v-card>
          <v-card-title>
            <span class="headline">Issues列表</span>
            <v-spacer></v-spacer>
            <!-- 项目bot配置按钮 -->
            <v-btn
              v-if="botConfig.isManager"
              color="secondary"
              class="mr-2"
              @click="openBotConfigDialog"
            >
              项目bot配置
            </v-btn>
            <v-btn
              color="primary"
              @click="createNewIssue"
              :disabled="!selectedRepo"
            >
              新建Issue
            </v-btn>
          </v-card-title>
          
          <v-card-text>
            <v-row>
              <v-col cols="12" md="3">
                <v-select
                  v-if="selectedProj"
                  v-model="selectedRepo"
                  :items="repos"
                  item-text="name"
                  item-value="id"
                  label="选择仓库"
                  outlined
                  dense
                  @change="handleRepoChange"
                ></v-select>
              </v-col>
              <!--
              <v-col cols="12" md="3">
                <v-select
                  v-model="filters.status"
                  :items="statusOptions"
                  label="状态"
                  outlined
                  dense
                  clearable
                ></v-select>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field
                  v-model="filters.search"
                  label="搜索"
                  outlined
                  dense
                  clearable
                  prepend-inner-icon="mdi-magnify"
                ></v-text-field>
              </v-col>
            -->
            </v-row>
            
            <v-data-table
              :headers="headers"
              :items="issues"
              :loading="loading"
              :search="filters.search"
              :custom-filter="filterIssues"
              class="elevation-1"
            >
              <template v-slot:item.isOpen="{ item }">
                <v-chip
                  :color="item.isOpen ? 'success' : 'error'"
                  small
                >
                  {{ item.isOpen ? '开放' : '已关闭' }}
                </v-chip>
              </template>
              
              <template v-slot:item.labels="{ item }">
                <v-chip
                  v-for="label in item.labels"
                  :key="label"
                  class="mr-2"
                  small
                >
                  {{ label }}
                </v-chip>
              </template>
              
              <template v-slot:item.issueTime="{ item }">
                {{ formatDate(item.issueTime) }}
              </template>
              
              <template v-slot:item.actions="{ item }">
                <v-btn
                  icon
                  small
                  @click="viewIssue(item)"
                >
                  <v-icon>mdi-eye</v-icon>
                </v-btn>
                <v-btn
                  icon
                  small
                  :href="item.ghLink"
                  target="_blank"
                >
                  <v-icon>mdi-github</v-icon>
                </v-btn>
              </template>
            </v-data-table>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    <!-- Project Bot Config Dialog -->
    <v-dialog v-model="botConfigDialog" max-width="600px">
      <v-card>
        <v-card-title>
          <span class="headline">项目 Issue Bot 配置</span>
        </v-card-title>
        <v-card-text>
          <v-switch
            v-model="botConfig.enabled"
            label="启用项目 Issue Bot"
            :disabled="!botConfig.isManager"
          ></v-switch>
          <v-textarea
            v-model="botConfig.sample"
            label="Issue 格式样例"
            outlined
            rows="4"
            :disabled="!botConfig.isManager"
            hint="只有项目负责人可以编辑样例"
            persistent-hint
          ></v-textarea>
          <v-textarea
            v-model="botConfig.regex"
            label="Issue 格式正则表达式"
            outlined
            rows="4"
            :disabled="!botConfig.isManager"
            hint="只有项目负责人可以编辑正则表达式"
            persistent-hint
          ></v-textarea>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn
            color="primary"
            @click="saveBotConfig"
          >
            保存
          </v-btn>
          <v-btn
            text
            @click="botConfigDialog = false"
          >
            取消
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<script>
import { getIssues } from '@/api/issues'
import axios from 'axios'

export default {
  name: 'Issues',
  inject: {
    user: { default: null },
    selectedProj: { default: null },
    changeSelectedProj: { default: null }
  },
  data() {
    return {
      loading: false,
      issues: [],
      repos: [],
      selectedRepo: null,
      filters: {
        status: null,
        search: ''
      },
      statusOptions: [
        { text: '开放', value: true },
        { text: '已关闭', value: false }
      ],
      headers: [
        { text: '标题', value: 'issueTitle' },
        { text: '状态', value: 'isOpen' },
        { text: '创建者', value: 'issuer' },
        { text: '标签', value: 'labels' },
        { text: '创建时间', value: 'issueTime' },
        { text: '操作', value: 'actions', sortable: false }
      ],
      botConfigDialog: false,
      botConfig: {
        enabled: false,
        sample: '',
        regex: '',
        isManager: false, // Indicates if the current user is the project manager
      }
    }
  },
  watch: {
    '$route.query': {
      handler(newQuery) {
        if (newQuery.projectId && newQuery.repoId) {
          this.fetchIssues()
        }
      },
      immediate: true
    }
  },
  methods: {
    async fetchIssues() {
      this.loading = true
      try {
        const res = await getIssues({
          userId: this.user?.id,
          projectId: this.selectedProj.projectId,
          repoId: this.$route.query.repoId
        })
        if (res.data.errcode === 0) {
          this.issues = res.data.data
        } else {
          this.$message.error('获取问题列表失败: ' + res.data.message)
        }
      } catch (error) {
        console.error('获取问题列表失败:', error)
        this.$message.error('获取问题列表失败，请稍后重试')
      } finally {
        this.loading = false
      }
    },
    async fetchRepos() {
      if (!this.selectedProj?.projectId) return
      try {
        const res = await axios.post('/api/develop/getBindRepos', {
          userId: this.user?.id,
          projectId: this.selectedProj.projectId
        })
        if (res.data.errcode === 0) {
          this.repos = res.data.data.map(cur => {
            let remotePath = cur.repoRemotePath.split('/')
            return {
              id: cur.repoId,
              name: remotePath[1],
              intro: cur.repoIntroduction
            }
          })
        } else {
          this.$message.error('获取仓库列表失败: ' + res.data.message)
        }
      } catch (error) {
        console.error('获取仓库列表失败:', error)
        this.$message.error('获取仓库列表失败，请稍后重试')
      }
    },
    handleRepoChange(repoId) {
      this.$router.replace({
        query: {
          projectId: this.selectedProj.projectId,
          repoId: repoId
        }
      })
      this.fetchIssues()
    },
    filterIssues(value, search, item) {
      const matchesSearch = !search || 
        item.issueTitle.toLowerCase().includes(search.toLowerCase())
      
      const matchesStatus = this.filters.status === null || 
        item.isOpen === this.filters.status
      
      return matchesSearch && matchesStatus
    },
    createNewIssue() {
      if (!this.selectedProj?.projectId || !this.selectedRepo) {
        this.$message.error('请先选择项目和仓库')
        return
      }

      this.$router.push({
        path: '/issues/new',
        query: { 
          projectId: this.selectedProj.projectId,
          repoId: this.selectedRepo
        }
      })
    },
    viewIssue(issue) {
      this.$router.push({
        path: `/issues/${issue.issueId}`,
        query: { 
          projectId: this.selectedProj.projectId,
          repoId: this.$route.query.repoId
        }
      })
    },
    formatDate(date) {
      return new Date(date).toLocaleString()
    },
    openBotConfigDialog() {
      this.botConfigDialog = true;
      this.fetchBotConfig();
      this.fetchProjectMembers();
    },
    async fetchProjectMembers() {
      if (!this.selectedProj?.projectId || !this.user?.id) return;
      try {
        const res = await axios.post('/api/plan/showPersonList', {
          projectId: this.selectedProj.projectId,
          userId: this.user.id
        });
        if (res.data.errcode === 0) {
          const members = res.data.data;
          const currentUser = members.find(member => member.peopleId === this.user.id);
          if (currentUser) {
            this.botConfig.isManager = (currentUser.peopleJob === 'C' || currentUser.peopleJob === 'B');
          } else {
            this.botConfig.isManager = false;
          }
        } else {
          console.error('获取项目成员列表失败:', res.data.message);
          this.botConfig.isManager = false;
        }
      } catch (error) {
        console.error('获取项目成员列表失败:', error);
        this.botConfig.isManager = false;
      }
    },
    async fetchBotConfig() {
      if (!this.selectedProj?.projectId || !this.user?.id) return;
      try {
        const res = await axios.get('/api/bot/config', {
          params: {
            projectId: this.selectedProj.projectId,
            userId: this.user.id
          }
        });
        console.log("获取 Bot 配置成功: res = " + res.data.data.config.enabled)
        console.log(res)
        if (res.data.errcode === 0) {
          this.botConfig.enabled = res.data.data.config.enabled ? res.data.data.config.enabled : false;
          this.botConfig.sample = res.data.data.config.sample ? res.data.data.config.sample : '';
          this.botConfig.regex = res.data.data.config.regex ? res.data.data.config.regex : '';
        } else {
          this.$message.error('获取 Bot 配置失败: ' + (res.data.message || '未知错误'));
        }
      } catch (error) {
        console.error('获取 Bot 配置失败:', error);
        this.$message.error('获取 Bot 配置失败，请稍后重试: ' + (error.response?.data?.message || error.message));
      }
    },
    async saveBotConfig() {
      if (!this.selectedProj?.projectId || !this.user?.id) return;
      const payload = {
        projectId: this.selectedProj.projectId,
        userId: this.user.id,
        enabled: this.botConfig.enabled,
      };
      console.log("enabled = " + this.botConfig.enabled)
      console.log("isManager = " + this.botConfig.isManager)
      console.log("sample = " + this.botConfig.sample)
      console.log("regex = " + this.botConfig.regex)
      if (this.botConfig.isManager) {
        payload.sample = this.botConfig.sample;
        payload.regex = this.botConfig.regex;
      }

      try {
        const res = await axios.post('/api/bot/config', payload);
        if (res.data.errcode === 0) {
          this.$message.success('Bot 配置保存成功');
          this.botConfigDialog = false;
        } else {
          this.$message.error('保存 Bot 配置失败: ' + (res.data.message || '未知错误'));
        }
      } catch (error) {
        console.error('保存 Bot 配置失败:', error);
        const errorMessage = error.response?.data?.message || error.message;
        if (error.response?.status === 403) {
          this.$message.error('保存 Bot 配置失败: 您没有权限进行此操作');
        } else {
          this.$message.error('保存 Bot 配置失败，请稍后重试: ' + errorMessage);
        }
      }
    }
  },
  created() {
    if (this.selectedProj) {
      this.fetchRepos()
      this.fetchProjectMembers()
    } else {
      this.$message.warning('请先选择一个项目')
    }
  }
}
</script>

<style scoped>
.issues-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.issues-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.issues-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.search-input {
  width: 300px;
}

.issues-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.issue-card {
  cursor: pointer;
  transition: all 0.3s;
}

.issue-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.issue-header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.issue-title {
  font-weight: bold;
  font-size: 16px;
}

.issue-number {
  color: #666;
}

.issue-meta {
  margin-top: 8px;
  font-size: 14px;
  color: #666;
}

.issue-state {
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  margin-right: 10px;
}

.issue-state.open {
  background-color: #28a745;
  color: white;
}

.issue-state.closed {
  background-color: #cb2431;
  color: white;
}

.issue-labels {
  margin-top: 8px;
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}
</style> 