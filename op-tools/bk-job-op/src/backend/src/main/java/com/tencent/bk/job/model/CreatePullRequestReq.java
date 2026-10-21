package com.tencent.bk.job.model;

import lombok.Data;

@Data
public class CreatePullRequestReq {

    private Long prId;
    private Long issueId;
    private String codeDir;
    private String status;
    private String agentName;
    private String authorName;
    private String imateConversationId;
    private String imateConversationTitle;
}
