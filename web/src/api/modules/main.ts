import service from '@/api/http';

const version = import.meta.env.VITE_API_BASEVERSION;

const mainApi: any = {
  // 用户信息
  infoList: (params: object) => service.get(`/api/ums/${version}/users/current`, params),
  // 修改密码
  modifyPassword: (params: object) => service.patch(`/api/ums/v1/users/password`, params),

  // 裸金属实例相关接口 ***保留项，暂未启用***
  bmsAdd: (params: object) => service.post(`/api/bm/${version}/baremetal_instances`, params),
  bmsDel: (id: string) => service.delete(`/api/bm/${version}/baremetal_instances/${id}`),
  bmsList: (params: object) => service.get(`/api/bm/${version}/baremetal_instances`, params),
  bmsDetail: (id: string) => service.get(`/api/bm/${version}/baremetal_instances/${id}`),
  bmsPoweroff: (id: string) => service.post(`/api/bm/${version}/baremetal_instances/${id}/poweroff`),
  bmsPoweron: (id: string) => service.post(`/api/bm/${version}/baremetal_instances/${id}/poweron`),
  bmsReboot: (id: string) => service.post(`/api/bm/${version}/baremetal_instances/${id}/reboot`),
  bmsEditPwd: (params: object, id: string) => service.put(`/api/bm/${version}/baremetal_instances/${id}/reset`, params),
  bmsEdit: (params: object, id: string) => service.put(`/api/bm/${version}/baremetal_instances/${id}`, params),

  // 裸金属设备相关接口 ***保留项，暂未启用***
  bmsDevListUsable: (params: object) => service.get(`/api/bm/${version}/baremetal_available_devices`, params),
  bmsDevList: (params: object) => service.get(`/api/bm/${version}/baremetal_devices`, params),
  bmsDevAdd: (params: object) => service.post(`/api/bm/${version}/baremetal_devices`, params),
  bmsDevDel: (id: string) => service.delete(`/api/bm/${version}/baremetal_devices/${id}`),
  bmsDevDetail: (id: string) => service.get(`/api/bm/${version}/baremetal_devices/${id}`),
  bmsDevEdit: (params: object, id: string) => service.put(`/api/bm/${version}/baremetal_devices/${id}`, params),
  bmsDevConfig: (params: object) => service.get(`/api/bm/${version}/baremetal_devices_configs`, params),

  // VPC相关接口
  vpcAdd: (params: object) => service.post(`/api/network/${version}/vpcs`, params),
  vpcEdit: (params: object, id: string) => service.put(`/api/network/${version}/vpcs/${id}`, params),
  vpcDel: (id: string) => service.delete(`/api/network/${version}/vpcs/${id}`),
  vpcList: (params: object) => service.get(`/api/network/${version}/vpcs`, params),
  vpcDetail: (id: string) => service.get(`/api/network/${version}/vpcs/${id}`),
  subnetInVpc: (params: object) => service.get(`/api/network/${version}/subnets`, params),

  // 子网相关接口
  subnetsAdd: (params: object) => service.post(`/api/network/${version}/subnets`, params),
  subnetsEdit: (params: object, id: string) => service.put(`/api/network/${version}/subnets/${id}`, params),
  subnetsDel: (id: string) => service.delete(`/api/network/${version}/subnets/${id}`),
  subnetsList: (params: object) => service.get(`/api/network/${version}/subnets`, params),
  subnetsDetail: (id: string) => service.get(`/api/network/${version}/subnets/${id}`),

  // 端口映射相关接口
  portMapsAdd: (params: object) => service.post(`/api/network/${version}/portMaps`, params),
  portMapsEdit: (params: object, id: string) => service.put(`/api/network/${version}/nat-gateways/${id}`, params),
  portMapsDel: (id: string) => service.delete(`/api/network/${version}/portMaps/${id}`),
  portMapsList: (params: object) => service.get(`/api/network/${version}/portMaps`, params),
  portMapsDetail: (id: string) => service.get(`/api/network/${version}/portMaps/${id}`),

  // 镜像相关接口
  imageList: (params: object) => service.get(`/api/repo/${version}/images`, params),
  imageDetail: (id: string) => service.get(`/api/repo/${version}/images/${id}`),

  // 密钥对相关接口
  pubkeysList: (params: object) => service.get(`/api/vm/${version}/pubkeys`, params),
  pubkeysAdd: (params: object) => service.post(`/api/vm/${version}/pubkeys`, params),
  pubkeysModify: (params: object, id: string) => service.put(`/api/vm/${version}/pubkeys/${id}`, params),
  pubkeysUpload: (params: object) => service.post(`/api/vm/${version}/pubkeys/upload`, params),
  pubkeysDel: (id: string) => service.delete(`/api/vm/${version}/pubkeys/${id}`),
  pubkeysDetail: (id: string) => service.get(`/api/vm/${version}/pubkeys/${id}`),

  // EIP相关接口
  eipsList: (params: object) => service.get(`/api/network/${version}/eips`, params),
  eipsAdd: (params: object) => service.post(`/api/network/${version}/eips`, params),
  eipsAllocate: (id: string) => service.post(`/api/network/${version}/eips/allocate/${id}`),
  eipsDel: (id: string) => service.delete(`/api/network/${version}/eips/${id}`),
  eipsAttach: (id: string, params: object) => service.put(`/api/network/${version}/eips/${id}/attach`, params),
  eipsDetach: (id: string) => service.put(`/api/network/${version}/eips/${id}/detach`),

  // EIP池相关接口
  eipPoolsList: (params: object) => service.get(`/api/network/${version}/eip_pools`, params),
  eipPoolsAdd: (params: object) => service.post(`/api/network/${version}/eip_pools`, params),
  eipPoolsDel: (id: string) => service.delete(`/api/network/${version}/eip_pools/${id}`),
  eipPoolsDetail: (id: string) => service.get(`/api/network/${version}/eip_pools/${id}`),
  eipPoolsModify: (params: object, id: string) => service.put(`/api/network/${version}/eip_pools/${id}`, params),

  // 端口映射相关接口
  portMapList: (params: object) => service.get(`/api/network/${version}/portMaps`, params),
  portMapAdd: (params: object) => service.post(`/api/network/${version}/portMaps`, params),
  portMapDel: (id: string) => service.delete(`/api/network/${version}/portMaps/${id}`),
  portMapDetail: (id: string) => service.get(`/api/network/${version}/portMaps/${id}`),

  topology: (id: string) => service.get(`/api/network/${version}/topology/${id}`),
  portsCheck: (params: object, id: string) => service.get(`/api/network/${version}/eips/${id}/ports`, params),

  // 快照相关接口
  snapsList: (params: object) => service.get(`/api/vm/${version}/snaps`, params),
  snapsAdd: (params: object) => service.post(`/api/vm/${version}/snaps`, params),
  snapsDel: (id: string) => service.delete(`/api/vm/${version}/snaps/${id}`),
  snapsDetail: (id: string) => service.get(`/api/vm/${version}/snaps/${id}`),
  snapsEdit: (params: object, id: string) => service.put(`/api/vm/${version}/snaps/${id}`, params),
  snapsSwitch: (id: string) => service.put(`/api/vm/${version}/snaps/${id}/switch`),

  // 规格相关接口
  flavorsList: (params: object) => service.get(`/api/repo/${version}/flavors`, params),
  flavorsAdd: (params: object) => service.post(`/api/repo/${version}/flavors`, params),
  flavorsDel: (id: string) => service.delete(`/api/repo/${version}/flavors/${id}`),
  flavorsDetail: (id: string) => service.get(`/api/repo/${version}/flavors/${id}`),
  flavorsEdit: (params: object, id: string) => service.put(`/api/repo/${version}/flavors/${id}`, params),
  flavorsMaxInfo: () => service.get(`/api/repo/${version}/flavors/max_info`),
  flavorGPUInfo: () => service.get(`/api/repo/${version}/flavor/gpus`),

  // 计算节点相关接口
  vmsHypervisorNodesList: (params: object) => service.get(`/api/vm/${version}/hypervisor_nodes`, params),
  vmsHypervisorNodesAdd: (params: object) => service.post(`/api/vm/${version}/hypervisor_nodes`, params),
  vmsHypervisorNodesDel: (id: string) => service.delete(`/api/vm/${version}/hypervisor_nodes/${id}`),
  vmsHypervisorNodesDetail: (id: string) => service.get(`/api/vm/${version}/hypervisor_nodes/${id}`),
  vmsHypervisorNodesEdit: (params: object, id: string) =>
    service.put(`/api/vm/${version}/hypervisor_nodes/${id}`, params),
  vmsHypervisorNodesAllocation: (params: object) =>
    service.get(`/api/vm/${version}/hypervisor_nodes/allocation`, params),

  // 虚拟机相关接口
  vmsToVolumesList: (params: object, id: string) => service.put(`/api/vm/${version}/instances/${id}/volumes`, params),
  vmsInstabcesList: (params: object) => service.get(`/api/vm/${version}/instances`, params),
  vmsInstabcesAdd: (params: object) => service.post(`/api/vm/${version}/instances`, params),
  vmsInstabcesAddCount: (params: object) => service.post(`/api/vm/${version}/instances/counts`, params),
  vmsInstabcesPoweron: (id: string) => service.put(`/api/vm/${version}/instances/${id}/poweron`),
  vmsInstabcesReboot: (id: string) => service.put(`/api/vm/${version}/instances/${id}/reboot`),
  vmsInstabcesPoweroff: (id: string) => service.put(`/api/vm/${version}/instances/${id}/poweroff`),
  vmsInstabcesDetachmentPoweroff: (id: string) =>
    service.put(`/api/vm/${version}/instances/${id}/poweroff?detachment=true`),
  vmsInstabcesDel: (id: string) => service.delete(`/api/vm/${version}/instances/${id}`),
  vmsInstabcesDetail: (id: string) => service.get(`/api/vm/${version}/instances/${id}`),
  vmsInstabcesBoundSg: (params: object, id: string) =>
    service.post(`/api/vm/${version}/instances/${id}/bound_sgs`, params),
  vmsInstabcesBoundSgSort: (params: object, id: string) =>
    service.put(`/api/vm/${version}/instances/${id}/bound_sgs`, params),
  vmsInstabcesEdit: (params: object, id: string) => service.put(`/api/vm/${version}/instances/${id}`, params),
  vmsMigratet: (params: object, id: string) => service.put(`/api/vm/${version}/instances/${id}/migrate`, params),
  vmsRenews: (params: object) => service.post(`/api/vm/${version}/instances/renews`, params),
  vmsResetPassword: (params: object, id: string) => service.put(`/api/vm/${version}/instances/${id}/reset`, params),

  // 虚机组相关接口
  vmInstanceGroupsList: (params: object) => service.get(`/api/vm/${version}/instance-groups`, params),
  vmInstanceGroupsAdd: (params: object) => service.post(`/api/vm/${version}/instance-groups`, params),
  vmInstanceGroupsDel: (id: string) => service.delete(`/api/vm/${version}/instance-groups/${id}`),
  vmInstanceGroupsDelVm: (groupId: string, id: string) =>
    service.delete(`/api/vm/${version}/instance-groups/${groupId}/${id}`),
  vmInstanceGroupsDetail: (id: string) => service.get(`/api/vm/${version}/instance-groups/${id}`),
  vmInstanceGroupsEdit: (params: object, id: string) => service.put(`/api/vm/${version}/instance-groups/${id}`, params),

  // 安全组相关接口
  sgsList: (params: object) => service.get(`/api/network/${version}/sgs`, params),
  sgsAdd: (params: object) => service.post(`/api/network/${version}/sgs`, params),
  sgsDel: (id: string) => service.delete(`/api/network/${version}/sgs/${id}`),
  sgsDetail: (id: string) => service.get(`/api/network/${version}/sgs/${id}`),
  sgsEdit: (params: object, id: string) => service.put(`/api/network/${version}/sgs/${id}`, params),
  sgsRuleAdd: (params: object, id: string) => service.post(`/api/network/${version}/sgs/${id}/rules`, params),
  sgsRulesDel: (id: string) => service.delete(`/api/network/${version}/rules/${id}`),
  sgsRulesEdit: (params: object, sgId: object, ruleId: string) =>
    service.put(`/api/network/${version}/sgs/${sgId}/rules/${ruleId}`, params),
  sgsBoundAdd: (params: object, id: string) => service.post(`/api/network/${version}/sgs/${id}/bound`, params),
  sgsUnboundAdd: (params: object, id: string) => service.post(`/api/network/${version}/sgs/${id}/unbound`, params),

  // 监控相关接口
  computePanels: (params: object) => service.get(`/api/vm/${version}/monitor/compute_panels`, params),
  vmPanels: (params: object) => service.get(`/api/vm/${version}/monitor/vm_panels`, params),
  gpuPanels: (params: object) => service.get(`/api/vm/${version}/monitor/gpu_panels`, params),
  vmsResourceStats: (params: object) => service.get(`/api/vm/${version}/resource_stats`, params),
  vmsUserStorageStats: (params: object) => service.get(`/api/vm/${version}/user_storage_stats`, params),
  vmsAllStorageStats: (params: object) => service.get(`/api/vm/${version}/all_storage_stats`, params),
  vmsVmStats: (params: object) => service.get(`/api/vm/${version}/vm_stats`, params),
  networkNetStats: (params: object) => service.get(`/api/network/${version}/net_stats`, params),
  networkVpcCount: (params: object) => service.get(`/api/network/${version}/vpc_count`, params),
  networkSubnetCount: (params: object) => service.get(`/api/network/${version}/subnet_count`, params),
  vmsMemStats: (params: object) => service.get(`/api/vm/${version}/mem_stats`, params),
  vmsCpuStats: (params: object) => service.get(`/api/vm/${version}/cpu_stats`, params),
  vmsVmCount: (params: object) => service.get(`/api/vm/${version}/vm_count`, params),

  // 云盘相关接口
  volumesList: (params: object) => service.get(`/api/repo/${version}/volumes`, params),
  volumesRecycleList: (params: object) => service.get(`/api/repo/${version}/volumes/recycle`, params),
  volumesAdd: (params: object) => service.post(`/api/repo/${version}/volumes`, params),
  volumesDel: (id: string) => service.delete(`/api/repo/${version}/volumes/${id}`),
  volumesDetail: (id: string) => service.get(`/api/repo/${version}/volumes/${id}`),
  volumesEdit: (params: object, id: string) => service.put(`/api/repo/${version}/volumes/${id}`, params),
  volumesAttach: (params: object, id: string) => service.put(`/api/repo/${version}/volumes/${id}/attach`, params),
  volumesDetach: (id: string) => service.put(`/api/repo/${version}/volumes/${id}/detach`),
  volumesExport: (params: object, id: string) => service.put(`/api/repo/${version}/volumes/${id}/export`, params),
  volumesSnapsList: (params: object, id: string) => service.get(`/api/repo/${version}/volume_snaps`, params),
  volumesSnapsAdd: (params: object, id: string) => service.post(`/api/repo/${version}/volume_snaps`, params),
  volumesSnapsDel: (id: string) => service.delete(`/api/repo/${version}/volume_snaps/${id}`),
  volumesSnapsDetail: (id: string) => service.get(`/api/repo/${version}/volume_snaps/${id}`),
  volumesSnapsEdit: (params: object, id: string) => service.put(`/api/repo/${version}/volume_snaps/${id}`, params),
  volumesSnapsSwitch: (id: string) => service.put(`/api/repo/${version}/volume_snaps/${id}/switch`),

  // 存储池相关接口
  storagePoolsList: (params: object) => service.get(`/api/repo/${version}/storage_pools`, params),
  storagePoolsEdit: (params: object, id: string) => service.put(`/api/repo/${version}/storage_pools/${id}`, params),
  storagePoolsDetail: (id: string) => service.get(`/api/repo/${version}/storage_pools/${id}`),

  // GPU相关接口
  pciNodeList: (params: object) => service.get(`/api/vm/${version}/pci_devices/available_nodes`, params),
  pciDeviceList: (params: object) => service.get(`/api/vm/${version}/pci_devices`, params),
  pciAvailableList: (params: object) => service.get(`/api/vm/${version}/pci_devices/available_devices`, params),
  pciAttach: (id: string, params: object) => service.put(`/api/vm/${version}/pci_devices/${id}/attach`, params),
  pciDetach: (id: string, params: object) => service.put(`/api/vm/${version}/pci_devices/${id}/detach`, params),

  // 通知对象相关接口
  operationReceivers: (params: object) => service.get(`/api/operation/${version}/receivers`, params),
  operationReceiversAdd: (params: object) => service.post(`/api/operation/${version}/receivers`, params),
  operationReceiversDel: (id: string) => service.delete(`/api/operation/${version}/receivers/${id}`),
  operationReceiversDetail: (id: string) => service.get(`/api/operation/${version}/receivers/${id}`),
  operationReceiversEdit: (params: object, id: string) =>
    service.put(`/api/operation/${version}/receivers/${id}`, params),

  // 报警器相关接口
  operationAlarmRules: (params: object) => service.get(`/api/operation/${version}/alarm-rules`, params),
  operationAlarmRulesAdd: (params: object) => service.post(`/api/operation/${version}/alarm-rules`, params),
  operationAlarmRulesDel: (id: string) => service.delete(`/api/operation/${version}/alarm-rules/${id}`),
  operationAlarmRulesDetail: (id: string) => service.get(`/api/operation/${version}/alarm-rules/${id}`),
  operationAlarmRulesEdit: (params: object, id: string) =>
    service.put(`/api/operation/${version}/alarm-rules/${id}`, params),

  // 运维管理相关接口
  operationAlarmInfosList: (params: object) => service.get(`/api/operation/${version}/alarm-infos`, params),
  operationAlarmMarkResolvedEdit: (params: object) =>
    service.put(`/api/operation/${version}/alarm-mark-resolved`, params),
  operationAlarmDistribution: (params: object) => service.get(`/api/operation/${version}/alarm-distribution`, params),
  operationAlarmStatistics: (params: object) => service.get(`/api/operation/${version}/alarm-statistics`, params),
  operationEventsList: (params: object) => service.get(`/api/operation/${version}/events`, params),
  operationLogsList: (params: object) => service.get(`/api/operation/${version}/logs`, params),
};

export default mainApi;
