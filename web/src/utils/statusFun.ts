import showStatusMessage from '@/utils/status/status';
import showBmsStatusMessage from '@/utils/status/bmsStatus';
import showBmsDeviceStatusMessage from '@/utils/status/bmsDeviceStatus';
import showVmStatusMessage from '@/utils/status/vmStatus';
import showPciStatusMessage from '@/utils/status/pciStatus';
import showVolumeStatusMessage from '@/utils/status/volumeStatus';
import showNatStatusMessage from '@/utils/status/natStatus';
import showSecGroupsStatusMessage from '@/utils/status/secGroupsStatus';
import showVmToEipStatusMessage from '@/utils/status/eipStatus';

const filtersFun = {
  // VPC和子网状态
  getStatus(code: any, type: any): any {
    return showStatusMessage(code, type);
  },

  // 裸金属实例状态
  getBmsStatus(code: any, type: any): any {
    return showBmsStatusMessage(code, type);
  },

  getBmsDeviceStatus(code: any, type: any): any {
    return showBmsDeviceStatusMessage(code, type);
  },

  // 虚拟机和快照状态
  getVmStatus(code: any, type: any): any {
    return showVmStatusMessage(code, type);
  },

  getPciStatus(code: any, type: any): any {
    return showPciStatusMessage(code, type);
  },

  getVolumeStatus(code: any, type: any): any {
    return showVolumeStatusMessage(code, type);
  },

  getNatStatus(code: any, type: any): any {
    return showNatStatusMessage(code, type);
  },

  getSecGroupsStatus(code: any, type: any): any {
    return showSecGroupsStatusMessage(code, type);
  },

  // EIP状态
  getVmToEipStatus(code: any, type: any): any {
    return showVmToEipStatusMessage(code, type);
  },
};

export default filtersFun;
