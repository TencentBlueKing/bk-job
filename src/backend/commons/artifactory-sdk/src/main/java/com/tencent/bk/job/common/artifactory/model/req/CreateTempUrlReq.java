package com.tencent.bk.job.common.artifactory.model.req;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTempUrlReq extends ArtifactoryReq {
    // 必传，项目Id
    String projectId;
    // 必传，仓库名称
    String repoName;
    // 必传，授权路径列表，支持批量创建
    List<String> fullPathSet;
    // 非必传，授权访问用户，不传则任意用户可访问
    List<String> authorizedUserSet;
    // 非必传，授权访问ip，不传则任意ip可访问
    List<String> authorizedIpSet;
    // 非必传，token有效时间，单位秒，小于等于0则永久有效
    Long expireSeconds = 30 * 60L;
    // 非必传，允许访问次数，null表示无限制
    Integer permits = 1;
    // 非必传，token类型。UPLOAD:允许上传, DOWNLOAD: 允许下载, ALL: 同时允许上传和下载
    String type = "UPLOAD";
    // 非必传，自定义分享链接host，不指定则使用系统默认host
    String host;
    // 非必传，是否通知授权访问用户
    Boolean needsNotify;
}
