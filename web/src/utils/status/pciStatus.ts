import i18n from '@/locales';
declare interface codeMessageMapTypes {
  80: string;
  81: string;
  82: string;
  83: string;
  84: string;
  85: string;
  86: string;
  87: string;
  88: string;

  [key: string]: string;
}

const codeMessageMap: codeMessageMapTypes = {
  80: i18n.global.t('utils.status.pciStatus.attach'), // DEVICE_ATTACHING
  81: i18n.global.t('utils.status.pciStatus.attach'), // DEVICE_AGENT_ATTACHING
  82: i18n.global.t('utils.status.pciStatus.detach'), // DEVICE_DETACHING
  83: i18n.global.t('utils.status.pciStatus.detach'), // DEVICE_AGENT_DETACHING
  84: i18n.global.t('utils.status.pciStatus.attachFailed'), // DEVICE_ATTACH_FAILED
  85: i18n.global.t('utils.status.pciStatus.detachFailed'), // DEVICE_DETACH_FAILED
  86: i18n.global.t('utils.status.pciStatus.attached'), // DEVICE_ATTACHED
  87: i18n.global.t('utils.status.pciStatus.detached'), // DEVICE_DETACHED
  88: i18n.global.t('utils.status.pciStatus.create'), // DEVICE_INIT_CREATE
};
const codeMessageTagMap: codeMessageMapTypes = {
  80: '', // DEVICE_ATTACHING  挂载中
  81: '', // DEVICE_AGENT_ATTACHING 挂载中
  82: 'warning', // DEVICE_DETACHING 卸载中
  83: 'warning', // DEVICE_AGENT_DETACHING 卸载中
  84: 'danger', // DEVICE_ATTACH_FAILED 挂载失败
  85: 'danger', // DEVICE_DETACH_FAILED 卸载失败
  86: 'success', // DEVICE_ATTACHED 已挂载
  87: '', // DEVICE_DETACHED 未挂载
  88: '', // DEVICE_INIT_CREATE 创建中
};
const showPciStatusMessage = (code: number | string, type: any): string => {
  if (type == 'tag') {
    return codeMessageTagMap[JSON.stringify(code)] != undefined ? codeMessageTagMap[JSON.stringify(code)] : 'info';
  }
  return codeMessageMap[JSON.stringify(code)] || i18n.global.t('utils.status.unknown');
};

export default showPciStatusMessage;
