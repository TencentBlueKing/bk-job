package com.tencent.bk.job.manage.validation.provider;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

import com.tencent.bk.job.manage.model.web.request.globalsetting.AddOrUpdateDangerousRuleReq;
import com.tencent.bk.job.manage.validation.common.Create;
import com.tencent.bk.job.manage.validation.common.Update;

public class DangerousRuleGroupSequenceProvider implements DefaultGroupSequenceProvider<AddOrUpdateDangerousRuleReq> {

    @Override
    public List<Class<?>> getValidationGroups(AddOrUpdateDangerousRuleReq bean){
        List<Class<?>> defaultGroupSequence = new ArrayList<>();
        defaultGroupSequence.add(AddOrUpdateDangerousRuleReq.class); 

        if (bean != null) { // 这块判空请务必要做
            try {
                Long id = bean.getId();
                if (id == null || id == -1) {
                    defaultGroupSequence.add(Create.class);
                } else{
                    defaultGroupSequence.add(Update.class);
                }
            } catch (Exception e) {
            }
        }
        return defaultGroupSequence;
    }
}