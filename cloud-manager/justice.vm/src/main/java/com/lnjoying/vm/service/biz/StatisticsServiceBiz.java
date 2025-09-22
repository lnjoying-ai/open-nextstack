// Copyright 2024 The NEXTSTACK Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.lnjoying.vm.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.service.network.NetworkService;
import com.lnjoying.vm.common.TsQueryParameters;
import com.lnjoying.vm.common.VmInstanceStatus;
import com.lnjoying.vm.config.ComputeConfig;
import com.lnjoying.vm.domain.dto.response.InstanceStatsRsp;
import com.lnjoying.vm.domain.dto.response.PrometheusRsp;
import com.lnjoying.vm.domain.dto.response.StorageInfoRsp;
import com.lnjoying.vm.entity.ResourceStats;
import com.lnjoying.vm.entity.VmInstance;
import com.lnjoying.vm.service.ResourceStatsService;
import com.lnjoying.vm.service.VmInstanceService;
import com.micro.core.common.Utils;
import com.micro.core.nework.http.HttpActionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;


@Slf4j
@Service
public class StatisticsServiceBiz
{
    @Autowired
    private ResourceStatsService resourceStatsService;

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    private ComputeConfig computeConfig;

    @Autowired
    private VmInstanceService vmInstanceService;


    public void addNatSummery(String userId)
    {
        NetworkService.NetSummeryInfo netSummeryInfo = combRpcSerice.getNetworkService().getNatSummery(userId,
                combRpcSerice.getUmsService().isAdminUser(userId));
        ResourceStats tblResourceStats = new ResourceStats();
        tblResourceStats.setPhaseStatus(0);
        tblResourceStats.setStatsId(Utils.assignUUId());
        tblResourceStats.setName("nat");
        tblResourceStats.setUserId(userId);
        tblResourceStats.setTotal(netSummeryInfo.getNatCount());
        tblResourceStats.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblResourceStats.setUpdateTime(tblResourceStats.getCreateTime());
        boolean ok = resourceStatsService.save(tblResourceStats);
        if (!ok)
        {
            log.info("add resource stats failed");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }

    public void addVmPhaseStats(String userId)
    {

        InstanceStatsRsp getVmStatsRsp = getInstanceStats(userId);
        ResourceStats tblResourceStats = new ResourceStats();
        tblResourceStats.setUserId(userId);
        tblResourceStats.setPhaseStatus(0);
        tblResourceStats.setStatsId(Utils.assignUUId());
        tblResourceStats.setName("vm");
        tblResourceStats.setRunning(getVmStatsRsp.getInstanceRunning());
        tblResourceStats.setTotal(getVmStatsRsp.getInstanceTotal());
        tblResourceStats.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblResourceStats.setUpdateTime(tblResourceStats.getCreateTime());
        boolean ok = resourceStatsService.save(tblResourceStats);
        if (!ok)
        {
            log.info("add resource stats failed");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }

    public void addVmMemSummery(String userId)
    {
        ResourceStats tblResourceStats = new ResourceStats();
        tblResourceStats.setName("mem");
        tblResourceStats.setPhaseStatus(0);
        tblResourceStats.setUserId(userId);
        tblResourceStats.setStatsId(Utils.assignUUId());

        ResourceStats tmp = getVmMemSummery(userId);
        tblResourceStats.setTotal(tmp.getTotal());
        tblResourceStats.setUnit(tmp.getUnit());
        tblResourceStats.setUsed(tmp.getUsed());

        tblResourceStats.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblResourceStats.setUpdateTime(tblResourceStats.getCreateTime());
        boolean ok = resourceStatsService.save(tblResourceStats);
        if (!ok)
        {
            log.info("add resource stats failed");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }

    public void addVmCpuSummery(String userId)
    {
        ResourceStats tblResourceStats = new ResourceStats();
        tblResourceStats.setName("cpu");
        tblResourceStats.setPhaseStatus(0);
        tblResourceStats.setStatsId(Utils.assignUUId());
        tblResourceStats.setUserId(userId);

        ResourceStats tmp = getVmCpuSummery(userId);
        tblResourceStats.setTotal(tmp.getTotal());
        tblResourceStats.setUsed(tmp.getUsed());
        tblResourceStats.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblResourceStats.setUpdateTime(tblResourceStats.getCreateTime());
        boolean ok = resourceStatsService.save(tblResourceStats);
        if (!ok)
        {
            log.info("add resource stats failed");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }

    public void addVmStorageSummery(String userId)
    {
        ResourceStats tblResourceStats = new ResourceStats();
        tblResourceStats.setName("storage");
        tblResourceStats.setPhaseStatus(0);
        tblResourceStats.setStatsId(Utils.assignUUId());
        tblResourceStats.setUserId(userId);

        long usedSize = getUserStorageSize(userId);
//        tblResourceStats.setTotal(tmp.getTotal());
        if (usedSize > 2147483647)
        {
            tblResourceStats.setUsed((int) (usedSize / 1024));
            tblResourceStats.setUnit("TB");
        }
        tblResourceStats.setUsed((int) usedSize);
        tblResourceStats.setUnit("GB");
        tblResourceStats.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblResourceStats.setUpdateTime(tblResourceStats.getCreateTime());
        boolean ok = resourceStatsService.save(tblResourceStats);
        if (!ok)
        {
            log.info("add resource stats failed");
            throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
        }
    }

    public List<ResourceStats> getResourceStats(String userId, String name, int days)
            throws WebSystemException
    {
        name = name.trim();
        LambdaQueryWrapper<ResourceStats> queryWrapper = new LambdaQueryWrapper<>();

        if (!"cpu".equals(name) && !"mem".equals(name) && !"vm".equals(name)
                && !"nat".equals(name) && !"storage".equals(name))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);

        }
        queryWrapper.ne(ResourceStats::getPhaseStatus, REMOVED)
                .eq(ResourceStats::getUserId, userId)
                .eq(ResourceStats::getName, name)
                .orderByDesc(ResourceStats::getCreateTime);
        Page<ResourceStats> page = new Page<>(0, days);
        Page<ResourceStats> resourceStatsPage = resourceStatsService.page(page, queryWrapper);
        return resourceStatsPage.getRecords();

    }

    public StorageInfoRsp getAllStorageSize()
    {
        String totalSizeUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{storageSize}";
        PrometheusRsp getPrometheusRsp = HttpActionUtil.getObject(totalSizeUrl, PrometheusRsp.class, TsQueryParameters.TOTAL_FILESYSTEM);
        if (0 == getPrometheusRsp.getData().getResult().length)
        {
            StorageInfoRsp getStorageInfoRsp = new StorageInfoRsp();
            getStorageInfoRsp.setTotal(0.0f);
            getStorageInfoRsp.setUsed(0.0f);
            getStorageInfoRsp.setUnused(0.0f);
            getStorageInfoRsp.setUnit("GB");
            return getStorageInfoRsp;
        }
        float totalSize = Float.parseFloat(getPrometheusRsp.getData().getResult()[0].getValue()[1]);
        String unusedSizeUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{unusedSize}";
        getPrometheusRsp = HttpActionUtil.getObject(unusedSizeUrl, PrometheusRsp.class, TsQueryParameters.UNUSED_FILESYSTEM);
        float unusedSize = Float.parseFloat(getPrometheusRsp.getData().getResult()[0].getValue()[1]);
        float usedSize = totalSize - unusedSize;
        StorageInfoRsp getStorageInfoRsp = new StorageInfoRsp();
        String unit = "GB";
        if (totalSize > 1024)
        {
//            totalSize = (float)(Math.round(totalSize /1024 * 100)/100);
            totalSize /= 1024;
            totalSize = Float.parseFloat(String.format("%.2f", totalSize));
            unusedSize /= 1024;
            unusedSize = Float.parseFloat(String.format("%.2f", unusedSize));
            usedSize /= 1024;
            usedSize = Float.parseFloat(String.format("%.2f", usedSize));
//            unusedSize = (float)(Math.round(unusedSize /1024 * 100)/100);
//            usedSize = (float)(Math.round(usedSize /1024 * 100)/100);
            unit = "TB";
        }
        getStorageInfoRsp.setTotal(totalSize);
        getStorageInfoRsp.setUnused(unusedSize);
        getStorageInfoRsp.setUsed(usedSize);
        getStorageInfoRsp.setUnit(unit);
        return getStorageInfoRsp;
    }

    public long getUserStorageSize(String userId)
    {
        if (combRpcSerice.getUmsService().isAdminUser(userId))
        {
            userId = null;
        }
        long rootDiskSize = vmInstanceService.sumRootDiskSizeByUserId(userId);
        long dataDiskSize = vmInstanceService.sumDataDiskSizeByUserId(userId);

        return rootDiskSize + dataDiskSize;
    }

    public long getVmCount(String userId)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(VmInstance::getPhaseStatus, REMOVED);

        if (!combRpcSerice.getUmsService().isAdminUser(userId))
        {
            queryWrapper.eq(VmInstance::getUserId, userId);
        }
        return vmInstanceService.count(queryWrapper);
    }

    public ResourceStats getVmCpuSummery(String userId)
    {
        ResourceStats tblResourceStats = new ResourceStats();
        if (!combRpcSerice.getUmsService().isAdminUser(userId))
        {
            int usedCpus = (int) vmInstanceService.sumCpusByUserId(userId);
            tblResourceStats.setUsed(usedCpus);
        }
        else
        {
            String nodeUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{totalCpu}";
            PrometheusRsp getPrometheusRsp = HttpActionUtil.getObject(nodeUrl, PrometheusRsp.class, TsQueryParameters.TOTAL_CPUS);
            int totalCpus = Integer.parseInt(getPrometheusRsp.getData().getResult()[0].getValue()[1]);

            int usedCpus = (int) vmInstanceService.sumCpusByUserId(userId);

//            String vmUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{vmCpu}";
//            getPrometheusRsp = HttpActionUtil.getObject(vmUrl, PrometheusRsp.class, TsQueryParameters.USED_CPUS);
//            int usedCpus = Integer.parseInt(getPrometheusRsp.getData().getResult()[0].getValue()[1]);
            tblResourceStats.setTotal(totalCpus);
            tblResourceStats.setUsed(usedCpus);
        }
        return tblResourceStats;
    }

    public ResourceStats getVmMemSummery(String userId)
    {
        ResourceStats tblResourceStats = new ResourceStats();
        if (!combRpcSerice.getUmsService().isAdminUser(userId))
        {
            long usedMem = vmInstanceService.sumMemByUserId(userId);
            if (usedMem > 2147483647)
            {
                tblResourceStats.setUsed((int) (usedMem / 1024));
                tblResourceStats.setUnit("TB");
            }
            else
            {
                tblResourceStats.setUsed((int) usedMem);
                tblResourceStats.setUnit("GB");
            }
        }
        else
        {

            String nodeUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{totalMem}";
            log.info("prometheus url:{}", nodeUrl);
            PrometheusRsp getPrometheusRsp = HttpActionUtil.getObject(nodeUrl, PrometheusRsp.class, TsQueryParameters.TOTAL_MEM);
            long totalMem = Long.parseLong(getPrometheusRsp.getData().getResult()[0].getValue()[1]);
            long totalGB = totalMem / 1024 / 1024 / 1024;

            String vmUrl = computeConfig.getPrometheusServer() + TsQueryParameters.API + "{vmMem}";
            getPrometheusRsp = HttpActionUtil.getObject(vmUrl, PrometheusRsp.class, TsQueryParameters.USED_MEM);
//            long usedMem = Long.parseLong(getPrometheusRsp.getData().getResult()[0].getValue()[1]);
            long usedGB = vmInstanceService.sumMemByUserId(userId);
//            long usedGB = usedMem / 1024 ;
            if (totalGB > 2147483647)
            {
                tblResourceStats.setTotal((int) (totalGB / 1024));
                tblResourceStats.setUnit("TB");
                tblResourceStats.setUsed((int) (usedGB / 1024));
            }
            else
            {
                tblResourceStats.setTotal((int) totalGB);
                tblResourceStats.setUnit("GB");
                tblResourceStats.setUsed((int) usedGB);
            }
        }
        return tblResourceStats;
    }

    public InstanceStatsRsp getInstanceStats(String userId)
    {
        if (combRpcSerice.getUmsService().isAdminUser(userId))
        {
            userId = null;
        }
        int vmTotal = countByVmPhase(userId, null);
        int vmRunning = countByVmPhase(userId, VmInstanceStatus.INSTANCE_RUNNING) + countByVmPhase(userId, VmInstanceStatus.INSTANCE_MONITOR_TAG_DONE);
        int vmCreateFailed = countByVmPhase(userId, VmInstanceStatus.INSTANCE_CREATE_FAILED);
        int vmPowerOff = countByVmPhase(userId, VmInstanceStatus.INSTANCE_POWEROFF);
        int vmCreating = vmTotal - vmRunning - vmPowerOff - vmCreateFailed;
        InstanceStatsRsp getVmStatsRsp = new InstanceStatsRsp();
        getVmStatsRsp.setInstancePowerOff(vmPowerOff);
        getVmStatsRsp.setInstanceRunning(vmRunning);
        getVmStatsRsp.setInstanceTotal(vmTotal);
        getVmStatsRsp.setInstanceCreateFailed(vmCreateFailed);
        getVmStatsRsp.setInstanceCreating(vmCreating);
        return getVmStatsRsp;
    }

    public int countByVmPhase(String userId, Integer phase)
    {
        LambdaQueryWrapper<VmInstance> queryWrapper = new LambdaQueryWrapper<>();

        if (null != userId)
        {
            queryWrapper.eq(VmInstance::getUserId, userId);
        }
        if (null != phase)
        {
            queryWrapper.eq(VmInstance::getPhaseStatus, phase);
        }
        else
        {
            queryWrapper.ne(VmInstance::getPhaseStatus, REMOVED);
        }
        return (int) vmInstanceService.count(queryWrapper);
    }
}
