<script setup>
import { ref, nextTick, onMounted, watch } from 'vue';
import { Plus, Moon, Sunny, User, ArrowRight, CircleClose, Upload } from '@element-plus/icons-vue';
import { createToolChat } from '@/api/tool';

// 聊天状态
const messages = ref([]);
const userInput = ref('');
const isLoading = ref(false);
const chatContainer = ref(null);
const textarea = ref(null);
const darkMode = ref(false);
const memoryId = ref(Date.now().toString());

// 创建工具聊天实例
const toolChat = createToolChat();
let typingInterval = null;

// 上传文件状态
const uploadedFiles = ref([]);
const fileInput = ref(null);

/**
 * 调整文本域高度
 */
const adjustTextareaHeight = () => {
  const textareaEl = textarea.value;
  if (!textareaEl) return;
  textareaEl.style.height = 'auto';
  textareaEl.style.height = `${Math.min(textareaEl.scrollHeight, 200)}px`;
};

/**
 * 滚动到底部
 */
const scrollToBottom = () => {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
    }
  });
};

/**
 * 切换暗黑模式
 */
const toggleDarkMode = () => {
  darkMode.value = !darkMode.value;
  localStorage.setItem('darkMode', darkMode.value);
};

/**
 * 新建会话
 */
const startNewConversation = () => {
  messages.value = [];
  memoryId.value = Date.now().toString();
  messages.value.push({
    role: 'assistant',
    content: '你好！我是Qwen,请问有什么能帮到您？',
    isLoading: false,
    visibleChars: 0,
    isStreaming: false
  });
  messages.value[0].visibleChars = messages.value[0].content.length;
  scrollToBottom();
  nextTick(() => {
    textarea.value?.focus();
  });
};

/**
 * 打字机效果
 */
const startTypingEffect = (messageIndex) => {
  const message = messages.value[messageIndex];
  if (!message || message.visibleChars >= message.content.length) {
    clearInterval(typingInterval);
    typingInterval = null;
    messages.value[messageIndex].isStreaming = false;
    return;
  }
  messages.value[messageIndex].visibleChars++;
  scrollToBottom();
};

/**
 * 发送消息
 */
const sendMessage = async () => {
  if (!userInput.value.trim() && uploadedFiles.value.length === 0) return;
  if (isLoading.value) return;

  // 构建消息内容，包含文件信息
  let messageContent = userInput.value.trim();
  if (uploadedFiles.value.length > 0) {
    const fileNames = uploadedFiles.value.map(f => f.name).join(', ');
    if (messageContent) {
      messageContent += `\n\n附件: ${fileNames}`;
    } else {
      messageContent = `附件: ${fileNames}`;
    }
  }

  const userMessage = {
    role: 'user',
    content: messageContent,
    isLoading: false,
    visibleChars: messageContent.length,
    isStreaming: false,
    files: uploadedFiles.value.length > 0 ? [...uploadedFiles.value] : null
  };

  messages.value.push(userMessage);

  const assistantMessage = {
    role: 'assistant',
    content: '',
    isLoading: true,
    visibleChars: 0,
    isStreaming: true
  };

  messages.value.push(assistantMessage);

  userInput.value = '';
  // 在清空之前保存文件列表，用于发送给 API
  const filesToSend = [...uploadedFiles.value];
  uploadedFiles.value = [];
  adjustTextareaHeight();
  scrollToBottom();

  isLoading.value = true;

  const messageIndex = messages.value.length - 1;

  // 清除之前的打字效果
  if (typingInterval) {
    clearInterval(typingInterval);
    typingInterval = null;
  }

  await toolChat.send(memoryId.value, messageContent, {
    onStart: () => {},
    onChunk: (buffer) => {
      messages.value[messageIndex].content = buffer;
      messages.value[messageIndex].isLoading = false;
      if (!typingInterval) {
        typingInterval = setInterval(() => {
          startTypingEffect(messageIndex);
        }, 20);
      }
      scrollToBottom();
    },
    onDone: () => {
      const lastMessage = messages.value[messageIndex];
      lastMessage.isLoading = false;
      lastMessage.isStreaming = false;
      if (lastMessage.visibleChars < lastMessage.content.length) {
        lastMessage.visibleChars = lastMessage.content.length;
      }
      isLoading.value = false;
      if (typingInterval) {
        clearInterval(typingInterval);
        typingInterval = null;
      }
      scrollToBottom();
    },
    onError: (err) => {
      const lastMessage = messages.value[messageIndex];
      lastMessage.content = '抱歉，请求过程中出现错误: ' + err.message;
      lastMessage.visibleChars = lastMessage.content.length;
      lastMessage.isLoading = false;
      lastMessage.isStreaming = false;
      isLoading.value = false;
    }
  }, filesToSend);
};

