<template>
    <div class="chat-container">
        <div class="chat-header">
            <img src="src/assets/robot-icon.png" alt="机器人图标" class="robot-icon" />
            <h1>问答机器人</h1>
        </div>

        <div class="message-list" ref="messageList">
            <div v-for="(message, index) in messages" :key="index" class="message"
                :class="{ 'user-message': message.isUser, 'bot-message': !message.isUser }">
                <div class="message-content-wrapper">
                    <div class="message-content">
                        <!-- 流式消息显示动态内容 -->
                        {{ message.content }}
                        <!-- 流式中显示打字光标 -->
                        <span v-if="message.isStreaming" class="typing-cursor">|</span>
                    </div>
                    <div v-if="!message.isUser && message.retrievalType">
                        <v-chip small :color="getRetrievalColor(message.retrievalType)">
                            {{ getRetrievalText(message.retrievalType) }}
                        </v-chip>
                    </div>
                    <div v-if="!message.isUser && message.toolName">
                        <v-chip small :color="getRetrievalColor('TOOLS')">
                            {{ getRetrievalText('TOOLS') }}
                        </v-chip>
                        <span v-if="message.toolName">
                            工具: {{ message.toolName }}
                        </span>
                    </div>
                    <img v-if="!message.isUser && !message.isStreaming" 
                         src="src/assets/copy.png" 
                         class="copy-icon" 
                         @click="copyToClipboard(message.content)" 
                         title="复制" />
                </div>
                
                
            </div>
            <div v-if="isLoading" class="loading-indicator">
                <div class="dot-flashing"></div>
            </div>
        </div>

        <div v-if="isChatInitialized" class="input-area">
            <textarea v-model="inputMessage" 
                      @keydown.enter.exact.prevent="sendMessage" 
                      placeholder="输入你的消息..."
                      :disabled="isLoading"></textarea>
            <button @click="sendMessage" 
                    :disabled="!inputMessage.trim() || isLoading">
                {{ isLoading ? '思考中...' : '发送' }}
            </button>
        </div>

        <div v-if="errorMsg" class="error-message">
            {{ errorMsg }}
        </div>
    </div>
</template>

<script setup>
import { ref, reactive, nextTick, onUnmounted, onMounted } from 'vue'

const props = defineProps({
    userID: { type: String, required: true },
    projectID: { type: String, required: true }
});

const messages = reactive([]);
const inputMessage = ref('');
const isLoading = ref(false);
const errorMsg = ref('');
const messageList = ref(null);
const isChatInitialized = ref(false);

let socket = null;
let typingTimer = null; // 流式输出定时器
let currentStreamMessage = null; // 当前正在流式输出的消息引用

// 初始化 WebSocket
const initWebSocket = () => {
    socket = new WebSocket('ws://8.140.206.102:8000/ws/aichat/' + props.userID + '/' + props.projectID);

    socket.onopen = () => {
        console.log('WebSocket 连接已建立');
    };

    socket.onmessage = (event) => {
        const data = JSON.parse(event.data);
        switch (data.type) {
            case 'answer':
                handleAnswerChunk(data.content);
                break;
            case 'retrieval':
            case 'tools':
                updateMessageMetadata(data);
                break;
            case 'end':
                finalizeStream();
                break;
            default:
                console.warn('未知消息类型:', data.type);
        }
        nextTick(() => scrollToBottom());
    };

    socket.onerror = (error) => {
        console.error('WebSocket 错误:', error);
        errorMsg.value = '连接异常，请刷新页面重试';
        isLoading.value = false;
    };

    socket.onclose = () => {
        console.log('WebSocket 连接已关闭');
        errorMsg.value = '连接已断开';
        isLoading.value = false;
    };
};

// 处理流式回答片段
const handleAnswerChunk = (content) => {
    if (!currentStreamMessage) {
        // 创建新的流式消息
        currentStreamMessage = {
            content: '',
            isUser: false,
            isStreaming: true,
            timestamp: new Date().toISOString(),
            queue: [] // 待输出的字符队列
        };
        messages.push(currentStreamMessage);
    }
    // 将新内容拆分为字符加入队列（保留原始换行和空格）
    currentStreamMessage.queue.push(...content.split(''));
    // console.log('当前流式消息队列:', currentStreamMessage.queue);
    // console.log('当前流式消息内容:', currentStreamMessage.content);

    // 启动/继续打字动画
    if (typingTimer === null) {
        // console.log('启动打字动画定时器');
        typingTimer = setInterval(() => {
            if (currentStreamMessage.queue.length > 0) {
                currentStreamMessage.content += currentStreamMessage.queue.shift();
            } else {
                // 队列为空时停止定时器（等待后续可能的chunk）
                clearInterval(typingTimer);
                typingTimer = null;
            }
        }, 30); // 50ms/字符的打字速度（可调整）
    }
};

// 更新消息元数据（检索类型/工具名）
const updateMessageMetadata = (data) => {
    console.log('更新消息元数据:', data);
    if (!currentStreamMessage) {
        // 创建新的流式消息
        currentStreamMessage = {
            content: '',
            isUser: false,
            isStreaming: true,
            timestamp: new Date().toISOString(),
            queue: [] // 待输出的字符队列
        };
        messages.push(currentStreamMessage);
    }
    if (currentStreamMessage) {
        if (data.type === 'retrieval') {
            currentStreamMessage.retrievalType = data.type;
        } else if (data.type === 'tools') {
            currentStreamMessage.toolName = data.content || null;
        }
    }
    console.log(currentStreamMessage.toolName)
    console.log(currentStreamMessage.isUser)
};

