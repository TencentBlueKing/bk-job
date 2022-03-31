package com.tencent.bk.job.common.iam.util;

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.sdk.iam.dto.callback.response.BaseDataResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

public class IamRespUtil {

    public interface InstanceInfoDTOConverter<T> {
        InstanceInfoDTO convert(T t);
    }

    private static <T> List<InstanceInfoDTO> convertToInstanceList(
        List<T> rawDataList,
        InstanceInfoDTOConverter<T> converter
    ) {
        return rawDataList.parallelStream().map(converter::convert).collect(Collectors.toList());
    }

    public static <T> ListInstanceResponseDTO getListInstanceRespFromPageData(
        PageData<T> pageData,
        InstanceInfoDTOConverter<T> converter
    ) {
        List<InstanceInfoDTO> instanceInfoList = convertToInstanceList(pageData.getData(), converter);

        ListInstanceResponseDTO instanceResponse = new ListInstanceResponseDTO();
        instanceResponse.setCode(0L);
        BaseDataResponseDTO<InstanceInfoDTO> baseDataResponse = new BaseDataResponseDTO<>();
        baseDataResponse.setResult(instanceInfoList);
        baseDataResponse.setCount(pageData.getTotal());
        instanceResponse.setData(baseDataResponse);
        return instanceResponse;
    }

    public static <T> SearchInstanceResponseDTO getSearchInstanceRespFromPageData(
        PageData<T> pageData,
        InstanceInfoDTOConverter<T> converter
    ) {
        List<InstanceInfoDTO> instanceInfoList = convertToInstanceList(pageData.getData(), converter);

        SearchInstanceResponseDTO instanceResponse = new SearchInstanceResponseDTO();
        instanceResponse.setCode(0L);
        BaseDataResponseDTO<InstanceInfoDTO> baseDataResponse = new BaseDataResponseDTO<>();
        baseDataResponse.setResult(instanceInfoList);
        baseDataResponse.setCount(pageData.getTotal());
        instanceResponse.setData(baseDataResponse);
        return instanceResponse;
    }
}
