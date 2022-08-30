import { messageSuccess } from '@/common/bkmagic';
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
        messageSuccess(message);
    }
    document.body.removeChild(textarea);
};
