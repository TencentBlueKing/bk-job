package com.tencent.bk.job.api.v3.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author citruswang
 * @since 17/11/2020 21:47
 */
@Data
@NoArgsConstructor
public class EsbFileDestinationV3DTO {
    private String path;

    private EsbAccountV3BasicDTO account;

    private EsbServerV3DTO server;
}
