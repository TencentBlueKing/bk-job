package com.tencent.bk.job.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ReleaseStatusResp {
    /**
     * 是否正在发布
     */
    private boolean releasing;
}
