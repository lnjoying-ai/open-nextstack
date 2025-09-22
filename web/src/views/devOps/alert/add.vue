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
          <el-form-item :label="$t('devOps.alert.form.name') + ':'" prop="name">
            <el-input v-model="form.name" class="!w-60" :placeholder="$t('devOps.alert.form.inputName')" />
          </el-form-item>
          <el-form-item :label="$t('devOps.alert.form.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="2"
              :placeholder="$t('devOps.alert.form.inputDescription')"
            />
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
          <el-form-item :label="$t('devOps.alert.form.resourceType') + ':'" prop="resourceType">
            <el-select
              v-model="form.resourceType"
              class="!w-50"
              :placeholder="$t('devOps.alert.form.selectResourceType')"
              @change="changeResourceType"
            >
              <el-option :label="$t('devOps.alert.virtualMachineInstance')" :value="0" />
              <el-option :label="$t('devOps.alert.virtualMachineInstanceGroup')" :value="1" />
              <el-option :label="$t('devOps.alert.computeNode')" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('devOps.alert.form.alarmElement') + ':'" prop="alarmElement">
            <el-select
              v-model="form.alarmElement"
              class="!w-50"
              :placeholder="$t('devOps.alert.form.selectAlarmElement')"
            >
              <!-- CPU_USAGE -->
              <el-option :label="$t('devOps.alert.cpuUsage')" :value="0" />
              <!-- MEM_USAGE -->
              <el-option :label="$t('devOps.alert.memoryUsage')" :value="1" />
              <!-- FILESYSTEM_USAGE -->
              <el-option :label="$t('devOps.alert.filesystemUsage')" :value="3" />
              <!-- NETWORK_THROUGHPUT -->
              <el-option :label="$t('devOps.alert.networkThroughput')" :value="4" />
              <!-- DISK_IOPS -->
              <el-option :label="$t('devOps.alert.diskIops')" :value="5" />
              <!-- DISK_THROUGHPUT -->
              <el-option :label="$t('devOps.alert.diskThroughput')" :value="6" />
              <el-option :label="$t('devOps.alert.instanceOnline')" :value="7" />
            </el-select>
            <!-- <el-select v-model="form.type"
                       class="!w-50"
                       placeholder="请选择报警条目">
              <el-option label="使用率"
                         value="0" />
              <el-option label="使用量"
                         value="1" />

            </el-select> -->
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
              <div class="text-right block w-full">
                <el-button type="primary" class="float-right" :size="mainStoreData.viewSize.main" @click="addVm()">
                  {{ $t('devOps.alert.addVirtualMachine') }}
                </el-button>
              </div>
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

                <el-table-column :label="$t('common.operation')" width="120">
                  <template #default="scope">
                    <el-button type="text" :size="mainStoreData.viewSize.listSet" @click="toVmDelete(scope.$index)">
                      {{ $t('common.remove') }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-if="form.resourceType == 1">
              <div class="text-right block w-full">
                <el-button type="primary" class="float-right" :size="mainStoreData.viewSize.main" @click="addVmGroup()">
                  {{ $t('devOps.alert.addVirtualMachineGroup') }}
                </el-button>
              </div>
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

                <el-table-column :label="$t('common.operation')" width="120">
                  <template #default="scope">
                    <el-button
                      v-if="scope.row.type == 1"
                      type="text"
                      :size="mainStoreData.viewSize.listSet"
                      @click="toVmGroupDelete(scope.$index)"
                    >
                      {{ $t('common.remove') }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div v-if="form.resourceType == 2">
              <div class="text-right block w-full">
                <el-button type="primary" class="float-right" :size="mainStoreData.viewSize.main" @click="addNode()">
                  {{ $t('devOps.alert.addComputeNode') }}
                </el-button>
              </div>
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

                <el-table-column :label="$t('common.operation')" width="120">
                  <template #default="scope">
                    <el-button type="text" :size="mainStoreData.viewSize.listSet" @click="toNodeDelete(scope.$index)">
                      {{ $t('common.remove') }}
                    </el-button>
                  </template>
                </el-table-column>
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
          <el-form-item
            :label="$t('devOps.alert.triggerRule') + ':'"
            :prop="form.alarmElement != 7 ? 'alarmValue' : ''"
          >
            <el-select
              v-if="form.alarmElement != 7"
              v-model="form.comparison"
              class="!w-50"
              :placeholder="$t('devOps.alert.selectTriggerRule')"
            >
              <el-option :label="$t('devOps.alert.lessThan')" :value="0" />
              <el-option :label="$t('devOps.alert.lessThanOrEqual')" :value="1" />
              <el-option :label="$t('devOps.alert.equal')" :value="2" />
              <el-option :label="$t('devOps.alert.greaterThanOrEqual')" :value="3" />
              <el-option :label="$t('devOps.alert.greaterThan')" :value="4" />
            </el-select>
            <el-input
              v-if="form.alarmElement != 7"
              v-model="form.alarmValue"
              class="!w-40 inputNumber"
              type="number"
              :placeholder="$t('devOps.alert.inputValue')"
            >
              <template #append>
                <span v-if="form.alarmElement == 0 || form.alarmElement == 1 || form.alarmElement == 3">%</span>
                <span v-if="form.alarmElement == 4">MB/s</span>
                <span v-if="form.alarmElement == 5">req/s</span>
                <span v-if="form.alarmElement == 6">MB/s</span>
              </template>
            </el-input>

            <span v-if="form.alarmElement == 7" class="mr-2">{{ $t('devOps.alert.instanceOfflineDuration') }}</span>
            <span v-if="form.alarmElement != 7" class="mx-2">{{ $t('devOps.alert.duration') }}</span>
            <el-select v-model="form.durationTime" class="!w-25" :placeholder="$t('devOps.alert.selectDuration')">
              <el-option :label="'3' + $t('devOps.alert.minute')" :value="3" />
              <el-option :label="'5' + $t('devOps.alert.minute')" :value="5" />
              <el-option :label="'10' + $t('devOps.alert.minute')" :value="10" />
              <el-option :label="'15' + $t('devOps.alert.minute')" :value="15" />
              <el-option :label="'20' + $t('devOps.alert.minute')" :value="20" />
              <el-option :label="'30' + $t('devOps.alert.minute')" :value="30" />
              <el-option :label="'40' + $t('devOps.alert.minute')" :value="40" />
              <el-option :label="'50' + $t('devOps.alert.minute')" :value="50" />
              <el-option :label="'1' + $t('devOps.alert.hour')" :value="60" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('devOps.alert.form.alertInterval') + ':'" prop="interval">
            <el-select v-model="form.interval" class="!w-50" :placeholder="$t('devOps.alert.selectAlertInterval')">
              <el-option :label="'10' + $t('devOps.alert.minute')" :value="10" />
              <el-option :label="'20' + $t('devOps.alert.minute')" :value="20" />
              <el-option :label="'30' + $t('devOps.alert.minute')" :value="30" />
              <el-option :label="'40' + $t('devOps.alert.minute')" :value="40" />
              <el-option :label="'50' + $t('devOps.alert.minute')" :value="50" />
              <el-option :label="'1' + $t('devOps.alert.hour')" :value="60" />
              <el-option :label="'2' + $t('devOps.alert.hour')" :value="120" />
              <el-option :label="'3' + $t('devOps.alert.hour')" :value="180" />
              <el-option :label="'4' + $t('devOps.alert.hour')" :value="240" />
              <el-option :label="'12' + $t('devOps.alert.hour')" :value="720" />
              <el-option :label="'24' + $t('devOps.alert.hour')" :value="1440" />
            </el-select>
          </el-form-item>
          <el-form-item :label="$t('devOps.alert.form.alertLevel') + ':'" prop="level">
            <el-select v-model="form.level" class="!w-50" :placeholder="$t('devOps.alert.selectAlertLevel')">
              <el-option :label="$t('devOps.alert.warning')" :value="0" />
              <el-option :label="$t('devOps.alert.serious')" :value="1" />
              <el-option :label="$t('devOps.alert.critical')" :value="2" />
            </el-select>
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
              <el-radio-group v-model="form.notice">
                <el-radio :label="false" :value="false">{{ $t('devOps.alert.form.noNotice') }}</el-radio>
                <el-radio :label="true" :value="true">{{ $t('devOps.alert.form.selectNoticeObject') }}</el-radio>
              </el-radio-group>
            </div>
          </el-form-item>
          <el-form-item v-if="form.notice" :label="$t('devOps.alert.form.noticeGroup') + ':'" prop="receiverListData">
            <div>
              <div class="text-right block w-full">
                <el-button
                  type="primary"
                  class="float-right"
                  :size="mainStoreData.viewSize.main"
                  @click="addNoticeGrop()"
                >
                  {{ $t('devOps.alert.form.addNoticeGroup') }}
                </el-button>
              </div>
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
                <el-table-column prop="id" width="290" label="ID" />

                <el-table-column :label="$t('common.operation')" width="120">
                  <template #default="scope">
                    <el-button
                      v-if="scope.row.type == 1"
                      type="text"
                      :size="mainStoreData.viewSize.listSet"
                      @click="toNoticeGropDelete(scope.$index)"
                    >
                      {{ $t('common.remove') }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </el-form-item>
        </div>
      </el-card>

      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toAlertAdd()">{{ $t('common.createNow') }}</el-button>
        </div>
      </el-card>
    </el-form>
    <el-dialog
      v-model="dialogVm"
      v-loading="vmLoading"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="vmHandleClose"
      :title="$t('devOps.alert.addVirtualMachine')"
    >
      <div class="block overflow-hidden">
        <el-row :gutter="10">
          <el-col :span="24">
            <el-table
              ref="multipleTableRef"
              v-loading="vmLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="vmTableList"
              max-height="calc(100% - 3rem)"
              class="!overflow-y-auto hypervisorNodesDialog"
              stripe
              :scrollbar-always-on="false"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span v-if="JSON.stringify(form.resourceVmList).includes(scope.row.instanceId)">
                    <span class="w-3 h-3 block border rounded-sm border-gray-300 bg-gray-300 text-base text-center">
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                  <span v-else>
                    <span
                      v-if="!nowCheckVmIncludes(scope.row)"
                      class="w-3 h-3 block border rounded-sm border-gray-300"
                      @click="vmHandleCheckChange(scope.row, true)"
                    ></span>
                    <span
                      v-else
                      class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                      @click="vmHandleCheckChange(scope.row, false)"
                    >
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="date" :label="$t('devOps.alert.form.vm.name')">
                <template #default="scope">
                  <router-link :to="'/vm/' + scope.row.instanceId" target="_blank">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>
              <el-table-column prop="hostname" :label="$t('devOps.alert.form.vm.hostname')" />
              <el-table-column prop="portInfo.ipAddress" :label="$t('devOps.alert.form.vm.ip')">
                <template #default="scope">
                  <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="vpcInfo.cidr" :label="$t('devOps.alert.form.vm.networkAddress')">
                <template #default="scope">
                  <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="eip" :label="$t('devOps.alert.form.vm.eip')">
                <template #default="scope">
                  <span v-if="!scope.row.boundPhaseStatus">{{ '-' }}</span>
                  <span v-if="scope.row.boundType && scope.row.boundType === 'port'">
                    <span v-if="scope.row.boundPhaseStatus == 82">{{ scope.row.eip || '-' }}</span>
                    <el-tag
                      v-else
                      :size="mainStoreData.viewSize.tagStatus"
                      :type="filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'tag')"
                      >{{ filtersFun.getVmToEipStatus(scope.row.boundPhaseStatus, 'status') }}</el-tag
                    >
                  </span>
                  <span v-if="scope.row.boundType && scope.row.boundType === 'nat'">
                    <span v-if="scope.row.boundPhaseStatus == 7">
                      <span v-if="scope.row.eip">
                        <el-tag :size="mainStoreData.viewSize.tagStatus">{{ $t('devOps.alert.form.vm.nat') }}</el-tag>
                        {{ scope.row.eip }}
                      </span>
                      <span v-else>-</span>
                    </span>
                    <el-tag
                      v-else
                      :size="mainStoreData.viewSize.tagStatus"
                      :type="filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'tag')"
                      >{{ filtersFun.getNatStatus(scope.row.boundPhaseStatus, 'status') }}</el-tag
                    >
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="phaseStatus" :label="$t('devOps.alert.form.vm.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>
              <el-table-column prop="imageInfo.name" :label="$t('devOps.alert.form.vm.system')" />
              <el-table-column prop="createTime" :label="$t('devOps.alert.form.vm.createTime')" />
            </el-table>
            <el-pagination
              v-model:page_num="vmForm.page_num"
              v-model:page-size="vmForm.page_size"
              class="!pt-4 !pr-8 float-right"
              :page-sizes="[10]"
              :current-page="vmForm.page_num"
              :small="true"
              layout="total, prev, pager, next, jumper"
              :total="vmForm.total"
              @size-change="vmHandleSizeChange"
              @current-change="vmHandleCurrentChange"
            />
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="vmHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toAddVm()">{{ $t('common.add') }}</el-button>
      </div>
    </el-dialog>
    <el-dialog
      v-model="dialogVmGroup"
      v-loading="vmGroupLoading"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="vmGroupHandleClose"
      :title="$t('devOps.alert.addVirtualMachineGroup')"
    >
      <div class="block overflow-hidden">
        <el-row :gutter="10">
          <el-col :span="24">
            <el-table
              ref="multipleTableRef"
              v-loading="vmGroupLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="vmGroupTableList"
              max-height="calc(100% - 3rem)"
              class="!overflow-y-auto hypervisorNodesDialog"
              stripe
              :scrollbar-always-on="false"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span v-if="JSON.stringify(form.resourceGroupList).includes(scope.row.instanceGroupId)">
                    <span class="w-3 h-3 block border rounded-sm border-gray-300 bg-gray-300 text-base text-center">
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                  <span v-else>
                    <span
                      v-if="!nowCheckVmGroupIncludes(scope.row)"
                      class="w-3 h-3 block border rounded-sm border-gray-300"
                      @click="vmGroupHandleCheckChange(scope.row, true)"
                    ></span>
                    <span
                      v-else
                      class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                      @click="vmGroupHandleCheckChange(scope.row, false)"
                    >
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                </template>
              </el-table-column>

              <el-table-column prop="name" :label="$t('devOps.alert.form.vmGroup.name')">
                <template #default="scope">
                  <router-link :to="'/vmGroup/' + scope.row.instanceGroupId" target="_blank">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>
              <el-table-column prop="description" :label="$t('devOps.alert.form.vmGroup.description')">
                <template #default="scope">
                  {{ scope.row.description }}
                </template>
              </el-table-column>
              <el-table-column prop="instanceCount" :label="$t('devOps.alert.form.vmGroup.instanceCount')">
                <template #default="scope">
                  {{ scope.row.instanceCount }}
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="$t('devOps.alert.form.vmGroup.createTime')" />
            </el-table>
            <el-pagination
              v-model:page_num="vmGroupForm.page_num"
              v-model:page-size="vmGroupForm.page_size"
              class="!pt-4 !pr-8 float-right"
              :page-sizes="[10]"
              :current-page="vmGroupForm.page_num"
              :small="true"
              layout="total, prev, pager, next, jumper"
              :total="vmGroupForm.total"
              @size-change="vmGroupHandleSizeChange"
              @current-change="vmGroupHandleCurrentChange"
            />
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="vmGroupHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toAddVmGroup()">{{ $t('common.add') }}</el-button>
      </div>
    </el-dialog>

    <el-dialog
      v-model="dialogNode"
      v-loading="nodeLoading"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="nodeHandleClose"
      :title="$t('devOps.alert.addComputeNode')"
    >
      <div class="block overflow-hidden">
        <el-row :gutter="10">
          <el-col :span="24">
            <el-table
              ref="multipleTableRef"
              v-loading="nodeLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="nodeTableList"
              max-height="calc(100% - 3rem)"
              class="!overflow-y-auto hypervisorNodesDialog"
              stripe
              :scrollbar-always-on="false"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span v-if="JSON.stringify(form.resourceNodeList).includes(scope.row.nodeId)">
                    <span class="w-3 h-3 block border rounded-sm border-gray-300 bg-gray-300 text-base text-center">
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                  <span v-else>
                    <span
                      v-if="!nowCheckNodeIncludes(scope.row)"
                      class="w-3 h-3 block border rounded-sm border-gray-300"
                      @click="nodeHandleCheckChange(scope.row, true)"
                    ></span>
                    <span
                      v-else
                      class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                      @click="nodeHandleCheckChange(scope.row, false)"
                    >
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="date" :label="$t('devOps.alert.form.computeNode.name')">
                <template #default="scope">
                  <router-link :to="'/hypervisorNodes/' + scope.row.nodeId" target="_blank">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>
              <el-table-column prop="hostname" :label="$t('devOps.alert.form.computeNode.hostname')" />
              <el-table-column prop="manageIp" :label="$t('devOps.alert.form.computeNode.manageIp')" />
              <el-table-column :label="$t('devOps.alert.form.computeNode.cpu')">
                <template #default="scope">
                  <div v-if="scope.row.cpuLogCount">
                    <p>
                      {{ scope.row.cpuLogCount - scope.row.usedCpuSum }}{{ $t('common.core') }}/{{
                        scope.row.cpuLogCount
                      }}{{ $t('common.core') }}
                    </p>
                    <p>{{ scope.row.cpuModel }}</p>
                  </div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column :label="$t('devOps.alert.form.computeNode.gpu')">
                <template #default="scope">
                  <div v-if="scope.row.gpuTotal">
                    <p>{{ scope.row.availableGpuCount }}/{{ scope.row.gpuTotal }}</p>
                    <p>{{ scope.row.gpuName }}</p>
                  </div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column :label="$t('devOps.alert.form.computeNode.memory')">
                <template #default="scope">
                  <div v-if="scope.row.memTotal">
                    {{ scope.row.memTotal - scope.row.usedMemSum }}GB/{{ scope.row.memTotal }}GB
                  </div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column :label="$t('devOps.alert.form.computeNode.ibNetwork')">
                <template #default="scope">
                  <div v-if="scope.row.ibTotal">{{ scope.row.availableIbCount }}/{{ scope.row.ibTotal }}</div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column prop="phaseStatus" :label="$t('devOps.alert.form.computeNode.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="$t('devOps.alert.form.computeNode.createTime')" />
            </el-table>
            <el-pagination
              v-model:page_num="nodeForm.page_num"
              v-model:page-size="nodeForm.page_size"
              class="!pt-4 !pr-8 float-right"
              :page-sizes="[10]"
              :current-page="nodeForm.page_num"
              :small="true"
              layout="total, prev, pager, next, jumper"
              :total="nodeForm.total"
              @size-change="nodeHandleSizeChange"
              @current-change="nodeHandleCurrentChange"
            />
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="nodeHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toAddNode()">{{ $t('common.add') }}</el-button>
      </div>
    </el-dialog>
    <el-dialog
      v-model="dialogNoticeGrop"
      v-loading="noticeGropLoading"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="noticeGropHandleClose"
      :title="$t('devOps.alert.form.noticeGroupForm.title')"
    >
      <div class="block overflow-hidden">
        <el-row :gutter="10">
          <el-col :span="24">
            <el-table
              ref="multipleTableRef"
              v-loading="noticeGropLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="noticeGropTableList"
              max-height="calc(100% - 3rem)"
              class="!overflow-y-auto hypervisorNodesDialog"
              stripe
              :scrollbar-always-on="false"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span v-if="JSON.stringify(form.receiverListData).includes(scope.row.receiverId)">
                    <span class="w-3 h-3 block border rounded-sm border-gray-300 bg-gray-300 text-base text-center">
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                  <span v-else>
                    <span
                      v-if="!nowCheckNoticeGropIncludes(scope.row)"
                      class="w-3 h-3 block border rounded-sm border-gray-300"
                      @click="noticeGropHandleCheckChange(scope.row, true)"
                    ></span>
                    <span
                      v-else
                      class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                      @click="noticeGropHandleCheckChange(scope.row, false)"
                    >
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                </template>
              </el-table-column>

              <el-table-column prop="name" :label="$t('devOps.alert.form.noticeGroupForm.name')">
                <template #default="scope">
                  <router-link :to="'/devOps/noticeGrop/' + scope.row.receiverId" target="_blank">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>
              <el-table-column prop="description" :label="$t('devOps.alert.form.noticeGroupForm.description')">
                <template #default="scope">
                  {{ scope.row.description }}
                </template>
              </el-table-column>
              <el-table-column prop="type" :label="$t('devOps.alert.form.noticeGroupForm.type')">
                <template #default="scope">
                  <span v-if="scope.row.type == 0">{{ $t('devOps.alert.form.noticeGroupForm.emailNotice') }}</span>
                  <span v-if="scope.row.type == 1">{{ $t('devOps.alert.form.noticeGroupForm.smsNotice') }}</span>
                  <span v-if="scope.row.type == 2">{{ $t('devOps.alert.form.noticeGroupForm.phoneNotice') }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="contactCount" :label="$t('devOps.alert.form.noticeGroupForm.contactCount')">
                <template #default="scope">
                  {{ scope.row.contactCount }}
                </template>
              </el-table-column>
              <el-table-column prop="contactInfo" :label="$t('devOps.alert.form.noticeGroupForm.noticeObject')">
                <template #default="scope">
                  {{ scope.row.contactInfo }}
                </template>
              </el-table-column>

              <el-table-column prop="createTime" :label="$t('devOps.alert.form.noticeGroupForm.createTime')" />
            </el-table>
            <el-pagination
              v-model:page_num="noticeGropForm.page_num"
              v-model:page-size="noticeGropForm.page_size"
              class="!pt-4 !pr-8 float-right"
              :page-sizes="[10]"
              :current-page="noticeGropForm.page_num"
              :small="true"
              layout="total, prev, pager, next, jumper"
              :total="noticeGropForm.total"
              @size-change="noticeGropHandleSizeChange"
              @current-change="noticeGropHandleCurrentChange"
            />
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="noticeGropHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toAddNoticeGrop()">{{ $t('common.add') }}</el-button>
      </div>
    </el-dialog>
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
  resourceVmList: [{ required: true, message: proxy.$t('devOps.alert.validator.selectVirtualMachine') }],
  resourceGroupList: [{ required: true, message: proxy.$t('devOps.alert.validator.selectVirtualMachineGroup') }],
  resourceNodeList: [{ required: true, message: proxy.$t('devOps.alert.validator.selectComputeNode') }],
  alarmValue: [{ required: true, message: proxy.$t('devOps.alert.validator.inputAlertValue') }],
  receiverListData: [{ required: true, message: proxy.$t('devOps.alert.validator.selectNoticeGroup') }],
});

const goBack = () => {
  router.push('/devOps/alert');
};
const resetForm = () => {
  // 重置
  addAlertForm.value.resetFields();
};
const toAlertAdd = () => {
  // 报警器add
  loading.value = true;

  addAlertForm.value.validate(async (valid: any) => {
    if (valid) {
      const formData = JSON.parse(JSON.stringify(form));
      const data = {
        alarmElement: formData.alarmElement,
        alarmValue: formData.alarmValue,
        comparison: formData.comparison,
        description: formData.description,
        durationTime: formData.durationTime,
        interval: formData.interval,
        level: formData.level,
        name: formData.name,
        notice: formData.notice,
        receiverList: [],
        resourceIds: [],
        resourceType: formData.resourceType,
      };
      if (data.notice) {
        data.receiverList = formData.receiverListData.map((item: any) => {
          return item.receiverId;
        });
      }
      if (data.resourceType == 0) {
        data.resourceIds = formData.resourceVmList.map((item: any) => {
          return item.instanceId;
        });
      } else if (data.resourceType == 1) {
        data.resourceIds = formData.resourceGroupList.map((item: any) => {
          return item.instanceGroupId;
        });
      } else if (data.resourceType == 2) {
        data.resourceIds = formData.resourceNodeList.map((item: any) => {
          return item.nodeId;
        });
      }
      mainApi
        .operationAlarmRulesAdd(data)
        .then((res: any) => {
          loading.value = false;
          resetForm();
          proxy.$emit('closeDrawer');
        })
        .catch((error: any) => {
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
const resourceTable: any = ref([]);

const changeResourceType = (val: any) => {
  if (val == 0) {
  } else if (val == 1) {
  } else {
    resourceTable.value = [];
  }
};
// 虚拟机 start
const nowCheckVm: any = ref([]); // 已选虚拟机列表
const vmTableList: any = ref([]); // 虚拟机列表
const dialogVm = ref(false); // 虚拟机弹窗
const vmLoading = ref(false); // 虚拟机弹窗loading
const vmForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});
const vmHandleClose = () => {
  nowCheckVm.value = [];
  vmForm.name = '';
  vmForm.page_num = 1;
  dialogVm.value = false;
};
const toAddVm = () => {
  // 添加虚拟机
  if (nowCheckVm.value.length === 0) {
    ElMessage.warning(proxy.$t('devOps.alert.message.selectVirtualMachine'));
    return;
  }
  form.resourceVmList = [...form.resourceVmList, ...nowCheckVm.value];
  vmHandleClose();
};
const toVmDelete = (index: number) => {
  // 删除虚拟机
  console.log(index);
  form.resourceVmList.splice(index, 1);
};
const nowCheckVmIncludes = (val: any) => {
  const data: any = nowCheckVm.value.filter((item: any) => item.instanceId === val.instanceId);
  return data.length > 0;
};
const vmHandleCheckChange = (val: any, type: boolean) => {
  if (type) {
    nowCheckVm.value.push(val);
  } else {
    nowCheckVm.value = nowCheckVm.value.filter((item: any) => item !== val);
  }
};
const getVmsInstabcesList = () => {
  // 虚拟机列表
  vmLoading.value = true;

  const params: any = {
    name: vmForm.name,
    page_num: vmForm.page_num,
    page_size: vmForm.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .vmsInstabcesList(params)
    .then((res: any) => {
      vmLoading.value = false;

      vmTableList.value = res.vmInstancesInfo;
      vmForm.total = res.totalNum;
    })
    .catch((error: any) => {
      vmLoading.value = false;
    });
};
const vmHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  vmForm.page_size = val;
  getVmsInstabcesList();
};
const vmHandleCurrentChange = (val: any) => {
  // 改变页码
  vmForm.page_num = val;
  getVmsInstabcesList();
};
const addVm = () => {
  // 点击添加虚拟机
  dialogVm.value = true;
  getVmsInstabcesList();
};

// 虚拟机组 start
const nowCheckVmGroup: any = ref([]); // 已选虚拟机组列表
const vmGroupTableList: any = ref([]); // 虚拟机组列表
const dialogVmGroup = ref(false); // 虚拟机组弹窗
const vmGroupLoading = ref(false); // 虚拟机组弹窗loading
const vmGroupForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});
const vmGroupHandleClose = () => {
  nowCheckVmGroup.value = [];
  vmGroupForm.name = '';
  vmGroupForm.page_num = 1;
  dialogVmGroup.value = false;
};
const toAddVmGroup = () => {
  // 添加虚拟机组
  if (nowCheckVmGroup.value.length === 0) {
    ElMessage.warning(proxy.$t('devOps.alert.message.selectVirtualMachineGroup'));
    return;
  }
  nowCheckVmGroup.value.forEach((item: any) => {
    item.id = item.instanceGroupId;
    item.hasChildren = true;
    item.type = 1;
  });
  form.resourceGroupList = [...form.resourceGroupList, ...nowCheckVmGroup.value];
  vmGroupHandleClose();
};
const toVmGroupDelete = (index: number) => {
  // 删除虚拟机组
  console.log(index);
  form.resourceGroupList.splice(index, 1);
};
const nowCheckVmGroupIncludes = (val: any) => {
  const data: any = nowCheckVmGroup.value.filter((item: any) => item.instanceGroupId === val.instanceGroupId);
  return data.length > 0;
};
const vmGroupHandleCheckChange = (val: any, type: boolean) => {
  if (type) {
    nowCheckVmGroup.value.push(val);
  } else {
    nowCheckVmGroup.value = nowCheckVmGroup.value.filter((item: any) => item !== val);
  }
};
const getVmGroupList = () => {
  // 虚拟机组列表
  vmGroupLoading.value = true;

  const params: any = {
    name: vmGroupForm.name,
    page_num: vmGroupForm.page_num,
    page_size: vmGroupForm.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .vmInstanceGroupsList(params)
    .then((res: any) => {
      vmGroupLoading.value = false;

      vmGroupTableList.value = res.instanceGroupInfos;
      vmGroupForm.total = res.totalNum;
    })
    .catch((error: any) => {
      vmGroupLoading.value = false;
    });
};
const vmGroupHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  vmGroupForm.page_size = val;
  getVmGroupList();
};
const vmGroupHandleCurrentChange = (val: any) => {
  // 改变页码
  vmGroupForm.page_num = val;
  getVmGroupList();
};
const addVmGroup = () => {
  // 点击添加虚拟机组
  dialogVmGroup.value = true;
  getVmGroupList();
};

// 计算节点 start
const nowCheckNode: any = ref([]); // 已选计算节点列表
const nodeTableList: any = ref([]); // 计算节点列表
const dialogNode = ref(false); // 计算节点弹窗
const nodeLoading = ref(false); // 计算节点弹窗loading
const nodeForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});
const nodeHandleClose = () => {
  nowCheckNode.value = [];
  nodeForm.name = '';
  nodeForm.page_num = 1;
  dialogNode.value = false;
};
const toAddNode = () => {
  // 添加计算节点
  if (nowCheckNode.value.length === 0) {
    ElMessage.warning(proxy.$t('devOps.alert.message.selectComputeNode'));
    return;
  }
  form.resourceNodeList = [...form.resourceNodeList, ...nowCheckNode.value];
  nodeHandleClose();
};
const toNodeDelete = (index: number) => {
  // 删除计算节点
  console.log(index);
  form.resourceNodeList.splice(index, 1);
};
const nowCheckNodeIncludes = (val: any) => {
  const data: any = nowCheckNode.value.filter((item: any) => item.nodeId === val.nodeId);
  return data.length > 0;
};
const nodeHandleCheckChange = (val: any, type: boolean) => {
  if (type) {
    nowCheckNode.value.push(val);
  } else {
    nowCheckNode.value = nowCheckNode.value.filter((item: any) => item !== val);
  }
};
const getNodesInstabcesList = () => {
  // 计算节点列表
  nodeLoading.value = true;
  const params: any = {
    name: nodeForm.name,
    page_num: nodeForm.page_num,
    page_size: nodeForm.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .vmsHypervisorNodesList(params)
    .then((res: any) => {
      nodeLoading.value = false;

      nodeTableList.value = res.nodeAllocationInfos;
      nodeForm.total = res.totalNum;
    })
    .catch((error: any) => {
      nodeLoading.value = false;
    });
};
const nodeHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  nodeForm.page_size = val;
  getNodesInstabcesList();
};
const nodeHandleCurrentChange = (val: any) => {
  // 改变页码
  nodeForm.page_num = val;
  getNodesInstabcesList();
};
const addNode = () => {
  // 点击添加计算节点
  dialogNode.value = true;
  getNodesInstabcesList();
};
// 通知组 start
const nowCheckNoticeGrop: any = ref([]); // 已选通知组列表
const noticeGropTableList: any = ref([]); // 通知组列表
const dialogNoticeGrop = ref(false); // 通知组弹窗
const noticeGropLoading = ref(false); // 通知组弹窗loading
const noticeGropForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});
const noticeGropHandleClose = () => {
  nowCheckNoticeGrop.value = [];
  noticeGropForm.name = '';
  noticeGropForm.page_num = 1;
  dialogNoticeGrop.value = false;
};
const toAddNoticeGrop = () => {
  // 添加通知组
  if (nowCheckNoticeGrop.value.length === 0) {
    ElMessage.warning(proxy.$t('devOps.alert.message.selectNoticeGroup'));
    return;
  }
  nowCheckNoticeGrop.value.forEach((item: any) => {
    item.id = item.receiverId;
    item.hasChildren = true;
    item.type = 1;
  });
  form.receiverListData = [...form.receiverListData, ...nowCheckNoticeGrop.value];
  noticeGropHandleClose();
};
const toNoticeGropDelete = (index: number) => {
  // 删除通知组
  console.log(index);
  form.receiverListData.splice(index, 1);
};
const nowCheckNoticeGropIncludes = (val: any) => {
  const data: any = nowCheckNoticeGrop.value.filter((item: any) => item.receiverId === val.receiverId);
  return data.length > 0;
};
const noticeGropHandleCheckChange = (val: any, type: boolean) => {
  if (type) {
    nowCheckNoticeGrop.value.push(val);
  } else {
    nowCheckNoticeGrop.value = nowCheckNoticeGrop.value.filter((item: any) => item !== val);
  }
};
const getNoticeGropList = () => {
  // 通知组列表
  noticeGropLoading.value = true;

  const params: any = {
    name: noticeGropForm.name,
    page_num: noticeGropForm.page_num,
    page_size: noticeGropForm.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .operationReceivers(params)
    .then((res: any) => {
      noticeGropLoading.value = false;

      noticeGropTableList.value = res.alarmRules;
      noticeGropForm.total = res.totalNum;
    })
    .catch((error: any) => {
      noticeGropLoading.value = false;
    });
};
const noticeGropHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  noticeGropForm.page_size = val;
  getNoticeGropList();
};
const noticeGropHandleCurrentChange = (val: any) => {
  // 改变页码
  noticeGropForm.page_num = val;
  getNoticeGropList();
};
const addNoticeGrop = () => {
  // 点击添加通知组
  dialogNoticeGrop.value = true;
  getNoticeGropList();
};

// 懒加载
const noticeGroupHandleExpand = (row: any, expandRow: any) => {
  if (row.children == undefined) {
    mainApi
      .operationReceiversDetail(row.receiverId)
      .then((res: any) => {
        row.children = res;
      })
      .catch((error: any) => {});
  }
};
const vmGroupHandleExpand = (row: any, expandRow: any) => {
  if (row.children == undefined || row.children.length == 0) {
    mainApi
      .vmsInstabcesList({ instance_group_id: row.instanceGroupId })
      .then((res: any) => {
        row.children = res;
      })
      .catch((error: any) => {});
  }
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
onMounted(() => {});
</script>

<style lang="scss" scoped>
.eipPoolAddPage {
  ::v-depp .inputNumber {
    input[type='number'] {
      padding-right: 0px;
      -moz-appearance: textfield;
      -webkit-appearance: textfield;
      // 解决el-input设置类型为number时，中文输入法光标上移问题
      line-height: 1px !important;
    }

    input[type='number']::-webkit-inner-spin-button,
    input[type='number']::-webkit-outer-spin-button {
      -webkit-appearance: none;
      margin: 0;
    }
  }
}
</style>
