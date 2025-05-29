import axios from "axios"; // 替换为 axios
export const getDiscussionSummary = (roomId, projectId) => {
  return axios.post("/api/knowledgeBase/generateDiscussionSummary", {
      roomId: roomId, 
      projectId: projectId,
  });
};