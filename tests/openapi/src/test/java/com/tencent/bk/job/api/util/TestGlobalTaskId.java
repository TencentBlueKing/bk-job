package com.tencent.bk.job.api.util;

/**
 * 每次 API 测试生成的任务 ID
 */
public class TestGlobalTaskId {
    private static String taskId;

    public static String get() {
        if (taskId == null) {
            synchronized (TestGlobalTaskId.class) {
                if (taskId == null) {
                    taskId = TestValueGenerator.generateRandomString(5);
                }
            }
        }
        return taskId;
    }
}
