<template>
  <div class="devOpsAlertHomePage">
    <!-- <h5 class="mb-3 px-5 pt-2">
      <span class="text-lg">{{ $route.meta.title }}</span>
      <el-divider class="!my-2"></el-divider>
    </h5> -->
    <div class="py-3 px-4 mb-4 bg-white rounded-lg tableTop">
      <el-form :inline="true" label-width="0px" :size="mainStoreData.viewSize.main">
        <el-form-item label="">
          <span class="text-base">{{ $t('devOps.alert.form.alertMessage') }}</span>
        </el-form-item>
        <el-form-item class="float-right !mr-0">
          <router-link to="/devOps/alert">
            <el-button type="primary" class="w-24"> {{ $t('devOps.alert.form.alertSetting') }}</el-button>
          </router-link>
        </el-form-item>
      </el-form>
    </div>
    <div class="py-3 px-4 bg-white rounded-lg">
      <el-row>
        <el-col :span="14">
          <div v-if="operationAlarmStatisticsData.length > 0" style="height: 300px" class="rightLine">
            <bar :id="'alertBarId'" :cur-data="operationAlarmStatisticsData" width="100%" :height="'300px'" />
          </div>
        </el-col>

        <el-col :span="10">
          <div v-if="operationAlarmDistributionData" style="height: 300px">
            <pie
              :id="'alertPieId'"
              :cur-data="[
                { name: $t('devOps.alert.cpuUsage'), value: operationAlarmDistributionData.cpuUsage },
                { name: $t('devOps.alert.memoryUsage'), value: operationAlarmDistributionData.memUsage },
                { name: $t('devOps.alert.filesystemUsage'), value: operationAlarmDistributionData.filesystemUsage },
                { name: $t('devOps.alert.networkThroughput'), value: operationAlarmDistributionData.networkThroughput },
                { name: $t('devOps.alert.diskIops'), value: operationAlarmDistributionData.diskIops },
                { name: $t('devOps.alert.diskThroughput'), value: operationAlarmDistributionData.diskThroughput },
                { name: $t('devOps.alert.instanceOnline'), value: operationAlarmDistributionData.instanceOffline },
              ]"
              width="100%"
              :height="'300px'"
            />
          </div>
        </el-col>
        <el-col :span="24">
          <div class="py-3 mb-4">
            <el-form :model="form" :inline="true" :size="mainStoreData.viewSize.main">
              <el-form-item class="!mr-0">
                <el-button class="p-2" @click="onSubmit">
                  <i-ep-refresh />
                </el-button>
                <el-button type="primary" class="w-24" @click="batchChangeStatus(0, 2)">
                  {{ $t('devOps.alert.form.markAsRead') }}
                </el-button>
                <el-button type="primary" class="w-24" @click="batchChangeStatus(2, 0)">
                  {{ $t('devOps.alert.form.markAsUnread') }}
                </el-button>
              </el-form-item>
              <el-form-item label=" ">
                <el-input v-model="form.name" class="!w-50" :placeholder="$t('devOps.alert.form.messageContent')" />
              </el-form-item>
              <el-form-item label="">
                <el-select
                  v-model="form.resource_type"
                  class="!w-32"
                  :placeholder="$t('devOps.alert.form.selectResourceType')"
                  @change="onSubmit"
                >
                  <el-option :label="$t('devOps.alert.all')" :value="''" />
                  <el-option :label="$t('devOps.alert.virtualMachineInstance')" :value="0" />
                  <el-option :label="$t('devOps.alert.virtualMachineInstanceGroup')" :value="1" />
                  <el-option :label="$t('devOps.alert.computeNode')" :value="2" />
                </el-select>
              </el-form-item>
              <el-form-item label="">
                <el-date-picker
                  v-model="form.time"
                  class="!w-88"
                  type="datetimerange"
                  :shortcuts="shortcuts"
                  range-separator="-"
                  :start-placeholder="$t('devOps.alert.startTime')"
                  :end-placeholder="$t('devOps.alert.endTime')"
                  @change="onSubmit"
                />
              </el-form-item>
              <el-form-item class="float-right !mr-0">
                <el-button class="resetBtn w-24" @click="onReset">{{ $t('common.reset') }}</el-button>
                <el-button type="primary" class="w-24" @click="onSubmit">{{ $t('common.search') }}</el-button>
              </el-form-item>
            </el-form>
            <el-table
              v-loading="alarmInfoLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="tableData"
              class="!overflow-y-auto"
              stripe
              :scrollbar-always-on="false"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="45px" />
              <el-table-column prop="summeryInfo" width="450px" :label="$t('devOps.alert.form.alertContent')">
                <template #default="scope">
                  <span class="block">{{ scope.row.summeryInfo }}</span>
                  <div class="leading-tight">
                    <small>({{ scope.row.detailInfo }})</small>
                  </div>
                </template>
              </el-table-column>
              <!-- <el-table-column prop="detailInfo"
                               label="消息内容">
                <template #default="scope">
                  <span>{{ scope.row.detailInfo }}</span>
                </template>
              </el-table-column> -->
              <el-table-column prop="triggerBehavior" :label="$t('devOps.alert.form.triggerBehavior')">
                <template #default="scope">
                  {{ scope.row.triggerBehavior }}
                </template>
              </el-table-column>
              <el-table-column prop="level" :label="$t('devOps.alert.alertLevel')">
                <template #default="scope">
                  <el-tag v-if="scope.row.level == 0" class="ml-2" type="info">{{ $t('devOps.alert.warning') }}</el-tag>
                  <el-tag v-if="scope.row.level == 1" class="ml-2" type="warning">{{
                    $t('devOps.alert.serious')
                  }}</el-tag>
                  <el-tag v-if="scope.row.level == 2" class="ml-2" type="danger">{{
                    $t('devOps.alert.critical')
                  }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column
                prop="alarmCount"
                align="center"
                width="80px"
                :label="$t('devOps.alert.form.alarmCount')"
              >
                <template #default="scope">
                  {{ scope.row.alarmCount }}
                </template>
              </el-table-column>
              <el-table-column prop="resourceType" align="center" :label="$t('devOps.alert.form.resourceType')">
                <template #default="scope">
                  {{
                    scope.row.resourceType == 0
                      ? $t('devOps.alert.virtualMachineInstance')
                      : scope.row.resourceType == 1
                      ? $t('devOps.alert.virtualMachineInstanceGroup')
                      : scope.row.resourceType == 2
                      ? $t('devOps.alert.computeNode')
                      : '-'
                  }}
                </template>
              </el-table-column>
              <el-table-column prop="phaseStatus" align="center" :label="$t('devOps.alert.form.status')">
                <template #default="scope">
                  <el-tag v-if="scope.row.phaseStatus == 0" class="ml-2" type="warning">{{
                    $t('devOps.alert.form.unread')
                  }}</el-tag>
                  <el-tag v-if="scope.row.phaseStatus == 1" class="ml-2" type="success">{{
                    $t('devOps.alert.form.recovered')
                  }}</el-tag>
                  <el-tag v-if="scope.row.phaseStatus == 2" class="ml-2" type="info">{{
                    $t('devOps.alert.form.read')
                  }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="interval" :label="$t('devOps.alert.form.alertInterval')">
                <template #default="scope">
                  <span>{{
                    scope.row.interval > 59
                      ? scope.row.interval / 60 + $t('devOps.alert.hourUnit')
                      : scope.row.interval + $t('devOps.alert.minuteUnit')
                  }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="$t('devOps.alert.form.alertTime')">
                <template #default="scope">
                  {{ scope.row.createTime }}
                </template>
              </el-table-column>
              <el-table-column :label="$t('common.operation')" width="120px">
                <template #default="scope">
                  <el-button
                    v-if="scope.row.phaseStatus == 0"
                    type="text"
                    link
                    class="w-24"
                    @click="changeStatus([scope.row.infoId], 2)"
                    >{{ $t('devOps.alert.form.markAsRead') }}</el-button
                  >
                  <el-button
                    v-if="scope.row.phaseStatus == 2"
                    type="text"
                    class="w-24"
                    @click="changeStatus([scope.row.infoId], 0)"
                    >{{ $t('devOps.alert.form.markAsUnread') }}</el-button
                  >
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-model:page_num="form.page_num"
              v-model:page-size="form.page_size"
              class="!py-4 !pr-8 float-right"
              :page-sizes="mainStoreData.page_sizes"
              :current-page="form.page_num"
              :small="true"
              layout="total, sizes, prev, pager, next, jumper"
              :total="form.total"
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
            />
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import bar from './echart/bar.vue';
import pie from './echart/pie.vue';

import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';

const { proxy }: any = getCurrentInstance();
const timer: any = ref('');
const alarmInfoLoading = ref(false);

const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();

const form = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  resource_type: '',
  time: [],
  page_size: mainStoreData.page_size,
  total: 0,
});
const tableData: any = ref([]);
const shortcuts = [
  {
    text: proxy.$t('devOps.alert.lastWeek'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.alert.lastMonth'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.alert.lastThreeMonths'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 90);
      return [start, end];
    },
  },
];

