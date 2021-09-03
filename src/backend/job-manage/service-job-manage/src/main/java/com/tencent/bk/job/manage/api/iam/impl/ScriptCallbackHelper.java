package com.tencent.bk.job.manage.api.iam.impl;

import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptQueryDTO;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListAttributeValueResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceByPolicyResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ScriptCallbackHelper {
    private ScriptService scriptService;
    private IGetBasicQueryCondition basicQueryInterface;

    public ScriptCallbackHelper(
        ScriptService scriptService,
        IGetBasicQueryCondition basicQueryInterface
    ) {
        this.scriptService = scriptService;
        this.basicQueryInterface = basicQueryInterface;
    }

    public interface IGetBasicQueryCondition {
        Pair<ScriptQueryDTO, BaseSearchCondition> getBasicQueryCondition(CallbackRequestDTO callbackRequest);
    }


    private InstanceInfoDTO convert(ScriptDTO script) {
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(String.valueOf(script.getId()));
        instanceInfo.setDisplayName(script.getName());
        return instanceInfo;
    }

    public ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        Pair<ScriptQueryDTO, BaseSearchCondition> basicQueryCond =
            basicQueryInterface.getBasicQueryCondition(callbackRequest);

        ScriptQueryDTO scriptQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();
        PageData<ScriptDTO> accountDTOPageData = scriptService.listPageScript(scriptQuery,
            baseSearchCondition);

        return IamRespUtil.getListInstanceRespFromPageData(accountDTOPageData, this::convert);
    }

    public SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {

        Pair<ScriptQueryDTO, BaseSearchCondition> basicQueryCond =
            basicQueryInterface.getBasicQueryCondition(callbackRequest);
        ScriptQueryDTO scriptQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();

        scriptQuery.setName(callbackRequest.getFilter().getKeyword());
        PageData<ScriptDTO> accountDTOPageData = scriptService.listPageScript(scriptQuery,
            baseSearchCondition);

        return IamRespUtil.getSearchInstanceRespFromPageData(accountDTOPageData, this::convert);
    }

    public CallbackBaseResponseDTO doCallback(CallbackRequestDTO callbackRequest){
        CallbackBaseResponseDTO response;
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        switch (callbackRequest.getMethod()) {
            case LIST_INSTANCE:
                response = listInstanceResp(callbackRequest);
                break;
            case FETCH_INSTANCE_INFO:
                log.debug("Fetch instance info request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());

                List<Object> instanceAttributeInfoList = new ArrayList<>();
                for (String instanceId : searchCondition.getIdList()) {
                    try {
                        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                        instanceInfo.setId(instanceId);
                        ScriptDTO scriptDTO = scriptService.getScriptByScriptId(instanceId);
                        if (scriptDTO != null) {
                            instanceInfo.setDisplayName(scriptDTO.getName());
                        } else {
                            instanceInfo.setDisplayName("Unknown(may be deleted)");
                            log.warn("Unexpected scriptId:{} passed by iam", instanceId);
                        }
                        instanceAttributeInfoList.add(instanceInfo);
                    } catch (NumberFormatException e) {
                        log.error("Parse object id failed!|{}", instanceId, e);
                    }
                }

                FetchInstanceInfoResponseDTO fetchInstanceInfoResponse = new FetchInstanceInfoResponseDTO();
                fetchInstanceInfoResponse.setCode(0L);
                fetchInstanceInfoResponse.setData(instanceAttributeInfoList);

                response = fetchInstanceInfoResponse;
                break;
            case LIST_ATTRIBUTE:
                log.debug("List attribute request!|{}|{}|{}", callbackRequest.getType(), callbackRequest.getFilter(),
                    callbackRequest.getPage());
                response = new ListAttributeResponseDTO();
                response.setCode(0L);
                break;
            case LIST_ATTRIBUTE_VALUE:
                log.debug("List attribute value request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());
                response = new ListAttributeValueResponseDTO();
                response.setCode(0L);
                break;
            case LIST_INSTANCE_BY_POLICY:
                log.debug("List instance by policy request!|{}|{}|{}", callbackRequest.getType(),
                    callbackRequest.getFilter(), callbackRequest.getPage());
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
