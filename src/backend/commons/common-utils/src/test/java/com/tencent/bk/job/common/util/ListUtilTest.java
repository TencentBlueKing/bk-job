package com.tencent.bk.job.common.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class ListUtilTest {

    @Test
    void testIsAllTrue() {
        List<Boolean> allTrueList = new ArrayList<>();
        assertThat(ListUtil.isAllTrue(allTrueList)).isTrue();
        allTrueList.add(true);
        allTrueList.add(true);
        allTrueList.add(true);
        assertThat(ListUtil.isAllTrue(allTrueList)).isTrue();
        List<Boolean> notAllTrueList = new ArrayList<>();
        notAllTrueList.add(true);
        notAllTrueList.add(false);
        notAllTrueList.add(false);
        assertThat(ListUtil.isAllTrue(notAllTrueList)).isFalse();
    }

    @Test
    void testSeparate() {
        List<String> list = Arrays.asList("a", "abc", "acc", "abb", "cc", "dd", "ee", null, "ff");
        Pair<List<String>, List<String>> pair = ListUtil.separate(list, s -> s != null && s.contains("a"));
        assertThat(pair.getLeft().size() + pair.getRight().size() == list.size());
        pair.getLeft().forEach(s -> assertThat(s.contains("a")));
        pair.getRight().forEach(s -> assertThat(s == null || !s.contains("a")));
    }

    @Test
    void testUnion() {
        List<String> list1 = null;
        List<String> list2 = null;
        List<String> result = ListUtil.union(list1, list2);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();


        list1 = Lists.newArrayList("a", "b");
        result = ListUtil.union(list1, list2);
        assertThat(result).containsOnly("a", "b");

        list2 = Lists.newArrayList("c", "d");
        result = ListUtil.union(list1, list2);
        assertThat(result).containsOnly("a", "b", "c", "d");
    }

    @Test
    void testTrimStringList() {
        assertThat(ListUtil.trimStringList(null)).isNull();
        assertThat(ListUtil.trimStringList(Collections.emptyList())).isNotNull();
        assertThat(ListUtil.trimStringList(Collections.emptyList())).size().isEqualTo(0);

        List<String> list1 = Lists.newArrayList(null, " a  ", " b b ", "");
        assertThat(ListUtil.trimStringList(list1)).size().isEqualTo(4);
        assertThat(ListUtil.trimStringList(list1).get(0)).isNull();
        assertThat(ListUtil.trimStringList(list1).get(1)).isEqualTo("a");
        assertThat(ListUtil.trimStringList(list1).get(2)).isEqualTo("b b");
        assertThat(ListUtil.trimStringList(list1).get(3)).isEqualTo("");
    }
}