const multipleSelection: any = ref([]); // 多选
const handleSelectionChange = (val: any) => {
  console.log(val);
  multipleSelection.value = val;
};
const onReset = () => {
  // 重置查询
  form.name = '';
  form.resource_type = '';
  form.time = [];
  form.page_num = 1;
  getOperationAlarmInfosList();
};
const onSubmit = () => {
  // 提交查询
  form.page_num = 1;
  getOperationAlarmInfosList();
};
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  console.log(`${val} items per page`);
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  form.page_size = val;
  getOperationAlarmInfosList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  form.page_num = val;
  getOperationAlarmInfosList();
};
const batchChangeStatus = (nowStatus: number, status: number) => {
  // 批量标记为已读
  if (multipleSelection.value.length == 0) {
    ElMessage.warning(proxy.$t('devOps.alert.message.selectAlertInfo'));
    return;
  }
  const ids: any = [];
  multipleSelection.value.forEach((item: any) => {
    if (item.phaseStatus == nowStatus) {
      ids.push(item.infoId);
    }
  });
  changeStatus(ids, status);
};
const changeStatus = (ids: any, status: number) => {
  // 标记为已解决
  const params: any = {
    infoIds: ids,
    phaseStatus: status,
  };
  mainApi
    .operationAlarmMarkResolvedEdit(params)
    .then((res: any) => {
      getOperationAlarmInfosList();
    })
    .catch((err: any) => {
      console.log(err);
    });
};

