import { baseURL } from '@/utils/request' // 引入 baseURL ('/api')

let controller = null;

/**
 * 创建一次工具对话会话（无状态，每次 send 独立）
 */
export function createToolChat() {
  return {
    /**
     * 发送消息，流式读取响应
     * @param {string} memoryId   会话 ID
     * @param {string} message    用户输入
     * @param {object} callbacks  回调
     * @param {Function} callbacks.onStart   开始请求
     * @param {Function} callbacks.onChunk   收到增量文本，参数
     * @param {Function} callbacks.onDone    传输完成，参数
     * @param {Function} callbacks.onError   出错，参数
     */
    async send(memoryId, message, { onStart, onChunk, onDone, onError } = {}, files = []) {
      // 先中止上一次未完成的请求
      this.abort();

      controller = new AbortController();
      const signal = controller.signal;

      try {
        onStart?.();

        // 构建 FormData
        const formData = new FormData();
        formData.append('question', message);

        // 添加文件（确保至少有一个文件）
        files.forEach(file => {
          formData.append('file', file.file);
        });

        const url = `${baseURL}/see`;
        const response = await fetch(url, {
          signal,
          method: 'POST',
          body: formData
        });

        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder('utf-8');
        let buffer = '';

        while (true) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          onChunk?.(buffer);
        }

        onDone?.(buffer);
      } catch (err) {
        if (err.name === 'AbortError') {
          console.log('tool请求已取消');
        } else {
          console.error('tool请求出错:', err);
          onError?.(err);
        }
      } finally {
        controller = null;
      }
    },

    /**
     * 中止当前请求
     */
    abort() {
      if (controller) {
        controller.abort();
        controller = null;
      }
    },

    /**
     * 是否有正在进行的请求
     */
    get isPending() {
      return controller !== null;
    }
  };
}