/**
 * 停止响应
 */
const stopResponse = () => {
  toolChat.abort();
  const lastMessage = messages.value[messages.value.length - 1];
  if (lastMessage) {
    lastMessage.isLoading = false;
    lastMessage.isStreaming = false;
    if (lastMessage.visibleChars < lastMessage.content.length) {
      lastMessage.visibleChars = lastMessage.content.length;
    }
  }
  isLoading.value = false;
  if (typingInterval) {
    clearInterval(typingInterval);
    typingInterval = null;
  }
};

/**
 * 打开文件选择对话框
 */
const openFileSelector = () => {
  fileInput.value?.click();
};

/**
 * 处理文件选择
 */
const handleFileChange = (event) => {
  const files = Array.from(event.target.files);
  if (files.length === 0) return;

  files.forEach(file => {
    // 限制文件大小为 10MB
    if (file.size > 10 * 1024 * 1024) {
      alert('文件大小不能超过 10MB');
      return;
    }

    // 创建文件信息对象
    const fileInfo = {
      id: Date.now() + Math.random(),
      name: file.name,
      size: formatFileSize(file.size),
      type: file.type,
      file: file
    };

    uploadedFiles.value.push(fileInfo);
  });

  // 清空 input，允许重复选择相同文件
  event.target.value = '';
};

/**
 * 格式化文件大小
 */
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
};

/**
 * 移除已上传的文件
 */
const removeFile = (fileId) => {
  const index = uploadedFiles.value.findIndex(f => f.id === fileId);
  if (index !== -1) {
    uploadedFiles.value.splice(index, 1);
  }
};

/**
 * 组件挂载时初始化
 */
onMounted(() => {
  const savedDarkMode = localStorage.getItem('darkMode');
  if (savedDarkMode !== null) {
    darkMode.value = savedDarkMode === 'true';
  } else {
    darkMode.value = false;
    localStorage.setItem('darkMode', 'false');
  }

  messages.value.push({
    role: 'assistant',
    content: '你好！我是qwen,请问有什么能帮到您？',
    isLoading: false,
    visibleChars: 0,
    isStreaming: false
  });

  messages.value[0].visibleChars = messages.value[0].content.length;
  scrollToBottom();

  nextTick(() => {
    textarea.value?.focus();
  });
});

/**
 * 监听消息变化自动滚动
 */
watch(messages, scrollToBottom, { deep: true });
</script>

