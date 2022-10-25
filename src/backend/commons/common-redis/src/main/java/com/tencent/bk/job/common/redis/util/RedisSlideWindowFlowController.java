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

package com.tencent.bk.job.common.redis.util;

import ch.qos.logback.classic.Level;
import com.tencent.bk.job.common.util.FlowController;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 基于Redis的分布式实时滑动窗口限流器
 */
@Slf4j
public class RedisSlideWindowFlowController implements FlowController {

    private static final String checkScript = "return redis.call('llen',KEYS[1])";

    private static final String checkAndPushScript =
        "local maxRate=redis.call('get',KEYS[2])\n" +
            "if(not maxRate)\n" +
            "then\n" +
            "return -1\n" +
            "end\n" +
            "if (redis.call('llen', KEYS[1]) < (0+maxRate)) \n" +
            "then \n" +
            "redis.call('rpush',KEYS[1],ARGV[1])\n" +
            "return 1\n" +
            "else\n" +
            "return 0\n" +
            "end";

    private static final String popScript =
        "local firstValue=redis.call('lindex', KEYS[1],0)\n" +
            "if(not firstValue)\n" +
            "then\n" +
            "return\n" +
            "end\n" +
            "while (redis.call('llen',KEYS[1])>0 and (ARGV[1]-firstValue)>1000) \n" +
            "do \n" +
            "redis.call('lpop',KEYS[1])\n" +
            "firstValue=redis.call('lindex', KEYS[1],0)\n" +
            "if(not firstValue)\n" +
            "then\n" +
            "return\n" +
            "end\n" +
            "end";

    private static StringRedisTemplate redisTemplate = null;
    private static final String REDIS_PREFIX_SLIDE_WINDOW = "job:slideWindow:";
    private static final String REDIS_PREFIX_MAX_RATE = "job:maxRate:";
    private static final Set<String> slideWindowKeys = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> maxRateKeys = Collections.synchronizedSet(new HashSet<>());
    private Integer defaultMaxRate = 1000;
    private Integer precision = 10;
    private int suppressCount = 0;
    private boolean isReady = false;
    public Timer timer = new Timer();
    public TimerTask clearTask = new TimerTask() {
        @Override
        public void run() {
            tryToClearSlideWindows();
        }
    };

    private void logWithLevel(Level logLevel, String message) {
        if (logLevel == Level.ERROR) {
            log.error(message);
        } else if (logLevel == Level.WARN) {
            log.warn(message);
        } else if (logLevel == Level.INFO) {
            log.info(message);
        } else {
            log.debug(message);
        }
    }

    private void logDurationWithSuppress(Level logLevel, long duration) {
        if (suppressCount == 0) {
            logWithLevel(logLevel, "slideWindow update time consuming:" + duration + "ms");
            suppressCount = 100;
        } else {
            suppressCount -= 1;
        }
    }

    private void tryToClearSlideWindows() {
        try {
            long timeStart = System.currentTimeMillis();
            // 清理Redis中的各个window内容
            Object[] keys = slideWindowKeys.toArray();
            for (Object windowKey : keys) {
                clear((String) windowKey, System.currentTimeMillis());
            }
            long duration = System.currentTimeMillis() - timeStart;

            if (duration >= 1000) {
                log.warn("DANGER:slideWindow update time consuming:" + duration + "ms");
            } else if (duration >= 100) {
                logDurationWithSuppress(Level.WARN, duration);
            } else if (duration >= 10) {
                logDurationWithSuppress(Level.INFO, duration);
            }
        } catch (Exception e) {
            log.error("Exception when clear slideWindows", e);
        }
    }

    public void init(StringRedisTemplate pRedisTemplate,
                     Map<String, Integer> configMap,
                     Integer pDefaultMaxRate,
                     Integer pPrecision) {
        redisTemplate = pRedisTemplate;
        if (pDefaultMaxRate != null && pDefaultMaxRate > 0) {
            defaultMaxRate = pDefaultMaxRate;
        }
        if (pPrecision != null && pPrecision > 0) {
            precision = pPrecision;
        }
        configMap.forEach((key, value) -> {
            // 写入本地缓存
            slideWindowKeys.add(REDIS_PREFIX_SLIDE_WINDOW + key);
            maxRateKeys.add(REDIS_PREFIX_MAX_RATE + key);
            // 写入Redis
            redisTemplate.opsForValue().set(REDIS_PREFIX_MAX_RATE + key, "" + value);
        });
        timer.schedule(clearTask, (long) (100 * Math.random()), 1000 / precision);
        isReady = true;
    }

    private void clear(String key, Long timeMills) {
        RedisScript<Void> script = RedisScript.of(popScript, Void.class);
        List<String> keyList = new ArrayList<>();
        keyList.add(key);
        redisTemplate.execute(script, keyList, "" + timeMills);
    }

