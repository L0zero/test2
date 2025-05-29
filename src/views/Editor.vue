<script setup>
import * as monaco from 'monaco-editor';
import WebSocket from './webSocket';
import { ref, inject, onMounted, onUnmounted, watch, computed } from 'vue';
import { userGetDocContent } from '../api/user';

const aiMessages = ref([
  { from: 'ai', text: '你好，我是你的AI助手！有什么可以帮你？' },
]);

const showChat = ref(false);
const toggleChat = () => {
  showChat.value = !showChat.value;
};

const newMessage = ref('');
const sending = ref(false);

const sendToAI = async (msg) => {
  const res = await fetch("http://8.140.206.102:8000/api/aiChat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message: msg }),
  });

  const data = await res.json();
  return data.response || "AI 没有回复";
};

const handleSendMessage = async () => {
  if (!newMessage.value.trim()) return;
  aiMessages.value.push({ from: 'user', text: newMessage.value });

  const userText = newMessage.value;
  newMessage.value = '';
  sending.value = true;

  try {
    const aiReply = await sendToAI(userText);
    aiMessages.value.push({ from: 'ai', text: aiReply });
  } catch (err) {
    aiMessages.value.push({ from: 'ai', text: '⚠️ AI 服务出错。' });
  } finally {
    sending.value = false;
  }
};

const fetchAICompletions = async (codeContext, language, position) => {
  try {
    // 注意：URL 和请求体结构需要根据你的后端实际情况调整
    const res = await fetch("http://8.140.206.102:8000/api/aiCompletion", { // 假设的后端端点
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        prompt: codeContext, // 发送光标前的代码作为上下文
        language: language   // 当前文件语言
      }),
    });

    if (!res.ok) {
      console.error("AI Completion API Error:", res.status, await res.text());
      return []; // 出错时返回空数组
    }

    const data = await res.json();
    // 假设后端返回格式为 { suggestions: ["...", "..."] } 或更复杂的结构
    // 例如: { suggestions: [{ label: "...", insertText: "...", kind: "..." }, ...] }
    return data.suggestions || [];

  } catch (err) {
    console.error('Error fetching AI completions:', err);
    return []; // 出错时返回空数组
  }
};

let ws = null;
let isApplyingRemoteChange = false;
const remoteCursors = ref({});

function createRemoteCursorWidget(user) {
  const domNode = document.createElement('div');
  domNode.className = 'cursor-widget';
  domNode.innerText = user;
  domNode.style.background = getUserColor(user);
  domNode.style.color = 'white';
  domNode.style.padding = '2px 5px';
  domNode.style.borderRadius = '3px';
  domNode.style.fontSize = '12px';
  domNode.style.whiteSpace = 'nowrap';
  domNode.style.zIndex = '10';
  domNode.style.border = '1px solid black'; // 辅助调试，可移除

  const widget = {
    getId: () => `cursor.widget.${user}`,
    getDomNode: () => domNode,
    getPosition: () => {
      const data = remoteCursors.value[user];
      if (!data || !data.position) return null;
      return {
        position: data.position,
        preference: [monaco.editor.ContentWidgetPositionPreference.ABOVE],
      };
    },
  };

  editor.value.addContentWidget(widget);
  return widget;
}

const userColorMap = {};
const getUserColor = (user) => {
  if (!userColorMap[user]) {
    const colors = ['#f44336', '#2196f3', '#4caf50', '#ff9800', '#9c27b0'];
    userColorMap[user] = colors[Object.keys(userColorMap).length % colors.length];
  }
  return userColorMap[user];
};

/*remoteCursors.value[message.user] = {
  position: message.position,
  widget: widgetInstance,
};*/

// 编辑器容器
const editorContainer = ref(null);
// 编辑器实例
const editor = ref(null);
// 编辑器内容
const code = ref('');

