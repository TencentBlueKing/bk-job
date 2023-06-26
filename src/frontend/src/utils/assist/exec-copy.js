import { messageSuccess } from '@/common/bkmagic';
import I18n from '@/i18n';

export const execCopy = (value, message = I18n.t('复制成功')) => {
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
