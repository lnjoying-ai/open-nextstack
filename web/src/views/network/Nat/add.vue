<template>
  <div class="bmsAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="addNatForm"
      v-loading="addLoading"
      :size="mainStoreData.viewSize.main"
      :rules="rules"
      :model="form"
      label-width="120px"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.nat.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.nat.form.name') + ':'" prop="mapName">
            <el-input v-model="form.mapName" class="!w-60" :placeholder="$t('network.nat.form.namePlaceholder')" />
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('network.nat.advancedConfig') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('network.nat.form.network') + ':'" prop="subnetId">
            <el-select
              v-model="vpcId"
              class="m-2 ml-0"
              :placeholder="$t('network.nat.form.selectVpc')"
              @change="vpcIdChange()"
            >
              <el-option
                v-for="(item, index) in vpcList"
                :key="index"
                :label="item.name + ' (' + item.cidr + ')'"
                :value="item.vpcId"
              />
            </el-select>
            <el-select
              v-model="form.subnetId"
              class="m-2 ml-0"
              :placeholder="$t('network.nat.form.selectSubnet')"
              :disabled="!vpcId"
              @change="subnetChange(form.subnetId)"
            >
              <template v-for="(item, index) in subnetsDataList">
                <el-option
                  v-if="vpcId == item.vpcId"
                  :key="index"
                  :label="item.name + ' (' + item.cidr + ')'"
                  :value="item.subnetId"
                />
              </template>
            </el-select>
          </el-form-item>

          <el-form-item :label="$t('network.nat.form.server') + ':'" prop="portId">
            <el-select
              v-model="portIdStatus"
              class="m-2 ml-0"
              :placeholder="$t('network.nat.form.selectVm')"
              @change="portIdChange()"
            >
              <!-- <el-option label="裸金属" :value="0" /> -->
              <el-option :label="$t('network.nat.form.vm')" :value="1" />
            </el-select>
            <el-select
              v-model="form.portId"
              class="m-2 ml-0"
              :placeholder="$t('network.nat.form.selectVmPlaceholder')"
              :disabled="!form.subnetId"
            >
              <el-option
                v-for="(item, index) in bmsDataList"
                :key="index"
                :label="item.name"
                :value="item.portInfo.portId"
              />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('network.nat.form.protocolType') + ':'">
            <el-radio-group v-model="form.oneToOne" @change="protocolChange">
              <el-radio :label="true" :value="true">{{ $t('network.nat.form.ip') }}</el-radio>
              <el-radio :label="false" :value="false">{{ $t('network.nat.form.tcpUdp') }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="$t('network.nat.form.eipAddress') + ':'" prop="eipId">
            <el-select
              v-model="form.eipId"
              class="m-2 ml-0 float-left"
              :placeholder="$t('network.nat.form.selectEip')"
              :disabled="vpcId == ''"
              @change="eipChange(form.eipId)"
            >
              <template v-if="eipsDataloading">
                <el-option
                  v-for="(item, index) in eipsDataList"
                  :key="index"
                  :label="item.ipAddress"
                  :value="item.eipId"
                />
              </template>
            </el-select>
            <el-button
              type="primary"
              :disabled="form.subnetId == ''"
              :size="mainStoreData.viewSize.main"
              @click="applyEip"
              >{{ $t('network.nat.form.applyEip') }}</el-button
            >
          </el-form-item>
          <div v-if="!form.oneToOne">
            <template v-for="(item, index) in form.portMaps">
              <div class="overflow-hidden">
                <el-form-item :label="$t('network.nat.form.protocol') + ':'" class="!w-60 float-left">
                  <el-select
                    v-model="item.protocol"
                    class="m-0 ml-0"
                    :placeholder="$t('network.nat.form.protocolPlaceholder')"
                  >
                    <el-option key="TCP" :label="$t('network.nat.form.tcp')" :value="0" />
                    <el-option key="UDP" :label="$t('network.nat.form.udp')" :value="1" />
                  </el-select>
                </el-form-item>
                <el-form-item
                  :label="$t('network.nat.form.port') + ':'"
                  class="!w-45 float-left"
                  label-width="70px"
                  :prop="'portMaps.' + index + '.globalPort'"
                  :rules="[
                    {
                      required: true,
                      validator: validatePort,
                      trigger: 'blur',
                    },
                    {
                      required: true,
                      validator: validatePort,
                      trigger: 'change',
                    },
                  ]"
                >
                  <el-input v-model="item.globalPort" class="!w-60" :placeholder="$t('network.nat.form.inputPort')" />
                </el-form-item>
                <el-form-item :label="$t('network.nat.form.eipPort') + ':'" class="!w-45 float-left" label-width="80px">
                  <el-input
                    v-model="item.globalPort"
                    class="!w-60"
                    disabled
                    :placeholder="$t('network.nat.form.inputEipPort')"
                  />
                </el-form-item>
                <el-form-item label=" " class="!w-60" label-width="0">
                  <el-button type="danger" :size="mainStoreData.viewSize.main" @click="delProtocol(index)">{{
                    $t('common.delete')
                  }}</el-button>
                </el-form-item>
              </div>
            </template>
            <el-form-item label=" ">
              <el-button type="primary" @click="addProtocol">{{ $t('network.nat.form.addProtocol') }}</el-button>
            </el-form-item>
          </div>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toNatAdd">{{ $t('common.createNow') }}</el-button>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const mainStoreData = mainStore(); // pinia 信息
