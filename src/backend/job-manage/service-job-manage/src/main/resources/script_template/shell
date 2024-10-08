#!/bin/bash


# 定义获取当前时间和PID的函数
function job_get_now
{
    echo "[`date +'%Y-%m-%d %H:%M:%S'`][PID:$$]"
}

# 在脚本开始运行时调用，打印当前的时间戳及PID
function job_start
{
    echo "$(job_get_now) job_start"
}

# 在脚本执行成功的逻辑分支处调用，打印当前的时间戳及PID
function job_success
{
    local msg="$*"
    echo "$(job_get_now) job_success:[$msg]"
    exit 0
}

# 在脚本执行失败的逻辑分支处调用，打印当前的时间戳及PID
function job_fail
{
    local msg="$*"
    echo "$(job_get_now) job_fail:[$msg]"
    exit 1
}

# 在当前脚本执行时，第一行输出当前时间和进程ID，详见上面函数：job_get_now
job_start

###### 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值
###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败
###### 可在此处开始编写您的脚本逻辑代码


