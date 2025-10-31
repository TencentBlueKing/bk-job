/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.BasicHostDTO;
import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.common.model.dto.HostStatusNumStatisticsDTO;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.mysql.util.JooqDataTypeUtil;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.dao.NoTenantHostDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import lombok.var;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.BatchBindStep;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.conf.ParamType;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 租户无关的主机DAO
 */
@Slf4j
@Repository
public class NoTenantHostDAOImpl extends AbstractBaseHostDAO implements NoTenantHostDAO {

    @Autowired
    public NoTenantHostDAOImpl(@Qualifier("job-manage-dsl-context") DSLContext context,
                               ApplicationDAO applicationDAO,
                               HostTopoDAO hostTopoDAO,
                               TopologyHelper topologyHelper) {
        super(context, applicationDAO, hostTopoDAO, topologyHelper);
    }

    @Override
    protected List<Condition> getBasicConditions() {
        return new ArrayList<>();
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByIps(Collection<String> ips) {
        return batchQueryHostInfo(ips, this::listHostInfoByIpsIndeed);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByCloudIps(Collection<String> cloudIps) {
        return batchQueryHostInfo(cloudIps, this::listHostInfoByCloudIpsIndeed);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByIpv6s(Collection<String> ipv6s) {
        return super.listHostInfoByIpv6s(ipv6s);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByHostNames(Collection<String> hostNames) {
        return super.listHostInfoByHostNames(hostNames);
    }

    @Override
    public Long countHostInfoBySearchContents(Collection<Long> bizIds,
                                              Collection<Long> moduleIds,
                                              Collection<Long> cloudAreaIds,
                                              List<String> searchContents,
                                              Integer agentStatus) {
        return super.countHostInfoBySearchContents(bizIds, moduleIds, cloudAreaIds, searchContents, agentStatus);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoBySearchContents(Collection<Long> bizIds,
                                                                 Collection<Long> moduleIds,
                                                                 Collection<Long> cloudAreaIds,
                                                                 List<String> searchContents,
                                                                 Integer agentStatus,
                                                                 Long start,
                                                                 Long limit) {
        return super.listHostInfoBySearchContents(
            bizIds,
            moduleIds,
            cloudAreaIds,
            searchContents,
            agentStatus,
            start,
            limit
        );
    }

    @Override
    public List<Long> getHostIdListBySearchContents(Collection<Long> bizIds,
                                                    Collection<Long> moduleIds,
                                                    Collection<Long> cloudAreaIds,
                                                    List<String> searchContents,
                                                    Integer agentAlive,
                                                    Long start,
                                                    Long limit) {
        return super.getHostIdListBySearchContents(
            bizIds,
            moduleIds,
            cloudAreaIds,
            searchContents,
            agentAlive,
            start,
            limit
        );
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByMultiKeys(Collection<Long> bizIds,
                                                            Collection<Long> moduleIds,
                                                            Collection<Long> cloudAreaIds,
                                                            Collection<String> ipKeys,
                                                            Collection<String> ipv6Keys,
                                                            Collection<String> hostNameKeys,
                                                            Collection<String> osNameKeys,
                                                            Integer agentAlive,
                                                            Long start,
                                                            Long limit) {
        return super.listHostInfoByMultiKeys(
            bizIds,
            moduleIds,
            cloudAreaIds,
            ipKeys,
            ipv6Keys,
            hostNameKeys,
            osNameKeys,
            agentAlive,
            start,
            limit
        );
    }

    @Override
    public List<Long> getHostIdListByMultiKeys(Collection<Long> bizIds,
                                               Collection<Long> moduleIds,
                                               Collection<Long> cloudAreaIds,
                                               Collection<String> ipKeys,
                                               Collection<String> ipv6Keys,
                                               Collection<String> hostNameKeys,
                                               Collection<String> osNameKeys,
                                               Integer agentAlive,
                                               Long start,
                                               Long limit) {
        return super.getHostIdListByMultiKeys(
            bizIds,
            moduleIds,
            cloudAreaIds,
            ipKeys,
            ipv6Keys,
            hostNameKeys,
            osNameKeys,
            agentAlive,
            start,
            limit
        );
    }

    @Override
    public Long countHostInfoByMultiKeys(Collection<Long> bizIds,
                                         Collection<Long> moduleIds,
                                         Collection<Long> cloudAreaIds,
                                         Collection<String> ipKeys,
                                         Collection<String> ipv6Keys,
                                         Collection<String> hostNameKeys,
                                         Collection<String> osNameKeys,
                                         Integer agentAlive) {
        return super.countHostInfoByMultiKeys(
            bizIds,
            moduleIds,
            cloudAreaIds,
            ipKeys,
            ipv6Keys,
            hostNameKeys,
            osNameKeys,
            agentAlive
        );
    }

    @Override
    public ApplicationHostDTO getHostById(Long hostId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(JooqDataTypeUtil.buildULong(hostId)));
        Record record = context.select(ALL_FIELDS).from(TABLE).where(conditions).fetchOne();
        return extractData(record);
    }

    @Override
    public List<Long> listHostId(long bizId, long minUpdateTimeMills, long maxUpdateTimeMills) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(bizId)));
        conditions.add(TABLE.LAST_MODIFY_TIME.greaterThan(JooqDataTypeUtil.buildULong(minUpdateTimeMills)));
        conditions.add(TABLE.LAST_MODIFY_TIME.lessThan(JooqDataTypeUtil.buildULong(maxUpdateTimeMills)));
        return listHostIdByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizId(long bizId) {
        List<Condition> conditions = buildBizIdCondition(bizId);
        return listHostInfoByConditions(conditions);
    }

    @Override
    public List<BasicHostDTO> listBasicHostInfo(Collection<Long> hostIds) {
        List<Condition> conditions = buildHostIdsCondition(hostIds);
        return listBasicHostInfoByConditions(conditions);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByBizIds(Collection<Long> bizIds, Long start, Long limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.in(bizIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions, start, limit);
    }

    @Override
    public List<ApplicationHostDTO> listHostInfoByHostIds(Collection<Long> hostIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.in(hostIds.stream().map(ULong::valueOf).collect(Collectors.toList())));
        return listHostInfoByConditions(conditions);
    }

    @Override
    public int insertHostWithoutTopo(ApplicationHostDTO applicationHostDTO) {
        return insertOrUpdateHost(context, applicationHostDTO);
    }

    @SuppressWarnings("resource")
    private int insertOrUpdateHost(DSLContext defaultContext,
                                   ApplicationHostDTO applicationHostDTO) {
        int result;
        String finalSetIdsStr = applicationHostDTO.getSetIdsStr();
        String finalModuleIdsStr = applicationHostDTO.getModuleIdsStr();
        String finalModuleTypeStr = applicationHostDTO.getModuleTypeStr();
        ULong bizId = ULong.valueOf(applicationHostDTO.getBizId());
        String ip = applicationHostDTO.getIp();
        String ipv6 = applicationHostDTO.preferFullIpv6();
        String agentId = applicationHostDTO.getAgentId();
        String ipDesc = applicationHostDTO.getHostName();
        String displayIp = applicationHostDTO.getDisplayIp();
        String os = applicationHostDTO.getOsName();
        String osType = applicationHostDTO.getOsType();
        UByte gseAgentAlive = UByte.valueOf(applicationHostDTO.getGseAgentAlive() ? 1 : 0);
        String cloudIp = applicationHostDTO.getCloudIp();
        String cloudVendor = applicationHostDTO.getCloudVendorId();
        Long lastTime = applicationHostDTO.getLastTime();
        String tenantId = applicationHostDTO.getTenantId();
        var query = defaultContext.insertInto(TABLE,
            TABLE.HOST_ID,
            TABLE.APP_ID,
            TABLE.IP,
            TABLE.IP_V6,
            TABLE.AGENT_ID,
            TABLE.IP_DESC,
            TABLE.SET_IDS,
            TABLE.MODULE_IDS,
            TABLE.CLOUD_ID,
            TABLE.DISPLAY_IP,
            TABLE.OS,
            TABLE.OS_TYPE,
            TABLE.MODULE_TYPE,
            TABLE.IS_AGENT_ALIVE,
            TABLE.CLOUD_IP,
            TABLE.LAST_MODIFY_TIME,
            TABLE.CLOUD_VENDOR_ID,
            TABLE.LAST_TIME,
            TABLE.TENANT_ID
        ).values(
            JooqDataTypeUtil.buildULong(applicationHostDTO.getHostId()),
            bizId,
            ip,
            ipv6,
            agentId,
            ipDesc,
            finalSetIdsStr,
            finalModuleIdsStr,
            applicationHostDTO.getCloudAreaId(),
            displayIp,
            os,
            osType,
            finalModuleTypeStr,
            gseAgentAlive,
            cloudIp,
            JooqDataTypeUtil.buildULong(System.currentTimeMillis()),
            cloudVendor,
            lastTime,
            tenantId
        );
        try {
            result = query.onDuplicateKeyUpdate()
                .set(TABLE.APP_ID, bizId)
                .set(TABLE.IP, ip)
                .set(TABLE.IP_V6, ipv6)
                .set(TABLE.AGENT_ID, agentId)
                .set(TABLE.IP_DESC, ipDesc)
                .set(TABLE.SET_IDS, finalSetIdsStr)
                .set(TABLE.MODULE_IDS, finalModuleIdsStr)
                .set(TABLE.CLOUD_ID, applicationHostDTO.getCloudAreaId())
                .set(TABLE.DISPLAY_IP, displayIp)
                .set(TABLE.OS, os)
                .set(TABLE.OS_TYPE, osType)
                .set(TABLE.MODULE_TYPE, finalModuleTypeStr)
                .set(TABLE.IS_AGENT_ALIVE, gseAgentAlive)
                .set(TABLE.CLOUD_IP, cloudIp)
                .set(TABLE.CLOUD_VENDOR_ID, cloudVendor)
                .set(TABLE.LAST_TIME, lastTime)
                .set(TABLE.TENANT_ID, tenantId)
                .execute();
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
        return result;
    }

    @Override
    public int batchInsertHost(List<ApplicationHostDTO> applicationHostDTOList) {
        int batchSize = 1000;
        int size = applicationHostDTOList.size();
        int start = 0;
        int end;
        int affectedNum = 0;
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<ApplicationHostDTO> subList = applicationHostDTOList.subList(start, end);
            val insertQuery = context.insertInto(TABLE,
                TABLE.HOST_ID,
                TABLE.APP_ID,
                TABLE.IP,
                TABLE.IP_V6,
                TABLE.AGENT_ID,
                TABLE.IP_DESC,
                TABLE.SET_IDS,
                TABLE.MODULE_IDS,
                TABLE.CLOUD_ID,
                TABLE.DISPLAY_IP,
                TABLE.OS,
                TABLE.OS_TYPE,
                TABLE.MODULE_TYPE,
                TABLE.IS_AGENT_ALIVE,
                TABLE.CLOUD_IP,
                TABLE.LAST_MODIFY_TIME,
                TABLE.CLOUD_VENDOR_ID,
                TABLE.LAST_TIME,
                TABLE.TENANT_ID
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
                null,
                null,
                null,
                null,
                null,
                null,
                null
            );
            BatchBindStep batchQuery = context.batch(insertQuery);
            for (ApplicationHostDTO applicationHostDTO : subList) {
                batchQuery = batchQuery.bind(
                    JooqDataTypeUtil.buildULong(applicationHostDTO.getHostId()),
                    JooqDataTypeUtil.buildULong(applicationHostDTO.getBizId()),
                    applicationHostDTO.getIp(),
                    applicationHostDTO.preferFullIpv6(),
                    applicationHostDTO.getAgentId(),
                    applicationHostDTO.getHostName(),
                    applicationHostDTO.getSetIdsStr(),
                    applicationHostDTO.getModuleIdsStr(),
                    applicationHostDTO.getCloudAreaId(),
                    applicationHostDTO.getDisplayIp(),
                    applicationHostDTO.getOsName(),
                    applicationHostDTO.getOsType(),
                    applicationHostDTO.getModuleTypeStr(),
                    JooqDataTypeUtil.buildUByte(applicationHostDTO.getAgentStatusValue()),
                    applicationHostDTO.getCloudIp(),
                    JooqDataTypeUtil.buildULong(System.currentTimeMillis()),
                    applicationHostDTO.getCloudVendorId(),
                    applicationHostDTO.getLastTime(),
                    applicationHostDTO.getTenantId()
                );
            }
            int[] results = batchQuery.execute();
            for (int result : results) {
                affectedNum += result;
            }
            start += batchSize;
        } while (end < size);
        return affectedNum;
    }

    @SuppressWarnings("all")
    @Override
    public boolean existAppHostInfoByHostId(Long hostId) {
        val query = context.selectCount().from(TABLE)
            .where(TABLE.HOST_ID.eq(JooqDataTypeUtil.buildULong(hostId)));
        try {
            return query.fetchOne(0, Long.class) >= 1;
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
    }

    @Override
    public int updateHostAttrsByHostId(ApplicationHostDTO applicationHostDTO) {
        checkHostId(applicationHostDTO);
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostDTO.getHostId())));
        return updateHostAttrsByConditions(applicationHostDTO, conditions);
    }

    @Override
    public int updateHostAttrsBeforeLastTime(ApplicationHostDTO applicationHostDTO) {
        checkHostId(applicationHostDTO);
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(applicationHostDTO.getHostId())));
        conditions.add(TABLE.LAST_TIME.lessThan(applicationHostDTO.getLastTime()));
        return updateHostAttrsByConditions(applicationHostDTO, conditions);
    }

    private void checkHostId(ApplicationHostDTO applicationHostDTO) {
        Long hostId = applicationHostDTO.getHostId();
        if (hostId == null || hostId <= 0) {
            FormattingTuple msg = MessageFormatter.format(
                "fail to update host, hostId is invalid:{}",
                applicationHostDTO
            );
            log.error(msg.getMessage());
            throw new InternalException(msg.getMessage(), ErrorCode.INTERNAL_ERROR);
        }
    }

    public int updateHostAttrsByConditions(ApplicationHostDTO applicationHostDTO, Collection<Condition> conditions) {
        val query = context.update(TABLE)
            .set(TABLE.CLOUD_ID, applicationHostDTO.getCloudAreaId())
            .set(TABLE.IP, applicationHostDTO.getIp())
            .set(TABLE.IP_V6, applicationHostDTO.preferFullIpv6())
            .set(TABLE.AGENT_ID, applicationHostDTO.getAgentId())
            .set(TABLE.DISPLAY_IP, applicationHostDTO.getDisplayIp())
            .set(TABLE.CLOUD_IP, applicationHostDTO.getCloudIp())
            .set(TABLE.IP_DESC, applicationHostDTO.getHostName())
            .set(TABLE.OS, applicationHostDTO.getOsName())
            .set(TABLE.OS_TYPE, applicationHostDTO.getOsType())
            .set(TABLE.IS_AGENT_ALIVE, UByte.valueOf(applicationHostDTO.getAgentStatusValue()))
            .set(TABLE.LAST_MODIFY_TIME, JooqDataTypeUtil.buildULong(System.currentTimeMillis()))
            .set(TABLE.CLOUD_VENDOR_ID, applicationHostDTO.getCloudVendorId())
            .set(TABLE.LAST_TIME, applicationHostDTO.getLastTime())
            .set(TABLE.TENANT_ID, applicationHostDTO.getTenantId())
            .where(conditions);
        try {
            return query.execute();
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
    }

    private Query buildQueryWithHostAndConditions(ApplicationHostDTO applicationHostDTO, List<Condition> conditions) {
        return context.update(TABLE)
            .set(TABLE.APP_ID, JooqDataTypeUtil.buildULong(applicationHostDTO.getBizId()))
            .set(TABLE.IP, applicationHostDTO.getIp())
            .set(TABLE.IP_V6, applicationHostDTO.preferFullIpv6())
            .set(TABLE.AGENT_ID, applicationHostDTO.getAgentId())
            .set(TABLE.CLOUD_IP, applicationHostDTO.getCloudIp())
            .set(TABLE.IP_DESC, applicationHostDTO.getHostName())
            .set(TABLE.SET_IDS, applicationHostDTO.getSetIdsStr())
            .set(TABLE.MODULE_IDS, applicationHostDTO.getModuleIdsStr())
            .set(TABLE.CLOUD_ID, applicationHostDTO.getCloudAreaId())
            .set(TABLE.DISPLAY_IP, applicationHostDTO.getDisplayIp())
            .set(TABLE.OS, applicationHostDTO.getOsName())
            .set(TABLE.OS_TYPE, applicationHostDTO.getOsType())
            .set(TABLE.MODULE_TYPE, applicationHostDTO.getModuleTypeStr())
            .set(TABLE.IS_AGENT_ALIVE, JooqDataTypeUtil.buildUByte(applicationHostDTO.getAgentStatusValue()))
            .set(TABLE.LAST_MODIFY_TIME, JooqDataTypeUtil.buildULong(System.currentTimeMillis()))
            .set(TABLE.CLOUD_VENDOR_ID, applicationHostDTO.getCloudVendorId())
            .set(TABLE.LAST_TIME, applicationHostDTO.getLastTime())
            .set(TABLE.TENANT_ID, applicationHostDTO.getTenantId())
            .where(conditions);
    }

    @Override
    public int batchUpdateHostsBeforeLastTime(List<ApplicationHostDTO> hostList) {
        int batchSize = 1000;
        int size = hostList.size();
        int start = 0;
        int end;
        List<Query> queryList = new ArrayList<>();
        int affectedNum = 0;
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<ApplicationHostDTO> subList = hostList.subList(start, end);
            for (ApplicationHostDTO host : subList) {
                List<Condition> conditions = new ArrayList<>();
                conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(host.getHostId())));
                conditions.add(TABLE.LAST_TIME.lessThan(host.getLastTime()));
                queryList.add(buildQueryWithHostAndConditions(host, conditions));
            }
            int[] results = context.batch(queryList).execute();
            queryList.clear();
            for (int result : results) {
                affectedNum += result;
            }
            start += batchSize;
        } while (end < size);
        return affectedNum;
    }

    @JobTransactional(transactionManager = "jobManageTransactionManager")
    @Override
    public int deleteHostBeforeOrEqualLastTime(Long bizId, Long hostId, Long lastTime) {
        int affectedNum;
        List<Condition> conditions = new ArrayList<>();
        if (bizId != null) {
            conditions.add(TABLE.APP_ID.eq(JooqDataTypeUtil.buildULong(bizId)));
        }
        if (hostId != null) {
            conditions.add(TABLE.HOST_ID.eq(JooqDataTypeUtil.buildULong(hostId)));
        }
        if (lastTime != null) {
            conditions.add(TABLE.LAST_TIME.lessOrEqual(lastTime));
        }
        affectedNum = context.deleteFrom(TABLE)
            .where(conditions)
            .execute();
        hostTopoDAO.deleteHostTopoByHostId(bizId, hostId);
        return affectedNum;
    }

    private int batchDeleteBizHostInfoById(Long bizId, List<Long> hostIdList) {
        int affectedNum = 0;
        int batchSize = 1000;
        int size = hostIdList.size();
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
                    affectedNum += result;
                }
            }
            hostTopoDAO.batchDeleteHostTopo(subList);
            start += batchSize;
        } while (end < size);
        if (!queryList.isEmpty()) {
            int[] results = context.batch(queryList).execute();
            for (int result : results) {
                affectedNum += result;
            }
        }
        hostTopoDAO.batchDeleteHostTopo(hostIdList);
        return affectedNum;
    }

    @Override
    @JobTransactional(transactionManager = "jobManageTransactionManager")
    public int deleteBizHostInfoByBizId(long bizId) {
        // 先查出所有的hostId
        List<Long> hostIds = getHostIdListByConditions(buildBizIdCondition(bizId), null, null);
        // 删除拓扑信息+主机信息
        return batchDeleteBizHostInfoById(bizId, hostIds);
    }

    @Override
    public int deleteByBasicHost(List<BasicHostDTO> basicHostList) {
        int affectedNum = 0;
        int batchSize = 1000;
        int size = basicHostList.size();
        int start = 0;
        int end;
        List<Query> queryList = new ArrayList<>();
        do {
            end = start + batchSize;
            end = Math.min(end, size);
            List<BasicHostDTO> subList = basicHostList.subList(start, end);
            for (BasicHostDTO basicHost : subList) {
                queryList.add(context.deleteFrom(TABLE)
                    .where(TABLE.HOST_ID.eq(JooqDataTypeUtil.buildULong(basicHost.getHostId())))
                    .and(TABLE.LAST_TIME.eq(basicHost.getLastTime()))
                );
            }
            int[] results = context.batch(queryList).execute();
            queryList.clear();
            for (int result : results) {
                affectedNum += result;
            }
            start += batchSize;
        } while (end < size);
        return affectedNum;
    }

    @Override
    public List<HostSimpleDTO> listAllHostSimpleInfo() {
        val query = context.select(SIMPLE_FIELDS)
            .from(TABLE);
        Result<Record> records = query.fetch();
        List<HostSimpleDTO> hostInfoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(records)) {
            records.map(record -> hostInfoList.add(extractSimpleData(record)));
        }
        return hostInfoList;
    }

    @Override
    public List<BasicHostDTO> listAllBasicHost() {
        val query = context.select(BASIC_FIELDS)
            .from(TABLE);
        Result<Record> records = query.fetch();
        List<BasicHostDTO> basicHostList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(records)) {
            records.map(record -> basicHostList.add(extractBasicHost(record)));
        }
        return basicHostList;
    }

    @Override
    public int batchUpdateHostStatusByHostIds(int status, List<Long> hostIdList) {
        return context.update(TABLE)
            .set(TABLE.IS_AGENT_ALIVE, UByte.valueOf(status))
            .where(TABLE.HOST_ID.in(hostIdList))
            .execute();
    }

    @Override
    public long countAllHosts() {
        log.debug("countAllHosts");
        return countHostByConditions(null);
    }

    @Override
    public int syncHostTopo(Long hostId) {
        ApplicationHostDTO hostInfoDTO = getHostById(hostId);
        if (hostInfoDTO != null) {
            List<HostTopoDTO> hostTopoDTOList = hostTopoDAO.listHostTopoByHostId(hostId);
            List<Long> setIds =
                hostTopoDTOList.stream().map(HostTopoDTO::getSetId).collect(Collectors.toList());
            List<Long> moduleIds =
                hostTopoDTOList.stream().map(HostTopoDTO::getModuleId).collect(Collectors.toList());
            List<Long> moduleTypes = moduleIds.stream().map(it -> 1L).collect(Collectors.toList());
            if (!hostTopoDTOList.isEmpty()) {
                hostInfoDTO.setBizId(hostTopoDTOList.get(0).getBizId());
            } else {
                hostInfoDTO.setBizId(JobConstants.PUBLIC_APP_ID);
            }
            hostInfoDTO.setSetId(setIds);
            hostInfoDTO.setModuleId(moduleIds);
            hostInfoDTO.setModuleType(moduleTypes);
            return updateHostTopoAttrsByHostId(hostInfoDTO);
        }
        return -1;
    }

    private int updateHostTopoAttrsByHostId(ApplicationHostDTO host) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.eq(ULong.valueOf(host.getHostId())));
        val query = context.update(TABLE)
            .set(TABLE.APP_ID, ULong.valueOf(host.getBizId()))
            .set(TABLE.SET_IDS, host.getSetIdsStr())
            .set(TABLE.MODULE_IDS, host.getModuleIdsStr())
            .set(TABLE.MODULE_TYPE, host.getModuleTypeStr())
            .where(conditions);
        try {
            return query.execute();
        } catch (Throwable t) {
            log.info("SQL=" + query.getSQL(ParamType.INLINED));
            throw t;
        }
    }

    private List<Condition> buildHostIdsCondition(Collection<Long> hostIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.HOST_ID.in(hostIds));
        return conditions;
    }

    @Override
    public List<ApplicationHostDTO> listHostsByCloudIps(Collection<String> cloudIps) {
        List<Condition> conditions = getBasicConditions();
        conditions.add(TABLE.CLOUD_IP.in(cloudIps));
        return queryHostsByCondition(conditions);
    }

    @Override
    public List<HostStatusNumStatisticsDTO> countHostStatusNumByBizIds(List<Long> bizIds) {
        return super.countHostStatusNumByBizIds(bizIds);
    }

}