<template>
  <div class="chat-container" :class="{ 'dark': darkMode }">
    <!-- 顶部导航栏 -->
    <header class="chat-header">
      <div class="header-left">
        <span class="logo">qwen</span>
      </div>
      <div class="header-right">
        <el-button
          @click="startNewConversation"
          class="new-chat-btn"
          type="success"
          circle
        >
          <el-icon><Plus /></el-icon>
        </el-button>
        <el-button
          @click="toggleDarkMode"
          class="theme-btn"
          circle
        >
          <el-icon v-if="darkMode"><Moon /></el-icon>
          <el-icon v-else><Sunny /></el-icon>
        </el-button>
      </div>
    </header>

    <!-- 聊天内容区域 -->
    <main ref="chatContainer" class="chat-main">
      <div
        v-for="(message, index) in messages"
        :key="index"
        class="message-wrapper"
      >
        <div
          :class="['message-row', message.role === 'user' ? 'user-row' : '']"
        >
          <!-- 用户头像 -->
          <div
            v-if="message.role === 'user'"
            class="avatar user-avatar"
          >
            <el-icon><User /></el-icon>
          </div>
          <!-- AI 助手头像 -->
          <div
            v-else
            class="avatar ai-avatar"
          >
            <img src="@/assets/logo.png" alt="AI" style="width: 100%; height: 100%; object-fit: cover;" />
          </div>
          <!-- 消息气泡 -->
          <div
            :class="['message-bubble', {
              'user-bubble': message.role === 'user',
              'ai-bubble': message.role === 'assistant',
              'dark-ai-bubble': message.role === 'assistant' && darkMode
            }]"
          >
            <!-- 加载动画 -->
            <div v-if="message.isLoading" class="loading-dots">
              <span class="dot"></span>
              <span class="dot delay-100"></span>
              <span class="dot delay-200"></span>
            </div>
            <!-- 消息内容 -->
            <div v-else class="message-content">
              <span
                v-for="(char, charIndex) in message.content"
                :key="charIndex"
                :class="{ 'fade-in': charIndex < message.visibleChars }"
              >
                {{ char }}
              </span>
              <span v-if="message.isStreaming" class="typing-cursor"></span>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 输入框区域 -->
    <footer class="chat-footer">
      <!-- 已上传文件列表 -->
      <div v-if="uploadedFiles.length > 0" class="files-list">
        <div
          v-for="file in uploadedFiles"
          :key="file.id"
          class="file-item"
        >
          <i class="fas fa-file-alt"></i>
          <span class="file-name">{{ file.name }}</span>
          <span class="file-size">{{ file.size }}</span>
          <button @click="removeFile(file.id)" class="remove-file-btn">
            <i class="fas fa-times"></i>
          </button>
        </div>
      </div>

      <div class="input-wrapper">
        <!-- 上传按钮 -->
        <button
          @click="openFileSelector"
          class="upload-btn"
          type="button"
        >
          <el-icon><Upload /></el-icon>
        </button>

        <textarea
          v-model="userInput"
          @keydown.enter.exact.prevent="sendMessage"
          @keydown.ctrl.enter.exact.prevent="sendMessage"
          @keydown.esc.exact="stopResponse"
          placeholder="输入您的问题..."
          class="chat-input"
          rows="1"
          ref="textarea"
          @input="adjustTextareaHeight"
        ></textarea>
        <el-button
          @click="isLoading ? stopResponse() : sendMessage()"
          :disabled="(uploadedFiles.length === 0) && !isLoading"
          :type="isLoading ? 'danger' : 'primary'"
          class="send-btn"
          circle
        >
          <el-icon v-if="isLoading"><CircleClose /></el-icon>
          <el-icon v-else><ArrowRight /></el-icon>
        </el-button>
      </div>

      <!-- 隐藏的文件选择器 -->
      <input
        ref="fileInput"
        type="file"
        multiple
        @change="handleFileChange"
        class="hidden-file-input"
      />
    </footer>
  </div>
</template>

<style scoped>
/* 容器样式 */
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f9fafb;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
}

/* 暗黑模式 */
.chat-container.dark {
  background: #1f2937;
}

