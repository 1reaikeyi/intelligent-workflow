package service.memory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import constants.Constant;
import org.springframework.ai.chat.messages.*;
import service.tools.ToolResultHolder;

/**
 * 消息转换工具类，提供消息对象与JSON字符串之间的转换功能
 */
public class MessageUtil {

    public static String toJson(Message message, String sessionId) {
        var myMessage = BeanUtil.toBean(message, MessageBO.class);
        // 设置消息内容
        myMessage.setTextContent(message.getText());
        myMessage.setSessionId(sessionId);
        if (message instanceof AssistantMessage assistantMessage) {
            myMessage.setToolCalls(assistantMessage.getToolCalls());
            // 通过 messageId 获取 requestId，再通过 requestId 获取参数列表，如果有，就存储起来
            // 最后，删除 messageId 对应的数据
            var messageId = Convert.toStr(assistantMessage.getMetadata().get(Constant.ID));
            var requestId = Convert.toStr(ToolResultHolder.get(messageId, Constant.REQUEST_ID));
            var params = ToolResultHolder.get(requestId);
            if (ObjectUtil.isNotEmpty(params)) {
                myMessage.setParams(params);
            }
            ToolResultHolder.remove(messageId);
        }

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            myMessage.setToolResponses(toolResponseMessage.getResponses());
        }

        return JSONUtil.toJsonStr(myMessage);
    }

    public static Message toMessage(String json) {
        var myMessage = JSONUtil.toBean(json, MessageBO.class);
        var messageType = MessageType.valueOf(myMessage.getMessageType());
        switch (messageType) {
            case SYSTEM -> {
                return new SystemMessage(myMessage.getTextContent());
            }
            case USER -> {
                return UserMessage.builder()
                        .text(myMessage.getTextContent())
                        .metadata(myMessage.getMetadata())
                        .media(myMessage.getMedia())
                        .build();
            }
            case ASSISTANT -> {
                return new AssistantMessageBO(
                        myMessage.getTextContent(),
                        myMessage.getMetadata(),
                        myMessage.getToolCalls(),
                        myMessage.getMedia(),
                        myMessage.getParams());
            }
            case TOOL -> {
                return ToolResponseMessage.builder()
                        .responses(myMessage.getToolResponses())
                        .metadata(myMessage.getMetadata())
                        .build();
            }
        }

        throw new RuntimeException("Message data conversion failed.");
    }

/**
 * ● AssistantMessage 		大模型生成的消息
 * ● SystemMessage  		系统消息
 * ● ToolResponseMessage	工具响应消息
 * ● UserMessage  		用户消息
 */

}