const props = defineProps({
  userId: {
    type: Number,
    default: 0
  },
  projectId: {
    type: Number,
    default: 0
  },
  docId: {
    type: Number,
    default: 0
  },
  userName: {
    type: String,
    default: 'User1'
  }
});
//const userId = ref(0);
//const projectId = ref(0);
//const user = inject('user', null);
//const proj = inject('proj', null);
//console.log('user', user);
const userName = ref('User1');
userName.value = props.userName;
const userId = ref(0);
userId.value = props.userId;
const projectId = ref(0);
projectId.value = props.projectId;
const docId = ref(0);
docId.value = props.docId;


// 保存 Content Widget 实例
const cursorWidget = ref(null);
// 支持的语言列表
const languages = [
  'apex', 'azcli', 'bat', 'c', 'clojure', 'coffeescript', 'cpp', 'csharp',
  'csp', 'css', 'dockerfile', 'fsharp', 'go', 'graphql', 'handlebars',
  'html', 'ini', 'java', 'json', 'kotlin', 'less', 'lua', 'markdown',
  'msdax', 'mysql', 'objective-c', 'pascal', 'perl', 'pgsql', 'php',
  'plaintext', 'postiats', 'powerquery', 'powershell', 'pug', 'python',
  'r', 'razor', 'redis', 'redshift', 'ruby', 'rust', 'sb', 'scheme',
  'scss', 'shell', 'sol', 'sql', 'st', 'swift', 'tcl', 'typescript',
  'vb', 'xml', 'yaml'
];
// 当前选择的语言
const currentLanguage = ref('python');

// 语言扩展名映射
const languageExtensions = {
  apex: '.apex',
  azcli: '.azcli',
  bat: '.bat',
  c: '.c',
  clojure: '.clj',
  coffeescript: '.coffee',
  cpp: '.cpp',
  csharp: '.cs',
  csp: '.csp',
  css: '.css',
  dockerfile: '.dockerfile',
  fsharp: '.fs',
  go: '.go',
  graphql: '.graphql',
  handlebars: '.hbs',
  html: '.html',
  ini: '.ini',
  java: '.java',
  json: '.json',
  kotlin: '.kt',
  less: '.less',
  lua: '.lua',
  markdown: '.md',
  msdax: '.msdax',
  mysql: '.sql',
  objectivec: '.m',
  pascal: '.pas',
  perl: '.pl',
  pgsql: '.sql',
  php: '.php',
  plaintext: '.txt',
  postiats: '.dats',
  powerquery: '.pq',
  powershell: '.ps1',
  pug: '.pug',
  python: '.py',
  r: '.r',
  razor: '.cshtml',
  redis: '.redis',
  redshift: '.sql',
  ruby: '.rb',
  rust: '.rs',
  sb: '.sb',
  scheme: '.scm',
  scss: '.scss',
  shell: '.sh',
  sol: '.sol',
  sql: '.sql',
  st: '.st',
  swift: '.swift',
  tcl: '.tcl',
  typescript: '.ts',
  vb: '.vb',
  xml: '.xml',
  yaml: '.yaml'
};

