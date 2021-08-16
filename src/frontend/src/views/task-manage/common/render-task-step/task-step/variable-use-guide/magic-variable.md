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