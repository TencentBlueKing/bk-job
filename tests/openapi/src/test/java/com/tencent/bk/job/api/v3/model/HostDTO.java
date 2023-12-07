package com.tencent.bk.job.api.v3.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 主机
 */
@Setter
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
public class HostDTO {

    @JsonProperty("bk_host_id")
    private Long hostId;

    @JsonProperty("bk_cloud_id")
    private Long bkCloudId;

    private String ip;

    private String ipv6;

    public HostDTO(Long hostId) {
        this.hostId = hostId;
    }

    public HostDTO(Long bkCloudId, String ip) {
        this.bkCloudId = bkCloudId;
        this.ip = ip;
    }

    public HostDTO(Long hostId, Long bkCloudId, String ip) {
        this.hostId = hostId;
        this.bkCloudId = bkCloudId;
        this.ip = ip;
    }
}
