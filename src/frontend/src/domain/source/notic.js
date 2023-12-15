import Request from '@utils/request';

import ModuleBase from './module-base';

class NoticManage  extends ModuleBase {
  constructor() {
    super();
    this.module = '/job-manage/web/notice';
  }
  getAnnouncement() {
    return Request.get(`${this.module}/announcement/currentAnnouncements`);
  }
}

export default new NoticManage();
