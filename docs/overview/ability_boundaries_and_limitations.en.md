## Ability Boundaries and Limitations of BK-JOB

#### 1. File distribution with regular expression special characters in the path is not supported (currently unsupported)  
For details, please refer to: https://github.com/TencentBlueKing/bk-job/issues/3449

#### 2. Differences in system parameters and environment variables compared to interactive shell login execution, cannot guarantee identical execution results
Since the GSE Agent underlying BK-JOB is a **resident process** on the machine, the system parameters (ulimit parameters, etc.) and environment variables it uses are the data read **at the moment the Agent process starts**. Due to **operating system limitations**, the Agent cannot detect and hot-reload subsequently modified system parameters and environment variables before restarting, which may cause differences between task execution on BK-JOB and interactive shell login execution for tasks that depend on system parameters or environment variables.  
Interactive shell login execution: Uses the system parameters and environment variables that the user can currently load;  
GSE Agent execution: Uses the environment variables that the user could load at the time the Agent started;  
**Usage recommendations:**    
If your script depends on system parameters or environment variables, please specify them explicitly in the script, **do not modify them by logging into the machine first and then execute via BK-JOB**.  
If you must modify them by logging into the machine first before executing via BK-JOB, please **restart the GSE Agent through Node Management or command to make the changes take effect** after modification. The command to restart the Agent is maintained by GSE. Please refer to the GSE Agent operation and maintenance documentation. Here is a reference example:  
```shell
// Linux machine
cd /usr/local/gse2/agent/bin && ./gsectl restart

// Windows machine
cd /cygdrive/c/gse2/agent/bin && ./gsectl.bat restart
or
cd C:/gse2/agent/bin && ./gsectl.bat restart
# The GSE installation path may vary in different environments. If the above paths do not match, you can first find the GSE Agent installation location by using `ps -ef|grep gse`, and then use the control program in the corresponding path to operate.
```
