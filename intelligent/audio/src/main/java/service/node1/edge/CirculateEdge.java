package service.node1.edge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import org.springframework.stereotype.Component;
import service.node1.Constant;


@Component
public class CirculateEdge implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        String judge = state.value(Constant.judge, "");
        int retryCount = state.value(Constant.RetryCount, 0);
        if ("PASS".equals(judge)) {
            return Constant.RoutePass;
        }
        if (retryCount >= Constant.MaxRetry) {
            return Constant.RoutePass;
        }
        return Constant.RouteRetry;
    }
}
