package com.tencent.bk.job.common.cc.model.bizset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 基础业务集信息，用于生成更新CMDB业务集ID的Json数据文件
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BasicBizSet {
    @JsonProperty("biz_set_id")
    private Long id;

    @JsonProperty("biz_set_name")
    private String name;
}
