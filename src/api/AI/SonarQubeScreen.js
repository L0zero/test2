import axios from 'axios';

/**
 * 获取项目的指标数据
 * @param {string} projectKey 项目标识
 * @param {string} token SonarQube Token
 * @returns {Promise<Array>} 指标数据
 */
export async function getSonarMetrics(projectKey) {
  const response = await axios.get('/api/sonar/measures/component', {
    params: {
      component: projectKey,
      metricKeys: 'coverage,bugs,vulnerabilities'
    },
  });

  if (!response.data.component || !response.data.component.measures) {
    throw new Error('Invalid response format: Missing component or measures');
  }

  return response.data.component.measures.map(measure => ({
    metric: measure.metric,
    value: parseFloat(measure.value), // 转换为数字类型
    bestValue: measure.bestValue
  }));
}

/**
 * 获取项目的问题列表
 * @param {string} projectKey 项目标识
 * @param {string} token SonarQube Token
 * @returns {Promise<Array>} 问题列表
 */
export async function getSonarIssues(projectKey) {
  const response = await axios.get('/api/sonar/issues/search', {
    params: {
      components: projectKey,
      asc: true,
      assigned: true,
      resolved: false,
    },
  });
  return response.data.issues;
}

export async function useSonarScanner(repoId) {
  const response = await axios.post('/api/sonar/scan', {
    repoId: repoId,
  });
  return response.data; // 返回完整的响应数据
}
