<template>
  <div class="eipPoolAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="addAlertForm"
      v-loading="loading"
      :size="mainStoreData.viewSize.main"
      :model="form"
      :rules="rules"
      label-width="120px"
      :element-loading-text="$t('common.loading')"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('devOps.alert.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('devOps.alert.form.name') + ':'">
            <span>
              {{ form.name }}
            </span>
          </el-form-item>
          <el-form-item :label="$t('common.id') + ':'">
            <span>{{ form.ruleId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('devOps.alert.form.description') + ':'">
            <span>
              {{ form.description }}
            </span>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('devOps.alert.monitorContent') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('devOps.alert.form.resourceType') + ':'">
            <span v-if="form.resourceType == 0">{{ $t('devOps.alert.virtualMachineInstance') }}</span>
            <span v-if="form.resourceType == 1">{{ $t('devOps.alert.virtualMachineInstanceGroup') }}</span>
            <span v-if="form.resourceType == 2">{{ $t('devOps.alert.computeNode') }}</span>
          </el-form-item>
          <el-form-item :label="$t('devOps.alert.form.alarmElement') + ':'">
            <span v-if="form.alarmElement == 0">{{ $t('devOps.alert.cpuUsage') }}</span>
            <span v-if="form.alarmElement == 1">{{ $t('devOps.alert.memoryUsage') }}</span>
            <span v-if="form.alarmElement == 3">{{ $t('devOps.alert.filesystemUsage') }}</span>
            <span v-if="form.alarmElement == 4">{{ $t('devOps.alert.networkThroughput') }}</span>
            <span v-if="form.alarmElement == 5">{{ $t('devOps.alert.diskIops') }}</span>
            <span v-if="form.alarmElement == 6">{{ $t('devOps.alert.diskThroughput') }}</span>
            <span v-if="form.alarmElement == 7">{{ $t('devOps.alert.instanceOnline') }}</span>
          </el-form-item>
          <el-form-item
            :label="$t('devOps.alert.form.monitorObject') + ':'"
            :prop="
              form.resourceType == 0
                ? 'resourceVmList'
                : form.resourceType == 1
                ? 'resourceGroupList'
                : form.resourceType == 2
                ? 'resourceNodeList'
                : ''
            "
          >
            <div v-if="form.resourceType == 0">
              <el-table
                :size="mainStoreData.viewSize.main"
                :data="form.resourceVmList"
                class="!overflow-y-auto w-200"
                stripe
                :scrollbar-always-on="false"
              >
                <el-table-column prop="date" :label="$t('devOps.alert.form.name')">
                  <template #default="scope">
                    <!-- 新窗口打开 -->
                    <router-link :to="'/vm/' + scope.row.instanceId" target="_blank">
                      <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                    </router-link>
                  </template>
                </el-table-column>

                <el-table-column prop="instanceId" label="ID" />
              </el-table>
            </div>
            <div v-if="form.resourceType == 1">
              <el-table
                :size="mainStoreData.viewSize.main"
                :data="form.resourceGroupList"
                class="!overflow-y-auto w-200"
                stripe
                row-key="id"
                lazy
                :load="load"
                :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
              >
                <el-table-column prop="name" :label="$t('devOps.alert.form.name')">
                  <template #default="scope">
                    <router-link v-if="scope.row.type == 1" :to="'/vmGroup/' + scope.row.id" target="_blank">
                      <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                    </router-link>
                    <router-link v-if="scope.row.type == 0" :to="'/vm/' + scope.row.id" target="_blank">
                      <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                    </router-link>
                  </template>
                </el-table-column>
                <el-table-column prop="type" width="120" :label="$t('devOps.alert.form.type')">
                  <template #default="scope">
                    <span v-if="scope.row.type == 1">{{ $t('devOps.alert.form.virtualMachineGroup') }}</span>
                    <span v-if="scope.row.type == 0">{{ $t('devOps.alert.form.virtualMachine') }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="id" label="ID" />
              </el-table>
            </div>
            <div v-if="form.resourceType == 2">
              <el-table
                :size="mainStoreData.viewSize.main"
                :data="form.resourceNodeList"
                class="!overflow-y-auto w-200"
                stripe
                :scrollbar-always-on="false"
              >
                <el-table-column prop="date" :label="$t('devOps.alert.form.name')">
                  <template #default="scope">
                    <!-- 新窗口打开 -->
                    <router-link :to="'/hypervisorNodes/' + scope.row.nodeId" target="_blank">
                      <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                    </router-link>
                  </template>
                </el-table-column>

                <el-table-column prop="nodeId" label="ID" />
              </el-table>
            </div>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('devOps.alert.alarm') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('devOps.alert.triggerRule') + ':'">
            <span v-if="form.comparison == 0">{{ $t('devOps.alert.lessThan') }}</span>
            <span v-if="form.comparison == 1">{{ $t('devOps.alert.lessThanOrEqual') }}</span>
            <span v-if="form.comparison == 2">{{ $t('devOps.alert.equal') }}</span>
            <span v-if="form.comparison == 3">{{ $t('devOps.alert.greaterThanOrEqual') }}</span>
            <span v-if="form.comparison == 4">{{ $t('devOps.alert.greaterThan') }}</span>

            <span>{{ form.alarmValue }}</span>

            <span v-if="form.alarmElement == 0 || form.alarmElement == 1 || form.alarmElement == 3">%</span>
            <span v-if="form.alarmElement == 4">MB/s</span>
            <span v-if="form.alarmElement == 5">req/s</span>
            <span v-if="form.alarmElement == 6">MB/s</span>
            <span v-if="form.alarmElement == 7">{{ $t('devOps.alert.instanceOfflineDuration') }}</span>
            <span v-if="form.alarmElement != 7">{{ $t('devOps.alert.duration') }}</span>
            <span>{{
              form.durationTime > 59
                ? form.durationTime / 60 + $t('devOps.alert.hourUnit')
                : form.durationTime + $t('devOps.alert.minuteUnit')
            }}</span>
          </el-form-item>
          <el-form-item :label="$t('devOps.alert.alertInterval') + ':'">
            <span>{{
              form.interval > 59
                ? form.interval / 60 + $t('devOps.alert.hourUnit')
                : form.interval + $t('devOps.alert.minuteUnit')
            }}</span>
          </el-form-item>
          <el-form-item :label="$t('devOps.alert.alertLevel') + ':'">
            <el-tag v-if="form.level == 0" type="info">{{ $t('devOps.alert.warning') }}</el-tag>
            <el-tag v-if="form.level == 1" type="warning">{{ $t('devOps.alert.serious') }}</el-tag>
            <el-tag v-if="form.level == 2" type="danger">{{ $t('devOps.alert.critical') }}</el-tag>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('devOps.alert.notice') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('devOps.alert.form.noticeObject') + ':'" prop="type">
            <div class="w-full">
              <span v-if="form.notice">{{ $t('devOps.alert.form.selectNoticeObject') }}</span>

              <span v-if="!form.notice">{{ $t('devOps.alert.form.noNotice') }}</span>
            </div>
          </el-form-item>
          <el-form-item v-if="form.notice" :label="$t('devOps.alert.form.noticeGroup') + ':'" prop="receiverListData">
            <div>
              <el-table
                :size="mainStoreData.viewSize.main"
                :data="form.receiverListData"
                class="!overflow-y-auto w-200"
                stripe
                row-key="id"
                lazy
                :load="noticeLoad"
                :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
              >
                <!-- <el-table-column type="expand">

                  <template #default="props">
                    <el-tag class="ml-2"
                            type="info"
                            v-for="(item,index) in props.row.children.contactInfos"
                            :key="index">
                      {{ item }}
                    </el-tag>

                  </template>
                </el-table-column> -->
                <el-table-column prop="name" :label="$t('devOps.alert.form.name')">
                  <template #default="scope">
                    <router-link v-if="scope.row.type == 1" :to="'/devOps/noticeGrop/' + scope.row.id" target="_blank">
                      <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                    </router-link>
                    <span v-else>{{ scope.row.name }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="type" width="120" :label="$t('devOps.alert.form.type')">
                  <template #default="scope">
                    <span v-if="scope.row.type == 1">{{ $t('devOps.alert.form.noticeGroup') }}</span>
                    <span v-if="scope.row.type == 0">{{ $t('devOps.alert.form.noticeObject') }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="id" label="ID" />
              </el-table>
            </div>
          </el-form-item>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import mainApi from '@/api/modules/main';
import mainStore from '@/store/mainStore';
import filtersFun from '@/utils/statusFun';

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const { proxy }: any = getCurrentInstance();

const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();
const loading = ref(false);
const addAlertForm = ref<any>();

const form: any = reactive({
  alarmElement: 0, // 告警元素
  alarmValue: 0, //
  comparison: 0, //
  description: '', // 描述
  durationTime: 10, // 持续时间
  interval: 10, // 间隔
  level: 0,
  name: '', // 名称
  ruleId: '', // 规则id
  notice: false,
  receiverList: [], // 接收人
  resourceIds: [], // 资源id
  resourceType: 0, // 资源类型

  resourceVmList: [], // 资源列表
  resourceGroupList: [], // 资源列表
  resourceNodeList: [], // 资源列表
  receiverListData: [], // 接收人列表
});

const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
  resourceVmList: [
    { required: true, message: proxy.$t('devOps.alert.validator.selectVirtualMachine'), trigger: 'change' },
  ],
  resourceGroupList: [
    { required: true, message: proxy.$t('devOps.alert.validator.selectVirtualMachineGroup'), trigger: 'change' },
  ],
  resourceNodeList: [
    { required: true, message: proxy.$t('devOps.alert.validator.selectComputeNode'), trigger: 'change' },
  ],
  alarmValue: [{ required: true, message: proxy.$t('devOps.alert.validator.inputAlertValue'), trigger: 'change' }],
  receiverListData: [
    { required: true, message: proxy.$t('devOps.alert.validator.selectNoticeGroup'), trigger: 'change' },
  ],
});

const goBack = () => {
  router.push('/devOps/alert');
};

const getDetail = () => {
  // 获取详情
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .operationAlarmRulesDetail(id)
    .then((res: any) => {
      form.alarmElement = res.alarmElement;
      form.alarmValue = res.alarmValue;
      form.comparison = res.comparison;
      form.description = res.description;
      form.durationTime = res.durationTime;
      form.interval = res.interval;
      form.level = res.level;
      form.name = res.name;
      form.ruleId = res.ruleId;
      form.notice = res.notice;
      form.resourceType = res.resourceType;
      form.receiverListData = res.receiverDetailsResps ? res.receiverDetailsResps : [];
      form.receiverListData.forEach((item: any) => {
        item.id = item.receiverId;
        item.hasChildren = true;
        item.type = 1;
      });
      if (form.resourceType == 0) {
        form.resourceVmList = res.resourceDetailsResps ? res.resourceDetailsResps : [];
        form.resourceVmList.forEach((item: any) => {
          item.instanceId = item.resourceId;
        });
      } else if (form.resourceType == 1) {
        form.resourceGroupList = res.resourceDetailsResps ? res.resourceDetailsResps : [];
        form.resourceGroupList.forEach((item: any) => {
          item.id = item.resourceId;
          item.hasChildren = true;
          item.type = 1;
        });
      } else if (form.resourceType == 2) {
        form.resourceNodeList = res.resourceDetailsResps ? res.resourceDetailsResps : [];
        form.resourceNodeList.forEach((item: any) => {
          item.nodeId = item.resourceId;
        });
      }

      // getVmsInstabcesDetail(res.instanceGroupId);
    })
    .catch((error: any) => {});
};

const noticeLoad = (row: any, treeNode: unknown, resolve: (date: any) => void) => {
  mainApi
    .operationReceiversDetail(row.receiverId)
    .then((res: any) => {
      if (res.contactInfos && res.contactInfos.length > 0) {
        const data: any = [];
        res.contactInfos.forEach((item: any) => {
          data.push({
            name: item,
            type: 0,
          });
        });
        resolve(data);
      } else {
        resolve([]);
      }
    })
    .catch((error: any) => {});
};
const load = (row: any, treeNode: unknown, resolve: (date: any) => void) => {
  mainApi
    .vmsInstabcesList({ instance_group_id: row.id })
    .then((res: any) => {
      if (res.vmInstancesInfo && res.vmInstancesInfo.length > 0) {
        res.vmInstancesInfo.forEach((item: any) => {
          item.id = item.instanceId;
          item.type = 0;
        });
        resolve(res.vmInstancesInfo);
      } else {
        resolve([]);
      }
    })
    .catch((error: any) => {});
};
onMounted(() => {
  getDetail();
});
</script>

<style lang="scss" scoped>
.eipPoolAddPage {
}
</style>
