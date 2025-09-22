<template>
  <div class="vmsInstabcesDetailPage h-full relative">
    <h5 v-if="!(drawerData && drawerData.isDrawer)" class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md overflow-hidden">
      <el-page-header :title="$t('common.backToList')" class="float-left" @back="goBack">
        <template #content> {{ $route.meta.title }} </template>
      </el-page-header>
    </h5>
    <div
      class="float-right absolute z-10"
      :class="drawerData && drawerData.isDrawer ? 'right-0 -top-5' : 'right-4 top-2'"
    >
      <el-button
        :size="mainStoreData.viewSize.listSet"
        type="primary"
        class="float-left mt-0.5 mr-2"
        @click="restartDetail()"
      >
        <img src="@/assets/img/btn/restart_b.png" class="w-3 mr-1" alt="" />
        {{ $t('common.refresh') }}
      </el-button>
      <operate
        :key="vmDetailData.instanceId"
        :prop-vm-detail="vmDetailData"
        class="float-left"
        :prop-show-type="2"
        :prop-show-btn="[
          'poweron',
          'poweroff',
          'reboot',
          'delete',
          'edit',
          'resetPassword',
          'snaps',
          'flavor',
          'images',
          'transfer',
          'vnc',
        ]"
        @initVmList="getDetail"
      />
    </div>
    <el-tabs
      v-model="activeName"
      class="demo-tabs bg-white vmDetailTabs"
      :class="drawerData && drawerData.isDrawer ? 'pt-4' : ''"
    >
      <el-tab-pane :label="$t('compute.vm.info.basicInfo')" name="first"></el-tab-pane>
      <el-tab-pane :label="$t('compute.vm.info.networkConfig')" name="second"></el-tab-pane>
      <el-tab-pane :label="$t('compute.vm.info.storageConfig')" name="sixth"></el-tab-pane>
      <el-tab-pane :label="$t('compute.vm.info.pciDevices')" name="fifth"></el-tab-pane>
      <el-tab-pane :label="$t('compute.vm.info.advancedConfig')" name="third"></el-tab-pane>
      <el-tab-pane :label="$t('compute.vm.info.monitor')" name="fourth"></el-tab-pane>
      <el-tab-pane :label="$t('compute.vm.info.log')" name="seventh"></el-tab-pane>
      <el-tab-pane :label="$t('compute.vm.info.event')" name="eighth"></el-tab-pane>
    </el-tabs>
    <el-form :model="form" label-width="120px" :size="mainStoreData.viewSize.main">
      <el-card v-if="activeName == 'first'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('compute.vm.info.basicInfo') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-row>
            <el-col :span="12">
              <el-form-item :label="$t('compute.vm.form.name') + ':'">
                <span>{{ form.name || '-' }}</span>
              </el-form-item>
              <el-form-item :label="$t('common.id') + ':'">
                <span>{{ form.instanceId || '-' }}</span>
              </el-form-item>
              <el-form-item :label="$t('compute.vm.form.description') + ':'">
                <span>{{ form.description || '-' }}</span>
              </el-form-item>
              <el-form-item :label="$t('compute.vm.form.imageType') + ':'">
                <span v-if="form.imageOsType == 0">Linux</span>
                <span v-else-if="form.imageOsType == 1">Windows</span>
                <span v-else>-</span>
              </el-form-item>
              <el-form-item :label="$t('compute.vm.form.os') + ':'">
                <span>{{ form.imageName || '-' }}</span>
              </el-form-item>
              <el-form-item :label="$t('compute.vm.form.computeNode') + ':'">
                <span>
                  <router-link :to="'/hypervisorNodes/' + form.hypervisorNodeId" class="text-blue-400">{{
                    form.hypervisorNodeName || '-'
                  }}</router-link>
                </span>
              </el-form-item>

              <el-form-item :label="$t('compute.vm.form.status') + ':'">
                <el-tag
                  :size="mainStoreData.viewSize.tagStatus"
                  :type="filtersFun.getVmStatus(form.phaseStatus, 'tag')"
                  >{{ filtersFun.getVmStatus(form.phaseStatus, 'status') }}</el-tag
                >
                <span v-if="form.phaseStatus == 66">
                  <el-tooltip
                    class="box-item"
                    effect="dark"
                    :content="$t('compute.vm.form.insufficientComputeResources')"
                    placement="top"
                  >
                    <span class="inline-block ml-1">
                      <i-bi:info-square class="inline-block"></i-bi:info-square>
                    </span>
                  </el-tooltip>
                </span>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item :label="$t('compute.vm.form.flavorName') + ':'">
                <span>
                  <router-link :to="'/flavors/' + form.flavorId" class="text-blue-400">{{
                    form.flavorName || '-'
                  }}</router-link>
                </span>
              </el-form-item>
              <el-form-item :label="$t('compute.vm.form.cpu') + ':'">
                <span> {{ form.cpu || '-' }}{{ $t('common.core') }} </span>
              </el-form-item>
              <el-form-item :label="$t('compute.vm.form.memory') + ':'">
                <span> {{ form.mem || '-' }}GB </span>
              </el-form-item>
              <el-form-item :label="$t('compute.vm.form.rootDisk') + ':'">
                <span> {{ form.rootDisk || '-' }}GB </span>
              </el-form-item>
              <el-form-item :label="$t('compute.vm.form.gpu') + ':'">
                <span>
                  {{
                    form.pciInfos && form.pciInfos.length > 0 ? form.pciInfos.length : $t('compute.vm.form.unmounted')
                  }}
                </span>
              </el-form-item>

              <el-form-item :label="$t('compute.vm.form.createTime') + ':'">
                <span>{{ form.createTime || '-' }}</span>
              </el-form-item>
              <el-form-item :label="$t('compute.vm.form.updateTime') + ':'">
                <span>{{ form.updateTime || '-' }}</span>
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-card>
      <el-card v-if="activeName == 'first'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('compute.vm.info.bootDev') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('compute.vm.info.bootDev') + ':'" label-width="150px">
            <!-- <span v-if="form.bootDev=='hd'">HD硬盘</span>
            <span v-if="form.bootDev=='cdrom'">CDROM光驱</span> -->
            <!-- <el-button :size="mainStoreData.viewSize.listSet"
                       type="primary"
                       class="ml-2"
                       @click="changeBootDev(form.bootDev=='hd'?'cdrom':'hd')">
              切换至 {{form.bootDev=='hd'?'CDROM光驱':'HD硬盘'}}
            </el-button> -->
            <el-dropdown class="mr-2">
              <el-button size="small" type="primary">
                {{
                  form.bootDev == 'hd'
                    ? $t('compute.vm.info.hdDisk')
                    : form.bootDev == 'cdrom'
                    ? $t('compute.vm.info.cdrom')
                    : '-'
                }}<el-icon class="el-icon--right">
                  <i-ic-twotone-keyboard-arrow-down></i-ic-twotone-keyboard-arrow-down>
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="form.bootDev != 'hd'" @click="changeBootDev('hd')">{{
                    $t('compute.vm.info.changeBootDev')
                  }}</el-dropdown-item>
                  <el-dropdown-item v-if="form.bootDev != 'cdrom'" @click="changeBootDev('cdrom')">{{
                    $t('compute.vm.info.changeBootDev2')
                  }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </el-form-item>

          <el-form-item :label="$t('compute.vm.info.isoImage') + ':'" class="w-full" label-width="150px">
            <div class="block w-full">
              <el-button :size="mainStoreData.viewSize.listSet" type="primary" class="" @click="openNewPage()">
                {{ $t('compute.vm.info.connectVirtualMedia') }}
              </el-button>
            </div>
          </el-form-item>
        </div>
      </el-card>
      <el-card v-if="activeName == 'second'" class="!border-none mb-3 w-full">
        <template #header>
          <div class="">
            <span>{{ $t('compute.vm.info.networkConfig') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('compute.vm.form.networkInfo') + ':'">
            <el-table :data="form.networkDetailInfos" class="w-auto">
              <el-table-column :label="$t('compute.vm.form.vpc')" width="220">
                <template #default="scope">
                  <span>
                    <router-link :to="'/vpc/' + scope.row.vpcId" class="text-blue-400">{{
                      scope.row.vpcName
                    }}</router-link>
                  </span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('compute.vm.form.subnet')" width="220">
                <template #default="scope">
                  <span>
                    <router-link :to="'/subnet/' + scope.row.subnetId" class="text-blue-400">{{
                      scope.row.subnetName
                    }}</router-link>
                  </span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('compute.vm.form.ip')" width="180">
                <template #default="scope">
                  <span>{{ scope.row.ipAddress || '-' }}</span>
                </template>
              </el-table-column>
              <el-table-column :label="$t('compute.vm.form.eip')" width="180">
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
                        <el-tag :size="mainStoreData.viewSize.tagStatus">{{ $t('compute.vm.form.nat') }}</el-tag>
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
            </el-table>
          </el-form-item>
          <el-form-item :label="$t('compute.vm.form.securityGroup') + ':'">
            <div class="text-right block w-full">
              <el-button
                v-if="!sortStatus"
                type="primary"
                class="float-right mx-2"
                :size="mainStoreData.viewSize.main"
                @click="openSort()"
                >{{ $t('compute.vm.form.sort') }}</el-button
              >
              <el-button type="primary" class="float-right" :size="mainStoreData.viewSize.main" @click="addInstance()">
                {{ $t('compute.vm.form.associateSecurityGroup') }}
              </el-button>
            </div>

            <el-table
              :size="mainStoreData.viewSize.main"
              :data="detailSgsListData"
              row-key="sgId"
              class="w-full moveTable"
            >
              <el-table-column v-if="!sortStatus" type="expand">
                <template #default="scope">
                  <div class="px-20 pb-10">
                    <div class="overflow-hidden">
                      <el-radio-group
                        v-model="rulesSwitch[scope.$index]"
                        class="float-left"
                        :size="mainStoreData.viewSize.tabChange"
                      >
                        <el-radio-button :label="0" :value="0">{{ $t('compute.vm.form.inDirection') }}</el-radio-button>

                        <el-radio-button :label="1" :value="1">{{
                          $t('compute.vm.form.outDirection')
                        }}</el-radio-button>
                      </el-radio-group>
                      <el-button
                        class="float-right mt-1"
                        type="primary"
                        :size="mainStoreData.viewSize.tabChange"
                        @click="addRule(scope.row.sgId, rulesSwitch[scope.$index])"
                        >{{ $t('compute.vm.form.addRule') }}</el-button
                      >
                    </div>
                    <el-table
                      :data="scope.row.rules.filter((v: any) => {
      return v.direction === rulesSwitch[scope.$index];
    })"
                      style="width: 100%"
                    >
                      <el-table-column prop="priority" width="160" :label="$t('compute.vm.form.priority')">
                        <template #header>
                          {{ $t('compute.vm.form.priority') }}

                          <el-tooltip placement="top" effect="dark">
                            <template #content>
                              <div class="w-200px">
                                {{ $t('compute.vm.form.priorityRange') }}
                              </div>
                            </template>
                            <span class="text-xs inline-block align-middle cursor-pointer">
                              <i-bi:info-square></i-bi:info-square>
                            </span>
                          </el-tooltip>
                        </template>
                        <template #default="scope">
                          <span class="block">{{ scope.row.priority }}</span>
                          <div class="leading-tight">
                            <small>(id:{{ scope.row.ruleId }})</small>
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column prop="action" :label="$t('compute.vm.form.policy')">
                        <template #default="scope">
                          {{
                            scope.row.action === 0
                              ? $t('compute.vm.form.deny')
                              : scope.row.action === 1
                              ? $t('compute.vm.form.allow')
                              : '-'
                          }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="name" :label="$t('compute.vm.form.protocolPort')">
                        <template #default="props"
                          >{{
                            props.row.protocol === 0
                              ? 'TCP:'
                              : props.row.protocol === 1
                              ? 'UDP:'
                              : props.row.protocol === 3
                              ? '全部'
                              : props.row.protocol === 4
                              ? 'ICMP:'
                              : ''
                          }}
                          <span v-if="props.row.protocol === 0">
                            <el-tag
                              v-for="(item, index) in getport(props.row.port)"
                              :key="index"
                              size="small"
                              class="mx-0.3"
                              >{{ item }}</el-tag
                            >
                          </span>
                          <span v-if="props.row.protocol === 1">
                            <el-tag
                              v-for="(item, index) in getport(props.row.port)"
                              :key="index"
                              size="small"
                              class="mx-0.3"
                              >{{ item }}</el-tag
                            >
                          </span>
                          <span v-if="props.row.protocol === 3"></span>
                          <span v-if="props.row.protocol === 4">
                            <el-tag
                              v-for="(item, index) in getport(props.row.port)"
                              :key="index"
                              size="small"
                              class="mx-0.3"
                              >{{ getICMPname(item == 'all' ? '0' : item) }}</el-tag
                            >
                          </span>
                        </template>
                      </el-table-column>
                      <el-table-column prop="name" :label="$t('compute.vm.form.type')">
                        <template #default="props">
                          {{
                            props.row.addressType === 0 ? 'IPv4' : props.row.addressType === 1 ? 'IPv6' : '-'
                          }}</template
                        >
                      </el-table-column>
                      <el-table-column prop="name" :label="$t('compute.vm.form.sourceAddress')">
                        <template #default="props">
                          <span v-if="props.row.addressRef && props.row.addressRef.cidr">{{
                            props.row.addressRef.cidr
                          }}</span>
                          <span v-if="props.row.addressRef && props.row.addressRef.sgId">{{
                            getSg(props.row.addressRef)
                          }}</span>
                        </template>
                      </el-table-column>
                      <el-table-column prop="description" :label="$t('compute.vm.form.description')"> </el-table-column>
                      <el-table-column
                        v-if="!sortStatus"
                        prop="address"
                        :label="$t('compute.vm.form.action')"
                        width="120"
                      >
                        <template #default="props">
                          <span
                            class="text-blue-400 cursor-pointer"
                            @click="editRule(scope.row.sgId, rulesSwitch[scope.$index], props.row)"
                            >{{ $t('compute.vm.operation.edit') }}</span
                          >
                          <el-popconfirm
                            :confirm-button-text="$t('compute.vm.operation.delete')"
                            :cancel-button-text="$t('common.cancel')"
                            icon-color="#626AEF"
                            :title="$t('compute.vm.message.confirmDeleteRule')"
                            @confirm="toDeleteRule(props.row)"
                          >
                            <template #reference>
                              <span class="text-blue-400 cursor-pointer ml-2">{{
                                $t('compute.vm.operation.delete')
                              }}</span>
                            </template>
                          </el-popconfirm>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>
                </template>
              </el-table-column>
              <el-table-column v-if="!sortStatus" width="120">
                <template #header>
                  {{ $t('compute.vm.form.priority') }}
                  <el-tooltip placement="top" effect="dark">
                    <template #content>
                      <div class="w-200px">
                        {{ $t('compute.vm.form.priorityRangeTip') }}
                      </div>
                    </template>
                    <span class="text-xs inline-block align-middle cursor-pointer">
                      <i-bi:info-square></i-bi:info-square>
                    </span>
                  </el-tooltip>
                </template>
                <template #default="scope">
                  <span>{{ scope.$index + 1 }}</span>
                </template>
              </el-table-column>
              <el-table-column v-if="sortStatus" width="120">
                <template #default="scope">
                  <span class="move cursor-pointer">
                    <img src="@/assets/img/sortBtn.png" class="w-7" />
                  </span>
                </template>
              </el-table-column>

              <el-table-column prop="sgName" :label="$t('compute.vm.form.securityGroup')">
                <template #default="scope">
                  <span>
                    {{ scope.row.sgName }}
                    <router-link :to="'/secGroups/' + scope.row.sgId + '?type=info'">
                      <br />
                      <small class="inline-block text-blue-400">(Id:{{ scope.row.sgId }})</small>
                    </router-link>
                  </span>
                </template>
              </el-table-column>

              <el-table-column prop="description" :label="$t('compute.vm.form.description')"> </el-table-column>

              <el-table-column
                v-if="!sortStatus"
                prop="address"
                :label="$t('compute.vm.operation.unbound')"
                width="120"
              >
                <template #default="scope">
                  <el-popconfirm
                    :confirm-button-text="$t('compute.vm.operation.unbound')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('compute.vm.message.confirmUnbound')"
                    @confirm="toUnbound(scope.row.sgId)"
                  >
                    <template #reference>
                      <span class="text-blue-400 cursor-pointer">{{ $t('compute.vm.operation.unbound') }}</span>
                    </template>
                  </el-popconfirm>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="sortStatus" class="block w-full pt-2">
              <el-button class="mx-2" :size="mainStoreData.viewSize.main" @click="cancelSort()">{{
                $t('common.cancel')
              }}</el-button>
              <el-button type="primary" class="" :size="mainStoreData.viewSize.main" @click="saveSort()">{{
                $t('common.save')
              }}</el-button>
            </div>
          </el-form-item>
        </div>
      </el-card>
      <el-card v-if="activeName == 'sixth'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('compute.vm.info.storageConfig') }}</span>
            <el-button
              class="float-right"
              :size="mainStoreData.viewSize.tabChange"
              type="primary"
              @click="addVolumes"
              >{{ $t('compute.vm.form.addVolume') }}</el-button
            >
          </div>
        </template>
        <div class="text item">
          <el-table
            :size="mainStoreData.viewSize.main"
            :data="form.diskInfos"
            max-height="calc(100% - 3rem)"
            class="!overflow-y-auto"
            stripe
            :scrollbar-always-on="false"
          >
            <el-table-column prop="volumeName" :label="$t('compute.vm.form.name')" width="400">
              <template #default="scope">
                <span>{{ scope.row.volumeName }}</span>
              </template>
            </el-table-column>

            <el-table-column prop="type" :label="$t('compute.vm.form.type')">
              <template #default="scope">
                <span v-if="scope.row.type == 2">{{ $t('compute.vm.form.fileSystem') }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="size" :label="$t('compute.vm.form.size')">
              <template #default="scope">
                <span>{{ scope.row.size }}GB</span>
              </template>
            </el-table-column>
            <el-table-column prop="phaseStatus" :label="$t('compute.vm.form.status')">
              <template #default="scope">
                <el-tag
                  :size="mainStoreData.viewSize.tagStatus"
                  :type="filtersFun.getVolumeStatus(scope.row.phaseStatus, 'tag')"
                  >{{ filtersFun.getVolumeStatus(scope.row.phaseStatus, 'status') }}</el-tag
                >
              </template>
            </el-table-column>
            <el-table-column :label="$t('compute.vm.form.action')" width="120">
              <template #default="scope">
                <el-dropdown trigger="click" :size="mainStoreData.viewSize.listSet">
                  <el-button type="text" :size="mainStoreData.viewSize.listSet">
                    {{ $t('compute.vm.form.action')
                    }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-popconfirm
                        :confirm-button-text="$t('compute.vm.operation.detach')"
                        :cancel-button-text="$t('common.cancel')"
                        icon-color="#626AEF"
                        :title="$t('compute.vm.message.confirmDetach')"
                        @confirm="toVolumesDetach(scope.row)"
                      >
                        <template #reference>
                          <span class="listDelBtn"
                            ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                              $t('compute.vm.operation.detach')
                            }}</span
                          >
                        </template>
                      </el-popconfirm>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
      <el-card v-if="activeName == 'fifth'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('compute.vm.info.pciDevices') }}</span>
            <el-button class="float-right" :size="mainStoreData.viewSize.tabChange" type="primary" @click="addPci">{{
              $t('compute.vm.form.addDevice')
            }}</el-button>
          </div>
        </template>
        <div class="text item">
          <el-table
            :size="mainStoreData.viewSize.main"
            :data="form.pciInfos"
            max-height="calc(100% - 3rem)"
            class="!overflow-y-auto"
            stripe
            :scrollbar-always-on="false"
          >
            <el-table-column prop="pciDeviceName" :label="$t('compute.vm.form.name')" width="400">
              <template #default="scope">
                <span>{{ scope.row.pciDeviceName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="pciDeviceGroupId" :label="$t('compute.vm.form.group')">
              <template #default="scope">
                <span>{{ scope.row.pciDeviceGroupId }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="pciDeviceType" :label="$t('compute.vm.form.type')">
              <template #default="scope">
                <span>{{ scope.row.pciDeviceType }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="phaseStatus" :label="$t('compute.vm.form.status')">
              <template #default="scope">
                <el-tag
                  :size="mainStoreData.viewSize.tagStatus"
                  :type="filtersFun.getPciStatus(scope.row.phaseStatus, 'tag')"
                  >{{ filtersFun.getPciStatus(scope.row.phaseStatus, 'status') }}</el-tag
                >
              </template>
            </el-table-column>

            <el-table-column prop="createTime" :label="$t('compute.vm.form.createTime')" />
            <el-table-column :label="$t('compute.vm.form.action')" width="120">
              <template #default="scope">
                <el-dropdown trigger="click" :size="mainStoreData.viewSize.listSet">
                  <el-button type="text" :size="mainStoreData.viewSize.listSet">
                    {{ $t('compute.vm.form.action')
                    }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-popconfirm
                        :confirm-button-text="$t('compute.vm.operation.detach')"
                        :cancel-button-text="$t('common.cancel')"
                        icon-color="#626AEF"
                        :title="$t('compute.vm.message.confirmDetach')"
                        @confirm="toPciDetach(scope.row)"
                      >
                        <template #reference>
                          <span class="listDelBtn"
                            ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                              $t('compute.vm.operation.detach')
                            }}</span
                          >
                        </template>
                      </el-popconfirm>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
      <el-card v-if="activeName == 'third'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('compute.vm.info.advancedConfig') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-form-item :label="$t('compute.vm.form.hostname') + ':'">
            <span>{{ form.hostname || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('compute.vm.form.loginUser') + ':'">
            <span>{{ form.sysUsername || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('compute.vm.form.loginCredential') + ':'">
            <span>{{ form.pubkeyId ? $t('compute.vm.form.keyPair') : $t('compute.vm.form.password') }}</span>
          </el-form-item>

          <el-form-item v-if="form.pubkeyId" :label="$t('compute.vm.form.keyPair') + ':'">
            <span>
              <router-link :to="'/publicKey/' + form.pubkeyId" class="text-blue-400">{{ form.pubkeyId }}</router-link>
            </span>
          </el-form-item>
          <el-form-item :label="$t('compute.vm.form.snapshot') + ':'">
            <el-table :size="mainStoreData.viewSize.main" :data="form.snapInfos">
              <el-table-column prop="date" :label="$t('compute.vm.form.name')">
                <template #default="scope">
                  <router-link :to="'/snaps/' + scope.row.snapId" class="text-blue-400">{{
                    scope.row.snapName || '-'
                  }}</router-link>
                </template>
              </el-table-column>

              <el-table-column prop="phaseStatus" :label="$t('compute.vm.form.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="$t('compute.vm.form.createTime')" />
              <el-table-column :label="$t('compute.vm.form.action')" width="120">
                <template #default="scope">
                  <el-dropdown trigger="click" :size="mainStoreData.viewSize.listSet">
                    <el-button type="text" :size="mainStoreData.viewSize.listSet">
                      {{ $t('compute.vm.form.action')
                      }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-popconfirm
                          :confirm-button-text="$t('compute.vm.operation.delete')"
                          :cancel-button-text="$t('common.cancel')"
                          icon-color="#626AEF"
                          :title="$t('compute.vm.message.confirmDeleteSnap')"
                          @confirm="toDeleteSnaps(scope.row)"
                        >
                          <template #reference>
                            <span class="listDelBtn"
                              ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                                $t('compute.vm.operation.delete')
                              }}</span
                            >
                          </template>
                        </el-popconfirm>
                      </el-dropdown-menu>
                      <el-dropdown-menu>
                        <el-popconfirm
                          :confirm-button-text="$t('common.confirm')"
                          :cancel-button-text="$t('common.cancel')"
                          icon-color="#626AEF"
                          :title="$t('compute.vm.message.confirmRestoreSnap')"
                          @confirm="toRestore(scope.row)"
                        >
                          <template #reference>
                            <span class="listDelBtn"
                              ><img src="@/assets/img/btn/rollBack.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                                $t('compute.vm.operation.rollback')
                              }}</span
                            >
                          </template>
                        </el-popconfirm>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </template>
              </el-table-column>
            </el-table>
          </el-form-item>
        </div>
      </el-card>
      <el-card v-if="activeName == 'fourth'" class="!border-none" :body-style="{ padding: '0px' }">
        <template #header>
          <div class="">
            <span>{{ $t('compute.vm.info.monitor') }}</span>
          </div>
        </template>
        <div v-if="vmPanelsStatus" style="height: 1395px">
          <iframe width="100%" height="100%" :src="getVmPanelsUrl()" frameborder="0"></iframe>
        </div>
        <div
          v-if="gpuPanelsStatus && vmDetailData && vmDetailData.pciInfos && vmDetailData.pciInfos.length > 0"
          style="height: 940px"
        >
          <iframe width="100%" height="100%" :src="getVmGpuPanelsUrl()" frameborder="0"></iframe>
        </div>
      </el-card>
      <el-card v-if="activeName == 'seventh'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('compute.vm.info.log') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-table
            :size="mainStoreData.viewSize.main"
            :data="logList"
            class="!overflow-y-auto"
            stripe
            :scrollbar-always-on="false"
          >
            <el-table-column prop="description" width="700" :label="$t('compute.vm.form.description')">
              <template #default="scope">
                <span>{{ scope.row.description }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="resource" :label="$t('compute.vm.form.operationResource')">
              <template #default="scope">
                {{ scope.row.resource }}
              </template>
            </el-table-column>
            <el-table-column prop="operator" :label="$t('compute.vm.form.operator')">
              <template #default="scope">
                {{ scope.row.operator }}
              </template>
            </el-table-column>
            <el-table-column prop="createTime" :label="$t('compute.vm.form.operationTime')">
              <template #default="scope">
                {{ scope.row.createTime }}
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:page_num="logForm.page_num"
            v-model:page-size="logForm.page_size"
            class="!py-4 !pr-8 float-right"
            :page-sizes="mainStoreData.page_sizes"
            :current-page="logForm.page_num"
            :small="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="logForm.total"
            @size-change="logHandleSizeChange"
            @current-change="logHandleCurrentChange"
          />
        </div>
      </el-card>
      <el-card v-if="activeName == 'eighth'" class="!border-none mb-3">
        <template #header>
          <div class="">
            <span>{{ $t('compute.vm.info.event') }}</span>
          </div>
        </template>
        <div class="text item">
          <el-table
            :size="mainStoreData.viewSize.main"
            :data="eventList"
            class="!overflow-y-auto"
            stripe
            :scrollbar-always-on="false"
          >
            <el-table-column prop="content" :label="$t('compute.vm.form.event')">
              <template #default="scope">
                {{ scope.row.content }}
              </template>
            </el-table-column>
            <el-table-column prop="detailInfo" :label="$t('compute.vm.form.detailInfo')">
              <template #default="scope">
                {{ scope.row.detailInfo }}
              </template>
            </el-table-column>

            <el-table-column prop="result" :label="$t('compute.vm.form.result')">
              <template #default="scope">
                {{ scope.row.result }}
              </template>
            </el-table-column>
            <el-table-column prop="userId" :label="$t('compute.vm.form.initiator')">
              <template #default="scope">
                <span class="block">{{ scope.row.username }}</span>
                <span
                  ><small>({{ scope.row.userId }})</small></span
                >
              </template>
            </el-table-column>
            <el-table-column prop="createTime" :label="$t('compute.vm.form.triggerTime')" />
          </el-table>
          <el-pagination
            v-model:page_num="eventForm.page_num"
            v-model:page-size="eventForm.page_size"
            class="!py-4 !pr-8 float-right"
            :page-sizes="mainStoreData.page_sizes"
            :current-page="eventForm.page_num"
            :small="true"
            layout="total, sizes, prev, pager, next, jumper"
            :total="eventForm.total"
            @size-change="eventHandleSizeChange"
            @current-change="eventHandleCurrentChange"
          />
        </div>
      </el-card>
    </el-form>
    <!-- 关联安全组 start -->
    <el-dialog
      v-model="addInstanceDialog"
      v-loading="addInstanceLoading"
      :element-loading-text="$t('common.loading')"
      :title="$t('compute.vm.form.associateSecurityGroup')"
      width="1000px"
      :before-close="sgsHandleCloseInstance"
      :close-on-click-modal="false"
    >
      <el-form :model="sgsForm" :inline="true" label-width="85px" :size="mainStoreData.viewSize.main">
        <el-form-item :label="$t('compute.vm.form.securityGroupName') + ':'">
          <el-input v-model="sgsForm.name" class="!w-50" :placeholder="$t('compute.vm.form.inputSecurityGroupName')" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="onSubmit">{{ $t('common.search') }}</el-button>
          <el-button type="warning" @click="onReset">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
      <el-table
        ref="multipleTableRef"
        :size="mainStoreData.viewSize.main"
        :data="sgsListData"
        style="width: 100%"
        @selection-change="sgsHandleSelectionChange"
      >
        <el-table-column type="selection" width="45" :selectable="vmTableSelected" />
        <el-table-column prop="name" :label="$t('compute.vm.form.name')">
          <template #default="scope">
            <router-link :to="'/secGroups/' + scope.row.sgId + '?type=info'" class="text-blue-400">{{
              scope.row.name
            }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('compute.vm.form.securityGroupRules')">
          <template #default="scope">
            <router-link :to="'/secGroups/' + scope.row.sgId + '?type=ingress'" class="text-blue-400">{{
              scope.row.ruleCount
            }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('compute.vm.form.associatedInstance')">
          <template #default="scope">
            <router-link :to="'/secGroups/' + scope.row.sgId + '?type=instances'" class="text-blue-400">{{
              scope.row.computeInstanceCount
            }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="phaseStatus" :label="$t('compute.vm.form.status')">
          <template #default="scope">
            <el-tag
              :size="mainStoreData.viewSize.tagStatus"
              :type="filtersFun.getSecGroupsStatus(scope.row.phaseStatus, 'tag')"
              >{{ filtersFun.getSecGroupsStatus(scope.row.phaseStatus, 'status') }}</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('compute.vm.form.description')" />
      </el-table>
      <el-pagination
        v-model:page_num="sgsForm.page_num"
        v-model:page-size="sgsForm.page_size"
        class="!py-4 !pr-8 float-right"
        :page-sizes="mainStoreData.page_sizes"
        :current-page="sgsForm.page_num"
        :small="true"
        layout="total, sizes, prev, pager, next, jumper"
        :total="sgsForm.total"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button :size="mainStoreData.viewSize.main" @click="sgsHandleCloseInstance()">{{
            $t('common.cancel')
          }}</el-button>
          <el-button type="primary" :size="mainStoreData.viewSize.main" @click="toAddInstance()">{{
            $t('common.confirm')
          }}</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 关联安全组 end -->
    <el-dialog
      v-model="dialogPci"
      v-loading="pciLoading"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="pciHandleClose"
      :title="$t('compute.vm.form.mount')"
    >
      <div class="block overflow-hidden">
        <el-row :gutter="10">
          <el-col :span="24">
            <el-table
              ref="multipleTableRef"
              v-loading="pciLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="pciDeviceTableData"
              max-height="500vh"
              class="!overflow-y-auto hypervisorNodesDialog"
              stripe
              :scrollbar-always-on="false"
              @current-change="pciHandleCheckChange"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span>
                    <span
                      v-if="scope.row.deviceId != nowCheckpci.deviceId"
                      class="w-3 h-3 block border rounded-sm border-gray-300"
                    ></span>
                    <span
                      v-else
                      class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center"
                    >
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="pciDeviceName" :label="$t('compute.vm.form.name')" width="400">
                <template #default="scope">
                  <span>{{ scope.row.pciDeviceName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="pciDeviceGroupId" :label="$t('compute.vm.form.group')">
                <template #default="scope">
                  <span>{{ scope.row.pciDeviceGroupId }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="pciDeviceType" :label="$t('compute.vm.form.type')">
                <template #default="scope">
                  <span>{{ scope.row.pciDeviceType }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="phaseStatus" :label="$t('compute.vm.form.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getPciStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getPciStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>

              <el-table-column prop="createTime" :label="$t('compute.vm.form.createTime')" />
            </el-table>
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="pciHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toPciAttach()">{{ $t('compute.vm.form.mount') }}</el-button>
      </div>
    </el-dialog>
    <el-dialog
      v-model="dialogVolumes"
      v-loading="volumenLoading"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="volumesHandleClose"
      :title="$t('compute.vm.form.mount')"
    >
      <div class="block overflow-hidden">
        <el-row :gutter="10">
          <el-col :span="24">
            <el-form :model="volumesForm" :inline="true" label-width="100px" :size="mainStoreData.viewSize.main">
              <el-form-item :label="$t('compute.vm.form.storagePool') + ':'">
                <el-select
                  v-model="volumesForm.storage_pool_id"
                  class="!w-50"
                  :placeholder="$t('compute.vm.form.selectStoragePool')"
                  @change="onVolumesSubmit"
                >
                  <el-option :label="$t('compute.vm.form.all')" :value="''" />
                  <el-option
                    v-for="(item, index) in storagePoolsList"
                    :key="index"
                    :label="item.name"
                    :value="item.poolId"
                  />
                </el-select>
              </el-form-item>
            </el-form>

            <el-table
              ref="multipleTableRef"
              v-loading="volumenLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="volumenDeviceTableData"
              max-height="500vh"
              class="!overflow-y-auto hypervisorNodesDialog"
              stripe
              :scrollbar-always-on="false"
              @row-click="volumesHandleCheckChange"
            >
              <el-table-column label="" width="40px">
                <template #default="scope">
                  <span>
                    <span
                      v-if="getCheckStatus(scope.row)"
                      class="w-3 h-3 block border rounded-sm border-gray-300 cursor-pointer"
                    ></span>
                    <span
                      v-else
                      class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center cursor-pointer"
                    >
                      <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
                    </span>
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="name" :label="$t('compute.vm.form.name')" width="400">
                <template #default="scope">
                  <span>{{ scope.row.name }}</span>
                </template>
              </el-table-column>

              <el-table-column prop="type" :label="$t('compute.vm.form.type')">
                <template #default="scope">
                  <span v-if="scope.row.type == 2">{{ $t('compute.vm.form.fileSystem') }}</span>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column prop="size" :label="$t('compute.vm.form.size')">
                <template #default="scope"> {{ scope.row.size }}GB </template>
              </el-table-column>
              <el-table-column prop="phaseStatus" :label="$t('compute.vm.form.status')">
                <template #default="scope">
                  <el-tag
                    :size="mainStoreData.viewSize.tagStatus"
                    :type="filtersFun.getVolumeStatus(scope.row.phaseStatus, 'tag')"
                    >{{ filtersFun.getVolumeStatus(scope.row.phaseStatus, 'status') }}</el-tag
                  >
                </template>
              </el-table-column>

              <el-table-column prop="description" :label="$t('compute.vm.form.description')">
                <template #default="scope">
                  {{ scope.row.description }}
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="$t('compute.vm.form.createTime')" width="160px" />
            </el-table>
            <el-pagination
              v-model:page_num="volumesForm.page_num"
              v-model:page-size="volumesForm.page_size"
              class="!py-4 !pr-8 float-right"
              :page-sizes="mainStoreData.page_sizes"
              :current-page="volumesForm.page_num"
              :small="true"
              layout="total, sizes, prev, pager, next, jumper"
              :total="volumesForm.total"
              @size-change="volumesHandleSizeChange"
              @current-change="volumesHandleCurrentChange"
            />
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="volumesHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toVolumesAttach()">{{ $t('compute.vm.form.mount') }}</el-button>
      </div>
    </el-dialog>

    <addRulePage :item-info="addRuleInfo" @addRuleClose="addRuleClose" />
  </div>
</template>

<script setup lang="ts">
import Sortable from 'sortablejs';
import { ref } from 'vue';
import NBDServer from '@/utils/nbd.js';
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';
import addRulePage from '@/views/network/secGroups/addRule.vue';
import operate from './operate.vue';

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const { proxy }: any = getCurrentInstance();
const secondTime: any = ref([new Date(new Date().setHours(new Date().getHours() - 1)), new Date()]);
const vmPanelsData: any = ref([]); // 虚拟机面板数据
const vmPanelsStatus: any = ref(false); // 虚拟机面板状态
const gpuPanelsData: any = ref([]); // gpu面板数据
const gpuPanelsStatus: any = ref(false); // gpu面板状态

const mainStoreData = mainStore(); // pinia 信息
const router = useRouter();
const form: any = ref({});
const activeName: any = ref('first');

// 监听activeName
watch(activeName, (newValue: any) => {
  if (newValue == 'first') {
    getDetail();
  }
  router.push({
    query: {
      type: newValue,
    },
  });
});

const sgsForm = reactive({
  // 虚拟机搜索 筛选
  name: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const rulesSwitch: any = ref([]);
const sortStatus: any = ref(false); // 是否正在排序
const sgsListData: any = ref([]); // 安全组列表
const initDetailSgsListData: any = ref([]); // 详情安全组列表
const detailSgsListData: any = ref([]); // 详情安全组列表
const addInstanceLoading = ref(false); // 添加关联实例
const addInstanceDialog = ref(false); // 添加实例弹窗
const multipleTableRef: any = ref();
const multipleSelection: any = ref([]); // 关联实例 添加虚拟机 当前选中内容
const multipleSelectionId: any = ref([]); // 关联实例 添加虚拟机 当前选中内容ID
const formVmInstancesIds: any = ref([]); // 当前安全组列表 id集合

const pciDeviceTableData: any = ref([]); // pci设备列表
const dialogPci = ref(false); // pci设备弹窗
const pciLoading = ref(false); // pci设备弹窗加载状态
const nowCheckpci: any = ref(''); // 当前选中的pci设备

const storagePoolsList: any = ref([]);
const volumenDeviceTableData: any = ref([]); // volumen设备列表
const dialogVolumes = ref(false); // volumen设备弹窗
const volumenLoading = ref(false); // volumen设备弹窗加载状态
const nowCheckvolumen: any = ref([]); // 当前选中的volumen设备
const volumesForm = reactive({
  name: '',
  storage_pool_id: '',
  detached: true,
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const vmDetailData: any = ref({}); // 虚拟机详情

// 虚拟机操作 end
const addPci = () => {
  getPciDeviceList(); // 获取pci设备列表

  dialogPci.value = true;
};
const pciHandleCheckChange = (val: any) => {
  nowCheckpci.value = val;
};
const toPciAttach = () => {
  // 挂载
  mainApi
    .pciAttach(nowCheckpci.value.deviceId, { vmInstanceId: form.value.instanceId })
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.startMount'));
      getDetail(); // 获取详情
      pciHandleClose();
    })
    .catch((error: any) => {});
  return true;
};
const toPciDetach = (item: any) => {
  // 卸载
  mainApi
    .pciDetach(item.deviceId, { vmInstanceId: form.value.instanceId })
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.startDetach'));
      getDetail(); // 获取详情
    })
    .catch((error: any) => {});
  return true;
};
const pciHandleClose = () => {
  dialogPci.value = false;

  nowCheckpci.value = '';
};

const goBack = () => {
  router.push('/vm');
};
const getport = (val: any) => {
  return val.split(',');
};
const getICMPname = (val: any) => {
  // 获取ICMP名称
  let nameData = '';
  if (val != '') {
    nameData = proxy.$scriptMain.getICMP().filter((item: any) => {
      return val == item.value;
    })[0].name;
  }
  return nameData;
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
    .vmsInstabcesDetail(id)
    .then((res: any) => {
      vmDetailData.value = res;
      form.value = res;
      if (res.hypervisorNodeId) {
        getNodeDetail(res.hypervisorNodeId);
      }
      initDetailSgsListData.value = JSON.parse(JSON.stringify(res.sgInfos));
      detailSgsListData.value = JSON.parse(JSON.stringify(res.sgInfos));
      formVmInstancesIds.value = res.sgInfos.map((v: any) => {
        rulesSwitch.value.push(0);
        return v.sgId;
      });
      detailSgsListData.value.forEach((item: any, index: any) => {
        if (item.rules && item.rules.length > 0) {
        } else {
          item.rules = [];
        }
      });
    })
    .catch((error: any) => {});
};
const nodeDetail: any = ref('');
const getNodeDetail = (id: any) => {
  // 获取详情

  mainApi
    .vmsHypervisorNodesDetail(id)
    .then((res: any) => {
      nodeDetail.value = res;
    })
    .catch((error: any) => {});
};
const vmTableSelected = (row: any, index: any) => {
  // 筛选出已关联的实例
  return !formVmInstancesIds.value.includes(row.sgId);
};
const sgsHandleSelectionChange = (val: any) => {
  multipleSelection.value = val;
  multipleSelectionId.value = val.map((item: any) => {
    return item.sgId;
  });
};
const addInstance = () => {
  // 点击添加实例
  addInstanceDialog.value = true; // 打开添加实例弹窗
};
const sgsHandleCloseInstance = () => {
  multipleTableRef.value!.clearSelection();
  multipleSelectionId.value = [];

  addInstanceDialog.value = false; // 关闭关联实例弹窗
};
const toUnbound = (item: any) => {
  // 取消关联
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .sgsUnboundAdd({ vmInstances: [id] }, item)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.unboundSuccess'));
      getDetail(); // 请求详情
    })
    .catch((error: any) => {});

  return true;
};
const toSortSg = (item: any) => {
  // 关联安全组排序
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .vmsInstabcesBoundSgSort({ sgIds: item }, id)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.sortSuccess'));
      sortStatus.value = false;

      getDetail(); // 请求详情
    })
    .catch((error: any) => {});
};
const toAddInstance = () => {
  // 关联安全组

  if (multipleSelectionId.value.length == 0) {
    ElMessage.warning(proxy.$t('compute.vm.message.selectSecurityGroup'));
    return;
  }
  addInstanceLoading.value = true;
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .vmsInstabcesBoundSg({ sgIds: multipleSelectionId.value }, id)
    .then((res: any) => {
      addInstanceLoading.value = false;
      ElMessage.success(proxy.$t('compute.vm.message.addInstanceSuccess'));
      sgsHandleCloseInstance(); // 初始化输入框 关闭弹窗
      getDetail(); // 请求详情
    })
    .catch((error: any) => {
      addInstanceLoading.value = false;
    });
};
const getSg = (item: any) => {
  const data = sgsListData.value.filter((v: any) => {
    return v.sgId == item.sgId;
  })[0];
  return data && data.name ? data.name : '';
};
const onSubmit = () => {
  // 提交查询
  sgsForm.page_num = 1;
  getSgsList();
};
const onReset = () => {
  // 重置查询
  sgsForm.name = '';
  sgsForm.page_num = 1;
  getSgsList();
};
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  sgsForm.page_size = val;

  getSgsList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  sgsForm.page_num = val;
  getSgsList();
};
const addRuleInfo: any = ref({
  isAdd: true,
  dialogVisible: false,
  sgId: '',
  ruleType: '',
});
const addRule = (sgId: any, type: any) => {
  // 添加规则
  addRuleInfo.value = {
    isAdd: true,
    dialogVisible: true,
    sgId,
    ruleType: type,
  };
};
const editRule = (sgId: any, type: any, item: any) => {
  // 编辑规则
  addRuleInfo.value = {
    isAdd: false,
    dialogVisible: true,
    sgId,
    ruleType: type,
    item,
  };
};
const toDeleteRule = (item: any) => {
  // 删除
  mainApi
    .sgsRulesDel(item.ruleId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.deleteSuccess'));
      restartDetail();
    })
    .catch((error: any) => {});
  return true;
};
const addRuleClose = () => {
  // 添加规则
  addRuleInfo.value = {
    isAdd: true,
    dialogVisible: false,
    sgId: '',
    ruleType: '',
  };
  restartDetail();
};
const getSgsList = () => {
  // sgs列表
  const params: any = {
    name: sgsForm.name,
    page_num: sgsForm.page_num,
    page_size: sgsForm.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi
    .sgsList(params)
    .then((res: any) => {
      sgsListData.value = res.securityGroups;
      sgsForm.total = res.totalNum;
    })
    .catch((error: any) => {});
};
const cancelSort = () => {
  // 取消排序
  sortStatus.value = false;
  detailSgsListData.value = JSON.parse(JSON.stringify(initDetailSgsListData.value));
};
const saveSort = () => {
  // 保存排序
  const ids = detailSgsListData.value.map((v: any) => {
    return v.sgId;
  });
  toSortSg(ids);
};
const openSort = () => {
  // 点击排序按钮
  sortStatus.value = true;
  rowDrop();
};
// 行拖拽
const rowDrop = () => {
  const tbody = document.querySelector('.moveTable .el-table__body-wrapper tbody');

  Sortable.create(tbody, {
    // or { name: "...", pull: [true, false, 'clone', array], put: [true, false, array] }
    handle: '.move',
    animation: 150, // ms, number 单位：ms，定义排序动画的时间
    onEnd({ newIndex, oldIndex }: any) {
      // 结束拖拽
      const currRow = detailSgsListData.value.splice(oldIndex, 1)[0];
      detailSgsListData.value.splice(newIndex, 0, currRow);
    },
  });
};
const toRestore = (row: any) => {
  // 回滚
  mainApi.snapsSwitch(row.snapId).then((res: any) => {
    ElMessage.success(proxy.$t('compute.vm.message.startRestore'));
    getDetail();
  });
  return true;
};
const toDeleteSnaps = (item: any) => {
  // 删除
  mainApi
    .snapsDel(item.snapId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.startDelete'));
      getDetail();
    })
    .catch((error: any) => {});
  return true;
};
const getVmPanelsUrl = () => {
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const urlOrigin = window.location.origin;
  // var urlOrigin = 'http://192.168.1.112:3000';
  const url = `${urlOrigin}/api/monitor/vm/d/${vmPanelsData.value.dashboardId}/${
    vmPanelsData.value.dashboardName
  }?orgId=1&var-vm_instance_id=${id}&from=${secondTime.value[0].getTime()}&to=${secondTime.value[1].getTime()}&kiosk`;
  return url;
};
const getVmGpuPanelsUrl = () => {
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const urlOrigin = window.location.origin;
  // var urlOrigin = 'http://192.168.12.99:3000';
  const url = `${urlOrigin}/api/monitor/vm/d/${gpuPanelsData.value.dashboardId}/${
    gpuPanelsData.value.dashboardName
  }?orgId=1&var-vm_instance_id=${id}&from=${secondTime.value[0].getTime()}&to=${secondTime.value[1].getTime()}&kiosk`;
  return url;
};
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
      'body{background-color: #fff;}.css-1nm48fy{padding:20px;background-color: #fff;}.panel-header{display:block}.panel-header:hover{background-color: transparent;}.panel-header .panel-title div{display:none}.panel-header .panel-title h2{cursor: move;}.panel-container{border-bottom: 2px solid #e4e7ed;padding-bottom: 20px;}.panel-dropdown{display:none!important}.submenu-controls{display:none!important}.page-toolbar{display:none!important}.css-keyl2d{display:none!important}';
    iframeHead.appendChild(iframeStyle);
  }
};
const getPanels = async () => {
  const vmres = await mainApi.vmPanels();
  vmPanelsData.value = vmres;
  const gpures = await mainApi.gpuPanels();
  gpuPanelsData.value = gpures;

  gpuPanelsStatus.value = true;
  vmPanelsStatus.value = true;

  setIframeStyle();
};

const getPciDeviceList = () => {
  // GPU列表
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const data = {
    node_id: form.value.hypervisorNodeId,
    vm_id: id,
  };
  mainApi
    .pciAvailableList(data)
    .then((res: any) => {
      pciDeviceTableData.value = res;
    })
    .catch((error: any) => {});
};
const toVolumesDetach = (item: any) => {
  // 云盘卸载
  mainApi
    .volumesDetach(item.volumeId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.startDetach'));
      getDetail();
    })
    .catch((error: any) => {});
  return true;
};
const addVolumes = () => {
  // 添加云盘
  getVolumesList(); // 获取云盘列表

  dialogVolumes.value = true;
};
const volumesHandleClose = () => {
  dialogVolumes.value = false;

  nowCheckvolumen.value = [];
};
const toVolumesAttach = () => {
  // 挂载 nowCheckvolumen
  const ids = nowCheckvolumen.value.map((item: any) => {
    return item.volumeId;
  });
  if (ids.length == 0) {
    ElMessage.warning(proxy.$t('compute.vm.message.selectVolume'));
    return false;
  }
  mainApi
    .vmsToVolumesList({ volumeIds: ids }, form.value.instanceId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.startMount'));
      getDetail(); // 获取详情
      volumesHandleClose();
    })
    .catch((error: any) => {});
  return true;
};
const getVolumesList = () => {
  // 云盘列表
  volumenLoading.value = true;
  const params: any = {
    name: volumesForm.name,
    storage_pool_id: volumesForm.storage_pool_id,
    detached: volumesForm.detached,
    page_num: volumesForm.page_num,
    page_size: volumesForm.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi
    .volumesList(params)
    .then((res: any) => {
      volumenLoading.value = false;
      volumenDeviceTableData.value = res.volumes;
      volumesForm.total = res.totalNum;
    })
    .catch((error: any) => {
      volumenLoading.value = false;
    });
};
const volumesHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  volumesForm.page_size = val;

  getVolumesList();
};
const volumesHandleCurrentChange = (val: any) => {
  // 改变页码
  volumesForm.page_num = val;
  getVolumesList();
};
const onVolumesSubmit = () => {
  // 提交查询
  volumesForm.page_num = 1;
  getVolumesList();
};
const getCheckStatus = (val: any) => {
  const ids = nowCheckvolumen.value.map((item: any) => {
    return item.volumeId;
  });

  if (ids.includes(val.volumeId)) {
    return false;
  }
  return true;
};
const volumesHandleCheckChange = (val: any) => {
  const ids = nowCheckvolumen.value.map((item: any) => {
    return item.volumeId;
  });

  if (ids.includes(val.volumeId)) {
    nowCheckvolumen.value = nowCheckvolumen.value.filter((item: any) => {
      return item.volumeId != val.volumeId;
    });
  } else {
    const num = form.value.diskInfos ? form.value.diskInfos.length : 0;
    if (nowCheckvolumen.value.length + num >= 4) {
      ElMessage.warning(proxy.$t('compute.vm.message.maxVolume'));
      return false;
    }
    nowCheckvolumen.value.push(val);
  }
};
const getStoragePools = () => {
  // 存储池列表
  mainApi.storagePoolsList().then((res: any) => {
    storagePoolsList.value = res.storagePools;
  });
};
const logList: any = ref([]);
const eventList: any = ref([]);
const logForm = reactive({
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const eventForm = reactive({
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const eventHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  eventForm.page_size = val;
  getEventsList();
};
const eventHandleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  eventForm.page_num = val;
  getEventsList();
};
const logHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  logForm.page_size = val;
  getLogsList();
};
const logHandleCurrentChange = (val: any) => {
  // 改变页码
  console.log(`current page: ${val}`);
  logForm.page_num = val;
  getLogsList();
};
const getLogsList = () => {
  // 日志列表
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const params: any = {
    description: id,
    page_num: logForm.page_num,
    page_size: logForm.page_size,
  };
  mainApi
    .operationLogsList(params)
    .then((res: any) => {
      logList.value = res.alarmRules;
      logForm.total = res.totalNum;
    })
    .catch((error: any) => {});
};
const getEventsList = () => {
  // 事件列表
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const params: any = {
    detail_info: id,
    page_num: eventForm.page_num,
    page_size: eventForm.page_size,
  };
  mainApi
    .operationEventsList(params)
    .then((res: any) => {
      eventList.value = res.events;
      eventForm.total = res.totalNum;
    })
    .catch((error: any) => {});
};
const restartDetail = () => {
  getDetail(); // 获取详情
  getStoragePools(); // 存储池列表
  getSgsList(); // sgs列表
  getLogsList(); // 日志列表
  getEventsList(); // 事件列表
  getPanels(); // 获取云主机面板
};

const changeBootDev = (val: any) => {
  // 改变启动盘
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  const data = {
    bootDev: val,
  };
  mainApi
    .vmsInstabcesEdit(data, id)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.startChangeBootDev'));
      getDetail(); // 获取详情
    })
    .catch((error: any) => {});
};

const openNewPage = () => {
  window.open(
    `/#/vmCommand?instanceId=${vmDetailData.value.instanceId}&instanceName=${vmDetailData.value.name}&type=` + `iso`,
    'newwindow',
    'height=800,width=1220,top=190,left=350,toolbar=no,menubar=no,scrollbars=no,resizable=yes, location=no,status=no',
  );
};
onMounted(() => {
  if (drawerData && drawerData.isDrawer) {
    activeName.value = drawerData.pageType ? drawerData.pageType : 'first';
  } else {
    activeName.value = router.currentRoute.value.query.type ? router.currentRoute.value.query.type : 'first';
  }
  getDetail(); // 获取详情
  getStoragePools(); // 存储池列表
  getSgsList(); // sgs列表
  getLogsList(); // 日志列表
  getEventsList(); // 事件列表
  getPanels(); // 获取云主机面板
});
</script>

<style lang="scss" scoped>
.vmsInstabcesDetailPage {
  display: block;

  .vmDetailTabs {
    border-top-left-radius: 0.4rem;
    border-top-right-radius: 0.4rem;

    ::v-deep .el-tabs__header {
      padding: 0 0 0 1rem;
      margin-bottom: 0;
    }
  }
}
</style>
