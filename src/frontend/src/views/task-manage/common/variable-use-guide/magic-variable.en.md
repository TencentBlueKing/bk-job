# Instruction
Magic Variable is a special variable that built-in specific to the JOB platform, and it's only supported in the JOB platform

# How to

  Must declare before use it:
  `# job_import {{VarName}}`

  After that, using `$` and `{}`,
  e.g.: `${VarName}`

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

    The result output format is `Cloud Area + : + inner IP addr`, multiple IPs are separated by commas(,)

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

  - Two ways to processing JSON string with Shell (For reference only, it's recommended to be on-line after debug)：

    1. Using sed & awk :
    ```bash
      #!/bin/bash
      
      str='{"ns1":{"ip1":"value1","ip2":"value2"},"ns2":{"ip3":"value3","ip4":"value4"}}'
      
      function json_parse {
          local _json=$1
          local _key=$2
          temp=`echo $_json | sed 's/\\\\\//\//g' | sed 's/[{}]//g' | awk -v k="text" '{n=split($0,a,","); for (i=1; i<=n; i++) print a[i]}' | sed 's/\"\:\"/\|/g' | sed 's/[\,]/ /g' | sed 's/\"//g' | grep -w $_key`
          echo ${temp##*|}
      }
      
      json_parse $str ip1			## Value of ip1 in $str variable, it will be "value1"
      json_parse $str ns1			## Value of ns1, it will be {"ip1":"value1","ip2":"value2"}
    ```

    2. Using Python's json module :
    ```bash
      #!/bin/bash
      
      str='{"ns1":{"ip1":"value1","ip2":"value2"},"ns2":{"ip3":"value3","ip4":"value4"}}'
      
      ### To get value of ip2 in $str variable, it will be "value2"
      echo ${str} | python -c 'import json,sys;obj=json.load(sys.stdin);print obj["ns1"]["ip2"]'
    ```