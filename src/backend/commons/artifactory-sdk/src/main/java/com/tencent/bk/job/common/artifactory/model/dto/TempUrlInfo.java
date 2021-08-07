package com.tencent.bk.job.common.artifactory.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class TempUrlInfo {
    String projectId;
    String repoName;
    String fullPath;
    String url;
    List<String> authorizedUserSet;
    List<String> authorizedIpSet;
    String expireDate;
    Integer permits;
    String type;
}
