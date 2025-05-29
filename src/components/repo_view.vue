<script>
import {computed} from 'vue'
import branch_view from "@/components/branch_view.vue";
import commit_view from "@/components/commit_view.vue";
import bindGithubRepo from "../components/bind_repo.vue"
import bindedGithubRepos from "../components/repo_list.vue"
import pr_view from "@/components/pr_view.vue";
import topicSetting from "@/utils/topic-setting";
import { useSonarScanner } from "@/api/AI/SonarQubeScreen";

export default {
    name: "repoView",
    components: {
      branch_view,
      commit_view,
      pr_view,
      bindGithubRepo,
      bindedGithubRepos
    },
    data() {
      return {
        selectedRepo: 0,
        showBindDialog: false,
        showAnalysisButton: false,
        analysisProjectName: null, // 用于存储静态代码分析返回的项目名称
        showBindDialog: false,
        // bondedRepos: [
        //   {
        //     id: 1,
        //     user: "opencv",
        //     repo: "opencv",
        //     intro: "opencv introduction. A Bonded repo of #" + this.proj.id + " called " + this.proj.name
        //   }, {
        //     id: 2,
        //     user: "buaa2023sw",
        //     repo: "doc",
        //     intro: "doc introduction"
        //   }
        // ]
      }
    },
    inject: {
        user: {default: null},
        proj: { default: null },
        bindRepos: {default: null},
        bindReposBusy: {default: null}
    },
    provide() {
        return {
          selectedRepo: computed(() => this.bindRepos[this.selectedRepo])
        }
    },
    methods: {
      getTopicColor: topicSetting.getColor,
      getDarkColor: topicSetting.getDarkColor,
      getRadialGradient: topicSetting.getRadialGradient,
      triggerStaticCodeAnalysis() {
            // 调用 SonarQube 接口进行静态代码分析
            alert('开始静态代码分析...请勿关闭界面');
            useSonarScanner(this.bindRepos[this.selectedRepo].id)
                .then((response) => {
                    if (response.project_name) {
                        alert(`静态代码分析已完成！项目名称：${response.project_name}`);
                        this.analysisProjectName = response.project_name; // 存储项目名称
                        this.showAnalysisButton = true;
                    } else {
                        alert('静态代码分析失败：' + response.message);
                        this.showAnalysisButton = false;
                    }
                })
                .catch((error) => {
                    alert('静态代码分析出错：' + error.message);
                    this.showAnalysisButton = false;
                });
        },
        navigateToCodeAnalysis() {
            if (this.analysisProjectName) {
                const url = this.$router.resolve({ name: '静态代码analysis', params: { projectKey: this.analysisProjectName } }).href;
                window.open(url, '_blank'); // 在新窗口中打开
            } else {
                alert('请等待静态代码分析运行结束');
            }
        },
        handleBindRepoClose() {
            this.showBindDialog = false;
            this.updateBindRepos();
        },
    }
}
</script>

<template>
<v-col cols="12" class="px-1">
  <h2 v-if="bindReposBusy">代码存储库</h2>
  <h2 v-else-if="bindRepos.length > 0">代码存储库 - {{ bindRepos[selectedRepo].user }} / {{ bindRepos[selectedRepo].repo}}</h2>
  <h2 v-else>绑定一个代码存储库</h2>

  <v-skeleton-loader v-if="bindReposBusy" type="card"></v-skeleton-loader>
  <div v-else-if="bindRepos.length > 0">
      <v-tabs :color="getDarkColor(user.topic)" v-model="selectedRepo">
        <v-tab v-for="repository in bindRepos" :key="repository.id">{{ repository.repo }}</v-tab>
        
      </v-tabs>
      <v-tabs-items v-model="selectedRepo">
          <v-tab-item v-for="repository in bindRepos" :key="repository.id">
              <p v-if="bindRepos[selectedRepo].intro !== 'No description available'">代码存储库介绍：{{ bindRepos[selectedRepo].intro }}</p>
              <p v-else>这个代码存储库没有介绍哦</p>
              <v-row>
                <v-btn v-if="!showAnalysisButton" @click="triggerStaticCodeAnalysis" class="mr-2">运行静态代码分析</v-btn>
                <v-btn v-if="showAnalysisButton" @click="navigateToCodeAnalysis" class="mr-2">进入代码分析界面</v-btn>
                <v-btn @click="showBindDialog = true" color="primary">绑定新的仓库</v-btn>
              </v-row>
              <v-row><v-col class="ma-1"><v-card :style="getRadialGradient(user.topic)" raised class="pa-2"><branch_view /></v-card></v-col></v-row>
              <v-row>
                <v-col cols="12" sm="12" md="6" lg="6" xl="6" class="ma-auto">
                  <v-card height="200px" :style="getRadialGradient(user.topic)" raised class="pa-2 overflow-y-auto">
                    <v-card-title>事务</v-card-title>
                    <v-card-text>
                      <commit_view />
                    </v-card-text>
                  </v-card>
                </v-col>
                <v-col cols="12" sm="12" md="6" lg="6" xl="6" class="ma-auto">
                  <v-card height="200px" :style="getRadialGradient(user.topic)" raised class="pa-2 overflow-y-auto">
                    <v-card-title>和并请求</v-card-title>
                    <v-card-text>
                      <pr_view />
                    </v-card-text>
                  </v-card>
                </v-col>
              </v-row>
          </v-tab-item>
      </v-tabs-items>
  </div>
  <div v-else>
    
        <bindGithubRepo />
      
  </div>
  <!-- 绑定仓库对话框 -->
        <v-dialog v-model="showBindDialog" max-width="800px">
            <v-card>
                <v-card-title>绑定新的仓库</v-card-title>
                <v-card-text>
                    <bindGithubRepo @close="handleBindRepoClose" />
                </v-card-text>
            </v-card>
        </v-dialog>
</v-col>



</template>