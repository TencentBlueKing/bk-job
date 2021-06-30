/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
*/

import Cookie from 'js-cookie';

const BLUEKINNG_LANGUAGE = 'blueking_language';
let lang = 'zh-CN';
const bluekingLanguage = Cookie.get(BLUEKINNG_LANGUAGE);
if (bluekingLanguage && bluekingLanguage.toLowerCase() === 'en') {
    lang = 'en-US';
}

/* eslint-disable max-len */
export default {
    Shell: (function () {
        const shell = [];
        shell.push(
            '#!/bin/bash',
            '',
            'anynowtime="date +\'%Y-%m-%d %H:%M:%S\'"',
            'NOW="echo [\\`$anynowtime\\`][PID:$$]"',
            '',
            lang === 'zh-CN' ? '##### 可在脚本开始运行时调用，打印当时的时间戳及PID。' : '##### It\'s usually called when the script starts to run, prints the timestamp and PID.',
            'function job_start',
            '{',
            '    echo "`eval $NOW` job_start"',
            '}',
            '',
            lang === 'zh-CN' ? '##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。 ' : '##### Use this func. to combine the execution results of multiple hosts and display by group.',
            'function job_success',
            '{',
            '    MSG="$*"',
            '    echo "`eval $NOW` job_success:[$MSG]"',
            '    exit 0',
            '}',
            '',
            // '#####可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。',
            lang === 'zh-CN' ? '##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。' : '##### Use this func. to combine the execution results of multiple hosts and display by group.',
            'function job_fail',
            '{',
            '    MSG="$*"',
            '    echo "`eval $NOW` job_fail:[$MSG]"',
            '    exit 1',
            '}',
            '',
            'job_start',
            '',
            lang === 'zh-CN' ? '###### 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值' : '###### The script execution result is depends on the value of return/exit code, weather it success or failed.',
            lang === 'zh-CN' ? '###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败' : '###### If return code is 0, means success, otherwise failed.',
            lang === 'zh-CN' ? '###### 可在此处开始编写您的脚本逻辑代码' : '###### Start to writing your script code below this line',
            '',
            '',
        );
        return shell.join('\n');
    }()),
    Bat: (function () {
        const batch = [];
        batch.push(
            '@echo on',
            'setlocal enabledelayedexpansion',
            'call:job_start',
            '',
            lang === 'zh-CN' ? 'REM 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值' : 'REM The script execution result is depends on the value of return/exit code, weather it success or failed.',
            lang === 'zh-CN' ? 'REM 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败' : 'REM If return code is 0, means success, otherwise failed.',
            lang === 'zh-CN' ? 'REM 可在此处开始编写您的脚本逻辑代码' : 'REM Start to writing your script code below this line',
            '',
            '',
            '',
            // 'REM 函数定定义区域，不要把正文写到函数区下面 ',
            lang === 'zh-CN' ? 'REM 函数定定义区域，不要把正文写到函数区下面 ' : 'REM Function definition area, do not write the text below the function area',
            'goto:eof',
            lang === 'zh-CN' ? 'REM 可在脚本开始运行时调用，打印当时的时间戳及PID。' : 'REM It\'s usually called when the script starts to run, prints the timestamp and PID.',
            ':job_start',
            '    set cu_time=[%date:~0,10% %time:~0,8%]',
            '    for /F "skip=3 tokens=2" %%i in (\'tasklist /v /FI "IMAGENAME eq cmd.exe" /FI "STATUS eq Unknown"\') do (',
            '        set pid=[PID:%%i]',
            '        goto:break',
            '    )',
            '    :break',
            '    echo %cu_time%%pid% job_start',
            '    goto:eof',
            '    ',
            lang === 'zh-CN' ? 'REM 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。 ' : 'REM Use this func. to combine the execution results of multiple hosts and display by group.',
            ':job_success',
            '    set cu_time=[%date:~0,10% %time:~0,8%]',
            '    for /F "skip=3 tokens=2" %%i in (\'tasklist /v /FI "IMAGENAME eq cmd.exe" /FI "STATUS eq Unknown"\') do (',
            '        set pid=[PID:%%i]',
            '        goto:break',
            '    )',
            '    :break',
            '    echo %cu_time%%pid% job_success:[%*]',
            '    exit 0',
            '    ',
            lang === 'zh-CN' ? 'REM 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。' : 'REM Use this func. to combine the execution results of multiple hosts and display by group.',
            ':job_fail',
            '    set cu_time=[%date:~0,10% %time:~0,8%]',
            '    for /F "skip=3 tokens=2" %%i in (\'tasklist /v /FI "IMAGENAME eq cmd.exe" /FI "STATUS eq Unknown"\') do (',
            '        set pid=[PID:%%i]',
            '        goto:break',
            '    )',
            '    :break',
            '    echo %cu_time%%pid% job_fail:[%*]',
            '    exit 1',
            '',
            '',
        );
        return batch.join('\n');
    }()),
    Perl: (function () {
        const perl = [];
        perl.push(
            '#!/usr/bin/perl',
            '',
            'use strict;',
            '',
            'sub job_localtime {',
            '    my @n = localtime();',
            '    return sprintf("%04d-%02d-%02d %02d:%02d:%02d",$n[5]+1900,$n[4]+1,$n[3], $n[2], $n[1], $n[0] );',
            '}',
            '',
            lang === 'zh-CN' ? '##### 可在脚本开始运行时调用，打印当时的时间戳及PID。' : '##### It\'s usually called when the script starts to run, prints the timestamp and PID.',
            'sub job_start {',
            '    print "[",&job_localtime,"][PID:$$] job_start\\n";',
            '}',
            '',
            lang === 'zh-CN' ? '##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。 ' : '##### Use this func. to combine the execution results of multiple hosts and display by group.',
            'sub job_success {',
            '    print "[",&job_localtime,"][PID:$$] job_success:[@_]\\n";',
            '    exit 0;',
            '}',
            '',
            // '#####可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。',
            lang === 'zh-CN' ? '##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。' : '##### Use this func. to combine the execution results of multiple hosts and display by group.',
            'sub job_fail {',
            '    print "[",&job_localtime,"][PID:$$] job_fail:[@_]\\n";',
            '    exit 1;',
            '}',
            '',
            'job_start;',
            '',
            lang === 'zh-CN' ? '###### iJobs中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值' : '###### The script execution result is depends on the value of return/exit code, weather it success or failed.',
            lang === 'zh-CN' ? '###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败' : '###### If return code is 0, means success, otherwise failed.',
            lang === 'zh-CN' ? '###### 可在此处开始编写您的脚本逻辑代码' : '###### You can start writing your script logic code here',
            '',
            '',
        );
        return perl.join('\n');
    }()),
    Python: (function () {
        const python = [];
        python.push(
            '#!/usr/bin/env python',
            '# -*- coding: utf8 -*-',
            '',
            'import datetime',
            'import os',
            'import sys',
            '',
            'def _now(format="%Y-%m-%d %H:%M:%S"):',
            '    return datetime.datetime.now().strftime(format)',
            '',
            lang === 'zh-CN' ? '##### 可在脚本开始运行时调用，打印当时的时间戳及PID。' : '##### It\'s usually called when the script starts to run, prints the timestamp and PID.',
            'def job_start():',
            '    print("[%s][PID:%s] job_start" % (_now(), os.getpid()))',
            '',
            lang === 'zh-CN' ? '##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。 ' : '##### Use this func. to combine the execution results of multiple hosts and display by group.',
            'def job_success(msg):',
            '    print("[%s][PID:%s] job_success:[%s]" % (_now(), os.getpid(), msg))',
            '    sys.exit(0)',
            '',
            lang === 'zh-CN' ? '##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。' : '##### Use this func. to combine the execution results of multiple hosts and display by group.',
            'def job_fail(msg):',
            '    print("[%s][PID:%s] job_fail:[%s]" % (_now(), os.getpid(), msg))',
            '    sys.exit(1)', '',
            'if __name__ == \'__main__\':', '',
            '    job_start()', '',
            lang === 'zh-CN' ? '###### iJobs中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值' : '###### The script execution result is depends on the value of return/exit code, weather it success or failed.',
            lang === 'zh-CN' ? '###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败' : '###### If return code is 0, means success, otherwise failed.',
            lang === 'zh-CN' ? '###### 可在此处开始编写您的脚本逻辑代码' : '###### Start to writing your script code below this line',
            '',
            '',
        );
        return python.join('\n');
    }()),
    Powershell: (function () {
        const powershell = [];
        powershell.push(
            lang === 'zh-CN' ? '##### 可在脚本开始运行时调用，打印当时的时间戳及PID。' : '#### It\'s usually called when the script starts to run, prints the timestamp and PID.',
            'function job_start',
            '{',
            '    $cu_date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"    ',
            '    "[{0}][PID:{1}] job_start" -f $cu_date,$pid',
            '}',
            '',
            lang === 'zh-CN' ? '##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。 ' : '##### Use this func. to combine the execution results of multiple hosts and display by group.',
            'function job_success',
            '{',
            '    $cu_date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"',
            '    if($args.count -ne 0)',
            '    {',
            '        $args | foreach {$arg_str=$arg_str + " " + $_}',
            '        "[{0}][PID:{1}] job_success:[{2}]" -f $cu_date,$pid,$arg_str.TrimStart(\' \')',
            '    }',
            '    else',
            '    {',
            '        "[{0}][PID:{1}] job_success:[]" -f $cu_date,$pid',
            '    }',
            '    exit 0',
            '}',
            '',
            lang === 'zh-CN' ? '##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。' : '##### Use this func. to combine the execution results of multiple hosts and display by group.',
            'function job_fail',
            '{',
            '    $cu_date = Get-Date -Format "yyyy-MM-dd HH:mm:ss"',
            '    if($args.count -ne 0)',
            '    {',
            '        $args | foreach {$arg_str=$arg_str + " " + $_}',
            '        "[{0}][PID:{1}] job_fail:[{2}]" -f $cu_date,$pid,$arg_str.TrimStart(\' \')',
            '    }',
            '    else',
            '    {',
            '        "[{0}][PID:{1}] job_fail:[]" -f $cu_date,$pid',
            '    }',
            '    exit 1',
            '}',
            '',
            'job_start',
            '',
            lang === 'zh-CN' ? '###### 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值' : '###### The script execution result is depends on the value of return/exit code, weather it success or failed.',
            lang === 'zh-CN' ? '###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败' : '###### If return code is 0, means success, otherwise failed.',
            lang === 'zh-CN' ? '###### 可在此处开始编写您的脚本逻辑代码' : '###### Start to writing your script code below this line',
            '',
            '',
        );
        return powershell.join('\n');
    }()),
    SQL: (function () {
        return '';
    }()),
};
