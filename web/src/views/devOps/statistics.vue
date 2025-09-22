<template>
  <div class="">
    <div class="pageTitle bg-white pt-2 px-4 border-t-1 border-gray-100 border-solid">
      <el-tabs v-model="activeName" class="demo-tabs">
        <el-tab-pane :label="$t('devOps.statistics.overview')" name="first"> </el-tab-pane>
        <el-tab-pane :label="$t('devOps.statistics.computeNodeDetail')" name="third"></el-tab-pane>
        <el-tab-pane :label="$t('devOps.statistics.virtualMachineDetail')" name="second"></el-tab-pane>
        <el-tab-pane :label="$t('devOps.statistics.gpuDetail')" name="fourth"></el-tab-pane>
      </el-tabs>
    </div>

    <div v-if="activeName == 'first'" class="indexMain p-3">
      <el-row :gutter="10" class="mb-3">
        <el-col :span="12" class="mb-3">
          <el-card class="box-card">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.resourceOverview') }}</span>
              </div>
            </template>
            <div style="height: 12.5rem">
              <el-row :gutter="10">
                <el-col :span="6">
                  <div style="background-color: rgba(0, 145, 255, 0.05)" class="rounded-xl text-center pt-4 pb-6">
                    <img src="@/assets/img/statistics/1.png" alt="" class="inline-block w-2/5" />
                    <span class="block text-2xl text-bold py-4">{{ apiCountData.vm || 0 }}</span>
                    <span class="block text-sm text-gray-400">{{ $t('devOps.statistics.virtualMachine') }}</span>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div style="background-color: rgba(0, 145, 255, 0.05)" class="rounded-xl text-center pt-4 pb-6">
                    <img src="@/assets/img/statistics/2.png" alt="" class="inline-block w-2/5" />
                    <span class="block text-2xl text-bold py-4"
                      >{{ apiCountData.storage.total || 0 }}<small>{{ apiCountData.storage.unit || 'GB' }}</small></span
                    >
                    <span class="block text-sm text-gray-400">{{ $t('devOps.statistics.storage') }}</span>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div style="background-color: rgba(0, 145, 255, 0.05)" class="rounded-xl text-center pt-4 pb-6">
                    <img src="@/assets/img/statistics/3.png" alt="" class="inline-block w-2/5" />
                    <span class="block text-2xl text-bold py-4">{{ apiCountData.vpc || 0 }}</span>
                    <span class="block text-sm text-gray-400">{{ $t('devOps.statistics.vpc') }}</span>
                  </div>
                </el-col>
                <el-col :span="6">
                  <div style="background-color: rgba(0, 145, 255, 0.05)" class="rounded-xl text-center pt-4 pb-6">
                    <img src="@/assets/img/statistics/4.png" alt="" class="inline-block w-2/5" />
                    <span class="block text-2xl text-bold py-4">{{ apiCountData.subnet || 0 }}</span>
                    <span class="block text-sm text-gray-400">{{ $t('devOps.statistics.subnet') }}</span>
                  </div>
                </el-col>
              </el-row>
            </div>
          </el-card>
        </el-col>
        <el-col :span="6" class="mb-3">
          <el-card class="box-card" :body-style="{ padding: 0 }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.cpuTotalUsage') }}</span>
              </div>
            </template>
            <div style="height: calc(12.5rem + 40px)">
              <pie
                :id="'cpuId'"
                :cur-data="[
                  { name: $t('devOps.statistics.cpuUsed'), value: apiData.cpuStats.used },
                  { name: $t('devOps.statistics.cpuUnused'), value: apiData.cpuStats.total - apiData.cpuStats.used },
                ]"
                :unit="$t('devOps.statistics.core')"
                :title="$t('devOps.statistics.totalCore')"
                :total-num="apiData.cpuStats.total"
                width="100%"
                :height="'calc(12.5rem + 40px)'"
              />
            </div>
          </el-card>
        </el-col>
        <el-col :span="6" class="mb-3">
          <el-card class="box-card" :body-style="{ padding: 0 }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.memoryTotalUsage') }}</span>
              </div>
            </template>
            <div style="height: calc(12.5rem + 40px)">
              <pie
                :id="'memId'"
                :cur-data="[
                  { name: $t('devOps.statistics.memoryUsed'), value: apiData.memStats.used },
                  { name: $t('devOps.statistics.memoryUnused'), value: apiData.memStats.total - apiData.memStats.used },
                ]"
                :unit="apiData.memStats.unit"
                :title="$t('devOps.statistics.totalMemory')"
                :total-num="apiData.memStats.total"
                width="100%"
                :height="'calc(12.5rem + 40px)'"
              />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" class="mb-3">
          <el-card class="box-card" :body-style="{ padding: 0 }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.computeNodeResourceUsage') }}</span>
              </div>
            </template>
            <div style="height: calc(16rem + 40px)">
              <linetwo
                :id="'linetwoId'"
                :cur-data="[apiData.cpu, apiData.mem]"
                width="100%"
                :height="'calc(16rem + 40px)'"
              />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12" class="mb-3">
          <el-card class="box-card" :body-style="{ padding: 0 }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.virtualMachine7Days') }}</span>
              </div>
            </template>
            <div style="height: calc(16rem + 40px)">
              <vmbar :id="'vmbarId'" :cur-data="apiData.vm" width="100%" :height="'calc(16rem + 40px)'" />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="box-card" :body-style="{ padding: 0 }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.storageResourceUsage') }}</span>
              </div>
            </template>
            <div style="height: calc(16rem + 40px)">
              <lineone :id="'lineoneId'" :cur-data="apiData.storage" width="100%" :height="'calc(16rem + 40px)'" />
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="box-card" :body-style="{ padding: 0 }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.natGateway') }}</span>
              </div>
            </template>
            <div style="height: calc(16rem + 40px)">
              <bar :id="'barId'" :cur-data="apiData.nat" width="100%" :height="'calc(16rem + 40px)'" />
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    <div v-if="activeName == 'second'" class="indexMain" style="height: calc(100vh - 45px - 3rem)">
      <el-row :gutter="10" class="!m-0">
        <el-col :span="24" class="mb-3 bg-white py-2 rounded">
          <div class="px-2 text-gray-400 text-sm">
            <div class="float-left pt-1">
              <span v-if="vmData">
                <span class="mr-4"
                  ><span>{{ $t('devOps.statistics.virtualMachineName') }}：</span
                  >{{ vmData.name || $t('devOps.statistics.noVirtualMachine') }}</span
                >
              </span>
              <span v-else class="mr-4"> {{ $t('devOps.statistics.allVirtualMachine') }} </span>
              <el-button class="resetBtn" size="small" @click="changeVm">
                {{ $t('devOps.statistics.virtualMachineSwitch') }}
              </el-button>
              <!-- <el-button class="resetBtn"
                         @click="showAllVm"
                         size="small">展示全部</el-button> -->
            </div>
            <div class="float-right">
              <el-form :model="secondForm" label-width="120px">
                <el-form-item :label="$t('devOps.statistics.time') + ':'">
                  <el-date-picker
                    v-model="secondTime"
                    type="datetimerange"
                    :shortcuts="shortcuts"
                    :range-separator="$t('devOps.statistics.rangeSeparator')"
                    :start-placeholder="$t('devOps.statistics.startTime')"
                    :end-placeholder="$t('devOps.statistics.endTime')"
                  />
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-col>

        <el-col :span="24" class="px-2 mb-2">
          <el-card class="box-card" :body-style="{ padding: '0px' }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.basicMonitoringInformation') }}</span>
              </div>
            </template>
            <div v-if="vmPanelsStatus" style="height: 1395px">
              <iframe width="100%" height="100%" :src="getVmPanelsUrl()" frameborder="0"></iframe>
            </div>
          </el-card>
        </el-col>
        <el-col
          v-if="vmDetailData && vmDetailData.pciInfos && vmDetailData.pciInfos.length > 0"
          :span="24"
          class="px-2 mb-2"
        >
          <el-card class="box-card" :body-style="{ padding: '0px' }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.gpuMonitoringInformation') }}</span>
              </div>
            </template>
            <div v-if="gpuPanelsStatus" style="height: 940px">
              <iframe width="100%" height="100%" :src="getVmGpuPanelsUrl()" frameborder="0"></iframe>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    <div v-if="activeName == 'third'" class="indexMain" style="height: calc(100vh - 45px - 3rem)">
      <el-row :gutter="10" class="!m-0">
        <el-col :span="24" class="mb-3 bg-white py-2 rounded">
          <div class="px-2 text-gray-400 text-sm">
            <div class="float-left pt-1">
              <span v-if="nodeData">
                <span class="mr-4"
                  ><span>{{ $t('devOps.statistics.computeNodeName') }}：</span
                  >{{ nodeData.name || $t('devOps.statistics.noComputeNode') }}</span
                >
                <span class="mr-4"
                  ><span>{{ $t('devOps.statistics.computeNodeIp') }}：</span
                  >{{ nodeData.manageIp || $t('devOps.statistics.noComputeNode') }}</span
                >
              </span>
              <!-- <span class="mr-4"
                    v-else>
                全部计算节点
              </span> -->
              <el-button class="resetBtn" size="small" @click="changeNode">
                {{ $t('devOps.statistics.computeNodeSwitch') }}
              </el-button>
              <!-- <el-button class="resetBtn"
                         @click="showAllNode"
                         size="small">展示全部</el-button> -->
            </div>
            <div class="float-right">
              <el-form :model="thirdForm" label-width="120px">
                <el-form-item :label="$t('devOps.statistics.time') + ':'">
                  <el-date-picker
                    v-model="thirdTime"
                    type="datetimerange"
                    :shortcuts="shortcuts"
                    :range-separator="$t('devOps.statistics.rangeSeparator')"
                    :start-placeholder="$t('devOps.statistics.startTime')"
                    :end-placeholder="$t('devOps.statistics.endTime')"
                  />
                </el-form-item>
              </el-form>
            </div>
          </div>
        </el-col>

        <el-col :span="24" class="">
          <el-card class="box-card" :body-style="{ padding: '0px' }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.monitor') }}</span>
              </div>
            </template>

            <div v-if="nodePanelsStatus" style="height: calc(100vh - 230px)">
              <iframe width="100%" height="100%" :src="getNodePanelsUrl()" frameborder="0"></iframe>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    <div v-if="activeName == 'fourth'" class="indexMain" style="height: calc(100vh - 45px - 3rem)">
      <el-row :gutter="10" class="!m-0">
        <el-col :span="24" class="mb-3 bg-white py-2 rounded">
          <div class="px-2 text-gray-400 text-sm">
            <el-form :model="fourthForm">
              <div class="overflow-hidden">
                <el-form-item :label="$t('devOps.statistics.type') + ':'" class="float-left mr-6">
                  <el-select v-model="searchGpu" :placeholder="$t('devOps.statistics.selectType')">
                    <el-option :label="$t('devOps.statistics.computeNode')" value="node"></el-option>
                    <el-option :label="$t('devOps.statistics.virtualMachine')" value="vm"></el-option>
                  </el-select>
                </el-form-item>
                <el-form-item
                  v-if="searchGpu == 'node'"
                  :label="$t('devOps.statistics.computeNode') + ':'"
                  class="float-left"
                >
                  <el-select v-model="nodeDataGpu" :placeholder="$t('devOps.statistics.selectComputeNode')">
                    <el-option
                      v-for="(item, index) in nodeAllTableData"
                      :key="index"
                      :label="item.name + '(' + item.manageIp + ')'"
                      :value="item.manageIp"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item v-if="searchGpu == 'vm'" :label="$t('devOps.statistics.virtualMachine') + ':'">
                  <el-select v-model="vmDataGpu" :placeholder="$t('devOps.statistics.selectVirtualMachine')">
                    <el-option
                      v-for="(item, index) in vmAllTableData"
                      :key="index"
                      :label="item.name"
                      :value="item.instanceId"
                    />
                  </el-select>
                </el-form-item>

                <el-form-item :label="$t('devOps.statistics.time') + ':'" class="float-right" label-width="60px">
                  <el-date-picker
                    v-model="fourthTime"
                    type="datetimerange"
                    :shortcuts="shortcuts"
                    :range-separator="$t('devOps.statistics.rangeSeparator')"
                    :start-placeholder="$t('devOps.statistics.startTime')"
                    :end-placeholder="$t('devOps.statistics.endTime')"
                  />
                </el-form-item>
              </div>
            </el-form>
          </div>
        </el-col>

        <el-col :span="24" class="px-2">
          <el-card class="box-card" :body-style="{ padding: '0px' }">
            <template #header>
              <div class="card-header">
                <span>{{ $t('devOps.statistics.monitor') }}</span>
              </div>
            </template>
            <div v-if="gpuPanelsStatus" style="height: calc(100vh - 230px)">
              <iframe width="100%" height="100%" :src="getGpuPanelsUrl()" frameborder="0"></iframe>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    <el-dialog
      v-model="vmDialog"
      :close-on-click-modal="false"
      width="1150px"
      destroy-on-close
      :before-close="vmHandleClose"
      :title="$t('devOps.statistics.form.switchVirtualMachine')"
    >
      <div class="mb-2">
        <el-form :model="vmForm" :inline="true" label-width="85px" :size="mainStoreData.viewSize.main">
          <el-form-item :label="$t('devOps.statistics.form.virtualMachineName') + ':'">
            <el-input
              v-model="vmForm.name"
              class="!w-50"
              :placeholder="$t('devOps.statistics.form.selectVirtualMachineName')"
            />
          </el-form-item>

          <el-form-item class="float-right">
            <el-button class="resetBtn w-24" @click="vmOnReset">{{ $t('common.reset') }}</el-button>
            <el-button type="primary" class="w-24" @click="vmOnSubmit">{{ $t('common.search') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        v-loading="loading"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :data="vmTableData"
        max-height="calc(100% - 3rem)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
        @current-change="vmChange"
      >
        <el-table-column width="55">
          <template #header> </template>
          <template #default="{ row }">
            <span v-if="vmChangeData && vmChangeData.instanceId == row.instanceId">
              <label class="el-checkbox el-checkbox--default is-checked" data-v-aa386586="">
                <span class="el-checkbox__input is-checked" aria-checked="false">
                  <span class="el-checkbox__inner"></span>
                </span>
                <!--v-if-->
              </label>
            </span>
            <span v-else>
              <label class="el-checkbox el-checkbox--default" data-v-aa386586="">
                <span class="el-checkbox__input" aria-checked="false">
                  <span class="el-checkbox__inner"></span>
                </span>
                <!--v-if-->
              </label>
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="date" :label="$t('devOps.statistics.form.name')">
          <template #default="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="hostname" :label="$t('devOps.statistics.form.hostname')" />
        <el-table-column prop="portInfo.ipAddress" :label="$t('devOps.statistics.form.ip')">
          <template #default="scope">
            <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="vpcInfo.cidr" :label="$t('devOps.statistics.form.networkAddress')">
          <template #default="scope">
            <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="eip" :label="$t('devOps.statistics.form.eip')">
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
                  <el-tag :size="mainStoreData.viewSize.tagStatus">{{ $t('devOps.statistics.form.nat') }}</el-tag>
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
        <el-table-column prop="phaseStatus" :label="$t('devOps.statistics.form.status')">
          <template #default="scope">
            <el-tag
              :size="mainStoreData.viewSize.tagStatus"
              :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
              >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="imageInfo.name" :label="$t('devOps.statistics.form.system')" />
        <el-table-column prop="createTime" :label="$t('devOps.statistics.form.createTime')" />
      </el-table>
      <el-pagination
        v-model:page_num="vmForm.page_num"
        v-model:page-size="vmForm.page_size"
        class="!py-4 !pr-8 float-right"
        :page-sizes="mainStoreData.page_sizes"
        :current-page="vmForm.page_num"
        :small="true"
        layout="total, sizes, prev, pager, next, jumper"
        :total="vmForm.total"
        @size-change="vmHandleSizeChange"
        @current-change="vmHandleCurrentChange"
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="vmHandleClose()">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" @click="toVmChange()">{{ $t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>
    <el-dialog
      v-model="nodeDialog"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :before-close="nodeHandleClose"
      :title="$t('devOps.statistics.form.switchComputeNode')"
    >
      <div class="mb-2">
        <el-form :model="nodeForm" :inline="true" label-width="100px" :size="mainStoreData.viewSize.main">
          <el-form-item :label="$t('devOps.statistics.form.computeNodeName') + ':'">
            <el-input
              v-model="nodeForm.name"
              class="!w-50"
              :placeholder="$t('devOps.statistics.form.selectComputeNodeName')"
            />
          </el-form-item>

          <el-form-item class="float-right">
            <el-button class="resetBtn w-24" @click="nodeOnReset">{{ $t('common.reset') }}</el-button>
            <el-button type="primary" class="w-24" @click="nodeOnSubmit">{{ $t('common.search') }}</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table
        v-loading="loading"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :data="nodeTableData"
        max-height="calc(100% - 3rem)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
        @current-change="nodeChange"
      >
        <el-table-column width="55">
          <template #header> </template>
          <template #default="{ row }">
            <span v-if="nodeChangeData && nodeChangeData.nodeId == row.nodeId">
              <label class="el-checkbox el-checkbox--default is-checked" data-v-aa386586="">
                <span class="el-checkbox__input is-checked" aria-checked="false">
                  <span class="el-checkbox__inner"></span>
                </span>
                <!--v-if-->
              </label>
            </span>
            <span v-else>
              <label class="el-checkbox el-checkbox--default" data-v-aa386586="">
                <span class="el-checkbox__input" aria-checked="false">
                  <span class="el-checkbox__inner"></span>
                </span>
                <!--v-if-->
              </label>
            </span>
          </template>
        </el-table-column>

        <el-table-column prop="date" :label="$t('devOps.statistics.form.name')">
          <template #default="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="hostname" :label="$t('devOps.statistics.form.hostname')" />
        <el-table-column prop="manageIp" :label="$t('devOps.statistics.form.manageIp')" />
        <el-table-column :label="$t('devOps.statistics.form.cpu')">
          <template #default="scope">
            <div v-if="scope.row.cpuLogCount">
              <p>
                {{ scope.row.cpuLogCount - scope.row.usedCpuSum }}{{ $t('devOps.statistics.core') }}/{{
                  scope.row.cpuLogCount
                }}{{ $t('devOps.statistics.core') }}
              </p>
              <p>{{ scope.row.cpuModel }}</p>
            </div>
            <div v-else>-</div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('devOps.statistics.form.gpu')">
          <template #default="scope">
            <div v-if="scope.row.gpuTotal">
              <p>{{ scope.row.availableGpuCount }}/{{ scope.row.gpuTotal }}</p>
              <p>{{ scope.row.gpuName }}</p>
            </div>
            <div v-else>-</div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('devOps.statistics.form.memory')">
          <template #default="scope">
            <div v-if="scope.row.memTotal">
              {{ scope.row.memTotal - scope.row.usedMemSum }}GB/{{ scope.row.memTotal }}GB
            </div>
            <div v-else>-</div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('devOps.statistics.form.ibNetwork')">
          <template #default="scope">
            <div v-if="scope.row.ibTotal">{{ scope.row.availableIbCount }}/{{ scope.row.ibTotal }}</div>
            <div v-else>-</div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" :label="$t('devOps.statistics.form.createTime')" />
      </el-table>
      <el-pagination
        v-model:page_num="nodeForm.page_num"
        v-model:page-size="nodeForm.page_size"
        class="!py-4 !pr-8 float-right"
        :page-sizes="mainStoreData.page_sizes"
        :current-page="nodeForm.page_num"
        :small="true"
        layout="total, sizes, prev, pager, next, jumper"
        :total="nodeForm.total"
        @size-change="nodeHandleSizeChange"
        @current-change="nodeHandleCurrentChange"
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="nodeHandleClose()">{{ $t('common.cancel') }}</el-button>
          <el-button type="primary" @click="toNodeChange()">{{ $t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { nextTick } from '@vue/runtime-core';
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';

import mainStore from '@/store/mainStore'; // 是否是管理员

// 引入组件
import pie from './statistics/pie.vue';
import lineone from './statistics/line.vue';
import linetwo from './statistics/linetwo.vue';
import vmbar from './statistics/vmbar.vue';
import bar from './statistics/bar.vue';

const { proxy }: any = getCurrentInstance();
const mainStoreData = mainStore(); // pinia 信息
const { isAdmin } = mainStoreData;

const loading: any = ref(false);

const userInfo: any = ref({}); // 用户信息
const apiData: any = ref({
  nat: '',
  vm: '',
  cpu: '',
  mem: '',
  storage: '',
  cpuStats: '',
  memStats: '',
});
const apiCountData: any = ref({
  vm: '',
  storage: '',
  vpc: '',
  subnet: '',
});
const shortcuts = [
  {
    text: proxy.$t('devOps.statistics.validator.recent5Minutes'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 60 * 1000 * 5);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.statistics.validator.recent10Minutes'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 60 * 1000 * 10);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.statistics.validator.recent15Minutes'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 60 * 1000 * 15);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.statistics.validator.recent30Minutes'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 60 * 1000 * 30);
      return [start, end];
    },
  },

  {
    text: proxy.$t('devOps.statistics.validator.recent1Hour'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000);
      return [start, end];
    },
  },

  {
    text: proxy.$t('devOps.statistics.validator.recent3Hours'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 3);
      return [start, end];
    },
  },
  // 最近 6 小时
  {
    text: proxy.$t('devOps.statistics.validator.recent6Hours'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 6);
      return [start, end];
    },
  },
  // 最近 12 小时
  {
    text: proxy.$t('devOps.statistics.validator.recent12Hours'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 12);
      return [start, end];
    },
  },
  // 最近一天
  {
    text: proxy.$t('devOps.statistics.validator.recent1Day'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24);
      return [start, end];
    },
  },
  // 最近三天
  {
    text: proxy.$t('devOps.statistics.validator.recent3Days'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 3);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.statistics.validator.recent1Week'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
      return [start, end];
    },
  },
  {
    text: proxy.$t('devOps.statistics.validator.recent1Month'),
    value: () => {
      const end = new Date();
      const start = new Date();
      start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
      return [start, end];
    },
  },
];
const vmPanelsData: any = ref([]); // 虚拟机面板数据
const nodePanelsData: any = ref([]); // 计算节点面板数据
const vmPanelsStatus: any = ref(false); // 虚拟机面板状态
const nodePanelsStatus: any = ref(false); // 计算节点面板状态
const gpuPanelsData: any = ref([]); // GPU面板数据
const gpuPanelsStatus: any = ref(false); // GPU面板状态
const searchGpu: any = ref('node'); // 搜索GPU
const nodeAllTableData: any = ref([]); // 全部 node
const vmAllTableData: any = ref([]); // 全部 vm
const nodeDataGpu: any = ref('');
const vmDataGpu: any = ref('');
const activeName = ref('first'); // tab切换
const vmData: any = ref('');
const vmDetailData: any = ref('');
const vmChangeData: any = ref();
const vmForm: any = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const vmDialog: any = ref(false);
const vmTableData: any = ref([]);
const secondForm: any = ref('');
// 最近一天 24小时
const secondTime: any = ref([new Date(new Date().setHours(new Date().getHours() - 1)), new Date()]);

