package com.tencent.bk.job.model;

import lombok.Data;

@Data
public class UpdatePullRequestStatusReq {

    private Long prId;
    private String status;
}
