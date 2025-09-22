<template>
  <div class="detailPage h-full">
    <h5 class="bg-white mb-3 px-5 pt-2 pb-2 rounded-md">
      <!-- <el-page-header title="返回列表"
                      @back="goBack"
                      class="float-left">
        <template #content>
          {{ $route.meta.title }} <small>(所属安全组:{{ form.name }})</small>
        </template>
      </el-page-header> -->
      <el-radio-group
        v-model="tabContent"
        :size="mainStoreData.viewSize.tabChange"
        class="overflow-hidden align-middle ml-8"
      >
        <el-radio-button label="info">{{ $t('network.secGroups.detailInfo') }}</el-radio-button>
        <el-radio-button label="ingress">{{ $t('network.secGroups.ingress') }}</el-radio-button>
        <el-radio-button label="egress">{{ $t('network.secGroups.egress') }}</el-radio-button>
        <el-radio-button label="instances">{{ $t('network.secGroups.instances') }}</el-radio-button>
      </el-radio-group>
    </h5>
    <!-- 基本信息 start -->
    <el-card v-show="tabContent == 'info'" class="!border-none mb-3">
      <template #header>
        <div class="">
          <span>{{ $t('network.secGroups.basicInfo') }}</span>
        </div>
      </template>
      <el-form
        v-loading="loadingDetail"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :model="form"
        label-width="120px"
      >
        <div class="text item">
          <el-form-item :label="$t('network.secGroups.form.name') + ':'">
            <span>{{ form.name || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.secGroups.form.id') + ':'">
            <span>{{ form.sgId || '-' }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.secGroups.form.rules') + ':'">
            <span class="text-blue-400 cursor-pointer" @click="tabContent = 'ingress'">{{
              form.rules ? form.rules.length : 0
            }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.secGroups.form.associatedInstances') + ':'">
            <span class="text-blue-400 cursor-pointer" @click="tabContent = 'instances'">{{
              form.vmInstances ? form.vmInstances.length : 0
            }}</span>
          </el-form-item>
          <el-form-item :label="$t('network.secGroups.form.description') + ':'">
            <span>{{ form.description || '-' }}</span>
          </el-form-item>
        </div>
      </el-form>
    </el-card>
    <!-- 基本信息 end -->
    <!-- 入方向规则 start -->
    <el-card v-show="tabContent == 'ingress'" class="!border-none mb-3">
      <template #header>
        <div class="">
          <span>{{ $t('network.secGroups.ingress') }}</span>

          <el-button
            class="ml-4"
            type="primary"
            :size="mainStoreData.viewSize.tabChange"
            @click="addRule(form.sgId, 0)"
            >{{ $t('network.secGroups.form.addRuleButton') }}</el-button
          >
        </div>
      </template>

      <el-table
        v-loading="loadingDetail"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :data="inList"
        max-height="calc(100vh - 250px)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
      >
        <el-table-column prop="priority" width="160px" :label="$t('network.secGroups.form.priority')">
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
            <span class="block">{{ scope.row.priority }}</span>
            <div class="leading-tight">
              <small>(id:{{ scope.row.ruleId }})</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('network.secGroups.form.action')">
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
            {{
              scope.row.action === 0
                ? $t('network.secGroups.form.deny')
                : scope.row.action === 1
                ? $t('network.secGroups.form.allow')
                : '-'
            }}
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('network.secGroups.form.protocol')" width="340">
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
          <template #default="scope"
            >{{
              scope.row.protocol === 0
                ? $t('network.secGroups.form.tcp')
                : scope.row.protocol === 1
                ? $t('network.secGroups.form.udp')
                : scope.row.protocol === 3
                ? $t('network.secGroups.form.all')
                : scope.row.protocol === 4
                ? $t('network.secGroups.form.icmp')
                : ''
            }}
            <span v-if="scope.row.protocol === 0">
              <el-tag v-for="(item, index) in getport(scope.row.port)" :key="index" size="small" class="mx-0.3">{{
                item
              }}</el-tag>
            </span>
            <span v-if="scope.row.protocol === 1">
              <el-tag v-for="(item, index) in getport(scope.row.port)" :key="index" size="small" class="mx-0.3">{{
                item
              }}</el-tag>
            </span>
            <span v-if="scope.row.protocol === 3"></span>
            <span v-if="scope.row.protocol === 4">
              <el-tag v-for="(item, index) in getport(scope.row.port)" :key="index" size="small" class="mx-0.3">{{
                getICMPname(item == 'all' ? '0' : item)
              }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('network.secGroups.form.type')">
          <template #default="scope">
            {{
              scope.row.addressType === 0
                ? $t('network.secGroups.form.ipv4')
                : scope.row.addressType === 1
                ? $t('network.secGroups.form.ipv6')
                : '-'
            }}</template
          >
        </el-table-column>

        <el-table-column prop="name" :label="$t('network.secGroups.form.sourceAddress')">
          <template #header>
            {{ $t('network.secGroups.form.sourceAddress') }}

            <el-tooltip placement="top" effect="dark">
              <template #content>
                <div class="w-220px">
                  <p>{{ $t('network.secGroups.form.sourceAddressTooltip') }}</p>
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
            <span v-if="scope.row.addressRef && scope.row.addressRef.cidr">{{ scope.row.addressRef.cidr }}</span>
            <span v-if="scope.row.addressRef && scope.row.addressRef.sgId">{{ getSg(scope.row.addressRef) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('network.secGroups.form.description')"> </el-table-column>
        <el-table-column prop="createTime" :label="$t('network.secGroups.form.createTime')"> </el-table-column>
        <el-table-column prop="updateTime" :label="$t('network.secGroups.form.updateTime')"> </el-table-column>

        <el-table-column prop="address" :label="$t('common.operation')" width="120">
          <template #default="scope">
            <el-dropdown trigger="click" :size="mainStoreData.viewSize.listSet">
              <el-button type="text" :size="mainStoreData.viewSize.listSet">
                {{ $t('common.operation') }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>
                    <span class="w-full" @click="toEdit(form.sgId, 0, scope.row)"
                      ><img src="@/assets/img/btn/edit.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                        $t('common.edit')
                      }}
                    </span>
                  </el-dropdown-item>
                  <el-popconfirm
                    :confirm-button-text="$t('common.delete')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('network.secGroups.message.confirmDelete')"
                    @confirm="toDelete(scope.row)"
                  >
                    <template #reference>
                      <span class="listDelBtn"
                        ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                          $t('common.delete')
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
    </el-card>
    <!-- 入方向规则 end -->
    <!-- 出方向规则 start -->
    <el-card v-show="tabContent == 'egress'" class="!border-none mb-3">
      <template #header>
        <div class="">
          <span>{{ $t('network.secGroups.outDirectionRule') }}</span>

          <el-button
            class="ml-4"
            type="primary"
            :size="mainStoreData.viewSize.tabChange"
            @click="addRule(form.sgId, 1)"
          >
            {{ $t('network.secGroups.form.addRuleButton') }}
          </el-button>
        </div>
      </template>

      <el-table
        v-loading="loadingDetail"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :data="outList"
        max-height="calc(100vh - 250px)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
      >
        <el-table-column prop="priority" width="160px" :label="$t('network.secGroups.form.priority')">
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
            <span class="block">{{ scope.row.priority }}</span>
            <div class="leading-tight">
              <small>(id:{{ scope.row.ruleId }})</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('network.secGroups.form.action')">
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
            {{
              scope.row.action === 0
                ? $t('network.secGroups.form.deny')
                : scope.row.action === 1
                ? $t('network.secGroups.form.allow')
                : '-'
            }}
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('network.secGroups.form.protocol')" width="340">
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
          <template #default="scope"
            >{{
              scope.row.protocol === 0
                ? $t('network.secGroups.form.tcp')
                : scope.row.protocol === 1
                ? $t('network.secGroups.form.udp')
                : scope.row.protocol === 3
                ? $t('network.secGroups.form.all')
                : scope.row.protocol === 4
                ? $t('network.secGroups.form.icmp')
                : ''
            }}
            <span v-if="scope.row.protocol === 0">
              <el-tag v-for="(item, index) in getport(scope.row.port)" :key="index" size="small" class="mx-0.3">{{
                item
              }}</el-tag>
            </span>
            <span v-if="scope.row.protocol === 1">
              <el-tag v-for="(item, index) in getport(scope.row.port)" :key="index" size="small" class="mx-0.3">{{
                item
              }}</el-tag>
            </span>
            <span v-if="scope.row.protocol === 3"></span>
            <span v-if="scope.row.protocol === 4">
              <el-tag v-for="(item, index) in getport(scope.row.port)" :key="index" size="small" class="mx-0.3">{{
                getICMPname(item == 'all' ? '0' : item)
              }}</el-tag>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="name" :label="$t('network.secGroups.form.type')">
          <template #default="scope">
            {{
              scope.row.addressType === 0
                ? $t('network.secGroups.form.ipv4')
                : scope.row.addressType === 1
                ? $t('network.secGroups.form.ipv6')
                : '-'
            }}</template
          >
        </el-table-column>

        <el-table-column prop="name" :label="$t('network.secGroups.form.destinationAddress')">
          <template #header>
            {{ $t('network.secGroups.form.destinationAddress') }}

            <el-tooltip placement="top" effect="dark">
              <template #content>
                <div class="w-220px">
                  <p>{{ $t('network.secGroups.form.destinationAddressTooltip') }}</p>
                  <ul>
                    <li>{{ $t('network.secGroups.form.destinationAddressTips.text1') }}</li>
                    <li>{{ $t('network.secGroups.form.destinationAddressTips.text2') }}</li>
                    <li>{{ $t('network.secGroups.form.destinationAddressTips.text3') }}</li>
                    <li>{{ $t('network.secGroups.form.destinationAddressTips.text4') }}</li>
                  </ul>
                </div>
              </template>
              <span class="text-xs inline-block align-middle cursor-pointer">
                <i-bi:info-square></i-bi:info-square>
              </span>
            </el-tooltip>
          </template>
          <template #default="scope">
            <span v-if="scope.row.addressRef && scope.row.addressRef.cidr">{{ scope.row.addressRef.cidr }}</span>
            <span v-if="scope.row.addressRef && scope.row.addressRef.sgId">{{ getSg(scope.row.addressRef) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" :label="$t('network.secGroups.form.description')"> </el-table-column>
        <el-table-column prop="createTime" :label="$t('network.secGroups.form.createTime')"> </el-table-column>
        <el-table-column prop="updateTime" :label="$t('network.secGroups.form.updateTime')"> </el-table-column>

        <el-table-column prop="address" :label="$t('common.operation')" width="120">
          <template #default="scope">
            <el-dropdown trigger="click" :size="mainStoreData.viewSize.listSet">
              <el-button type="text" :size="mainStoreData.viewSize.listSet">
                {{ $t('common.operation') }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item>
                    <span class="w-full" @click="toEdit(form.sgId, 1, scope.row)"
                      ><img src="@/assets/img/btn/edit.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                        $t('common.edit')
                      }}
                    </span>
                  </el-dropdown-item>
                  <el-popconfirm
                    :confirm-button-text="$t('common.delete')"
                    :cancel-button-text="$t('common.cancel')"
                    icon-color="#626AEF"
                    :title="$t('network.secGroups.message.confirmDelete')"
                    @confirm="toDelete(scope.row)"
                  >
                    <template #reference>
                      <span class="listDelBtn"
                        ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                          $t('common.delete')
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
    </el-card>
    <!-- 出方向规则 end -->
    <!-- 关联实例 start -->
    <el-card v-show="tabContent == 'instances'" class="!border-none mb-3">
      <template #header>
        <div class="">
          <span>{{ $t('network.secGroups.instances') }}</span>
          <el-radio-group
            v-model="associatedInstanceTab"
            :size="mainStoreData.viewSize.tabChange"
            class="overflow-hidden align-middle ml-8"
          >
            <el-radio-button label="vm">{{ $t('network.secGroups.instance') }}</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <el-button class="float-right" type="primary" :size="mainStoreData.viewSize.main" @click="addInstance('vm')">{{
        $t('common.add')
      }}</el-button>

      <el-popconfirm
        :confirm-button-text="$t('common.confirm')"
        :cancel-button-text="$t('common.cancel')"
        icon-color="#626AEF"
        :title="$t('network.secGroups.message.confirmUnbound')"
        :disabled="nowmultipleSelectionId.length === 0"
        @confirm="toUnbound(nowmultipleSelectionId)"
      >
        <template #reference>
          <el-button
            :disabled="nowmultipleSelectionId.length === 0"
            class="float-right mr-4"
            type="primary"
            :size="mainStoreData.viewSize.main"
            >{{ $t('network.secGroups.form.batchUnbound') }}</el-button
          >
        </template>
      </el-popconfirm>
      <el-table
        ref="nowmultipleTableRef"
        v-loading="loadingDetail"
        :size="mainStoreData.viewSize.main"
        :element-loading-text="$t('common.loading')"
        :data="form.vmInstances"
        max-height="calc(100vh - 250px)"
        class="!overflow-y-auto"
        stripe
        :scrollbar-always-on="false"
        @selection-change="nowhandleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="date" :label="$t('network.secGroups.form.vm.name')">
          <template #default="scope">
            <router-link target="_blank" :to="'/vm/' + scope.row.instanceId" class="text-blue-400">{{
              scope.row.name
            }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="phaseStatus" :label="$t('network.secGroups.form.vm.status')">
          <template #default="scope">
            <el-tag
              :size="mainStoreData.viewSize.tagStatus"
              :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
              >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="ip" :label="$t('network.secGroups.form.vm.ip')"> </el-table-column>
        <el-table-column prop="flavorName" :label="$t('network.secGroups.form.vm.flavor')"> </el-table-column>

        <el-table-column prop="address" :label="$t('common.operation')" width="120">
          <template #default="scope">
            <el-popconfirm
              :confirm-button-text="$t('common.confirm')"
              :cancel-button-text="$t('common.cancel')"
              icon-color="#626AEF"
              :title="$t('network.secGroups.message.confirmUnboundInstance')"
              @confirm="toUnbound([scope.row.instanceId])"
            >
              <template #reference>
                <el-button type="text" :size="mainStoreData.viewSize.main">{{
                  $t('network.secGroups.form.unbound')
                }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <!-- 关联实例 end -->

    <!-- 关联实例 start -->
    <el-dialog
      v-model="addInstanceDialog"
      v-loading="addInstanceLoading"
      :element-loading-text="$t('common.loading')"
      :title="addInstanceType == 'vm' ? $t('network.secGroups.form.vm.add') : ''"
      width="1000px"
      :before-close="handleCloseInstance"
      :close-on-click-modal="false"
    >
      <el-table
        ref="multipleTableRef"
        :size="mainStoreData.viewSize.main"
        :data="vmTableData"
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="45" :selectable="vmTableSelected" />
        <el-table-column prop="date" :label="$t('network.secGroups.form.vm.name')">
          <template #default="scope">
            <router-link target="_blank" :to="'/vm/' + scope.row.instanceId" class="text-blue-400">{{
              scope.row.name
            }}</router-link>
          </template>
        </el-table-column>

        <el-table-column prop="hostname" :label="$t('network.secGroups.form.vm.hostname')" />
        <el-table-column prop="portInfo.ipAddress" :label="$t('network.secGroups.form.vm.ip')">
          <template #default="scope">
            <span>{{ scope.row.portInfo.ipAddress || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="subnetInfo.cidr" :label="$t('network.secGroups.form.vm.networkAddress')">
          <template #default="scope">
            <span>{{ scope.row.subnetInfo.cidr || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="eip" :label="$t('network.secGroups.form.vm.eip')">
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
                  <el-tag :size="mainStoreData.viewSize.tagStatus">{{ $t('network.secGroups.form.vm.nat') }}</el-tag>
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
        <el-table-column prop="phaseStatus" :label="$t('network.secGroups.form.vm.status')">
          <template #default="scope">
            <el-tag
              :size="mainStoreData.viewSize.tagStatus"
              :type="filtersFun.getVmStatus(scope.row.phaseStatus, 'tag')"
              >{{ filtersFun.getVmStatus(scope.row.phaseStatus, 'status') }}</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="imageInfo.name" :label="$t('network.secGroups.form.vm.system')" />
        <el-table-column prop="createTime" :label="$t('network.secGroups.form.vm.createTime')" width="160px" />
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
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
      <template #footer>
        <span class="dialog-footer">
          <el-button :size="mainStoreData.viewSize.main" @click="handleCloseInstance()">{{
            $t('common.cancel')
          }}</el-button>
          <el-button :size="mainStoreData.viewSize.main" type="primary" @click="toAddInstance()">{{
            $t('common.confirm')
          }}</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 关联实例 end -->
    <addRulePage :item-info="addRuleInfo" @addRuleClose="addRuleClose" />
  </div>
</template>

<script setup lang="ts">
import netmask from 'netmask';
import mainApi from '@/api/modules/main';
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';
import addRulePage from '@/views/network/secGroups/addRule.vue';

const { Netmask } = netmask;

const { drawerData } = defineProps<{
  drawerData: any;
}>();

const { proxy }: any = getCurrentInstance();

const mainStoreData = mainStore(); // pinia 信息
const loadingDetail = ref(false);
const loading = ref(false);
const addInstanceLoading = ref(false); // 添加关联实例
const router = useRouter();
const tabContent: any = ref('info'); // 标签页切换
const associatedInstanceTab: any = ref('vm'); // 关联实例类型
const addInstanceDialog = ref(false); // 添加实例弹窗
const addInstanceType: any = ref('vm'); // 添加实例 vm/虚拟机
const form: any = ref({});
const formVmInstancesIds: any = ref([]); // 当前实例列表 id集合
const inList: any = ref([]);
const outList: any = ref([]);
const sgsListData: any = ref([]); // 安全组列表

const vmTableData: any = ref([]); // 虚拟机列表

const vmForm = reactive({
  // 虚拟机搜索 筛选
  name: '',
  page_num: 1,
  page_size: mainStoreData.page_size,
  total: 0,
});
const nowmultipleTableRef: any = ref();
const nowmultipleSelection: any = ref([]); // 选中已关联的实例
const nowmultipleSelectionId: any = ref([]);
const nowhandleSelectionChange = (val: any) => {
  nowmultipleSelection.value = val;
  nowmultipleSelectionId.value = val.map((item: any) => {
    return item.instanceId;
  });
};

const multipleTableRef: any = ref();
const multipleSelection: any = ref([]); // 关联实例 添加虚拟机 当前选中内容
const multipleSelectionId: any = ref([]); // 关联实例 添加虚拟机 当前选中内容ID
const handleSelectionChange = (val: any) => {
  multipleSelection.value = val;
  multipleSelectionId.value = val.map((item: any) => {
    return item.instanceId;
  });
};
const rules = {};

const getSg = (item: any) => {
  const data = sgsListData.value.filter((v: any) => {
    return v.sgId == item.sgId;
  })[0];
  return data && data.name ? data.name : '';
};
const vmTableSelected = (row: any, index: any) => {
  // 筛选出已关联的实例
  return !formVmInstancesIds.value.includes(row.instanceId);
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
const addInstance = (type: any) => {
  // 点击添加实例
  addInstanceType.value = type; // 打开添加实例弹窗
  addInstanceDialog.value = true; // 打开添加实例弹窗
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
    .sgsUnboundAdd({ vmInstances: item }, id)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('network.secGroups.message.unboundSuccess'));
      getDetail(); // 请求详情
    })
    .catch((error: any) => {
      loading.value = false;
    });

  return true;
};
const toAddInstance = () => {
  // 关联实例
  loading.value = true;
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .sgsBoundAdd({ vmInstances: multipleSelectionId.value }, id)
    .then((res: any) => {
      loading.value = false;
      ElMessage.success(proxy.$t('network.secGroups.message.startAdd'));
      handleCloseInstance(); // 初始化输入框 关闭弹窗
      getDetail(); // 请求详情
    })
    .catch((error: any) => {
      loading.value = false;
    });
};

const goBack = () => {
  router.push('/secGroups');
};

const handleCloseInstance = () => {
  multipleTableRef.value!.clearSelection();
  multipleSelectionId.value = [];

  addInstanceDialog.value = false; // 关闭关联实例弹窗
};

const getDetail = () => {
  // 获取详情
  loadingDetail.value = true;
  let id: any = '';
  if (drawerData && drawerData.isDrawer) {
    id = drawerData.id;
  } else {
    id = router.currentRoute.value.params.id;
  }
  mainApi
    .sgsDetail(id)
    .then((res: any) => {
      form.value = res;
      loadingDetail.value = false;
      inList.value = res.rules.filter((v: any) => {
        return v.direction === 0;
      });
      outList.value = res.rules.filter((v: any) => {
        return v.direction === 1;
      });
      formVmInstancesIds.value = res.vmInstances.map((v: any) => {
        return v.instanceId;
      });
    })
    .catch((error: any) => {
      loadingDetail.value = false;
    });
};
const getport = (val: any) => {
  return val.split(',');
};
watch(tabContent, (newValue: any) => {
  if (newValue) {
    router.push({
      query: {
        type: newValue,
      },
    });
  }
});
const addRuleInfo: any = ref({
  isAdd: true,
  dialogVisible: false,
  sgId: '',
  ruleType: '',
});
const addRule = (sgId: any, type: any) => {
  // 点击添加规则

  // 添加规则
  addRuleInfo.value = {
    isAdd: true,
    dialogVisible: true,
    sgId,
    ruleType: type,
  };
};
const toEdit = (sgId: any, type: any, item: any) => {
  // 编辑规则

  addRuleInfo.value = {
    isAdd: false,
    dialogVisible: true,
    sgId,
    ruleType: type,
    item,
  };
};
const addRuleClose = (val: any) => {
  // 关闭添加规则弹窗
  getDetail(); // 请求详情
};
const toDelete = (item: any) => {
  // 删除
  mainApi
    .sgsRulesDel(item.ruleId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('common.operations.success.delete'));
      getDetail();
    })
    .catch((error: any) => {});
  return true;
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
const handleSizeChange = (val: any) => {
  // 改变每页显示数量
  localStorage.setItem('page_size', val);
  mainStoreData.page_size = val;
  vmForm.page_size = val;
  getVmsInstabcesList();
};
const handleCurrentChange = (val: any) => {
  // 改变页码
  vmForm.page_num = val;
  getVmsInstabcesList();
};
const getVmsInstabcesList = () => {
  // 虚拟机列表

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
      vmTableData.value = res.vmInstancesInfo;
      vmForm.total = res.totalNum;
    })
    .catch((error: any) => {});
};
onMounted(() => {
  if (drawerData && drawerData.isDrawer) {
    tabContent.value = drawerData.pageType ? drawerData.pageType : 'info';
  } else {
    tabContent.value = router.currentRoute.value.query.type ? router.currentRoute.value.query.type : 'info';
  }
  getSgsList(); // sgs列表

  getVmsInstabcesList(); // 虚拟机列表
  getDetail(); // 获取详情
});
</script>

<style lang="scss" scoped>
.detailPage {
  .rulesTable {
    ::v-deep .el-table__inner-wrapper {
      .el-table__body-wrapper {
        .el-table__row {
          .el-table__cell {
            padding-bottom: 18px !important;
            .cell {
              overflow: inherit;
            }
          }
        }
      }
    }
  }
}
</style>
