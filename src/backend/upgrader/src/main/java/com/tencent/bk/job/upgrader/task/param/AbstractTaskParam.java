package com.tencent.bk.job.upgrader.task.param;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractTaskParam {
    private String key;
    private String description;
    private String descriptionEn;

    @Data
    @AllArgsConstructor
    public static class ParamCheckResult {
        private boolean pass;
        private String message;
        private String messageEn;

        public static ParamCheckResult ok() {
            return new ParamCheckResult(true, "成功", "ok");
        }
    }

    public abstract ParamCheckResult checkParam(String value);
}
