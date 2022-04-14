package com.tencent.bk.job.manage.manager.cache;

import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.globalsetting.DangerousRuleDAO;
import com.tencent.bk.job.manage.model.db.DangerousRuleDO;
import com.tencent.bk.job.manage.model.dto.globalsetting.DangerousRuleDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.DSLContext;
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
    private final DSLContext dslContext;
    private final DangerousRuleDAO dangerousRuleDAO;
    private final RedisTemplate redisTemplate;

    @Autowired
    public DangerousRuleCache(DSLContext dslContext,
                              DangerousRuleDAO dangerousRuleDAO,
                              @Qualifier("jsonRedisTemplate") RedisTemplate redisTemplate) {
        this.dslContext = dslContext;
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
            List<DangerousRuleDTO> dangerousRuleDTOList = dangerousRuleDAO.listDangerousRulesByScriptType(dslContext,
                scriptType);
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
