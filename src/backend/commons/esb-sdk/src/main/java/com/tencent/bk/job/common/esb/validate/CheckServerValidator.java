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

package com.tencent.bk.job.common.esb.validate;

import com.tencent.bk.job.common.esb.model.job.EsbIpDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbServerV3DTO;
import com.tencent.bk.job.common.util.ip.IpUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CheckServerValidator implements ConstraintValidator<CheckServer, EsbServerV3DTO> {
    @Override
    public boolean isValid(EsbServerV3DTO value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }

        HibernateConstraintValidatorContext hibernateContext = context.unwrap(
            HibernateConstraintValidatorContext.class
        );

        if(!(CollectionUtils.isEmpty(value.getIps()) && CollectionUtils.isEmpty(value.getDynamicGroups())
            && CollectionUtils.isEmpty(value.getTopoNodes()))) {
            hibernateContext.disableDefaultConstraintViolation();
            hibernateContext
                .buildConstraintViolationWithTemplate( "{validation.constraints.Pattern.ip.message}" )
                .addConstraintViolation();

        }
        for (EsbIpDTO ip : value.getIps()) {
            if (!IpUtils.checkIp(ip.getIp())) {
                hibernateContext.disableDefaultConstraintViolation();
                hibernateContext.addMessageParameter("ip", ip.getIp());
                hibernateContext
                    .buildConstraintViolationWithTemplate("{validation.constraints.Pattern.ip.message}")
                    .addPropertyNode("ips")
                    .addPropertyNode("ip").inIterable().atIndex(1).addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