const { proxy }: any = getCurrentInstance();
const addNatForm = ref<any>();

const router: any = useRouter();
const loading = ref(false);
const addLoading = ref(false);
const vpcList: any = ref([]);
const subnetsDataList: any = ref([]);
const bmsDataList: any = ref([]);
const eipsDataList: any = ref([]);
const eipsDataloading = ref(false);

const portsCheckTCPList: any = ref([]); // TCP端口检查列表
const portsCheckUDPList: any = ref([]); // UDP端口检查列表
const vpcId = ref('');
const portIdStatus = ref(1);
const form = reactive({
  // 表单
  mapName: '',
  subnetId: '',
  eipId: '',
  addressType: 0,
  portId: '',
  portMaps: [
    { globalPort: '', localPort: '', protocol: 0 },
    // { globalPort: 53, localPort: 53, protocol: 1 },
  ],
  oneToOne: true,
  bandwidth: '',
});
const validatePort = (rule: any, value: any, callback: any) => {
  // 端口是否被占用校验
  const isnumber = /^\d+$/.test(value);
  let checkPortData: any = [];
  let checkPortData2: any = [];
  const tcpPort: any = [];
  const udpPort: any = [];
  form.portMaps.forEach((item: any) => {
    if (item.protocol == 0) {
      tcpPort.push(item.globalPort * 1);
    } else {
      udpPort.push(item.globalPort * 1);
    }
  });

  if (form.portMaps[rule.field.split('.')[1]].protocol === 0) {
    checkPortData = [...portsCheckTCPList.value];
    checkPortData2 = tcpPort.filter((item: any) => {
      return item * 1 == value * 1;
    });
  } else if (form.portMaps[rule.field.split('.')[1]].protocol === 1) {
    checkPortData = [...portsCheckUDPList.value];
    checkPortData2 = udpPort.filter((item: any) => {
      return item * 1 == value * 1;
    });
  } else {
    checkPortData = [];
    checkPortData2 = [];
  }

  if (value === '') {
    callback(new Error(proxy.$t('network.nat.validation.portRequired')));
  } else if (value < 1 || value > 65535 || !isnumber) {
    callback(new Error(proxy.$t('network.nat.validation.portValid')));
  } else if (checkPortData.includes(value * 1)) {
    callback(new Error(proxy.$t('network.nat.validation.portOccupied')));
  } else if (checkPortData2.length > 1) {
    callback(new Error(proxy.$t('network.nat.validation.portDuplicate')));
  } else {
    callback();
  }
};
const rules = {
  mapName: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
  subnetId: [
    {
      required: true,
      message: proxy.$t('network.nat.validation.selectVpcAndSubnet'),
      trigger: 'change',
    },
  ],
  portId: [
    {
      required: true,
      message: proxy.$t('network.nat.validation.selectVm'),
      trigger: 'change',
    },
  ],
  eipId: [
    {
      required: true,
      message: proxy.$t('network.nat.validation.selectEip'),
      trigger: 'change',
    },
  ],
};
const applyEip = () => {
  loading.value = true;
  mainApi
    .eipsAllocate(vpcId.value)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('network.nat.message.applySuccess'));
      geteipsList();
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const toNatAdd = async () => {
  const data = JSON.parse(JSON.stringify(form));
  if (data.oneToOne) {
    data.portMaps = [];
  } else {
    data.portMaps.forEach((item: any) => {
      item.globalPort = parseInt(item.globalPort);
      item.localPort = parseInt(item.globalPort);
    });
  }

  addLoading.value = true;
  addNatForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .portMapAdd(data)
        .then((res: any) => {
          ElMessage.success(proxy.$t('network.nat.message.startCreate'));
          addLoading.value = false;
          proxy.$emit('closeDrawer');
        })
        .catch((error: any) => {
          addLoading.value = false;
        });
    } else {
      addLoading.value = false;
    }
  });
};
const delProtocol = (index: number) => {
  // 删除协议
  if (form.portMaps.length > 1) {
    form.portMaps.splice(index, 1);
  } else {
    form.portMaps[0].globalPort = '';
    form.portMaps[0].localPort = '';
    form.portMaps[0].protocol = 0;
  }
};
const addProtocol = () => {
  // 新增协议
  form.portMaps.push({ globalPort: '', localPort: '', protocol: 0 });
};
const goBack = () => {
  router.push('/Nat');
};
const eipChange = (e: any) => {
  // EIP变更
  getPortsCheck(e);
};

