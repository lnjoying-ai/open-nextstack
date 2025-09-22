<template>
  <div class="indexPage">
    <div class="pageTitle bg-white py-2 px-4 border-t-1 border-gray-100 border-solid">
      {{ $t('index.title') }}
    </div>
    <div class="indexMain p-3">
      <el-row :gutter="10">
        <el-col :span="12">
          <el-row :gutter="10" class="mb-3">
            <el-col :span="24" class="mb-3">
              <el-card class="box-card">
                <template #header>
                  <div class="card-header">
                    <span>{{ $t('index.vm.title') }}</span>
                  </div>
                </template>
                <div>
                  <div class="text-center mb-3">
                    <img src="@/assets/img/index/vm.png" alt="" class="w-6 inline-block mr-4 align-text-bottom" />
                    <span class="text-3xl text-bold leading-8">{{ apiData.vm.instanceTotal }}</span>
                  </div>
                  <ul>
                    <li class="overflow-hidden">
                      <span class="float-left text-center">
                        <img src="@/assets/img/index/1.png" alt="" class="w-4 inline-block mr-2 align-text-bottom" />
                        <span class="text-sm text-gray-400 leading-10">{{ $t('index.vm.instanceRunning') }}</span>
                      </span>
                      <span class="float-right text-base leading-10">{{ apiData.vm.instanceRunning }}</span>
                    </li>
                    <li class="overflow-hidden">
                      <span class="float-left text-center">
                        <img src="@/assets/img/index/2.png" alt="" class="w-4 inline-block mr-2 align-text-bottom" />
                        <span class="text-sm text-gray-400 leading-10">{{ $t('index.vm.instanceCreating') }}</span>
                      </span>
                      <span class="float-right text-base leading-10">{{ apiData.vm.instanceCreating }}</span>
                    </li>
                    <li class="overflow-hidden">
                      <span class="float-left text-center">
                        <img src="@/assets/img/index/3.png" alt="" class="w-4 inline-block mr-2 align-text-bottom" />
                        <span class="text-sm text-gray-400 leading-10">{{ $t('index.vm.instanceCreateFailed') }}</span>
                      </span>
                      <span class="float-right text-base leading-10">{{ apiData.vm.instanceCreateFailed }}</span>
                    </li>
                    <li class="overflow-hidden">
                      <span class="float-left text-center">
                        <img src="@/assets/img/index/4.png" alt="" class="w-4 inline-block mr-2 align-text-bottom" />
                        <span class="text-sm text-gray-400 leading-10">{{ $t('index.vm.instancePowerOff') }}</span>
                      </span>
                      <span class="float-right text-base leading-10">{{ apiData.vm.instancePowerOff }}</span>
                    </li>
                  </ul>
                </div>
              </el-card>
            </el-col>
            <el-col :span="24">
              <el-row class="mb-3">
                <el-col :span="24">
                  <el-card class="box-card">
                    <template #header>
                      <div class="card-header">
                        <span>{{ $t('index.storage.title') }}</span>
                        <span v-if="isAdmin" class="float-right"
                          >{{ $t('index.storage.total') }}：{{ apiData.allStorage.total
                          }}{{ apiData.allStorage.unit }}</span
                        >
                      </div>
                    </template>
                    <div>
                      <div v-if="isAdmin">
                        <el-progress :text-inside="true" :stroke-width="45" :percentage="percentageNum"></el-progress>
                      </div>
                      <div v-else>
                        <div class="h-12 rounded text-center" style="background-color: #409eff">
                          <span class="block leading-12 text-white text-bold text-xl"
                            >{{ apiData.userStorage.used || 0 }}<small>{{ apiData.userStorage.unit }}</small></span
                          >
                        </div>
                      </div>
                      <div class="text-center pt-6">
                        <ul>
                          <li class="inline-block px-14">
                            <div class="mb-2">
                              <span
                                class="inline-block w-4 h-4 align-middle mr-2"
                                style="background-color: #409eff"
                              ></span>
                              <span class="text-sm">{{ $t('index.storage.used') }}</span>
                            </div>
                            <div>
                              <span v-if="!isAdmin" class="text-3xl text-bold mr-2">{{
                                apiData.userStorage.used
                              }}</span>
                              <span v-if="isAdmin" class="text-3xl text-bold mr-2">{{
                                apiData.allStorage.used || 0
                              }}</span>
                              <span>{{ apiData.allStorage.unit }}</span>
                            </div>
                          </li>
                          <li v-if="isAdmin" class="inline-block px-14">
                            <div class="mb-2">
                              <span
                                class="inline-block w-4 h-4 align-middle mr-2"
                                style="background-color: #ebeef5"
                              ></span>
                              <span class="text-sm">{{ $t('index.storage.unused') }}</span>
                            </div>
                            <div>
                              <span class="text-3xl text-bold mr-2">{{ apiData.allStorage.unused || 0 }}</span>
                              <span>{{ apiData.allStorage.unit }}</span>
                            </div>
                          </li>
                        </ul>
                      </div>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
              <el-row :gutter="10">
                <el-col :span="24">
                  <el-card class="box-card">
                    <template #header>
                      <div class="card-header">
                        <span>{{ $t('index.network.title') }}</span>
                      </div>
                    </template>
                    <div style="height: 11rem">
                      <ul class="text-center pt-10">
                        <li class="w-1/5 inline-block">
                          <span class="block text-dark-200">{{ $t('index.network.sgCount') }}</span>
                          <span class="block text-2xl text-bold leading-12">{{ apiData.network.sgCount || 0 }}</span>
                        </li>
                        <li class="w-1/5 inline-block">
                          <span class="block text-dark-200">{{ $t('index.network.natCount') }}</span>
                          <span class="block text-2xl text-bold leading-12">{{ apiData.network.natCount || 0 }}</span>
                        </li>
                        <li class="w-1/5 inline-block">
                          <span class="block text-dark-200">{{ $t('index.network.eipCount') }}</span>
                          <span class="block text-2xl text-bold leading-12">{{ apiData.network.eipCount || 0 }}</span>
                        </li>
                        <li class="w-1/5 inline-block">
                          <span class="block text-dark-200">{{ $t('index.network.vpcCount') }}</span>
                          <span class="block text-2xl text-bold leading-12">{{ apiData.vpcCount || 0 }}</span>
                        </li>
                        <li class="w-1/5 inline-block">
                          <span class="block text-dark-200">{{ $t('index.network.subnetCount') }}</span>
                          <span class="block text-2xl text-bold leading-12">{{ apiData.subnetCount || 0 }}</span>
                        </li>
                      </ul>
                    </div>
                  </el-card>
                </el-col>
              </el-row>
            </el-col>
          </el-row>
        </el-col>
        <el-col :span="12">
          <el-row>
            <el-col :span="24" class="mb-3">
              <el-card class="box-card">
                <template #header>
                  <div class="card-header">
                    <span>{{ $t('index.recentSevenDaysAlarms') }}</span>
                    <small> ({{ $t('index.recentSevenDaysAlarmsTip', { count: tableDataTotal }) }})</small>
                    <router-link to="/devOps/alertHome" class="float-right text-blue-400 cursor-pointer text-sm">{{
                      $t('index.viewAll')
                    }}</router-link>
                  </div>
                </template>
                <div style="height: calc(12.8rem + 40px)" class="-m-20px">
                  <el-table
                    v-loading="alarmInfoLoading"
                    :size="mainStoreData.viewSize.main"
                    :element-loading-text="$t('common.loading')"
                    :data="tableData"
                    max-height="calc(12.5rem + 40px)"
                    class="!overflow-y-auto"
                    stripe
                    :show-header="false"
                    :scrollbar-always-on="false"
                  >
                    <el-table-column prop="summeryInfo" width="350" :label="$t('index.alarmContent')">
                      <template #default="scope">
                        <span class="block">{{ scope.row.summeryInfo }}</span>
                        <div class="leading-tight">
                          <small>({{ scope.row.detailInfo }})</small>
                        </div>
                      </template>
                    </el-table-column>
                    <el-table-column prop="triggerBehavior" :label="$t('index.triggerBehavior')">
                      <template #default="scope">
                        {{ scope.row.triggerBehavior }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="level" width="80" :label="$t('index.alarmLevel')">
                      <template #default="scope">
                        <el-tag v-if="scope.row.level == 0" class="ml-2" type="info">{{ $t('index.tip') }}</el-tag>
                        <el-tag v-if="scope.row.level == 1" class="ml-2" type="warning">{{
                          $t('index.warning')
                        }}</el-tag>
                        <el-tag v-if="scope.row.level == 2" class="ml-2" type="danger">{{ $t('index.danger') }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="createTime" :label="$t('index.alarmTime')">
                      <template #default="scope">
                        {{ scope.row.createTime }}
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </el-card>
            </el-col>
            <el-col :span="24">
              <el-card class="box-card">
                <template #header>
                  <div class="card-header">
                    <span>{{ $t('index.vpc.title') }}</span>
                    <small> ({{ $t('index.vpc.totalNum', { count: apiData.vpcList.totalNum }) }})</small>
                    <router-link to="/vpc" class="float-right text-blue-400 cursor-pointer text-sm">{{
                      $t('index.viewAll')
                    }}</router-link>
                  </div>
                </template>
                <div style="height: 26.7rem">
                  <tree :id="'treeId'" :cur-data="apiData.vpcList.vpcs" width="100%" :height="'26.7rem'" />
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import tree from './index/tree.vue';
import mainStore from '@/store/mainStore';

const mainStoreData = mainStore();
const { isAdmin } = mainStoreData;
const percentageNum: any = ref(0);
const apiData: any = ref({
  vm: '',
  userStorage: '',
  allStorage: '',
  network: '',
  vpcCount: '',
  subnetCount: '',
  vpcList: [],
});

const init = async () => {
  apiData.value.vm = await mainApi.vmsVmStats();
  apiData.value.network = await mainApi.networkNetStats();
  apiData.value.vpcCount = await mainApi.networkVpcCount();
  apiData.value.subnetCount = await mainApi.networkSubnetCount();
  apiData.value.vpcList = await mainApi.vpcList({
    page_num: 1,
    page_size: 10,
  });
};

const getStorage = () => {
  if (isAdmin) {
    mainApi.vmsAllStorageStats().then((res: any) => {
      apiData.value.allStorage = res;
      percentageNum.value = ((apiData.value.allStorage.used / apiData.value.allStorage.total) * 100).toFixed(2);
    });
  } else {
    mainApi.vmsUserStorageStats().then((res: any) => {
      apiData.value.userStorage = res;
    });
  }
};

const timer: any = ref(null);
const alarmInfoLoading = ref(false);
const tableData: any = ref([]);
const tableDataTotal: any = ref(0);
const sevenTime: any = ref([new Date(new Date().setDate(new Date().getDate() - 7)), new Date()]);

const getOperationAlarmInfosList = () => {
  alarmInfoLoading.value = true;
  const params: any = {
    start_time: sevenTime[0],
    end_time: sevenTime[1],
    phase_status: 0,
    page_num: 1,
    page_size: 10,
  };

  mainApi
    .operationAlarmInfosList(params)
    .then((res: any) => {
      alarmInfoLoading.value = false;
      tableData.value = res.alarmInfos;
      tableDataTotal.value = res.totalNum;
    })
    .catch((error: any) => {
      alarmInfoLoading.value = false;
    });
};

const getOperationAlarmInfosListTime = () => {
  const params: any = {
    start_time: sevenTime[0],
    end_time: sevenTime[1],
    phase_status: 0,
    page_num: 1,
    page_size: 10,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi.operationAlarmInfosList(params).then((res: any) => {
    tableData.value = res.alarmInfos;
    tableDataTotal.value = res.totalNum;
  });
};

onMounted(() => {
  init();
  getStorage();
  getOperationAlarmInfosList();
  timer.value = setInterval(async () => {
    getOperationAlarmInfosListTime();
  }, mainStoreData.listRefreshTime);
});

onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.indexMain {
  height: calc(100vh - 45px - 3rem);
  overflow-y: auto;
}

.el-progress {
  ::v-deep .el-progress-bar {
    .el-progress-bar__outer {
      border-radius: 0.5rem;

      .el-progress-bar__inner {
        border-radius: 0.5rem;
      }
    }
  }
}
</style>
