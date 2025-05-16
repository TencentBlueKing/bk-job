package com.tencent.bk.job.manage.util;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;

import java.util.function.BooleanSupplier;

public class AssertUtil {

    public static void scriptAvailable(BooleanSupplier check) throws NotFoundException {
        if (!check.getAsBoolean()) {
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }
    }
}
