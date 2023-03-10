package com.tencent.bk.job.common.esb.sdk;

import com.tencent.bk.job.common.util.json.JsonUtils;
import org.slf4j.Logger;

/**
 * 自定义 BK API 调用日志输出策略
 */
public interface BkApiLogStrategy {
    /**
     * 打印请求
     *
     * @param log     logger
     * @param context BK Open API 调用上下文
     * @param <T>     请求
     * @param <R>     响应对象
     */
    default <T, R> void logReq(Logger log, BkApiContext<T, R> context) {
        if (log.isInfoEnabled()) {
            log.info("Request|method={}|uri={}|reqStr={}", context.getMethod(),
                context.getUri(), JsonUtils.toJsonWithoutSkippedFields(context.getReq()));
        }
    }

    /**
     * 打印响应
     *
     * @param log     logger
     * @param context BK Open API 调用上下文
     * @param <T>     请求
     * @param <R>     响应对象
     */
    default <T, R> void logResp(Logger log, BkApiContext<T, R> context) {
        if (log.isInfoEnabled()) {
            log.info("Response|method={}|uri={}|success={}|costTime={}|resp={}|",
                context.getMethod(), context.getUri(), context.isSuccess(),
                context.getCostTime(), context.getOriginResp());
        }
    }
}
