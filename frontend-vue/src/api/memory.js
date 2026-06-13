import { baseURL } from '@/utils/request' // 引入 baseURL ('/api')

let controller = null;

/**
 * 创建一次工具对话会话（无状态，每次 send 独立）
 */
export function createMemoryChat() {
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
        async send(memoryId, message, { onStart, onChunk, onDone, onError } = {}) {
            // 先中止上一次未完成的请求
            this.abort();

            controller = new AbortController();
            const signal = controller.signal;

            try {
                onStart?.();

                // 关键修改：拼接 baseURL，使得请求路径变为 /api/tool?...
                const url = `${baseURL}/memory?message=${encodeURIComponent(message)}&memoryId=${encodeURIComponent(memoryId)}`;
                const response = await fetch(url, { signal });

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
                    console.log('memory请求已取消');
                } else {
                    console.error('memory请求出错:', err);
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
