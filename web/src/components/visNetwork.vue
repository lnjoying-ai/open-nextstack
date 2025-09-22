<template>
  <div class="networkPage">
    <div id="mynetwork" v-loading="loading" :element-loading-text="$t('common.loading')"></div>
    <el-drawer v-model="drawerStatus" :size="'60%'" direction="rtl">
      <template #title>
        <h4>{{ getDrawerTitle }}</h4>
      </template>
      <template #default>
        <!-- 子网详情 -->
        <div v-if="networkLevel == 2">
          <el-form :model="subnetForm" label-width="160px" size="small">
            <el-card class="!border-none mb-3">
              <template #header>
                <div>
                  <span>{{ $t('components.visNetwork.vpcInfo') }}</span>
                </div>
              </template>
              <div class="text item">
                <el-form-item :label="$t('components.visNetwork.vpcName') + ':'">
                  <span>{{ subnetForm.vpcName || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.vpcId') + ':'">
                  <span>
                    <router-link :to="'/vpc/' + subnetForm.vpcId" class="text-blue-400">
                      {{ subnetForm.vpcId }}
                    </router-link>
                  </span>
                </el-form-item>
              </div>
            </el-card>
            <el-card class="!border-none mb-3">
              <template #header>
                <div>
                  <span>{{ $t('components.visNetwork.basicInfo') }}</span>
                </div>
              </template>
              <div class="text item">
                <el-form-item :label="$t('components.visNetwork.subnetName') + ':'">
                  <span>{{ subnetForm.name || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.subnetId') + ':'">
                  <span>{{ subnetForm.subnetId || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.subnetCidr') + ':'">
                  <span>{{ subnetForm.cidr || '-' }}</span>
                </el-form-item>
              </div>
            </el-card>
          </el-form>
        </div>

        <!-- 裸金属实例详情 -->
        <div v-if="networkLevel == 3">
          <el-form :model="bmsForm" label-width="120px" size="small">
            <el-card class="!border-none mb-3">
              <template #header>
                <div>
                  <span>{{ $t('components.visNetwork.basicInfo') }}</span>
                </div>
              </template>
              <div class="text item">
                <el-form-item :label="$t('components.visNetwork.name') + ':'">
                  <span>{{ bmsForm.name || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.cpuArchitecture') + ':'">
                  <span>{{ bmsForm.a || $t('components.visNetwork.cpuArchitectureValue') }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.flavorSelection') + ':'">
                  <span>{{ bmsForm.a || $t('components.visNetwork.flavorSelectionValue') }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.operatingSystem') + ':'">
                  <span>{{ bmsForm.imageName || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.status') + ':'">
                  <span>{{ filtersFun.getBmsStatus(bmsForm.phaseStatus, 'status') }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.createTime') + ':'">
                  <span>{{ bmsForm.createTime || '-' }}</span>
                </el-form-item>
              </div>
            </el-card>
            <el-card class="!border-none mb-3">
              <template #header>
                <div>
                  <span>{{ $t('components.visNetwork.networkConfig') }}</span>
                </div>
              </template>
              <div class="text item">
                <el-form-item :label="$t('components.visNetwork.vpcName') + ':'">
                  <span>
                    <router-link :to="'/vpc/' + bmsForm.vpcId" class="text-blue-400">
                      {{ bmsForm.vpcName }}
                    </router-link>
                  </span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.subnet') + ':'">
                  <span>
                    <router-link :to="'/subnet/' + bmsForm.subnetId" class="text-blue-400">
                      {{ bmsForm.subnetName }}
                    </router-link>
                  </span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.ip') + ':'">
                  <span>{{ bmsForm.ip || '-' }}</span>
                </el-form-item>
              </div>
            </el-card>
            <el-card class="!border-none mb-3">
              <template #header>
                <div>
                  <span>{{ $t('components.visNetwork.advancedConfig') }}</span>
                </div>
              </template>
              <div class="text item">
                <el-form-item :label="$t('components.visNetwork.hostname') + ':'">
                  <span>{{ bmsForm.hostname || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.loginUsername') + ':'">
                  <span>{{ bmsForm.sysUsername || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.loginPassword') + ':'">
                  <span>{{
                    bmsForm.pubkeyId
                      ? $t('components.visNetwork.loginPasswordValueValue')
                      : $t('components.visNetwork.loginPasswordValue')
                  }}</span>
                </el-form-item>
                <el-form-item
                  v-if="bmsForm.pubkeyId"
                  :label="$t('components.visNetwork.loginPasswordValueValue') + ':'"
                >
                  <span>
                    <router-link :to="'/publicKey/' + bmsForm.pubkeyId" class="text-blue-400">
                      {{ bmsForm.pubkeyId }}
                    </router-link>
                  </span>
                </el-form-item>
              </div>
            </el-card>
          </el-form>
        </div>

        <!-- 虚拟机实例详情 -->
        <div v-if="networkLevel == 33">
          <el-form :model="vmForm" label-width="120px" size="small">
            <el-card class="!border-none mb-3">
              <template #header>
                <div>
                  <span>{{ $t('components.visNetwork.basicConfig') }}</span>
                </div>
              </template>
              <div class="text item">
                <el-form-item :label="$t('components.visNetwork.name') + ':'">
                  <span>{{ vmForm.name || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.description') + ':'">
                  <span>{{ vmForm.desc || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.operatingSystem') + ':'">
                  <span>{{ vmForm.imageName || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.hypervisorNode') + ':'">
                  <span>
                    <router-link :to="'/hypervisorNodes/' + vmForm.hypervisorNodeId" class="text-blue-400">
                      {{ vmForm.hypervisorNodeName || '-' }}
                    </router-link>
                  </span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.flavorName') + ':'">
                  <span>
                    <router-link :to="'/flavors/' + vmForm.flavorId" class="text-blue-400">
                      {{ vmForm.flavorName || '-' }}
                    </router-link>
                  </span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.cpu') + ':'">
                  <span>{{ vmForm.cpu || '-' }}{{ $t('common.core') }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.memory') + ':'">
                  <span>{{ vmForm.mem || '-' }}MB</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.rootDisk') + ':'">
                  <span>{{ vmForm.rootDisk || '-' }}GB</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.status') + ':'">
                  <span>{{ filtersFun.getVmStatus(vmForm.phaseStatus, 'status') }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.createTime') + ':'">
                  <span>{{ vmForm.createTime || '-' }}</span>
                </el-form-item>
              </div>
            </el-card>
            <el-card class="!border-none mb-3">
              <template #header>
                <div>
                  <span>{{ $t('components.visNetwork.networkConfig') }}</span>
                </div>
              </template>
              <div class="text item">
                <el-form-item :label="$t('components.visNetwork.vpcName') + ':'">
                  <span>
                    <router-link :to="'/vpc/' + vmForm.vpcId" class="text-blue-400">
                      {{ vmForm.vpcName }}
                    </router-link>
                  </span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.subnet') + ':'">
                  <span>
                    <router-link :to="'/subnet/' + vmForm.subnetId" class="text-blue-400">
                      {{ vmForm.subnetName }}
                    </router-link>
                  </span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.ip') + ':'">
                  <span>{{ vmForm.ip || '-' }}</span>
                </el-form-item>
              </div>
            </el-card>
            <el-card class="!border-none mb-3">
              <template #header>
                <div>
                  <span>{{ $t('components.visNetwork.advancedConfig') }}</span>
                </div>
              </template>
              <div class="text item">
                <el-form-item :label="$t('components.visNetwork.hostname') + ':'">
                  <span>{{ vmForm.hostname || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.loginUsername') + ':'">
                  <span>{{ vmForm.sysUsername || '-' }}</span>
                </el-form-item>
                <el-form-item :label="$t('components.visNetwork.loginPassword') + ':'">
                  <span>{{
                    vmForm.pubkeyId
                      ? $t('components.visNetwork.loginPasswordValueValue')
                      : $t('components.visNetwork.loginPasswordValue')
                  }}</span>
                </el-form-item>
                <el-form-item v-if="vmForm.pubkeyId" :label="$t('components.visNetwork.loginPasswordValueValue') + ':'">
                  <span>
                    <router-link :to="'/publicKey/' + vmForm.pubkeyId" class="text-blue-400">
                      {{ vmForm.pubkeyId }}
                    </router-link>
                  </span>
                </el-form-item>
              </div>
            </el-card>
          </el-form>
        </div>
      </template>
      <template #footer>
        <div style="flex: auto">
          <el-button v-if="networkLevel == 2" type="primary" size="small" @click="goSubnet">
            {{ $t('components.visNetwork.addSubnet') }}
          </el-button>
          <el-button v-if="networkLevel == 33" type="primary" size="small" @click="goVm">
            {{ $t('components.visNetwork.addVm') }}
          </el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { DataSet, Network } from 'vis-network/standalone';
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';

import imgInternet from '@/assets/img/visNetwork/internet.png';
import imgRoute from '@/assets/img/visNetwork/route.png';
import imgSubnet from '@/assets/img/visNetwork/subnet.png';
import imgInstance from '@/assets/img/visNetwork/instance.png';
import imgVm from '@/assets/img/visNetwork/vm.png';

const { proxy }: any = getCurrentInstance();
const { id } = defineProps<{ id: any }>();
const router = useRouter();

// 状态变量
const loading = ref(false);
const drawerStatus = ref(false);
const networkLevel = ref(0);
const subnetForm = ref<Record<string, any>>({});
const bmsForm = ref<Record<string, any>>({});
const vmForm = ref<Record<string, any>>({});
const topologyData = ref<Record<string, any>>({});

// 网络图相关变量
const nodes: Array<{
  id: string;
  label: string;
  level: number;
  image: string;
  shape: string;
  vm?: boolean;
}> = [];
const edges: Array<{
  from: string;
  to: string;
}> = [];
let network: Network | null = null;

// 计算属性
const getDrawerTitle = computed(() => {
  switch (networkLevel.value) {
    case 2:
      return proxy.$t('components.visNetwork.message.subnetDetail');
    case 3:
      return proxy.$t('components.visNetwork.message.bmsDetail');
    case 33:
      return proxy.$t('components.visNetwork.message.vmDetail');
    default:
      return proxy.$t('components.visNetwork.message.detail');
  }
});

// 网络图相关方法
const destroy = () => {
  if (network !== null) {
    network.destroy();
    network = null;
  }
};

// 获取拓扑数据
const getTopology = async () => {
  try {
    loading.value = true;
    const res = await mainApi.topology(id);

    res.subnetTopologies.forEach((subnet: any) => {
      nodes.push({
        id: subnet.subnetId,
        label: `${subnet.subnetName}\n${subnet.cidr}`,
        level: 2,
        image: imgSubnet,
        shape: 'image',
      });
      edges.push({ from: 'route', to: subnet.subnetId });
      topologyData.value[subnet.subnetId] = { level: 2, id: subnet.subnetId };

      subnet.instanceInfos?.forEach((instance: any) => {
        if (!nodes.map((v) => v.id).includes(instance.instanceId)) {
          nodes.push({
            id: instance.instanceId,
            vm: instance.vm,
            label: `${instance.name}\n${instance.ip}`,
            level: 3,
            image: instance.vm ? imgVm : imgInstance,
            shape: 'image',
          });
        }
        edges.push({ from: subnet.subnetId, to: instance.instanceId });
        topologyData.value[instance.instanceId] = {
          level: 3,
          vm: instance.vm,
          id: instance.instanceId,
        };
      });
    });
    draw();
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

// 绘制网络图
const draw = () => {
  destroy();

  nodes.push(
    {
      id: 'internet',
      label: 'internet',
      level: 0,
      image: imgInternet,
      shape: 'image',
    },
    {
      id: 'route',
      label: proxy.$t('components.visNetwork.message.route'),
      level: 1,
      image: imgRoute,
      shape: 'image',
    },
  );
  edges.push({ from: 'internet', to: 'route' });

  const container = document.getElementById('mynetwork');
  const data = { nodes, edges };
  const options = {
    nodes: {
      size: 20,
      color: { border: '#1296db' },
    },
    edges: {
      smooth: {
        enabled: true,
        type: 'cubicBezier',
        forceDirection: 'horizontal',
        roundness: 0.8,
      },
      arrows: {
        to: { enabled: false, type: 'arrow' },
      },
      length: 50,
    },
    layout: {
      hierarchical: { direction: 'LR' },
    },
    physics: true,
  };

  if (!container) return;

  network = new Network(container, data, options);
  network.on('click', (params) => getDetail(topologyData.value[params.nodes[0]]));
};

// 获取详情
const getDetail = async (item: any) => {
  if (!item) return;

  try {
    if (item.level === 2) {
      const res = await mainApi.subnetsDetail(item.id);
      subnetForm.value = res;
      networkLevel.value = item.level;
    } else if (item.level === 3) {
      if (item.vm) {
        const res = await mainApi.vmsInstabcesDetail(item.id);
        vmForm.value = res;
        networkLevel.value = 33;
      } else {
        const res = await mainApi.bmsDetail(item.id);
        bmsForm.value = res;
        networkLevel.value = 3;
      }
    }
    drawerStatus.value = true;
  } catch (error) {
    console.error(error);
  }
};

// 路由跳转
const goSubnet = () => router.push('/subNetAdd');
const goVm = () => router.push('/vmAdd');

onMounted(() => {
  getTopology();
});
</script>

<style lang="scss" scoped>
.networkPage {
  #mynetwork {
    width: 100%;
    height: 600px;
    margin: 0 auto;
  }
}
</style>
