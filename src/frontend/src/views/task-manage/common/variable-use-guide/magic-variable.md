# 说明

JOB平台执行引擎提供的特有的变量能力

# 用法

  脚本中使用，并且需要事先声明：`job_import {{变量名}}`

  声明后，同样是使用 dollar 符 + 大括号：`${变量名}`

# 在哪里用

  当前仅支持在 `shell` 脚本语言中使用

# 变量列表

  - 获取 `主机列表` 类型的全局变量值

    ```bash
    # job_import {{主机列表的全局变量名}}

    echo ${主机列表的全局变量名}
    ```

    输出结果（示例）：

    ```
    0:10.1.1.100,1:20.2.2.200
    ```

    输出的格式为： `云区域ID + 冒号 + 内网IP`，多个IP地址以逗号分隔

  - 获取上一个步骤执行的主机列表

    ```bash
    # job_import {{JOB_LAST_ALL}}
    # 获取上一个步骤的所有执行主机IP列表

    # job_import {{JOB_LAST_SUCCESS}}
    # 获取上一个步骤执行成功的主机IP列表

    # job_import {{JOB_LAST_FAIL}}
    # 获取上一个步骤执行失败的主机IP列表
    ```

    输出的格式同上： `云区域ID + 冒号 + 内网IP`，多个IP地址以逗号分隔

  - 获取其他主机的命名空间变量值

    ```bash
    # job_import {{JOB_NAMESPACE_ALL}}
    # 获取所有命名空间变量的汇聚值
    echo ${JOB_NAMESPACE_ALL}

    # job_import {{JOB_NAMESPACE_命名空间变量名}}
    # 获取某个命名空间变量的汇聚值
    echo ${JOB_NAMESPACE_命名空间变量名}
    ```

    输出结果（示例）：

    ```bash
    ### echo ${JOB_NAMESPACE_ALL} 的输出(假定有 ns_var1 和 ns_var2 两个命名空间类型全局变量)：
    {"ns_var1":{"0:10.10.10.1":"xxxx","0:10.10.10.2":"yyyy","0:10.10.10.3":"zzzz"},"ns_var2":{"0:20.20.20.1":"aaaa","0:20.20.20.2":"bbbb","0:20.20.20.3":"cccc","0:20.20.20.4":"dddd"}}

    ### echo ${JOB_NAMESPACE_命名空间变量名} 的输出：
    {"0:10.10.10.1":"xxxx","0:10.10.10.2":"yyyy","0:10.10.10.3":"zzzz"}
    ```

# 处理建议
  - Shell 脚本处理 JSON字符串的两种方式（仅供参考，正式投入使用建议调试确认后再上线）：

	  方式一：（纯 sed + awk 实现）
    ```bash
      #!/bin/bash

      str='{"ns1":{"ip1":"value1","ip2":"value2"},"ns2":{"ip3":"value3","ip4":"value4"}}'

      function json_parse {
          local _json=$1
          local _key=$2
          temp=`echo $_json | sed 's/\\\\\//\//g' | sed 's/[{}]//g' | awk -v k="text" '{n=split($0,a,","); for (i=1; i<=n; i++) print a[i]}' | sed 's/\"\:\"/\|/g' | sed 's/[\,]/ /g' | sed 's/\"//g' | grep -w $_key`
          echo ${temp##*|}
      }

      json_parse $str ip1			## 获取 str 的 json字符串中 ip1 的值，即：value1
      json_parse $str ns1			## 获取 str 的 json字符串中 ns1 的值，即：{"ip1":"value1","ip2":"value2"}
    ```

    方式二：（借助 Python 的 json 模块实现）
    ```bash
      #!/bin/bash

      str='{"ns1":{"ip1":"value1","ip2":"value2"},"ns2":{"ip3":"value3","ip4":"value4"}}'

      ### 获取 str 的 json字符串中 ip2 的值，即：value2
      echo ${str} | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["ns1"]["ip2"]'
    ```