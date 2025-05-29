<template>
  <div>
    <!-- 浮标 -->
    <div class="floating-chat" :style="chatStyle" @mousedown="startDrag">
      <img src="@/assets/robot-chat.png" alt="浮标" @click="toggleChat" />
    </div>

    <!-- 聊天窗口 -->
    <div v-if="isChatOpen" class="chat-window" :class="{ fullscreen: isFullscreen }">
      <img src="@/assets/close.png" alt="关闭" class="close-btn" @click="toggleChat" />
      <v-btn icon @click="toggleFullscreen" class="fullscreen-btn">
        <v-icon>{{ isFullscreen ? 'mdi-fullscreen-exit' : 'mdi-fullscreen' }}</v-icon>
      </v-btn>
      <Llvm :userID="userID" :projectID="projectID" />
    </div>
  </div>
</template>

<script>
import Llvm from "@/views/user/AI/Llvm.vue";

export default {
  name: "ChatFloatingWidget",
  components: {
    Llvm,
  },
  props: {
    userID: {
      type: String,
      required: true,
    },
    projectID: {
      type: String,
      required: true,
    },
  },
  data() {
    return {
      isChatOpen: false,
      isFullscreen: false,
      chatStyle: {
        position: "fixed",
        bottom: "20px",
        right: "20px",
        cursor: "move",
      },
    };
  },
  methods: {
    toggleChat() {
      this.isChatOpen = !this.isChatOpen;
    },
    toggleFullscreen() {
      this.isFullscreen = !this.isFullscreen;
    },
    startDrag(event) {
      document.addEventListener("mousemove", this.drag);
      document.addEventListener("mouseup", this.stopDrag);

      // 禁止水平拖动
      document.body.style.cursor = "ns-resize";
    },
    drag(event) {
      const windowHeight = window.innerHeight;

      // 实时更新浮标的 Y 轴坐标，使其跟随鼠标上下移动
      const newTop = event.clientY;

      // 限制浮标只能在窗口范围内上下移动
      this.chatStyle.top = `${Math.max(0, Math.min(newTop, windowHeight - 50))}px`;
      this.chatStyle.bottom = "auto";
    },
    stopDrag() {
      document.removeEventListener("mousemove", this.drag);
      document.removeEventListener("mouseup", this.stopDrag);

      // 恢复默认鼠标样式
      document.body.style.cursor = "default";
    },
  },
};
</script>

<style scoped>
.floating-chat {
  z-index: 1000;
  width: 50px;
  height: 50px;
  position: fixed;
  bottom: 20px;
  right: 20px;
  cursor: move;
}

.floating-chat img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
}

.chat-window {
  position: fixed;
  bottom: 80px;
  right: 20px;
  width: 400px;
  height: 600px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 10px;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  overflow: hidden;
}

.chat-window.fullscreen {
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  border-radius: 0;
}

.fullscreen-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1001;
}

.close-btn {
  position: absolute;
  top: 17px;
  right: 45px;
  width: 20px;
  height: 20px;
  cursor: pointer;
  z-index: 1001;
}
</style>
