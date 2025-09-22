<template>
  <div class="vmAddPage h-full">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <el-page-header :title="$t('common.backToList')" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <el-form
      ref="addVmForm"
      v-loading="loading"
      :size="mainStoreData.viewSize.main"
      :model="form"
      :rules="rules"
      label-width="120px"
      :element-loading-text="proxy.$t('common.loading')"
    >
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('storage.volumes.vmAdd.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('storage.volumes.vmAdd.form.name') + ':'" prop="name">
            <el-input
              v-model="form.name"
              class="!w-60"
              :placeholder="$t('storage.volumes.vmAdd.form.namePlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.vmAdd.form.description') + ':'" prop="description">
            <el-input
              v-model="form.description"
              class="!w-100"
              maxlength="255"
              show-word-limit
              type="textarea"
              :rows="2"
              :placeholder="$t('storage.volumes.vmAdd.form.descriptionPlaceholder')"
            />
          </el-form-item>
          <el-divider />
          <el-form-item>
            <div class="w-full">
              <div class="w-full">
                <el-radio-group v-model="vmAddType" :size="mainStoreData.viewSize.main">
                  <el-radio-button :label="0" :value="0">{{ $t('storage.volumes.vmAdd.quickCreate') }}</el-radio-button>
                  <el-radio-button :label="1" :value="1">{{
                    $t('storage.volumes.vmAdd.customCreate')
                  }}</el-radio-button>
                </el-radio-group>
              </div>
            </div>
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.vmAdd.form.flavor') + ':'" prop="flavorId" class="relative">
            <el-radio-group v-model="flavorsListType" size="small">
              <el-radio-button label="info" value="info">{{
                $t('storage.volumes.vmAdd.form.generalFlavor')
              }}</el-radio-button>

              <el-radio-button label="gpu" value="gpu">{{
                $t('storage.volumes.vmAdd.form.gpuFlavor')
              }}</el-radio-button>
            </el-radio-group>
            <el-button
              class="absolute z-10 top-3 right-2"
              :size="mainStoreData.viewSize.tabChange"
              type="primary"
              @click="clearFilter"
              >{{ $t('storage.volumes.vmAdd.resetSort') }}</el-button
            >

            <el-table
              ref="singleTableRef"
              max-height="300px"
              :size="mainStoreData.viewSize.main"
              :data="flavorsList"
              highlight-current-row
              style="width: 100%"
              @current-change="flavorsHandleCurrentChange"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span
                    v-if="scope.row.flavorId != form.flavorId"
                    class="w-3 h-3 block border rounded-sm border-gray-300"
                  ></span>
                  <span
                    v-else
                    class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                  >
                    <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="date" :label="$t('storage.volumes.vmAdd.form.name')">
                <template #default="scope">
                  <router-link :to="'/flavors/' + scope.row.flavorId" class="text-blue-400">{{
                    scope.row.name || '-'
                  }}</router-link>
                </template>
              </el-table-column>
              <el-table-column prop="type" :label="$t('storage.volumes.vmAdd.form.type')">
                <template #default="scope">
                  <span v-if="scope.row.gpuCount">{{ $t('storage.volumes.vmAdd.form.gpuFlavor') }}</span>
                  <span v-else>{{ $t('storage.volumes.vmAdd.form.generalFlavor') }}</span>
                </template>
              </el-table-column>
              <el-table-column
                prop="cpu"
                :label="$t('storage.volumes.vmAdd.form.cpu')"
                :filters="flavorCpu"
                :filter-method="filterCpu"
              >
                <template #default="scope"> {{ scope.row.cpu || '-' }}{{ $t('common.core') }} </template>
              </el-table-column>
              <el-table-column
                prop="mem"
                :label="$t('storage.volumes.vmAdd.form.mem')"
                :filters="flavorMem"
                :filter-method="filterMem"
              >
                <template #default="scope"> {{ scope.row.mem || '-' }}GB </template>
              </el-table-column>

              <el-table-column
                prop="rootDisk"
                :label="$t('storage.volumes.vmAdd.form.rootDisk')"
                :filters="flavorDisk"
                :filter-method="filterDisk"
              >
                <template #default="scope"> {{ scope.row.rootDisk || '-' }}GB </template>
              </el-table-column>
              <el-table-column prop="gpuCount" :label="$t('storage.volumes.vmAdd.form.gpuCount')">
                <template #default="scope">
                  <span v-if="scope.row.gpuCount && scope.row.gpuCount > 0">
                    {{ scope.row.gpuName }}*{{ scope.row.gpuCount }}
                  </span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
            <span class="align-text-top mr-4 text-gray-500">
              {{ $t('storage.volumes.vmAdd.form.selected') }}：{{ currentFlavorRow.name }}
            </span>
          </el-form-item>
          <el-form-item
            v-if="vmAddType == 1"
            :label="$t('storage.volumes.vmAdd.form.node.title') + ':'"
            :prop="vmAddType == 1 ? '' : 'nodeId'"
          >
            <div class="w-full">
              <el-table
                ref="nodeTableRef"
                :size="mainStoreData.viewSize.main"
                :data="vmsHypervisorNodesList"
                max-height="300px"
                highlight-current-row
                class="w-full mt-2"
                @current-change="nodeHandleCurrentChange"
              >
                <el-table-column label="" width="40px">
                  <template #default="scope">
                    <span
                      v-if="scope.row.nodeId != form.nodeId"
                      class="w-3 h-3 block border rounded-sm border-gray-300"
                    ></span>
                    <span
                      v-else
                      class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                    >
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="date" :label="$t('storage.volumes.vmAdd.form.node.name')">
                  <template #default="scope">
                    <span class=" ">{{ scope.row.name }}</span>
                  </template>
                </el-table-column>

                <el-table-column
                  prop="cpuAllocation"
                  :label="$t('storage.volumes.vmAdd.form.node.cpuAllocation')"
                  width="160"
                >
                  <template #default="scope">
                    {{
                      (scope.row.cpuLogCount ? scope.row.cpuLogCount : 0) -
                      (scope.row.usedCpuSum ? scope.row.usedCpuSum : 0)
                    }}
                    {{ $t('common.core') }}
                  </template>
                </el-table-column>
                <el-table-column prop="memAllocation" :label="$t('storage.volumes.vmAdd.form.node.memAllocation')">
                  <template #default="scope">
                    {{
                      (scope.row.memTotal ? scope.row.memTotal : 0) - (scope.row.usedMemSum ? scope.row.usedMemSum : 0)
                    }}GB
                  </template>
                </el-table-column>
                <el-table-column prop="availableGpuCount" :label="$t('storage.volumes.vmAdd.form.node.gpuAllocation')">
                  <template #default="scope">
                    <span>{{ scope.row.availableGpuCount }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="manageIp" :label="$t('storage.volumes.vmAdd.form.node.manageIp')" />
              </el-table>
              <span class="align-text-top mr-4 text-gray-500">
                {{ $t('storage.volumes.vmAdd.form.node.selectedNode') }}：{{
                  currentNodeRow.nodeName || currentNodeRow.name
                }}
              </span>
            </div>
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.vmAdd.form.startSource') + ':'">
            <el-radio-group :model-value="0" :size="mainStoreData.viewSize.main">
              <el-radio-button :label="0" :value="0">{{ $t('storage.volumes.vmAdd.form.image') }}</el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item
            :label="$t('storage.volumes.vmAdd.form.os.title') + ':'"
            :prop="nowImageType == 0 ? 'imageOsType' : 'imageId'"
          >
            <div class="w-full">
              <el-row>
                <el-col v-for="(item, index) in imagesTypeList" :key="index" :span="4">
                  <div class="text-center" @click="imagesTypeChange(item)">
                    <!-- 置灰 -->
                    <span class="text-xl block" style="filter: grayscale(100%)">
                      <i-codicon-vm v-show="item.value == 0" class="inline-block"></i-codicon-vm>
                      <i-uim-windows
                        v-show="item.value == 1"
                        class="inline-block"
                        style="color: rgba(0, 120, 212, 1)"
                      ></i-uim-windows>
                      <i-logos-ubuntu v-show="item.value == 2" class="inline-block"></i-logos-ubuntu>
                      <i-logos-centos-icon v-show="item.value == 3" class="inline-block"></i-logos-centos-icon>
                    </span>
                    <span class="text-xs" :class="nowImageType == item.value ? 'text-blue-500 font-bold' : ''">
                      {{ item.label }}
                    </span>
                  </div>
                </el-col>
              </el-row>
            </div>
            <el-table
              v-if="nowImageType == 0"
              ref="nodeTableRef"
              :size="mainStoreData.viewSize.main"
              :data="emptyImagesList"
              max-height="300px"
              highlight-current-row
              style="width: 100%"
              @current-change="EmptyImagesHandleCurrentChange"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span
                    v-if="scope.row.typeValue != form.imageOsType"
                    class="w-3 h-3 block border rounded-sm border-gray-300"
                  ></span>
                  <span
                    v-else
                    class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                  >
                    <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="date" :label="$t('storage.volumes.vmAdd.form.os.name')">
                <template #default="scope">
                  <span class=" ">{{ scope.row.label }}</span>
                </template>
              </el-table-column>
            </el-table>
            <el-table
              v-if="nowImageType != 0"
              ref="nodeTableRef"
              :size="mainStoreData.viewSize.main"
              :data="imagesList"
              max-height="300px"
              highlight-current-row
              style="width: 100%"
              @current-change="imagesHandleCurrentChange"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span
                    v-if="scope.row.imageId != form.imageId"
                    class="w-3 h-3 block border rounded-sm border-gray-300"
                  ></span>
                  <span
                    v-else
                    class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                  >
                    <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="date" :label="$t('storage.volumes.vmAdd.form.os.name')">
                <template #default="scope">
                  <span class=" ">{{ scope.row.imageName }}</span>
                </template>
              </el-table-column>
            </el-table>
            <span class="align-text-top mr-4 text-gray-500">
              {{ $t('storage.volumes.vmAdd.form.os.selected') }}：<span v-if="nowImageType == 0"
                >{{ form.imageOsType == 1 ? 'Windows' : form.imageOsType == 0 ? 'Linux' : '' }}
                {{ $t('storage.volumes.vmAdd.form.os.blankVm') }}</span
              >
              <span v-if="nowImageType != 0">{{ currentImageRow.imageName }}</span>
            </span>
          </el-form-item>
          <el-divider />
          <el-form-item :label="$t('storage.volumes.vmAdd.form.storagePool.title') + ':'" prop="storagePoolId">
            <el-select
              v-model="form.storagePoolId"
              class="ml-0 !w-60"
              :disabled="true"
              :placeholder="$t('storage.volumes.vmAdd.form.storagePool.inputStoragePool')"
              @change="changeStoragePool"
            >
              <el-option
                v-for="(item, index) in storagePoolsList"
                :key="index"
                :label="item.name"
                :value="item.poolId"
              />
            </el-select>
          </el-form-item>
          <div class="overflow-hidden">
            <el-form-item
              :label="$t('storage.volumes.vmAdd.form.disk.dataDisk') + ':'"
              :prop="'diskInfos'"
              class="w-100"
            >
              <template #label>
                <span class="">{{ $t('storage.volumes.vmAdd.form.disk.restoreDisk') }}</span>
              </template>
              <el-form-item label="" class="w-100">
                <div class="w-100">{{ drawerData.item.name }} ({{ drawerData.item.size }}GB)</div>
              </el-form-item>
            </el-form-item>
          </div>
          <div v-for="(item, index) in form.diskInfos" :key="index" class="overflow-hidden">
            <el-form-item
              :label="$t('storage.volumes.vmAdd.form.disk.dataDisk') + ':'"
              :prop="'diskInfos'"
              required
              class="w-100"
            >
              <template #label>
                <span v-show="index == 0" class="">{{ $t('storage.volumes.vmAdd.form.disk.dataDisk') }}</span>
              </template>
              <el-form-item
                v-if="!item.diskStatus"
                label=""
                :prop="'diskInfos.' + index + '.volumeId'"
                class="w-100"
                :rules="{
                  required: true,
                  message: $t('storage.volumes.vmAdd.form.disk.selectVolume'),
                  trigger: 'change',
                }"
              >
                <div class="w-100">
                  <el-select
                    v-model="item.volumeId"
                    class="float-left !w-60"
                    :placeholder="$t('storage.volumes.vmAdd.form.disk.selectDisk')"
                  >
                    <el-option
                      v-for="(item, index) in volumesList"
                      :key="index"
                      :disabled="getCheckedVolumes(item.volumeId)"
                      :label="item.name + ' (' + item.size + 'GB)'"
                      :value="item.volumeId"
                    />
                  </el-select>
                  <div class="overflow-hidden block text-right mt-2 ml-2">
                    <div class="overflow-hidden float-left h-4 mx-2">
                      <el-button
                        size="small"
                        circle
                        class="align-top p-0 !w-4 !h-4"
                        type="danger"
                        @click="removeData(form.diskInfos, index)"
                      >
                        <i-ic-baseline-remove></i-ic-baseline-remove>
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-form-item>
              <el-form-item
                v-if="item.diskStatus"
                label=""
                :prop="'diskInfos.' + index + '.size'"
                class="w-100"
                :rules="{
                  required: true,
                  message: $t('storage.volumes.vmAdd.form.disk.inputSize'),
                  trigger: 'blur',
                }"
              >
                <div class="w-100">
                  <el-input-number
                    v-model="item.size"
                    :min="1"
                    :max="2147483647"
                    :precision="0"
                    controls-position="right"
                    class="float-left !w-60"
                  />
                  <span class="pl-2 float-left">GB</span>
                  <div class="overflow-hidden block text-right mt-2 ml-2">
                    <div class="overflow-hidden float-left h-4 mx-2">
                      <el-button
                        size="small"
                        circle
                        class="align-top p-0 !w-4 !h-4"
                        type="danger"
                        @click="removeData(form.diskInfos, index)"
                      >
                        <i-ic-baseline-remove></i-ic-baseline-remove>
                      </el-button>
                    </div>
                  </div>
                </div>
              </el-form-item>
            </el-form-item>
          </div>
          <el-form-item>
            <el-button size="small" class="" :disabled="!form.storagePoolId" type="text" @click="addDisk(false)">
              <i-ic-baseline-add></i-ic-baseline-add>{{ $t('storage.volumes.vmAdd.form.disk.mountExistingVolume') }}
            </el-button>
            <el-button size="small" class="" :disabled="!form.storagePoolId" type="text" @click="addDisk(true)">
              <i-ic-baseline-add></i-ic-baseline-add>{{ $t('storage.volumes.vmAdd.form.disk.createVolume') }}
            </el-button>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('storage.volumes.vmAdd.networkConfig') }}</span>
          </div>
        </template>
        <div class="text item">
          <div v-for="(item, index) in form.networkInfos" :key="index" class="overflow-hidden">
            <el-form-item
              :label="$t('storage.volumes.vmAdd.form.network.title') + ':'"
              :prop="'networkInfos.' + index + '.subnetId'"
              required
              class="float-left w-90"
              :rules="{
                required: true,
                message: $t('storage.volumes.vmAdd.form.network.selectSubnet'),
                trigger: 'change',
              }"
            >
              <template #label>
                <span class="">{{ $t('storage.volumes.vmAdd.form.network.title') }}</span>
                <div class="block text-right">
                  <div v-show="form.networkInfos.length > 1 && index > 0" class="overflow-hidden h-6">
                    <el-button
                      size="small"
                      circle
                      class="align-top p-0 !w-4 !h-4"
                      type="danger"
                      @click="removeData(form.networkInfos, index)"
                    >
                      <i-ic-baseline-remove></i-ic-baseline-remove>
                    </el-button>
                  </div>
                </div>
              </template>
              <el-select
                v-model="item.vpcId"
                class="m-2 ml-0 !w-60"
                :placeholder="$t('storage.volumes.vmAdd.form.network.selectVpc')"
                @change="vpcIdChange(form.networkInfos, index)"
              >
                <el-option
                  v-for="(vpcItem, index) in vpcList"
                  :key="index"
                  :label="vpcItem.name + ' (' + vpcItem.cidr + ')'"
                  :value="vpcItem.vpcId"
                />
              </el-select>
              <el-select
                v-model="item.subnetId"
                :disabled="!item.vpcId"
                class="m-2 ml-0 !w-60"
                :placeholder="$t('storage.volumes.vmAdd.form.network.selectSubnet')"
                @change="staticChange(form.networkInfos, index)"
              >
                <template v-for="(subnetItem, index) in subnetsDataList">
                  <el-option
                    v-if="item.vpcId == subnetItem.vpcId"
                    :key="index"
                    :label="subnetItem.name + ' (' + subnetItem.cidr + ')'"
                    :value="subnetItem.subnetId"
                  />
                </template>
              </el-select>
            </el-form-item>

            <el-form-item :label="$t('storage.volumes.vmAdd.form.network.ipAllocation') + ':'" required>
              <el-radio-group v-model="item.staticStatus" @change="staticChange(form.networkInfos, index)">
                <el-radio :label="0" :value="0">{{ $t('storage.volumes.vmAdd.form.network.fixedIp') }}</el-radio>
                <el-radio :label="1" :value="1">{{
                  $t('storage.volumes.vmAdd.form.network.dynamicAllocation')
                }}</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item
              v-show="item.staticStatus === 0"
              :label="$t('storage.volumes.vmAdd.form.network.fixedIp') + ':'"
              required
            >
              <el-input
                v-model="item.ip1"
                class="!w-15"
                :controls="false"
                :step="1"
                :min="0"
                :max="255"
                step-strictly
                @input="changeIp(form.networkInfos, index)"
              />
              <span class="text-xl px-1">.</span>
              <el-input
                v-model="item.ip2"
                class="!w-15"
                :controls="false"
                :step="1"
                :min="0"
                :max="255"
                step-strictly
                @input="changeIp(form.networkInfos, index)"
              />
              <span class="text-xl px-1">.</span>
              <el-input
                v-model="item.ip3"
                class="!w-15"
                :controls="false"
                :step="1"
                :min="0"
                :max="255"
                step-strictly
                @input="changeIp(form.networkInfos, index)"
              />
              <span class="text-xl px-1">.</span>
              <el-tooltip :visible="item.ipStatus" placement="right">
                <template #content>
                  <span class="inline-block w-4 h-4 bg-red-500 rounded-1/2 text-right leading-tight">！</span>

                  {{ $t('storage.volumes.vmAdd.form.network.ipNotInRange', { range: subnetsDataList.filter((v: any) => { return item.subnetId === v.subnetId; })[0].cidr }) }}
                </template>
                <el-input
                  v-model="item.ip4"
                  class="!w-15"
                  :controls="false"
                  :step="1"
                  :min="0"
                  :max="255"
                  step-strictly
                  @input="changeIp(form.networkInfos, index)"
                />
              </el-tooltip>
            </el-form-item>
          </div>
          <el-form-item class="-mt-2">
            <el-button size="small" class="" type="text" @click="addData()">
              <i-ic-baseline-add></i-ic-baseline-add>{{ $t('storage.volumes.vmAdd.form.network.addNetwork') }}
            </el-button>
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.vmAdd.form.securityGroup.title') + ':'" prop="sgIds">
            <el-select
              v-model="form.sgIds"
              multiple
              class="m-2 ml-0 !w-60"
              :placeholder="$t('storage.volumes.vmAdd.form.securityGroup.selectSecurityGroup')"
            >
              <el-option v-for="(item, index) in secGroupsList" :key="index" :label="item.name" :value="item.sgId" />
            </el-select>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('storage.volumes.vmAdd.advancedConfig') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('storage.volumes.vmAdd.form.hostname') + ':'" prop="hostname">
            <el-input
              v-model="form.hostname"
              class="!w-60"
              :placeholder="$t('storage.volumes.vmAdd.form.hostnamePlaceholder')"
            />
          </el-form-item>
          <el-form-item :label="$t('storage.volumes.vmAdd.form.sysUsername') + ':'" prop="sysUsername">
            <el-input
              v-model="form.sysUsername"
              class="!w-60"
              :disabled="isOs == 1"
              :placeholder="$t('storage.volumes.vmAdd.form.sysUsernamePlaceholder')"
            />
          </el-form-item>

          <el-form-item :label="$t('storage.volumes.vmAdd.form.loginType') + ':'">
            <el-radio-group v-model="loginType">
              <el-radio :label="true" :value="true">{{ $t('storage.volumes.vmAdd.form.password') }}</el-radio>
              <el-radio :label="false" :disabled="nowImageTypeOs == 1" :value="false">{{
                $t('storage.volumes.vmAdd.form.pubkeyId')
              }}</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item
            v-if="loginType"
            :label="$t('storage.volumes.vmAdd.form.password') + ':'"
            prop="isPassword"
            :rules="[
              { required: true, validator: validatePass2, trigger: 'change' },
              { required: true, validator: validatePass2, trigger: 'blur' },
            ]"
          >
            <el-input
              v-model="isPassword"
              class="!w-60"
              type="password"
              :placeholder="$t('storage.volumes.vmAdd.form.passwordPlaceholder')"
            />
          </el-form-item>
          <el-form-item
            v-if="loginType"
            :label="$t('storage.volumes.vmAdd.form.confirmPassword') + ':'"
            prop="sysPassword"
          >
            <el-input
              v-model="form.sysPassword"
              class="!w-60"
              type="password"
              :placeholder="$t('storage.volumes.vmAdd.form.confirmPasswordPlaceholder')"
            />
          </el-form-item>

          <el-form-item v-if="!loginType" :label="$t('storage.volumes.vmAdd.form.pubkeyId') + ':'" prop="pubkeyId">
            <el-select
              v-model="form.pubkeyId"
              class="m-2 ml-0"
              :placeholder="$t('storage.volumes.vmAdd.form.pubkeyIdPlaceholder')"
            >
              <el-option
                v-for="(item, index) in pubkeysDataList"
                :key="index"
                :label="item.name"
                :value="item.pubkeyId"
              />
            </el-select>
          </el-form-item>
        </div>
      </el-card>
      <el-card class="!border-none mb-3">
        <div class="text item text-center">
          <el-button type="primary" @click="toVmAdd()">{{ $t('common.createNow') }}</el-button>
        </div>
      </el-card>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import netmask from 'netmask';
