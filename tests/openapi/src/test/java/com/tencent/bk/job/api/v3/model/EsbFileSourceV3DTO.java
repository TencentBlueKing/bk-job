package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 源文件定义-ESB
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class EsbFileSourceV3DTO {
    /**
     * 文件列表
     */
    @JsonProperty("file_list")
    private List<String> files;

    /**
     * 账号
     */
    private EsbAccountV3BasicDTO account;

    @JsonProperty("server")
    private EsbServerV3DTO server;

    /**
     * 文件源类型，不传默认为服务器文件
     *
     * 1：服务器文件，2：本地文件，3：第三方文件源文件
     */
    @JsonProperty("file_type")
    private Integer fileType;

    /**
     * 从文件源分发的文件源Id，非文件源类型可不传
     */
    @JsonProperty("file_source_id")
    private Integer fileSourceId;

    /**
     * 从文件源分发的文件源标识，非文件源类型可不传
     */
    @JsonProperty("file_source_code")
    private String fileSourceCode;
}
