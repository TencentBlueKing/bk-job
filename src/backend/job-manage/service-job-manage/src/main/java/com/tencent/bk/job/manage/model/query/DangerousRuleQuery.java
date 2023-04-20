package com.tencent.bk.job.manage.model.query;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 高危语句规则查询
 */
@Getter
@Setter
@ToString
@Builder
public class DangerousRuleQuery implements Cloneable {
    /**
     * 表达式
     */
    private String expression;
    /**
     * 脚本类型：SHELL(1), BAT(2), PERL(3), PYTHON(4),POWERSHELL(5), SQL(6)
     */
    private List<Byte> scriptTypeList;
    /**
     * 描述
     */
    private String description;
    /**
     * 处理动作
     */
    private List<Byte> action;
}
