package com.tencent.bk.job.common.paas.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SendVoiceReq {

    /**
     * 自动语音读字信息
     */
    @JsonProperty("auto_read_message")
    private String message;

    /**
     * 待通知的用户列表，包含用户名，多个以逗号分隔
     */
    @JsonProperty("receiver__username")
    public String receivers;
}