import { Icon } from '@iconify/vue';
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const { Netmask } = netmask;

const mainStoreData = mainStore(); // pinia 信息
const { proxy }: any = getCurrentInstance();

const router = useRouter();
const loading = ref(false);
const addVmForm = ref<any>();
const currentFlavorRow: any = ref(''); // 规格选择 当前行

const nowImageTypeOs: any = ref(''); // 当前镜像类型
const loginType = ref(true);
const isPassword = ref('');
const isOs = ref(0);
const nodeIdAuto = ref(0);
const vmAddType = ref(0);
const imagesTypeList = ref([
  {
    label: 'Windows',
    type: 'Windows',
    typeValue: 1,
    vendor: 0,
    value: 1,
  },
  {
    label: 'Ubuntu',
    type: 'Linux',
    typeValue: 0,
    vendor: 2,
    value: 2,
  },
  {
    label: 'Centos',
    type: 'Linux',
    typeValue: 0,
    vendor: 1,
    value: 3,
  },
  {
    label: proxy.$t('storage.volumes.vmAdd.form.os.blankVm'),
    type: '',
    typeValue: 0,
    vendor: '',
    value: 0,
  },
]);
const emptyImagesList = ref([
  {
    label: 'Windows',
    type: 'Windows',
    typeValue: 1,
    id: 1,
  },
  {
    label: 'Linux',
    type: 'Linux',
    typeValue: 0,
    id: 2,
  },
]);
const form: any = reactive({
  // 表单
  name: '',
  description: '',
  flavorId: '', // 规格ID
  nodeId: '', // 计算节点ID
  imageOsType: 0, // 镜像类型
  imageId: '', // 镜像ID

  hostname: '', // 主机名
  sysUsername: '', // 系统用户名
  sysPassword: '', // 系统密码
  pubkeyId: '', // 密钥对ID
  sgIds: [], // 安全组ID
  networkInfos: [
    {
      vpcId: '', // 虚拟私有云ID
      subnetId: '', // 子网ID
      staticIp: '', // IP分配
      staticStatus: 1, // IP分配类型
      // isVip: false,

      ip1: 0, // IP分配
      ip2: 0, // IP分配
      ip3: 0, // IP分配
      ip4: 0, // IP分配
      ipStatus: false, // IP状态
    },
  ],
  storagePoolId: '', // 存储池ID
  diskInfos: [], // 云硬盘
});

