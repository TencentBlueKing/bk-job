package com.tencent.bk.job.model;

import java.util.Arrays;

public enum PullRequestStatusEnum {
    WAIT_FOR_REVIEW,
    HANDLING,
    MERGED,
    CLOSED;

    public static PullRequestStatusEnum fromValue(String value) {
        return Arrays.stream(values())
            .filter(status -> status.name().equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("invalid pull request status: " + value));
    }
}
