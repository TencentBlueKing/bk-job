package com.tencent.bk.job.execute.dao.sharding;

/**
 * 分片键异常
 */
public class IllegalShardKeyException extends RuntimeException {

    public IllegalShardKeyException(String message) {
        super(message);
    }
}