const singleTableRef = ref<any>();
const volumesList: any = ref([]); // 云硬盘列表
const storagePoolsList: any = ref([]); // 存储池列表
const CpuList: any = ref([]);
const MemList: any = ref([]);
const DiskList: any = ref([]);
const flavorCpu = ref<any>();
const flavorMem = ref<any>();
const flavorDisk = ref<any>();

const vpcList: any = ref([]); // 虚拟私有云列表
const imagesList: any = ref([]); // 镜像列表
const subnetsDataList: any = ref([]); // 子网列表
const pubkeysDataList: any = ref([]); // 密钥对列表
const flavorsList: any = ref([]); // 规格列表
const flavorsListData: any = ref([]); // 规格列表
const flavorsListType = ref('info'); // 规格列表类型
const vmsHypervisorNodesList: any = ref([]); // 计算节点列表
const secGroupsList: any = ref([]); // 安全组列表

const validatePass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error(proxy.$t('storage.volumes.vmAdd.validation.inputPassword')));
  } else {
    const reg =
      /^(?=.*\d)(?=.*[a-zA-Z])(?=.*[\~\!\@\#\$\%\^\&\*\(\)\_\+\-\=\|\;\:\'\"\,\.\<\>\/\?])[\da-zA-Z\~\!\@\#\$\%\^\&\*\(\)\_\+\-\=\|\;\:\'\"\,\.\<\>\/\?]{8,20}$/;

    if (value !== isPassword.value) {
      callback(new Error(proxy.$t('storage.volumes.vmAdd.validation.passwordNotMatch')));
    } else if (!reg.test(value)) {
      callback(new Error(proxy.$t('storage.volumes.vmAdd.validation.inputPasswordLength')));
    } else {
      callback();
    }
  }
};
const validatePass2 = (rule: any, value: any, callback: any) => {
  if (isPassword.value === '') {
    callback(new Error(proxy.$t('storage.volumes.vmAdd.validation.inputPassword')));
  } else {
    // 必须包含数字 字母 符号三种组合

    const reg =
      /^(?=.*\d)(?=.*[a-zA-Z])(?=.*[\~\!\@\#\$\%\^\&\*\(\)\_\+\-\=\|\;\:\'\"\,\.\<\>\/\?])[\da-zA-Z\~\!\@\#\$\%\^\&\*\(\)\_\+\-\=\|\;\:\'\"\,\.\<\>\/\?]{8,20}$/;
    console.log(reg.test(isPassword.value));
    if (!reg.test(isPassword.value)) {
      callback(new Error(proxy.$t('storage.volumes.vmAdd.validation.inputPasswordLength')));
    } else {
      callback();
    }
  }
};
const rules = reactive({
  name: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],

  flavorId: [
    {
      required: true,
      message: proxy.$t('storage.volumes.vmAdd.validation.selectFlavor'),
      trigger: 'change',
    },
  ],
  nodeId: [
    {
      required: true,
      message: proxy.$t('storage.volumes.vmAdd.validation.selectNode'),
      trigger: 'change',
    },
  ],
  imageOsType: [
    {
      required: true,
      message: proxy.$t('storage.volumes.vmAdd.validation.selectOs'),
      trigger: 'change',
    },
  ],
  imageId: [
    {
      required: true,
      message: proxy.$t('storage.volumes.vmAdd.validation.selectOs'),
      trigger: 'change',
    },
  ],
  hostname: [
    { required: true, message: proxy.$t('storage.volumes.vmAdd.validation.inputHostname'), trigger: 'blur' },
    { min: 3, max: 64, message: proxy.$t('storage.volumes.vmAdd.validation.hostnameLength'), trigger: 'blur' },
  ],
  sysUsername: [
    { required: true, message: proxy.$t('storage.volumes.vmAdd.validation.inputSysUsername'), trigger: 'blur' },
    { min: 3, max: 64, message: proxy.$t('storage.volumes.vmAdd.validation.sysUsernameLength'), trigger: 'blur' },
  ],
  sysPassword: [
    { required: true, validator: validatePass, trigger: 'change' },
    { required: true, validator: validatePass, trigger: 'blur' },
  ],
  sgIds: [
    {
      required: true,
      message: proxy.$t('storage.volumes.vmAdd.validation.selectSecurityGroup'),
      trigger: 'change',
    },
  ],
  storagePoolId: [
    {
      required: true,
      message: proxy.$t('storage.volumes.vmAdd.validation.selectStoragePool'),
      trigger: 'change',
    },
  ],
});
const nowImageType = ref(0);
const imagesTypeChange = (item: any) => {};
const currentImageRow: any = ref('');

const EmptyImagesHandleCurrentChange = (val: any | undefined) => {
  currentImageRow.value = val;
  form.imageId = '';
  form.imageOsType = val.typeValue;
};
const imagesHandleCurrentChange = (val: any | undefined) => {
  if (val) {
    currentImageRow.value = val;
    form.imageId = val.imageId;
  }
};
const staticChange = (item: any, index: any) => {
  if (item[index].staticStatus === 1) {
    item[index].ipStatus = false;
  } else {
    changeIp(item, index);
  }
};
const ipToint = (ip: any) => {
  let num = 0;
  ip = ip.split('.');
  num = Number(ip[0]) * 256 * 256 * 256 + Number(ip[1]) * 256 * 256 + Number(ip[2]) * 256 + Number(ip[3]);
  num >>>= 0;
  return num;
};
const changeIp = (item: any, index: any) => {
  // IP变动
  item[index].ipStatus = false;
  item[index].ip1 = proxy.$scriptMain.parseIntIpNum(item[index].ip1);
  item[index].ip2 = proxy.$scriptMain.parseIntIpNum(item[index].ip2);
  item[index].ip3 = proxy.$scriptMain.parseIntIpNum(item[index].ip3);
  item[index].ip4 = proxy.$scriptMain.parseIntIpNum(item[index].ip4);
  const ip = `${item[index].ip1}.${item[index].ip2}.${item[index].ip3}.${item[index].ip4}`;
  const subnetIp = subnetsDataList.value.filter((v: any) => {
    return item[index].subnetId === v.subnetId;
  })[0].cidr;

  const netBlock = new Netmask(subnetIp);

  if (netBlock.contains(ip)) {
    console.log('ok');
    if (ipToint(ip) > ipToint(netBlock.first) && ipToint(ip) < ipToint(netBlock.last)) {
      console.log('ok');
    } else {
      item[index].ipStatus = true;
    }
  } else {
    item[index].ipStatus = true;
  }
};

const flavorsHandleCurrentChange = (val: any | undefined) => {
  if (val) {
    currentFlavorRow.value = val;
    form.flavorId = val.flavorId;
    getvmsHypervisorNodesList();
  } else {
    currentFlavorRow.value = '';
    form.flavorId = '';
    vmsHypervisorNodesList.value = [];
  }
};
const currentNodeRow: any = ref('');
const nodeHandleCurrentChange = (val: any | undefined) => {
  if (val) {
    currentNodeRow.value = val;
    form.nodeId = val.nodeId;
  } else {
    currentNodeRow.value = '';
    form.nodeId = '';
  }
};

const goBack = () => {
  router.push('/vm');
};
const resetForm = () => {
  // 重置
  addVmForm.value.resetFields();
  isPassword.value = '';
};
const vpcIdChange = (item: any, index: any) => {
  // vpc 改变
  item[index].subnetId = '';
};
const toVmAdd = () => {
  // 虚拟机add
  const data = JSON.parse(JSON.stringify(form));

  data.diskInfos.map((v: any) => {
    delete v.diskStatus;
  });
  data.diskInfos.unshift({ volumeId: drawerData.item.volumeId });

  const ipStatusData: any = [];
  if (nodeIdAuto.value == 0) {
    delete data.nodeId;
  }
  data.networkInfos.forEach((v: any) => {
    ipStatusData.push(v.ipStatus);
    if (v.staticStatus === 0) {
      // 固定IP or 动态分配
      v.staticIp = `${v.ip1}.${v.ip2}.${v.ip3}.${v.ip4}`;
    } else {
      v.staticIp = '';
    }
    delete v.ip1;
    delete v.ip2;
    delete v.ip3;
    delete v.ip4;
    delete v.ipStatus;
    delete v.staticStatus;
  });

  if (ipStatusData.includes(true)) {
    return;
  }
  if (!loginType.value) {
    data.sysPassword = '';
  }

  loading.value = true;

  addVmForm.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .vmsRenews(data)
        .then((res: any) => {
          loading.value = false;
          proxy.$emit('closeDrawer');

          resetForm();
        })
        .catch((error: any) => {
          resetForm();
          loading.value = false;
        });
    } else {
      loading.value = false;
    }
  });
};
const getVpcList = () => {
  // VPC列表
  mainApi
    .vpcList({ vpc_phase: 1, page_num: 1, page_size: 99999 })
    .then((res: any) => {
      vpcList.value = res.vpcs;
    })
    .catch((error: any) => {});
};
const getImageList = (vendor: any) => {
  // 镜像列表
  mainApi
    .imageList({ is_vm: true, image_os_vendor: vendor, is_ok: true, page_num: 1, page_size: 99999 })
    .then((res: any) => {
      imagesList.value = res.images;
    })
    .catch((error: any) => {});
};
const getSubNetList = () => {
  // 子网列表

  mainApi
    .subnetsList({ page_num: 1, page_size: 99999 })
    .then((res: any) => {
      subnetsDataList.value = res.subnets;
    })
    .catch((error: any) => {});
};
const getpubkeysList = () => {
  // 公钥列表

  mainApi
    .pubkeysList()
    .then((res: any) => {
      pubkeysDataList.value = res.pubkeys;
    })
    .catch((error: any) => {});
};
const filterFlavor = () => {
  if (flavorsListType.value == 'info') {
    flavorsList.value = flavorsListData.value.filter((v: any) => {
      return !v.gpuCount;
    });
  } else {
    flavorsList.value = flavorsListData.value.filter((v: any) => {
      return v.gpuCount && v.gpuCount > 0;
    });
  }
};
const getFlavorList = () => {
  // 规格列表

  mainApi
    .flavorsList({ type: 1, page_num: 1, page_size: 99999 })
    .then((res: any) => {
      CpuList.value = [];
      MemList.value = [];
      DiskList.value = [];
      flavorsListData.value = res.flavors;
      filterFlavor();

      if (res.flavors && res.flavors.length > 0) {
        currentFlavorRow.value = res.flavors[0];
        form.flavorId = res.flavors[0].flavorId;
        getvmsHypervisorNodesList();
      }
      res.flavors.forEach((item: any) => {
        CpuList.value.push(item.cpu);
        MemList.value.push(item.mem);
        DiskList.value.push(item.rootDisk);
      });
      flavorCpu.value = Array.from(new Set(CpuList.value))
        .sort((a: any, b: any) => a - b)
        .map((item: any) => {
          return {
            text: `${item}${proxy.$t('common.core')}`,
            value: item,
          };
        });
      flavorMem.value = Array.from(new Set(MemList.value))
        .sort((a: any, b: any) => a - b)
        .map((item: any) => {
          return {
            text: `${item}GB`,
            value: item,
          };
        });
      flavorDisk.value = Array.from(new Set(DiskList.value))
        .sort((a: any, b: any) => a - b)
        .map((item: any) => {
          return {
            text: `${item}GB`,
            value: item,
          };
        });
    })
    .catch((error: any) => {});
};
const clearFilter = () => {
  singleTableRef.value!.clearFilter();
};
const filterCpu = (value: string, row: any, column: any) => {
  const { property } = column;
  return row[property] === value;
};
const filterMem = (value: string, row: any, column: any) => {
  const { property } = column;
  return row[property] === value;
};
const filterDisk = (value: string, row: any, column: any) => {
  const { property } = column;
  return row[property] === value;
};
const addData = () => {
  // 添加数据
  form.networkInfos.push({
    vpcId: '', // 虚拟私有云ID
    subnetId: '', // 子网ID
    staticIp: '', // IP分配
    staticStatus: 1, // IP分配类型
    // isVip: false,
    ip1: 0, // IP分配
    ip2: 0, // IP分配
    ip3: 0, // IP分配
    ip4: 0, // IP分配
    ipStatus: false, // IP状态
  });
  proxy.$forceUpdate();
};
const addDisk = (status: boolean) => {
  // 添加数据盘
  if (form.diskInfos.length >= 4) {
    ElMessage.warning(proxy.$t('storage.volumes.vmAdd.message.maxDataDisks'));
    return;
  }
  form.diskInfos.push({
    diskStatus: status, // 状态
    size: 10, // 数据盘大小
    diskType: 0, // 数据盘类型
    volumeId: '', // 云盘ID
  });
  proxy.$forceUpdate();
};

