import i18n from '@/locales';
declare interface codeMessageMapTypes {
  0: string;
  1: string;
  2: string;
  3: string;
  4: string;
  5: string;
  6: string;
  7: string;
  8: string;
  9: string;
  10: string;
  11: string;
  12: string;
  13: string;
  [key: string]: string;
}

const codeMessageMap: codeMessageMapTypes = {
  0: i18n.global.t('utils.status.status.create'), // ADDING
  1: i18n.global.t('utils.status.status.running'), // ADDED
  2: i18n.global.t('utils.status.status.createFailed'), // ADD_FAILED
  3: i18n.global.t('utils.status.status.deleting'), // DELETING
  4: i18n.global.t('utils.status.status.deleted'), // DELETED
  5: i18n.global.t('utils.status.status.deleteFailed'), // DELETE_FAILED
  6: i18n.global.t('utils.status.status.create'), // AGENT_ADDING
  7: i18n.global.t('utils.status.status.createFailed'), // AGENT_ADDING_ERR
  8: i18n.global.t('utils.status.status.deleting'), // AGENT_DELETING
  9: i18n.global.t('utils.status.status.deleteFailed'), // AGENT_DELETING_ERR
  10: i18n.global.t('utils.status.status.empty'), // ARPING
  11: i18n.global.t('utils.status.status.empty'), // ARPING_DONE
  12: i18n.global.t('utils.status.status.empty'), // ARPING_ERR
  13: i18n.global.t('utils.status.status.empty'), // ARPING_OK
};
const codeMessageTagMap: codeMessageMapTypes = {
  0: '', // ADDING 创建中
  1: 'success', // ADDED 运行中
  2: 'danger', // ADD_FAILED 创建失败
  3: 'warning', // DELETING 删除中
  4: 'danger', // DELETED 已删除
  5: 'danger', // DELETE_FAILED 删除失败
  6: '', // AGENT_ADDING 创建中
  7: 'danger', // AGENT_ADDING_ERR 创建失败
  8: 'warning', // AGENT_DELETING 删除中
  9: 'danger', // AGENT_DELETING_ERR 删除失败
  10: '-', // ARPING
  11: '-', // ARPING_DONE
  12: '-', // ARPING_ERR
  13: '-', // ARPING_OK
};
const showStatusMessage = (code: number | string, type: any): string => {
  if (type == 'tag') {
    return codeMessageTagMap[JSON.stringify(code)] != undefined ? codeMessageTagMap[JSON.stringify(code)] : 'info';
  }
  return codeMessageMap[JSON.stringify(code)] || i18n.global.t('utils.status.unknown');
};

export default showStatusMessage;
