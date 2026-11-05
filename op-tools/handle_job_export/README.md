# 处理 3.11.x 作业导出包的时间字段

3.11.x 导出作业时，作业模板、执行方案以及标签上的 `createTime`、`lastModifyTime` 会被写成 `yyyy-MM-dd HH:mm:ss`。3.12.x 这两个字段改成了 Long，导入时期望毫秒时间戳，因此直接导入 3.11 的包会解析失败。

导出文件下载下来的名字一般是 `xxx.jobexport`。它的内容是 zip：里面有一份作业 JSON，可能还有本地上传文件。本工具解开这个 zip，只改上述时间字段，再压回 zip。

## 用法

```shell
cd op-tools/handle_job_export
python3 handle_job_export.py /path/to/backup.jobexport
```

默认在原文件同目录写出 `backup-handled.jobexport`。扩展名保持不变，所以 `.jobexport` 的产物可以直接在 3.12 的导入页上传。

```shell
# 指定输出路径
python3 handle_job_export.py /path/to/backup.jobexport -o /tmp/backup-handled.jobexport

# 输出文件已存在时覆盖
python3 handle_job_export.py /path/to/backup.jobexport --force
```

导入页只接受以 `.jobexport` 结尾的文件。如果手头文件扩展名是 `.zip`，处理后请把产物改名为 `.jobexport`，或用 `-o` 直接指定这个后缀。

## 转换规则

- 只改 JSON 里名为 `createTime`、`lastModifyTime` 的字段。
- 值是 `yyyy-MM-dd HH:mm:ss` 时，按东八区解释，写成毫秒时间戳。例如 `2020-01-01 00:00:00` 变成 `1577808000000`。这与 3.12 导出时的写法一致，也与作业平台默认时区（UTC+8）一致。
- 值已经是数字（例如脚本对象上的秒级时间、导出包自身的 `createTime`）保持原样。
- 脚本内容、步骤说明、变量值里出现的时间字符串不会被改。
- 压缩包里的本地文件原样放回。

导出机器如果不是东八区，用 `--tz-offset` 指定小时偏移，例如 `--tz-offset 0`。

## 加密包

勾选了密码的导出包文件头是作业平台魔数，内容是密文，本工具会直接拒绝。请用不带密码重新导出后再处理。

## 环境

Python 3.6 及以上，只用标准库，不需要安装依赖。