const removeData = (data: any, index: any) => {
  // 删除数据
  data.splice(index, 1);
};
const getCheckedVolumes = (data: any) => {
  // 获取选中的云盘
  const volumeIdData = form.diskInfos.map((item: any, index: any) => {
    return item.volumeId;
  });
  return volumeIdData.includes(data);
};
const getsecGroupsList = () => {
  // 安全组列表

  mainApi
    .sgsList({ page_num: 1, page_size: 99999 })
    .then((res: any) => {
      secGroupsList.value = res.securityGroups;
      if (res.securityGroups && res.securityGroups.length > 0) {
        form.sgIds = [res.securityGroups[0].sgId];
      }
    })
    .catch((error: any) => {
      loading.value = false;
    });
};
const vmsHypervisorNodesLoading = ref(false);
const getvmsHypervisorNodesList = () => {
  // 计算节点列表
  if (!currentFlavorRow.value) {
    return;
  }
  currentNodeRow.value = '';
  form.nodeId = '';
  vmsHypervisorNodesLoading.value = true;

  mainApi
    .vmsHypervisorNodesAllocation({
      // is_healthy: true,
      // is_gpu: true,
      flavor_id: currentFlavorRow.value.flavorId,
      page_num: 1,
      page_size: 99999,
    })
    .then((res: any) => {
      vmsHypervisorNodesList.value = res.nodeAllocationInfos;

      vmsHypervisorNodesLoading.value = false;
    })
    .catch((error: any) => {
      vmsHypervisorNodesLoading.value = false;
    });
};
const changeStoragePool = (storagePoolId: any) => {
  // 改变存储池
  form.diskInfos = form.diskInfos.filter((item: any) => {
    return item.diskStatus === true;
  });
  getVolumesList(storagePoolId);
};
const getVolumesList = (storagePoolId: any) => {
  // 云盘列表
  mainApi
    .volumesList({ storage_pool_id: storagePoolId, detached: true, page_num: 1, page_size: 99999 })
    .then((res: any) => {
      volumesList.value = res.volumes;
    });
};
const getStoragePools = () => {
  // 存储池列表
  mainApi.storagePoolsList({ page_num: 1, page_size: 99999 }).then((res: any) => {
    storagePoolsList.value = res.storagePools;
    if (res.storagePools && res.storagePools.length > 0) {
      form.storagePoolId = res.storagePools[0].poolId;
    }
  });
};

