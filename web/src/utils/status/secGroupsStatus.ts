import i18n from '@/locales';
declare interface codeMessageMapTypes {
  0: string;
  1: string;
  2: string;
  3: string;
  5: string;
  6: string;
  8: string;
  41: string;
  42: string;
  43: string;
  45: string;

  [key: string]: string;
}

const codeMessageMap: codeMessageMapTypes = {
  0: i18n.global.t('utils.status.secGroupsStatus.create'), // ADDING
  1: i18n.global.t('utils.status.secGroupsStatus.added'), // ADDED
  2: i18n.global.t('utils.status.secGroupsStatus.addFailed'), // ADD_FAILED
  3: i18n.global.t('utils.status.secGroupsStatus.deleting'), // DELETING
  5: i18n.global.t('utils.status.secGroupsStatus.deleteFailed'), // DELETE_FAILED
  6: i18n.global.t('utils.status.secGroupsStatus.create'), // AGENT_ADDING
  8: i18n.global.t('utils.status.secGroupsStatus.deleting'), // DELETING
  41: i18n.global.t('utils.status.secGroupsStatus.updating'), // UPDATING
  42: i18n.global.t('utils.status.secGroupsStatus.added'), // UPDATED
  43: i18n.global.t('utils.status.secGroupsStatus.updating'), // UPDATEING
  45: i18n.global.t('utils.status.secGroupsStatus.updateFailed'), // UPDATE_FAILED
};
const codeMessageTagMap: codeMessageMapTypes = {
  0: '', // ADDING  创建中
  1: 'success', // ADDED  可用
  2: 'danger', // ADD_FAILED  创建失败
  3: 'warning', // DELETING  删除中
  5: 'danger', // DELETE_FAILED  删除失败
  6: '', // AGENT_ADDING  创建中
  8: 'warning', // DELETING  删除中
  41: '', // UPDATING  更新中
  42: 'success', // UPDATED  可用
  43: '', // UPDATEING  更新中
  45: 'danger', // UPDATE_FAILED  更新失败
};
const showSecGroupsStatusMessage = (code: number | string, type: any): string => {
  if (type == 'tag') {
    return codeMessageTagMap[JSON.stringify(code)] != undefined ? codeMessageTagMap[JSON.stringify(code)] : 'info';
  }
  return codeMessageMap[JSON.stringify(code)] || i18n.global.t('utils.status.unknown');
};

export default showSecGroupsStatusMessage;
