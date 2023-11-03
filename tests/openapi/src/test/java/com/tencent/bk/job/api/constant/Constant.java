package com.tencent.bk.job.api.constant;

import static com.tencent.bk.job.api.constant.ErrorCode.BAD_REQUEST;
import static com.tencent.bk.job.api.constant.ErrorCode.ILLEGAL_PARAM;
import static com.tencent.bk.job.api.constant.ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME;
import static com.tencent.bk.job.api.constant.ErrorCode.MISSING_OR_ILLEGAL_PARAM;
import static com.tencent.bk.job.api.constant.ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME;
import static com.tencent.bk.job.api.constant.ErrorCode.MISSING_PARAM;
import static com.tencent.bk.job.api.constant.ErrorCode.MISSING_PARAM_WITH_PARAM_NAME;

public class Constant {
    public static Integer[] ILLEGAL_OR_MISSING_PARAM_ERROR_ARRAY = new Integer[]{
        ILLEGAL_PARAM, MISSING_PARAM, MISSING_OR_ILLEGAL_PARAM, ILLEGAL_PARAM_WITH_PARAM_NAME,
        MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, MISSING_PARAM_WITH_PARAM_NAME, BAD_REQUEST};

    /**
     * shell 脚本内容
     * #!/bin/bash
     * echo '123'
     */
    public static String SHELL_SCRIPT_CONTENT_BASE64 = "IyEvYmluL2Jhc2gKZWNobyAnMTIzJwo=";

    /**
     * shell 高危脚本内容
     * #!/bin/bash
     * rm -rf /tmp
     */
    public static String SHELL_DANGEROUS_SCRIPT_CONTENT_BASE64 = "IyEvYmluL2Jhc2gKcm0gLXJmIC90bXA=";

}
