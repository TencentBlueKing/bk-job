import Vue from 'vue';

const Message = Vue.prototype.$bkMessage;
/**
 * @desc 复制文本
 * @param { String } value
 * @param { String } message
 * @returns { Boolean }
 */
export const execCopy = (value, message = '复制成功') => {
    const textarea = document.createElement('textarea');
    document.body.appendChild(textarea);
    textarea.value = value;
    textarea.select();
    if (document.execCommand('copy')) {
        document.execCommand('copy');
        Message({
            message,
            delay: 500,
            theme: 'success',
        });
    }
    document.body.removeChild(textarea);
};
