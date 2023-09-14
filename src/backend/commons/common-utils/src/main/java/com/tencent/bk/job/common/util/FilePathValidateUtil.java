package com.tencent.bk.job.common.util;

import org.apache.commons.lang3.StringUtils;
import java.util.regex.Pattern;

/**
 * 文件路径合法性校验工具类
 */
public class FilePathValidateUtil {
    // 传统DOS正则表达式
    private static final String CONVENTIONAL_DOS_PATH_REGEX = "(^[A-Za-z]:\\\\([^\\\\])(([^\\\\/:*?\"<>|])*\\\\?)*)|" +
        "(^[A-Za-z]:[\\\\])";
    // Linux路径正则表达式
    private static final String LINUX_PATH_REGEX = "^/(((../)*|(./)*)|(\\.?[^.].*/{0,1}))+";

    // 传统DOS Pattern
    private static final Pattern CONVENTIONAL_DOS_PATH_PATTERN = Pattern.compile(CONVENTIONAL_DOS_PATH_REGEX);
    // Linux路径Pattern
    private static final Pattern LINUX_PATH_PATTERN = Pattern.compile(LINUX_PATH_REGEX);

    /**
     * 验证文件系统绝对路径的合法性
     * @param path 绝对路径
     * @return boolean true合法，false非法
     */
    public static boolean validateFileSystemAbsolutePath(String path) {
        if (StringUtils.isBlank(path)) {
            return false;
        }
        if (isLinuxAbsolutePath(path)) {
            return validateLinuxFileSystemAbsolutePath(path);
        } else {
            return validateWindowsFileSystemAbsolutePath(path);
        }
    }

    /**
     * 判断是否Linux绝对路径
     *
     * @param path 文件路径
     * @return boolean
     */
    private static boolean isLinuxAbsolutePath(String path) {
        if (path.startsWith("/")) {
            return true;
        }
        return false;
    }

    /**
     * 1 传统DOS路径
     *    标准的DOS路径可由以下三部分组成：
     *    1)卷号或驱动器号，后跟卷分隔符(:)。
     *    2)目录名称。目录分隔符用来分隔嵌套目录层次结构中的子目录。
     *    3)文件名。目录分隔符用来分隔文件路径和文件名。
     * @param path
     * @return boolean
     */
    private static boolean validateWindowsFileSystemAbsolutePath(String path) {
        // 传统DOS
        if (CONVENTIONAL_DOS_PATH_PATTERN.matcher(path).matches()) {
            return true;
        }
        return false;
    }

    /**
     * 文件或目录名，除了/以外，所有的字符都合法
     *
     * @param path
     * @return boolean
     */
    private static boolean validateLinuxFileSystemAbsolutePath(String path) {
        if (LINUX_PATH_PATTERN.matcher(path).matches()) {
            return true;
        }
        return false;
    }
}
