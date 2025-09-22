import i18n from '@/locales';
declare interface codeMessageMapTypes {
  1: string;
  2: string;
  3: string;
  4: string;
  5: string;
  6: string;
  7: string;
  70: string;

  [key: string]: string;
}

const codeMessageMap: codeMessageMapTypes = {
  1: i18n.global.t('utils.status.natStatus.unmappingFailed'), // AGENT_UNMAPPING_ERR 删除失败
  2: i18n.global.t('utils.status.natStatus.unmapping'), // AGENT_UNMAPPING 删除中
  3: i18n.global.t('utils.status.natStatus.unmapping'), // UNMAPPING 删除中
  4: i18n.global.t('utils.status.natStatus.mapping'), // MAPPING 创建中
  5: i18n.global.t('utils.status.natStatus.mapping'), // AGENT_MAPPING 创建中
  6: i18n.global.t('utils.status.natStatus.mappingFailed'), // AGENT_MAPPING_ERR 创建失败
  7: i18n.global.t('utils.status.natStatus.running'), // MAPPED 运行中
  70: i18n.global.t('utils.status.natStatus.mapping'), // PORT_CREATING 创建中
};
const codeMessageTagMap: codeMessageMapTypes = {
  1: 'danger', // AGENT_UNMAPPING_ERR 删除失败
  2: 'warning', // AGENT_UNMAPPING 删除中
  3: 'warning', // UNMAPPING 删除中
  4: '', // MAPPING 创建中
  5: '', // AGENT_MAPPING 创建中
  6: 'danger', // AGENT_MAPPING_ERR 创建失败
  7: 'success', // MAPPED 运行中
  70: '', // PORT_CREATING 创建中
};
const showNatStatusMessage = (code: number | string, type: any): string => {
  if (type == 'tag') {
    return codeMessageTagMap[JSON.stringify(code)] != undefined ? codeMessageTagMap[JSON.stringify(code)] : 'info';
  }
  return codeMessageMap[JSON.stringify(code)] || i18n.global.t('utils.status.unknown');
};

export default showNatStatusMessage;
