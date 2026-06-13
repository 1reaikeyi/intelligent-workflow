<template>
  <div class="app-container" :class="{ 'dark-mode': darkMode }">
    <!-- 顶部导航栏 -->
    <header class="app-header">
      <div class="header-left">
        <div class="app-logo">Qwen</div>
      </div>
      <div class="header-actions">
        <button
            class="btn-icon btn-new-session"
            title="新对话"
            @click="startNewConversation"
        >
          <!-- 替换为 Element Plus Plus 图标 -->
          <el-icon><Plus /></el-icon>
        </button>
        <button
            class="btn-icon btn-theme"
            title="切换主题"
            @click="toggleDarkMode"
        >
          <!-- 替换为 Element Plus Moon/Sunny 图标 -->
          <el-icon>
            <Moon v-if="darkMode" />
            <Sunny v-else />
          </el-icon>
        </button>
      </div>
    </header>

    <!-- 聊天内容区域 -->
    <main ref="chatContainer" class="chat-main">
      <div class="chat-wrapper">
        <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-row"
            :class="message.role === 'user' ? 'message-row--user' : 'message-row--assistant'"
        >
          <div
              class="message-inner"
              :class="message.role === 'user' ? 'message-inner--user' : 'message-inner--assistant'"
          >
            <!-- 头像 -->
            <div v-if="message.role === 'user'" class="avatar avatar--user">
              <!-- 替换为 Element Plus User 图标 -->
              <el-icon><User /></el-icon>
            </div>
            <div v-else class="avatar avatar--assistant">
              <img src="@/assets/logo.png" alt="Logo" />
            </div>

            <!-- 消息气泡 -->
            <div
                class="message-bubble"
                :class="[
                message.role === 'user' ? 'message-bubble--user' : 'message-bubble--assistant',
                { 'message-bubble--dark': darkMode && message.role !== 'user' }
              ]"
            >
              <!-- 加载动画 -->
              <div v-if="message.role === 'assistant' && message.isLoading" class="loading-dots">
                <span class="dot"></span>
                <span class="dot delay-100"></span>
                <span class="dot delay-200"></span>
              </div>
              <!-- 消息内容 -->
              <div v-else class="message-content">
                <span
                    v-for="(char, charIndex) in message.content"
                    :key="charIndex"
                    :class="{
                    'char-hidden': charIndex >= message.visibleChars,
                    'char-visible': charIndex < message.visibleChars
                  }"
                >{{ char }}</span>
                <span v-if="message.isStreaming" class="typing-cursor"></span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 输入框区域 -->
    <footer class="app-footer" :class="{ 'footer--dark': darkMode }">
      <div class="input-container">
        <div class="input-row">
          <textarea
              ref="textareaRef"
              v-model="userInput"
              class="msg-input"
              :class="{ 'input--dark': darkMode }"
              placeholder="输入您的问题..."
              rows="1"
              @keydown.enter.exact.prevent="sendMessage"
              @keydown.ctrl.enter.exact.prevent="sendMessage"
              @keydown.esc.exact="stopResponse"
              @input="adjustTextareaHeight"
          ></textarea>
          <button
              class="btn-send"
              :class="isLoading ? 'btn-stop' : 'btn-go'"
              :disabled="!userInput.trim() && !isLoading"
              @click="isLoading ? stopResponse() : sendMessage()"
          >
            <!-- 替换为 Element Plus 发送/停止 图标 -->
            <el-icon>
              <VideoPause v-if="isLoading" />
              <Promotion v-else />
            </el-icon>
          </button>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted, watch } from 'vue';
import { createRagChat } from '../api/rag.js';
// 引入所需的 Element Plus 图标
import { Plus, Moon, Sunny, User, VideoPause, Promotion } from '@element-plus/icons-vue';

// ---- 响应式状态 ----
const messages = ref([]);
const userInput = ref('');
const isLoading = ref(false);
const darkMode = ref(false);
const chatContainer = ref(null);
const textareaRef = ref(null);
const memoryId = ref(Date.now().toString());

// ---- 外部请求层 ----
const chat = createRagChat();
let typingTimer = null;

