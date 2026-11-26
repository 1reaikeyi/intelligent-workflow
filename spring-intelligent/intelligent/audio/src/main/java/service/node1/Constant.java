package service.node1;

public interface Constant {
    String question = "question";
    String story = "story";
    String judge = "judge";
    String read = "read";
    /** 已重写次数，用于防止死循环 */
    String RetryCount = "retryCount";
    /** 条件边：审核通过 */
    String RoutePass = "pass";
    /** 条件边：审核不通过，回到 StoryNode 重写 */
    String RouteRetry = "retry";
    /** 最大重写次数 */
    int MaxRetry = 3;
}
