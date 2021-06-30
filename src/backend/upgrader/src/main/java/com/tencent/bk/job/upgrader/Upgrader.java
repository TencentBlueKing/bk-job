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

package com.tencent.bk.job.upgrader;

import com.tencent.bk.job.common.util.CompareUtil;
import com.tencent.bk.job.upgrader.anotation.ExecuteTimeEnum;
import com.tencent.bk.job.upgrader.anotation.UpgradeTask;
import com.tencent.bk.job.upgrader.task.IUpgradeTask;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Slf4j
public class Upgrader {

    public Upgrader() {
    }

    public static void usage() {
        log.info("===========================================Usage==============================================");
        log.info("Usage: java -Dconfig.file=/path/to/config/file -jar upgrader-[x.x.x.x].jar [fromVersion] " +
            "[toVersion] [executeTime] ");
        log.info("fromVersion is the current version of Job, example:3.1.4.3");
        log.info("toVersion is the target version of Job to upgrade, example:3.2.7.3");
        log.info("executeTime is the time point to execute upgrade tasks, which value can be:BEFORE_UPDATE_JOB/AFTER_UPDATE_JOB, " +
            "BEFORE_UPDATE_JOB means executing upgrade tasks before upgrading job service jar, while AFTER_UPDATE_JOB means executing upgrade tasks after upgrading job service jar");
        log.info("Example: java -Dconfig.file=/data/bkee/job/upgrader.properties -jar upgrader-1.0.0.0.jar 3.1.4.3 3.2.7.3 " +
            "BEFORE_UPDATE_JOB");
        log.info("==========================================用法说明=============================================");
        log.info("工具用法: java -Dconfig.file=/path/to/config/file -jar upgrader-[x.x.x.x].jar [fromVersion] " +
            "[toVersion] [executeTime] ");
        log.info("fromVersion为当前作业平台版本，如3.1.4.3");
        log.info("toVersion为目标作业平台版本，如3.2.7.3");
        log.info("executeTime为升级任务执行的时间点，取值为BEFORE_UPDATE_JOB、AFTER_UPDATE_JOB，" +
            "在更新作业平台进程前执行本工具填写BEFORE_UPDATE_JOB，更新进程后执行则填写AFTER_UPDATE_JOB");
        log.info("示例：java -Dconfig.file=/data/bkee/job/upgrader.properties -jar upgrader-1.0.0.0.jar 3.1.4.3 3.2.7.3 " +
            "BEFORE_UPDATE_JOB");
    }

    public static boolean isTaskNeedToExecute(
        String fromVersion,
        String toVersion,
        String dataStartVersion,
        String taskTargetVersion
    ) {
        // 需要升级的旧数据已经产生
        // && 当前版本比升级任务的目标版本低
        // && 升级任务的目标版本比要升级到的新版本低
        return CompareUtil.compareVersion(dataStartVersion, fromVersion) <= 0
            && CompareUtil.compareVersion(fromVersion, taskTargetVersion) < 0
            && CompareUtil.compareVersion(taskTargetVersion, toVersion) <= 0;
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            usage();
            return;
        }
        String fromVersion = args[0];
        String toVersion = args[1];
        String executeTime = args[2];
        log.info("fromVersion={}", fromVersion);
        log.info("toVersion={}", toVersion);
        log.info("executeTime={}", executeTime);

