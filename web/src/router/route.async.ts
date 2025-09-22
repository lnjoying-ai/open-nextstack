import { RouteRecordRaw } from 'vue-router';
import layout from '../layout/index.vue';

import vm from '@/views/compute/vm/index.vue';
import vmCommand from '@/views/compute/vm/command.vue';
import vmAdd from '@/views/compute/vm/add.vue';
import vmDetail from '@/views/compute/vm/detail.vue';
import vmEdit from '@/views/compute/vm/edit.vue';

import vmGroup from '@/views/compute/vmGroup/index.vue';
import vmGroupAdd from '@/views/compute/vmGroup/add.vue';
import vmGroupDetail from '@/views/compute/vmGroup/detail.vue';
import vmGroupEdit from '@/views/compute/vmGroup/edit.vue';

import hypervisorNodes from '@/views/hardware/hypervisorNodes/index.vue';
import hypervisorNodesAdd from '@/views/hardware/hypervisorNodes/add.vue';
import hypervisorNodesDetail from '@/views/hardware/hypervisorNodes/detail.vue';
import hypervisorNodesEdit from '@/views/hardware/hypervisorNodes/edit.vue';

import storagePool from '@/views/hardware/storagePool/index.vue';
import storagePoolAdd from '@/views/hardware/storagePool/add.vue';
import storagePoolDetail from '@/views/hardware/storagePool/detail.vue';
import storagePoolEdit from '@/views/hardware/storagePool/edit.vue';

import flavors from '@/views/compute/flavors/index.vue';
import flavorsAdd from '@/views/compute/flavors/add.vue';
import flavorsDetail from '@/views/compute/flavors/detail.vue';
import snaps from '@/views/compute/snaps/index.vue';
import snapsAdd from '@/views/compute/snaps/add.vue';
import snapsDetail from '@/views/compute/snaps/detail.vue';
import vpc from '@/views/network/vpc/index.vue';
import vpcAdd from '@/views/network/vpc/add.vue';
import vpcDetail from '@/views/network/vpc/detail.vue';
import secGroups from '@/views/network/secGroups/index.vue';
import secGroupsAdd from '@/views/network/secGroups/add.vue';
import secGroupsDetail from '@/views/network/secGroups/detail.vue';
import subNet from '@/views/network/subNet/index.vue';
import subNetAdd from '@/views/network/subNet/add.vue';
import subNetDetail from '@/views/network/subNet/detail.vue';
import Nat from '@/views/network/Nat/index.vue';
import NatAdd from '@/views/network/Nat/add.vue';
import NatDetail from '@/views/network/Nat/detail.vue';
import publicKey from '@/views/publicKey/index.vue';
import publicKeyAdd from '@/views/publicKey/add.vue';
import publicKeyUpload from '@/views/publicKey/upload.vue';
import publicKeyDetail from '@/views/publicKey/detail.vue';
import eip from '@/views/network/eip/index.vue';
import eipAdd from '@/views/network/eip/add.vue';
import eipDetail from '@/views/network/eip/detail.vue';
import eipPool from '@/views/network/eipPool/index.vue';
import eipPoolAdd from '@/views/network/eipPool/add.vue';
import eipPoolEdit from '@/views/network/eipPool/edit.vue';
import eipPoolDetail from '@/views/network/eipPool/detail.vue';

import volumes from '@/views/storage/volumes/index.vue';
import volumesAdd from '@/views/storage/volumes/add.vue';
import volumesEdit from '@/views/storage/volumes/edit.vue';
import volumesDetail from '@/views/storage/volumes/detail.vue';

import volumesSnap from '@/views/storage/volumesSnap/index.vue';
import volumesSnapAdd from '@/views/storage/volumesSnap/add.vue';
import volumesSnapEdit from '@/views/storage/volumesSnap/edit.vue';
import volumesSnapDetail from '@/views/storage/volumesSnap/detail.vue';

import cloudDiskImage from '@/views/images/cloudDiskImage/index.vue';
import cloudDiskImageDetail from '@/views/images/cloudDiskImage/detail.vue';

import devOpsAlert from '@/views/devOps/alert/index.vue';
import devOpsAlertHome from '@/views/devOps/alert/alert.vue';
import devOpsAlertAdd from '@/views/devOps/alert/add.vue';
import devOpsAlertEdit from '@/views/devOps/alert/edit.vue';
import devOpsAlertDetail from '@/views/devOps/alert/detail.vue';
import devOpsLog from '@/views/devOps/log/index.vue';
import devOpsEvent from '@/views/devOps/event/index.vue';

