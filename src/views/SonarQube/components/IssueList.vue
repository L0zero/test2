<!-- src/views/SonarQube/components/IssueList.vue -->
<template>
  <div class="issue-list">
    <h3 @click="onClickWrap">{{ title }}</h3>
    <ul v-if="paginatedIssues.length">
      <li v-for="issue in paginatedIssues" :key="issue.key" class="issue-item">
        <div class="issue-header">
          <span class="issue-severity">{{ issue.severity }}</span>
          <span class="issue-type">{{ issue.type }}</span>
        </div>
        <div class="issue-message">{{ issue.message }}</div>
        <div class="issue-details">
          <span>文件: {{ issue.component }}</span>
          <span>行号: {{ issue.textRange?.startLine || '未知' }}</span>
        </div>
      </li>
    </ul>
    <div v-if="currentPage * pageSize < issues.length" class="buttons">
      <button @click="loadMore" class="load-more">查看更多</button>
      <button @click="viewAll" class="view-all">查看全部</button>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    title: String,
    issues: {
      type: Array,
      default: () => []
    },
    onClick: Function
  },
  data() {
    return {
      currentPage: 1,
      pageSize: 10 // 每页显示的数量
    };
  },
  computed: {
    paginatedIssues() {
      return this.issues.slice(0, this.currentPage * this.pageSize);
    }
  },
  methods: {
    loadMore() {
      this.currentPage++;
    },
    viewAll() {
      this.currentPage = Math.ceil(this.issues.length / this.pageSize);
    },
    onClickWrap() {
      this.onClick();
      //currentPage = 1;
    }
  }
};
</script>

<style scoped>
.issue-list {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1rem;
  background: #f9f9f9;
}
.issue-list h3 {
  cursor: pointer;
  color: #007bff;
}
.issue-list ul {
  list-style: none;
  padding: 0;
}
.issue-item {
  margin: 1rem 0;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background: #fff;
}
.issue-header {
  display: flex;
  justify-content: space-between;
  font-weight: bold;
}
.issue-severity {
  color: #ff4d4f;
}
.issue-type {
  color: #1890ff;
}
.issue-message {
  margin: 0.5rem 0;
  font-size: 0.9rem;
}
.issue-details {
  font-size: 0.8rem;
  color: #888;
  display: flex;
  justify-content: space-between;
}
.load-more {
  padding: 0.5rem 1rem;
  background-color: #007bff;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.load-more:hover {
  background-color: #0056b3;
}
.buttons {
  margin-top: 1rem;
  display: flex;
  gap: 0.5rem;
}
.view-all {
  padding: 0.5rem 1rem;
  background-color: #28a745;
  color: #fff;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
.view-all:hover {
  background-color: #218838;
}
</style>