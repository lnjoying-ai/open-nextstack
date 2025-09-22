<template>
  <div class="operatePage">
    <div v-if="propShowType == 1">
      <el-dropdown trigger="click" :size="mainStoreData.viewSize.listSet">
        <el-button type="text" :size="mainStoreData.viewSize.listSet">
          {{ $t('common.operation') }}<i-ic:baseline-keyboard-arrow-down></i-ic:baseline-keyboard-arrow-down>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item v-if="propShowBtn.includes('poweron') && [8, 63].includes(propVmDetail.phaseStatus)">
              <span class="w-full" @click="toPoweron(propVmDetail)">
                <img src="@/assets/img/btn/switch.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.poweron')
                }}
              </span>
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('poweroff') && [6, 10, 29].includes(propVmDetail.phaseStatus)">
              <span class="w-full" @click="toPoweroff(propVmDetail)"
                ><img src="@/assets/img/btn/switch.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.poweroff')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('poweroff') && [6, 10, 29].includes(propVmDetail.phaseStatus)">
              <span class="w-full" @click="toPoweroff2(propVmDetail)"
                ><img src="@/assets/img/btn/switch.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.poweroff2')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('reboot') && [6, 10, 29].includes(propVmDetail.phaseStatus)">
              <span class="w-full" @click="toReboot(propVmDetail)"
                ><img src="@/assets/img/btn/switch.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.reboot')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('delete')">
              <span class="w-full" @click="toDelete(propVmDetail)"
                ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.delete')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('edit')">
              <span class="w-full" @click="toEdit(propVmDetail)"
                ><img src="@/assets/img/btn/edit.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.edit')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('resetPassword')">
              <span class="w-full" @click="openResetPassword(propVmDetail)"
                ><img src="@/assets/img/btn/resetPassword.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.resetPassword')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('eip') && !propVmDetail.boundPhaseStatus">
              <span class="w-full" @click="openEip(propVmDetail)"
                ><img src="@/assets/img/btn/attach.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.eip')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('eip') && propVmDetail.boundPhaseStatus == 82">
              <span class="w-full" @click="detachEip(propVmDetail)"
                ><img src="@/assets/img/btn/detach.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.detachEip')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('snaps')">
              <span class="w-full" @click="openSnaps(propVmDetail)"
                ><img src="@/assets/img/btn/snaps.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.snaps')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('flavor')">
              <span class="w-full" @click="openFlavor(propVmDetail)"
                ><img src="@/assets/img/btn/changeFlavor.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.flavor')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('images')">
              <span class="w-full" @click="openImages(propVmDetail)"
                ><img src="@/assets/img/btn/rollBack.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.images')
                }}</span
              >
            </el-dropdown-item>
            <el-dropdown-item v-if="propShowBtn.includes('transfer')">
              <span class="w-full" @click="openTransfer(propVmDetail)"
                ><img src="@/assets/img/btn/qy.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                  $t('compute.vm.operation.transfer')
                }}</span
              >
            </el-dropdown-item>
            <router-link
              v-if="propShowBtn.includes('secGroup')"
              :to="'/vm/' + propVmDetail.instanceId + '?type=' + 'second'"
            >
              <el-dropdown-item>
                <span class="w-full"
                  ><img src="@/assets/img/btn/secGroup.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                    $t('compute.vm.operation.secGroup')
                  }}</span
                ></el-dropdown-item
              >
            </router-link>
            <router-link
              v-if="propShowBtn.includes('vnc')"
              :to="
                '/vmCommand?instanceId=' +
                propVmDetail.instanceId +
                '&instanceName=' +
                propVmDetail.name +
                '&type=' +
                'vnc'
              "
              target="_blank"
            >
              <el-dropdown-item>
                <span class="w-full"
                  ><img src="@/assets/img/btn/code.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
                    $t('compute.vm.operation.vnc')
                  }}</span
                ></el-dropdown-item
              >
            </router-link>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>

    <div v-if="propShowType == 2">
      <el-button
        v-if="propShowBtn.includes('poweron') && [8, 63].includes(propVmDetail.phaseStatus)"
        :size="mainStoreData.viewSize.listSet"
        @click="toPoweron(propVmDetail)"
        ><img src="@/assets/img/btn/switch.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.poweron')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('poweroff') && [6, 10, 29].includes(propVmDetail.phaseStatus)"
        :size="mainStoreData.viewSize.listSet"
        @click="toPoweroff(propVmDetail)"
        ><img src="@/assets/img/btn/switch.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.poweroff')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('poweroff') && [6, 10, 29].includes(propVmDetail.phaseStatus)"
        :size="mainStoreData.viewSize.listSet"
        @click="toPoweroff2(propVmDetail)"
        ><img src="@/assets/img/btn/switch.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.poweroff2')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('reboot') && [6, 10, 29].includes(propVmDetail.phaseStatus)"
        :size="mainStoreData.viewSize.listSet"
        @click="toReboot(propVmDetail)"
        ><img src="@/assets/img/btn/switch.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.reboot')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('delete')"
        :size="mainStoreData.viewSize.listSet"
        @click="toDelete(propVmDetail)"
        ><img src="@/assets/img/btn/delete.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.delete')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('edit')"
        :size="mainStoreData.viewSize.listSet"
        @click="toEdit(propVmDetail)"
        ><img src="@/assets/img/btn/edit.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.edit')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('resetPassword')"
        :size="mainStoreData.viewSize.listSet"
        @click="openResetPassword(propVmDetail)"
        ><img src="@/assets/img/btn/resetPassword.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.resetPassword')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('eip') && !propVmDetail.boundPhaseStatus"
        :size="mainStoreData.viewSize.listSet"
        @click="openEip(propVmDetail)"
        ><img src="@/assets/img/btn/attach.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.eip')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('eip') && propVmDetail.boundPhaseStatus == 82"
        :size="mainStoreData.viewSize.listSet"
        @click="detachEip(propVmDetail)"
        ><img src="@/assets/img/btn/detach.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.detachEip')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('snaps')"
        :size="mainStoreData.viewSize.listSet"
        @click="openSnaps(propVmDetail)"
        ><img src="@/assets/img/btn/snaps.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.snaps')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('flavor')"
        :size="mainStoreData.viewSize.listSet"
        @click="openFlavor(propVmDetail)"
        ><img src="@/assets/img/btn/changeFlavor.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.flavor')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('images')"
        :size="mainStoreData.viewSize.listSet"
        @click="openImages(propVmDetail)"
        ><img src="@/assets/img/btn/rollBack.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.images')
        }}</el-button
      >
      <el-button
        v-if="propShowBtn.includes('transfer')"
        :size="mainStoreData.viewSize.listSet"
        @click="openTransfer(propVmDetail)"
        ><img src="@/assets/img/btn/qy.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
          $t('compute.vm.operation.transfer')
        }}</el-button
      >
      <router-link
        v-if="propShowBtn.includes('secGroup')"
        :to="'/vm/' + propVmDetail.instanceId + '?type=' + 'second'"
        class="ml-2"
      >
        <el-button :size="mainStoreData.viewSize.listSet"
          ><img src="@/assets/img/btn/secGroup.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
            $t('compute.vm.operation.secGroup')
          }}</el-button
        >
      </router-link>
      <router-link
        v-if="propShowBtn.includes('vnc')"
        :to="
          '/vmCommand?instanceId=' + propVmDetail.instanceId + '&instanceName=' + propVmDetail.name + '&type=' + 'vnc'
        "
        target="_blank"
        class="ml-2"
      >
        <el-button :size="mainStoreData.viewSize.listSet"
          ><img src="@/assets/img/btn/code.png" class="w-3 float-left mt-3px mr-1" alt="" />{{
            $t('compute.vm.operation.vnc')
          }}</el-button
        >
      </router-link>
    </div>
    <!-- 重置密码 -->
    <el-dialog
      v-model="dialogResetPassword"
      :close-on-click-modal="false"
      width="600px"
      destroy-on-close
      :before-close="resetPasswordHandleClose"
      :append-to-body="true"
      :title="$t('compute.vm.operate.resetPassword')"
    >
      <el-alert :title="$t('compute.vm.operate.resetPasswordTip')" class="mb-2" type="warning" :closable="false" />

      <el-form
        ref="resetPasswordFormRef"
        v-loading="resetPasswordLoading"
        :rules="resetPasswordRules"
        :model="resetPasswordForm"
        label-width="80px"
        :element-loading-text="$t('common.loading')"
      >
        <el-form-item :label="$t('compute.vm.operate.vm') + ':'">
          <el-input v-model="nowVmData.name" :disabled="true" autocomplete="off" placeholder="-" />
        </el-form-item>

        <el-form-item :label="$t('compute.vm.operate.hostname') + ':'" prop="hostname">
          <el-input
            v-model="resetPasswordForm.hostname"
            autocomplete="off"
            :placeholder="$t('compute.vm.operate.hostnamePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('compute.vm.operate.sysPassword') + ':'" prop="sysPassword">
          <el-input
            v-model="resetPasswordForm.sysPassword"
            type="password"
            show-password
            autocomplete="off"
            :placeholder="$t('compute.vm.operate.sysPasswordTip')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="toResetPassword()">{{ $t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- eip 绑定 -->
    <el-dialog
      v-model="dialogEipVisible"
      :close-on-click-modal="false"
      width="600px"
      destroy-on-close
      :before-close="eipHandleClose"
      :append-to-body="true"
      :title="$t('compute.vm.operate.bindEip')"
    >
      <div class="mb-4">
        <el-table
          ref="singleTableRef"
          max-height="300px"
          :size="mainStoreData.viewSize.main"
          :data="eipTableData"
          highlight-current-row
          style="width: 100%"
          @current-change="eipHandleChange"
        >
          <el-table-column label="" width="40px">
            <template #default="scope">
              <span
                v-if="scope.row.eipId != eipForm.eipId"
                class="w-3 h-3 block border rounded-sm border-gray-300"
              ></span>
              <span v-else class="w-3 h-3 block border rounded-sm border-blue-500 bg-blue-500 text-base text-center">
                <i-dashicons:yes class="text-white w-3.5 h-3.5 -m-0.5 leading-none table"></i-dashicons:yes>
              </span>
            </template>
          </el-table-column>

          <el-table-column prop="date" :label="$t('compute.vm.operate.eip')">
            <template #default="scope">
              {{ scope.row.ipAddress }}
            </template>
          </el-table-column>

          <el-table-column prop="addressType" :label="$t('compute.vm.operate.ipv4Address')">
            <template #default="scope">
              <span>{{ scope.row.addressType === 0 ? 'IPv4' : 'Ipv6' }}</span>
            </template>
          </el-table-column>

          <el-table-column prop="createTime" :label="$t('common.createTime')" />
        </el-table>
        <el-pagination
          v-model:page_num="eipForm.page_num"
          v-model:page-size="eipForm.page_size"
          class="!pt-4 !pr-8 float-right"
          :page-sizes="[10]"
          :current-page="eipForm.page_num"
          :small="true"
          layout="total, prev, pager, next, jumper"
          :total="eipForm.total"
          @size-change="eipHandleSizeChange"
          @current-change="eipHandleCurrentChange"
        />
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="toEip()">{{ $t('compute.vm.operate.bindEipNow') }}</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 快照add -->
    <el-dialog
      v-model="dialogFormVisible"
      :close-on-click-modal="false"
      width="600px"
      destroy-on-close
      :before-close="snapsHandleClose"
      :append-to-body="true"
      :title="$t('compute.vm.operate.createSnapshot')"
    >
      <el-form
        ref="snapsformRef"
        v-loading="snapLoading"
        :rules="rules"
        :model="snapsform"
        label-width="80px"
        :element-loading-text="$t('common.loading')"
      >
        <el-form-item :label="$t('compute.vm.operate.vm') + ':'">
          <el-input v-model="nowVmData.name" :disabled="true" autocomplete="off" placeholder="-" />
        </el-form-item>
        <el-form-item :label="$t('compute.vm.operate.name') + ':'" prop="name">
          <el-input
            v-model="snapsform.name"
            autocomplete="off"
            :placeholder="$t('compute.vm.operate.namePlaceholder')"
          />
        </el-form-item>
        <el-form-item :label="$t('compute.vm.operate.description') + ':'" prop="description">
          <el-input
            v-model="snapsform.description"
            type="textarea"
            :rows="2"
            autocomplete="off"
            :placeholder="$t('compute.vm.operate.descriptionPlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="toSnaps()">{{ $t('compute.vm.operate.createSnapshotNow') }}</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 变更规格 -->
    <el-dialog
      v-model="dialogFlavorVisible"
      :close-on-click-modal="false"
      width="800px"
      destroy-on-close
      :before-close="flavorHandleClose"
      :append-to-body="true"
      :title="$t('compute.vm.operate.flavor')"
    >
      <el-form
        ref="flavorformRef"
        v-loading="flavorLoading"
        :rules="flavorrules"
        :model="flavorform"
        label-width="100px"
        :element-loading-text="$t('common.loading')"
      >
        <el-row>
          <el-col :span="12">
            <el-form-item :label="$t('compute.vm.operate.name') + ':'">
              <span>{{ nowVmData.name || '-' }}</span>
            </el-form-item>

            <el-form-item :label="$t('compute.vm.operate.os') + ':'">
              <span>{{ nowVmData.imageName || '-' }}</span>
            </el-form-item>

            <el-form-item :label="$t('compute.vm.operate.description') + ':'">
              <span>{{ nowVmData.description || '-' }}</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="$t('common.id') + ':'">
              <span>{{ nowVmData.instanceId || '-' }}</span>
            </el-form-item>
            <el-form-item :label="$t('compute.vm.operate.hypervisorNode') + ':'">
              <span>
                <router-link
                  :to="'/hypervisorNodes/' + nowVmData.hypervisorNodeId"
                  target="_blank"
                  class="text-blue-400"
                  >{{ nowVmData.hypervisorNodeName || '-' }}</router-link
                >
              </span>
            </el-form-item>
            <el-form-item :label="$t('compute.vm.operate.status') + ':'">
              <el-tag
                :size="mainStoreData.viewSize.tagStatus"
                :type="filtersFun.getVmStatus(nowVmData.phaseStatus, 'tag')"
                >{{ filtersFun.getVmStatus(nowVmData.phaseStatus, 'status') }}</el-tag
              >
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item :label="$t('compute.vm.info.flavor') + ':'" prop="flavorId" class="relative">
          <div class="overflow-hidden w-full pt-2">
            <el-button
              class="float-right"
              :size="mainStoreData.viewSize.tabChange"
              type="primary"
              @click="clearFilter"
              >{{ $t('compute.vm.operate.resetOrder') }}</el-button
            >
          </div>
          <el-table
            ref="singleTableRef"
            max-height="300px"
            :size="mainStoreData.viewSize.main"
            :data="flavorsList"
            highlight-current-row
            :row-class-name="flavorsTableRowClassName"
            style="width: 100%"
            @current-change="flavorsHandleCurrentChange"
          >
            <el-table-column label="" width="50px">
              <template #default="scope">
                <span v-if="scope.row.disabled">
                  <small>{{ $t('compute.vm.operate.current') }}</small>
                </span>
                <span v-else>
                  <span
                    v-if="scope.row.flavorId != flavorform.flavorId"
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
            <el-table-column prop="date" :label="$t('compute.vm.operate.name')">
              <template #default="scope">
                <router-link :to="'/flavors/' + scope.row.flavorId" target="_blank" class="text-blue-400">{{
                  scope.row.name || '-'
                }}</router-link>
              </template>
            </el-table-column>
            <el-table-column prop="type" :label="$t('compute.vm.operate.type')">
              <template #default="scope">
                <span v-if="scope.row.gpuCount">{{ $t('compute.vm.operate.gpu') }}</span>
                <span v-else>{{ $t('compute.vm.operate.general') }}</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="cpu"
              :label="$t('compute.vm.info.cpu')"
              :filters="flavorCpu"
              :filter-method="filterCpu"
            >
              <template #default="scope"> {{ scope.row.cpu || '-' }} {{ $t('compute.vm.info.core') }} </template>
            </el-table-column>
            <el-table-column
              prop="mem"
              :label="$t('compute.vm.info.memory')"
              :filters="flavorMem"
              :filter-method="filterMem"
            >
              <template #default="scope"> {{ scope.row.mem || '-' }}GB </template>
            </el-table-column>

            <el-table-column
              prop="rootDisk"
              :label="$t('compute.vm.info.rootDisk')"
              :filters="flavorDisk"
              :filter-method="filterDisk"
            >
              <template #default="scope"> {{ scope.row.rootDisk || '-' }}GB </template>
            </el-table-column>
            <el-table-column prop="gpuCount" label="GPU">
              <template #default="scope">
                <span v-if="scope.row.gpuCount && scope.row.gpuCount > 0">
                  {{ scope.row.gpuName }}*{{ scope.row.gpuCount }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>
          <span class="align-text-top mr-4 text-gray-500">
            {{ $t('compute.vm.info.selected') }}：{{ currentFlavorRow.name }}
          </span>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" @click="toFlavor()">{{ $t('common.confirm') }}</el-button>
        </span>
      </template>
    </el-dialog>
    <!-- 导出镜像 -->
    <el-dialog
      v-model="dialogExport"
      :close-on-click-modal="false"
      width="600px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="exportHandleClose"
      :append-to-body="true"
      :title="$t('compute.vm.operate.images')"
    >
      <div class="block overflow-hidden">
        <el-alert
          type="warning"
          class="mb-4"
          :closable="false"
          :description="$t('compute.vm.operate.imagesTip')"
        ></el-alert>
        <el-form
          ref="imagesFormRef"
          :model="imagesForm"
          label-width="180px"
          :rules="imagesFormRules"
          :size="mainStoreData.viewSize.main"
        >
          <el-form-item :label="$t('compute.vm.operate.imageName') + ':'" prop="imageName">
            <el-input
              v-model="imagesForm.imageName"
              class="!w-50"
              :placeholder="$t('compute.vm.operate.imageNamePlaceholder')"
            />
          </el-form-item>
          <!-- <el-form-item label="类型">
            <el-select v-model="imagesForm.imageOsType"
                       class="!w-50"
                       placeholder="请选择类型">
              <el-option label="linux"
                         :value="0" />
              <el-option label="windows"
                         :value="1" />
            </el-select>
          </el-form-item> -->
          <el-form-item :label="$t('compute.vm.operate.isPublic') + ':'">
            <el-radio-group v-model="imagesForm.isPublic">
              <el-radio :label="true">{{ $t('compute.vm.operate.public') }}</el-radio>
              <el-radio :label="false">{{ $t('compute.vm.operate.private') }}</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="exportHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toExport()">{{ $t('compute.vm.operate.export') }}</el-button>
      </div>
    </el-dialog>
    <!-- --迁移-- -->
    <el-dialog
      v-model="dialogTransfer"
      v-loading="transferLoading"
      :close-on-click-modal="false"
      width="1200px"
      destroy-on-close
      :element-loading-text="$t('common.loading')"
      :before-close="transferHandleClose"
      :append-to-body="true"
      :title="$t('compute.vm.operate.transfer')"
    >
      <div class="block overflow-hidden">
        <el-row :gutter="10">
          <el-col :span="18">
            <el-table
              ref="multipleTableRef"
              v-loading="transferLoading"
              :size="mainStoreData.viewSize.main"
              :element-loading-text="$t('common.loading')"
              :data="transferTableData"
              max-height="calc(100% - 3rem)"
              class="!overflow-y-auto hypervisorNodesDialog"
              stripe
              :scrollbar-always-on="false"
              @select="handleSelectionChange"
            >
              <el-table-column type="selection" width="55" :selectable="selectable"> </el-table-column>
              <el-table-column prop="date" :label="$t('compute.vm.operate.name')">
                <template #default="scope">
                  <router-link :to="'/hypervisorNodes/' + scope.row.nodeId">
                    <span class="text-blue-400 cursor-pointer">{{ scope.row.name }}</span>
                  </router-link>
                </template>
              </el-table-column>
              <el-table-column prop="hostname" :label="$t('compute.vm.operate.hostname')" />
              <el-table-column prop="manageIp" :label="$t('compute.vm.operate.manageIp')" />
              <el-table-column :label="$t('compute.vm.operate.cpuAvailableAndTotal')">
                <template #default="scope">
                  <div v-if="scope.row.cpuLogCount">
                    <p>
                      {{ scope.row.cpuLogCount - scope.row.usedCpuSum }}{{ $t('compute.vm.info.core') }}/{{
                        scope.row.cpuLogCount
                      }}{{ $t('compute.vm.info.core') }}
                    </p>
                    <p>{{ scope.row.cpuModel }}</p>
                  </div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column :label="$t('compute.vm.operate.gpuAvailableAndTotal')">
                <template #default="scope">
                  <div v-if="scope.row.gpuTotal">
                    <p>{{ scope.row.availableGpuCount }}/{{ scope.row.gpuTotal }}</p>
                    <p>{{ scope.row.gpuName }}</p>
                  </div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column :label="$t('compute.vm.operate.memoryAvailableAndTotal')">
                <template #default="scope">
                  <div v-if="scope.row.memTotal">
                    {{ scope.row.memTotal - scope.row.usedMemSum }}GB/{{ scope.row.memTotal }}GB
                  </div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column :label="$t('compute.vm.operate.ibNetworkAvailableAndTotal')">
                <template #default="scope">
                  <div v-if="scope.row.ibTotal">{{ scope.row.availableIbCount }}/{{ scope.row.ibTotal }}</div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" :label="$t('common.createTime')" />
            </el-table>
            <el-pagination
              v-model:page_num="transferForm.page_num"
              v-model:page-size="transferForm.page_size"
              class="!pt-4 !pr-8 float-right"
              :page-sizes="[10]"
              :current-page="transferForm.page_num"
              :small="true"
              layout="total, prev, pager, next, jumper"
              :total="transferForm.total"
              @size-change="transferHandleSizeChange"
              @current-change="transferHandleCurrentChange"
            />
          </el-col>
          <el-col :span="6">
            <el-card class="!border-none mb-3">
              <template #header>
                <div class="">
                  <span>{{ $t('compute.vm.operate.vm') }}：{{ vmDetail.name }}</span>
                </div>
              </template>
              <div class="text item">
                <el-form :model="vmDetail" label-width="100px" :size="mainStoreData.viewSize.main">
                  <el-form-item :label="$t('compute.vm.operate.destinationNode') + ':'">
                    <span v-if="multipleSelection.name">
                      <router-link :to="'/hypervisorNodes/' + multipleSelection.nodeId" class="text-blue-400">{{
                        multipleSelection.name
                      }}</router-link>
                    </span>
                    <span v-else> {{ $t('compute.vm.operate.noSelectDestinationNode') }} </span>
                  </el-form-item>
                  <el-form-item :label="$t('compute.vm.operate.currentNode') + ':'">
                    <span>
                      <router-link :to="'/hypervisorNodes/' + vmDetail.hypervisorNodeId" class="text-blue-400">{{
                        vmDetail.hypervisorNodeName || '-'
                      }}</router-link>
                    </span>
                  </el-form-item>

                  <el-form-item :label="$t('compute.vm.operate.os') + ':'">
                    <span>{{ vmDetail.imageName || '-' }}</span>
                  </el-form-item>

                  <el-form-item :label="$t('compute.vm.operate.status') + ':'">
                    <el-tag
                      :size="mainStoreData.viewSize.tagStatus"
                      :type="filtersFun.getVmStatus(vmDetail.phaseStatus, 'tag')"
                      >{{ filtersFun.getVmStatus(vmDetail.phaseStatus, 'status') }}</el-tag
                    >
                  </el-form-item>

                  <el-form-item :label="$t('compute.vm.info.cpu') + ':'">
                    <span> {{ vmDetail.cpu || '-' }} {{ $t('compute.vm.info.core') }} </span>
                  </el-form-item>
                  <el-form-item :label="$t('compute.vm.info.memory') + ':'">
                    <span> {{ vmDetail.mem || '-' }}GB </span>
                  </el-form-item>
                  <el-form-item :label="$t('compute.vm.info.rootDisk') + ':'">
                    <span> {{ vmDetail.rootDisk || '-' }}GB </span>
                  </el-form-item>
                  <el-row v-if="vmDetail.diskInfos && vmDetail.diskInfos.length > 0">
                    <el-col v-for="(item, index) in vmDetail.diskInfos" :key="index" :span="24">
                      <el-form-item :label="$t('compute.vm.info.dataDisk') + (index + 1) + ':'">
                        <span> {{ item.volumeName }} ({{ item.size }}GB) </span>
                      </el-form-item>
                    </el-col>
                  </el-row>
                </el-form>
              </div>
            </el-card>
          </el-col>
        </el-row>
      </div>
      <div class="dialog-footer text-center">
        <el-button type="text" @click="transferHandleClose()">{{ $t('common.cancel') }}</el-button>
        <el-button type="primary" @click="toTransfer()">{{ $t('compute.vm.operate.transfer') }}</el-button>
      </div>
    </el-dialog>
    <el-drawer
      v-model="drawer"
      :title="drawerData.title"
      :direction="drawerData.direction"
      :size="drawerData.size"
      :append-to-body="true"
      :before-close="handleClose"
    >
      <component
        :is="currentView"
        :drawer-data="drawerData"
        class="table w-full"
        @closeDrawer="closeDrawer"
      ></component>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import filtersFun from '@/utils/statusFun';
import mainStore from '@/store/mainStore';
import mainApi from '@/api/modules/main';
import vmedit from './edit.vue';

const mainStoreData = mainStore(); // pinia 信息
const { proxy }: any = getCurrentInstance();

const router: any = useRouter();
// propVmDetail propShowType
const { propVmDetail, propShowType, propShowBtn } = defineProps<{
  propVmDetail: any;
  propShowType: any;
  propShowBtn: any; // ['poweron','poweroff','reboot','delete','edit','eip','snaps','flavor','images','transfer','secGroup','vnc']
}>();

const nowVmData: any = ref(''); // 当前虚拟机信息

const drawer = ref(false);
const drawerData: any = ref('');
const currentView: any = ref(vmedit);

const handleClose = (done: () => void) => {
  if (drawerData.value.close) {
    ElMessageBox.confirm(drawerData.value.closeText)
      .then(() => {
        currentView.value = '';
        done();
      })
      .catch(() => {
        // catch error
      });
  } else {
    currentView.value = '';

    done();
  }
};
const closeDrawer = () => {
  drawer.value = false;
  currentView.value = '';
  drawerData.value = '';
  proxy.$emit('initVmList');
};
// ----- 重置密码-----
const validatePass = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback();
  } else {
    const reg =
      /^(?=.*\d)(?=.*[a-zA-Z])(?=.*[\~\!\@\#\$\%\^\&\*\(\)\_\+\-\=\|\;\:\'\"\,\.\<\>\/\?])[\da-zA-Z\~\!\@\#\$\%\^\&\*\(\)\_\+\-\=\|\;\:\'\"\,\.\<\>\/\?]{8,20}$/;

    if (!reg.test(value)) {
      callback(new Error(proxy.$t('compute.vm.validation.passwordFormat')));
    } else {
      callback();
    }
  }
};
const dialogResetPassword = ref(false);
const resetPasswordLoading = ref(false);
const resetPasswordForm = reactive({
  hostname: '',
  sysPassword: '',
});
const resetPasswordFormRef = ref<any>();
const resetPasswordRules = reactive({
  hostname: [
    { required: true, message: proxy.$t('compute.vm.validation.hostnameRequired'), trigger: 'blur' },
    { min: 3, max: 64, message: proxy.$t('compute.vm.validation.hostnameLength'), trigger: 'blur' },
  ],
  sysPassword: [
    { required: true, validator: validatePass, trigger: 'change' },
    { required: true, validator: validatePass, trigger: 'blur' },
  ],
});

// -----eip-----
const eipTableData: any = ref([]);
const dialogEipVisible = ref(false);
// eip 搜索 筛选
const eipForm = reactive({
  eipId: '',
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});

// -----快照-----
const dialogFormVisible = ref(false);
const snapLoading = ref(false);
const snapsform = reactive({
  name: '',
  description: '',
  vmInstanceId: '',
});
const snapsformRef = ref<any>();
const rules = reactive({
  name: [
    { required: true, message: proxy.$t('compute.vm.validation.nameRequired'), trigger: 'blur' },
    { min: 3, max: 64, message: proxy.$t('compute.vm.validation.nameLength'), trigger: 'blur' },
  ],
});

// -------规格-------
const dialogFlavorVisible = ref(false);
const flavorLoading = ref(false);
const flavorform = reactive({
  flavorId: '',
});
const flavorrules = reactive({
  flavorId: [{ required: true, message: proxy.$t('compute.vm.validation.flavorRequired'), trigger: 'change' }],
});
const flavorformRef = ref<any>();
const currentFlavorRow: any = ref('');
const singleTableRef: any = ref();
const flavorsList: any = ref([]);
const CpuList: any = ref([]);
const MemList: any = ref([]);
const DiskList: any = ref([]);
const flavorCpu = ref<any>();
const flavorMem = ref<any>();
const flavorDisk = ref<any>();

// -----导出镜像-----
const dialogExport = ref(false);
const imagesFormRef = ref<any>();
const imagesForm = reactive({
  imageName: '',
  // imageOsType: 0,
  isPublic: true,
});
const imagesFormRules = reactive({
  imageName: [{ required: true, validator: proxy.$scriptMain.validateName, trigger: 'change' }],
});
// -----迁移-----
const dialogTransfer = ref(false);
const transferloadingDialog = ref(false);
const transferLoading = ref(false);
const transferTableData: any = ref([]);
const transferForm = reactive({
  // 搜索 筛选
  name: '',
  page_num: 1,
  page_size: 10,
  total: 0,
});
const vmDetail: any = ref('');
const multipleSelection: any = ref('');
const multipleTableRef: any = ref();

// 开机
const toPoweron = (item: any) => {
  mainApi
    .vmsInstabcesPoweron(item.instanceId)
    .then((res: any) => {
      ElMessage.success(proxy.$t('compute.vm.message.startPoweron'));
    })
    .catch((error: any) => {});
};
// 关机
const toPoweroff = (item: any) => {
  ElMessageBox.confirm(
    proxy.$t('compute.vm.message.confirmPoweroff', { name: item.name }),
    proxy.$t('compute.vm.operation.poweroff'),
    {
      confirmButtonText: proxy.$t('common.confirm'),
      cancelButtonText: proxy.$t('common.cancel'),
      type: 'warning',
    },
  )
    .then(() => {
      mainApi
        .vmsInstabcesPoweroff(item.instanceId)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startPoweroff'));
        })
        .catch((error: any) => {});
    })
    .catch(() => {});
};
const toPoweroff2 = (item: any) => {
  ElMessageBox.confirm(
    proxy.$t('compute.vm.message.confirmPoweroff', { name: item.name }),
    proxy.$t('compute.vm.operation.poweroff2'),
    {
      confirmButtonText: proxy.$t('common.confirm'),
      cancelButtonText: proxy.$t('common.cancel'),
      type: 'warning',
    },
  )
    .then(() => {
      mainApi
        .vmsInstabcesDetachmentPoweroff(item.instanceId)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startPoweroff2'));
        })
        .catch((error: any) => {});
    })
    .catch(() => {});
};
// 重启
const toReboot = (item: any) => {
  ElMessageBox.confirm(
    proxy.$t('compute.vm.message.confirmReboot', { name: item.name }),
    proxy.$t('compute.vm.operation.reboot'),
    {
      confirmButtonText: proxy.$t('common.confirm'),
      cancelButtonText: proxy.$t('common.cancel'),
      type: 'warning',
    },
  )
    .then(() => {
      mainApi
        .vmsInstabcesReboot(item.instanceId)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startReboot'));
        })
        .catch((error: any) => {});
    })
    .catch(() => {});
};
// 删除
const toDelete = (item: any) => {
  ElMessageBox.confirm(
    proxy.$t('compute.vm.message.confirmDelete', { name: item.name }),
    proxy.$t('compute.vm.operation.delete'),
    {
      confirmButtonText: proxy.$t('common.confirm'),
      cancelButtonText: proxy.$t('common.cancel'),
      type: 'warning',
    },
  )
    .then(() => {
      mainApi
        .vmsInstabcesDel(item.instanceId)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startDelete'));
        })
        .catch((error: any) => {});
    })
    .catch(() => {});
};