watch(loginType, (newValue) => {
  if (newValue) {
    form.pubkeyId = '';
  }
});
watch(vmAddType, (newValue) => {
  currentNodeRow.value = '';
  form.nodeId = '';
});
watch(flavorsListType, (newValue) => {
  currentFlavorRow.value = '';
  form.flavorId = '';
  filterFlavor();
});
// watch(
//   () => form.imageId,
//   (newVal, oldVal) => {
//     if (newVal) {
//       isOs.value = imagesList.value.filter((item: any) => item.imageId === newVal)[0].imageOsType;
//       nowImageTypeOs.value = isOs.value;
//       loginType.value = true;
//       if (isOs.value === 1) {
//         form.sysUsername = 'Administrator';
//       } else {
//         form.sysUsername = 'cloud';
//       }
//     }
//   },
// );
watch(
  () => form.imageOsType,
  (newVal, oldVal) => {
    nowImageTypeOs.value = form.imageOsType;
    loginType.value = true;
    if (form.imageOsType === 1) {
      form.sysUsername = 'Administrator';
    } else {
      form.sysUsername = 'cloud';
    }
  },
);
const getDetail = () => {
  // 获取详情
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .volumesDetail(id)
    .then((res: any) => {
      form.storagePoolId = res.storagePoolId;
      form.imageOsType = res.imageOsType;
      isOs.value = res.imageOsType;

      nowImageType.value = res.imageOsType;
      if (res.imageOsVendor === 0) {
        nowImageType.value = 1;
        form.imageId = res.imageId;
      } else if (res.imageOsVendor === 1) {
        nowImageType.value = 3;
        form.imageId = res.imageId;
      } else if (res.imageOsVendor === 2) {
        nowImageType.value = 2;
        form.imageId = res.imageId;
      } else {
        nowImageType.value = 0;
        emptyImagesList.value = emptyImagesList.value.filter((item: any) => {
          return item.typeValue === res.imageOsType;
        });
      }

      if (res.imageOsType == 1) {
        form.sysUsername = 'Administrator';
      } else {
        form.sysUsername = 'cloud';
      }
    })
    .catch((error: any) => {});
};

onMounted(() => {
  getDetail();

  getVpcList(); // VPC列表
  // getImageList(); //镜像列表
  getSubNetList(); // 子网列表
  getpubkeysList(); // 公钥列表
  getFlavorList(); // 规格列表
  getVolumesList(form.storagePoolId); // 云盘列表
  getStoragePools(); // 存储池列表
  getsecGroupsList(); // 安全组列表
});
</script>

<style lang="scss" scoped>
.vmAddPage {
}
</style>