import devOpsNoticeGrop from '@/views/devOps/noticeGrop/index.vue';
import devOpsNoticeGropAdd from '@/views/devOps/noticeGrop/add.vue';
import devOpsNoticeGropEdit from '@/views/devOps/noticeGrop/edit.vue';
import devOpsNoticeGropDetail from '@/views/devOps/noticeGrop/detail.vue';

const asyncRoutes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'index',
    meta: {
      title: 'router.async.index',
      hidden: true,
      isLogin: true,
      icon: 'clarity:view-list-line',
      img: 'home',
      layout: true,
    },
    component: () => import('@/views/index.vue'),
  },

  {
    path: '/compute',
    name: 'compute',
    meta: {
      title: 'router.async.compute',
      hidden: true,
      isLogin: true,
      icon: 'jam:computer-alt',
      img: 'compute',
      layout: true,
    },
    component: layout,
    children: [
      {
        path: '/vm',
        name: 'vm',
        component: vm,
        meta: {
          title: 'router.async.vm',
          hidden: true,
          isLogin: true,
          icon: 'codicon:vm',
          img: 'vm',
          layout: true,
        },
      },
      {
        path: '/vmAdd',
        name: 'vmAdd',
        component: vmAdd,
        meta: {
          title: 'router.async.vmAdd',
          hidden: false,
          isLogin: true,
          icon: 'codicon:vm',
          img: 'vm',
          layout: true,
        },
      },
      {
        path: '/vm/:id',
        name: 'vmDetail',
        component: vmDetail,
        meta: {
          title: 'router.async.vmDetail',
          hidden: false,
          isLogin: true,
          icon: 'codicon:vm',
          img: 'vm',
          layout: true,
        },
      },
      {
        path: '/vmEdit/:id',
        name: 'vmEdit',
        component: vmEdit,
        meta: {
          title: 'router.async.vmEdit',
          hidden: false,
          isLogin: true,
          icon: 'codicon:vm',
          img: 'vm',
          layout: true,
        },
      },
      {
        path: '/vmGroup',
        name: 'vmGroup',
        component: vmGroup,
        meta: {
          title: 'router.async.vmGroup',
          hidden: true,
          isLogin: true,
          icon: 'codicon:vmGroup',
          img: 'vmGroup',
          layout: true,
        },
      },
      {
        path: '/vmGroupAdd',
        name: 'vmGroupAdd',
        component: vmGroupAdd,
        meta: {
          title: 'router.async.vmGroupAdd',
          hidden: false,
          isLogin: true,
          icon: 'codicon:vmGroup',
          img: 'vmGroup',
          layout: true,
        },
      },
      {
        path: '/vmGroup/:id',
        name: 'vmGroupDetail',
        component: vmGroupDetail,
        meta: {
          title: 'router.async.vmGroupDetail',
          hidden: false,
          isLogin: true,
          icon: 'codicon:vmGroup',
          img: 'vmGroup',
          layout: true,
        },
      },
      {
        path: '/vmGroupEdit/:id',
        name: 'vmGroupEdit',
        component: vmGroupEdit,
        meta: {
          title: 'router.async.vmGroupEdit',
          hidden: false,
          isLogin: true,
          icon: 'codicon:vmGroup',
          img: 'vmGroup',
          layout: true,
        },
      },

      {
        path: '/flavors',
        name: 'flavors',
        component: flavors,
        meta: {
          title: 'router.async.flavors',
          hidden: true,
          isLogin: true,
          icon: 'eos-icons:templates',
          img: 'flavors',
          layout: true,
        },
      },
      {
        path: '/flavorsAdd',
        name: 'flavorsAdd',
        component: flavorsAdd,
        meta: {
          title: 'router.async.flavorsAdd',
          hidden: false,
          isLogin: true,
          icon: 'eos-icons:templates',
          img: 'flavors',
          layout: true,
        },
      },
      {
        path: '/flavors/:id',
        name: 'flavorsDetail',
        component: flavorsDetail,
        meta: {
          title: 'router.async.flavorsDetail',
          hidden: false,
          isLogin: true,
          icon: 'eos-icons:templates',
          img: 'flavors',
          layout: true,
        },
      },

      {
        path: '/snaps',
        name: 'snaps',
        component: snaps,
        meta: {
          title: 'router.async.snaps',
          hidden: true,
          isLogin: true,
          icon: 'material-symbols:aspect-ratio-outline',
          img: 'snaps',
          layout: true,
        },
      },
      {
        path: '/snapsAdd',
        name: 'snapsAdd',
        component: snapsAdd,
        meta: {
          title: 'router.async.snapsAdd',
          hidden: false,
          isLogin: true,
          icon: 'material-symbols:aspect-ratio-outline',
          img: 'snaps',
          layout: true,
        },
      },
      {
        path: '/snaps/:id',
        name: 'snapsDetail',
        component: snapsDetail,
        meta: {
          title: 'router.async.snapsDetail',
          hidden: false,
          isLogin: true,
          icon: 'material-symbols:aspect-ratio-outline',
          img: 'snaps',
          layout: true,
        },
      },
    ],
  },
  {
    path: '/network',
    name: 'network',
    meta: {
      title: 'router.async.network',
      hidden: true,
      isLogin: true,
      icon: 'icon-park-outline:network-tree',
      img: 'network',
      layout: true,
    },
    component: layout,
    children: [
      {
        path: '/vpc',
        name: 'vpc',
        component: vpc,
        meta: {
          title: 'router.async.vpc',
          hidden: true,
          isLogin: true,
          icon: 'carbon:load-balancer-vpc',
          img: 'vpc',
          layout: true,
        },
      },
      {
        path: '/vpcAdd',
        name: 'vpcAdd',
        component: vpcAdd,
        meta: {
          title: 'router.async.vpcAdd',
          hidden: false,
          isLogin: true,
          icon: 'carbon:load-balancer-vpc',
          img: 'vpc',
          layout: true,
        },
      },
      {
        path: '/vpc/:id',
        name: 'vpcDetail',
        component: vpcDetail,
        meta: {
          title: 'router.async.vpcDetail',
          hidden: false,
          isLogin: true,
          icon: 'carbon:load-balancer-vpc',
          img: 'vpc',
          layout: true,
        },
      },
      {
        path: '/subNet',
        name: 'subNet',
        component: subNet,
        meta: {
          title: 'router.async.subNet',
          hidden: true,
          isLogin: true,
          icon: 'carbon:ibm-cloud-subnets',
          img: 'subnet',
          layout: true,
        },
      },
      {
        path: '/subNetAdd',
        name: 'subNetAdd',
        component: subNetAdd,
        meta: {
          title: 'router.async.subNetAdd',
          hidden: false,
          isLogin: true,
          icon: 'carbon:ibm-cloud-subnets',
          img: 'subnet',
          layout: true,
        },
      },
      {
        path: '/subNet/:id',
        name: 'subNetDetail',
        component: subNetDetail,
        meta: {
          title: 'router.async.subNetDetail',
          hidden: false,
          isLogin: true,
          icon: 'carbon:ibm-cloud-subnets',
          img: 'subnet',
          layout: true,
        },
      },
      {
        path: '/secGroups',
        name: 'secGroups',
        component: secGroups,
        meta: {
          title: 'router.async.secGroups',
          hidden: true,
          isLogin: true,
          icon: 'ph:shield-checkered',
          img: 'secGroups',
          layout: true,
        },
      },
      {
        path: '/secGroupsAdd',
        name: 'secGroupsAdd',
        component: secGroupsAdd,
        meta: {
          title: 'router.async.secGroupsAdd',
          hidden: false,
          isLogin: true,
          icon: 'ph:shield-checkered',
          img: 'secGroups',
          layout: true,
        },
      },
      {
        path: '/secGroups/:id',
        name: 'secGroupsDetail',
        component: secGroupsDetail,
        meta: {
          title: 'router.async.secGroupsDetail',
          hidden: false,
          isLogin: true,
          icon: 'ph:shield-checkered',
          img: 'secGroups',
          layout: true,
        },
      },

      {
        path: '/Nat',
        name: 'Nat',
        component: Nat,
        meta: {
          title: 'router.async.nat',
          hidden: true,
          isLogin: true,
          icon: 'carbon:network-2',
          img: 'nat',
          layout: true,
        },
      },
      {
        path: '/NatAdd',
        name: 'NatAdd',
        component: NatAdd,
        meta: {
          title: 'router.async.natAdd',
          hidden: false,
          isLogin: true,
          icon: 'carbon:network-2',
          img: 'nat',
          layout: true,
        },
      },
      {
        path: '/Nat/:id',
        name: 'NatDetail',
        component: NatDetail,
        meta: {
          title: 'router.async.natDetail',
          hidden: false,
          isLogin: true,
          icon: 'carbon:network-2',
          img: 'nat',
          layout: true,
        },
      },
      {
        path: '/eip',
        name: 'EIP',
        component: eip,
        meta: {
          title: 'router.async.eip',
          hidden: true,
          isLogin: true,
          icon: 'mdi-ip-outline',
          img: 'eip',
          layout: true,
        },
      },
      {
        path: '/eipAdd',
        name: 'eipAdd',
        component: eipAdd,
        meta: {
          title: 'router.async.eipAdd',
          hidden: false,
          isLogin: true,
          icon: 'mdi-ip-outline',
          img: 'eip',
          layout: true,
        },
      },
      {
        path: '/eip/:id',
        name: 'eipDetail',
        component: eipDetail,
        meta: {
          title: 'router.async.eipDetail',
          hidden: false,
          isLogin: true,
          icon: 'mdi-ip-outline',
          img: 'eip',
          layout: true,
        },
      },
      {
        path: '/eipPool',
        name: 'EIP Pool',
        component: eipPool,
        meta: {
          title: 'router.async.eipPool',
          hidden: true,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'eipPool',
          layout: true,
        },
      },
      {
        path: '/eipPoolAdd',
        name: 'eipPoolAdd',
        component: eipPoolAdd,
        meta: {
          title: 'router.async.eipPoolAdd',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'eipPool',
          layout: true,
        },
      },
      {
        path: '/eipPoolEdit/:id',
        name: 'eipPoolEdit',
        component: eipPoolEdit,
        meta: {
          title: 'router.async.eipPoolEdit',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'eipPool',
          layout: true,
        },
      },
      {
        path: '/eipPool/:id',
        name: 'eipPoolDetail',
        component: eipPoolDetail,
        meta: {
          title: 'router.async.eipPoolDetail',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'eipPool',
          layout: true,
        },
      },
    ],
  },
  {
    path: '/storage',
    name: 'storage',
    meta: {
      title: 'router.async.storage',
      hidden: true,
      isLogin: true,
      icon: 'icon-park-outline:network-tree',
      img: 'storage',
      layout: true,
    },
    component: layout,
    children: [
      {
        path: '/volumes',
        name: 'volumes',
        component: volumes,
        meta: {
          title: 'router.async.volumes',
          hidden: true,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'volume',
          layout: true,
        },
      },
      {
        path: '/volumesAdd',
        name: 'volumesAdd',
        component: volumesAdd,
        meta: {
          title: 'router.async.volumesAdd',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'volume',
          layout: true,
        },
      },
      {
        path: '/volumesEdit/:id',
        name: 'volumesEdit',
        component: volumesEdit,
        meta: {
          title: 'router.async.volumesEdit',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'volume',
          layout: true,
        },
      },
      {
        path: '/volumes/:id',
        name: 'volumesDetail',
        component: volumesDetail,
        meta: {
          title: 'router.async.volumesDetail',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'volume',
          layout: true,
        },
      },
      {
        path: '/volumesSnap',
        name: 'volumesSnap',
        component: volumesSnap,
        meta: {
          title: 'router.async.volumesSnap',
          hidden: true,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'volumeSnap',
          layout: true,
        },
      },
      {
        path: '/volumesSnapAdd',
        name: 'volumesSnapAdd',
        component: volumesSnapAdd,
        meta: {
          title: 'router.async.volumesSnapAdd',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'volumeSnap',
          layout: true,
        },
      },
      {
        path: '/volumesSnapEdit/:id',
        name: 'volumesSnapEdit',
        component: volumesSnapEdit,
        meta: {
          title: 'router.async.volumesSnapEdit',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'volumeSnap',
          layout: true,
        },
      },
      {
        path: '/volumesSnap/:id',
        name: 'volumesSnapDetail',
        component: volumesSnapDetail,
        meta: {
          title: 'router.async.volumesSnapDetail',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'volumeSnap',
          layout: true,
        },
      },
    ],
  },
  {
    path: '/hardware',
    name: 'hardware',
    meta: {
      title: 'router.async.hardware',
      hidden: true,
      isLogin: true,
      icon: 'icon-park-outline:network-tree',
      img: 'hardware',
      layout: true,
    },
    component: layout,
    children: [
      {
        path: '/hypervisorNodes',
        name: 'hypervisorNodes',
        component: hypervisorNodes,
        meta: {
          title: 'router.async.hypervisorNodes',
          hidden: true,
          isLogin: true,
          icon: 'ri:node-tree',
          img: 'node',
          layout: true,
        },
      },
      {
        path: '/hypervisorNodesAdd',
        name: 'hypervisorNodesAdd',
        component: hypervisorNodesAdd,
        meta: {
          title: 'router.async.hypervisorNodesAdd',
          hidden: false,
          isLogin: true,
          icon: 'ri:node-tree',
          img: 'node',
          layout: true,
        },
      },
      {
        path: '/hypervisorNodesEdit/:id',
        name: 'hypervisorNodesEdit',
        component: hypervisorNodesEdit,
        meta: {
          title: 'router.async.hypervisorNodesEdit',
          hidden: false,
          isLogin: true,
          icon: 'ri:node-tree',
          img: 'node',
          layout: true,
        },
      },
      {
        path: '/hypervisorNodes/:id',
        name: 'hypervisorNodesDetail',
        component: hypervisorNodesDetail,
        meta: {
          title: 'router.async.hypervisorNodesDetail',
          hidden: false,
          isLogin: true,
          icon: 'ri:node-tree',
          img: 'node',
          layout: true,
        },
      },
      {
        path: '/storagePool',
        name: 'storagePool',
        component: storagePool,
        meta: {
          title: 'router.async.storagePool',
          hidden: true,
          isLogin: true,
          icon: 'ri:node-tree',
          img: 'storagePool',
          layout: true,
        },
      },
      {
        path: '/storagePoolAdd',
        name: 'storagePoolAdd',
        component: storagePoolAdd,
        meta: {
          title: 'router.async.storagePoolAdd',
          hidden: false,
          isLogin: true,
          icon: 'ri:node-tree',
          img: 'storagePool',
          layout: true,
        },
      },
      {
        path: '/storagePoolEdit/:id',
        name: 'storagePoolEdit',
        component: storagePoolEdit,
        meta: {
          title: 'router.async.storagePoolEdit',
          hidden: false,
          isLogin: true,
          icon: 'ri:node-tree',
          img: 'storagePool',
          layout: true,
        },
      },
      {
        path: '/storagePool/:id',
        name: 'storagePoolDetail',
        component: storagePoolDetail,
        meta: {
          title: 'router.async.storagePoolDetail',
          hidden: false,
          isLogin: true,
          icon: 'ri:node-tree',
          img: 'storagePool',
          layout: true,
        },
      },
    ],
  },
  {
    path: '/images',
    name: 'images',
    meta: {
      title: 'router.async.images',
      hidden: true,
      isLogin: true,
      icon: 'icon-park-outline:network-tree',
      img: 'images',
      layout: true,
    },
    component: layout,
    children: [
      {
        path: '/cloudDiskImage',
        name: 'cloudDiskImage',
        component: cloudDiskImage,
        meta: {
          title: 'router.async.cloudDiskImage',
          hidden: true,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'images',
          layout: true,
        },
      },

      {
        path: '/cloudDiskImage/:id',
        name: 'cloudDiskImageDetail',
        component: cloudDiskImageDetail,
        meta: {
          title: 'router.async.cloudDiskImageDetail',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'images',
          layout: true,
        },
      },
    ],
  },
  {
    path: '/devOps',
    name: 'devOps',
    meta: {
      title: 'router.async.devOps',
      hidden: true,
      isLogin: true,
      icon: 'icon-park-outline:network-tree',
      img: 'devOps',
      layout: true,
    },
    component: layout,
    children: [
      {
        path: '/statistics',
        name: 'statistics',
        meta: {
          title: 'router.async.statistics',
          hidden: true,
          isLogin: true,
          icon: 'clarity:view-list-line',
          img: 'statistics',
          layout: true,
        },
        component: () => import('@/views/devOps/statistics.vue'),
      },
      {
        path: '/devOps/alertHome',
        name: 'alertHome',
        component: devOpsAlertHome,
        meta: {
          title: 'router.async.devOpsData.alertHome',
          hidden: true,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'alert',
          layout: true,
        },
      },
      {
        path: '/devOps/alert',
        name: 'alert',
        component: devOpsAlert,
        meta: {
          title: 'router.async.devOpsData.alert',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'alert',
          layout: true,
        },
      },
      {
        path: '/devOps/alertAdd',
        name: 'devOpsAlertAdd',
        component: devOpsAlertAdd,
        meta: {
          title: 'router.async.devOpsData.devOpsAlertAdd',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'alert',
          layout: true,
        },
      },
      {
        path: '/devOps/alertEdit/:id',
        name: 'devOpsAlertEdit',
        component: devOpsAlertEdit,
        meta: {
          title: 'router.async.devOpsData.devOpsAlertEdit',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'alert',
          layout: true,
        },
      },
      {
        path: '/devOps/alert/:id',
        name: 'devOpsAlertDetail',
        component: devOpsAlertDetail,
        meta: {
          title: 'router.async.devOpsData.devOpsAlertDetail',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'dataBase',
          layout: true,
        },
      },
      {
        path: '/devOps/noticeGrop',
        name: 'noticeGrop',
        component: devOpsNoticeGrop,
        meta: {
          title: 'router.async.devOpsData.noticeGrop',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'dataBase',
          layout: true,
        },
      },
      {
        path: '/devOps/noticeGropAdd',
        name: 'devOpsNoticeGropAdd',
        component: devOpsNoticeGropAdd,
        meta: {
          title: 'router.async.devOpsData.devOpsNoticeGropAdd',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'dataBase',
          layout: true,
        },
      },
      {
        path: '/devOps/noticeGropEdit/:id',
        name: 'devOpsNoticeGropEdit',
        component: devOpsNoticeGropEdit,
        meta: {
          title: 'router.async.devOpsData.devOpsNoticeGropEdit',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'dataBase',
          layout: true,
        },
      },
      {
        path: '/devOps/noticeGrop/:id',
        name: 'devOpsNoticeGropDetail',
        component: devOpsNoticeGropDetail,
        meta: {
          title: 'router.async.devOpsData.devOpsNoticeGropDetail',
          hidden: false,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'dataBase',
          layout: true,
        },
      },
      {
        path: '/devOps/log',
        name: 'log',
        component: devOpsLog,
        meta: {
          title: 'router.async.devOpsData.log',
          hidden: true,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'log',
          layout: true,
        },
      },
      {
        path: '/devOps/event',
        name: 'event',
        component: devOpsEvent,
        meta: {
          title: 'router.async.devOpsData.event',
          hidden: true,
          isLogin: true,
          icon: 'carbon-load-balancer-pool',
          img: 'event',
          layout: true,
        },
      },
    ],
  },
  {
    path: '/publicKey',
    name: 'publicKey',
    meta: {
      title: 'router.async.publicKey',
      hidden: true,
      isLogin: true,
      icon: 'ic:outline-vpn-key',
      img: 'publicKey',
      layout: true,
    },
    component: publicKey,
  },
  {
    path: '/publicKeyAdd',
    name: 'publicKeyAdd',
    meta: {
      title: 'router.async.publicKeyAdd',
      hidden: false,
      isLogin: true,
      icon: 'ic:outline-vpn-key',
      img: 'publicKey',
      layout: true,
    },
    component: publicKeyAdd,
  },
  {
    path: '/publicKeyUpload',
    name: 'publicKeyUpload',
    meta: {
      title: 'router.async.publicKeyUpload',
      hidden: false,
      isLogin: true,
      icon: 'ic:outline-vpn-key',
      img: 'publicKey',
      layout: true,
    },
    component: publicKeyUpload,
  },
  {
    path: '/publicKey/:id',
    name: 'publicKeyDetail',
    meta: {
      title: 'router.async.publicKeyDetail',
      hidden: false,
      isLogin: true,
      icon: 'ic:outline-vpn-key',
      img: 'publicKey',
      layout: true,
    },
    component: publicKeyDetail,
  },
  {
    path: '/vmCommand',
    name: 'vmCommand',
    component: vmCommand,
    meta: {
      title: 'router.async.vmCommand',
      hidden: false,
      isLogin: true,
      icon: 'material-symbols:aspect-ratio-outline',
      layout: false,
    },
  },
];

export default asyncRoutes;