// 编辑
const toEdit = (item: any) => {
  drawerData.value = {
    title: proxy.$t('compute.vm.page.openEdit'),
    closeText: proxy.$t('compute.vm.page.closeEdit'),
    direction: 'rtl',
    size: '80%',
    close: true,
    isDrawer: true,
    link: `/vmEdit/${item.instanceId}`,
    linkName: proxy.$t('compute.vm.page.openEditVm'),
    id: item.instanceId,
  };
  currentView.value = vmedit;
  drawer.value = true;
};
// ------重置密码------
// 重置密码弹窗

const openResetPassword = (item: any) => {
  nowVmData.value = item;

  resetPasswordForm.hostname = item.hostname;

  dialogResetPassword.value = true;
};
// 重置密码
const toResetPassword = () => {
  resetPasswordFormRef.value.validate(async (valid: any) => {
    if (valid) {
      resetPasswordLoading.value = true;
      const data = JSON.parse(JSON.stringify(resetPasswordForm));
      if (!data.sysPassword) {
        delete data.sysPassword;
      }

      mainApi
        .vmsResetPassword(data, nowVmData.value.instanceId)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startResetPassword'));
          resetPasswordLoading.value = false;
          dialogResetPassword.value = false;
          resetPasswordFormRef.value.resetFields();
        })
        .catch((error: any) => {
          resetPasswordLoading.value = false;
        });
    } else {
      resetPasswordLoading.value = false;
    }
  });
};
// 重置密码关闭
const resetPasswordHandleClose = (done: () => void) => {
  resetPasswordFormRef.value.resetFields();
  done();
};

