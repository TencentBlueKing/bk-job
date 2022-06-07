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

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.gse.constants.AgentStatusEnum;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.TagUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.ApplicationHostDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.TableField;
import org.jooq.conf.ParamType;
import org.jooq.generated.tables.Host;
import org.jooq.generated.tables.HostTopo;
import org.jooq.impl.DSL;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 主机DAO
 */
@Slf4j
@Repository
public class ApplicationHostDAOImpl implements ApplicationHostDAO {
    private static final Host TABLE = Host.HOST;
    private static final TableField<?, ?>[] ALL_FIELDS = {
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
        TABLE.IS_AGENT_ALIVE,
        TABLE.CLOUD_IP
    };

    private final DSLContext context;
    private final ApplicationDAO applicationDAO;
    private final HostTopoDAO hostTopoDAO;
    private final TopologyHelper topologyHelper;

    @Autowired
    public ApplicationHostDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context,
                                  ApplicationDAO applicationDAO,
                                  HostTopoDAO hostTopoDAO,
                                  TopologyHelper topologyHelper) {
        this.context = context;
        this.applicationDAO = applicationDAO;
        this.topologyHelper = topologyHelper;
        this.hostTopoDAO = hostTopoDAO;
    }

    @Override
    public ApplicationHostDTO getHostById(Long hostId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(hostId)));
        Record record = context.select(ALL_FIELDS).from(TABLE).where(conditions).fetchOne();
        return extractData(record);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByIps(Long bizId, List<String> ips) {
        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();
        // 分批，防止SQL超长
        int batchSize = 30000;
        int start = 0;
        int end = start + batchSize;
        int ipSize = ips.size();
        end = Math.min(end, ipSize);
        do {
            List<String> ipSubList = ips.subList(start, end);
            hostInfoList.addAll(listHostInfoByIpsIndeed(bizId, ipSubList));
            start += batchSize;
            end = start + batchSize;
            end = Math.min(end, ipSize);
        } while (start < ipSize);
        return hostInfoList;
    }

    private List<ApplicationHostDTO> listHostInfoByIpsIndeed(Long bizId, Collection<String> ips) {
        List<Condition> conditions = new ArrayList<>();
        if (bizId != null) {
            conditions.addAll(buildBizIdCondition(bizId));
        }
        conditions.add(TABLE.IP.in(ips));
        return queryHostsByCondition(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizId(long bizId) {
        List<Condition> conditions = buildBizIdCondition(bizId);
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listAllHostInfo(Long start, Long limit) {
        return listHostInfoByConditions(Collections.emptyList(), start, limit);
    }

    private List<ApplicationHostDTO> listHostInfoByConditions(Collection<Condition> conditions) {
        return listHostInfoByConditions(conditions, null, null);
    }

    private List<ApplicationHostDTO> listHostInfoByConditions(Collection<Condition> conditions, Long start,
                                                              Long limit) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        val query = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(conditions)
            .orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc());
        Result<Record> records;
        if (start == null || start < 0) {
            start = 0L;
        }
        if (limit != null && limit > 0) {
            records = query.limit(start, limit).fetch();
        } else {
            records = query.fetch();
        }
        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(records)) {
            records.map(record -> hostInfoList.add(extractData(record)));
        }
        return hostInfoList;
    }

    @Override
    public List<ApplicationHostDTO> listHostInfo(Collection<Long> bizIds, Collection<String> ips) {
        List<Condition> conditions = new ArrayList<>();
        if (bizIds != null) {
            conditions.add(TABLE.APP_ID.in(bizIds.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        conditions.add(TABLE.IP.in(ips.parallelStream().map(String::trim).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizIds(Collection<Long> bizIds, Long start, Long limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.in(bizIds.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions, start, limit);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.in(hostIds.parallelStream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public Long countHostInfoBySearchContents(Collection<Long> bizIds, Collection<Long> moduleIds,
                                              Collection<Long> cloudAreaIds, List<String> searchContents,
                                              Integer agentStatus) {
        List<Long> hostIdList = getHostIdListBySearchContents(bizIds, moduleIds, cloudAreaIds, searchContents,
            agentStatus, null, null);
        return (long) (hostIdList.size());
    }

    @Override
    public Long countHostByIdAndStatus(Collection<Long> hostIds, AgentStatusEnum agentStatus) {
        List<Condition> conditions = new ArrayList<>();
        if (hostIds != null) {
            conditions.add(TABLE.HOST_ID.in(hostIds));
        }
        if (agentStatus != null) {
            conditions.add(TABLE.IS_AGENT_ALIVE.eq(UByte.valueOf(agentStatus.getValue())));
        }
        return countHostByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoBySearchContents(Collection<Long> bizIds,
                                                                 Collection<Long> moduleIds,
                                                                 Collection<Long> cloudAreaIds,
                                                                 List<String> searchContents,
                                                                 Integer agentStatus,
                                                                 Long start,
                                                                 Long limit) {
        List<Long> hostIdList = getHostIdListBySearchContents(bizIds, moduleIds, cloudAreaIds, searchContents,
            agentStatus, start, limit);
        return listHostInfoByHostIds(hostIdList);
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
        if (records.size() >= 1) {
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
    public List<ApplicationHostDTO> listHostInfoBySourceAndIps(long cloudAreaId, Set<String> ips) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.CLOUD_AREA_ID.eq(ULong.valueOf(cloudAreaId)));
        conditions.add(TABLE.IP.in(ips));

        Result<Record> result = context
            .select(ALL_FIELDS)
            .from(TABLE)
            .where(conditions)
            .orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc())
            .fetch();

        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(result)) {
            result.map(record -> hostInfoList.add(extractData(record)));
        }
        return hostInfoList;
    }

    @Override
    public PageData<ApplicationHostDTO> listHostInfoByPage(ApplicationHostDTO applicationHostInfoCondition,
                                                           BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildBizIdCondition(applicationHostInfoCondition.getBizId());
        conditions.addAll(buildCondition(applicationHostInfoCondition, baseSearchCondition));

        long hostCount = countHostByConditions(conditions);

        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);

        Result<Record> result = context.select(ALL_FIELDS)
            .from(TABLE)
            .where(conditions)
            .orderBy(TABLE.IS_AGENT_ALIVE.desc(), TABLE.HOST_ID.asc())
            .limit(start, length)
            .fetch();

        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(result)) {
            result.map(record -> hostInfoList.add(extractData(record)));
        }

        PageData<ApplicationHostDTO> hostInfoPageData = new PageData<>();
        hostInfoPageData.setTotal(hostCount);
        hostInfoPageData.setStart(start);
        hostInfoPageData.setPageSize(length);
        hostInfoPageData.setData(hostInfoList);

        return hostInfoPageData;
    }

    private void setDefaultValue(ApplicationHostDTO applicationHostDTO) {
        if (applicationHostDTO.getHostId() == null) {
            applicationHostDTO.setHostId(-1L);
        }
        if (applicationHostDTO.getBizId() == null) {
            applicationHostDTO.setBizId(-1L);
        }
        if (applicationHostDTO.getCloudAreaId() == null) {
            applicationHostDTO.setCloudAreaId(-1L);
        }
        if (applicationHostDTO.getSetId() == null) {
            applicationHostDTO.setSetId(new ArrayList<>());
        }
        if (applicationHostDTO.getGseAgentAlive() == null) {
            applicationHostDTO.setGseAgentAlive(true);
        }
        if (applicationHostDTO.getIp() == null) {
            applicationHostDTO.setIp("0.0.0.0");
        }
        if (applicationHostDTO.getDisplayIp() == null) {
            applicationHostDTO.setDisplayIp("0.0.0.0");
        }
    }

    private List<HostTopoDTO> genHostTopoDTOList(ApplicationHostDTO applicationHostDTO) {
        List<Long> setIdList = applicationHostDTO.getSetId();
        List<Long> moduleIdList = applicationHostDTO.getModuleId();
        if (setIdList == null || moduleIdList == null) {
            return Collections.emptyList();
        } else if (setIdList.size() != moduleIdList.size()) {
            throw new RuntimeException("setIdList.size()!=moduleIdList.size(),hostInfo=" + JsonUtils.toJson(applicationHostDTO));
        } else {
            List<HostTopoDTO> hostTopoDTOList = new ArrayList<>();
            for (int i = 0; i < setIdList.size(); i++) {
                hostTopoDTOList.add(new HostTopoDTO(applicationHostDTO.getHostId(),
                    applicationHostDTO.getBizId(), setIdList.get(i), moduleIdList.get(i)));
            }
            return hostTopoDTOList;
        }
    }

    @Override
    public int insertAppHostWithoutTopo(DSLContext dslContext, ApplicationHostDTO applicationHostDTO) {
        return insertAppHostInfo(dslContext, applicationHostDTO, false);
    }

    @Override
    public int insertAppHostInfo(DSLContext dslContext, ApplicationHostDTO applicationHostDTO) {
        return insertAppHostInfo(dslContext, applicationHostDTO, true);
    }

    private int insertAppHostInfo(DSLContext dslContext, ApplicationHostDTO applicationHostDTO,
                                  Boolean insertTopo) {
        setDefaultValue(applicationHostDTO);
        String moduleIdsStr = null;
        List<Long> moduleIdList = applicationHostDTO.getModuleId();
        if (moduleIdList != null) {
            moduleIdsStr = moduleIdList.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        String setIdsStr = null;
        List<Long> setIdList = applicationHostDTO.getSetId();
        if (setIdList != null) {
            setIdsStr = setIdList.stream().map(Object::toString).collect(Collectors.joining(","));
        }
        String moduleTypeStr = null;
        List<Long> moduleTypeList = applicationHostDTO.getModuleType();
        if (moduleTypeList != null) {
            moduleTypeStr = moduleTypeList.stream().map(Object::toString).collect(Collectors.joining(","));
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
                TABLE.IS_AGENT_ALIVE,
                TABLE.CLOUD_IP
            ).values(
                ULong.valueOf(applicationHostDTO.getHostId()),
                ULong.valueOf(applicationHostDTO.getBizId()),
                applicationHostDTO.getIp(),
                applicationHostDTO.getIpDesc(),
                finalSetIdsStr,
                finalModuleIdsStr,
                ULong.valueOf(applicationHostDTO.getCloudAreaId()),
                applicationHostDTO.getDisplayIp(),
                applicationHostDTO.getOs(),
                applicationHostDTO.getOsType(),
                finalModuleTypeStr,
                UByte.valueOf(applicationHostDTO.getGseAgentAlive() ? 1 : 0),
                applicationHostDTO.getCloudIp()
            );
            try {
                result[0] = query.execute();
            } catch (Throwable t) {
                log.info("SQL=" + query.getSQL(ParamType.INLINED));
                throw t;
            }
            if (insertTopo) {
                List<HostTopoDTO> hostTopoDTOList = genHostTopoDTOList(applicationHostDTO);
                hostTopoDAO.deleteHostTopoByHostId(context, applicationHostDTO.getBizId(),
                    applicationHostDTO.getHostId());
                hostTopoDAO.batchInsertHostTopo(context, hostTopoDTOList);
            }
        });
        return result[0];
    }

    @Override
    public int batchInsertAppHostInfo(DSLContext dslContext, List<ApplicationHostDTO> applicationHostDTOList) {
        int batchSize = 1000;
        int size = applicationHostDTOList.size();
        int start = 0;
        int end;
        int[] affectedNum = new int[]{0};
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<ApplicationHostDTO> subList = applicationHostDTOList.subList(start, end);
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
                    TABLE.IS_AGENT_ALIVE,
                    TABLE.CLOUD_IP
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
                    null,
                    null
                );
                BatchBindStep batchQuery = context.batch(insertQuery);
                List<HostTopoDTO> hostTopoDTOList = new ArrayList<>();
                for (ApplicationHostDTO applicationHostDTO : subList) {
                    setDefaultValue(applicationHostDTO);
                    String moduleIdsStr = null;
                    List<Long> moduleIdList = applicationHostDTO.getModuleId();
                    if (moduleIdList != null) {
                        moduleIdsStr = moduleIdList.stream().map(Object::toString).collect(Collectors.joining(","));
                    }
                    String setIdsStr = null;
                    List<Long> setIdList = applicationHostDTO.getSetId();
                    if (setIdList != null) {
                        setIdsStr = setIdList.stream().map(Object::toString).collect(Collectors.joining(","));
                    }
                    String moduleTypeStr = null;
                    List<Long> moduleTypeList = applicationHostDTO.getModuleType();
                    if (moduleTypeList != null) {
                        moduleTypeStr = moduleTypeList.stream().map(Object::toString).collect(Collectors.joining(","));
                    }
                    batchQuery = batchQuery.bind(
                        ULong.valueOf(applicationHostDTO.getHostId()),
                        ULong.valueOf(applicationHostDTO.getBizId()),
                        applicationHostDTO.getIp(),
                        applicationHostDTO.getIpDesc(),
                        setIdsStr,
                        moduleIdsStr,
                        ULong.valueOf(applicationHostDTO.getCloudAreaId()),
                        applicationHostDTO.getDisplayIp(),
                        applicationHostDTO.getOs(),
                        applicationHostDTO.getOsType(),
                        moduleTypeStr,
                        UByte.valueOf(applicationHostDTO.getGseAgentAlive() ? 1 : 0),
                        applicationHostDTO.getCloudIp()
                    );
                    hostTopoDTOList.addAll(genHostTopoDTOList(applicationHostDTO));
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

    @SuppressWarnings("all")
    @Override
    public boolean existAppHostInfoByHostId(DSLContext dslContext, ApplicationHostDTO applicationHostDTO) {
        setDefaultValue(applicationHostDTO);
        if (applicationHostDTO.getHostId() == -1L) {
            return false;
        }
        String moduleIdsStr =
            applicationHostDTO.getModuleId().stream().map(Object::toString).collect(Collectors.joining(","));
        String setIdsStr = applicationHostDTO.getSetId().stream().map(Object::toString)
            .collect(Collectors.joining(","));
        String moduleTypeStr =
            applicationHostDTO.getModuleType().stream().map(Object::toString).collect(Collectors.joining(","));
        val query = dslContext.selectCount().from(TABLE)
            .where(TABLE.APP_ID.eq(ULong.valueOf(applicationHostDTO.getBizId())))
            .and(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostDTO.getHostId())))
            .and(TABLE.IP.eq(applicationHostDTO.getIp()))
            .and(TABLE.IP_DESC.eq(applicationHostDTO.getIpDesc()))
            .and(TABLE.SET_IDS.eq(setIdsStr))
            .and(TABLE.MODULE_IDS.eq(moduleIdsStr))
            .and(TABLE.CLOUD_AREA_ID.eq(ULong.valueOf(applicationHostDTO.getCloudAreaId())))
            .and(TABLE.DISPLAY_IP.eq(applicationHostDTO.getDisplayIp()))
            .and(TABLE.OS.eq(applicationHostDTO.getOs()))
            .and(TABLE.MODULE_TYPE.eq(moduleTypeStr))
            .and(TABLE.IS_AGENT_ALIVE.eq(UByte.valueOf(applicationHostDTO.getGseAgentAlive() ? 1 : 0)));
        try {
            return query.fetchOne(0, Long.class) >= 1;
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
    }

    @SuppressWarnings("all")
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
    public int updateBizHostInfoByHostId(DSLContext dslContext, Long bizId,
                                         ApplicationHostDTO applicationHostDTO) {
        setDefaultValue(applicationHostDTO);
        if (applicationHostDTO.getHostId() == -1L) {
            return -1;
        }
        String moduleIdsStr =
            applicationHostDTO.getModuleId().stream().map(Object::toString).collect(Collectors.joining(","));
        String setIdsStr = applicationHostDTO.getSetId().stream().map(Object::toString)
            .collect(Collectors.joining(","));
        String moduleTypeStr =
            applicationHostDTO.getModuleType().stream().map(Object::toString).collect(Collectors.joining(","));
        int[] affectedNum = new int[]{-1};
        List<Condition> conditions = new ArrayList<>();
        if (bizId != null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(bizId)));
        }
        Long hostId = applicationHostDTO.getHostId();
        if (hostId != null) {
            conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostDTO.getHostId())));
        }
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            val query = context.update(TABLE)
                .set(TABLE.APP_ID, ULong.valueOf(applicationHostDTO.getBizId()))
                .set(TABLE.IP, applicationHostDTO.getIp())
                .set(TABLE.CLOUD_IP, applicationHostDTO.getCloudIp())
                .set(TABLE.IP_DESC, applicationHostDTO.getIpDesc())
                .set(TABLE.SET_IDS, setIdsStr)
                .set(TABLE.MODULE_IDS, moduleIdsStr)
                .set(TABLE.CLOUD_AREA_ID, ULong.valueOf(applicationHostDTO.getCloudAreaId()))
                .set(TABLE.DISPLAY_IP, applicationHostDTO.getDisplayIp())
                .set(TABLE.OS, applicationHostDTO.getOs())
                .set(TABLE.OS_TYPE, applicationHostDTO.getOsType())
                .set(TABLE.MODULE_TYPE, moduleTypeStr)
                .set(TABLE.IS_AGENT_ALIVE, UByte.valueOf(applicationHostDTO.getGseAgentAlive() ? 1 : 0))
                .where(conditions);
            try {
                affectedNum[0] = query.execute();
            } catch (Throwable t) {
                log.info("SQL=" + query.getSQL(ParamType.INLINED));
                throw t;
            }
            List<HostTopoDTO> hostTopoDTOList = genHostTopoDTOList(applicationHostDTO);
            hostTopoDAO.deleteHostTopoByHostId(context, bizId, hostId);
            hostTopoDAO.batchInsertHostTopo(context, hostTopoDTOList);
        });
        return affectedNum[0];
    }

    @Override
    public int batchUpdateBizHostInfoByHostId(DSLContext dslContext,
                                              List<ApplicationHostDTO> applicationHostDTOList) {
        int batchSize = 1000;
        int size = applicationHostDTOList.size();
        int start = 0;
        int end;
        List<Query> queryList = new ArrayList<>();
        int[] affectedNum = new int[]{0};
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<ApplicationHostDTO> subList = applicationHostDTOList.subList(start, end);
            dslContext.transaction(configuration -> {
                DSLContext context = DSL.using(configuration);
                List<HostTopoDTO> hostTopoDTOList = new ArrayList<>();
                for (ApplicationHostDTO applicationHostDTO : subList) {
                    setDefaultValue(applicationHostDTO);
                    if (applicationHostDTO.getHostId() == -1L) {
                        log.warn("Unexpected hostId==-1,hostInfo={}", applicationHostDTO);
                        continue;
                    }
                    String moduleIdsStr =
                        applicationHostDTO.getModuleId().stream().map(Object::toString)
                            .collect(Collectors.joining(","));
                    String setIdsStr =
                        applicationHostDTO.getSetId().stream().map(Object::toString)
                            .collect(Collectors.joining(","));
                    String moduleTypeStr =
                        applicationHostDTO.getModuleType().stream().map(Object::toString)
                            .collect(Collectors.joining(","));
                    queryList.add(dslContext.update(TABLE)
                        .set(TABLE.APP_ID, ULong.valueOf(applicationHostDTO.getBizId()))
                        .set(TABLE.IP, applicationHostDTO.getIp())
                        .set(TABLE.CLOUD_IP, applicationHostDTO.getCloudIp())
                        .set(TABLE.IP_DESC, applicationHostDTO.getIpDesc())
                        .set(TABLE.SET_IDS, setIdsStr)
                        .set(TABLE.MODULE_IDS, moduleIdsStr)
                        .set(TABLE.CLOUD_AREA_ID, ULong.valueOf(applicationHostDTO.getCloudAreaId()))
                        .set(TABLE.DISPLAY_IP, applicationHostDTO.getDisplayIp())
                        .set(TABLE.OS, applicationHostDTO.getOs())
                        .set(TABLE.OS_TYPE, applicationHostDTO.getOsType())
                        .set(TABLE.MODULE_TYPE, moduleTypeStr)
                        .set(TABLE.IS_AGENT_ALIVE, UByte.valueOf(applicationHostDTO.getGseAgentAlive() ? 1 : 0))
                        .where(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostDTO.getHostId())))
                        .and(TABLE.APP_ID.eq(ULong.valueOf(applicationHostDTO.getBizId())))
                    );
                    hostTopoDTOList.addAll(genHostTopoDTOList(applicationHostDTO));
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
    public int deleteBizHostInfoById(DSLContext dslContext, Long bizId, Long hostId) {
        int[] affectedNum = new int[]{-1};
        List<Condition> conditions = new ArrayList<>();
        if (bizId != null) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(bizId)));
        }
        if (hostId != null) {
            conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(hostId)));
        }
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            affectedNum[0] = context.deleteFrom(TABLE)
                .where(conditions)
                .execute();
            hostTopoDAO.deleteHostTopoByHostId(context, bizId, hostId);
        });
        return affectedNum[0];
    }

    @Override
    public int batchDeleteBizHostInfoById(DSLContext dslContext, Long bizId, List<Long> hostIdList) {
        int[] affectedNum = new int[]{0};
        int batchSize = 1000;
        int size = hostIdList.size();
        List<Condition> conditions = new ArrayList<>();
        dslContext.transaction(configuration -> {
            DSLContext context = DSL.using(configuration);
            int start = 0;
            int end;
            List<Query> queryList = new ArrayList<>();
            do {
                end = start + batchSize;
                end = Math.min(end, size);
                List<Long> subList = hostIdList.subList(start, end);
                if (bizId != null) {
                    queryList.add(context.deleteFrom(TABLE)
                        .where(TABLE.HOST_ID.in(subList.stream().map(ULong::valueOf).collect(Collectors.toList())))
                        .and(TABLE.APP_ID.eq(ULong.valueOf(bizId)))
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
            hostTopoDAO.batchDeleteHostTopo(context, hostIdList);
        });
        return affectedNum[0];
    }

    @Override
    public int deleteBizHostInfoByBizId(DSLContext dslContext, long bizId) {
        // 先查出所有的hostId
        List<Long> hostIds = getHostIdListBySearchContents(Collections.singleton(bizId), null, null, null, null, null
            , null);
        // 删除拓扑信息+主机信息
        return batchDeleteBizHostInfoById(dslContext, bizId, hostIds);
    }

    @Override
    public boolean existsHost(DSLContext dslContext, long bizId, String ip) {
        return dslContext.fetchExists(TABLE, TABLE.APP_ID.eq(ULong.valueOf(bizId)).and(TABLE.IP.eq(ip)));
    }

    @Override
    public ApplicationHostDTO getLatestHost(DSLContext dslContext, long bizId, long cloudAreaId, String ip) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(bizId)));
        conditions.add(TABLE.CLOUD_AREA_ID.eq(ULong.valueOf(cloudAreaId)));
        conditions.add(TABLE.IP.eq(ip));
        Record record = context
            .select(ALL_FIELDS)
            .from(TABLE)
            .where(conditions)
            .orderBy(TABLE.ROW_UPDATE_TIME.desc(), TABLE.HOST_ID.asc())
            .fetchOne();
        return extractData(record);
    }

    @Override
    public long countHostsByBizIds(DSLContext dslContext, Collection<Long> bizIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.in(bizIds));
        return countHostByConditions(conditions);
    }

    @Override
    public long countAllHosts() {
        log.debug("countAllHosts");
        return countHostByConditions(null);
    }

    @Override
    public long countHostsByOsType(String osType) {
        List<Condition> conditions = new ArrayList<>();
        if (osType != null) {
            conditions.add(TABLE.OS_TYPE.eq(osType));
        }
        return countHostByConditions(conditions);
    }

    @Override
    public long syncHostTopo(DSLContext dslContext, Long hostId) {
        ApplicationHostDTO hostInfoDTO = getHostById(hostId);
        if (hostInfoDTO != null) {
            List<HostTopoDTO> hostTopoDTOList = hostTopoDAO.listHostTopoByHostId(dslContext, hostId);
            List<Long> setIds =
                hostTopoDTOList.parallelStream().map(HostTopoDTO::getSetId).collect(Collectors.toList());
            List<Long> moduleIds =
                hostTopoDTOList.parallelStream().map(HostTopoDTO::getModuleId).collect(Collectors.toList());
            List<Long> moduleTypes = moduleIds.parallelStream().map(it -> 1L).collect(Collectors.toList());
            if (!hostTopoDTOList.isEmpty()) {
                hostInfoDTO.setBizId(hostTopoDTOList.get(0).getBizId());
            }
            hostInfoDTO.setSetId(setIds);
            hostInfoDTO.setModuleId(moduleIds);
            hostInfoDTO.setModuleType(moduleTypes);
            return updateBizHostInfoByHostId(dslContext, null, hostInfoDTO);
        }
        return -1L;
    }

    /**
     * 查询符合条件的主机数量
     */
    @SuppressWarnings("all")
    private long countHostByConditions(List<Condition> conditions) {
        if (conditions == null) {
            conditions = Collections.emptyList();
        }
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
    }

    private List<Condition> buildBizIdCondition(long bizId) {
        ApplicationDTO appInfo = applicationDAO.getAppByScope(
            new ResourceScope(ResourceScopeTypeEnum.BIZ, "" + bizId)
        );
        List<Condition> conditions = new ArrayList<>();
        if (appInfo.isBiz()) {
            conditions.add(TABLE.APP_ID.eq(ULong.valueOf(bizId)));
        } else if (!appInfo.isAllBizSet() && appInfo.isBizSet()) {
            List<Long> subBizIds = topologyHelper.getBizSetSubBizIds(appInfo);
            conditions.add(TABLE.APP_ID.in(subBizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        }
        return conditions;
    }

    private List<Condition> buildCondition(ApplicationHostDTO applicationHostInfoCondition,
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

    @Override
    public List<ApplicationHostDTO> listHosts(Collection<IpDTO> hostIps) {
        List<Condition> conditions = new ArrayList<>();
        List<String> cloudIpList = hostIps.stream().map(IpDTO::convertToStrIp).collect(Collectors.toList());
        conditions.add(TABLE.CLOUD_IP.in(cloudIpList));
        return queryHostsByCondition(conditions);
    }

    private List<ApplicationHostDTO> queryHostsByCondition(List<Condition> conditions) {
        Result<Record> result =
            context.select(ALL_FIELDS)
                .from(TABLE)
                .where(conditions)
                .fetch();

        List<ApplicationHostDTO> hostInfoList = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(result)) {
            result.map(record -> hostInfoList.add(extractData(record)));
        }
        return hostInfoList;
    }

    public static ApplicationHostDTO extractData(Record record) {
        if (record == null) {
            return null;
        }

        ApplicationHostDTO applicationHostDTO = new ApplicationHostDTO();
        applicationHostDTO.setBizId(record.get(TABLE.APP_ID).longValue());
        applicationHostDTO.setIp(record.get(TABLE.IP));
        applicationHostDTO.setIpDesc(record.get(TABLE.IP_DESC));
        applicationHostDTO.setGseAgentAlive(record.get(TABLE.IS_AGENT_ALIVE).intValue() == 1);
        List<Long> setIdList = new ArrayList<>();
        String setIdsStr = record.get(TABLE.SET_IDS);
        if (setIdsStr != null) {
            List<Long> list = new ArrayList<>();
            for (String id : setIdsStr.split(",")) {
                if (!id.trim().equals("")) {
                    Long parseLong = Long.parseLong(id);
                    list.add(parseLong);
                }
            }
            setIdList = list;
        }
        applicationHostDTO.setSetId(setIdList);
        applicationHostDTO.setModuleId(StringUtil.strToList(record.get(TABLE.MODULE_IDS), Long.class, ","));
        applicationHostDTO.setCloudAreaId(record.get(TABLE.CLOUD_AREA_ID).longValue());
        applicationHostDTO.setDisplayIp(record.get(TABLE.DISPLAY_IP));
        applicationHostDTO.setOs(record.get(TABLE.OS));
        applicationHostDTO.setOsType(record.get(TABLE.OS_TYPE));
        applicationHostDTO.setModuleType(StringUtil.strToList(record.get(TABLE.MODULE_TYPE), Long.class, ","));
        applicationHostDTO.setHostId(record.get(TABLE.HOST_ID).longValue());
        applicationHostDTO.setCloudIp(record.get(TABLE.CLOUD_IP));
        return applicationHostDTO;
    }
}
