package com.tencent.bk.job.model;

import lombok.Data;

@Data
public class PullRequestDTO {

    private Long id;
    private Long prId;
    private Long issueId;
    private String codeDir;
    private String status;
    private String agentName;
    private String authorName;
    private String imateConversationId;
    private String imateConversationTitle;
    private Long createTime;
    private Long updateTime;
}
