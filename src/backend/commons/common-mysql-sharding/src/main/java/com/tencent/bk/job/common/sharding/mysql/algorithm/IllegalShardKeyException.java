package com.tencent.bk.job.common.sharding.mysql.algorithm;

/**
 * 分片键异常
 */
public class IllegalShardKeyException extends RuntimeException {

    public IllegalShardKeyException(String message) {
        super(message);
    }
}
