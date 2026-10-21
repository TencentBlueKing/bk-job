package com.tencent.bk.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class PullRequestResp {

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
