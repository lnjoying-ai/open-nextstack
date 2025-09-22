<template>
  <div class="sgsAddPage h-full">
    <el-dialog
      v-model="addruleDialog"
      :title="
        (isAddruleType ? $t('network.secGroups.add') : $t('network.secGroups.edit')) +
        (addruleType === 0 ? $t('network.secGroups.inDirectionRule') : $t('network.secGroups.outDirectionRule'))
      "
      width="1000px"
      :before-close="handleClose"
      :close-on-click-modal="false"
    >
      <el-form ref="addRuleDataRef" key="0" :model="addRuleData" label-width="0px">
        <el-table
          v-loading="loading"
          :size="mainStoreData.viewSize.main"
          :data="addRuleData.list"
          style="width: 100%"
          class="rulesTable"
          :element-loading-text="$t('common.loading')"
        >
          <el-table-column prop="priority" :label="$t('network.secGroups.form.priority')" width="100px">
            <template #header>
              {{ $t('network.secGroups.form.priority') }}

              <el-tooltip placement="top" effect="dark">
                <template #content>
                  <div class="w-200px">
                    {{ $t('network.secGroups.form.priorityTooltip') }}
                  </div>
                </template>
                <span class="text-xs inline-block align-middle cursor-pointer">
                  <i-bi:info-square></i-bi:info-square>
                </span>
              </el-tooltip>
            </template>
            <template #default="scope">
              <el-form-item
                class="m-0"
                label=""
                :prop="'list.' + scope.$index + '.priority'"
                :rules="[
                  {
                    required: true,
                    validator: validatePriority,
                    trigger: 'change',
                  },
                  {
                    required: true,
                    validator: validatePriority,
                    trigger: 'blur',
                  },
                ]"
              >
                <el-input
                  v-model="scope.row.priority"
                  class="w-4/5"
                  :size="mainStoreData.viewSize.main"
                  placeholder="1-100"
                />
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column prop="action" :label="$t('network.secGroups.form.action')" width="100">
            <template #header>
              {{ $t('network.secGroups.form.action') }}

              <el-tooltip placement="top" effect="dark">
                <template #content>
                  <div class="w-200px">{{ $t('network.secGroups.form.actionTooltip') }}</div>
                </template>
                <span class="text-xs inline-block align-middle cursor-pointer">
                  <i-bi:info-square></i-bi:info-square>
                </span>
              </el-tooltip>
            </template>
            <template #default="scope">
              <el-select v-model="scope.row.action" class="" :size="mainStoreData.viewSize.main">
                <el-option key="accept" :label="$t('network.secGroups.form.allow')" :value="1" />
                <el-option key="drop" :label="$t('network.secGroups.form.deny')" :value="0" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column prop="port" :label="$t('network.secGroups.form.protocol')">
            <template #header>
              {{ $t('network.secGroups.form.protocol') }}

              <el-tooltip placement="top" effect="dark">
                <template #content>
                  <div class="w-220px">
                    <p>{{ $t('network.secGroups.form.protocolTips.title') }}</p>
                    <ul>
                      <li>{{ $t('network.secGroups.form.protocolTips.text1') }}</li>
                      <li>{{ $t('network.secGroups.form.protocolTips.text2') }}</li>
                      <li>{{ $t('network.secGroups.form.protocolTips.text3') }}</li>
                      <li>{{ $t('network.secGroups.form.protocolTips.text4') }}</li>
                      <li>{{ $t('network.secGroups.form.protocolTips.text5') }}</li>
                    </ul>
                    <p>{{ $t('network.secGroups.form.protocolTips.text6') }}</p>
                  </div>
                </template>
                <span class="text-xs inline-block align-middle cursor-pointer">
                  <i-bi:info-square></i-bi:info-square>
                </span>
              </el-tooltip>
            </template>
            <template #default="scope">
              <el-form-item class="m-0" label="" :prop="'list.' + scope.$index + '.port'">
                <el-select
                  v-model="scope.row.protocol"
                  :size="mainStoreData.viewSize.main"
                  @change="changeProtocol(scope.row)"
                >
                  <el-option key="IP" :label="$t('network.secGroups.form.allProtocol')" :value="3" />
                  <el-option key="TCP" :label="$t('network.secGroups.form.tcp')" :value="0" />
                  <el-option key="UDP" :label="$t('network.secGroups.form.udp')" :value="1" />
                  <el-option key="ICMP" :label="$t('network.secGroups.form.icmp')" :value="4" />
                </el-select>
                <el-input
                  v-if="scope.row.protocol != 4 && scope.row.protocol != 3"
                  v-model="scope.row.port"
                  :size="mainStoreData.viewSize.main"
                  :disabled="scope.row.protocol == 3"
                  :placeholder="scope.row.protocol == 3 ? $t('network.secGroups.form.allPort') : '1-65535'"
                  class="max-w-215px"
                  @input="changePort(addRuleData, scope.$index)"
                />
                <el-select
                  v-if="scope.row.protocol == 4"
                  v-model="scope.row.port"
                  multiple
                  clearable
                  :size="mainStoreData.viewSize.main"
                  @change="changePortIcmp(addRuleData, scope.$index)"
                >
                  <el-option
                    v-for="(item, index) in proxy.$scriptMain.getICMP()"
                    :key="index"
                    :label="item.name"
                    :value="item.value"
                    >{{ item.name }}</el-option
                  >
                </el-select>
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column prop="addressType" :label="$t('network.secGroups.form.type')" width="100">
            <template #default="scope">
              <el-select v-model="scope.row.addressType" :size="mainStoreData.viewSize.main" :disabled="true">
                <el-option key="IPv4" :label="$t('network.secGroups.form.ipv4')" :value="0" />
                <el-option key="IPv6" :label="$t('network.secGroups.form.ipv6')" :value="1" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column
            prop="addressRefType"
            :label="
              addruleType === 0
                ? $t('network.secGroups.form.sourceAddress')
                : $t('network.secGroups.form.destinationAddress')
            "
            width="180"
          >
            <template #header>
              {{
                addruleType === 0
                  ? $t('network.secGroups.form.sourceAddress')
                  : $t('network.secGroups.form.destinationAddress')
              }}

              <el-tooltip placement="top" effect="dark">
                <template #content>
                  <div class="w-220px">
                    <p>
                      {{
                        addruleType === 0
                          ? $t('network.secGroups.form.sourceAddressTooltip')
                          : $t('network.secGroups.form.destinationAddressTooltip')
                      }}
                    </p>
                    <ul>
                      <li>{{ $t('network.secGroups.form.addressTypeTips.text1') }}</li>
                      <li>{{ $t('network.secGroups.form.addressTypeTips.text2') }}</li>
                      <li>{{ $t('network.secGroups.form.addressTypeTips.text3') }}</li>
                      <li>{{ $t('network.secGroups.form.addressTypeTips.text4') }}</li>
                    </ul>
                  </div>
                </template>
                <span class="text-xs inline-block align-middle cursor-pointer">
                  <i-bi:info-square></i-bi:info-square>
                </span>
              </el-tooltip>
            </template>
            <template #default="scope">
              <el-form-item
                class="m-0"
                label=""
                :prop="'list.' + scope.$index + '.addressRef'"
                :rules="[
                  {
                    required: true,
                    validator: (rule: any, value: any, callback: any) => {
                      validateAddressRef(rule, value, callback, scope.$index);
                    },
                    trigger: 'change',
                  },
                  {
                    required: true,
                    validator: (rule: any, value: any, callback: any) => {
                      validateAddressRef(rule, value, callback, scope.$index);
                    },
                    trigger: 'blur',
                  },
                ]"
              >
                <el-select
                  v-model="scope.row.addressRefType"
                  :size="mainStoreData.viewSize.main"
                  @change="changeAddressRefType(scope.row)"
                >
                  <el-option key="cidr" :label="$t('network.secGroups.form.ipAddress')" value="cidr" />
                  <el-option key="sgId" :label="$t('network.secGroups.form.securityGroup')" value="sgId" />
                  <!-- <el-option key="ipPoolId" label="IP地址组" value="ipPoolId" /> -->
                </el-select>
                <el-input
                  v-if="scope.row.addressRefType == 'cidr'"
                  v-model="scope.row.addressRef.cidr"
                  :size="mainStoreData.viewSize.main"
                  placeholder="0.0.0.0/0"
                />
                <el-select
                  v-if="scope.row.addressRefType == 'sgId'"
                  v-model="scope.row.addressRef.sgId"
                  :size="mainStoreData.viewSize.main"
                >
                  <el-option v-for="(item, index) in sgsListData" :key="index" :label="item.name" :value="item.sgId" />
                </el-select>
              </el-form-item>
            </template>
          </el-table-column>
          <el-table-column prop="description" :label="$t('network.secGroups.form.description')" width="160">
            <template #default="scope">
              <el-input v-model="scope.row.description" :size="mainStoreData.viewSize.main" autosize type="textarea" />
            </template>
          </el-table-column>
          <el-table-column v-if="isAddruleType" prop="name" :label="$t('common.operation')" width="100">
            <template #default="scope">
              <el-button :size="mainStoreData.viewSize.main" type="text" @click="sgAddCopy(scope.row)">
                {{ $t('common.copy') }}
              </el-button>
              <el-button
                :size="mainStoreData.viewSize.main"
                type="text"
                :disabled="!(addRuleData.list.length > 1)"
                @click="sgAddDel(scope)"
                >{{ $t('common.delete') }}</el-button
              >
            </template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button
            v-if="isAddruleType"
            type="text"
            :size="mainStoreData.viewSize.main"
            class="float-left"
            @click="sgAddCopy(initRuleData[0])"
          >
            <i-ic:baseline-add></i-ic:baseline-add>
            {{ $t('network.secGroups.form.addRule') }}
          </el-button>

          <el-button :size="mainStoreData.viewSize.main" @click="handleClose()">
            {{ $t('common.cancel') }}
          </el-button>
          <el-button v-if="isAddruleType" :size="mainStoreData.viewSize.main" type="primary" @click="toAddSg()">{{
            $t('common.confirm')
          }}</el-button>
          <el-button v-else :size="mainStoreData.viewSize.main" type="primary" @click="toEditSg()">{{
            $t('common.confirm')
          }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import netmask from 'netmask';
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';

const mainStoreData = mainStore(); // pinia 信息

const { proxy }: any = getCurrentInstance();
const { Netmask } = netmask;
const props = defineProps({
  itemInfo: {
    type: Object,
    default: {},
  },
});
const router = useRouter();
const loading: any = ref(false); // loading
const addruleDialog = ref(false); // 添加规则弹窗
const addruleType: any = ref(0); // 添加规则 类型 in/out 0/1
const isAddruleType: any = ref(true); // 规则 类型 add OR edit
const sgsListData: any = ref([]); // 安全组列表

const addRuleDataRef: any = ref<any>(); // 添加规则数据

const initRuleData: any = reactive([
  {
    // 添加规则表单
    description: '', // 描述
    priority: '1', // 优先级
    direction: addruleType, // 出入规则 in/out
    protocol: 0, // 协议端口 TCP/UDP/IP/ICMP 0/1/3/4
    port: '', // 端口
    addressType: 0, // 源地址类型 IPv4/IPv6 0/1
    addressRefType: 'cidr',
    addressRef: {
      cidr: '0.0.0.0/0', // 源地址 0.0.0.0/0
      sgId: '', // 安全组ID
      // ipPoolId: '', //IP地址组
    },
    action: 1, // 拒绝/允许  drop/accept 0/1xz
  },
]);
const addRuleData: any = ref({
  list: [],
}); // 添加规则列表

const validatePriority = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error(proxy.$t('network.secGroups.validation.priorityRequired')));
  } else if (!/^\d+$/.test(value) || value < 1 || value > 100) {
    callback(new Error(proxy.$t('network.secGroups.validation.priorityValid')));
  } else {
    callback();
  }
};
const validateAddressRef = (rule: any, value: any, callback: any, index: any) => {
  if (addRuleData.value.list[index].addressRefType === 'cidr') {
    if (addRuleData.value.list[index].addressRef.cidr === '') {
      callback(new Error(proxy.$t('network.secGroups.validation.cidrRequired')));
    } else {
      const { cidr } = addRuleData.value.list[index].addressRef;
      const ip = cidr.split('/')[0];
      const netBlock = new Netmask(cidr);
      const netBlock1 = new Netmask(netBlock.base);

      if (netBlock1.contains(ip)) {
        callback();
      } else {
        callback(new Error(proxy.$t('network.secGroups.validation.cidrValid')));
      }
    }
  } else if (addRuleData.value.list[index].addressRefType === 'sgId') {
    if (addRuleData.value.list[index].addressRef.sgId === '') {
      callback(new Error(proxy.$t('network.secGroups.validation.sgIdRequired')));
    } else {
      callback();
    }
  } else {
    callback();
  }
};
watch(
  () => props.itemInfo,
  (val) => {
    if (val) {
      if (val.dialogVisible && val.isAdd) {
        isAddruleType.value = val.isAdd; // 规则弹窗为add
        addruleType.value = val.ruleType; // 打开添加规则弹窗
        addRuleData.value.list.push(JSON.parse(JSON.stringify(initRuleData[0])));
        addruleDialog.value = val.dialogVisible; // 打开添加规则弹窗
      }
      if (val.dialogVisible && !val.isAdd) {
        isAddruleType.value = val.isAdd; // 规则弹窗为add
        addruleType.value = val.ruleType; // 打开添加规则弹窗
        addruleDialog.value = val.dialogVisible; // 打开添加规则弹窗
        addRuleData.value.list.push({
          // 添加规则表单
          id: props.itemInfo.item.ruleId,
          description: props.itemInfo.item.description, // 描述
          priority: props.itemInfo.item.priority, // 优先级
          direction: props.itemInfo.item.direction, // 出入规则 in/out
          protocol: props.itemInfo.item.protocol, // 协议端口 TCP/UDP/IP/ICMP 0/1/3/4
          port:
            props.itemInfo.item.protocol == 4
              ? props.itemInfo.item.port == 'all'
                ? ['0']
                : props.itemInfo.item.port.split(',')
              : props.itemInfo.item.port, // 端口
          addressType: props.itemInfo.item.addressType, // 源地址类型 IPv4/IPv6 0/1
          addressRefType: props.itemInfo.item.addressRef.cidr
            ? 'cidr'
            : props.itemInfo.item.addressRef.sgId
            ? 'sgId'
            : '',
          addressRef: {
            cidr:
              props.itemInfo.item.addressRef && props.itemInfo.item.addressRef.cidr
                ? props.itemInfo.item.addressRef.cidr
                : '', // 源地址 0.0.0.0/0
            sgId:
              props.itemInfo.item.addressRef && props.itemInfo.item.addressRef.sgId
                ? props.itemInfo.item.addressRef.sgId
                : '', // 安全组ID
            // ipPoolId: '', //IP地址组
          },
          action: props.itemInfo.item.action, // 拒绝/允许  drop/accept 0/1xz
        });
      }
    }
  },
  {
    immediate: true,
  },
);

