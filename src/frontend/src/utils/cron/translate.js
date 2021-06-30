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

class Node {
    constructor ({
        type,
        value,
        min,
        max,
        repeatInterval,
    }) {
        this.type = type;
        this.value = value || '';
        this.min = min;
        this.max = max;
        this.repeatInterval = repeatInterval;
    }
}

const fieldList = [
    'minute', 'hour', 'dayOfMonth', 'month', 'dayOfWeek',
];

// const preDefined = {
//     '@yearly': '每年',
//     '@monthly': '每月',
//     '@weekly': '每周',
//     '@daily': '每天',
//     '@hourly': '每小时'
// }
const weekDayMap = {
    0: '日',
    1: '一',
    2: '二',
    3: '三',
    4: '四',
    5: '五',
    6: '六',
    7: '日',
};
const weekDesDayMap = {
    sun: '日',
    mon: '一',
    tue: '二',
    wed: '三',
    thu: '四',
    fri: '五',
    sat: '六',
};

const getWeekDayValue = (value) => {
    if (weekDayMap[value]) {
        return weekDayMap[value];
    }
    const text = value.toString().toLowerCase();
    if (weekDesDayMap[text]) {
        return weekDesDayMap[text];
    }
    return value;
};

const getHourValue = (value) => {
    const num = ~~value;
    if (num < 5) {
        return `凌晨${num}点`;
    }
    if (num < 12) {
        return `上午${num}点`;
    }
    if (num === 12) {
        return `中午${num}点`;
    }
    if (num < 18) {
        return `下午${num}点`;
    }
    return `晚上${num}点`;
};

const getMinuteValue = (value) => {
    const num = ~~value;
    if (num < 10) {
        return `0${num}`;
    }
    return num;
};

const parsetest = (expression) => {
    const stack = [];
    const rangReg = /-/;
    const repeatReg = /\//;
    const atoms = (`${expression}`).trim().split(',');
    let index = -1;
    // eslint-disable-next-line no-plusplus
    while (++index < atoms.length) {
        const enumValue = atoms[index];
        if (rangReg.test(enumValue) && repeatReg.test(enumValue)) {
            const [
                rang, repeatInterval,
            ] = enumValue.split('/');
            const [
                min, max,
            ] = rang.split('-');
            stack.push(new Node({
                type: 4,
                min,
                max,
                repeatInterval,
            }));
            continue;
        } else if (repeatReg.test(enumValue)) {
            const [
                value, repeatInterval,
            ] = enumValue.split('/');
            stack.push(new Node({
                type: 3,
                value,
                repeatInterval,
            }));
            continue;
        } else if (rangReg.test(enumValue)) {
            const [
                min, max,
            ] = enumValue.split('-');
            stack.push(new Node({
                type: 2,
                min,
                max,
            }));
            continue;
        } else {
            stack.push(new Node({
                type: 1,
                value: enumValue,
            }));
        }
    }
    return stack;
};

const optimze = (fieldMap) => {
    const isAllValue = node => node.length === 1
    && node[0].type === 1
    && (node[0].value === '*' || node[0].value === '?');
    const prettyMap = {};

    prettyMap.month = isAllValue(fieldMap.month) ? [] : fieldMap.month;

    if (isAllValue(fieldMap.dayOfMonth) && isAllValue(fieldMap.month) && isAllValue(fieldMap.dayOfWeek)) {
        prettyMap.dayOfMonth = [];
        delete prettyMap.month;
    } else {
        if (!isAllValue(fieldMap.dayOfWeek)) {
            prettyMap.dayOfWeek = fieldMap.dayOfWeek;
        }
        if (!isAllValue(fieldMap.dayOfMonth)) {
            prettyMap.dayOfMonth = fieldMap.dayOfMonth;
        }
        if (!prettyMap.dayOfMonth && !prettyMap.dayOfWeek && prettyMap.month.length > 0) {
            prettyMap.dayOfMonth = [];
        }
    }
    prettyMap.hour = isAllValue(fieldMap.hour) ? [] : fieldMap.hour;
    if (prettyMap.hour.length < 1 && prettyMap.dayOfMonth && prettyMap.dayOfMonth.length < 1) {
        delete prettyMap.dayOfMonth;
    }
    prettyMap.minute = isAllValue(fieldMap.minute) ? [] : fieldMap.minute;
    if (prettyMap.minute.length < 1 && prettyMap.hour.length < 1) {
        delete prettyMap.hour;
    }
    return prettyMap;
};

