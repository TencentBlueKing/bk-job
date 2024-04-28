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

package com.tencent.bk.job.manage.manager.cache;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.common.constants.rule.HighRiskGrammarRuleStatusEnum;
import com.tencent.bk.job.manage.dao.globalsetting.DangerousRuleDAO;
import com.tencent.bk.job.manage.model.db.DangerousRuleDO;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 高危语句规则分布式缓存，基于Redis实现
 */
@Component
@Slf4j
public class DangerousRuleCache {
    public static final String DANGEROUS_RULE_HASH_KEY = "job:manage:dangerousRules";
    private final DangerousRuleDAO dangerousRuleDAO;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public DangerousRuleCache(DangerousRuleDAO dangerousRuleDAO,
                              @Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> redisTemplate) {
        this.dangerousRuleDAO = dangerousRuleDAO;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 根据脚本类型获取高危脚本规则；如果缓存中不存在，那么会尝试从MySQL DB 中加载
     *
     * @param scriptType 脚本类型
     * @return 高危语句规则
     */
    public List<DangerousRuleDTO> listDangerousRuleFromCache(Integer scriptType) {

        Object dangerousRules = redisTemplate.opsForHash().get(DANGEROUS_RULE_HASH_KEY, String.valueOf(scriptType));
        if (dangerousRules == null) {
            log.info("DangerousRules is not in cache!");
            DangerousRuleDTO dangerousRuleQuery = new DangerousRuleDTO();
            dangerousRuleQuery.setScriptType(scriptType);
            dangerousRuleQuery.setStatus(HighRiskGrammarRuleStatusEnum.ENABLED.getCode());
            List<DangerousRuleDTO> dangerousRuleDTOList = dangerousRuleDAO.listDangerousRules(dangerousRuleQuery);
            if (CollectionUtils.isEmpty(dangerousRuleDTOList)) {
                redisTemplate.opsForHash().put(DANGEROUS_RULE_HASH_KEY, String.valueOf(scriptType),
                    new ArrayList<DangerousRuleDO>());
                return new ArrayList<>();
            }
            try {
                log.info("Refresh dangerousRules cache, dangerousRules:{}", JsonUtils.toJson(dangerousRuleDTOList));
                List<DangerousRuleDO> dangerousRuleDOList = new ArrayList<>();
                dangerousRuleDTOList.forEach(dangerousRuleDTO -> {
                    dangerousRuleDOList.add(dangerousRuleDTO.toDangerousRuleDO());
                });
                redisTemplate.opsForHash().put(DANGEROUS_RULE_HASH_KEY, String.valueOf(scriptType),
                    dangerousRuleDOList);
            } catch (Exception e) {
                log.error("Refresh dangerousRules cache fail", e);
            }
            return dangerousRuleDTOList;
        }
        List<DangerousRuleDO> dangerousRuleDOList = (List<DangerousRuleDO>) dangerousRules;
        List<DangerousRuleDTO> dangerousRuleDTOList = new ArrayList<>();
        dangerousRuleDOList.forEach(dangerousRuleDO -> dangerousRuleDTOList.add(dangerousRuleDO.toDangerousRuleDTO()));
        return dangerousRuleDTOList;
    }

    /**
     * 根据脚本类型删除缓存的高危规则
     *
     * @param scriptTypes 脚本类型列表
     */
    public void deleteDangerousRuleCacheByScriptTypes(List<Byte> scriptTypes) {
        if (CollectionUtils.isNotEmpty(scriptTypes)) {
            scriptTypes.forEach(this::deleteDangerousRuleCacheByScriptType);
        }
    }

    /**
     * 根据脚本类型删除缓存的高危规则
     *
     * @param scriptType 脚本类型
     */
    public void deleteDangerousRuleCacheByScriptType(Byte scriptType) {
        redisTemplate.opsForHash().delete(DANGEROUS_RULE_HASH_KEY, String.valueOf(scriptType));
    }
}