const nodeData: any = ref('');
const nodeChangeData: any = ref();
const nodeForm: any = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const nodeDialog: any = ref(false);
const nodeTableData: any = ref([]);
const thirdForm: any = ref('');
const thirdTime: any = ref([new Date(new Date().setHours(new Date().getHours() - 1)), new Date()]);

const fourthForm: any = ref('');
const fourthTime: any = ref([new Date(new Date().setHours(new Date().getHours() - 1)), new Date()]);

const setIframeStyle = () => {
  const iframeMain: any = document.getElementsByTagName('iframe');
  let iframeNum = 0;
  for (let i = 0; i < iframeMain.length; i++) {
    // iframeMain[i].style.display = 'none';
    iframeMain[i].onload = function () {
      // iframeMain[i].style.display = 'block';
      iframeNum++;
      if (iframeNum == iframeMain.length) {
        toStyle();
      }
    };
  }
};
const toStyle = () => {
  const iframeMain: any = document.getElementsByTagName('iframe');
  for (let i = 0; i < iframeMain.length; i++) {
    // iframeMain[i].style.display = 'none';
    // iframeMain[i].style.display = 'block';
    const iframeDoc = iframeMain[i].contentWindow.document;
    const iframeHead = iframeDoc.getElementsByTagName('head')[0];
    const iframeStyle = iframeDoc.createElement('style');
    iframeStyle.type = 'text/css';
    iframeStyle.innerHTML =
      'body{background-color: #fff;}.css-1nm48fy{padding:20px}.panel-header{display:block}.panel-header:hover{background-color: transparent;}.panel-header .panel-title div{display:none}.panel-header .panel-title h2{cursor: move;}.panel-container{border-bottom: 2px solid #e4e7ed;padding-bottom: 20px;}.panel-dropdown{display:none!important}.submenu-controls{display:none!important}.page-toolbar{display:none!important}.css-keyl2d{display:none!important}';

    iframeHead.appendChild(iframeStyle);
  }
};
const getVmPanelsUrl = () => {
  const urlOrigin = window.location.origin;
  // var urlOrigin = 'http://192.168.12.99:3000';
  const url = `${urlOrigin}/api/monitor/vm/d/${vmPanelsData.value.dashboardId}/${
    vmPanelsData.value.dashboardName
  }?orgId=1${
    vmData.value ? `&var-vm_instance_id=${vmData.value.instanceId}` : `&var-vm_user_id=${userInfo.value.id}`
  }&from=${secondTime.value[0].getTime()}&to=${secondTime.value[1].getTime()}&kiosk`;
  return url;
};
const getVmGpuPanelsUrl = () => {
  const urlOrigin = window.location.origin;
  // var urlOrigin = 'http://192.168.12.99:3000';
  const url = `${urlOrigin}/api/monitor/vm/d/${gpuPanelsData.value.dashboardId}/${
    gpuPanelsData.value.dashboardName
  }?orgId=1${
    vmData.value ? `&var-vm_instance_id=${vmData.value.instanceId}` : ''
  }&from=${secondTime.value[0].getTime()}&to=${secondTime.value[1].getTime()}&kiosk`;
  return url;
};
const vmHandleClose = () => {
  vmDialog.value = false;
  vmChangeData.value = '';
};
const toVmChange = () => {
  vmDialog.value = false;
  vmData.value = JSON.parse(JSON.stringify(vmChangeData.value));
  vmDetailData.value = '';
  getDetail(vmData.value.instanceId);
};
const vmOnSubmit = () => {
  // 提交查询
  vmForm.page_num = 1;
  getVmsInstabcesList();
};
const vmOnReset = () => {
  // 重置查询
  vmForm.name = '';
  vmForm.page_num = 1;
  getVmsInstabcesList();
};
const vmChange = (val: any) => {
  // 选择
  console.log(val);
  vmChangeData.value = val;
};
const vmHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  vmForm.page_size = val;
  getVmsInstabcesList();
};
const vmHandleCurrentChange = (val: any) => {
  // 改变页码
  vmForm.page_num = val;
  getVmsInstabcesList();
};
const getDetail = (id: any) => {
  mainApi
    .vmsInstabcesDetail(id)
    .then((res: any) => {
      vmDetailData.value = res;
    })
    .catch((error: any) => {});
};
const changeVm = () => {
  vmDialog.value = true;

  vmForm.page_num = 1;
  vmChangeData.value = JSON.parse(JSON.stringify(vmData.value));
  getVmsInstabcesList();
};
const showAllVm = () => {
  vmData.value = '';
  vmChangeData.value = '';
  vmDetailData.value = '';
};
const getVmPanels = async () => {
  const res = await mainApi.vmPanels();
  vmPanelsData.value = res;
  vmPanelsStatus.value = true;
  setIframeStyle();
};
const getNodePanels = async () => {
  const res = await mainApi.computePanels();
  nodePanelsData.value = res;
  nodePanelsStatus.value = true;
  setIframeStyle();
};
const getNodePanelsUrl = () => {
  const urlOrigin = window.location.origin;
  // var urlOrigin = 'http://192.168.12.99:3000';
  const url = `${urlOrigin}/api/monitor/vm/d/${nodePanelsData.value.dashboardId}/${
    nodePanelsData.value.dashboardName
  }?orgId=1${
    nodeData.value ? `&var-node=${nodeData.value.manageIp}:9100` : `&var-vm_user_id=${userInfo.value.id}`
  }&from=${thirdTime.value[0].getTime()}&to=${thirdTime.value[1].getTime()}&kiosk`;
  // var url =
  // urlOrigin +
  // '/api/monitor/vm/d/' +
  // vmPanelsData.value.dashboardId +
  // '/' +
  // vmPanelsData.value.dashboardName +
  // '?orgId=1&var-vm_name=05b0bd2a-85bf-47e8-bb41-f7a9b62933d1&from=1667972049492&to=1667993649492&viewPanel=' +
  // panelId +
  // '&kiosk';
  return url;
};
const nodeHandleClose = () => {
  nodeDialog.value = false;
  nodeChangeData.value = '';
};
const toNodeChange = () => {
  nodeDialog.value = false;
  nodeData.value = JSON.parse(JSON.stringify(nodeChangeData.value));
  // nodeChangeData.value = '';
};
const nodeOnSubmit = () => {
  // 提交查询
  nodeForm.page_num = 1;
  getvmsHypervisorNodesList();
};
const nodeOnReset = () => {
  // 重置查询
  nodeForm.name = '';
  nodeForm.page_num = 1;
  getvmsHypervisorNodesList();
};
const changeNode = () => {
  nodeDialog.value = true;

  nodeForm.page_num = 1;
  nodeChangeData.value = JSON.parse(JSON.stringify(nodeData.value));

  getvmsHypervisorNodesList();
};
const showAllNode = () => {
  nodeData.value = '';
};
const nodeChange = (val: any) => {
  // 选择
  nodeChangeData.value = val;
};
const nodeHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  nodeForm.page_size = val;
  getvmsHypervisorNodesList();
};
const nodeHandleCurrentChange = (val: any) => {
  // 改变页码
  nodeForm.page_num = val;
  getvmsHypervisorNodesList();
};
const getvmsHypervisorNodesList = () => {
  // 计算节点列表
  loading.value = true;

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
      loading.value = false;
      nodeTableData.value = res.nodeAllocationInfos;
      nodeForm.total = res.totalNum;
      if (!nodeData.value) {
        nodeData.value = res.nodeAllocationInfos[0];
      }
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const getVmsInstabcesList = () => {
  // 虚拟机列表
  loading.value = true;

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
      loading.value = false;
      vmTableData.value = res.vmInstancesInfo;
      vmForm.total = res.totalNum;
      if (!vmData.value && res.vmInstancesInfo.length > 0) {
        vmData.value = res.vmInstancesInfo[0];
        getDetail(res.vmInstancesInfo[0].instanceId);
      }
    })
    .catch((error: any) => {
      loading.value = false;
    });
};

