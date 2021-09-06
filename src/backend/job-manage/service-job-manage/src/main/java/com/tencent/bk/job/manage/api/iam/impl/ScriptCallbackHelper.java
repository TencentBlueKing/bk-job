package com.tencent.bk.job.manage.api.iam.impl;

import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.service.BaseIamCallbackService;
import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.ScriptQueryDTO;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class ScriptCallbackHelper extends BaseIamCallbackService {
    private ScriptService scriptService;
    private IGetBasicInfo basicInfoInterface;

    public ScriptCallbackHelper(
        ScriptService scriptService,
        IGetBasicInfo basicInfoInterface
    ) {
        this.scriptService = scriptService;
        this.basicInfoInterface = basicInfoInterface;
    }

    public interface IGetBasicInfo {
        Pair<ScriptQueryDTO, BaseSearchCondition> getBasicQueryCondition(CallbackRequestDTO callbackRequest);

        boolean isPublicScript();
    }


    private InstanceInfoDTO convert(ScriptDTO script) {
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(String.valueOf(script.getId()));
        instanceInfo.setDisplayName(script.getName());
        return instanceInfo;
    }

    @Override
    protected ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        Pair<ScriptQueryDTO, BaseSearchCondition> basicQueryCond =
            basicInfoInterface.getBasicQueryCondition(callbackRequest);

        ScriptQueryDTO scriptQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();
        PageData<ScriptDTO> accountDTOPageData = scriptService.listPageScript(scriptQuery,
            baseSearchCondition);

        return IamRespUtil.getListInstanceRespFromPageData(accountDTOPageData, this::convert);
    }

    @Override
    protected SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {

        Pair<ScriptQueryDTO, BaseSearchCondition> basicQueryCond =
            basicInfoInterface.getBasicQueryCondition(callbackRequest);
        ScriptQueryDTO scriptQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();

        scriptQuery.setName(callbackRequest.getFilter().getKeyword());
        PageData<ScriptDTO> accountDTOPageData = scriptService.listPageScript(scriptQuery,
            baseSearchCondition);

        return IamRespUtil.getSearchInstanceRespFromPageData(accountDTOPageData, this::convert);
    }

    @Override
    protected CallbackBaseResponseDTO fetchInstanceResp(
        CallbackRequestDTO callbackRequest
    ) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        List<Object> instanceAttributeInfoList = new ArrayList<>();
        for (String instanceId : searchCondition.getIdList()) {
            try {
                ScriptDTO scriptDTO = scriptService.getScriptByScriptId(instanceId);
                if (scriptDTO == null || scriptDTO.isPublicScript() != basicInfoInterface.isPublicScript()) {
                    return getNotFoundRespById(instanceId);
                }
                // 拓扑路径构建
                List<PathInfoDTO> path = new ArrayList<>();
                PathInfoDTO rootNode = new PathInfoDTO();
                if (basicInfoInterface.isPublicScript()) {
                    // 公共脚本
                    rootNode.setType(ResourceId.PUBLIC_SCRIPT);
                    rootNode.setId(scriptDTO.getId());
                } else {
                    // 业务脚本
                    rootNode.setType(ResourceId.APP);
                    rootNode.setId(scriptDTO.getAppId().toString());
                    PathInfoDTO scriptNode = new PathInfoDTO();
                    scriptNode.setType(ResourceId.SCRIPT);
                    scriptNode.setId(scriptDTO.getId());
                    rootNode.setChild(scriptNode);
                }
                path.add(rootNode);
                // 实例组装
                InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                instanceInfo.setId(instanceId);
                instanceInfo.setDisplayName(scriptDTO.getName());
                instanceInfo.setPath(path);
                instanceAttributeInfoList.add(instanceInfo);
            } catch (NumberFormatException e) {
                log.error("Parse object id failed!|{}", instanceId, e);
            }
        }

        FetchInstanceInfoResponseDTO fetchInstanceInfoResponse = new FetchInstanceInfoResponseDTO();
        fetchInstanceInfoResponse.setCode(0L);
        fetchInstanceInfoResponse.setData(instanceAttributeInfoList);
        return fetchInstanceInfoResponse;
    }

    public CallbackBaseResponseDTO doCallback(CallbackRequestDTO callbackRequest) {
        return baseCallback(callbackRequest);
    }
}
