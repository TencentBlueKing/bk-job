package com.tencent.bk.job.common.web.exception.handler;

import com.tencent.bk.job.common.annotation.IamCallbackAPI;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.I18nUtil;
import com.tencent.bk.sdk.iam.constants.CommonResponseCode;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

/**
 * 处理权限中心回调接口异常
 */
@ControllerAdvice(annotations = {IamCallbackAPI.class})
@Slf4j
public class IamCallbackExceptionControllerAdvice extends ExceptionControllerAdviceBase {

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    ResponseEntity<?> handleException(HttpServletRequest request, Throwable ex) {
        log.error("Handle Throwable", ex);
        CallbackBaseResponseDTO responseDTO = new CallbackBaseResponseDTO();
        responseDTO.setCode(CommonResponseCode.SYSTEM_ERROR);
        responseDTO.setMessage(I18nUtil.getI18nMessage(String.valueOf(ErrorCode.INTERNAL_ERROR)));
        return new ResponseEntity<>(responseDTO, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidParamException.class)
    @ResponseBody
    ResponseEntity<?> handleServiceException(HttpServletRequest request, InvalidParamException ex) {
        log.info("Handle InvalidParamException", ex);
        CallbackBaseResponseDTO responseDTO = new CallbackBaseResponseDTO();
        responseDTO.setCode(CommonResponseCode.PARAMS_INVALID);
        responseDTO.setMessage(ex.getI18nMessage());
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex), HttpStatus.BAD_REQUEST);
    }

}