/* 顶部导航栏 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: white;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.chat-container.dark .chat-header {
  background: #1f2937;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.header-left .logo {
  font-size: 20px;
  font-weight: bold;
  color: #2563eb;
}

.chat-container.dark .header-left .logo {
  color: #60a5fa;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.new-chat-btn {
  width: 40px;
  height: 40px;
  padding: 0;
}

.theme-btn {
  width: 40px;
  height: 40px;
  padding: 0;
}

/* 聊天内容区域 */
.chat-main {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.chat-container.dark .chat-main {
  background: #1f2937;
}

/* 滚动条样式 */
.chat-main::-webkit-scrollbar {
  width: 6px;
}

.chat-main::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.chat-container.dark .chat-main::-webkit-scrollbar-track {
  background: #374151;
}

.chat-main::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.chat-container.dark .chat-main::-webkit-scrollbar-thumb {
  background: #6b7280;
}

/* 消息包装器 */
.message-wrapper {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
}

/* 消息行 */
.message-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.user-row {
  flex-direction: row-reverse;
}

/* 头像 */
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.user-avatar {
  background: #dbeafe;
  color: #2563eb;
}

.chat-container.dark .user-avatar {
  background: #1e40af;
  color: #93c5fd;
}

.ai-avatar {
  background: #f3f4f6;
  color: #6b7280;
}

.chat-container.dark .ai-avatar {
  background: #374151;
  color: #9ca3af;
}

/* 消息气泡 */
.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
}

.user-bubble {
  background: #2563eb;
  color: white;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background: white;
  color: #1f2937;
  border: 1px solid #e5e7eb;
  border-bottom-left-radius: 4px;
}

.dark-ai-bubble {
  background: #374151;
  color: #f3f4f6;
  border-color: #4b5563;
}

/* 加载动画 */
.loading-dots {
  display: flex;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d1d5db;
  animation: pulse 1.5s infinite;
}

.chat-container.dark .dot {
  background: #9ca3af;
}

.delay-100 {
  animation-delay: 0.1s;
}

.delay-200 {
  animation-delay: 0.2s;
}

@keyframes pulse {
  0%, 100% {
    opacity: 0.5;
  }
  50% {
    opacity: 1;
  }
}

/* 消息内容 */
.message-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.message-content span.fade-in {
  opacity: 1;
}

.message-content span:not(.fade-in) {
  opacity: 0;
}

/* 打字光标 */
.typing-cursor::after {
  content: "|";
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  from, to {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}

/* 底部输入框 */
.chat-footer {
  padding: 16px;
  background: white;
  border-top: 1px solid #e5e7eb;
}

.chat-container.dark .chat-footer {
  background: #1f2937;
  border-top-color: #374151;
}

.input-wrapper {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.chat-input {
  flex: 1;
  min-height: 44px;
  max-height: 200px;
  padding: 12px 16px;
  padding-right: 12px;
  border: 1px solid #d1d5db;
  border-radius: 12px;
  resize: none;
  font-size: 14px;
  transition: height 0.2s, border-color 0.2s, box-shadow 0.2s;
  background: white;
  color: #1f2937;
}

.chat-input:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.chat-container.dark .chat-input {
  background: #374151;
  border-color: #4b5563;
  color: #f3f4f6;
}

.chat-container.dark .chat-input::placeholder {
  color: #9ca3af;
}

.chat-container.dark .chat-input:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.send-btn {
  width: 44px;
  height: 44px;
  padding: 0;
  flex-shrink: 0;
}

/* 上传按钮 */
.upload-btn {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 12px;
  background: #f3f4f6;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.2s, color 0.2s;
}

.upload-btn:hover {
  background: #e5e7eb;
  color: #374151;
}

.chat-container.dark .upload-btn {
  background: #374151;
  color: #9ca3af;
}

.chat-container.dark .upload-btn:hover {
  background: #4b5563;
  color: #d1d5db;
}

/* 已上传文件列表 */
.files-list {
  max-width: 900px;
  margin: 0 auto 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 文件项 */
.file-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f3f4f6;
  border-radius: 8px;
  font-size: 13px;
}

.chat-container.dark .file-item {
  background: #374151;
}

.file-item i {
  color: #2563eb;
  font-size: 14px;
}

.file-name {
  color: #374151;
  font-weight: 500;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-container.dark .file-name {
  color: #d1d5db;
}

.file-size {
  color: #9ca3af;
  font-size: 12px;
}

.remove-file-btn {
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  padding: 0;
  margin-left: 4px;
  transition: color 0.2s;
}

.remove-file-btn:hover {
  color: #ef4444;
}

/* 隐藏的文件选择器 */
.hidden-file-input {
  display: none;
}
</style>