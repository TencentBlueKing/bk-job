# JOB 有哪些类型的变量？如何使用这些变量



```bash
function GetHostByAttr() {
    local str="nekzhang"
    local target="1.1.1.1"
    
    echo $str
}
```



```python
def send_command(host, port, cmd, expect=None):
    s = socket.socket()
    s.connect((host, port))
    s.send(cmd + "\r\n")
    reply = s.recv(2000)
    s.close()
    if expect == reply or expect is None:
        return 0, reply
    else:
        return 1, reply
```



```perl
sub job_localtime {
    my @n = localtime();
    return sprintf("%04d-%02d-%02d %02d:%02d:%02d",$n[5]+1900,$n[4]+1,$n[3], $n[2], $n[1], $n[0] );
}
```



```powershell
##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。 
function job_success
{
    $cu_date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    if($args.count -ne 0)
    {
        $args | foreach {$arg_str=$arg_str + " " + $_}
        "[{0}][PID:{1}] job_success:[{2}]" -f $cu_date,$pid,$arg_str.TrimStart(' ')
    }
    else
    {
        "[{0}][PID:{1}] job_success:[]" -f $cu_date,$pid
    }
    exit 0
}
```



```bat
REM 函数定定义区域，不要把正文写到函数区下面 
goto:eof
REM 可在脚本开始运行时调用，打印当时的时间戳及PID。
:job_start
    set cu_time=[%date:~0,10% %time:~0,8%]
    for /F "skip=3 tokens=2" %%i in ('tasklist /v /FI "IMAGENAME eq cmd.exe" /FI "STATUS eq Unknown"') do (
        set pid=[PID:%%i]
        goto:break
    )
    :break
    echo %cu_time%%pid% job_start
    goto:eof
```



