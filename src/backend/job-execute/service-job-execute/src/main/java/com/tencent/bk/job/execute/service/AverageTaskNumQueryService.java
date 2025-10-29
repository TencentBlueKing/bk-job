package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.analysis.api.consts.StatisticsConstants;
import com.tencent.bk.job.analysis.api.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.dao.StatisticsDAO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 平均任务量查询服务
 * 用于查询业务的平均任务执行量统计信息
 */
@Slf4j
@Service
public class AverageTaskNumQueryService {

    private final StatisticsDAO statisticsDAO;

    @Autowired
    public AverageTaskNumQueryService(StatisticsDAO statisticsDAO) {
        this.statisticsDAO = statisticsDAO;
    }

    /**
     * 计算过去一定时间范围内指定业务的平均每天任务执行量
     *
     * @param appId      业务ID
     * @param sampleDays 用于估算每日任务量的样本天数
     * @return 平均每天任务执行量，如果没有数据则返回0
     */
    public long getAverageDailyTaskNum(Long appId, int sampleDays) {
        try {
            // 计算过去几天的日期范围
            String currentDateStr = DateUtils.getCurrentDateStr();
            String startDateStr = DateUtils.getPreviousDateStr(currentDateStr, sampleDays);
            String endDateStr = DateUtils.getPreviousDateStr(currentDateStr, 1);
            // 查询过去几天的任务执行量统计数据
            List<StatisticsDTO> statisticsList = statisticsDAO.getStatisticsListBetweenDate(
                appId,
                StatisticsConstants.RESOURCE_EXECUTED_TASK,
                StatisticsConstants.DIMENSION_TIME_UNIT,
                StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY,
                startDateStr,
                endDateStr
            );

            if (statisticsList == null || statisticsList.isEmpty()) {
                log.debug("No statistics data found for appId={}", appId);
                return 0L;
            }

            // 计算总任务量和有效天数
            long totalTaskCount = 0L;
            int validDays = 0;

            for (StatisticsDTO statistics : statisticsList) {
                if (StringUtils.isBlank(statistics.getValue())) {
                    continue;
                }
                long value = Long.parseLong(statistics.getValue());
                if (value <= 0) {
                    continue;
                }
                totalTaskCount += value;
                validDays++;
            }

            if (validDays == 0) {
                log.debug("No valid statistics data found for appId={}", appId);
                return 0L;
            }

            // 计算平均每天任务量，对小数采用进一法
            long averageDailyTaskNum = (totalTaskCount + validDays - 1) / validDays;

            log.debug(
                "Calculated average daily taskNum for appId={}: " +
                    "totalTaskCount={}, validDays={}, averageDailyTaskNum={}",
                appId,
                totalTaskCount,
                validDays,
                averageDailyTaskNum
            );
            return averageDailyTaskNum;
        } catch (Exception e) {
            String message = MessageFormatter.format(
                "Failed to calculate average daily taskNum for appId={}", appId
            ).getMessage();
            log.error(message, e);
            return 0L;
        }
    }
}
