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

  [key: string]: string;
}

const codeMessageMap: codeMessageMapTypes = {
  80: i18n.global.t('utils.status.eipStatus.attachInit'), // ATTACH_EIP_INIT
  81: i18n.global.t('utils.status.eipStatus.attachIng'), // ATTACH_EIP_ING
  82: i18n.global.t('utils.status.eipStatus.attachDone'), // ATTACH_EIP_DONE
  83: i18n.global.t('utils.status.eipStatus.detachInit'), // DETACH_EIP_INIT
  84: i18n.global.t('utils.status.eipStatus.detachIng'), // DETACH_EIP_ING
  85: i18n.global.t('utils.status.eipStatus.detachDone'), // DETACH_EIP_DONE
  86: i18n.global.t('utils.status.eipStatus.attachFailed'), // ATTACH_EIP_ERR
  87: i18n.global.t('utils.status.eipStatus.detachFailed'), // DETACH_EIP_ERR
};
const codeMessageTagMap: codeMessageMapTypes = {
  80: '', // ATTACH_EIP_INIT
  81: '', // ATTACH_EIP_ING
  82: 'success', // ATTACH_EIP_DONE
  83: 'warning', // DETACH_EIP_INIT
  84: 'warning', // DETACH_EIP_ING
  85: 'success', // DETACH_EIP_DONE
  86: 'danger', // ATTACH_EIP_ERR
  87: 'danger', // DETACH_EIP_ERR
};
const showVmToEipStatusMessage = (code: number | string, type: any): string => {
  if (type == 'tag') {
    return codeMessageTagMap[JSON.stringify(code)] != undefined ? codeMessageTagMap[JSON.stringify(code)] : 'info';
  }
  return codeMessageMap[JSON.stringify(code)] || i18n.global.t('utils.status.unknown');
};

export default showVmToEipStatusMessage;