// 绑定EIP
const openEip = (item: any) => {
  dialogEipVisible.value = true;
  nowVmData.value = item;
  eipForm.name = '';
  eipForm.eipId = '';
  eipForm.page_num = 1;
  eipForm.page_size = 10;
  getEipList();
};
// 关闭EIP弹窗
const eipHandleClose = (done: () => void) => {
  done();
};
// 解绑EIP
const detachEip = (item: any) => {
  ElMessageBox.confirm(proxy.$t('compute.vm.message.confirmDetachEip', { name: item.name })).then(() => {
    mainApi
      .eipsDetach(item.eipId)
      .then((res: any) => {
        ElMessage.success(proxy.$t('compute.vm.message.startDetachEip'));
      })
      .catch((error: any) => {});
  });
};
// eip列表
const getEipList = () => {
  const params: any = {
    name: eipForm.name,
    page_num: eipForm.page_num,
    page_size: eipForm.page_size,
    bound_type: 'unbound',
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }
  mainApi.eipsList(params).then((res: any) => {
    eipTableData.value = res.eips;
    eipForm.total = res.totalNum;
  });
};
// eip改变每页显示数量
const eipHandleSizeChange = (val: any) => {
  eipForm.page_size = val;
  getEipList();
};
// eip改变页码
const eipHandleCurrentChange = (val: any) => {
  eipForm.page_num = val;
  getEipList();
};
// 选择eip
const eipHandleChange = (val: any | undefined) => {
  eipForm.eipId = val.eipId;
};
// 绑定EIP
const toEip = () => {
  if (!eipForm.eipId) {
    ElMessage.warning(proxy.$t('compute.vm.message.selectEip'));
    return;
  }
  mainApi.eipsAttach(eipForm.eipId, { portId: nowVmData.value.portInfo.portId }).then((res: any) => {
    ElMessage.success(proxy.$t('compute.vm.message.startEip'));
    dialogEipVisible.value = false;
    proxy.$emit('initVmList');
  });
};

