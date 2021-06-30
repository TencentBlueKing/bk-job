/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
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

truncate table script;
truncate table script_version;

insert into job_manage.script (id,name,app_id,type,creator,create_time,last_modify_user,last_modify_time,category,tags,description) values ('dc65a20cd91811e993a2309c2357fc12','test1',2,1,'user1',1568710800000,'user1',1568710860000,1,'<1>,<2>','desc1');
insert into job_manage.script (id,name,app_id,type,creator,create_time,last_modify_user,last_modify_time,category,tags,description) values ('d68700a6db8711e9ac466c92bf62a896','test2',2,1,'user1',1568710920000,'user1',1568710920000,1,'<2>,<3>','desc2');
insert into job_manage.script (id,name,app_id,type,creator,create_time,last_modify_user,last_modify_time,category,tags,description) values ('553285c5db8211e9ac466c92bf62a896','test3',2,2,'user2',1568710980000,'user2',1568711040000,1,'<2>,<3>','desc3');
insert into job_manage.script (id,name,app_id,type,creator,create_time,last_modify_user,last_modify_time,category,tags,description) values ('3507cad7db8411e9ac466c92bf62a896','deploy',3,1,'user3',1568711100000,'user3',1568711100000,1,'<4>,<5>','desc4');
insert into job_manage.script (id,name,app_id,type,creator,create_time,last_modify_user,last_modify_time,category,tags,description, is_public) values ('3507cad7db8411e9ac466c92bf62a321','deploy',2,1,'user3',1568711100000,'user3',1568711100000,1,'<4>,<5>','desc4',1);

insert into job_manage.script_version (id,script_id,content,version_desc,creator,create_time,last_modify_user,last_modify_time,version,status) values (1,'dc65a20cd91811e993a2309c2357fc12','df','version_desc1','user1',1568710800000,'user1',1568710800000,'user1.20190917170000',0);
insert into job_manage.script_version (id,script_id,content,version_desc,creator,create_time,last_modify_user,last_modify_time,version,status) values (2,'dc65a20cd91811e993a2309c2357fc12','ls','version_desc2','user1',1568710860000,'user1',1568710860000,'user1.20190917170100',1);
insert into job_manage.script_version (id,script_id,content,version_desc,creator,create_time,last_modify_user,last_modify_time,version,status) values (3,'d68700a6db8711e9ac466c92bf62a896','df','version_desc3','user1',1568710920000,'user1',1568710920000,'user1.20190917170200',1);
insert into job_manage.script_version (id,script_id,content,version_desc,creator,create_time,last_modify_user,last_modify_time,version,status) values (4,'553285c5db8211e9ac466c92bf62a896','df','version_desc4','user2',1568710980000,'user2',1568710980000,'user2.20190917170300',1);
insert into job_manage.script_version (id,script_id,content,version_desc,creator,create_time,last_modify_user,last_modify_time,version,status) values (5,'553285c5db8211e9ac466c92bf62a896','ls','version_desc5','user2',1568711040000,'user2',1568711040000,'user2.20190917170400',2);
insert into job_manage.script_version (id,script_id,content,version_desc,creator,create_time,last_modify_user,last_modify_time,version,status) values (6,'3507cad7db8411e9ac466c92bf62a896','df','version_desc6','user3',1568711100000,'user3',1568711100000,'user3.20190917170500',3);
insert into job_manage.script_version (id,script_id,content,version_desc,creator,create_time,last_modify_user,last_modify_time,version,status) values (7,'3507cad7db8411e9ac466c92bf62a321','df','version_desc','user3',1568711100000,'user3',1568711100000,'user3.20190917170500',3);
