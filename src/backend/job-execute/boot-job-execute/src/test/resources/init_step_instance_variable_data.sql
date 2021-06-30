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

TRUNCATE TABLE step_instance_variable;

INSERT INTO job_execute.step_instance_variable(task_instance_id,step_instance_id,execute_count, type,param_values) values (1, 1, 0, 1, '{
    "stepInstanceId": 1,
    "taskInstanceId": 1,
    "execute_count": 0,
    "type": 1,
    "globalParams": [{
            "name": "param1",
            "value": "value1"
        }, {
            "name": "param2",
            "value": "value2"
        }
    ],
    "namespaceParams": [{
            "ip": "1.1.1.1",
            "values": [{
                    "name": "param1",
                    "value": "value1"
                }, {
                    "name": "param2",
                    "value": "value2"
                }
            ]
        }
    ]
}');

INSERT INTO job_execute.step_instance_variable(task_instance_id,step_instance_id,execute_count,type,param_values) values (1, 2, 0, 1, '{
    "stepInstanceId": 2,
    "taskInstanceId": 1,
    "type": 1,
    "execute_count": 0,
    "globalParams": [{
            "name": "param1",
            "value": "new_value1"
        }, {
            "name": "param2",
            "value": "new_value2"
        }
    ],
    "namespaceParams": [{
            "ip": "1.1.1.1",
            "values": [{
                    "name": "param1",
                    "value": "new_value1"
                }, {
                    "name": "param2",
                    "value": "new_value2"
                }
            ]
        }
    ]
}');
INSERT INTO job_execute.step_instance_variable(task_instance_id,step_instance_id,execute_count,type,param_values) values (1, 2, 1, 1, '{
    "stepInstanceId": 2,
    "taskInstanceId": 1,
    "execute_count": 1,
    "type": 1,
    "globalParams": [{
            "name": "param1",
            "value": "new-latest-value1"
        }, {
            "name": "param2",
            "value": "new-latest-value2"
        }
    ],
    "namespaceParams": [{
            "ip": "1.1.1.1",
            "values": [{
                    "name": "param1",
                    "value": "new-latest-value1"
                }, {
                    "name": "param2",
                    "value": "new-latest-value2"
                }
            ]
        }
    ]
}');
INSERT INTO job_execute.step_instance_variable(task_instance_id,step_instance_id,execute_count,type,param_values) values (1, 3, 0, 1, '{
    "stepInstanceId": 3,
    "taskInstanceId": 1,
    "execute_count": 0,
    "type": 1,
    "globalParams": [{
            "name": "param1",
            "value": "new-latest-value1"
        }, {
            "name": "param2",
            "value": "new-latest-value2"
        }
    ],
    "namespaceParams": [{
            "ip": "1.1.1.1",
            "values": [{
                    "name": "param1",
                    "value": "new-latest-value1"
                }, {
                    "name": "param2",
                    "value": "new-latest-value2"
                }
            ]
        }
    ]
}');

INSERT INTO job_execute.step_instance_variable(task_instance_id,step_instance_id,execute_count,type,param_values) values (2, 4, 0, 1, '{
    "stepInstanceId": 3,
    "taskInstanceId": 2,
    "execute_count": 0,
    "type": 1,
    "globalParams": [{
            "name": "param1",
            "value": "value1"
        }, {
            "name": "param2",
            "value": "value2"
        }
    ],
    "namespaceParams": [{
            "ip": "1.1.1.1",
            "values": [{
                    "name": "param1",
                    "value": "value1"
                }, {
                    "name": "param2",
                    "value": "value2"
                }
            ]
        }
    ]
}');