// 初始化编辑器
onMounted(() => {
  if (editorContainer.value) {
    // 创建编辑器实例
    editor.value = monaco.editor.create(editorContainer.value, {
      value: code.value,
      language: currentLanguage.value,
      theme: 'vs-dark',
      readOnly: false,
      automaticLayout: true,
      minimap: { enabled: false },
      'semanticHighlighting.enabled': true, // 建议开启语义高亮
      quickSuggestions: { // 开启输入时自动建议
            other: true,
            comments: true,
            strings: true
       },
       suggestOnTriggerCharacters: true, // 在触发字符后显示建议
    });

    let completionProviderDisposable = null;

    const registerAICompletionProvider = (lang) => {
        // 如果已存在，先销毁旧的
        if (completionProviderDisposable) {
            completionProviderDisposable.dispose();
        }

        completionProviderDisposable = monaco.languages.registerCompletionItemProvider(lang, {
            // 可选：定义触发建议的字符，如果希望输入任何字符都可能触发，则留空或不设置
            // triggerCharacters: ['.', '(', "'", '"', ' '],

            provideCompletionItems: async (model, position, context, token) => {
                // 获取光标前的代码作为上下文
                // 可以根据需要调整获取上下文的范围，例如只取当前行或前几行
                // const codeContext = model.getValueInRange({
                //     startLineNumber: 1,
                //     startColumn: 1,
                //     endLineNumber: position.lineNumber,
                //     endColumn: position.column
                // });
                // 更优化的方式可能是只取光标附近的一段代码
                const lineContent = model.getLineContent(position.lineNumber);
                const codeBeforeCursor = lineContent.substring(0, position.column - 1); // 当前行光标前的代码
                // 或者获取更多行的上下文
                 const range = new monaco.Range(Math.max(1, position.lineNumber - 10), 1, position.lineNumber, position.column);
                 const broaderContext = model.getValueInRange(range);


                console.log("Requesting AI completion for:", currentLanguage.value, "at", position);
                console.log("Context:", broaderContext);


                // 调用后端 API 获取建议
                // 注意：这里可以加上 debounce 防止频繁请求
                const aiSuggestions = await fetchAICompletions(broaderContext, currentLanguage.value, {
                    lineNumber: position.lineNumber,
                    column: position.column
                });

                if (token.isCancellationRequested) {
                     console.log("Completion request cancelled.");
                     return { suggestions: [] };
                }

                if (!aiSuggestions || aiSuggestions.length === 0) {
                    return { suggestions: [] }; // 没有建议则返回空列表
                }

                // 将后端建议转换为 Monaco CompletionItem 格式
                const suggestions = aiSuggestions.map(suggestion => {
                    let insertText = suggestion.insertText;
                    let label = suggestion.label;
                    let kind = monaco.languages.CompletionItemKind.Snippet; // 默认类型，可调整
                    let detail = 'AI Suggestion';
                    let documentation = 'Generated by AI';
                    let insertTextRules = monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet; // 允许使用 $1, $2 等占位符

                    // 处理不同格式的后端响应
                    if (typeof suggestion === 'string') {
                        insertText = suggestion;
                        label = suggestion.length > 50 ? suggestion.substring(0, 47) + '...' : suggestion; // 截断过长的标签
                    } else if (typeof suggestion === 'object' && suggestion !== null) {
                        label = suggestion.label || (suggestion.insertText || '').split('\n')[0]; // 使用 label 或 insertText 的第一行
                        insertText = suggestion.insertText || '';
                        kind = mapSuggestionKind(suggestion.kind) || kind; // 映射后端类型到 Monaco 类型
                        detail = suggestion.detail || detail;
                        documentation = suggestion.documentation || documentation;
                         // 检查后端是否明确说明不是 Snippet
                        if (suggestion.isSnippet === false) {
                           insertTextRules = monaco.languages.CompletionItemInsertTextRule.KeepWhitespace;
                        }
                    } else {
                        return null; // 无效建议，跳过
                    }

                    // --- 关键：确定建议替换的范围 ---
                    // Monaco 需要知道这个建议应该替换掉编辑器中的哪部分文本。
                    // 一个常见的策略是替换从 "单词起点" 到 "当前光标" 的部分。
                    const wordInfo = model.getWordUntilPosition(position);
                    const replaceRange = new monaco.Range(
                        position.lineNumber,
                        wordInfo.startColumn, // 从单词开始处替换
                        position.lineNumber,
                        position.column      // 到当前光标处
                    );
                    // 如果是插入新代码（不是替换当前词），range 可以是光标位置
                    // const insertRange = new monaco.Range(position.lineNumber, position.column, position.lineNumber, position.column);


                    return {
                        label: label, // 显示在建议列表中的文本
                        kind: kind,   // 建议类型 (Function, Variable, Snippet, etc.)
                        detail: detail, // 建议的额外信息
                        documentation: documentation, // 建议的文档或描述
                        insertText: insertText, // 实际插入编辑器中的文本
                        insertTextRules: insertTextRules, // 如何插入文本 (例如，作为代码片段处理)
                        range: replaceRange, // !!! 重要：定义建议替换的文本范围
                        // sortText / filterText / preselect 等可选属性可以进一步优化体验
                        sortText: `0_${label}`, // 让 AI 建议排在前面 (使用前缀)
                        // preselect: true, // 可以默认选中第一个 AI 建议
                    };
                }).filter(item => item !== null); // 过滤掉无效的建议


                console.log("AI Suggestions provided:", suggestions);
                return { suggestions: suggestions };
            }
        });
        console.log(`AI Completion provider registered for ${lang}`);
    };

    // 辅助函数：映射后端建议类型到 Monaco 类型 (需要根据后端实际返回调整)
    const mapSuggestionKind = (backendKind) => {
      const kindMap = {
        'function': monaco.languages.CompletionItemKind.Function,
        'method': monaco.languages.CompletionItemKind.Method,
        'variable': monaco.languages.CompletionItemKind.Variable,
        'class': monaco.languages.CompletionItemKind.Class,
        'interface': monaco.languages.CompletionItemKind.Interface,
        'module': monaco.languages.CompletionItemKind.Module,
        'property': monaco.languages.CompletionItemKind.Property,
        'enum': monaco.languages.CompletionItemKind.Enum,
        'keyword': monaco.languages.CompletionItemKind.Keyword,
        'snippet': monaco.languages.CompletionItemKind.Snippet,
        'text': monaco.languages.CompletionItemKind.Text,
        'file': monaco.languages.CompletionItemKind.File,
        'folder': monaco.languages.CompletionItemKind.Folder,
        'color': monaco.languages.CompletionItemKind.Color,
        'unit': monaco.languages.CompletionItemKind.Unit,
        'value': monaco.languages.CompletionItemKind.Value,
        'constant': monaco.languages.CompletionItemKind.Constant,
        'struct': monaco.languages.CompletionItemKind.Struct,
        'event': monaco.languages.CompletionItemKind.Event,
        'operator': monaco.languages.CompletionItemKind.Operator,
        'typeparameter': monaco.languages.CompletionItemKind.TypeParameter,
      };
      return kindMap[backendKind?.toLowerCase()] || monaco.languages.CompletionItemKind.Snippet; // 默认给 Snippet
    };

    // 初始注册当前语言的 Provider
    registerAICompletionProvider(currentLanguage.value);

    // 监听语言变化，重新注册 Provider
    watch(currentLanguage, (newLang) => {
        console.log(`Language changed to ${newLang}, re-registering AI provider.`);
        registerAICompletionProvider(newLang);
    });

    userGetDocContent({ userId: userId, projectId: projectId, docId: docId }).then((res) => {
      code.value = res.data.content;
      editor.value.setValue(code.value);
    });

    // 初始化 Content Widget
    initCursorWidget();

    // 监听内容变化
    editor.value.onDidChangeModelContent(() => {
      if (isApplyingRemoteChange) return; // 阻止回音

      const content = editor.value.getValue();
      code.value = content;

      sendMessage({
        type: 'code_change',
        content,
      });
    });


    // 监听光标位置变化
    editor.value.onDidChangeCursorPosition(() => {
      updateCursorWidget();

        const position = editor.value.getPosition();
        sendMessage({
          type: 'cursor_move',
          user: userName.value,
          position,
        });
    });


    // 监听用户名变化
    watch(userName, () => {
      updateCursorWidget();
    });

    // 初始化 WebSocket
    ws = new WebSocket('ws://8.140.206.102:8000/ws/editor/' + docId.value);


      ws.connect((message) => {
      console.log('Received message:', message);

      if (message.user === userName.value) {
        return;
      }

      if (message.type === 'code_change' && editor.value) {
        console.log(`[代码] 来自 ${message.user} 的代码变更`, message.content);
        isApplyingRemoteChange = true;
        editor.value.setValue(message.content);
        isApplyingRemoteChange = false;
      }

      if (message.type === 'cursor_move') {
        console.log(`[光标] 来自 ${message.user} 的光标变更`, message.position);
        //updateRemoteCursor(message.user, message.position);
        if (!remoteCursors.value[message.user]) {
          // 新建 widget 并添加
          const widget = createRemoteCursorWidget(message.user, message.position);
          remoteCursors.value[message.user] = {
          position: message.position,
          widget
          };

          editor.value.layoutContentWidget(widget);
        } else {
          // 更新位置
          remoteCursors.value[message.user].position = message.position;
          editor.value.layoutContentWidget(remoteCursors.value[message.user].widget);
          }
      }
    });
  }
});

