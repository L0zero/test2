<script>
import { getDiscussionSummary } from "@/api/AI/AbstractSidebar"; // 引入API
import { ref, onMounted, watch } from "vue";

export default {
    name: "Sidebar",
    props: {
        collapsed: {
            type: Boolean,
            default: false,
        },
        allowQA: {
            type: Boolean,
            default: true, // 默认允许生成问答元组
        },
        roomId: {
            type: String,
            required: true, // 从父组件传入当前 roomId
        },
        projectId: {
            type:  String,
            required: true, // 从父组件传入当前 projectId
        },
    },
    emits: ["toggle"],
    setup(props, { emit }) {
        const qaList = ref([]); // 初始化问答元组列表

        const fetchQAList = async () => {
            if (!props.allowQA) return; // 如果不允许生成问答元组，则直接返回
            try {
                const response = await getDiscussionSummary(props.roomId, props.projectId); // 调用API
                if (response.data.errcode === 0) {
                    qaList.value = response.data.data.summary.map(item => ({ ...item, expanded: false })); // 适配新的数据结构
                } else {
                    console.error("Failed to fetch QA list:", response.data.message);
                }
            } catch (error) {
                console.error("Error fetching QA list:", error);
            }
        };

        onMounted(() => {
            fetchQAList();
        });

        watch(() => props.allowQA, (newVal) => {
            if (newVal) {
                fetchQAList(); // 当允许生成问答元组时，重新获取数据
            } else {
                qaList.value = []; // 清空问答元组列表
            }
        });

        const toggleExpand = (index) => {
            qaList.value.forEach((item, i) => {
                item.expanded = i === index ? !item.expanded : false; // 仅展开当前点击的项
            });
        };

        const regenerateQAList = async () => {
            try {
                await fetchQAList(); // 调用已有的 fetchQAList 方法重新生成问答元组
            } catch (error) {
                console.error("Error regenerating QA list:", error); // 添加错误处理逻辑
            }
        };

        return {
            qaList,
            emitToggle: () => emit("toggle"),
            toggleExpand,
            regenerateQAList,
        };
    },
};
</script>

<template>
<div class="sidebar-container">
    <!-- 将按钮固定在右上角 -->
    <v-btn icon class="toggle-btn" @click="emitToggle">
        <v-icon>{{ collapsed ? "mdi-chevron-right" : "mdi-chevron-left" }}</v-icon>
    </v-btn>

    <v-card v-if="!collapsed" class="sidebar-card">
        <v-card-text class="qa-list">
            <v-btn icon class="refresh-btn" color="primary" @click="regenerateQAList">
                <img src="@/assets/refresh.png" alt="刷新" class="refresh-icon" />
            </v-btn>
            <v-list v-if="qaList.length > 0">
                <v-list-item v-for="(qa, index) in qaList" :key="index" @click="toggleExpand(index)">
                    <v-list-item-content>
                        <v-list-item-title style="white-space: pre-wrap; word-break: break-word;">{{ qa.question }}</v-list-item-title>
                        <v-list-item-subtitle v-if="qa.expanded" style="white-space: pre-wrap; word-break: break-word;">{{ qa.answer }}</v-list-item-subtitle>
                        <v-list-item-subtitle v-else style="white-space: pre-wrap; word-break: break-word;">{{ qa.answer.substring(0, 20) }}...</v-list-item-subtitle>
                    </v-list-item-content>
                </v-list-item>
            </v-list>
            <v-alert v-else type="warning" border="left" color="orange lighten-4" icon="mdi-alert-circle-outline" class="mt-3 alert-custom">
                问答元组生成已禁用或暂无数据。
            </v-alert>
        </v-card-text>
    </v-card>
</div>
</template>

<style>
.sidebar-container {
    height: 100%;
    width: 100%;
    display: flex;
    flex-direction: column;
    background-color: #fff;
}

.sidebar-card {
    flex: 1;
    display: flex;
    flex-direction: column;
    height: 100%;
    overflow-y: auto;
}

.qa-list {
    overflow-y: auto;
    max-height: 100%;
    padding: 16px;
}

/* 按钮样式，固定在右上角 */
.toggle-btn {
    position: absolute;
    top: 10px;
    right: 10px;
    z-index: 2;
    background-color: #87CEEB;
    color: white;
    border-radius: 50%;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    transition: transform 0.3s ease, background-color 0.3s ease;
}

.toggle-btn:hover {
    transform: scale(1.1);
    background-color: #00BFFF;
}

/* 自定义 v-alert 样式 */
.alert-custom {
    font-size: 16px;
    font-weight: bold;
    color: #d35400;
}

/* 刷新按钮样式 */
.refresh-btn {
    margin-bottom: 10px;
    align-self: center;
}

/* 刷新图标样式 */
.refresh-icon {
    width: 24px;
    height: 24px;
}
</style>
