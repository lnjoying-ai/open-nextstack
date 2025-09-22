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
  41: string;
  42: string;
  45: string;
  20: string;
  21: string;
  22: string;
  23: string;
  24: string;
  25: string;
  26: string;
  27: string;
  28: string;
  29: string;
  30: string;
  31: string;
  32: string;
  33: string;
  34: string;
  35: string;
  36: string;
  37: string;
  38: string;
  39: string;
  50: string;
  51: string;
  52: string;
  53: string;

  [key: string]: string;
}

const codeMessageMap: codeMessageMapTypes = {
  0: i18n.global.t('utils.status.volumeStatus.create'), // ADDING
  1: i18n.global.t('utils.status.volumeStatus.created'), // ADDED
  2: i18n.global.t('utils.status.volumeStatus.createFailed'), // ADD_FAILED
  3: i18n.global.t('utils.status.volumeStatus.delete'), // DELETING
  4: i18n.global.t('utils.status.volumeStatus.deleted'), // DELETED
  5: i18n.global.t('utils.status.volumeStatus.deleteFailed'), // DELETE_FAILED
  6: i18n.global.t('utils.status.volumeStatus.create'), // AGENT_ADDING
  7: i18n.global.t('utils.status.volumeStatus.createFailed'), // AGENT_ADDING_ERR
  8: i18n.global.t('utils.status.volumeStatus.delete'), // AGENT_DELETING
  9: i18n.global.t('utils.status.volumeStatus.deleteFailed'), // AGENT_DELETING_ERR
  41: i18n.global.t('utils.status.volumeStatus.update'), // UPDATING
  42: i18n.global.t('utils.status.volumeStatus.updated'), // UPDATED
  45: i18n.global.t('utils.status.volumeStatus.updateFailed'), // UPDATE_FAILED
  20: i18n.global.t('utils.status.volumeStatus.attach'), // ATTACHING
  21: i18n.global.t('utils.status.volumeStatus.attach'), // AGENT_ATTACHING
  22: i18n.global.t('utils.status.volumeStatus.detach'), // DETACHING
  23: i18n.global.t('utils.status.volumeStatus.detach'), // AGENT_DETACHING
  24: i18n.global.t('utils.status.volumeStatus.attachFailed'), // ATTACH_FAILED
  25: i18n.global.t('utils.status.volumeStatus.detachFailed'), // DETACH_FAILED
  26: i18n.global.t('utils.status.volumeStatus.attached'), // ATTACHED
  27: i18n.global.t('utils.status.volumeStatus.detached'), // DETACHED
  28: i18n.global.t('utils.status.volumeStatus.resume'), // PRE_DEST_RESUMING
  29: i18n.global.t('utils.status.volumeStatus.resume'), // RESUMING
  30: i18n.global.t('utils.status.volumeStatus.resume'), // AGENT_RESUMING
  31: i18n.global.t('utils.status.volumeStatus.running'), // RESUMED
  32: i18n.global.t('utils.status.volumeStatus.migrate'), // SUSPEND
  33: i18n.global.t('utils.status.volumeStatus.export'), // EXPORTING
  34: i18n.global.t('utils.status.volumeStatus.export'), // AGENT_EXPORTING
  35: i18n.global.t('utils.status.volumeStatus.migrate'), // SUSPENDING
  36: i18n.global.t('utils.status.volumeStatus.migrate'), // AGENT_SUSPENDING
  37: i18n.global.t('utils.status.volumeStatus.migrateFailed'), // SUSPEND_FAILED
  38: i18n.global.t('utils.status.volumeStatus.resumeFailed'), // RESUME_FAILED
  39: i18n.global.t('utils.status.volumeStatus.import'), // IMPORTING
  50: i18n.global.t('utils.status.volumeStatus.switch'), // SNAP_SWITCHING
  51: i18n.global.t('utils.status.volumeStatus.switch'), // AGENT_SWITCHING
  52: i18n.global.t('utils.status.volumeStatus.switchFailed'), // SNAP_SWITCH_FAILED
  53: i18n.global.t('utils.status.volumeStatus.exportFailed'), // EXPORT_FAILED
};
const codeMessageTagMap: codeMessageMapTypes = {
  0: '', // ADDING 创建中
  1: 'success', // ADDED 已创建
  2: 'danger', // ADD_FAILED  创建失败
  3: 'warning', // DELETING  删除中
  4: '', // DELETED  已删除
  5: 'danger', // DELETE_FAILED  删除失败
  6: '', // AGENT_ADDING 创建中
  7: 'danger', // AGENT_ADDING_ERR  创建失败
  8: 'warning', // AGENT_DELETING  删除中
  9: 'danger', // AGENT_DELETING_ERR  删除失败
  41: '', // UPDATING 更新中
  42: 'success', // UPDATED 已更新
  45: 'danger', // UPDATE_FAILED 更新失败
  20: '', // ATTACHING 挂载中
  21: '', // AGENT_ATTACHING  挂载中
  22: '', // DETACHING 卸载中
  23: '', // AGENT_DETACHING 卸载中
  24: 'danger', // ATTACH_FAILED 挂载失败
  25: 'danger', // DETACH_FAILED 卸载失败
  26: 'success', // ATTACHED 已挂载
  27: '', // DETACHED 未挂载
  28: '', // PRE_DEST_RESUMING 恢复中
  29: '', // RESUMING 恢复中
  30: '', // AGENT_RESUMING 恢复中
  31: 'success', // RESUMED 运行中
  32: '', // SUSPEND 迁移中
  33: '', // EXPORTING 导出中
  34: '', // AGENT_EXPORTING 导出中
  35: '', // SUSPENDING 迁移中
  36: '', // AGENT_SUSPENDING 迁移中
  37: 'danger', // SUSPEND_FAILED 迁移失败
  38: 'danger', // RESUME_FAILED 恢复失败
  39: '', // IMPORTING 导入中
  50: '', // SNAP_SWITCHING 切换中
  51: '', // AGENT_SWITCHING  切换中
  52: 'danger', // SNAP_SWITCH_FAILED 切换失败
  53: 'danger', // EXPORT_FAILED 导出失败
};
const showVolumeStatusMessage = (code: number | string, type: any): string => {
  if (type == 'tag') {
    return codeMessageTagMap[JSON.stringify(code)] != undefined ? codeMessageTagMap[JSON.stringify(code)] : 'info';
  }
  return codeMessageMap[JSON.stringify(code)] || i18n.global.t('utils.status.unknown');
};

export default showVolumeStatusMessage;
