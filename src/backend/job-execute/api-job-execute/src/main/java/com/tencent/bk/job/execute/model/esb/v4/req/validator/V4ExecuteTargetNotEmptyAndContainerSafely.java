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

package com.tencent.bk.job.execute.model.esb.v4.req.validator;

import com.tencent.bk.job.execute.model.esb.v4.req.V4ContainerFilter;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteTargetDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.Constraint;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 1. 校验执行目标非空
 * 2. 若执行对象为容器，校验集群名称不为空,且不允许全业务或者全集群执行
 */
@Target({ElementType.TYPE})
@Documented
@Retention(RUNTIME)
@Constraint(validatedBy = V4ExecuteTargetNotEmptyAndContainerSafely.Validator.class)
public @interface V4ExecuteTargetNotEmptyAndContainerSafely {
    String message() default "{validation.constraints.ExecuteTarget_empty.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<V4ExecuteTargetNotEmptyAndContainerSafely, V4ExecuteTargetDTO> {

        @Override
        public boolean isValid(V4ExecuteTargetDTO v4ExecuteTargetDTO,
                               ConstraintValidatorContext constraintValidatorContext) {

            HibernateConstraintValidatorContext hibernateContext = constraintValidatorContext.unwrap(
                HibernateConstraintValidatorContext.class
            );
            if (!(v4ExecuteTargetDTO != null && !v4ExecuteTargetDTO.isTargetEmpty())) {
                // 执行目标为空
                hibernateContext.disableDefaultConstraintViolation();
                hibernateContext
                    .buildConstraintViolationWithTemplate("{validation.constraints.ExecuteTarget_empty.message}")
                    .addConstraintViolation();
                return false;
            }

            // 执行目标存在容器
            List<V4ContainerFilter> containerFilters = v4ExecuteTargetDTO.getKubeContainerFilters();
            if (CollectionUtils.isNotEmpty(containerFilters)) {
                for (V4ContainerFilter containerFilter : containerFilters) {
                    // 缺少集群ID
                    if (containerFilter.getClusterFilter() == null
                        || CollectionUtils.isEmpty(containerFilter.getClusterFilter().getClusterUIDs())) {
                        hibernateContext.disableDefaultConstraintViolation();
                        hibernateContext
                            .buildConstraintViolationWithTemplate(
                                "{validation.constraints.ExecuteTarget_ClusterFilterEmpty.message}")
                            .addConstraintViolation();
                        return false;
                    }

                    // 未设置整个集群执行，但是只给了集群过滤器
                    if (containerFilter.isAllCluster()
                        && (containerFilter.getExecuteInWholeCluster() == null
                            || !containerFilter.getExecuteInWholeCluster())) {
                        hibernateContext.disableDefaultConstraintViolation();
                        hibernateContext
                            .buildConstraintViolationWithTemplate(
                                "{validation.constraints.ExecuteTarget_AllCluster.message}")
                            .addConstraintViolation();
                        return false;
                    }
                }
            }

            return true;
        }
    }
}