const translateText = (ast) => {
    const concatText = (target) => {
        if (target.length < 2) {
            return target.join('');
        }
        const pre = target.slice(0, -1);
        const last = target.slice(-1);
        return `${pre.join('，')}和${last[0]}`;
    };
    const translateMinute = (ast) => {
        if (!Object.prototype.hasOwnProperty.call(ast, 'minute')) {
            return '';
        }
        const sequence = ast.minute;
        if (sequence.length < 1) {
            return '每分钟';
        }
        
        const stack = sequence.map((day) => {
            if (day.type === 1) {
                return `${getMinuteValue(day.value)}分`;
            }
            if (day.type === 2) {
                return `${getMinuteValue(day.min)}分到${getMinuteValue(day.max)}分`;
            }
            if (day.type === 3) {
                if (day.value === '*') {
                    return `每隔${day.repeatInterval}分钟`;
                }
                return `从${getMinuteValue(day.value)}分开始每隔${day.repeatInterval}分钟`;
            }
            if (day.type === 4) {
                return `从${getMinuteValue(day.min)}分开始到${getMinuteValue(day.max)}分的每${day.repeatInterval}分钟`;
            }
            return '';
        });
        return concatText(stack);
    };
    
    const translateHour = (ast) => {
        if (!Object.prototype.hasOwnProperty.call(ast, 'hour')) {
            return '';
        }
        const sequence = ast.hour;
        if (sequence.length < 1) {
            return '每小时';
        }
        const stack = sequence.map((hour) => {
            if (hour.type === 1) {
                return `${getHourValue(hour.value)}`;
            }
            if (hour.type === 2) {
                return `${getHourValue(hour.min)}到${getHourValue(hour.max)}`;
            }
            if (hour.type === 3) {
                if (hour.value === '*') {
                    return `每隔${hour.repeatInterval}个小时`;
                }
                return `从${getHourValue(hour.value)}开始每隔${hour.repeatInterval}个小时`;
            }
            if (hour.type === 4) {
                return `从${getHourValue(hour.min)}开始到${getHourValue(hour.max)}的每${hour.repeatInterval}个小时`;
            }
            return '';
        });
        return concatText(stack);
    };
    
    const translateDayOfMonth = (ast) => {
        if (!Object.prototype.hasOwnProperty.call(ast, 'dayOfMonth')) {
            return '';
        }
        const sequence = ast.dayOfMonth;
        if (sequence.length < 1) {
            return '每天';
        }
        const stack = sequence.map((day) => {
            if (day.type === 1) {
                return `${day.value}号`;
            }
            if (day.type === 2) {
                return `${day.min}号到${day.max}号`;
            }
            if (day.type === 3) {
                if (day.value === '*') {
                    return `每隔${day.repeatInterval}天`;
                }
                return `从${day.value}号开始每隔${day.repeatInterval}天`;
            }
            if (day.type === 4) {
                return `从${day.min}号开始到${day.max}号的每${day.repeatInterval}天`;
            }
            return '';
        });
        return concatText(stack);
    };
    
    const translateMonth = (ast) => {
        if (!Object.prototype.hasOwnProperty.call(ast, 'month')) {
            return '';
        }
        const sequence = ast.month;
        if (sequence.length < 1) {
            return '每月';
        }
        const stack = sequence.map((month) => {
            if (month.type === 1) {
                return `${month.value}月`;
            }
            if (month.type === 2) {
                return `${month.min}月到${month.max}月`;
            }
            if (month.type === 3) {
                if (month.value === '*') {
                    return `每隔${month.repeatInterval}个月`;
                }
                return `从${month.value}月开始每隔${month.repeatInterval}个月`;
            }
            if (month.type === 4) {
                return `从${month.min}月开始到${month.max}月的每${month.repeatInterval}个月`;
            }
            return '';
        });
        return concatText(stack);
    };
    
    const translateDayOfWeek = (ast) => {
        if (!Object.prototype.hasOwnProperty.call(ast, 'dayOfWeek')) {
            return '';
        }
        const sequence = ast.dayOfWeek;
        if (sequence.length < 1) {
            return '每天';
        }
        const stack = sequence.map((week) => {
            if (week.type === 1) {
                return `每周${getWeekDayValue(week.value)}`;
            }
            if (week.type === 2) {
                return `每周${getWeekDayValue(week.min)}到周${getWeekDayValue(week.max)}`;
            }
            if (week.type === 3) {
                if (week.value === '*') {
                    return `每隔${week.repeatInterval}天`;
                }
                return `从每周${getWeekDayValue(week.value)}开始每隔${week.repeatInterval}天`;
            }
            if (week.type === 4) {
                return `从每周${getWeekDayValue(week.min)}开始到周${getWeekDayValue(week.max)}的每${week.repeatInterval}天`;
            }
            return '';
        });
        return concatText(stack);
    };
    
    return [
        translateMonth(ast),
        translateDayOfMonth(ast),
        translateDayOfWeek(ast),
        translateHour(ast),
        translateMinute(ast),
    ];
};

const print = (expression) => {
    const atoms = (`${expression}`).trim().split(/\s+/);
    const fieldMap = {};
    atoms.forEach((item, index) => {
        fieldMap[fieldList[index]] = parsetest(item);
    });
    const ast = optimze(fieldMap);
    return translateText(ast);
};

export default expression => print(expression);
