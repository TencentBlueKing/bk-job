package com.tencent.bk.job.common.iam.service;

import com.tencent.bk.sdk.iam.constants.CommonResponseCode;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeValueResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceByPolicyResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseIamCallbackService {

    public CallbackBaseResponseDTO getFailResp(Long code, String message) {
        CallbackBaseResponseDTO respDTO = new CallbackBaseResponseDTO();
        respDTO.setCode(code);
        respDTO.setMessage(message);
        return respDTO;
    }

    public CallbackBaseResponseDTO getNotFoundResp(String message) {
        return getFailResp(CommonResponseCode.NOT_FOUND, message);
    }

    public CallbackBaseResponseDTO getNotFoundRespById(String id) {
        String msg = String.format("cannot find resource by id %d, may be deleted", id);
        log.warn(msg);
        return getNotFoundResp(msg);
    }

    protected abstract ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest);

    protected abstract SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest);

    protected abstract CallbackBaseResponseDTO fetchInstanceResp(CallbackRequestDTO callbackRequest);

    public CallbackBaseResponseDTO baseCallback(CallbackRequestDTO callbackRequest) {
        CallbackBaseResponseDTO response;
        switch (callbackRequest.getMethod()) {
            case LIST_INSTANCE:
                response = listInstanceResp(callbackRequest);
                break;
            case FETCH_INSTANCE_INFO:
                response = fetchInstanceResp(callbackRequest);
                break;
            case LIST_ATTRIBUTE:
                response = new ListAttributeResponseDTO();
                response.setCode(0L);
                break;
            case LIST_ATTRIBUTE_VALUE:
                response = new ListAttributeValueResponseDTO();
                response.setCode(0L);
                break;
            case LIST_INSTANCE_BY_POLICY:
                response = new ListInstanceByPolicyResponseDTO();
                response.setCode(0L);
                break;
            case SEARCH_INSTANCE:
                response = searchInstanceResp(callbackRequest);
                break;
            default:
                log.error("Unknown callback method!|{}|{}|{}|{}", callbackRequest.getMethod(),
                    callbackRequest.getType(), callbackRequest.getFilter(), callbackRequest.getPage());
                response = new CallbackBaseResponseDTO();
        }
        return response;
    }
}