// ---- 工具函数 ----
const adjustTextareaHeight = () => {
  const el = textareaRef.value;
  if (!el) return;
  el.style.height = 'auto';
  el.style.height = `${Math.min(el.scrollHeight, 200)}px`;
};

const scrollToBottom = () => {
  nextTick(() => {
    const container = chatContainer.value;
    if (container) {
      container.scrollTop = container.scrollHeight;
    }
  });
};

// ---- 暗黑模式 ----
const toggleDarkMode = () => {
  darkMode.value = !darkMode.value;
  localStorage.setItem('darkMode', String(darkMode.value));
};

// ---- 新建会话 ----
const startNewConversation = () => {
  chat.abort();
  messages.value = [];
  memoryId.value = Date.now().toString();
  messages.value.push({
    role: 'assistant',
    content: '你好！我是Qwen，请问有什么能帮到您？',
    isLoading: false,
    visibleChars: 0,
    isStreaming: false
  });
  messages.value[0].visibleChars = messages.value[0].content.length;
  scrollToBottom();
  nextTick(() => textareaRef.value?.focus());
};

// ---- 逐字打印动画 ----
const startTyping = (msgIndex) => {
  const msg = messages.value[msgIndex];
  if (!msg || msg.visibleChars >= msg.content.length) {
    clearInterval(typingTimer);
    typingTimer = null;
    if (msg) msg.isStreaming = false;
    return;
  }
  msg.visibleChars++;
  scrollToBottom();
};

// ---- 清理状态 ----
const cleanupTyping = (msgIndex) => {
  clearInterval(typingTimer);
  typingTimer = null;
  const msg = messages.value[msgIndex];
  if (msg) {
    msg.isLoading = false;
    msg.isStreaming = false;
    if (msg.visibleChars < msg.content.length) {
      msg.visibleChars = msg.content.length;
    }
  }
  isLoading.value = false;
  scrollToBottom();
};

// ---- 发送消息 ----
const sendMessage = async () => {
  const input = userInput.value.trim();
  if (!input || isLoading.value) return;

  // 用户消息
  messages.value.push({
    role: 'user',
    content: input,
    isLoading: false,
    visibleChars: input.length,
    isStreaming: false
  });

  // 占位助手消息
  const placeholderIdx = messages.value.push({
    role: 'assistant',
    content: '',
    isLoading: true,
    visibleChars: 0,
    isStreaming: true
  }) - 1;

  userInput.value = '';
  adjustTextareaHeight();
  scrollToBottom();
  isLoading.value = true;

  await chat.send(memoryId.value, input, {
    onStart() {
      if (typingTimer) {
        clearInterval(typingTimer);
        typingTimer = null;
      }
    },
    onChunk(fullText) {
      messages.value[placeholderIdx].content = fullText;
      messages.value[placeholderIdx].isLoading = false;
      if (!typingTimer) {
        typingTimer = setInterval(() => startTyping(placeholderIdx), 20);
      }
      scrollToBottom();
    },
    onDone() {
      cleanupTyping(placeholderIdx);
    },
    onError(err) {
      const msg = messages.value[placeholderIdx];
      msg.content = '抱歉，请求过程中出现错误: ' + err.message;
      msg.visibleChars = msg.content.length;
      cleanupTyping(placeholderIdx);
    }
  });
};

// ---- 停止响应 ----
const stopResponse = () => {
  chat.abort();
  const lastIdx = messages.value.length - 1;
  cleanupTyping(lastIdx);
};

// ---- 生命周期 ----
onMounted(() => {
  // 主题初始化
  const saved = localStorage.getItem('darkMode');
  darkMode.value = saved === 'true';

  // 欢迎消息
  messages.value.push({
    role: 'assistant',
    content: '你好！我是Qwen，请问有什么能帮到您？',
    isLoading: false,
    visibleChars: 0,
    isStreaming: false
  });
  messages.value[0].visibleChars = messages.value[0].content.length;
  scrollToBottom();
  nextTick(() => textareaRef.value?.focus());
});

// 自动滚动
watch(messages, scrollToBottom, { deep: true });
</script>

<style scoped>
/* ========== 全局重置 ========== */
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
  background: #f9fafb;
  color: #111827;
  transition: background 0.3s, color 0.3s;
}

.app-container.dark-mode {
  background: #1f2937;
  color: #e5e7eb;
}

