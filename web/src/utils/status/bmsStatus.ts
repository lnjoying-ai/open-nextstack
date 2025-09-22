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
  14: string;
  15: string;
  16: string;
  17: string;
  18: string;
  19: string;
  20: string;
  21: string;
  22: string;
  23: string;
  24: string;
  100: string;
  101: string;
  102: string;
  103: string;
  104: string;
  110: string;
  120: string;
  121: string;
  122: string;
  200: string;
  201: string;
  202: string;
  203: string;
  204: string;
  [key: string]: string;
}

const codeMessageMap: codeMessageMapTypes = {
  0: '初始化', // INSTANCE_INIT
  1: '获取镜像连接', // INSTANCE_GETTING_IMAGE_CONNECTION
  2: '获取镜像连接成功', // INSTANCE_GET_IMAGE_CONNECTION_SUCCESS
  3: '获取镜像连接失败', // INSTANCE_GET_IMAGE_CONNECTION_FAILED
  4: '加入部署网络', // INSTANCE_CREATING_SETTING_TO_DEPLOY_NETWORK
  5: '加入部署网络成功', // INSTANCE_SET_TO_DEPLOY_NETWORK_SUCCESS
  6: '加入部署网络失败', // INSTANCE_SET_TO_DEPLOY_NETWORK_FAILED
  7: 'pxe Agent 部署中', // INSTANCE_DEPLOYING_BY_PXE_AGENT
  8: 'pxe Agent 部署成功', // INSTANCE_DEPLOYED_BY_PXE_AGENT_SUCCESS
  9: 'pxe Agent 部署失败', // INSTANCE_DEPLOYED_BY_PXE_AGENT_FAILED
  10: '从部署网络中删除', // INSTANCE_DELETING_FROM_DEPLOY_NETWORK
  11: '从部署网络中删除成功', // INSTANCE_DELETED_FROM_DEPLOY_NETWORK_SUCCESS
  12: '从部署网络中删除失败', // INSTANCE_DELETED_FROM_DEPLOY_NETWORK_FAILED
  13: '加入到租户网络', // INSTANCE_SETTING_TO_TENANT_NETWORK
  14: '加入到租户网络成功', // INSTANCE_SET_TO_TENANT_NETWORK_SUCCESS
  15: '加入到租户网络失败', // INSTANCE_SET_TO_TENANT_NETWORK_FAILED
  16: '注入ISO到CLOUDINIT', // INSTANCE_INJECTING_ISO_TO_CLOUDINIT
  17: '注入ISO到CLOUDINIT成功', // INSTANCE_INJECT_ISO_TO_CLOUDINIT_SUCCESS
  18: '注入ISO到CLOUDINIT失败', // INSTANCE_INJECT_ISO_TO_CLOUDINIT_FAILED
  19: '正在注入ISO到CLOUDINIT中', // WAIT_INSTANCE_CLOUD_INIT_RESULT
  20: 'CLOUDINIT成功', // INSTANCE_CLOUD_INIT_SUCCESS
  21: 'CLOUDINIT失败', // INSTANCE_CLOUD_INIT_FAILED
  22: '使用ARPPING进行实例探测', // INSTANCE_RPOBE_INSTANCE_BY_ARP_PING
  23: '使用ARPPING进行实例探测成功', // INSTANCE_ARP_PING_SUCCESS
  24: '使用ARPPING进行实例探测失败', // INSTANCE_ARP_PING_FAILED
  100: '创建失败', // INSTANCE_CREATE_FAILED
  101: '运行中', // INSTANCE_RUNNING
  102: '关机中', // INSTANCE_POWEROFFING
  103: '已关机', // INSTANCE_POWEROFF
  104: '关机失败', // INSTANCE_POWEROFF_FAILED
  110: '开机中', // INSTANCE_POWERONING
  120: '重启中', // INSTANCE_REBOOTING
  121: '关机中(正在重启)', // INSTANCE_REBOOT_POWEROFFING
  122: '开机中(正在重启)', // INSTANCE_REBOOT_POWERONING
  200: '等待删除', // INSTANCE_WAITING_REMOVED
  201: '删除失败', // INSTANCE_REMOVED_FAILED
  202: '正在从租户网络中删除', // INSTANCE_REMOVE_DELETING_FROM_TENANT_NETWORK
  203: '加入到部署网络中准备删除', // INSTANCE_REMOVE_SETTING_TO_DEPLOY_NETWORK
  204: '通过PXE AGENT摧毁', // INSTANCE_DESTROYING_BY_PXE_AGENT
};
const codeMessageTagMap: codeMessageMapTypes = {
  0: '', // INSTANCE_INIT 初始化',
  1: 'info', // INSTANCE_GETTING_IMAGE_CONNECTION 获取镜像连接',
  2: 'success', // INSTANCE_GET_IMAGE_CONNECTION_SUCCESS 获取镜像连接成功',
  3: 'danger', // INSTANCE_GET_IMAGE_CONNECTION_FAILED 获取镜像连接失败',
  4: 'info', // INSTANCE_CREATING_SETTING_TO_DEPLOY_NETWORK 加入部署网络',
  5: 'success', // INSTANCE_SET_TO_DEPLOY_NETWORK_SUCCESS 加入部署网络成功',
  6: 'danger', // INSTANCE_SET_TO_DEPLOY_NETWORK_FAILED 加入部署网络失败',
  7: '', // INSTANCE_DEPLOYING_BY_PXE_AGENT pxe Agent 部署中
  8: 'success', // INSTANCE_DEPLOYED_BY_PXE_AGENT_SUCCESS pxe Agent 部署成功
  9: 'danger', // INSTANCE_DEPLOYED_BY_PXE_AGENT_FAILED pxe Agent 部署失败
  10: 'warning', // INSTANCE_DELETING_FROM_DEPLOY_NETWORK 从部署网络中删除',
  11: 'danger', // INSTANCE_DELETED_FROM_DEPLOY_NETWORK_SUCCESS 从部署网络中删除成功',
  12: 'danger', // INSTANCE_DELETED_FROM_DEPLOY_NETWORK_FAILED 从部署网络中删除失败',
  13: '', // INSTANCE_SETTING_TO_TENANT_NETWORK 加入到租户网络',
  14: 'success', // INSTANCE_SET_TO_TENANT_NETWORK_SUCCESS 加入到租户网络成功',
  15: 'danger', // INSTANCE_SET_TO_TENANT_NETWORK_FAILED 加入到租户网络失败',
  16: '', // INSTANCE_INJECTING_ISO_TO_CLOUDINIT 注入ISO到CLOUDINIT',
  17: 'success', // INSTANCE_INJECT_ISO_TO_CLOUDINIT_SUCCESS 注入ISO到CLOUDINIT成功',
  18: 'danger', // INSTANCE_INJECT_ISO_TO_CLOUDINIT_FAILED 注入ISO到CLOUDINIT失败',
  19: 'warning', // WAIT_INSTANCE_CLOUD_INIT_RESULT 正在注入ISO到CLOUDINIT中',
  20: 'success', // INSTANCE_CLOUD_INIT_SUCCESS CLOUDINIT成功',
  21: 'danger', // INSTANCE_CLOUD_INIT_FAILED CLOUDINIT失败',
  22: 'info', // INSTANCE_RPOBE_INSTANCE_BY_ARP_PING 使用ARPPING进行实例探测',
  23: 'success', // INSTANCE_ARP_PING_SUCCESS 使用ARPPING进行实例探测成功',
  24: 'danger', // INSTANCE_ARP_PING_FAILED 使用ARPPING进行实例探测失败',
  100: 'danger', // INSTANCE_CREATE_FAILED 创建失败',
  101: 'success', // INSTANCE_RUNNING 运行中',
  102: 'warning', // INSTANCE_POWEROFFING 关机中',
  103: '', // INSTANCE_POWEROFF 已关机',
  104: 'danger', // INSTANCE_POWEROFF_FAILED 关机失败',
  110: '', // INSTANCE_POWERONING 开机中',
  120: 'warning', // INSTANCE_REBOOTING 重启中',
  121: 'warning', // INSTANCE_REBOOT_POWEROFFING 关机中(正在
  122: '', // INSTANCE_REBOOT_POWERONING 开机中(正在
  200: 'warning', // INSTANCE_WAITING_REMOVED 等待删除',
  201: 'danger', // INSTANCE_REMOVED_FAILED 删除失败',
  202: 'warning', // INSTANCE_REMOVE_DELETING_FROM_TENANT_NETWORK 正在从租户网络中删除',
  203: 'warning', // INSTANCE_REMOVE_SETTING_TO_DEPLOY_NETWORK 加入到部署网络中准备删除',
  204: 'danger', // INSTANCE_DESTROYING_BY_PXE_AGENT 通过PXE AGENT
};
const showStatusMessage = (code: number | string, type: any): string => {
  if (type == 'tag') {
    return codeMessageTagMap[JSON.stringify(code)] != undefined ? codeMessageTagMap[JSON.stringify(code)] : 'info';
  }
  return codeMessageMap[JSON.stringify(code)] || '未知状态';
};

export default showStatusMessage;
