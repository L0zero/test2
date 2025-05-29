<template>
  <div class="sonar-report">
    <!-- 加载状态 -->
    <div v-if="loading" class="loading">Loading...</div>

    <!-- 错误提示 -->
    <div v-if="error" class="error">{{ error }}</div>

    <!-- 数据展示 -->
    <div v-if="!loading && !error">
      <h1>项目代码质量报告 - {{ projectKey }}</h1>

      <!-- 下载按钮 -->
      <button @click="downloadReport">下载报告</button>

      <!-- 指标卡片 -->
      <div class="metrics-grid">
        <MetricCard title="代码覆盖率" :value="`${metrics.coverage}%`" subtitle="目标: 80%" />
        <MetricCard title="Bugs" :value="metrics.bugs" :subtitle="'严重问题:' + blockerIssues.length" />
        <MetricCard title="安全漏洞" :value="metrics.vulnerabilities" />
      </div>

      <!-- 问题列表 -->
      <div class="issues-section">
        <div>
          <IssueList :title="`严重问题 (Blocker/Critical) ${showBlockerIssues ? '▲' : '▼'}`"
            :issues="showBlockerIssues ? blockerIssues : []" :onClick="toggleBlockerIssues" />
        </div>
        <div>
          <IssueList :title="`一般问题 (Major/Minor) ${showMinorIssues ? '▲' : '▼'}`"
            :issues="showMinorIssues ? minorIssues : []" :onClick="toggleMinorIssues" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getSonarMetrics, getSonarIssues } from '@/api/AI/SonarQubeScreen.js'
import MetricCard from '@/views/SonarQube/components/MetricCard.vue'
import IssueList from '@/views/SonarQube/components/IssueList.vue'

export default {
  inject: {
    user: { default: null },
    selectedProj: { default: null },
  },
  components: { MetricCard, IssueList },
  data() {
    return {
      projectKey: this.$route.params.projectKey || 'error',
      loading: false,  // TODO 实现成功后改为true
      error: null,
      metrics: {
        coverage: 0,
        bugs: 0,
        vulnerabilities: 0
      },
      allIssues: [],
      showBlockerIssues: false,
      showMinorIssues: false
    }
  },
  computed: {
    // 过滤严重问题
    blockerIssues() {
      return this.allIssues.filter(issue =>
        ['BLOCKER', 'CRITICAL'].includes(issue.severity)
      )
    },
    // 过滤一般问题
    minorIssues() {
      return this.allIssues.filter(issue =>
        ['MAJOR', 'MINOR'].includes(issue.severity))
    }
  },
  methods: {
    toggleBlockerIssues() {
      this.showBlockerIssues = !this.showBlockerIssues;
    },
    toggleMinorIssues() {
      this.showMinorIssues = !this.showMinorIssues;
    },
    downloadReport() {
      const metricsText = `代码覆盖率: ${this.metrics.coverage}%\nBugs: ${this.metrics.bugs}\n安全漏洞: ${this.metrics.vulnerabilities}\n`;
      const blockerIssuesText = `严重问题:\n${this.blockerIssues.map(issue => `- ${issue.message}`).join('\n')}\n`;
      const minorIssuesText = `一般问题:\n${this.minorIssues.map(issue => `- ${issue.message}`).join('\n')}\n`;

      const reportContent = `项目代码质量报告 - ${this.projectKey}\n\n${metricsText}\n${blockerIssuesText}\n${minorIssuesText}`;
      const blob = new Blob([reportContent], { type: 'text/plain;charset=utf-8' });
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `${this.projectKey}_SonarQube_Report.txt`;
      link.click();
      URL.revokeObjectURL(link.href);
    }
  },
  async mounted() {
    try {
        // 获取指标数据
        const metricsData = await getSonarMetrics(this.projectKey);
        this.metrics = {
            coverage: metricsData.find(m => m.metric === 'coverage')?.value || 0,
            bugs: metricsData.find(m => m.metric === 'bugs')?.value || 0,
            vulnerabilities: metricsData.find(m => m.metric === 'vulnerabilities')?.value || 0
        };

        // 获取问题列表
        this.allIssues = await getSonarIssues(this.projectKey);
    } catch (err) {
        console.error('Error loading data:', err); // 打印详细错误信息
        this.error = '无法加载数据: ' + (err.message || '未知错误');
    } finally {
        this.loading = false;
    }
  }
} 
</script>

<style scoped>
.sonar-report {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 1rem;
  margin: 2rem 0;
}

.issues-section {
  display: grid;
  gap: 2rem;
  margin-top: 2rem;
}

.loading,
.error {
  padding: 2rem;
  text-align: center;
  font-size: 1.2rem;
}

.error {
  color: #ff4d4f;
}

button {
  margin-bottom: 1rem;
  padding: 0.5rem 1rem;
  font-size: 1rem;
  cursor: pointer;
  background-color: #99ffff;
  /* 添加背景色 */
  border: 1px solid #99ffff;
  /* 添加边框 */
  border-radius: 4px;
  /* 圆角 */
  transition: background-color 0.3s, border-color 0.3s;
  /* 添加过渡效果 */
  margin-right: 1rem;
  /* 添加右边距 */
}

button:hover {
  background-color: #e6f7ff;
  /* 鼠标悬停时的背景色 */
  border-color: #91d5ff;
  /* 鼠标悬停时的边框色 */
}
</style>