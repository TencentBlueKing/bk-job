package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.execute.common.constants.FileTransferModeEnum;

public interface FileTransferModeService {

    FileTransferModeEnum getTransferMode(Integer duplicateHandler, Integer notExistPathHandler);
}
