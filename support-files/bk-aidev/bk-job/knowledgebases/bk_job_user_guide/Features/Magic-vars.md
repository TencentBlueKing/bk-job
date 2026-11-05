# 魔法变量

“魔法变量” 是作业平台执行引擎提供的独有的能力，通过对作业步骤间关联参数的解析和处理，以特殊的方式提供给用户根据不同消费场景选择使用；旨在帮助运维在处理复杂的多步骤上下文关系参数时能够更得心应手，提供更强大的能力满足更多想像空间。

### 用法

  脚本中使用，并且需要事先声明：`job_import {{变量名}}`
  声明后，同样是使用 dollar 符 + 大括号：`${变量名}`

### 在哪里用
  当前仅支持在 `shell` 脚本语言中使用

### 变量列表
  - 获取 `执行目标` 类型的全局变量值

    ```bash
    # job_import {{执行目标的全局变量名}}
    
    echo ${执行目标的全局变量名}
    ```

    主机输出格式为 `云区域ID:内网IP`，多个地址以逗号分隔：

    ```text
    0:10.1.1.100,1:20.2.2.200
    ```

    容器输出格式为 `clusterName/namespace/podName/containerName`，多个容器以逗号分隔。同一变量里同时包含主机和容器时，两段结果依次拼接。

  - 获取上一个步骤的执行对象列表

    ```bash
    # job_import {{JOB_LAST_ALL}}
    # 获取上一个步骤的所有执行对象
    
    # job_import {{JOB_LAST_SUCCESS}}
    # 获取上一个步骤执行成功的执行对象
    
    # job_import {{JOB_LAST_FAIL}}
    # 获取上一个步骤执行失败的执行对象
    ```

    输出格式与「执行目标」变量相同：主机为 `云区域ID:IP`，容器为 `cluster/namespace/pod/container`。

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
