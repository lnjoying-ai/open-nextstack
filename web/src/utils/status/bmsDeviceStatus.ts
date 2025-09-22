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
  11: string;
  12: string;
  20: string;
  21: string;
  22: string;
  23: string;
  24: string;
  [key: string]: string;
}

const codeMessageMap: codeMessageMapTypes = {
  0: '初始化', // DEVICE_INIT
  1: '添加中', // DEVICE_CRETATING
  2: '添加中', // DEVICE_CREATED_SUCCESS
  3: '添加失败', // DEVICE_CREATED_FAILED
  4: '删除中', // DEVICE_REMOVING
  5: '删除中', // DEVICE_REMOVING_CHECK
  6: '已删除', // DEVICE_REMOVED_SUCCESS
  7: '删除失败', // DEVICE_REMOVED_FAILED
  8: '设备自检', // DEVICE_INSPECTING
  9: '设备自检', // DEVICE_INSPECTING_CHECK
  11: '自检失败', // DEVICE_INSPECTED_FAILED
  12: '可用', // DEVICE_INSPECTED_SUCCESS //自检成功
  20: '网卡初始化', // DEVICE_NIC_INIT
  21: '', // DEVICE_NIC_SLOT_CREATING
  22: '', // DEVICE_NIC_SLOT_CREATING_CHECK
  23: '', // DEVICE_NIC_SLOT_CREATED_SUCCESS
  24: '', // DEVICE_NIC_SLOT_CREATED_FAILED
};
const codeMessageTagMap: codeMessageMapTypes = {
  0: '', // DEVICE_INIT 初始化
  1: '', // DEVICE_CRETATING 添加中
  2: '', // DEVICE_CREATED_SUCCESS 添加中
  3: 'danger', // DEVICE_CREATED_FAILED 添加失败
  4: 'warning', // DEVICE_REMOVING 删除中
  5: 'warning', // DEVICE_REMOVING_CHECK 删除中
  6: 'danger', // DEVICE_REMOVED_SUCCESS 已删除
  7: 'danger', // DEVICE_REMOVED_FAILED 删除失败
  8: 'warning', // DEVICE_INSPECTING 设备自检
  9: 'warning', // DEVICE_INSPECTING_CHECK 设备自检
  11: 'danger', // DEVICE_INSPECTED_FAILED 自检失败
  12: 'success', // DEVICE_INSPECTED_SUCCESS 可用 //自检成功
  20: '', // DEVICE_NIC_INIT 网卡初始化
  21: '', // DEVICE_NIC_SLOT_CREATING
  22: '', // DEVICE_NIC_SLOT_CREATING_CHECK
  23: '', // DEVICE_NIC_SLOT_CREATED_SUCCESS
  24: '', // DEVICE_NIC_SLOT_CREATED_FAILED
};
const showBmsDeviceStatusMessage = (code: number | string, type: any): string => {
  if (type == 'tag') {
    return codeMessageTagMap[JSON.stringify(code)] != undefined ? codeMessageTagMap[JSON.stringify(code)] : 'info';
  }
  return codeMessageMap[JSON.stringify(code)] || '未知状态';
};

export default showBmsDeviceStatusMessage;