    private Long checkAndPush(String resourceId, Long timeMills) {
        RedisScript<Long> script = RedisScript.of(checkAndPushScript, Long.class);
        List<String> keyList = new ArrayList<>();
        keyList.add(REDIS_PREFIX_SLIDE_WINDOW + resourceId);
        keyList.add(REDIS_PREFIX_MAX_RATE + resourceId);
        return redisTemplate.execute(script, keyList, "" + timeMills);
    }

    @Override
    public boolean isReady() {
        return isReady;
    }

    @Override
    public int updateConfig(Map<String, Long> configMap) {
        if (configMap == null || configMap.isEmpty()) return 0;
        int count = 0;
        for (Map.Entry<String, Long> entry : configMap.entrySet()) {
            String key = entry.getKey();
            Long value = entry.getValue();
            String redisKey = REDIS_PREFIX_MAX_RATE + key;
            String redisValue = "" + value;
            try {
                // 写入Redis
                redisTemplate.opsForValue().set(redisKey, redisValue);
                count += 1;
            } catch (Exception e) {
                log.error("Fail to write redis:key={},value={}", redisKey, redisValue, e);
            }
        }
        return count;
    }

    @Override
    public Long getCurrentConfig(String resourceId) {
        String maxRateKey = REDIS_PREFIX_MAX_RATE + resourceId;
        String stringValue = redisTemplate.opsForValue().get(maxRateKey);
        if (stringValue == null) return null;
        try {
            return Long.parseLong(stringValue);
        } catch (Exception e) {
            log.warn("Fail to parse maxRate, resourceId={},stringValue={}", resourceId, stringValue, e);
            return null;
        }
    }

    @Override
    public Map<String, Long> getCurrentConfig() {
        Map<String, Long> map = new HashMap<>();
        if (maxRateKeys.isEmpty()) {
            return map;
        }
        for (String maxRateKey : maxRateKeys) {
            try {
                String value = redisTemplate.opsForValue().get(maxRateKey);
                if (StringUtils.isNotBlank(value)) {
                    map.put(maxRateKey.replace(REDIS_PREFIX_MAX_RATE, ""), Long.parseLong(value));
                }
            } catch (Exception e) {
                log.error("Fail to get redis value:key={}", maxRateKey, e);
            }
        }
        return map;
    }

    @Override
    public Long getCurrentRate(String resourceId) {
        RedisScript<Long> script = RedisScript.of(checkScript, Long.class);
        List<String> keyList = new ArrayList<>();
        keyList.add(REDIS_PREFIX_SLIDE_WINDOW + resourceId);
        return redisTemplate.execute(script, keyList);
    }

    @Override
    public Map<String, Long> getCurrentRateMap() {
        Map<String, Long> map = new HashMap<>();
        if (slideWindowKeys.isEmpty()) {
            return map;
        }
        for (String slideWindowKey : slideWindowKeys) {
            String resourceId = slideWindowKey.replace(REDIS_PREFIX_SLIDE_WINDOW, "");
            try {
                map.put(resourceId, getCurrentRate(resourceId));
            } catch (Exception e) {
                log.error("Fail to get current rate:resourceId={}", resourceId, e);
            }
        }
        return map;
    }

    @Override
    public void acquire(String resourceId) {
        boolean result = false;
        try {
            while (!result) {
                result = tryAcquire(resourceId);
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            log.warn("sleep interrupted", e);
        } catch (Exception e) {
            log.info("Exception when tryAcquire", e);
        }
    }

    private void registerDefaultMaxRateKeys(String resourceId) {
        maxRateKeys.add(REDIS_PREFIX_MAX_RATE + resourceId);
        slideWindowKeys.add(REDIS_PREFIX_SLIDE_WINDOW + resourceId);
        try {
            redisTemplate.opsForValue().setIfAbsent(REDIS_PREFIX_MAX_RATE + resourceId, "" + defaultMaxRate);
            log.info("add maxRate of resourceId {}:{}", resourceId, defaultMaxRate);
        } catch (Exception e) {
            log.error("Fail to write maxRateKey into redis", e);
        }
    }

    public boolean tryAcquire(String resourceId) {
        String maxRateKey = REDIS_PREFIX_MAX_RATE + resourceId;
        if (!maxRateKeys.contains(maxRateKey)) {
            // 设置默认值写入Redis
            registerDefaultMaxRateKeys(resourceId);
        }
        // 不限流
        Long result = checkAndPush(resourceId, System.currentTimeMillis());
        if (result == 1L) {
            return true;
        } else if (result == 0L) {
            return false;
        } else if (result == -1L) {
            registerDefaultMaxRateKeys(resourceId);
            return true;
        } else {
            log.warn("Redis lua script return an unexpected value:{}, do not limit rate for {}", result, resourceId);
            return true;
        }
    }
}
