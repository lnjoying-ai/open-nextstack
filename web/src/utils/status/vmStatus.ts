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
  40: string;
  41: string;
  42: string;
  60: string;
  61: string;
  62: string;
  63: string;
  64: string;
  65: string;
  66: string;
  70: string;
  101: string;
  103: string;
  104: string;
  105: string;
  110: string;
  111: string;
  300: string;
  301: string;
  302: string;
  303: string;
  304: string;
  305: string;
  350: string;
  351: string;
  352: string;
  353: string;
  360: string;
  361: string;
  [key: string]: string;
}

const codeMessageMap: codeMessageMapTypes = {
  0: i18n.global.t('utils.status.vmStatus.create'), // INSTANCE_INIT
  1: i18n.global.t('utils.status.vmStatus.create'), // INSTANCE_CREATING
  2: i18n.global.t('utils.status.vmStatus.create'), // INSTANCE_CREATED
  3: i18n.global.t('utils.status.vmStatus.create'), // INSTANCE_INJECTING
  4: i18n.global.t('utils.status.vmStatus.create'), // INSTANCE_INJECT_BOOTING
  5: i18n.global.t('utils.status.vmStatus.createSuccess'), // INSTANCE_CLOUDINIT_DONE
  6: i18n.global.t('utils.status.vmStatus.running'), // INSTANCE_RUNNING
  7: i18n.global.t('utils.status.vmStatus.poweringOff'), // INSTANCE_POWEROFFING
  8: i18n.global.t('utils.status.vmStatus.poweredOff'), // INSTANCE_POWEROFF
  9: i18n.global.t('utils.status.vmStatus.poweringOn'), // INSTANCE_POWERONING
  10: i18n.global.t('utils.status.vmStatus.running'), // INSTANCE_RUNNING
  11: i18n.global.t('utils.status.vmStatus.create'), // GET_PORT_PHASE_STATUS
  12: i18n.global.t('utils.status.vmStatus.create'), // GET_INSTANCE_CREATED_STATUS
  13: i18n.global.t('utils.status.vmStatus.create'), // WAIT_INSTANCE_CLOUD_INIT_RESULT
  14: i18n.global.t('utils.status.vmStatus.deleting'), // GET_INSTANCE_REMOVED_STATUS
  15: i18n.global.t('utils.status.vmStatus.deleting'), // GET_SNAP_REMOVED_STATUS
  16: i18n.global.t('utils.status.vmStatus.poweringOn'), // GET_INSTANCE_POWERON_RESULT
  17: i18n.global.t('utils.status.vmStatus.poweringOff'), // GET_INSTANCE_POWEROFF_RESULT
  18: i18n.global.t('utils.status.vmStatus.snapshotPreparing'), // GET_SNAP_SWITCHED_STATUS
  21: i18n.global.t('utils.status.vmStatus.createFailed'), // INSTANCE_CREATE_FAILED
  22: i18n.global.t('utils.status.vmStatus.deleteFailed'), // INSTANCE_REMOVE_FAILED
  23: i18n.global.t('utils.status.vmStatus.createFailed'), // INSTANCE_INJECT_BOOT_FAILED
  24: i18n.global.t('utils.status.vmStatus.createFailed'), // INSTANCE_EJECT_FAILED
  25: i18n.global.t('utils.status.vmStatus.createFailed'), // SNAP_CREATE_FAILED
  26: i18n.global.t('utils.status.vmStatus.deleteFailed'), // SNAP_REMOVE_FAILED
  27: i18n.global.t('utils.status.vmStatus.snapshotSwitchFailed'), // SNAP_SWITCH_FAILED
  28: i18n.global.t('utils.status.vmStatus.migrateFailed'), // INSTANCE_MIGRATE_FAILED
  29: i18n.global.t('utils.status.vmStatus.running'), // INSTANCE_MIGRATE_CLEAN
  30: i18n.global.t('utils.status.vmStatus.create'), // SNAP_INIT
  31: i18n.global.t('utils.status.vmStatus.create'), // SNAP_CREATING
  32: i18n.global.t('utils.status.vmStatus.createSuccess'), // SNAP_CREATED
  33: i18n.global.t('utils.status.vmStatus.rollingBack'), // SNAP_SWITCHING
  34: i18n.global.t('utils.status.vmStatus.rollingBackSuccess'), // SNAP_SWITCHED
  35: i18n.global.t('utils.status.vmStatus.deleting'), // SNAP_REMOVING
  40: i18n.global.t('utils.status.vmStatus.added'), // HYPERVISOR_NODE_CREATED
  41: i18n.global.t('utils.status.vmStatus.adding'), // HYPERVISOR_NODE_CHECKING
  42: i18n.global.t('utils.status.vmStatus.offline'), // HYPERVISOR_NODE_OFFLINE
  60: i18n.global.t('utils.status.vmStatus.poweringOff'), // 关机中
  61: i18n.global.t('utils.status.vmStatus.poweringOff'), // INSTANCE_POWERING_OFF_DETACH_PCI
  62: i18n.global.t('utils.status.vmStatus.poweringOff'), // GET_INSTANCE_POWERING_OFF_DETACH_PCI_STATUS
  63: i18n.global.t('utils.status.vmStatus.poweredOff'), // INSTANCE_POWERED_OFF_DETACH_PCI
  64: i18n.global.t('utils.status.vmStatus.poweringOn'), // INSTANCE_POWERING_ON_PREPARE_PCI
  65: i18n.global.t('utils.status.vmStatus.poweringOn'), // INSTANCE_POWERING_ON_ATTACH_PCI
  66: i18n.global.t('utils.status.vmStatus.powerOnFailed'), // INSTANCE_POWER_ON_FAILED
  70: i18n.global.t('utils.status.vmStatus.create'), // PORT_CREATE
  101: i18n.global.t('utils.status.vmStatus.deleting'), // INSTANCE_REMOVING
  103: i18n.global.t('utils.status.vmStatus.deleteFailed'), // INSTANCE_REMOVED_FAILED
  104: i18n.global.t('utils.status.vmStatus.create'), // INSTANCE_EJECTING
  105: i18n.global.t('utils.status.vmStatus.created'), // INSTANCE_EJECTED
  110: i18n.global.t('utils.status.vmStatus.createFailed'), // INSTANCE_CREATE_FAILED_CLEANING
  111: i18n.global.t('utils.status.vmStatus.createFailed'), // INSTANCE_CREATE_FAILED_CLEANED
  300: i18n.global.t('utils.status.vmStatus.migrate'), // INSTANCE_MIGRATE_INIT
  301: i18n.global.t('utils.status.vmStatus.migrating'), // INSTANCE_SUSPENDING
  302: i18n.global.t('utils.status.vmStatus.migrating'), // INSTANCE_SUSPENDED
  303: i18n.global.t('utils.status.vmStatus.migrating'), // GET_PORT_MIGRATED_STATUS
  304: i18n.global.t('utils.status.vmStatus.migrating'), // GET_INSTANCE_RESUME_STATUS
  305: i18n.global.t('utils.status.vmStatus.migrating'), // INSTANCE_RESUMED
  350: i18n.global.t('utils.status.vmStatus.updating'), // INSTANCE_RESIZE_INIT
  351: i18n.global.t('utils.status.vmStatus.updating'), // GET_INSTANCE_UPDATED_STATUS
  352: i18n.global.t('utils.status.vmStatus.bootDevSwitching'), // INSTANCE_BOOT_DEV_SWITCHING
  353: i18n.global.t('utils.status.vmStatus.bootDevSwitching'), // GET_INSTANCE_BOOT_DEV_STATUS
  360: i18n.global.t('utils.status.vmStatus.resetPassword'), // INSTANCE_RESET_PASSWORD_HOSTNAME
  361: i18n.global.t('utils.status.vmStatus.resetPasswording'), // WAIT_INSTANCE_RESET_PASSWORD_HOSTNAME
};
const codeMessageTagMap: codeMessageMapTypes = {
  0: '', // INSTANCE_INIT  创建中
  1: '', // INSTANCE_CREATING  创建中
  2: '', // INSTANCE_CREATED  创建中
  3: '', // INSTANCE_INJECTING  创建中
  4: '', // INSTANCE_INJECT_BOOTING  创建中
  5: 'success', // INSTANCE_CLOUDINIT_DONE  创建成功
  6: 'success', // INSTANCE_RUNNING  运行中
  7: 'warning', // INSTANCE_POWEROFFING  关机中
  8: 'warning', // INSTANCE_POWEROFF  已关机
  9: '', // INSTANCE_POWERONING  开机中
  10: 'success', // INSTANCE_RUNNING  运行中
  11: '', // GET_PORT_PHASE_STATUS  创建中
  12: '', // GET_INSTANCE_CREATED_STATUS  创建中
  13: '', // WAIT_INSTANCE_CLOUD_INIT_RESULT  创建中
  14: 'warning', // GET_INSTANCE_REMOVED_STATUS  删除中
  15: 'warning', // GET_SNAP_REMOVED_STATUS  删除中
  16: '', // GET_INSTANCE_POWERON_RESULT  开机中
  17: 'warning', // GET_INSTANCE_POWEROFF_RESULT  关机中
  18: '', // GET_SNAP_SWITCHED_STATUS  创建中
  21: 'danger', // INSTANCE_CREATE_FAILED  创建失败
  22: 'danger', // INSTANCE_REMOVE_FAILED  删除失败
  23: 'danger', // INSTANCE_INJECT_BOOT_FAILED  创建失败
  24: 'danger', // INSTANCE_EJECT_FAILED  创建失败
  25: 'danger', // SNAP_CREATE_FAILED  创建失败
  26: 'danger', // SNAP_REMOVE_FAILED  删除失败
  27: 'danger', // SNAP_SWITCH_FAILED  快照切换失败
  28: 'danger', // INSTANCE_MIGRATE_FAILED 迁移失败
  29: 'success', // INSTANCE_MIGRATE_CLEAN 运行中
  30: '', // SNAP_INIT  创建中
  31: '', // SNAP_CREATING  创建中
  32: 'success', // SNAP_CREATED  创建完成
  33: 'warning', // SNAP_SWITCHING  回滚中
  34: 'success', // SNAP_SWITCHED  回滚成功
  35: 'warning', // SNAP_REMOVING  删除中
  40: 'success', // HYPERVISOR_NODE_CREATED  已添加
  41: '', // HYPERVISOR_NODE_CHECKING  添加中
  42: '', // HYPERVISOR_NODE_OFFLINE 已离线
  60: 'warning', //  关机中
  61: 'warning', // INSTANCE_POWERING_OFF_DETACH_PCI  正在卸载GPU---关机中
  62: 'warning', // GET_INSTANCE_POWERING_OFF_DETACH_PCI_STATUS  卸载GPU---关机中
  63: 'warning', // INSTANCE_POWERED_OFF_DETACH_PCI  已卸载GPU---已关机
  64: '', // INSTANCE_POWERING_ON_PREPARE_PCI  准备挂载GPU---开机中
  65: '', // INSTANCE_POWERING_ON_ATTACH_PCI  正在挂载GPU---开机中
  66: 'danger', // INSTANCE_POWER_ON_FAILED  开机失败
  70: '', // PORT_CREATE 创建中
  101: 'warning', // INSTANCE_REMOVING  删除中
  103: 'danger', // INSTANCE_REMOVED_FAILED  删除失败
  104: '', // INSTANCE_EJECTING  创建中
  105: 'success', // INSTANCE_EJECTED  创建成功
  110: 'danger', // INSTANCE_CREATE_FAILED_CLEANING
  111: 'danger', // INSTANCE_CREATE_FAILED_CLEANED
  300: '', // INSTANCE_MIGRATE_INIT
  301: '', // INSTANCE_SUSPENDING
  302: '', // INSTANCE_SUSPENDED
  303: '', // GET_PORT_MIGRATED_STATUS
  304: '', // GET_INSTANCE_RESUME_STATUS
  305: '', // INSTANCE_RESUMED
  350: '', // INSTANCE_RESIZE_INIT //更新中
  351: '', // GET_INSTANCE_UPDATED_STATUS //更新中
  352: '', // INSTANCE_BOOT_DEV_SWITCHING //启动项切换中
  353: '', // GET_INSTANCE_BOOT_DEV_STATUS //启动项切换中
  360: '', // INSTANCE_RESET_PASSWORD_HOSTNAME
  361: '', // WAIT_INSTANCE_RESET_PASSWORD_HOSTNAME
};
const showVmStatusMessage = (code: number | string, type: any): string => {
  if (type == 'tag') {
    return codeMessageTagMap[JSON.stringify(code)] != undefined ? codeMessageTagMap[JSON.stringify(code)] : 'info';
  }
  return codeMessageMap[JSON.stringify(code)] || i18n.global.t('utils.status.unknown');
};

export default showVmStatusMessage;
