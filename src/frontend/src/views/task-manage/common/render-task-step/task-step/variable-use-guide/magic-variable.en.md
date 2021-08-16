# Instruction
Magic Variable is a special variable that built-in specific to the JOB platform, and it's only supported in the JOB platform

# How to

  Must declare before use: `# job_import {{VarName}}`

  After that, using `$` and `{}`, e.g.: `${VarName}`

# Where to

  Only supported in `shell` script

# Variable List

  - To get value of `Host` type global variable

    ```bash
    # job_import {{host_type_var_name}}
    
    echo ${host_type_var_name}
    ```

    Result (Example):

    ```
    0:10.1.1.100,1:20.2.2.200
    ```

    The result output format is "Cloud Area + : + inner IP addr", multiple IPs are separated by commas(,)

  - To get value of last step host list

    ```bash
    # job_import {{JOB_LAST_ALL}}
    # Get all host list of last step
    
    # job_import {{JOB_LAST_SUCCESS}}
    # Get success host list of last step
    
    # job_import {{JOB_LAST_FAIL}}
    # Get failure host list of last step
    ```

    The result output format is "Cloud Area + : + inner IP addr", multiple IPs are separated by commas(,)

  - To get value of other host's namespace variable
  
    ```bash
    # job_import {{JOB_NAMESPACE_ALL}}
    # Get value of all namespace variable
    echo ${JOB_NAMESPACE_ALL}
    
    # job_import {{JOB_NAMESPACE_varname}}
    # Get value of specific namespace variable
    echo ${JOB_NAMESPACE_varname}
    ```

    Result (Example):
  
    ```bash
    ### echo ${JOB_NAMESPACE_ALL} (If there have two namespace variables: ns_var1 and ns_var2):
    {"ns_var1":{"0:10.10.10.1":"xxxx","0:10.10.10.2":"yyyy","0:10.10.10.3":"zzzz"},"ns_var2":{"0:20.20.20.1":"aaaa","0:20.20.20.2":"bbbb","0:20.20.20.3":"cccc","0:20.20.20.4":"dddd"}}
      
    ### echo ${JOB_NAMESPACE_ns_var1}:
    {"0:10.10.10.1":"xxxx","0:10.10.10.2":"yyyy","0:10.10.10.3":"zzzz"}
    ```