const sendMessage = (msg) => {
  if (ws && ws.readyState === 1) {
    ws.sendMessage({
      user: userName.value,
      ...msg,
    });
  }
};

// 销毁时清理资源
onUnmounted(() => {
  if (editor.value) {
    editor.value.dispose();
    editor.value = null;
  }
  cursorWidget.value = null;
});

// 初始化 Content Widget
const initCursorWidget = () => {
  if (!editor.value) return;

  cursorWidget.value = {
    getId: () => 'cursor.userNameWidget',
    getDomNode: () => {
      const domNode = document.createElement('div');
      domNode.className = 'cursor-widget';
      domNode.innerText = `${userName.value}`;
      domNode.style.background = 'rgba(0, 0, 0, 0.8)';
      domNode.style.color = 'white';
      domNode.style.padding = '2px 5px';
      domNode.style.borderRadius = '3px';
      domNode.style.fontSize = '14px';
      domNode.style.whiteSpace = 'nowrap';
      domNode.style.zIndex = '10';
      return domNode;
    },
    getPosition: () => {
      const position = editor.value.getPosition();
      return {
        position: {
          lineNumber: position.lineNumber,
          column: position.column,
        },
        preference: [monaco.editor.ContentWidgetPositionPreference.ABOVE]
      };
    }
  };

  editor.value.addContentWidget(cursorWidget.value);
};