        Properties properties = new Properties();
        String configFilePath = System.getProperty("config.file");
        if (StringUtils.isNotBlank(configFilePath)) {
            try {
                properties.load(new BufferedReader(new FileReader(configFilePath)));
            } catch (IOException e) {
                log.warn("Cannot read configFile from path:{}, exit", configFilePath, e);
                return;
            }
        } else {
            log.warn("Config file is empty");
            return;
        }
        log.info("Upgrader begin to run");
        // 找出所有UpgradeTask
        Reflections reflections = new Reflections(
            "com.tencent.bk.job.upgrader.task",
            new SubTypesScanner(false),
            new TypeAnnotationsScanner()
        );
        List<Triple<Class<? extends Object>, String, Integer>> upgradeTaskList = new ArrayList<>();
        Set<Class<?>> upgradeTaskSet = reflections.getTypesAnnotatedWith(UpgradeTask.class);
        // 筛选
        upgradeTaskSet.forEach(clazz -> {
            UpgradeTask anotation = clazz.getAnnotation(UpgradeTask.class);
            String dataStartVersion = anotation.dataStartVersion();
            String targetVersion = anotation.targetVersion();
            ExecuteTimeEnum targetExecuteTime = anotation.targetExecuteTime();
            int priority = anotation.priority();
            log.info("Found upgradeTask:[{}] for version {}, dataStartVersion={}, priority={}", clazz.getName(),
                targetVersion, dataStartVersion, priority);
            if (targetExecuteTime.name().equalsIgnoreCase(executeTime)
                && isTaskNeedToExecute(fromVersion, toVersion, dataStartVersion, targetVersion)) {
                log.info("{}-->{},{},Add task {}({},{})", fromVersion, toVersion, executeTime,
                    clazz.getSimpleName(), targetVersion, targetExecuteTime);
                upgradeTaskList.add(Triple.of(clazz, targetVersion, priority));
            } else {
                log.info("{}-->{},{},Ignore task {}({},{})", fromVersion, toVersion, executeTime,
                    clazz.getSimpleName(), targetVersion, targetExecuteTime);
            }
        });
        // 排序
        upgradeTaskList.sort((o1, o2) -> {
            int result = CompareUtil.compareVersion(o1.getMiddle(), o2.getMiddle());
            if (result != 0) return result;
            return o1.getRight().compareTo(o2.getRight());
        });
        log.info("upgradeTaskList after sort:");
        upgradeTaskList.forEach(entry -> {
            log.info("[{}] for version {}, priority={}", entry.getLeft(), entry.getMiddle(), entry.getRight());
        });
        int taskSize = upgradeTaskList.size();
        int successfulTaskCount = 0;
        // 运行
        for (Triple<Class<?>, String, Integer> entry : upgradeTaskList) {
            Class<? extends Object> clazz = entry.getLeft();
            try {
                IUpgradeTask upgradeTask = null;
                try {
                    Constructor<?> constructor = clazz.getDeclaredConstructor(Properties.class);
                    upgradeTask = (IUpgradeTask) constructor.newInstance(properties);
                } catch (NoSuchMethodException ignore) {
                    log.info("cannot find constructor with properties, ignore properties");
                } catch (Exception e) {
                    log.warn("Fail to find constructor with properties", e);
                }
                if (upgradeTask == null) {
                    upgradeTask = (IUpgradeTask) clazz.newInstance();
                }
                log.info("UpgradeTask [{}][priority={}] for version {} begin to run", upgradeTask.getName(),
                    upgradeTask.getPriority(),
                    upgradeTask.getTargetVersion());
                int resultCode = upgradeTask.execute(args);
                if (resultCode == 0) {
                    successfulTaskCount++;
                    log.info("UpgradeTask [{}][priority={}] for version {} successfully end", upgradeTask.getName(),
                        upgradeTask.getPriority(),
                        upgradeTask.getTargetVersion());
                } else {
                    log.warn("UpgradeTask [{}][priority={}] for version {} failed", upgradeTask.getName(),
                        upgradeTask.getPriority(),
                        upgradeTask.getTargetVersion());
                    break;
                }
            } catch (InstantiationException e) {
                log.error("Fail to Instantiate {}", clazz.getSimpleName(), e);
                break;
            } catch (IllegalAccessException e) {
                log.error("Fail to Instantiate {} because of illegalAccess", clazz.getSimpleName(), e);
                break;
            } catch (Exception e) {
                log.error("Fail to run {}", clazz.getSimpleName(), e);
                break;
            }
        }
        if (taskSize > 0) {
            if (successfulTaskCount == taskSize) {
                log.info("All {} upgradeTasks finished successfully", taskSize);
            } else {
                log.warn("{} of {} upgradeTasks finished successfully, others failed, please check",
                    successfulTaskCount,
                    taskSize);
            }
        } else {
            log.info("No matched upgradeTasks need to run");
        }
    }
}