// 结束流式输出
const finalizeStream = () => {
    if (currentStreamMessage) {
        // 清空剩余队列并停止定时器
        currentStreamMessage.content += currentStreamMessage.queue.join('');
        currentStreamMessage.queue = [];
        currentStreamMessage.isStreaming = false;
        
        if (typingTimer) {
            clearInterval(typingTimer);
            typingTimer = null;
        }
        
        // 重置状态
        currentStreamMessage = null;
        isLoading.value = false;
    }
};

// 关闭 WebSocket
const closeWebSocket = () => {
    if (socket) {
        socket.close();
        socket = null;
    }
    if (typingTimer) {
        clearInterval(typingTimer);
        typingTimer = null;
    }
};

const initializeChat = () => {
    isChatInitialized.value = true;
    initWebSocket();
    scrollToBottom();
};

onUnmounted(() => closeWebSocket());

onMounted(() => {
    // 初始化聊天
    initializeChat();
})

const getRetrievalText = (type) => ({
    CODE: '代码库检索',
    DOCUMENT: '共享文档检索',
    RAG: 'RAG向量库检索',
    MIXED: '混合检索',
    TOOLS: '工具调用'
}[type] || '未知检索方式');

const getRetrievalColor = (type) => ({
    CODE: 'blue',
    DOCUMENT: 'green',
    RAG: 'purple',
    MIXED: 'orange',
    TOOLS: 'red'
}[type] || 'grey');

const scrollToBottom = () => {
    if (messageList.value) {
        messageList.value.scrollTop = messageList.value.scrollHeight;
    }
};

const sendMessage = () => {
    const content = inputMessage.value.trim();
    if (!content || !socket || socket.readyState !== WebSocket.OPEN) {
        if (!socket || socket.readyState !== WebSocket.OPEN) {
            errorMsg.value = 'WebSocket 连接未就绪，请稍后重试';
        }
        return;
    }
    if (!content || !socket || socket.readyState !== WebSocket.OPEN) return;

    inputMessage.value = '';
    errorMsg.value = '';
    isLoading.value = true;

    // 添加用户消息
    messages.push({
        content,
        isUser: true,
        timestamp: new Date().toISOString()
    });

    // 发送请求
    socket.send(JSON.stringify({
        userId: props.userID,
        projectId: props.projectID,
        question: content,
    }));
};

const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text)
        .then(() => alert('已复制到剪切板'))
        .catch(err => console.error('复制失败:', err));
};
</script>

<style scoped>
.chat-container {
    height: 100%;
    display: flex;
    flex-direction: column;
    background: #f5f7fa;
}

.chat-header {
    padding: 1rem;
    background: white;
    border-bottom: 1px solid #e5e7eb;
    display: flex;
    align-items: center;
    gap: 1rem;
}

.robot-icon {
    height: 2.5rem;
    width: 2.5rem;
}

.message-list {
    flex: 1;
    overflow-y: auto;
    padding: 1rem;
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
}

.message {
    max-width: 70%;
    align-self: flex-start;
    padding: 0.8rem 1.2rem;
    border-radius: 12px;
    line-height: 1.6;
}

.user-message {
    background: white;
    align-self: flex-end;
    box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.bot-message {
    background: #4f46e5;
    color: white;
    box-shadow: 0 1px 3px rgba(79,70,229,0.2);
}

.message-content-wrapper {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
}

.copy-icon {
    position: absolute;
    left: -0.5rem;
    bottom: -3rem;
    width: 30px;
    height: 30px;
    cursor: pointer;
    opacity: 0.7;
    transition: opacity 0.2s, transform 0.2s;
}

.copy-icon:hover {
    opacity: 1;
    transform: scale(1.2);
}

.typing-cursor {
    animation: blink 1s step-end infinite;
    margin-left: 2px;
}

@keyframes blink {
    50% { opacity: 0; }
}

.init-chat {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
}

.input-area {
    display: flex;
    padding: 1rem;
    gap: 0.5rem;
    background: white;
    border-top: 1px solid #e5e7eb;
}

textarea {
    flex: 1;
    padding: 0.8rem;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    resize: none;
    font-size: 1rem;
}

button {
    padding: 0 1.5rem;
    background: #4f46e5;
    color: white;
    border: none;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.2s;
}

button:disabled {
    background: #938df4;
    cursor: not-allowed;
}

.error-message {
    padding: 1rem;
    background: #fef2f2;
    color: #b91c1c;
    text-align: center;
}

.loading-indicator {
    align-self: center;
    margin: 1rem 0;
}

.dot-flashing {
    position: relative;
    width: 8px;
    height: 8px;
    border-radius: 4px;
    background-color: #9ca3af;
    animation: dot-flashing 1s infinite linear alternate;
    animation-delay: 0.5s;
}

.dot-flashing::before, 
.dot-flashing::after {
    content: '';
    display: inline-block;
    position: absolute;
    top: 0;
    width: 8px;
    height: 8px;
    border-radius: 4px;
    background-color: #9ca3af;
}

.dot-flashing::before {
    left: -15px;
    animation: dot-flashing 1s infinite alternate;
    animation-delay: 0s;
}

.dot-flashing::after {
    left: 15px;
    animation: dot-flashing 1s infinite alternate;
    animation-delay: 1s;
}

@keyframes dot-flashing {
    0% { background-color: #9ca3af; }
    50%, 100% { background-color: #e5e7eb; }
}
</style>
