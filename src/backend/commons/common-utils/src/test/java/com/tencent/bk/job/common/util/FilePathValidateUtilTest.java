package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class FilePathValidateUtilTest {
    @Test
    void testWindowsFilePath(){
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\Documents\\abc.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("c:\\Documents\\abc.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\Documents\\嘉 abc.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath(":\\abc.txt")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("test.log")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("logs\\test.log")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath(".\\test.log")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("..\\test.log")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("1:\\test.log")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("AA:\\test.log")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:test.log")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\\\")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\logs\\..\\access.log")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\.config\\conf")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\user\\abc>a")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\user\\abc:a")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\user\\abc|a")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\user\\abc?a")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\user\\abc<a")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\logs/logs")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\logs\\log*.log")).isTrue();
        // 兼容斜杠作为目录分隔符的Windows路径
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:/cygwinroot/tmp")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("D:/P4_chiji/WinClient_CGame/")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("E:/")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("D:\\xxx1/xxx2.MD5")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("D:/Download/")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:/logs/access.log")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:/")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\tmp\\REGEX:myfile-[A-Za-z]{0,10}.tar.gz")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\tmp\\REGE:a|b")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\tmp\\REGEX:a|b")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\tmp\\REGEX:^[a-zA-Z0-9]")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("[FILESRCIP]\\logs")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\[FILESRCIP]\\logs")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\[DATE:yyyy-MM-dd]\\logs")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("${path}")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("${!#path}")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("${_path}")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("${path}\\test.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\tmp\\${path}")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("C:\\${date}\\${path}")).isTrue();
    }

    @Test
    void testLinuxFilePath(){
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/data/test_2022 04 12.apk")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp/")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp/.conf/abc")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp/test/../test.log")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("data/test_2022-04-12.apk")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("./test.log")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("../test.log")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("///")).isTrue(); // 根目录
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp////")).isTrue(); // /tmp/
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp//test/")).isTrue();// /tmp/test/
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/logs/log*.log")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp/REGEX:myfile-[A-Za-z]{0,10}.tar.gz")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp/REGEX:aa|bb")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("[FILESRCIP]/logs")).isFalse();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/[FILESRCIP]/logs")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/[DATE:yyyy-MM-dd]/logs")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("${path}/test.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("${path}/../test.txt")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/[FILESRCIP]/${path}")).isTrue();
        assertThat(FilePathValidateUtil.validateFileSystemAbsolutePath("/tmp/${path}")).isTrue();
    }
}
