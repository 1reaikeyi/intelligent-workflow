package service;

import model.vo.ChatTittleVO;
import model.vo.MessageVO;

import java.util.List;
import java.util.Map;

public interface ChatSessionService {
    /**
     * 根据会话id查询消息列表
     *
     * @param sessionId 会话id
     * @return 消息列表
     */
    List<MessageVO> queryBySessionId(String sessionId);

    /**
     * 更新历史会话标题
     *
     * @param sessionId 会话id
     * @param title     标题
     */
    void updateTitle(String sessionId, String title);
    /**
     * 查询历史会话列表
     */
    Map<String, List<ChatTittleVO>> queryHistorySession();

    /**
     * 删除历史会话
     *
     * @param sessionId 会话ID
     */
    void deleteHistorySession(String sessionId);
}