// 更新 Content Widget
const updateCursorWidget = () => {
  if (!editor.value || !cursorWidget.value) return;
  const domNode = cursorWidget.value.getDomNode();
  domNode.innerText = `${userName.value}`;
  editor.value.layoutContentWidget(cursorWidget.value);
};

// 切换语言
const changeLanguage = (lang) => {
  currentLanguage.value = lang;
  if (editor.value) {
    monaco.editor.setModelLanguage(editor.value.getModel(), lang);
  }
};

// 保存文件
const saveFile = () => {
  if (!editor.value) return;

  // 获取当前语言对应的扩展名
  const extension = languageExtensions[currentLanguage.value] || '.txt';

  const blob = new Blob([code.value], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `${userName.value}_${Date.now()}${extension}`; // 使用对应扩展名
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url)
  }
</script>

<template>
  <div class="editor-container">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <select
        class="language-selector"
        v-model="currentLanguage"
        @change="changeLanguage(currentLanguage)"
      >
        <option v-for="lang in languages" :key="lang" :value="lang">
          {{ lang }}
        </option>
      </select>
      <button class="save-button" @click="saveFile">
        <span role="img" aria-hidden="true">💾</span> 保存文件
      </button>
    </div>

    <!-- 编辑器容器 -->
    <div ref="editorContainer" class="editor"></div>

    <!-- 光标信息栏 -->
    <div v-if="editor" class="cursor-info">
      {{ `${userName.value} | ${editor.getValue().split('\n').length} 行` }}
    </div>

    <!-- AI 显示切换按钮 -->
<button class="chat-toggle-btn" @click="toggleChat">
  {{ showChat ? '×' : 'AI' }}
