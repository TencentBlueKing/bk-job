/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.manage.dao.impl;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.util.TagUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.common.util.DbRecordMapper;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.ApplicationInfoDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.*;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.Host;
import org.jooq.generated.tables.HostTopo;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @since 4/11/2019 15:01
 */
@Slf4j
@Repository
public class ApplicationHostDAOImpl implements ApplicationHostDAO {
    private static final Host TABLE = Host.HOST;

    private DSLContext context;

    private ApplicationInfoDAO applicationInfoDAO;
    private HostTopoDAO hostTopoDAO;
    private TopologyHelper topologyHelper;

    @Autowired
    public ApplicationHostDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context,
                                  ApplicationInfoDAO applicationInfoDAO, HostTopoDAO hostTopoDAO,
                                  TopologyHelper topologyHelper) {
        this.context = context;
        this.applicationInfoDAO = applicationInfoDAO;
        this.topologyHelper = topologyHelper;
        this.hostTopoDAO = hostTopoDAO;
    }

    @Override
    public ApplicationHostInfoDTO getHostById(Long hostId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(hostId)));
        Result<
            Record12<ULong, ULong, String, String, String, String, ULong, String, String, String, String, UByte>> result =
            context
                .select(TABLE.HOST_ID, TABLE.APP_ID, TABLE.IP, TABLE.IP_DESC,
                    TABLE.SET_IDS, TABLE.MODULE_IDS, TABLE.CLOUD_AREA_ID,
                    TABLE.DISPLAY_IP, TABLE.OS, TABLE.OS_TYPE, TABLE.MODULE_TYPE, TABLE.IS_AGENT_ALIVE)
                .from(TABLE).where(conditions).fetch();

        List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();

        if (result != null && result.size() >= 1) {
            result.map(record -> hostInfoList.add(DbRecordMapper.convertRecordToApplicationHostInfo(record)));
        }
        if (hostInfoList.size() > 0) {
            return hostInfoList.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Set<Long> listCloudAreasByAppId(long appId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        Result<Record1<ULong>> result =
            context.select(TABLE.CLOUD_AREA_ID).distinctOn(TABLE.CLOUD_AREA_ID).from(TABLE).where(conditions).fetch();
        Set<Long> cloudAreaIdSet = new HashSet<>();
        for (Record1<ULong> record : result) {
            cloudAreaIdSet.add(record.get(TABLE.CLOUD_AREA_ID).longValue());
        }
        return cloudAreaIdSet;
    }

    @Override
    public List<ApplicationHostInfoDTO> listHostInfoByIps(Long appId, List<String> ips) {
        List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();
        // 分批，防止SQL超长
        int batchSize = 30000;
        int start = 0;
        int end = start + batchSize;
        int ipSize = ips.size();
        end = Math.min(end, ipSize);
        do {
            List<String> ipSubList = ips.subList(start, end);
            hostInfoList.addAll(listHostInfoByIpsIndeed(appId, ipSubList));
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, ipSize);
        } while (start < ipSize);
        return hostInfoList;
    }

    private List<ApplicationHostInfoDTO> listHostInfoByIpsIndeed(Long appId, Collection<String> ips) {
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.addAll(buildAppIdCondition(appId));
        }
        conditions.add(TABLE.IP.in(ips));
        Result<
            Record12<ULong, ULong, String, String, String, String, ULong, String, String, String, String, UByte>> result =
            context
                .select(TABLE.HOST_ID, TABLE.APP_ID, TABLE.IP, TABLE.IP_DESC,
                    TABLE.SET_IDS, TABLE.MODULE_IDS, TABLE.CLOUD_AREA_ID,
                    TABLE.DISPLAY_IP, TABLE.OS, TABLE.OS_TYPE, TABLE.MODULE_TYPE, TABLE.IS_AGENT_ALIVE)
                .from(TABLE).where(conditions).fetch();

        List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();

        if (result != null && result.size() >= 1) {
            result.map(record -> hostInfoList.add(DbRecordMapper.convertRecordToApplicationHostInfo(record)));
        }

        return hostInfoList;
    }

    @Override
    public List<ApplicationHostInfoDTO> listHostInfoByAppId(long appId) {
        List<Condition> conditions = buildAppIdCondition(appId);
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostInfoDTO> listAllHostInfo(Long start, Long limit) {
        return listHostInfoByConditions(Collections.emptyList(), start, limit);
    }

    private List<ApplicationHostInfoDTO> listHostInfoByConditions(Collection<Condition> conditions) {
        return listHostInfoByConditions(conditions, null, null);
    }

    private List<ApplicationHostInfoDTO> listHostInfoByConditions(Collection<Condition> conditions, Long start,
                                                                  Long limit) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        val query =
            context
                .select(TABLE.HOST_ID, TABLE.APP_ID, TABLE.IP, TABLE.IP_DESC,
                    TABLE.SET_IDS, TABLE.MODULE_IDS, TABLE.CLOUD_AREA_ID,
                    TABLE.DISPLAY_IP, TABLE.OS, TABLE.OS_TYPE, TABLE.MODULE_TYPE, TABLE.IS_AGENT_ALIVE)
                .from(TABLE).where(conditions).orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc());
        Result<Record12<ULong, ULong, String, String, String, String, ULong, String, String, String, String, UByte>> records;
        if (start == null || start < 0) {
            start = 0L;
        }
        if (limit != null && limit > 0) {
            records = query.limit(start, limit).fetch();
        } else {
            records = query.fetch();
        }
        List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();

        if (records != null && records.size() >= 1) {
            records.map(record -> hostInfoList.add(DbRecordMapper.convertRecordToApplicationHostInfo(record)));
        }
        return hostInfoList;
    }

    @Override
    public List<ApplicationHostInfoDTO> listHostInfo(Collection<Long> appIds, Collection<String> ips) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.in(appIds.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        conditions.add(TABLE.IP.in(ips.parallelStream().map(String::trim).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostInfoDTO> listHostInfoByNormalAppIds(Collection<Long> appIds, Long start, Long limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.in(appIds.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions, start, limit);
    }

    @Override
    public List<ApplicationHostInfoDTO> listHostInfoByHostIds(Collection<Long> hostIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.in(hostIds.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public Long countHostInfoBySearchContents(Collection<Long> appIds, Collection<Long> moduleIds,
                                              Collection<Long> cloudAreaIds, List<String> searchContents,
                                              Integer agentStatus) {
        List<Long> hostIdList = getHostIdListBySearchContents(appIds, moduleIds, cloudAreaIds, searchContents,
            agentStatus, null, null);
        return (long) (hostIdList.size());
    }

    @Override
    public List<ApplicationHostInfoDTO> listHostInfoBySearchContents(Collection<Long> appIds,
                                                                     Collection<Long> moduleIds,
                                                                     Collection<Long> cloudAreaIds,
                                                                     List<String> searchContents, Integer agentStatus
        , Long start, Long limit) {
        List<Long> hostIdList = getHostIdListBySearchContents(appIds, moduleIds, cloudAreaIds, searchContents,
            agentStatus, start, limit);
        List<ApplicationHostInfoDTO> hostInfoList = listHostInfoByHostIds(hostIdList);
        return hostInfoList;
    }

    public List<Long> getHostIdListBySearchContents(Collection<Long> appIds, Collection<Long> moduleIds,
                                                    Collection<Long> cloudAreaIds, List<String> searchContents,
                                                    Integer agentStatus, Long start, Long limit) {
        Host tHost = Host.HOST;
        HostTopo tHostTopo = HostTopo.HOST_TOPO;
        List<Condition> conditions = new ArrayList<>();
        if (appIds != null) {
            conditions.add(tHostTopo.APP_ID.in(appIds));
        }
        if (agentStatus != null) {
            conditions.add(tHost.IS_AGENT_ALIVE.eq(UByte.valueOf(agentStatus)));
        }
        if (moduleIds != null) {
            conditions.add(tHostTopo.MODULE_ID.in(moduleIds));
        }
        Condition condition = null;
        if (searchContents != null && !searchContents.isEmpty()) {
            String firstContent = searchContents.get(0);
            condition = tHost.IP.like("%" + firstContent + "%");
            for (int i = 1; i < searchContents.size(); i++) {
                condition = condition.or(tHost.IP.like("%" + searchContents.get(i) + "%"));
            }
            condition = condition.or(tHost.IP_DESC.like("%" + firstContent + "%"));
            for (int i = 1; i < searchContents.size(); i++) {
                condition = condition.or(tHost.IP_DESC.like("%" + searchContents.get(i) + "%"));
            }
            condition = condition.or(tHost.OS.like("%" + firstContent + "%"));
            for (int i = 1; i < searchContents.size(); i++) {
                condition = condition.or(tHost.OS.like("%" + searchContents.get(i) + "%"));
            }
        }
        if (cloudAreaIds != null) {
            if (condition != null) {
                condition = condition.or(tHost.CLOUD_AREA_ID.in(cloudAreaIds));
            } else {
                condition = tHost.CLOUD_AREA_ID.in(cloudAreaIds);
            }
        }
        if (condition != null) {
            conditions.add(condition);
        }
        val query =
            context
                .selectDistinct(tHost.HOST_ID)
                .select(tHost.IS_AGENT_ALIVE)
                .from(tHost)
                .join(tHostTopo)
                .on(tHost.HOST_ID.eq(tHostTopo.HOST_ID))
                .where(conditions)
                .orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc());
        log.debug("SQL={}", query.getSQL(ParamType.INLINED));
        Result<Record> records;
        if (start == null || start < 0) {
            start = 0L;
        }
        if (limit != null && limit > 0) {
            records = query.limit(start, limit).fetch();
        } else {
            records = query.fetch();
        }
        List<Long> hostIdList = new ArrayList<>();
        if (records != null && records.size() >= 1) {
            hostIdList = records.parallelStream().map(record -> record.get(0, Long.class)).collect(Collectors.toList());
        }
        Set<Long> hostIdSet = new HashSet<>();
        List<Long> uniqueHostIdList = new ArrayList<>();
        hostIdList.forEach(hostId -> {
            if (!hostIdSet.contains(hostId)) {
                uniqueHostIdList.add(hostId);
                hostIdSet.add(hostId);
            }
        });
        return uniqueHostIdList;
    }

    @Override
    public List<ApplicationHostInfoDTO> listHostInfoByDisplayIp(long appId, String ip) {
        List<Condition> conditions = buildAppIdCondition(appId);
        conditions.add(TABLE.DISPLAY_IP.like(ip + "%"));

        Result<Record12<ULong, ULong, String, String, String, String, ULong, String, String, String, String, UByte>> result =
            context
                .select(TABLE.HOST_ID, TABLE.APP_ID, TABLE.IP, TABLE.IP_DESC,
                    TABLE.SET_IDS, TABLE.MODULE_IDS, TABLE.CLOUD_AREA_ID,
                    TABLE.DISPLAY_IP, TABLE.OS, TABLE.OS_TYPE, TABLE.MODULE_TYPE, TABLE.IS_AGENT_ALIVE)
                .from(TABLE).where(conditions).orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc()).fetch();

        List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();

        if (result != null && result.size() >= 1) {
            result.map(record -> hostInfoList.add(DbRecordMapper.convertRecordToApplicationHostInfo(record)));
        }
        return hostInfoList;
    }

    @Override
    public List<ApplicationHostInfoDTO> listHostInfoBySourceAndIps(long cloudAreaId, Set<String> ips) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.CLOUD_AREA_ID.eq(ULong.valueOf(cloudAreaId)));
        conditions.add(TABLE.IP.in(ips));

        Result<Record12<ULong, ULong, String, String, String, String, ULong, String, String, String, String, UByte>> result =
            context
                .select(TABLE.HOST_ID, TABLE.APP_ID, TABLE.IP, TABLE.IP_DESC,
                    TABLE.SET_IDS, TABLE.MODULE_IDS, TABLE.CLOUD_AREA_ID,
                    TABLE.DISPLAY_IP, TABLE.OS, TABLE.OS_TYPE, TABLE.MODULE_TYPE, TABLE.IS_AGENT_ALIVE)
                .from(TABLE).where(conditions).orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc()).fetch();

        List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();

        if (result != null && result.size() >= 1) {
            result.map(record -> hostInfoList.add(DbRecordMapper.convertRecordToApplicationHostInfo(record)));
        }
        return hostInfoList;
    }

    @Override
    public List<ApplicationHostInfoDTO> listHostInfoByAppIdsAndSourceAndIps(long appId, long cloudAreaId,
                                                                            Set<String> ips) {
        List<Condition> conditions = buildAppIdCondition(appId);
        conditions.add(TABLE.CLOUD_AREA_ID.eq(ULong.valueOf(cloudAreaId)));
        conditions.add(TABLE.IP.in(ips));

        Result<Record12<ULong, ULong, String, String, String, String, ULong, String, String, String, String, UByte>> result =
            context
                .select(TABLE.HOST_ID, TABLE.APP_ID, TABLE.IP, TABLE.IP_DESC,
                    TABLE.SET_IDS, TABLE.MODULE_IDS, TABLE.CLOUD_AREA_ID,
                    TABLE.DISPLAY_IP, TABLE.OS, TABLE.OS_TYPE, TABLE.MODULE_TYPE, TABLE.IS_AGENT_ALIVE)
                .from(TABLE).where(conditions).orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc()).fetch();

        List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();

        if (result != null && result.size() >= 1) {
            result.map(record -> hostInfoList.add(DbRecordMapper.convertRecordToApplicationHostInfo(record)));
        }
        return hostInfoList;
    }

    @Override
    public PageData<ApplicationHostInfoDTO> listHostInfoByPage(ApplicationHostInfoDTO applicationHostInfoCondition,
                                                               BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildAppIdCondition(applicationHostInfoCondition.getAppId());
        conditions.addAll(buildCondition(applicationHostInfoCondition, baseSearchCondition));

        long hostCount = getPageHostInfoCount(conditions);

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        Result<Record12<ULong, ULong, String, String, String, String, ULong, String, String, String, String, UByte>> result =
            context
                .select(TABLE.HOST_ID, TABLE.APP_ID, TABLE.IP, TABLE.IP_DESC,
                    TABLE.SET_IDS, TABLE.MODULE_IDS, TABLE.CLOUD_AREA_ID,
                    TABLE.DISPLAY_IP, TABLE.OS, TABLE.OS_TYPE, TABLE.MODULE_TYPE, TABLE.IS_AGENT_ALIVE)
                .from(TABLE).where(conditions).orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc())
                .limit(start, length).fetch();

        List<ApplicationHostInfoDTO> hostInfoList = new ArrayList<>();

        if (result != null && result.size() >= 1) {
            result.map(record -> hostInfoList.add(DbRecordMapper.convertRecordToApplicationHostInfo(record)));
        }

        PageData<ApplicationHostInfoDTO> hostInfoPageData = new PageData<>();
        hostInfoPageData.setTotal(hostCount);
        hostInfoPageData.setStart(start);
        hostInfoPageData.setPageSize(length);
        hostInfoPageData.setData(hostInfoList);

        return hostInfoPageData;
    }

    private void setDefaultValue(ApplicationHostInfoDTO applicationHostInfoDTO) {
        if (applicationHostInfoDTO.getHostId() == null) {
            applicationHostInfoDTO.setHostId(-1L);
        }
        if (applicationHostInfoDTO.getAppId() == null) {
            applicationHostInfoDTO.setAppId(-1L);
        }
        if (applicationHostInfoDTO.getCloudAreaId() == null) {
            applicationHostInfoDTO.setCloudAreaId(-1L);
        }
        if (applicationHostInfoDTO.getSetId() == null) {
            applicationHostInfoDTO.setSetId(new ArrayList<>());
        }
        if (applicationHostInfoDTO.getGseAgentAlive() == null) {
            applicationHostInfoDTO.setGseAgentAlive(true);
        }
        if (applicationHostInfoDTO.getIp() == null) {
            applicationHostInfoDTO.setIp("0.0.0.0");
        }
        if (applicationHostInfoDTO.getDisplayIp() == null) {
            applicationHostInfoDTO.setDisplayIp("0.0.0.0");
        }
    }

    private List<HostTopoDTO> genHostTopoDTOList(ApplicationHostInfoDTO applicationHostInfoDTO) {
        List<Long> setIdList = applicationHostInfoDTO.getSetId();
        List<Long> moduleIdList = applicationHostInfoDTO.getModuleId();
        if (setIdList == null || moduleIdList == null) {
            return Collections.emptyList();
        } else if (setIdList.size() != moduleIdList.size()) {
            throw new RuntimeException("setIdList.size()!=moduleIdList.size(),hostInfo=" + JsonUtils.toJson(applicationHostInfoDTO));
        } else {
            List<HostTopoDTO> hostTopoDTOList = new ArrayList<>();
            for (int i = 0; i < setIdList.size(); i++) {
                hostTopoDTOList.add(new HostTopoDTO(applicationHostInfoDTO.getHostId(),
                    applicationHostInfoDTO.getAppId(), setIdList.get(i), moduleIdList.get(i)));
            }
            return hostTopoDTOList;
        }
    }

    @Override
    public int insertAppHostWithoutTopo(DSLContext dslContext, ApplicationHostInfoDTO applicationHostInfoDTO) {
        return insertAppHostInfo(dslContext, applicationHostInfoDTO, false);
    }

    @Override
    public int insertAppHostInfo(DSLContext dslContext, ApplicationHostInfoDTO applicationHostInfoDTO) {
        return insertAppHostInfo(dslContext, applicationHostInfoDTO, true);
    }

    private int insertAppHostInfo(DSLContext dslContext, ApplicationHostInfoDTO applicationHostInfoDTO,
                                  Boolean insertTopo) {
        setDefaultValue(applicationHostInfoDTO);
        String moduleIdsStr = null;
        List<Long> moduleIdList = applicationHostInfoDTO.getModuleId();
        if (moduleIdList != null) {
            moduleIdsStr = String.join(",",
                moduleIdList.stream().map(id -> id.toString()).collect(Collectors.toList()));
        }
        String setIdsStr = null;
        List<Long> setIdList = applicationHostInfoDTO.getSetId();
        if (setIdList != null) {
            setIdsStr = String.join(",", setIdList.stream().map(id -> id.toString()).collect(Collectors.toList()));
        }
        String moduleTypeStr = null;
        List<Long> moduleTypeList = applicationHostInfoDTO.getModuleType();
        if (moduleTypeList != null) {
            moduleTypeStr = String.join(",",
                moduleTypeList.stream().map(type -> type.toString()).collect(Collectors.toList()));
        }
        int[] result = new int[]{-1};
        String finalSetIdsStr = setIdsStr;
        String finalModuleIdsStr = moduleIdsStr;
        String finalModuleTypeStr = moduleTypeStr;
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            val query = context.insertInto(TABLE,
                TABLE.HOST_ID,
                TABLE.APP_ID,
                TABLE.IP,
                TABLE.IP_DESC,
                TABLE.SET_IDS,
                TABLE.MODULE_IDS,
                TABLE.CLOUD_AREA_ID,
                TABLE.DISPLAY_IP,
                TABLE.OS,
                TABLE.OS_TYPE,
                TABLE.MODULE_TYPE,
                TABLE.IS_AGENT_ALIVE
            ).values(
                ULong.valueOf(applicationHostInfoDTO.getHostId()),
                ULong.valueOf(applicationHostInfoDTO.getAppId()),
                applicationHostInfoDTO.getIp(),
                applicationHostInfoDTO.getIpDesc(),
                finalSetIdsStr,
                finalModuleIdsStr,
                ULong.valueOf(applicationHostInfoDTO.getCloudAreaId()),
                applicationHostInfoDTO.getDisplayIp(),
                applicationHostInfoDTO.getOs(),
                applicationHostInfoDTO.getOsType(),
                finalModuleTypeStr,
                UByte.valueOf(applicationHostInfoDTO.getGseAgentAlive() ? 1 : 0)
            );
            try {
                result[0] = query.execute();
            } catch (Throwable t) {
                log.info("SQL=" + query.getSQL(ParamType.INLINED));
                throw t;
            }
            if (insertTopo) {
                List<HostTopoDTO> hostTopoDTOList = genHostTopoDTOList(applicationHostInfoDTO);
                hostTopoDAO.deleteHostTopoByHostId(context, applicationHostInfoDTO.getAppId(),
                    applicationHostInfoDTO.getHostId());
                hostTopoDAO.batchInsertHostTopo(context, hostTopoDTOList);
            }
        });
        return result[0];
    }

    @Override
    public int batchInsertAppHostInfo(DSLContext dslContext, List<ApplicationHostInfoDTO> applicationHostInfoDTOList) {
        int batchSize = 1000;
        int size = applicationHostInfoDTOList.size();
        int start = 0;
        int end;
        int[] affectedNum = new int[]{0};
        do {
            end = start + batchSize;
            end = end < size ? end : size;
            List<ApplicationHostInfoDTO> subList = applicationHostInfoDTOList.subList(start, end);
            dslContext.transaction(configuration -> {
                DSLContext context = DSL.using(configuration);
                val insertQuery = context.insertInto(TABLE,
                    TABLE.HOST_ID,
                    TABLE.APP_ID,
                    TABLE.IP,
                    TABLE.IP_DESC,
                    TABLE.SET_IDS,
                    TABLE.MODULE_IDS,
                    TABLE.CLOUD_AREA_ID,
                    TABLE.DISPLAY_IP,
                    TABLE.OS,
                    TABLE.OS_TYPE,
                    TABLE.MODULE_TYPE,
                    TABLE.IS_AGENT_ALIVE
                ).values(
                    (ULong) null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
                );
                BatchBindStep batchQuery = context.batch(insertQuery);
                List<HostTopoDTO> hostTopoDTOList = new ArrayList<>();
                for (ApplicationHostInfoDTO applicationHostInfoDTO : subList) {
                    setDefaultValue(applicationHostInfoDTO);
                    String moduleIdsStr = null;
                    List<Long> moduleIdList = applicationHostInfoDTO.getModuleId();
                    if (moduleIdList != null) {
                        moduleIdsStr = String.join(",",
                            moduleIdList.stream().map(id -> id.toString()).collect(Collectors.toList()));
                    }
                    String setIdsStr = null;
                    List<Long> setIdList = applicationHostInfoDTO.getSetId();
                    if (setIdList != null) {
                        setIdsStr = String.join(",",
                            setIdList.stream().map(id -> id.toString()).collect(Collectors.toList()));
                    }
                    String moduleTypeStr = null;
                    List<Long> moduleTypeList = applicationHostInfoDTO.getModuleType();
                    if (moduleTypeList != null) {
                        moduleTypeStr = String.join(",",
                            moduleTypeList.stream().map(type -> type.toString()).collect(Collectors.toList()));
                    }
                    batchQuery = batchQuery.bind(
                        ULong.valueOf(applicationHostInfoDTO.getHostId()),
                        ULong.valueOf(applicationHostInfoDTO.getAppId()),
                        applicationHostInfoDTO.getIp(),
                        applicationHostInfoDTO.getIpDesc(),
                        setIdsStr,
                        moduleIdsStr,
                        ULong.valueOf(applicationHostInfoDTO.getCloudAreaId()),
                        applicationHostInfoDTO.getDisplayIp(),
                        applicationHostInfoDTO.getOs(),
                        applicationHostInfoDTO.getOsType(),
                        moduleTypeStr,
                        UByte.valueOf(applicationHostInfoDTO.getGseAgentAlive() ? 1 : 0)
                    );
                    hostTopoDTOList.addAll(genHostTopoDTOList(applicationHostInfoDTO));
                }
                int[] results = batchQuery.execute();
                for (int result : results) {
                    affectedNum[0] += result;
                }
                hostTopoDAO.batchInsertHostTopo(context, hostTopoDTOList);
            });
            start += batchSize;
        } while (end < size);
        return affectedNum[0];
    }

    @Override
    public boolean existAppHostInfoByHostId(DSLContext dslContext, ApplicationHostInfoDTO applicationHostInfoDTO) {
        setDefaultValue(applicationHostInfoDTO);
        if (applicationHostInfoDTO.getHostId() == -1L) {
            return false;
        }
        String moduleIdsStr = String.join(",",
            applicationHostInfoDTO.getModuleId().stream().map(id -> id.toString()).collect(Collectors.toList()));
        String setIdsStr = String.join(",",
            applicationHostInfoDTO.getSetId().stream().map(id -> id.toString()).collect(Collectors.toList()));
        String moduleTypeStr = String.join(",",
            applicationHostInfoDTO.getModuleType().stream().map(type -> type.toString()).collect(Collectors.toList()));
        val query = dslContext.selectCount().from(TABLE)
            .where(TABLE.APP_ID.eq(ULong.valueOf(applicationHostInfoDTO.getAppId())))
            .and(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostInfoDTO.getHostId())))
            .and(TABLE.IP.eq(applicationHostInfoDTO.getIp()))
            .and(TABLE.IP_DESC.eq(applicationHostInfoDTO.getIpDesc()))
            .and(TABLE.SET_IDS.eq(setIdsStr))
            .and(TABLE.MODULE_IDS.eq(moduleIdsStr))
            .and(TABLE.CLOUD_AREA_ID.eq(ULong.valueOf(applicationHostInfoDTO.getCloudAreaId())))
            .and(TABLE.DISPLAY_IP.eq(applicationHostInfoDTO.getDisplayIp()))
            .and(TABLE.OS.eq(applicationHostInfoDTO.getOs()))
            .and(TABLE.MODULE_TYPE.eq(moduleTypeStr))
            .and(TABLE.IS_AGENT_ALIVE.eq(UByte.valueOf(applicationHostInfoDTO.getGseAgentAlive() ? 1 : 0)));
        try {
            return query.fetchOne(0, Long.class) >= 1;
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
    }

    @Override
    public boolean existAppHostInfoByHostId(DSLContext dslContext, Long hostId) {
        val query = dslContext.selectCount().from(TABLE)
            .where(TABLE.HOST_ID.eq(ULong.valueOf(hostId)));
        try {
            return query.fetchOne(0, Long.class) >= 1;
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
    }

    @Override
    public int updateAppHostInfoByHostId(DSLContext dslContext, Long appId,
                                         ApplicationHostInfoDTO applicationHostInfoDTO) {
        setDefaultValue(applicationHostInfoDTO);
        if (applicationHostInfoDTO.getHostId() == -1L) {
            return -1;
        }
        String moduleIdsStr = String.join(",",
            applicationHostInfoDTO.getModuleId().stream().map(id -> id.toString()).collect(Collectors.toList()));
        String setIdsStr = String.join(",",
            applicationHostInfoDTO.getSetId().stream().map(id -> id.toString()).collect(Collectors.toList()));
        String moduleTypeStr = String.join(",",
            applicationHostInfoDTO.getModuleType().stream().map(type -> type.toString()).collect(Collectors.toList()));
        int[] affectedNum = new int[]{-1};
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        }
        Long hostId = applicationHostInfoDTO.getHostId();
        if (hostId != null) {
            conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostInfoDTO.getHostId())));
        }
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            val query = context.update(TABLE)
                .set(TABLE.APP_ID, ULong.valueOf(applicationHostInfoDTO.getAppId()))
                .set(TABLE.IP, applicationHostInfoDTO.getIp())
                .set(TABLE.IP_DESC, applicationHostInfoDTO.getIpDesc())
                .set(TABLE.SET_IDS, setIdsStr)
                .set(TABLE.MODULE_IDS, moduleIdsStr)
                .set(TABLE.CLOUD_AREA_ID, ULong.valueOf(applicationHostInfoDTO.getCloudAreaId()))
                .set(TABLE.DISPLAY_IP, applicationHostInfoDTO.getDisplayIp())
                .set(TABLE.OS, applicationHostInfoDTO.getOs())
                .set(TABLE.OS_TYPE, applicationHostInfoDTO.getOsType())
                .set(TABLE.MODULE_TYPE, moduleTypeStr)
                .set(TABLE.IS_AGENT_ALIVE, UByte.valueOf(applicationHostInfoDTO.getGseAgentAlive() ? 1 : 0))
                .where(conditions);
            try {
                affectedNum[0] = query.execute();
            } catch (Throwable t) {
                log.info("SQL=" + query.getSQL(ParamType.INLINED));
                throw t;
            }
            List<HostTopoDTO> hostTopoDTOList = genHostTopoDTOList(applicationHostInfoDTO);
            hostTopoDAO.deleteHostTopoByHostId(context, appId, hostId);
            hostTopoDAO.batchInsertHostTopo(context, hostTopoDTOList);
        });
        return affectedNum[0];
    }

    @Override
    public int batchUpdateAppHostInfoByHostId(DSLContext dslContext,
                                              List<ApplicationHostInfoDTO> applicationHostInfoDTOList) {
        int batchSize = 1000;
        int size = applicationHostInfoDTOList.size();
        int start = 0;
        int end;
        List<Query> queryList = new ArrayList<>();
        int[] affectedNum = new int[]{0};
        do {
            end = start + batchSize;
            end = end < size ? end : size;
            List<ApplicationHostInfoDTO> subList = applicationHostInfoDTOList.subList(start, end);
            dslContext.transaction(configuration -> {
                DSLContext context = DSL.using(configuration);
                List<HostTopoDTO> hostTopoDTOList = new ArrayList<>();
                for (ApplicationHostInfoDTO applicationHostInfoDTO : subList) {
                    setDefaultValue(applicationHostInfoDTO);
                    if (applicationHostInfoDTO.getHostId() == -1L) {
                        log.warn("Unexpected hostId==-1,hostInfo={}", applicationHostInfoDTO);
                        continue;
                    }
                    String moduleIdsStr =
                        applicationHostInfoDTO.getModuleId().stream().map(Object::toString).collect(Collectors.joining(","));
                    String setIdsStr =
                        applicationHostInfoDTO.getSetId().stream().map(Object::toString).collect(Collectors.joining(
                            ","));
                    String moduleTypeStr =
                        applicationHostInfoDTO.getModuleType().stream().map(Object::toString).collect(Collectors.joining(","));
                    queryList.add(dslContext.update(TABLE)
                        .set(TABLE.APP_ID, ULong.valueOf(applicationHostInfoDTO.getAppId()))
                        .set(TABLE.IP, applicationHostInfoDTO.getIp())
                        .set(TABLE.IP_DESC, applicationHostInfoDTO.getIpDesc())
                        .set(TABLE.SET_IDS, setIdsStr)
                        .set(TABLE.MODULE_IDS, moduleIdsStr)
                        .set(TABLE.CLOUD_AREA_ID, ULong.valueOf(applicationHostInfoDTO.getCloudAreaId()))
                        .set(TABLE.DISPLAY_IP, applicationHostInfoDTO.getDisplayIp())
                        .set(TABLE.OS, applicationHostInfoDTO.getOs())
                        .set(TABLE.OS_TYPE, applicationHostInfoDTO.getOsType())
                        .set(TABLE.MODULE_TYPE, moduleTypeStr)
                        .set(TABLE.IS_AGENT_ALIVE, UByte.valueOf(applicationHostInfoDTO.getGseAgentAlive() ? 1 : 0))
                        .where(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostInfoDTO.getHostId())))
                        .and(TABLE.APP_ID.eq(ULong.valueOf(applicationHostInfoDTO.getAppId())))
                    );
                    hostTopoDTOList.addAll(genHostTopoDTOList(applicationHostInfoDTO));
                }
                int[] results = dslContext.batch(queryList).execute();
                queryList.clear();
                for (int result : results) {
                    affectedNum[0] += result;
                }
                // 更新hostTopo表数据
                hostTopoDAO.batchDeleteHostTopo(context,
                    new ArrayList<>(hostTopoDTOList.stream().map(HostTopoDTO::getHostId).collect(Collectors.toSet())));
                hostTopoDAO.batchInsertHostTopo(context, hostTopoDTOList);
            });
            start += batchSize;
        } while (end < size);
        return affectedNum[0];
    }

    @Override
    public int deleteAppHostInfoById(DSLContext dslContext, Long appId, Long appHostId) {
        int[] affectedNum = new int[]{-1};
        List<Condition> conditions = new ArrayList<>();
        if (appId != null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        }
        if (appHostId != null) {
            conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(appHostId)));
        }
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            affectedNum[0] = context.deleteFrom(TABLE)
                .where(conditions)
                .execute();
            hostTopoDAO.deleteHostTopoByHostId(context, appId, appHostId);
        });
        return affectedNum[0];
    }

    @Override
    public int batchDeleteAppHostTopoById(DSLContext dslContext, List<Long> appHostIdList) {
        int[] affectedNum = new int[]{0};
        int batchSize = 1000;
        int size = appHostIdList.size();
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            int start = 0;
            int end;
            do {
                end = start + batchSize;
                end = end < size ? end : size;
                List<Long> subList = appHostIdList.subList(start, end);
                affectedNum[0] += hostTopoDAO.batchDeleteHostTopo(context, subList);
                start += batchSize;
            } while (end < size);
        });
        return affectedNum[0];
    }

    @Override
    public int batchDeleteAppHostInfoById(DSLContext dslContext, Long appId, List<Long> appHostIdList) {
        int[] affectedNum = new int[]{0};
        int batchSize = 1000;
        int size = appHostIdList.size();
        List<Condition> conditions = new ArrayList<>();
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            int start = 0;
            int end;
            List<Query> queryList = new ArrayList<>();
            do {
                end = start + batchSize;
                end = end < size ? end : size;
                List<Long> subList = appHostIdList.subList(start, end);
                if (appId != null) {
                    queryList.add(context.deleteFrom(TABLE)
                        .where(TABLE.HOST_ID.in(subList.stream().map(ULong::valueOf).collect(Collectors.toList())))
                        .and(TABLE.APP_ID.eq(ULong.valueOf(appId)))
                    );
                } else {
                    queryList.add(context.deleteFrom(TABLE)
                        .where(TABLE.HOST_ID.in(subList.stream().map(ULong::valueOf).collect(Collectors.toList())))
                    );
                }
                // SQL语句达到批量即执行
                if (queryList.size() >= batchSize) {
                    int[] results = context.batch(queryList).execute();
                    queryList.clear();
                    for (int result : results) {
                        affectedNum[0] += result;
                    }
                }
                hostTopoDAO.batchDeleteHostTopo(context, subList);
                start += batchSize;
            } while (end < size);
            if (!queryList.isEmpty()) {
                int[] results = dslContext.batch(queryList).execute();
                for (int result : results) {
                    affectedNum[0] += result;
                }
            }
            hostTopoDAO.batchDeleteHostTopo(context, appHostIdList);
        });
        return affectedNum[0];
    }

    @Override
    public int deleteAppHostInfoByAppId(DSLContext dslContext, long appId) {
        // 先查出所有的hostId
        List<Long> hostIds = getHostIdListBySearchContents(Collections.singleton(appId), null, null, null, null, null
            , null);
        // 删除拓扑信息+主机信息
        return batchDeleteAppHostInfoById(dslContext, appId, hostIds);
    }

    @Override
    public int deleteAppHostInfoNotInApps(DSLContext dslContext, Set<Long> notInAppIds) {
        val records = dslContext.select(TABLE.HOST_ID).from(TABLE)
            .where(TABLE.APP_ID.notIn(notInAppIds.stream().map(ULong::valueOf).collect(Collectors.toSet())))
            .fetch();
        if (records != null && records.isNotEmpty()) {
            List<Long> hostIds = records.map(it -> it.get(0, Long.class));
            // 删除拓扑信息+主机信息
            return batchDeleteAppHostInfoById(dslContext, null, hostIds);
        }
        return 0;
    }

    @Override
    public boolean existsHost(DSLContext dslContext, long appId, String ip) {
        return dslContext.fetchExists(TABLE, TABLE.APP_ID.eq(ULong.valueOf(appId)).and(TABLE.IP.eq(ip)));
    }

    @Override
    public ApplicationHostInfoDTO getLatestHost(DSLContext dslContext, long appId, long cloudAreaId, String ip) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.CLOUD_AREA_ID.eq(ULong.valueOf(cloudAreaId)));
        conditions.add(TABLE.IP.eq(ip));
        Result<
            Record12<ULong, ULong, String, String, String, String, ULong, String, String, String, String, UByte>> records =
            context
                .select(TABLE.HOST_ID, TABLE.APP_ID, TABLE.IP, TABLE.IP_DESC,
                    TABLE.SET_IDS, TABLE.MODULE_IDS, TABLE.CLOUD_AREA_ID,
                    TABLE.DISPLAY_IP, TABLE.OS, TABLE.OS_TYPE, TABLE.MODULE_TYPE, TABLE.IS_AGENT_ALIVE)
                .from(TABLE).where(conditions).orderBy(TABLE.ROW_UPDATE_TIME.desc(), TABLE.HOST_ID.asc()).fetch();
        if (records != null && records.size() >= 1) {
            return DbRecordMapper.convertRecordToApplicationHostInfo(records.get(0));
        }
        return null;
    }

    @Override
    public long countHostsByAppIds(DSLContext dslContext, Collection<Long> appIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.in(appIds));
        return getPageHostInfoCount(conditions);
    }

    @Override
    public long countAllHosts() {
        log.debug("countAllHosts");
        return getPageHostInfoCount(null);
    }

    @Override
    public long countHostsByOsType(String osType) {
        List<Condition> conditions = new ArrayList<>();
        if (osType != null) {
            conditions.add(TABLE.OS_TYPE.eq(osType));
        }
        return getPageHostInfoCount(conditions);
    }

    @Override
    public long syncHostTopo(DSLContext dslContext, Long hostId) {
        ApplicationHostInfoDTO hostInfoDTO = getHostById(hostId);
        if (hostInfoDTO != null) {
            List<HostTopoDTO> hostTopoDTOList = hostTopoDAO.listHostTopoByHostId(dslContext, hostId);
            List<Long> setIds =
                hostTopoDTOList.parallelStream().map(HostTopoDTO::getSetId).collect(Collectors.toList());
            List<Long> moduleIds =
                hostTopoDTOList.parallelStream().map(HostTopoDTO::getModuleId).collect(Collectors.toList());
            List<Long> moduleTypes = moduleIds.parallelStream().map(it -> 1L).collect(Collectors.toList());
            if (!hostTopoDTOList.isEmpty()) {
                hostInfoDTO.setAppId(hostTopoDTOList.get(0).getAppId());
            }
            hostInfoDTO.setSetId(setIds);
            hostInfoDTO.setModuleId(moduleIds);
            hostInfoDTO.setModuleType(moduleTypes);
            return updateAppHostInfoByHostId(dslContext, null, hostInfoDTO);
        }
        return -1L;
    }

    /**
     * 查询符合条件的主机数量
     */
    private long getPageHostInfoCount(List<Condition> conditions) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
    }

    private List<Condition> buildAppIdCondition(long appId) {
        ApplicationInfoDTO appInfo = applicationInfoDAO.getAppInfoById(appId);
        AppTypeEnum appType = appInfo.getAppType();
        List<Condition> conditions = new ArrayList<>();
        if (appType == null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
            return conditions;
        }
        switch (appType) {
            case NORMAL:
                conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
                break;
            case APP_SET:
                List<Long> subAppIds = topologyHelper.getAppSetSubAppIds(appInfo);
                conditions.add(TABLE.APP_ID.in(subAppIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
                break;
            case ALL_APP:
            default:
                break;
        }
        return conditions;
    }

    private List<Condition> buildCondition(ApplicationHostInfoDTO applicationHostInfoCondition,
                                           BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(applicationHostInfoCondition.getIpList())) {
            conditions.add(TABLE.IP.in(applicationHostInfoCondition.getIpList()));
        }

        if (applicationHostInfoCondition.getGseAgentAlive() != null) {
            conditions.add(TABLE.IS_AGENT_ALIVE.eq(UByte.valueOf(applicationHostInfoCondition.getGseAgentAlive() ? 1
                : 0)));
        }

        if (CollectionUtils.isNotEmpty(applicationHostInfoCondition.getModuleType())) {
            if (applicationHostInfoCondition.getModuleType().size() != 1
                || applicationHostInfoCondition.getModuleType().get(0) != 0) {
                conditions.add(TABLE.MODULE_TYPE
                    .like("%" + TagUtils.buildDbTag(applicationHostInfoCondition.getModuleType().get(0)) + "%"));
            }
        }

        if (StringUtils.isNotBlank(applicationHostInfoCondition.getIp())) {
            String ipQueryString = "%" + applicationHostInfoCondition.getIp() + "%";
            conditions.add(TABLE.IP.like(ipQueryString).or(TABLE.DISPLAY_IP.like(ipQueryString)));
        }
        return conditions;
    }
}