// ------快照------
// 快照弹窗

const openSnaps = (item: any) => {
  nowVmData.value = item;

  dialogFormVisible.value = true;
};
// 创建快照
const toSnaps = () => {
  snapsformRef.value.validate(async (valid: any) => {
    if (valid) {
      snapLoading.value = true;

      snapsform.vmInstanceId = nowVmData.value.instanceId;

      mainApi
        .snapsAdd(snapsform)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startCreateSnaps'));
          snapLoading.value = false;
          dialogFormVisible.value = false;
          snapsformRef.value.resetFields();
        })
        .catch((error: any) => {
          snapLoading.value = false;
        });
    } else {
      snapLoading.value = false;
    }
  });
};
// 快照关闭
const snapsHandleClose = (done: () => void) => {
  snapsformRef.value.resetFields();
  done();
};
// -------变更规格-------
// 变更规格弹窗
const openFlavor = (item: any) => {
  if (![8, 63].includes(item.phaseStatus)) {
    ElMessage.warning(proxy.$t('compute.vm.message.vmNotPoweroff'));
    return;
  }
  mainApi.vmsInstabcesDetail(item.instanceId).then((res: any) => {
    getFlavorList(res);
    dialogFlavorVisible.value = true;
    nowVmData.value = res;
  });
};
const flavorHandleClose = (done: () => void) => {
  done();
};
// 规格列表
const getFlavorList = (vmDetail: any) => {
  mainApi
    .flavorsList({ type: 1, page_num: 1, page_size: 99999 })
    .then((res: any) => {
      CpuList.value = [];
      MemList.value = [];
      DiskList.value = [];
      // 查找 id 为item.flavorId 的规格
      const flavor = res.flavors.find((flavor: any) => flavor.flavorId === vmDetail.flavorId);
      let flavorData = [];
      if (flavor.gpuCount && flavor.gpuCount > 0) {
        flavorData = res.flavors.filter((item: any) => item.gpuCount && item.gpuCount > 0);
      } else {
        flavorData = res.flavors.filter((item: any) => !item.gpuCount || item.gpuCount == 0);
      }
      flavorData.forEach((item: any) => {
        if (item.flavorId == vmDetail.flavorId) {
          item.disabled = true;
        }
      });
      // 查找 id 为item.flavorId 的规格并置顶
      const index = flavorData.findIndex((flavor: any) => flavor.flavorId === vmDetail.flavorId);
      if (index > -1) {
        const nowFlavor = flavorData.splice(index, 1);
        flavorData.unshift(nowFlavor[0]);
      }
      flavorsList.value = flavorData.filter((item: any) => item.rootDisk == flavor.rootDisk);

      flavorsList.value.forEach((item: any) => {
        CpuList.value.push(item.cpu);
        MemList.value.push(item.mem);
        DiskList.value.push(item.rootDisk);
      });
      flavorCpu.value = Array.from(new Set(CpuList.value))
        .sort((a: any, b: any) => a - b)
        .map((item: any) => {
          return {
            text: `${item}${proxy.$t('compute.vm.info.core')}`,
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
const flavorsHandleCurrentChange = (val: any | undefined) => {
  if (val.disabled) {
    return;
  }
  currentFlavorRow.value = val;
  flavorform.flavorId = val.flavorId;
};
const flavorsTableRowClassName = (row: any, rowIndex: any, item: any) => {
  if (row.row.disabled) {
    return 'success-row';
  }
  return '';
};
const toFlavor = () => {
  // 变更规格
  // 变更规格add
  flavorformRef.value.validate(async (valid: any) => {
    if (valid) {
      flavorLoading.value = true;

      mainApi
        .vmsInstabcesEdit(flavorform, nowVmData.value.instanceId)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startChangeFlavor'));
          flavorLoading.value = false;
          dialogFlavorVisible.value = false;
          flavorformRef.value.resetFields();
        })
        .catch((error: any) => {
          flavorLoading.value = false;
        });
    } else {
      flavorLoading.value = false;
    }
  });
};
// ------镜像------
const openImages = (item: any) => {
  if (![8, 63].includes(item.phaseStatus)) {
    ElMessage.warning(proxy.$t('compute.vm.message.vmNotPoweroff'));
    return;
  }
  // 镜像弹窗
  nowVmData.value = item;
  dialogExport.value = true;
};
const exportHandleClose = () => {
  nowVmData.value = '';
  imagesForm.imageName = '';
  // imagesForm.imageOsType = 0;
  imagesForm.isPublic = true;
  dialogExport.value = false;
};
const toExport = () => {
  // 导出
  // imagesForm
  imagesFormRef.value.validate(async (valid: any) => {
    if (valid) {
      mainApi
        .volumesExport(imagesForm, nowVmData.value.volumeId)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startExport'));
          exportHandleClose();
        })
        .catch((error: any) => {});
    }
  });
};

// -----迁移-----
const openTransfer = (item: any) => {
  getvmsHypervisorNodesList();
  mainApi
    .vmsInstabcesDetail(item.instanceId)
    .then((res: any) => {
      vmDetail.value = res;
      dialogTransfer.value = true;
      nowVmData.value = item;
    })
    .catch((error: any) => {});
};
const transferHandleClose = () => {
  nowVmData.value = '';
  multipleSelection.value = '';
  dialogTransfer.value = false;
  vmDetail.value = '';
};
const toTransfer = () => {
  // 迁移
  if (!multipleSelection.value) {
    ElMessage.warning(proxy.$t('compute.vm.message.selectTransferNode'));
    return;
  }
  ElMessageBox.confirm(
    `${proxy.$t('compute.vm.message.confirmTransfer', {
      name: vmDetail.value.name,
      nodeName: vmDetail.value.hypervisorNodeName,
      transferNodeName: multipleSelection.value.name,
    })}`,
  )
    .then(() => {
      transferloadingDialog.value = true;
      mainApi
        .vmsMigratet({ destNodeId: multipleSelection.value.nodeId }, nowVmData.value.instanceId)
        .then((res: any) => {
          ElMessage.success(proxy.$t('compute.vm.message.startTransfer'));
          transferloadingDialog.value = false;
          transferHandleClose();
          proxy.$emit('initVmList');
        })
        .catch((error: any) => {
          transferloadingDialog.value = false;
        });
    })
    .catch(() => {
      // catch error
    });
};
const selectable = (row: any) => {
  return row.nodeId != vmDetail.value.hypervisorNodeId;
};

const handleSelectionChange = (val: any, row: any) => {
  multipleTableRef.value!.clearSelection();

  multipleTableRef.value!.toggleRowSelection(row, undefined);
  multipleSelection.value = row;
};

const getvmsHypervisorNodesList = () => {
  // 计算节点列表
  transferLoading.value = true;

  const params: any = {
    name: transferForm.name,
    page_num: transferForm.page_num,
    page_size: transferForm.page_size,
  };
  for (const key in params) {
    if (params[key] === null || params[key] === undefined || params[key] === '') {
      delete params[key];
    }
  }

  mainApi
    .vmsHypervisorNodesList(params)
    .then((res: any) => {
      transferLoading.value = false;
      transferTableData.value = res.nodeAllocationInfos;
      transferForm.total = res.totalNum;
    })
    .catch((error: any) => {
      transferLoading.value = false;
    });
};
const transferHandleSizeChange = (val: any) => {
  // 改变每页显示数量
  transferForm.page_size = val;
  getvmsHypervisorNodesList();
};
const transferHandleCurrentChange = (val: any) => {
  // 改变页码
  transferForm.page_num = val;
  getvmsHypervisorNodesList();
};
onMounted(() => {
  if (
    router.currentRoute.value.params &&
    router.currentRoute.value.params.autoDetachEip == 1 &&
    router.currentRoute.value.params.eipId == propVmDetail.eipId
  ) {
    const data = {
      name: router.currentRoute.value.params.vmName,
      eipId: router.currentRoute.value.params.eipId,
    };
    detachEip(data);
    // 清空参数router.currentRoute.value.params.autoDetachEip
    router.push({ params: { autoDetachEip: null, eipId: null, vmName: null } });
  }
});
</script>

<style lang="scss" scoped>
.operatePage {
  position: relative;
  z-index: 999;
}
</style>
