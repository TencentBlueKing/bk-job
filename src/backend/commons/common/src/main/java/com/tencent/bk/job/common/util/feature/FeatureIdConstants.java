package com.tencent.bk.job.common.util.feature;

/**
 * 特性ID定义
 */
public interface FeatureIdConstants {
    /**
     * 特性: 对接 GSE2.0
     */
    String FEATURE_GSE_V2 = "gseV2";
    /**
     * 特性: OpenAPI 兼容bk_biz_id参数
     */
    String FEATURE_BK_BIZ_ID_COMPATIBLE = "bkBizIdCompatible";
    /**
     * 特性-第三方文件源
     */
    String FEATURE_FILE_MANAGE = "fileManage";

    /**
     * 特性-是否支持GSE 获取文件分发任务结果的API协议(2.0版本之前)
     */
    String GSE_FILE_PROTOCOL_BEFORE_V2 = "gseFileProtocolBeforeV2";
}
