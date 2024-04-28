package com.tencent.bk.job.execute.util;

import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.tencent.bk.job.common.constant.DuplicateHandlerEnum.GROUP_BY_DATE_AND_IP;
import static com.tencent.bk.job.common.constant.DuplicateHandlerEnum.GROUP_BY_IP;
import static com.tencent.bk.job.common.constant.DuplicateHandlerEnum.OVERWRITE;
import static com.tencent.bk.job.common.constant.NotExistPathHandlerEnum.CREATE_DIR;
import static com.tencent.bk.job.common.constant.NotExistPathHandlerEnum.STEP_FAIL;

@Slf4j
@Service
public class FileTransferModeUtil {

    public static FileTransferModeEnum getTransferMode(Integer duplicateHandler, Integer notExistPathHandler) {
        return getTransferMode(
            DuplicateHandlerEnum.valueOf(duplicateHandler),
            NotExistPathHandlerEnum.valueOf(notExistPathHandler)
        );
    }

    private static FileTransferModeEnum getTransferMode(DuplicateHandlerEnum duplicateHandlerEnum,
                                                        NotExistPathHandlerEnum notExistPathHandlerEnum) {
        if (duplicateHandlerEnum == null) {
            // 默认覆盖
            duplicateHandlerEnum = OVERWRITE;
        }
        if (notExistPathHandlerEnum == null) {
            // 默认直接创建
            notExistPathHandlerEnum = CREATE_DIR;
        }
        if (OVERWRITE == duplicateHandlerEnum && STEP_FAIL == notExistPathHandlerEnum) {
            return FileTransferModeEnum.STRICT;
        } else if (OVERWRITE == duplicateHandlerEnum && CREATE_DIR == notExistPathHandlerEnum) {
            return FileTransferModeEnum.FORCE;
        } else if (GROUP_BY_IP == duplicateHandlerEnum && CREATE_DIR == notExistPathHandlerEnum) {
            return FileTransferModeEnum.SAFETY_IP_PREFIX;
        } else if (GROUP_BY_DATE_AND_IP == duplicateHandlerEnum && CREATE_DIR == notExistPathHandlerEnum) {
            return FileTransferModeEnum.SAFETY_DATE_PREFIX;
        } else {
            return FileTransferModeEnum.STRICT;
        }
    }
}
