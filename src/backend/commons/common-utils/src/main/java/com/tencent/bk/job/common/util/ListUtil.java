package com.tencent.bk.job.common.util;

import java.util.List;

public class ListUtil {

    /**
     * 判断list中的元素是否全部为true
     *
     * @param list 元素列表
     * @return 最终结果
     */
    public static boolean isAllTrue(List<Boolean> list) {
        for (Boolean ele : list) {
            if (ele == null || !ele) return false;
        }
        return true;
    }
}