/* ========== 滚动条 ========== */
.chat-main::-webkit-scrollbar {
  width: 6px;
}

.chat-main::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.chat-main::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.chat-main::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* ========== Header ========== */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,.08);
  flex-shrink: 0;
}

.app-logo {
  font-size: 20px;
  font-weight: 700;
  color: #2563eb;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.btn-icon {
  width: 40px;
  height: 40px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background .2s;
  /* 确保 Element Plus 图标继承颜色和大小 */
  font-size: 20px;
}

.btn-new-session {
  background: #22c55e;
  color: #fff;
}

.btn-new-session:hover {
  background: #16a34a;
}

.btn-theme {
  background: transparent;
  color: #4b5563;
}

.btn-theme:hover {
  background: #f3f4f6;
}

/* ========== 聊天区域 ========== */
.chat-main {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.chat-wrapper {
  max-width: 768px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.message-row--user {
  display: flex;
  justify-content: flex-end;
}

.message-row--assistant {
  display: flex;
  justify-content: flex-start;
}

.message-inner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.message-inner--user {
  flex-direction: row-reverse;
}

/* ========== 头像 ========== */
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.avatar--user {
  background: #dbeafe;
  color: #2563eb;
  font-size: 18px; /* 调整 Element Plus User 图标大小 */
}

.avatar--assistant {
  background: #e5e7eb;
  overflow: hidden;
  font-size: 0;
}

.avatar--assistant img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* ========== 气泡 ========== */
.message-bubble {
  padding: 12px 16px;
  border-radius: 12px;
  max-width: 480px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.message-bubble--user {
  background: #2563eb;
  color: #fff;
}

.message-bubble--assistant {
  background: #fff;
  box-shadow: 0 1px 3px rgba(0,0,0,.06);
  border: 1px solid #f3f4f6;
}

.message-bubble--dark.message-bubble--assistant {
  background: #374151;
  border-color: #4b5563;
  color: #e5e7eb;
}

/* ========== 加载点动画 ========== */
.loading-dots {
  display: flex;
  gap: 6px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9ca3af;
  animation: pulse 1.5s infinite;
}

.dot.delay-100 {
  animation-delay: .2s;
}

.dot.delay-200 {
  animation-delay: .4s;
}

@keyframes pulse {
  0%, 100% { opacity: .4; }
  50% { opacity: 1; }
}

/* ========== 打字机光标 ========== */
.typing-cursor::after {
  content: '|';
  animation: blink 1s step-end infinite;
}

@keyframes blink {
  50% { opacity: 0; }
}

/* ========== 逐字显示动画 ========== */
.char-hidden { opacity: 0; }
.char-visible { opacity: 1; }

/* ========== Footer 输入区 ========== */
.app-footer {
  border-top: 1px solid #e5e7eb;
  padding: 16px;
  background: #fff;
  flex-shrink: 0;
}

.app-footer.footer--dark {
  background: #1f2937;
  border-color: #374151;
}

.input-container {
  max-width: 768px;
  margin: 0 auto;
}

.input-row {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.msg-input {
  flex: 1;
  border: 1px solid #d1d5db;
  border-radius: 10px;
  padding: 12px 16px;
  font-size: 15px;
  resize: none;
  outline: none;
  min-height: 44px;
  max-height: 200px;
  transition: height .2s, border .2s, box-shadow .2s;
  font-family: inherit;
  line-height: 1.5;
}

.msg-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37,99,235,.15);
}

.msg-input.input--dark {
  background: #374151;
  border-color: #4b5563;
  color: #e5e7eb;
}

.msg-input.input--dark::placeholder {
  color: #9ca3af;
}

.btn-send {
  width: 44px;
  height: 44px;
  border: none;
  border-radius: 10px;
  font-size: 20px; /* 调整 Element Plus 图标大小 */
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background .2s, opacity .2s;
  flex-shrink: 0;
}

.btn-go {
  background: #2563eb;
  color: #fff;
}

.btn-go:hover {
  background: #1d4ed8;
}

.btn-stop {
  background: #ef4444;
  color: #fff;
}

.btn-stop:hover {
  background: #dc2626;
}

.btn-send:disabled {
  opacity: .5;
  cursor: not-allowed;
}
</style>
