<template>
  <div class="commandPage bg-black h-full">
    <div class="text-white text-base w-full mx-auto block border-b border-gray-500 border-solid">
      <div class="relative leading-5 m-auto text-center" style="width: 1280px">
        <div class="block overflow-hidden text-white bg-black pt-1 pb-1">
          <span>
            {{ router.currentRoute.value.query.instanceName }}
            <el-tag v-if="vmDetailData" size="small" :type="filtersFun.getVmStatus(vmDetailData.phaseStatus, 'tag')">{{
              filtersFun.getVmStatus(vmDetailData.phaseStatus, 'status')
            }}</el-tag>
          </span>
          <div class="float-left text-sm">
            <el-tooltip v-if="nowIsoMsg" class="box-item" effect="dark" placement="bottom">
              <template #content> <span v-html="isoMsg"></span> </template>
              <span class="mr-2">{{ $t('compute.vm.info.status') }}:{{ nowIsoMsg }}</span>
            </el-tooltip>
            <el-tooltip
              v-if="isoFile && isoFile.name"
              class="box-item"
              effect="dark"
              :content="isoFile.name"
              placement="bottom"
            >
              <span style="width: 300px; vertical-align: text-top" class="mr-2 inline-block truncate"
                >{{ $t('compute.vm.info.fileName') }}:{{ isoFile.name }}</span
              >
            </el-tooltip>

            <el-button size="small" @click="dialogVisible = true"> {{ $t('compute.vm.operation.mount') }} </el-button>
            <el-button size="small" class="mr-2" @click="stop_server()">
              {{ $t('compute.vm.operation.cancel') }}
            </el-button>
          </div>
          <div class="float-right w-115 text-right">
            <el-button
              v-if="router.currentRoute.value.query.type != 'vnc' && rfb == null"
              size="small"
              class="mr-2"
              @click="connectVnc"
            >
              {{ $t('compute.vm.operation.showRemoteDesktop') }}
            </el-button>
            <el-button
              v-if="router.currentRoute.value.query.type != 'vnc' && rfb != null"
              size="small"
              class="mr-2"
              @click="closeVnc"
            >
              {{ $t('compute.vm.operation.closeRemoteDesktop') }}
            </el-button>
            <el-button size="small" class="mr-2 !ml-0" @click="toReboot">
              {{ $t('compute.vm.operation.reboot') }}
            </el-button>
            <el-dropdown v-if="vmDetailData" class="mr-2">
              <el-button size="small">
                {{ $t('compute.vm.info.bootDev') }}:{{
                  vmDetailData.bootDev == 'hd'
                    ? $t('compute.vm.info.hdDisk')
                    : vmDetailData.bootDev == 'cdrom'
                    ? $t('compute.vm.info.cdrom')
                    : '-'
                }}
                <el-icon class="el-icon--right">
                  <i-ic-twotone-keyboard-arrow-down></i-ic-twotone-keyboard-arrow-down>
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="vmDetailData.bootDev != 'hd'" @click="changeBootDev('hd')">{{
                    $t('compute.vm.info.changeBootDev')
                  }}</el-dropdown-item>
                  <el-dropdown-item v-if="vmDetailData.bootDev != 'cdrom'" @click="changeBootDev('cdrom')">{{
                    $t('compute.vm.info.changeBootDev2')
                  }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-dropdown>
              <el-button size="small">
                {{ $t('compute.vm.info.comboKey') }}
                <el-icon class="el-icon--right">
                  <i-ic-twotone-keyboard-arrow-down></i-ic-twotone-keyboard-arrow-down>
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="rfbunlock">Ctrl-Alt-Del</el-dropdown-item>
                  <!-- <el-dropdown-item @click="toFullScreen">全屏</el-dropdown-item> -->
                  <el-dropdown-item @click="toPaste">{{ $t('compute.vm.info.paste') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button size="small" class="ml-2 !mr-0" @click="toFullScreen">
              {{ $t('compute.vm.operation.fullScreen') }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div id="screen" style="height: calc(100% - 33px)"></div>
    <el-dialog
      v-model="dialogVisible"
      :title="$t('compute.vm.info.virtualMedia')"
      width="600px"
      :close-on-click-modal="false"
      :before-close="handleClose"
    >
      <el-row class="w-full">
        <el-col :span="24">
          <el-upload
            ref="upload"
            class="upload-demo text-center"
            :limit="1"
            :action="''"
            drag
            :on-exceed="handleExceed"
            :on-remove="handleRemove"
            :on-change="handleChange"
            :auto-upload="false"
          >
            <el-icon class="el-icon--upload"> <i-ion-cloud-upload></i-ion-cloud-upload></el-icon>

            <div class="el-upload__text">{{ $t('compute.vm.info.dragFile') }}</div>
            <template #tip>
              <div class="el-upload__tip">{{ $t('compute.vm.info.onlySupportIso') }}</div>
            </template>
          </el-upload>
        </el-col>
        <!-- <el-col :span="10">
           <el-input v-model="isoMsg"
                    placeholder="未连接"
                    type="textarea"
                    :rows="8"
                    :disabled="true" />
        </el-col> -->
      </el-row>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleClose">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" @click="start_server"> {{ $t('common.confirm') }} </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import RFB from '@novnc/novnc/core/rfb';
import { log, table } from 'console';
import mainApi from '@/api/modules/main';
import NBDServer from '@/utils/nbd.js';
import filtersFun from '@/utils/statusFun';

import mainStore from '@/store/mainStore';

const { proxy }: any = getCurrentInstance();

const mainStoreData = mainStore(); // pinia 信息

const router = useRouter();
const menuShow = ref(false);
let url: any = '';
const rfb: any = ref(null);
const getCookie = (cname: any) => {
  const name = `${cname}=`;
  const ca = document.cookie.split(';');
  for (let i = 0; i < ca.length; i++) {
    let c = ca[i];
    while (c.charAt(0) == ' ') c = c.substring(1);
    if (c.indexOf(name) != -1) return c.substring(name.length, c.length);
  }
  return '';
};
const getUrl = () => {
  let protocol = '';

  if (window.location.protocol === 'https:') {
    protocol = 'wss://';
  } else {
    protocol = 'ws://';
  }

  const wsUrl = `${protocol + window.location.host}/api/vnc/${
    router.currentRoute.value.query.instanceId
  }?token=${getCookie('Access-Token')}`;

  // const wsUrl =
  //   protocol +
  //   '192.168.1.92' +
  //   '/api/vnc/' +
  //   router.currentRoute.value.query.instanceId +
  //   '?token=' +
  //   getCookie('Access-Token');

  return wsUrl;
};
// vnc连接断开的回调函数
const disconnectedFromServer = (msg: any) => {
  // clean是boolean指示终止是否干净。在发生意外终止或错误时 clean将设置为 false。
  if (msg.detail.clean) {
    // 根据 断开信息的msg.detail.clean 来判断是否可以重新连接
    setTimeout(() => {
      rfb.value = null;
      connectVnc();
    }, 3000);
  } else {
    // 这里做不可重新连接的一些操作
  }
};
const connectedToServer = () => {};
// 关闭远程桌面
const closeVnc = () => {
  rfb.value && rfb.value.disconnect();
  rfb.value = null;
};
const connectVnc = () => {
  const PASSWORD = '';
  const rfbConnect = new RFB(document.getElementById('screen'), url, {
    // 向vnc 传递的一些参数，比如说虚拟机的开机密码等
    // credentials: { password: PASSWORD, token: '1' },
  });
  rfbConnect.addEventListener('connect', connectedToServer);
  rfbConnect.addEventListener('disconnect', disconnectedFromServer);
  // scaleViewport指示是否应在本地扩展远程会话以使其适合其容器。禁用时，如果远程会话小于其容器，则它将居中，或者根据clipViewport它是否更大来处理。默认情况下禁用。
  rfbConnect.scaleViewport = true;
  // 是一个boolean指示是否每当容器改变尺寸应被发送到调整远程会话的请求。默认情况下禁用
  rfbConnect.resizeSession = true;
  rfbConnect.qualityLevel = 9;
  rfb.value = rfbConnect;
};
const handleUnload = (e: any) => {
  // stop_server();

  e = e || window.event;
  e.returnValue = proxy.$t('compute.vm.message.confirmUnload');
  if (e) {
    e.returnValue = proxy.$t('compute.vm.message.confirmUnload');
  }
  return proxy.$t('compute.vm.message.confirmUnload');

  // rfb && rfb.disconnect();
};

// 发送 Ctrl-Alt-Del 键序列。
const rfbunlock = () => {
  rfb.value && rfb.value.sendCtrlAltDel();
};
const toFullScreen = () => {
  const screen: any = document.getElementById('screen');
  // 兼容不同浏览器的全屏方法
  if (screen.requestFullscreen) {
    screen.requestFullscreen();
  } else if (screen.mozRequestFullScreen) {
    screen.mozRequestFullScreen();
  } else if (screen.webkitRequestFullscreen) {
    screen.webkitRequestFullscreen();
  } else if (screen.msRequestFullscreen) {
    screen.msRequestFullscreen();
  }
};

const XK_Shift_L = 0xffe1;

const toPaste = () => {
  navigator.clipboard.readText().then((text) => {
    tttt(text.split(''));
  });
};
const tttt = (val: any) => {
  const character = val.shift(); //

  const i = [];
  const code = character.charCodeAt();
  const needs_shift = character.match(/[A-Z!@#$%^&*()_+{}:\"<>?~|]/);

  if (needs_shift) {
    rfb.value.sendKey(XK_Shift_L, 'ShiftLeft', true);
  }
  rfb.value.sendKey(code, character, true);
  rfb.value.sendKey(code, character, false);

  if (needs_shift) {
    rfb.value.sendKey(XK_Shift_L, 'ShiftLeft', false);
  }

  if (val.length > 0) {
    setTimeout(function () {
      tttt(val);
    }, 10);
  }
};
const toReboot = () => {
  ElMessageBox.confirm(proxy.$t('compute.vm.message.confirmRebootTip'), proxy.$t('compute.vm.operation.reboot')).then(
    () => {
      mainApi
        .vmsInstabcesReboot(router.currentRoute.value.query.instanceId)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startReboot'));
          getDetail(); // 获取详情
        })
        .catch((error: any) => {});
    },
  );
};

const dialogVisible = ref(false);
const upload: any = ref();
const isoFile: any = ref();
const isoloading: any = ref(false);
const isoserver: any = ref('');
const isoMsg: any = ref('');
const nowIsoMsg: any = ref('');

const handleClose = (done: any) => {
  dialogVisible.value = false;
  done();
};

const stop_server = () => {
  isoloading.value = false;
  if (isoserver.value) {
    isoserver.value.stop();
  }
};
const start_server = () => {
  console.log(isoFile.value);
  if (!isoFile.value) {
    ElMessage.warning(proxy.$t('compute.vm.message.selectIsoFile'));
    return false;
  }
  isoloading.value = true;
  dialogVisible.value = false;
  isoMsg.value = '';
  nowIsoMsg.value = '';

  const file = isoFile.value;
  let protocol = '';
  if (window.location.protocol === 'https:') {
    protocol = 'wss://';
  } else {
    protocol = 'ws://';
  }
  const wsUrl = `${protocol + window.location.host}/api/iso/${
    router.currentRoute.value.query.instanceId
  }?token=${getCookie('Access-Token')}`;
  // const wsUrl =
  //   protocol +
  //   '192.168.1.92' +
  //   '/api/iso/' +
  //   router.currentRoute.value.query.instanceId +
  //   '?token=' +
  //   getCookie('Access-Token');
  isoserver.value = new NBDServer(wsUrl, file);
  isoserver.value.onlog = (msg: any) => {
    // var container = document.getElementById("log");
    isoMsg.value += `${msg}<br />`;
    nowIsoMsg.value = msg;
    isoloading.value = false;
  };
  isoserver.value.start();
};
const handleChange = (file: any, fileList: any) => {
  // 限制格式

  const isIso = file.name.split('.').pop() === 'iso';
  if (!isIso) {
    ElMessage.error(proxy.$t('compute.vm.message.uploadFileFormat'));
    // 清空上传的文件
    isoFile.value = '';
    upload.value.clearFiles();
    return false;
  }
  isoFile.value = file.raw;
};
const handleExceed = (files: any) => {
  ElMessage.warning(proxy.$t('compute.vm.message.exceed'));
};
const handleRemove = (file: any, fileList: any) => {
  isoFile.value = '';
};
const timer: any = ref(null);
const vmDetailData: any = ref({});
const getDetail = () => {
  // 获取详情

  mainApi
    .vmsInstabcesDetail(router.currentRoute.value.query.instanceId)
    .then((res: any) => {
      vmDetailData.value = res;
    })
    .catch((error: any) => {});
};
const changeBootDev = (val: any) => {
  // 改变启动盘

  const data = {
    // name: form.value.name,
    // description: form.value.description,
    // flavorId: form.value.flavorId,
    bootDev: val,
  };
  mainApi
    .vmsInstabcesEdit(data, router.currentRoute.value.query.instanceId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.startChangeFlavor'));
      getDetail(); // 获取详情
    })
    .catch((error: any) => {});
};
onMounted(() => {
  // 设置 tab 标题
  document.title = `${router.currentRoute.value.query.instanceName}-${proxy.$t('compute.vm.info.remoteDesktop')}`;
  window.addEventListener('beforeunload', (e) => handleUnload(e)); // 监听浏览器关闭事件
  url = getUrl();
  getDetail(); // 请求虚拟机循环列表
  timer.value = setInterval(async () => {
    getDetail(); // 请求虚拟机循环列表
  }, mainStoreData.listRefreshTime);
  if (router.currentRoute.value.query.type == 'vnc') {
    connectVnc();
  }
  // connectVnc();
});
onUnmounted(() => {
  // 组件销毁后
  clearInterval(timer.value);
  window.removeEventListener('beforeunload', (e) => handleUnload(e));
});
onBeforeUnmount(() => {
  // 组件销毁前
  rfb.value && rfb.value.disconnect();
  // stop_server();
});
</script>

<style lang="scss" scoped>
.commandPage {
}
</style>