const handleClose = () => {
  resetForm();

  addruleDialog.value = false; // 关闭添加规则弹窗
};
const resetForm = () => {
  addRuleData.value.list.splice(0, addRuleData.value.list.length);
};
const changeProtocol = (item: any) => {
  // 添加规则 协议端口变化
  if (item.protocol == 4) {
    // ICMP
    item.port = [];
  } else {
    item.port = '';
  }
};
const changePort = (val: any, index: any) => {
  // 过滤去掉特殊字符
  val.list[index].port = val.list[index].port.replace(/[^\d,-]/g, '');
};
const changePortIcmp = (val: any, index: any) => {
  // 选择全部协议
  if (val.list[index].port.length > 1 && val.list[index].port.includes('0')) {
    val.list[index].port = ['0'];
  }
};
const changeAddressRefType = (item: any) => {
  // 源地址改变
  const id: any = props.itemInfo.sgId;

  if (item.addressRefType == 'cidr') {
    item.addressRef.cidr = '0.0.0.0/0';
    item.addressRef.sgId = '';
  }
  if (item.addressRefType == 'sgId') {
    item.addressRef.cidr = '';
    item.addressRef.sgId = id;
  }
};
const sgAddCopy = (item: any) => {
  // 添加规则复制一条
  addRuleData.value.list.push(JSON.parse(JSON.stringify(item)));
};
const sgAddDel = (item: any) => {
  // 添加规则删除一条
  addRuleData.value.list.splice(item.$index, 1);
};
const toEditSg = () => {
  // 确认修改规则
  loading.value = true;
  const id: any = props.itemInfo.sgId;

  addRuleDataRef.value.validate(async (valid: any) => {
    if (valid) {
      const data = JSON.parse(JSON.stringify(addRuleData.value.list));
      const ruleId = JSON.parse(JSON.stringify(data[0].id));
      data.filter((item: any) => {
        item.priority *= 1;
        if (item.protocol == 4) {
          item.port = item.port.toString();
        }
        delete item.addressRefType;
        delete item.id;
      });
      mainApi
        .sgsRulesEdit(data[0], id, ruleId)
        .then((res: any) => {
          loading.value = false;
          ElMessage.success(proxy.$t('common.operations.success.modify'));
          handleClose(); // 初始化输入框 关闭弹窗
          proxy.$emit('addRuleClose');
        })
        .catch((error: any) => {
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
const toAddSg = () => {
  // 确认添加规则
  const id: any = props.itemInfo.sgId;

  addRuleDataRef.value.validate(async (valid: any) => {
    if (valid) {
      loading.value = true;

      const data = JSON.parse(JSON.stringify(addRuleData.value.list));
      data.filter((item: any) => {
        item.priority *= 1;
        if (item.protocol == 4) {
          item.port = item.port.toString();
        }
        delete item.addressRefType;
      });
      mainApi
        .sgsRuleAdd({ createSgRules: data }, id)
        .then((res: any) => {
          loading.value = false;
          ElMessage.success(proxy.$t('common.operations.success.add'));
          handleClose(); // 初始化输入框 关闭弹窗
          proxy.$emit('addRuleClose');
        })
        .catch((error: any) => {
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
const getSgsList = () => {
  // sgs列表

  mainApi
    .sgsList({})
    .then((res: any) => {
      sgsListData.value = res.securityGroups;
    })
    .catch((error: any) => {});
};
onMounted(() => {
  getSgsList();
});
</script>

<style lang="scss" scoped>
.sgsAddPage {
}
</style>