const getPortsCheck = (e: any) => {
  // 获取端口检查
  mainApi.portsCheck({ protocol: 0 }, e).then((res: any) => {
    portsCheckTCPList.value = res.ports;
  });
  mainApi.portsCheck({ protocol: 1 }, e).then((res: any) => {
    portsCheckUDPList.value = res.ports;
  });
};
const getVpcList = () => {
  // VPC列表
  mainApi
    .vpcList({ vpc_phase: 1 })
    .then((res: any) => {
      vpcList.value = res.vpcs;
    })
    .catch((error: any) => {});
};
const getSubNetList = () => {
  // 子网列表

  mainApi
    .subnetsList()
    .then((res: any) => {
      subnetsDataList.value = res.subnets;
    })
    .catch((error: any) => {});
};
const portIdChange = () => {
  form.portId = '';
  getBmsList();
};
const vpcIdChange = () => {
  // vpc 改变
  form.subnetId = '';
  form.portId = '';
  geteipsList(); // 获取数据
};
const subnetChange = (e: any) => {
  // 子网变更
  form.portId = '';
  if (e != '') {
    getBmsList();
  }
};
const getBmsList = () => {
  // 服务器列表
  if (form.subnetId == '') {
    return;
  }
  if (portIdStatus.value === 0) {
    mainApi
      .bmsList({ port_id_is_null: false, eipMap_is_using: false, subnet_id: form.subnetId })
      .then((res: any) => {
        bmsDataList.value = res.instances;
      })
      .catch((error: any) => {});
  } else {
    mainApi
      .vmsInstabcesList({ port_id_is_null: false, eipMap_is_using: false, subnet_id: form.subnetId })
      .then((res: any) => {
        bmsDataList.value = res.vmInstancesInfo;
      })
      .catch((error: any) => {});
  }
};

const geteipsList = () => {
  eipsDataloading.value = false;
  // 获取数据
  mainApi
    .eipsList({ oneToOne: form.oneToOne, vpc_id: vpcId.value, page_size: 99999, page_num: 1 })
    .then((res: any) => {
      eipsDataList.value = res.eips;
      eipsDataloading.value = true;
    })
    .catch((error: any) => {
      eipsDataloading.value = true;
    });
};
// 监听协议类型
const protocolChange = () => {
  form.eipId = '';
  geteipsList();
};

onMounted(() => {
  getVpcList(); // VPC列表
  getSubNetList(); // 子网列表
  getBmsList(); // 裸金属列表
});
</script>

<style lang="scss" scoped>
.bmsAddPage {
}
</style>
