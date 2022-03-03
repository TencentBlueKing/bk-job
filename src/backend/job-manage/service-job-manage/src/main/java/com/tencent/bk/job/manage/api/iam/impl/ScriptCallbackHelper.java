package com.tencent.bk.job.manage.api.iam.impl;

import com.tencent.bk.job.common.app.AppTransferService;
import com.tencent.bk.job.common.app.ResourceScope;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamCallbackService;
import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.model.dto.ScriptBasicDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ScriptCallbackHelper extends BaseIamCallbackService {
    private final ScriptService scriptService;
    private final IGetBasicInfo basicInfoInterface;
    private final AppTransferService appTransferService;

    public ScriptCallbackHelper(
        ScriptService scriptService,
        IGetBasicInfo basicInfoInterface,
        AppTransferService appTransferService) {
        this.scriptService = scriptService;
        this.basicInfoInterface = basicInfoInterface;
        this.appTransferService = appTransferService;
    }

    public interface IGetBasicInfo {
        Pair<ScriptQuery, BaseSearchCondition> getBasicQueryCondition(CallbackRequestDTO callbackRequest);

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
        Pair<ScriptQuery, BaseSearchCondition> basicQueryCond =
            basicInfoInterface.getBasicQueryCondition(callbackRequest);

        ScriptQuery scriptQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();
        PageData<ScriptDTO> scriptDTOPageData = scriptService.listPageScript(scriptQuery,
            baseSearchCondition);

        return IamRespUtil.getListInstanceRespFromPageData(scriptDTOPageData, this::convert);
    }

    @Override
    protected SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {

        Pair<ScriptQuery, BaseSearchCondition> basicQueryCond =
            basicInfoInterface.getBasicQueryCondition(callbackRequest);
        ScriptQuery scriptQuery = basicQueryCond.getLeft();
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
        List<String> scriptIdList = searchCondition.getIdList();
        List<ScriptBasicDTO> scriptBasicDTOList = scriptService.listScriptBasicInfoByScriptIds(scriptIdList);
        Map<String, ScriptBasicDTO> scriptBasicDTOMap = new HashMap<>(scriptBasicDTOList.size());
        Set<Long> appIdSet = new HashSet<>();
        for (ScriptBasicDTO scriptBasicDTO : scriptBasicDTOList) {
            scriptBasicDTOMap.put(scriptBasicDTO.getId(), scriptBasicDTO);
            appIdSet.add(scriptBasicDTO.getAppId());
        }
        // Job app --> CMDB biz/businessSet转换
        Map<Long, ResourceScope> appIdScopeMap = appTransferService.getScopeByAppIds(appIdSet);
        for (String instanceId : searchCondition.getIdList()) {
            try {
                ScriptBasicDTO scriptBasicDTO = scriptBasicDTOMap.get(instanceId);
                if (scriptBasicDTO == null || scriptBasicDTO.isPublicScript() != basicInfoInterface.isPublicScript()) {
                    return getNotFoundRespById(instanceId);
                }
                // 拓扑路径构建
                List<PathInfoDTO> path = new ArrayList<>();
                PathInfoDTO rootNode = new PathInfoDTO();
                if (basicInfoInterface.isPublicScript()) {
                    // 公共脚本
                    rootNode.setType(ResourceTypeId.PUBLIC_SCRIPT);
                    rootNode.setId(scriptBasicDTO.getId());
                } else {
                    // 业务脚本
                    Long appId = scriptBasicDTO.getAppId();
                    rootNode = getPathNodeByAppId(appId, appIdScopeMap);
                    PathInfoDTO scriptNode = new PathInfoDTO();
                    scriptNode.setType(ResourceTypeId.SCRIPT);
                    scriptNode.setId(scriptBasicDTO.getId());
                    rootNode.setChild(scriptNode);
                }
                path.add(rootNode);
                // 实例组装
                InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                instanceInfo.setId(instanceId);
                instanceInfo.setDisplayName(scriptBasicDTO.getName());
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