const getGpuPanelsUrl = () => {
  const urlOrigin = window.location.origin;
  // var urlOrigin = 'http://192.168.12.99:3000';
  const url = `${urlOrigin}/api/monitor/vm/d/${gpuPanelsData.value.dashboardId}/${
    gpuPanelsData.value.dashboardName
  }?orgId=1${nodeDataGpu.value ? `&var-node=${nodeDataGpu.value}:9835` : ''}${
    vmDataGpu.value ? `&var-vm_instance_id=${vmDataGpu.value}` : ''
  }&from=${secondTime.value[0].getTime()}&to=${secondTime.value[1].getTime()}&kiosk`;
  return url;
};
const getGpuPanels = async () => {
  const res = await mainApi.gpuPanels();
  gpuPanelsData.value = res;

  gpuPanelsStatus.value = true;
  setIframeStyle();
};
const getAllNodesList = () => {
  // 计算节点列表
  mainApi.vmsHypervisorNodesList().then((res: any) => {
    nodeAllTableData.value = res.nodeAllocationInfos;
    if (!nodeDataGpu.value) {
      nodeDataGpu.value = res.nodeAllocationInfos[0].manageIp;
    }
  });
};
const getAllVmList = () => {
  // 虚拟机列表
  mainApi.vmsInstabcesList().then((res: any) => {
    vmAllTableData.value = res.vmInstancesInfo;
  });
};
const init = async () => {
  apiData.value.nat = await mainApi.vmsResourceStats({ name: 'nat', days: 7 });
  apiData.value.vm = await mainApi.vmsResourceStats({ name: 'vm', days: 7 });
  apiData.value.cpu = await mainApi.vmsResourceStats({ name: 'cpu', days: 7 });
  apiData.value.mem = await mainApi.vmsResourceStats({ name: 'mem', days: 7 });
  apiData.value.storage = await mainApi.vmsResourceStats({ name: 'storage', days: 7 });
  apiData.value.cpuStats = await mainApi.vmsCpuStats();
  apiData.value.memStats = await mainApi.vmsMemStats();
};
const initCount = async () => {
  apiCountData.value.vm = await mainApi.vmsVmCount();
  apiCountData.value.vpc = await mainApi.networkVpcCount();
  apiCountData.value.subnet = await mainApi.networkSubnetCount();
  if (isAdmin) {
    mainApi.vmsAllStorageStats().then((res: any) => {
      apiCountData.value.storage = res;
    });
  } else {
    mainApi.vmsUserStorageStats().then((res: any) => {
      apiCountData.value.storage = res;
    });
  }
  console.log(apiCountData);
};
const getInfo = () => {
  mainApi
    .infoList()
    .then((res: any) => {
      localStorage.setItem('userInfo', JSON.stringify(res));
      mainStoreData.userInfo = res;
      userInfo.value = res;
    })
    .catch((error: any) => {});
};
// 监听标签切换 activeName
watch(activeName, (val) => {
  if (val === 'second') {
    nextTick(() => {
      setIframeStyle();
    });
  } else if (val === 'third') {
    nextTick(() => {
      setIframeStyle();
    });
  } else if (val === 'fourth') {
    nextTick(() => {
      setIframeStyle();
    });
  }
});
watch(searchGpu, (val: any) => {
  if (val == 'node') {
    vmDataGpu.value = '';
    if (nodeAllTableData.value.length > 0) {
      nodeDataGpu.value = nodeAllTableData.value[0].manageIp;
    }
  } else {
    nodeDataGpu.value = '';
    if (vmAllTableData.value.length > 0) {
      vmDataGpu.value = vmAllTableData.value[0].instanceId;
    }
  }
  nextTick(() => {
    setIframeStyle();
  });
});
// 监听secondTime
watch(secondTime, (val) => {
  nextTick(() => {
    setIframeStyle();
  });
});
watch(thirdTime, (val) => {
  nextTick(() => {
    setIframeStyle();
  });
});
watch(fourthTime, (val) => {
  nextTick(() => {
    setIframeStyle();
  });
});
watch(vmData, (val) => {
  nextTick(() => {
    setIframeStyle();
  });
});
watch(nodeData, (val) => {
  nextTick(() => {
    setIframeStyle();
  });
});
onMounted(() => {
  getInfo();
  initCount();
  init();
  getVmsInstabcesList();
  getvmsHypervisorNodesList();
  getNodePanels();
  getVmPanels();
  getGpuPanels();
  getAllNodesList();
  getAllVmList();
  // 禁用ESC
  document.onkeydown = function (event) {
    if (event.keyCode == 27) {
      return false;
    }
  };
});
</script>

<style lang="scss" scoped>
.el-form {
  .el-form-item {
    margin-bottom: 0;
  }
}

.indexMain {
  height: calc(100vh - 45px - 3rem);
  overflow-y: auto;
}

.el-tabs {
  ::v-deep .el-tabs__header {
    margin-bottom: 0;
  }
}
</style>