const getOperationAlarmInfosList = () => {
  // 告警列表
  alarmInfoLoading.value = true;
  const dataTime: any = form.time || [];
  const params: any = {
    summery_info: form.name,
    resource_type: form.resource_type,
    start_time: dataTime[0] ? dataTime[0].getTime() : '',
    end_time: dataTime[1] ? dataTime[1].getTime() : '',
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi
    .operationAlarmInfosList(params)
    .then((res: any) => {
      alarmInfoLoading.value = false;
      tableData.value = res.alarmInfos;
      form.total = res.totalNum;
    })
    .catch((error: any) => {
      alarmInfoLoading.value = false;
    });
};
const getOperationAlarmInfosListTime = () => {
  // 告警循环列表
  const dataTime: any = form.time || [];
  const params: any = {
    summery_info: form.name,
    resource_type: form.resource_type,
    start_time: dataTime[0] ? dataTime[0].getTime() : '',
    end_time: dataTime[1] ? dataTime[1].getTime() : '',
    page_num: form.page_num,
    page_size: form.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi.operationAlarmInfosList(params).then((res: any) => {
    tableData.value = res.alarmInfos;
    form.total = res.totalNum;
  });
};

// 图标信息
const operationAlarmDistributionData: any = ref({});
const operationAlarmStatisticsData: any = ref([]);
const getOperationAlarmDistribution = () => {
  mainApi
    .operationAlarmDistribution()
    .then((res: any) => {
      operationAlarmDistributionData.value = res;
    })
    .catch((error: any) => {});
};
const getOperationAlarmStatistics = () => {
  mainApi
    .operationAlarmStatistics()
    .then((res: any) => {
      operationAlarmStatisticsData.value = res;
    })
    .catch((error: any) => {});
};
onMounted(() => {
  getOperationAlarmDistribution(); // 请求图表信息
  getOperationAlarmStatistics(); // 请求图表信息
  getOperationAlarmInfosList(); // 请求告警列表
  // timer.value = setInterval(async () => {
  //   getOperationAlarmInfosListTime(); //请求告警循环列表
  // }, mainStoreData.listRefreshTime);
});
onUnmounted(() => {
  clearInterval(timer.value);
});
</script>

<style lang="scss" scoped>
.devOpsAlertHomePage {
  .rightLine {
    position: relative;
    &:after {
      content: '';
      position: absolute;
      top: 10%;
      right: 0;
      width: 1px;
      height: 80%;
      background: #ebeef5;
    }
  }
}
</style>
