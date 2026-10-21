package com.tencent.bk.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class WebIssueStatusResp {

    private String id;
    private String status;
    private String statusDescription;
    private Long updateTime;
    private String prStatus;
    private Long prId;
}
