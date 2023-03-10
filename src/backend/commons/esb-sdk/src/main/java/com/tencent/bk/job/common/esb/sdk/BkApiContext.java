package com.tencent.bk.job.common.esb.sdk;

import com.tencent.bk.job.common.esb.model.EsbResp;
import lombok.Data;

/**
 * BK Open API 调用上下文
 *
 * @param <T> 请求
 * @param <R> 响应对象
 */
@Data
public class BkApiContext<T, R> {
    /**
     * HTTP 请求方法
     */
    private String method;
    private String uri;
    private T req;
    /**
     * 原始的 API 响应
     */
    private String originResp;
    /**
     * 反序列化之后的 API 响应
     */
    private EsbResp<R> resp;
    /**
     * API 调用耗时
     */
    private long costTime;
    /**
     * API 是否调用成功并正确响应
     */
    private boolean success;

    public BkApiContext(String method,
                        String uri,
                        T req,
                        String originResp,
                        EsbResp<R> resp,
                        long costTime,
                        boolean success) {
        this.method = method;
        this.uri = uri;
        this.req = req;
        this.originResp = originResp;
        this.resp = resp;
        this.costTime = costTime;
        this.success = success;
    }
}