</button>

<!-- AI 对话框 -->
<div class="ai-chatbox-side" v-if="showChat">
  <div class="chat-header">AI 助手</div>
  <div class="chat-messages">
    <div
      v-for="(msg, index) in aiMessages"
      :key="index"
      :class="['chat-msg', msg.from]"
    >
      <strong>{{ msg.from === 'ai' ? 'AI' : userName }}：</strong>
      <span>{{ msg.text }}</span>
    </div>
  </div>
  <div class="chat-input">
    <input
      type="text"
      v-model="newMessage"
      @keydown.enter="handleSendMessage"
      placeholder="输入消息回车发送"
    />
    <button @click="handleSendMessage" :disabled="sending">
      发送
    </button>
  </div>
</div>
<!-- AI 聊天框 -->

  </div>
</template>

<style scoped>
.cursor-widget {
  position: absolute;
  pointer-events: none;
  user-select: none;
  white-space: nowrap;
  background: rgba(0, 123, 255, 0.6);
  color: white;
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 14px;
  z-index: 100;
}

.editor-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  max-width: 100%;
}

.toolbar {
  display: flex;
  gap: 1rem;
  padding: 0.5rem;
  background: rgba(255, 255, 255, 0.1);
  border-bottom: 1px solid rgba(255, 255, 255, 0.2);
}

.language-selector {
  flex-grow: 1;
  min-width: 200px;
  padding: 0.5rem 1rem;
  border: 1px solid;
  background: rgba(0, 0, 0, 0.1);
}

.save-button {
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  border: none;
  background: #4CAF50;
  color: white;
  font-size: 1rem;
  cursor: pointer;
  transition: background 0.2s;
}

.save-button:hover {
  background: #45a049;
}

.editor {
  flex: 1;
  padding: 1rem;
  overflow: auto;
  background: #1e1e1e;
}

.cursor-info {
  padding: 0.5rem;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  position: sticky;
  bottom: 0;
  left: 0;
  right: 0;
  font-size: 0.875rem;
}

.cursor-widget {
  pointer-events: none;
  user-select: none;
}

.chat-toggle-btn {
  position: fixed;
  top: 50%;
  right: 0;
  transform: translateY(-50%);
  background: #4CAF50;
  color: white;
  border: none;
  padding: 0.75rem 1rem;
  border-top-left-radius: 8px;
  border-bottom-left-radius: 8px;
  cursor: pointer;
  z-index: 300;
  font-weight: bold;
  font-size: 1rem;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.3);
}

.ai-chatbox-side {
  position: fixed;
  top: 0;
  right: 0;
  height: 100vh;
  width: 350px;
  background: #2e2e2e;
  color: white;
  display: flex;
  flex-direction: column;
  z-index: 250;
  border-left: 1px solid #444;
  box-shadow: -2px 0 10px rgba(0, 0, 0, 0.4);
}

.chat-header {
  background: #444;
  padding: 0.75rem;
  font-weight: bold;
  border-bottom: 1px solid #555;
  font-size: 1rem;
}

.chat-messages {
  flex: 1;
  padding: 1rem;
  overflow-y: auto;
}

.chat-msg {
  margin-bottom: 0.75rem;
}

.chat-msg.user {
  text-align: right;
  color: #a3e635;
}

.chat-msg.ai {
  text-align: left;
  color: #60a5fa;
}

.chat-input {
  display: flex;
  border-top: 1px solid #555;
  padding: 0.75rem;
  gap: 0.5rem;
  background: #1e1e1e;
}

.chat-input input {
  flex: 1;
  padding: 0.5rem;
  background: #000;
  border: 1px solid #555;
  color: white;
  border-radius: 4px;
}

.chat-input button {
  background: #4CAF50;
  border: none;
  color: white;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
}

.chat-input button:disabled {
  background: gray;
  cursor: not-allowed;
}


</style>
