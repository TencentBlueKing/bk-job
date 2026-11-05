# 作业平台 V3 和 V2 有什么差异

作业平台 V3 新版除了在系统架构上全面采用「微服务」模式设计以外，在产品功能和体验上也实现了全面的提升！同样，产品架构和规划上也更突显了平台能力的方向和定位，持续深化 `脚本执行`  `文件分发` 这两个核心原子能力。

<table><tbody>
<tr style="font-weight:bold;"><td width="18%" ></td><td width="41%">	Job-v2 旧版	</td><td width="41%">	Job-v3 新版	</td></tr>
<tr><td style="vertical-align:middle;">	脚本执行	</td><td style="vertical-align:middle;">	支持 Shell / Bat / Perl / Python / PowerShell	</td><td style="vertical-align:middle;">	支持 Shell / Bat / Perl / Python / PowerShell / SQL	</td></tr>
<tr><td>	文件分发	</td><td>	不支持限速、不支持超时设置	</td><td>	支持超时、支持限速<br/>支持不同的传输模式	</td></tr>
<tr><td rowspan="2" style="vertical-align:middle;">	作业模板	</td><td rowspan="2" style="vertical-align:middle;">	「模板+作业实例」整合模式，通过「全局变量」方式统一配置和管理步骤所需的参数和主机，作业模板和实例的关系是「一对一」，每次执行需要从模板中勾选对应的步骤来执行。	</td><td>	纯模板，不可执行；使用「全局变量」的方式来统一配置和管理步骤所需的参数和主机，通过生成「执行方案」来管理不同场景的作业实例；作业模板和实例的关系是「一对多」。	</td></tr>
<tr><td>	步骤顺序变更、步骤增删，以及步骤的任意参数/内容修改（包括脚本的不同版本变更），都会使模板与执行方案产生差异！	</td></tr>
<tr><td rowspan="3" style="vertical-align:middle;">	执行方案	</td><td rowspan="3" style="vertical-align:middle;">	-	</td><td>	Job v3中作业的执行态改名为「作业执行方案」，由模板衍生的作业实例，一个真正可执行的作业对象；模板有改动时，由执行方案来选择是否同步来自模板的变更。	</td></tr>
<tr><td>	执行方案无法修改步骤的参数和执行目标，统一通过给「全局变量」的不同赋值来区分。 </td></tr>
<tr><td>	执行方案同步时，可以对比与模板的差异明细。	</td></tr>
<tr><td rowspan="4" style="vertical-align:middle;">	脚本管理	</td><td>	业务脚本 和 公共脚本（全局）	</td><td>	业务脚本 和 公共脚本（全局）	</td></tr>
<tr><td style="vertical-align:middle;">	一个脚本内可以有多个版本同时处于可用	</td><td style="vertical-align:middle;">	一个脚本内只能有一个版本处于「线上版」，即新创建的作业只能引用该版本（但存量的作业已引用旧的版本不受影响）；脚本版本状态支持「禁用」，禁用的脚本会完全不可用！用于在紧急或安全风险场景下，快速止损！ </td></tr>
<tr><td style="vertical-align:middle;">	不支持查询作业引用方	</td><td>	支持反向查询作业引用方	</td></tr>
<tr><td> - </td><td>支持版本对比</td></tr>
<tr><td rowspan="4" style="vertical-align:middle;">	主机选择方式	</td><td>	手工录入	</td><td>	手工录入	</td></tr>
<tr><td style="vertical-align:middle;">	拓扑选择主机	</td><td>	拓扑选择主机 </td></tr>
<tr><td style="vertical-align:middle;">	-	</td><td>	拓扑选择动态节点	</td></tr>
<tr><td style="vertical-align:middle;">	动态分组	</td><td>	动态分组	</td></tr>
<tr><td>	定时任务	</td><td>	Quartz后台 + 自带表达式	</td><td>	Quartz后台 + 类Linux原生Cron表达式	</td></tr>
<tr><td>	账号管理	</td><td>	面向全业务管理	</td><td>	按业务维度管理	</td></tr>
<tr><td>	消息通知	</td><td>	支持对作业执行人自己的执行成功、失败和等待确认状态的消息通知	</td><td>	支持灵活配置对任意业务角色或人员进行执行成功、失败状态的消息通知	</td></tr>
<tr><td>	高危语法检测	</td><td>	-	</td><td>	支持按脚本类型配置高危语句提醒规则	</td></tr>
<tr><td>	IP 白名单	</td><td>	支持	</td><td>	支持（不变）	</td></tr>
<tr><td rowspan="5" style="vertical-align:middle;">	全局设置	</td><td>	-	</td><td>	支持消息通知渠道和模板的配置	</td></tr>
<tr><td style="vertical-align:middle;">	-	</td><td>	存储策略配置 </td></tr>
<tr><td style="vertical-align:middle;">	-	</td><td>	账号命名规则配置	</td></tr>
<tr><td style="vertical-align:middle;">	-	</td><td>	按脚本类型支持高危语句规则配置	</td></tr>
<tr><td style="vertical-align:middle;">	--	</td><td>	支持个性化配置平台信息	</td></tr>
<tr><td>	CMDB数据同步机制	</td><td>	延迟定时拉取	</td><td>	通过事件订阅，秒级同步	</td></tr>
</tbody></table>



