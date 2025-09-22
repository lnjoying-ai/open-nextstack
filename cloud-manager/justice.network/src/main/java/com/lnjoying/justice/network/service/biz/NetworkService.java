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

package com.lnjoying.justice.network.service.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lnjoying.justice.commonweb.biz.LogRpcSerice;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.schema.service.compute.BaremetalService;
import com.lnjoying.justice.schema.service.compute.ComputeService;
import com.lnjoying.justice.schema.service.compute.VmService;
import com.lnjoying.justice.schema.service.network.NetworkService.NetSummeryInfo;
import com.lnjoying.justice.network.common.*;
import com.lnjoying.justice.network.domain.dto.request.*;
import com.lnjoying.justice.network.domain.dto.response.*;
import com.lnjoying.justice.network.entity.*;
import com.lnjoying.justice.network.entity.search.EipPortMapSearchCritical;
import com.lnjoying.justice.network.entity.search.EipSearchCritical;
import com.lnjoying.justice.network.entity.search.SubnetSearchCritical;
import com.lnjoying.justice.network.entity.search.VpcSearchCritical;
import com.lnjoying.justice.network.service.*;
import com.lnjoying.justice.network.utils.NetworkUtils;
import com.micro.core.common.Utils;
import lombok.extern.slf4j.Slf4j;
import org.jruby.ext.socket.SubnetUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

import static com.lnjoying.justice.schema.common.CommonPhaseStatus.REMOVED;


@Service("networkService")
@Slf4j
public class NetworkService
{
//    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkService.class);

    @Autowired
    private CombRpcSerice combRpcSerice;

    @Autowired
    private VpcService vpcService;

    @Autowired
    private SubnetService subnetService;

    @Autowired
    private PortService portService;

    @Autowired
    private EipService eipService;

    @Autowired
    private EipMapService eipMapService;

    @Autowired
    private PortMapService portMapService;

    @Autowired
    private EipPoolVpcRefService eipPoolVpcRefService;

    @Autowired
    private EipPoolService eipPoolService;

    @Autowired
    private SecurityGroupService sgService;

    @Autowired
    private LogRpcSerice logRpcSerice;


    //vpc

    public VpcsRspVo getVpcs(VpcSearchCritical vpcCritical, String userId) throws WebSystemException
    {
        LambdaQueryWrapper<Vpc> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Vpc::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(vpcCritical.getName()))
        {
            queryWrapper.like(Vpc::getName, vpcCritical.getName());
        }

        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(Vpc::getUserId, userId);
        }

        if (null != vpcCritical.getPhaseStatus())
        {
            queryWrapper.eq(Vpc::getPhaseStatus, vpcCritical.getPhaseStatus());
        }

        VpcsRspVo getVpcsRsp = new VpcsRspVo();

        long totalNum = vpcService.count(queryWrapper);
        getVpcsRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getVpcsRsp;
        }
        queryWrapper.orderByDesc(Vpc::getCreateTime);
//        int current = vpcCritical.getPageSize() * (vpcCritical.getPageNum()-1);
        Page<Vpc> page = new Page<>(vpcCritical.getPageNum(), vpcCritical.getPageSize());
        Page<Vpc> vpcPage = vpcService.page(page, queryWrapper);
        if (vpcPage.getTotal() < 1)
        {
            return getVpcsRsp;
        }
        List<Vpc> vpcs = vpcPage.getRecords();
        List<VpcDetailInfoRspVo> vpcDetailInfoRsps = vpcs.stream().map(vpc ->
        {
            VpcDetailInfoRspVo vpcDetailInfoRsp = new VpcDetailInfoRspVo();
            vpcDetailInfoRsp.setVpcDetailInfoRsp(vpc);
            return vpcDetailInfoRsp;
        }).collect(Collectors.toList());
        getVpcsRsp.setVpcs(vpcDetailInfoRsps);
        return getVpcsRsp;
    }

    public VpcDetailInfoRspVo getVpc(@NotBlank String vpcId, String userId)
    {
        //get vpc from db with vpc id
        Vpc tblVpc = vpcService.getById(vpcId);

        if (null == tblVpc)
        {
            throw new WebSystemException(ErrorCode.VPC_NOT_EXIST, ErrorLevel.INFO);
        }
        if (StrUtil.isBlank(userId))
        {
            log.info("user is admin, vpcId: {}", vpcId);
        }
        else if (!Objects.equals(tblVpc.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        VpcDetailInfoRspVo getVpcDetailInfoRsp = new VpcDetailInfoRspVo();

        //set vpc detail info resp
        getVpcDetailInfoRsp.setVpcDetailInfoRsp(tblVpc);

        LambdaQueryWrapper<Subnet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Subnet::getVpcId, vpcId)
                .ne(Subnet::getPhaseStatus, REMOVED);
        Integer count = (int) subnetService.count(queryWrapper);

        getVpcDetailInfoRsp.setCount(count);

        return getVpcDetailInfoRsp;
    }

    public VpcBaseRspVo createVpc(VpcCreateReqVo vpcInfo, @NotBlank String userId)
    {
        String vpcId = Utils.assignUUId();
        int addressType = (vpcInfo.getAddressType() == null) ? (short) 0 : vpcInfo.getAddressType();
        if (!NetworkUtils.isValidCidrIp(vpcInfo.getCidr()))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        SubnetUtils.SubnetInfo vpcCidr = new SubnetUtils(vpcInfo.getCidr()).getInfo();
        Vpc tblVpc = new Vpc();
        tblVpc.setVpcId(vpcId);
        tblVpc.setName(vpcInfo.getName());
        tblVpc.setCidr(NetworkUtils.getCidr(vpcCidr.getNetworkAddress(), vpcCidr.getNetmask()));
        tblVpc.setUserId(userId);
        tblVpc.setAddressType(addressType);
        tblVpc.setPhaseStatus(PhaseStatus.ADDING);
        tblVpc.setCreateTime(new Date(System.currentTimeMillis()));
        tblVpc.setUpdateTime(tblVpc.getCreateTime());
        if (null != vpcInfo.getVlanId())
        {
            tblVpc.setVlanId(vpcInfo.getVlanId());
        }
        boolean ok = vpcService.save(tblVpc);
        if (!ok)
        {
            log.error("insert tbl_rs_vpc error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return VpcBaseRspVo.builder().vpcId(vpcId).build();

    }

    public VpcBaseRspVo updateVpc(@NotBlank String vpcId, CommonReq commonReq,String userId)
    {
        Vpc tblVpc = vpcService.getById(vpcId);
        if (null == tblVpc || REMOVED == tblVpc.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VPC_NOT_EXIST, ErrorLevel.INFO);
        }
        // 如果userId 不是admin，且userId和vpc的userId不一致，抛出异常
        if (StrUtil.isNotBlank(userId) && !Objects.equals(tblVpc.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(commonReq.getName()))
        {
            tblVpc.setName(commonReq.getName());
        }
        if (StrUtil.isNotBlank(commonReq.getDescription()))
        {
            tblVpc.setDescription(commonReq.getDescription());
        }

        tblVpc.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = vpcService.updateById(tblVpc);
        if (!ok)
        {
            log.error("update tbl_rs_vpc error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return VpcBaseRspVo.builder().vpcId(vpcId).build();
    }

    public SubnetBaseRspVo updateSubnet(@NotBlank String subnetId, CommonReq commonReq,String userId)
    {
        Subnet tblSubnet = subnetService.getById(subnetId);
        if (null == tblSubnet || REMOVED == tblSubnet.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.SUBNET_NOT_EXIST, ErrorLevel.INFO);
        }
        // 如果userId 不是admin，且userId和vpc的userId不一致，抛出异常
        if (StrUtil.isNotBlank(userId) && !Objects.equals(tblSubnet.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(commonReq.getName()))
        {
            tblSubnet.setName(commonReq.getName());
        }
        if (StrUtil.isNotBlank(commonReq.getDescription()))
        {
            tblSubnet.setDescription(commonReq.getDescription());
        }

        tblSubnet.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = subnetService.updateById(tblSubnet);
        if (!ok)
        {
            log.error("update tbl_rs_vpc error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        SubnetBaseRspVo baseRspVo = new SubnetBaseRspVo();
        baseRspVo.setSubnetId(tblSubnet.getSubnetId());
        return baseRspVo;
    }

    public VpcBaseRspVo removeVpc(@NotBlank String vpcId, String userId)
    {
        Vpc tblVpc = vpcService.getById(vpcId);
        if (null == tblVpc)
        {
            throw new WebSystemException(ErrorCode.VPC_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!Objects.equals(tblVpc.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<Subnet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Subnet::getVpcId, vpcId)
                .ne(Subnet::getPhaseStatus, REMOVED);
        long count = subnetService.count(queryWrapper);
        if (count > 0)
        {
            throw new WebSystemException(ErrorCode.VPC_HAS_SUBNETS, ErrorLevel.INFO);
        }
        tblVpc.setPhaseStatus(PhaseStatus.DELETING);
        boolean ok = vpcService.updateById(tblVpc);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        return VpcBaseRspVo.builder().vpcId(vpcId).build();
    }

    //subnet
    public Object getSubnets(SubnetSearchCritical subnetCritical, String userId)
    {
        SubnetsRspVo getSubnetsRsp = new SubnetsRspVo();
        LambdaQueryWrapper<Subnet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Subnet::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(subnetCritical.getName()))
        {
            queryWrapper.like(Subnet::getName, subnetCritical.getName());
        }
        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(Subnet::getUserId, userId);
        }
        if (null != subnetCritical.getPhaseStatus())
        {
            queryWrapper.eq(Subnet::getPhaseStatus, subnetCritical.getPhaseStatus());
        }
        if (!StrUtil.isBlank(subnetCritical.getVpcId()))
        {
            queryWrapper.eq(Subnet::getVpcId, subnetCritical.getVpcId());
        }
        long totalNum = subnetService.count(queryWrapper);
        getSubnetsRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getSubnetsRsp;
        }
        queryWrapper.orderByDesc(Subnet::getCreateTime);
//        int current = subnetCritical.getPageSize() * (subnetCritical.getPageNum()-1);
        Page<Subnet> page = new Page<>(subnetCritical.getPageNum(), subnetCritical.getPageSize());
        Page<Subnet> subnetPage = subnetService.page(page, queryWrapper);
        if (subnetPage.getTotal() < 1)
        {
            return getSubnetsRsp;
        }
        List<Subnet> subnets = subnetPage.getRecords();
        List<SubnetDetailInfoRspVo> subnetDetailInfoRsps = subnets.stream().map(tblSubnet ->
        {
            SubnetDetailInfoRspVo subnetDetailInfoRsp = new SubnetDetailInfoRspVo();
            subnetDetailInfoRsp.setSubnetDetailInfoRsp(tblSubnet);
            subnetDetailInfoRsp.setVpcName(vpcService.getById(tblSubnet.getVpcId()).getName());
            return subnetDetailInfoRsp;
        }).collect(Collectors.toList());
        getSubnetsRsp.setSubnets(subnetDetailInfoRsps);
        return getSubnetsRsp;
    }

    public SubnetDetailInfoRspVo getSubnet(@NotBlank String subnetId, String userId)
    {
        //get vpc from db with vpc id
        Subnet tblSubnet = subnetService.getById(subnetId);

        if (null == tblSubnet)
        {
            throw new WebSystemException(ErrorCode.SUBNET_NOT_EXIST, ErrorLevel.INFO);
        }
        if (StrUtil.isBlank(userId))
        {
            log.info("user is admin, subnetId: {}", subnetId);
        }
        else if (!Objects.equals(tblSubnet.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        SubnetDetailInfoRspVo getSubnetDetailInfoRsp = new SubnetDetailInfoRspVo();

        //set vpc detail info resp
        getSubnetDetailInfoRsp.setSubnetDetailInfoRsp(tblSubnet);

        log.info("subnetId: {}, get vpcId:{}", subnetId, tblSubnet.getVpcId());

        if(!StrUtil.isBlank(tblSubnet.getVpcId()))
        {
            Vpc tblVpc = vpcService.getById(tblSubnet.getVpcId());
            if (null != tblVpc)
            {
                getSubnetDetailInfoRsp.setVpcName(tblVpc.getName());
            }
        }

        return getSubnetDetailInfoRsp;
    }


    public SubnetBaseRspVo createSubnet(SubnetCreateReqVo subnetInfo, @NotBlank String userId)
    {
        String vpcId = subnetInfo.getVpcId();
        String cidr = subnetInfo.getCidr();
        if (!NetworkUtils.isValidCidrIp(cidr))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }

        LambdaQueryWrapper<Subnet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Subnet::getCidr)
                .eq(Subnet::getVpcId, vpcId)
//                .eq(Subnet::getCidr, cidr)
                .ne(Subnet::getPhaseStatus, REMOVED);
        List<String> subnetCidrs = subnetService.listObjs(queryWrapper,Object::toString);
        if (subnetCidrs.size() > 0 && NetworkUtils.isCidrOverlap(cidr, subnetCidrs))
        {
            throw new WebSystemException(ErrorCode.CIDR_OVERLAP, ErrorLevel.INFO);
        }

        Integer addressType = (subnetInfo.getAddressType() == null) ?  0 : subnetInfo.getAddressType();
        String subnetId = Utils.assignUUId();
        log.info("create subnet ,subnetId:{}  vpcId:{} addressType:{}", subnetId, vpcId, addressType);
        Vpc tblVpc = vpcService.getById(vpcId);
        if (null == tblVpc || REMOVED == tblVpc.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VPC_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!Objects.equals(tblVpc.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }

        SubnetUtils.SubnetInfo vpcCidr = new SubnetUtils(tblVpc.getCidr()).getInfo();
        SubnetUtils.SubnetInfo subnetCidr = new SubnetUtils(subnetInfo.getCidr()).getInfo();
        String gatewayIp = subnetCidr.getLowAddress();
        String subnetMax = subnetCidr.getHighAddress();
        if (!(vpcCidr.isInRange(subnetMax) && vpcCidr.isInRange(gatewayIp)))
        {
            log.info("subnet cidr is not in vpc cidr,subnetCidr:{} vpcCidr:{}",subnetCidr, vpcCidr);
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }
        Subnet tblSubnet = new Subnet();
        tblSubnet.setSubnetId(subnetId);
        tblSubnet.setVpcId(vpcId);
        tblSubnet.setName(subnetInfo.getName());
        tblSubnet.setCidr(NetworkUtils.getCidr(subnetCidr.getNetworkAddress(),subnetCidr.getNetmask()));
//              .cidr(subnetInfo.getCidr())
        tblSubnet.setUserId(userId);
        tblSubnet.setGatewayIp(gatewayIp);
        tblSubnet.setAddressType(addressType);
        tblSubnet.setPhaseStatus(PhaseStatus.ADDING);
        tblSubnet.setCreateTime(new Date(System.currentTimeMillis()));
        tblSubnet.setUpdateTime(tblSubnet.getCreateTime());

        boolean ok = subnetService.save(tblSubnet);
        if (!ok)
        {
            log.error("insert tbl_rs_subnet error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        SubnetBaseRspVo baseRspVo = new SubnetBaseRspVo();
        baseRspVo.setSubnetId(tblSubnet.getSubnetId());
        return baseRspVo;
    }

    public SubnetBaseRspVo removeSubnet(@NotBlank String subnetId, String userId)
    {
        Subnet tblSubnet = subnetService.getById(subnetId);
        if (null == tblSubnet || REMOVED == tblSubnet.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.SUBNET_NOT_EXIST, ErrorLevel.INFO);
        }

        if (!Objects.equals(tblSubnet.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }

        LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Port::getSubnetId, subnetId)
                .isNotNull(Port::getPortIdFromAgent)
                .ne(Port::getPhaseStatus, REMOVED);
        if (portService.count(queryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.SUBNET_HAS_PORTS,ErrorLevel.INFO);
        }
        tblSubnet.setPhaseStatus(PhaseStatus.DELETING);
        tblSubnet.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = subnetService.updateById(tblSubnet);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        SubnetBaseRspVo baseRspVo = new SubnetBaseRspVo();
        baseRspVo.setSubnetId(tblSubnet.getSubnetId());
        return baseRspVo;
    }

    //eip
    public void ipIsValid(String ipAddress)
    {
        if (StrUtil.isBlank(ipAddress))
        {
            return;
        }
        if (!NetworkUtils.isIpV4Address(ipAddress))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }
    }

    public Eip updatePublicIp(Eip tblEip, long startPublicIp, long endPublicIp, long publicIp)
    {
        if (0 == startPublicIp && 0 == endPublicIp)
        {
            return tblEip;
        }
//        if (startPublicIp == endPublicIp) return tblEip;
        if (startPublicIp > endPublicIp)
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }
        tblEip.setPublicIp(NetworkUtils.longToIp(publicIp));
        return tblEip;
    }

    public EipsBaseRspVo createEip(EipCreateReqVo eipInfo)
    {
        if (!NetworkUtils.isIpV4Address(eipInfo.getStartIpAddress()))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }
        if (!StrUtil.isBlank(eipInfo.getEndIpAddress()) && !NetworkUtils.isIpV4Address(eipInfo.getEndIpAddress()))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }
        ipIsValid(eipInfo.getStartPublicIpAddress());
        ipIsValid(eipInfo.getEndPublicIpAddress());
        long startPublicIp = 0;
        long startIp = NetworkUtils.ipToLong(eipInfo.getStartIpAddress());
        long endIp = startIp;
        if (StrUtil.isNotBlank(eipInfo.getStartPublicIpAddress()))
        {
            startPublicIp = NetworkUtils.ipToLong(eipInfo.getStartPublicIpAddress());
        }
        long endPublicIp = startPublicIp;
        if (StrUtil.isNotBlank(eipInfo.getEndPublicIpAddress()))
        {
            endPublicIp = NetworkUtils.ipToLong(eipInfo.getEndPublicIpAddress());
        }
        if (StrUtil.isNotBlank(eipInfo.getEndIpAddress()))
        {
            endIp = NetworkUtils.ipToLong(eipInfo.getEndIpAddress());
        }
        //判断最后一个publicIp是否为空,如果不为空，则应该endPublicIp-startPublicIp与endIp-startIp相等
        if (0 != endPublicIp)
        {
            if((endPublicIp - startPublicIp) != (endIp - startIp))
            {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
            }
        }
        List<Eip> tblEips = new ArrayList<>();
        Integer addressType = (eipInfo.getAddressType() == null) ?  0 : eipInfo.getAddressType();
        if (endIp == startIp)
        {
            String eipId = Utils.assignUUId();
            Eip tblEip = new Eip();
            tblEip.setEipId(eipId);
            tblEip.setAddressType(addressType);
            tblEip.setIpaddr(eipInfo.getStartIpAddress());
            tblEip.setStatus(EipPortMapStatus.UNMAPPED);
            tblEip.setCreateTime(new Date(System.currentTimeMillis()));
            tblEip.setUpdateTime(tblEip.getCreateTime());
            tblEip.setPoolId(eipInfo.getEipPoolId());
            tblEip = updatePublicIp(tblEip, startPublicIp, endPublicIp, startPublicIp);
            boolean ok = eipService.save(tblEip);
            if (!ok)
            {
                log.error("insert tbl_eip error");
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            tblEips.add(tblEip);
        }
        else
        {
            if (endIp < startIp)
            {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
            }
            for (long i = 0; i <= endIp-startIp; i++)
            {
                Eip tblEip = new Eip();
                tblEip.setEipId(Utils.assignUUId());
                tblEip.setAddressType(addressType);
                tblEip.setIpaddr(NetworkUtils.longToIp(startIp+i));
                tblEip.setStatus(EipPortMapStatus.UNMAPPED);
                tblEip.setCreateTime(new Date(System.currentTimeMillis()));
                tblEip.setUpdateTime(tblEip.getCreateTime());
                tblEip.setPoolId(eipInfo.getEipPoolId());
                tblEip = updatePublicIp(tblEip, startPublicIp, endPublicIp,startPublicIp+i);
                tblEips.add(tblEip);
            }
            boolean ok = eipService.saveBatch(tblEips);
            if (!ok)
            {
                log.error("insert tbl_eip error");
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        EipsBaseRspVo baseRspVo = new EipsBaseRspVo();
        baseRspVo.setEipIds(tblEips.stream().map(Eip::getEipId).collect(Collectors.toList()));

        //入库操作日志
        String userName = logRpcSerice.getUmsService().getUser(ServiceCombRequestUtils.getUserId()).getUserName();
        String desc = StrUtil.format("创建EIP信息【开始IP：{}，结束IP：{}，地址类型：{}】", eipInfo.getStartIpAddress(), eipInfo.getEndIpAddress(), eipInfo.getAddressType() == 1 ? "out" : "on");
        logRpcSerice.getLogService().addLog(ServiceCombRequestUtils.getUserId(), userName, "网络-EIP", desc);


        return baseRspVo;
    }

    public EipBaseRspVo removeEip(@NotBlank String eipId)
    {
        Eip tblEip = eipService.getById(eipId);
        if (null == tblEip || REMOVED == tblEip.getStatus() )
        {
            throw new WebSystemException(ErrorCode.EIP_NOT_EXISTS, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(EipMap::getEipId, eipId)
                .ne(EipMap::getStatus, REMOVED);
        if (eipMapService.count(queryWrapper) > 0 )
        {
            throw new WebSystemException(ErrorCode.INSTANCE_USED_BY_NAT, ErrorLevel.INFO);
        }
        tblEip.setUpdateTime(new Date(System.currentTimeMillis()));
        tblEip.setStatus(REMOVED);
//        tblEip.setIpaddr();
        boolean ok = eipService.updateById(tblEip);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        EipBaseRspVo baseRspVo = new EipBaseRspVo();
        baseRspVo.setEipId(tblEip.getEipId());
        return baseRspVo;
    }

    public EipInfoVo getEip(Eip tblEip, String userId)
    {
        if (null == tblEip || REMOVED == tblEip.getStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (null == userId)
        {
            log.info("user is admin, eipId: {}", tblEip.getEipId());
        }
        else if (!Objects.equals(tblEip.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        StringBuilder boundName = new StringBuilder();
        String boundId = null;
        String boundType = null;
        if (StrUtil.isNotBlank(tblEip.getBoundId()))
        {
            if (AgentConstant.NAT_BOUND.equals(tblEip.getBoundType()))
            {
                String boundIdsStr = tblEip.getBoundId();
                boundId = boundIdsStr;
                //通过,号进行字符串切割成数组
                String[] boundIds = boundIdsStr.split(",");
                for (String id : boundIds)
                {
                    EipMap tblEipMap = eipMapService.getById(id);
                    if (null == tblEipMap || REMOVED == tblEipMap.getStatus())
                    {
                        log.error("eipMap not exists, eipId: {}, eipMapId:{}", tblEip.getEipId(), tblEip.getBoundId());
                    }
                    else
                    {
                        boundName.append(tblEipMap.getMapName()).append(",");
                        boundType = AgentConstant.NAT_BOUND;
                    }
                }

            }
            else if (AgentConstant.PORT_BOUND.equals(tblEip.getBoundType()))
            {
                Port tblPort = portService.getById(tblEip.getBoundId());
                if (null == tblPort || REMOVED == tblPort.getPhaseStatus())
                {
                    log.error("port not exists, eipId: {}, portId:{}", tblEip.getEipId(), tblEip.getBoundId());
                }
                else
                {
                    ComputeService.Instance instance = combRpcSerice.getVmService().getVmInstanceFromPortId(tblEip.getBoundId());
//                    boundName = new StringBuilder(instance.getInstanceName());
                    boundName = new StringBuilder(instance.getInstanceName());
                    boundId = instance.getInstanceId();
                    boundType = AgentConstant.PORT_BOUND;
                }
            }
        }
        // boundName 去掉逗号
        boundName = new StringBuilder(StrUtil.removeSuffix(boundName.toString(), ","));
        return EipInfoVo.builder()
                .eipId(tblEip.getEipId())
                .addressType(tblEip.getAddressType())
                .ipAddress(tblEip.getIpaddr())
                .boundId(boundId)
                .boundName(boundName.toString())
                .boundType(boundType)
                .eipPoolId(tblEip.getPoolId())
                .publicIp(tblEip.getPublicIp())
                .phaseStatus(tblEip.getStatus())
                .createTime(Utils.formatDate(tblEip.getCreateTime()))
                .updateTime(Utils.formatDate(tblEip.getUpdateTime()))
                .build();
    }

    public EipInfoVo getEip(@NotBlank String eipId, String userId)
    {
        Eip tblEip = eipService.getById(eipId);
        return  getEip(tblEip, userId);
    }

    private LambdaQueryWrapper<Eip> checkEipBindingType(String boundType, LambdaQueryWrapper<Eip> queryWrapper)
    {
        if (null == boundType)
        {
            return queryWrapper;
        }
        switch (boundType)
        {
            case AgentConstant.NAT_BOUND:
                queryWrapper.eq(Eip::getBoundType, AgentConstant.NAT_BOUND)
                        .isNotNull(Eip::getBoundId);
                break;
            case AgentConstant.PORT_BOUND:
                queryWrapper.eq(Eip::getBoundType, AgentConstant.PORT_BOUND)
                        .isNotNull(Eip::getBoundId);
                break;
            case AgentConstant.UNBOUND:
                queryWrapper.isNull(Eip::getBoundId);
                break;
            case AgentConstant.BOUND:
                queryWrapper.isNotNull(Eip::getBoundId);
                break;
            default:
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.ERROR);
        }
        return queryWrapper;
    }

    public EipsRspVo getEips(EipSearchCritical eipSearchCritical, String userId, Boolean oneTOne)
    {
        EipsRspVo getEipsRsp = new EipsRspVo();
        LambdaQueryWrapper<Eip> eipQueryWrapper = new LambdaQueryWrapper<>();
        eipQueryWrapper = checkEipBindingType(eipSearchCritical.getBoundType(), eipQueryWrapper);
        LambdaQueryWrapper<EipMap> eipMapLambdaQueryWrapper = new LambdaQueryWrapper<>();
        eipQueryWrapper.ne(Eip::getStatus, REMOVED);

        eipMapLambdaQueryWrapper.ne(EipMap::getStatus, REMOVED)
                .orderByDesc(EipMap::getCreateTime);
        if (!StrUtil.isBlank(eipSearchCritical.getEipPoolId()))
        {
            eipQueryWrapper.eq(Eip::getPoolId, eipSearchCritical.getEipPoolId());
        }
        if (!StrUtil.isBlank(eipSearchCritical.getVpcId()))
        {
            Vpc tblVpc = vpcService.getById(eipSearchCritical.getVpcId());
            if (null != tblVpc && REMOVED !=tblVpc.getPhaseStatus())
            {
                String poolId = getEipPoolByVlanId(tblVpc.getVlanId());
                if (StrUtil.isBlank(eipSearchCritical.getEipPoolId()))
                {
                    eipQueryWrapper.eq(Eip::getPoolId, poolId);
                }
                else if (!Objects.equals(poolId, eipSearchCritical.getEipPoolId()))
                {
                    return getEipsRsp;
                }
            }
        }
        if (!StrUtil.isBlank(userId))
        {
            eipMapLambdaQueryWrapper.eq(EipMap::getUserId, userId);
            eipQueryWrapper.eq(Eip::getUserId, userId);
        }
        if (!StrUtil.isBlank(eipSearchCritical.getIpAddress()))
        {
            eipQueryWrapper.like(Eip::getIpaddr, eipSearchCritical.getIpAddress());
        }
        //!oneTOne means udp or tcp nat
        if (null != oneTOne && !oneTOne)
        {
            eipMapLambdaQueryWrapper.eq(EipMap::getIsOneToOne, true);
            //todo 不同vpc时，需要过滤掉不同vpc已做nat的eip，保留相同vpc的eip
            LambdaQueryWrapper<Subnet> subnetLambdaQueryWrapper = new LambdaQueryWrapper<>();
            subnetLambdaQueryWrapper.select(Subnet::getSubnetId)
                    .eq(Subnet::getVpcId, eipSearchCritical.getVpcId())
                    .ne(Subnet::getPhaseStatus, REMOVED);
            if (subnetService.count(subnetLambdaQueryWrapper)>0)
            {
                eipMapLambdaQueryWrapper.or()
                .notIn(EipMap::getSubnetId, subnetService.listObjs(subnetLambdaQueryWrapper, Object::toString))
                        .ne(EipMap::getStatus, REMOVED);
            }
        }

        List<String> eipMapList = new ArrayList<>();
        List<EipInfoVo> eipInfoList ;
        long totalNum = eipService.count(eipQueryWrapper);

        if (null != oneTOne)
        {
            eipQueryWrapper.eq(Eip::getBoundType, AgentConstant.NAT_BOUND)
                    .ne(Eip::getStatus,REMOVED)
                    .or(tblEip -> tblEip.isNull(Eip::getBoundId)
                            .ne(Eip::getStatus, REMOVED));
//            eipQueryWrapper.isNull(Eip::getBoundId);
            totalNum = eipService.count(eipQueryWrapper);
            eipMapList = eipMapService.list(eipMapLambdaQueryWrapper).stream()
                    .map(EipMap::getEipId).distinct().collect(Collectors.toList());
            long eipMapNum = eipMapList.size();
            totalNum -= eipMapNum;
        }
        getEipsRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return getEipsRsp;
        }
        //query with page number and page size
//        int current = eipSearchCritical.getPageSize() * (eipSearchCritical.getPageNum()-1);
        Page<Eip> page = new Page<>(eipSearchCritical.getPageNum(), eipSearchCritical.getPageSize());
        Page<Eip> eipPage = eipService.page(page, eipQueryWrapper);
        if (eipPage.getTotal() < 1)
        {
            return getEipsRsp;
        }
        List<Eip> tblEips = eipPage.getRecords();

        if (oneTOne != null)
        {
            List<String> eipIdList = tblEips.stream().map(Eip::getEipId).distinct().collect(Collectors.toList());
            eipIdList.removeAll(eipMapList);
            eipInfoList = eipIdList.stream().map(
                    eipId -> getEip(eipId, userId)
            ).collect(Collectors.toList());
        }
        else
        {
            eipInfoList = tblEips.stream().map(
                    tblEip -> getEip(tblEip, userId)
            ).collect(Collectors.toList());
        }
        getEipsRsp.setEips(eipInfoList);
        return getEipsRsp;
    }

    public String getEipPoolByVlanId(Integer vlanId)
    {
        LambdaQueryWrapper<EipPoolVpcRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(EipPoolVpcRef::getPoolId)
                .eq(EipPoolVpcRef::getVlanId, vlanId)
                .ne(EipPoolVpcRef::getPhaseStatus, REMOVED);
        if (eipPoolVpcRefService.count(queryWrapper)>0)
        {
            return eipPoolVpcRefService.getObj(queryWrapper,Object::toString);
        }
        queryWrapper.clear();
        queryWrapper.select(EipPoolVpcRef::getPoolId)
                .eq(EipPoolVpcRef::getVlanId, 0)
                .ne(EipPoolVpcRef::getPhaseStatus, REMOVED);
        return eipPoolVpcRefService.getObj(queryWrapper,Object::toString);
    }

    public EipInfoVo allocateEip(String userId, @NotBlank String vpcId)
    {
        Vpc tblVpc = vpcService.getById(vpcId);
        if (null == tblVpc || REMOVED == tblVpc.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VPC_NOT_EXIST,ErrorLevel.INFO);
        }
        String poolId = getEipPoolByVlanId(tblVpc.getVlanId());
        if (null == poolId)
        {
            throw new WebSystemException(ErrorCode.EIP_POOL_NOT_EXISTS, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<Eip> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Eip::getStatus, EipPortMapStatus.UNMAPPED)
                .eq(Eip::getPoolId, poolId)
                .isNull(Eip::getUserId);
        if (eipService.count(queryWrapper) < 1)
        {
            throw new WebSystemException(ErrorCode.EIP_NOT_ENOUGH, ErrorLevel.INFO);
        }
        queryWrapper.orderByDesc(Eip::getCreateTime);

        Eip firstEip = eipService.getOne(queryWrapper, false);
        firstEip.setUserId(userId);
        firstEip.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = eipService.updateById(firstEip);
        if (!ok)
        {
            throw  new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return EipInfoVo.builder()
                .addressType(firstEip.getAddressType())
                .eipId(firstEip.getEipId())
                .ipAddress(firstEip.getIpaddr())
                .eipPoolId(firstEip.getPoolId())
                .createTime(Utils.formatDate(firstEip.getCreateTime()))
                .updateTime(Utils.formatDate(firstEip.getUpdateTime()))
                .build();
    }

    public EipPortsRspVo getEipProtocolPorts(String userId, String eipId, int protocol)
    {
        EipPortsRspVo getEipPortsRsp = new EipPortsRspVo();
        List<Integer> ports = eipService.getEipProtocolPorts(userId, eipId, protocol);
        getEipPortsRsp.setPorts(ports);
        getEipPortsRsp.setProtocol(protocol);
        getEipPortsRsp.setEipId(eipId);
        return getEipPortsRsp;
    }

    //PortMap
    public EipMapBaseRspVo updatePortMap(String eipMapId, CommonReq req, String userId)
    {
        EipMap tblEipMap = eipMapService.getById(eipMapId);
        if (null == tblEipMap || REMOVED == tblEipMap.getStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_MAP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (!Objects.equals(tblEipMap.getUserId(), userId) )
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        tblEipMap.setMapName(req.getName());
        tblEipMap.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = eipMapService.updateById(tblEipMap);
        if (!ok)
        {
            throw  new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return EipMapBaseRspVo.builder()
                .natId(eipMapId)
                .build();
    }

    //更新nat网关，只允许修改GlobalPort、LocalPort和协议
    @Transactional(rollbackFor = Exception.class)
    public EipMapBaseRspVo updatePortMap(@NotNull EipPortMapUpdateReqVo eipPortMapInfo, String eipMapId, String userId)
    {
        EipMap tblEipMap = eipMapService.getById(eipMapId);
        if (null == tblEipMap || REMOVED == tblEipMap.getStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_MAP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (!Objects.equals(tblEipMap.getUserId(), userId) )
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        if (tblEipMap.getIsOneToOne())
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        if (null == eipPortMapInfo.getPortMaps() || eipPortMapInfo.getPortMaps().isEmpty())
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        tblEipMap.setStatus(EipPortMapStatus.MAPPING);
        tblEipMap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = eipMapService.updateById(tblEipMap);
        if (!ok)
        {
            throw  new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        LambdaUpdateWrapper<PortMap> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PortMap::getEipMapId, eipMapId)
                .ne(PortMap::getStatus, REMOVED);
        PortMap tblPortMap = new PortMap();
        tblPortMap.setStatus(REMOVED);
        tblPortMap.setUpdateTime(tblEipMap.getUpdateTime());
        ok = portMapService.update(tblPortMap, updateWrapper);
        if (!ok)
        {
            throw  new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        for (EipPortMapCreateReqVo.portMap portMap:eipPortMapInfo.getPortMaps())
        {
            long portCount = eipService.countPortByEipProtocolPort(tblEipMap.getEipId(),portMap.getProtocol(),portMap.getGlobalPort());
            if (portCount > 0)
            {
                log.info("global port already allocated, eip : {}, port: {}",tblEipMap.getEipId(), portMap.getGlobalPort());
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
            PortMap tblPortMapUpdate = new PortMap();
            tblPortMapUpdate.setPortMapId(Utils.assignUUId());
            tblPortMapUpdate.setEipMapId(tblEipMap.getEipMapId());
            tblPortMapUpdate.setProtocol(portMap.getProtocol());
            tblPortMapUpdate.setGlobalPort(portMap.getGlobalPort());
            tblPortMapUpdate.setLocalPort(portMap.getLocalPort());
            tblPortMapUpdate.setStatus(EipPortMapStatus.MAPPING);
            tblPortMapUpdate.setCreateTime(tblEipMap.getUpdateTime());
            tblPortMapUpdate.setUpdateTime(tblEipMap.getUpdateTime());
            if(!portMapService.save(tblPortMapUpdate))
            {
                log.error("insert tbl_rs_port_map error");
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        return EipMapBaseRspVo.builder().natId(tblEipMap.getEipMapId()).build();
    }

    @Transactional(rollbackFor = Exception.class)
    public EipMapBaseRspVo createEipPortMap(@NotNull EipPortMapCreateReqVo eipPortMapInfo, String userId)
    {
        if (null == eipPortMapInfo.getOneToOne()) eipPortMapInfo.setOneToOne(false);
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EipMap::getPortId, eipPortMapInfo.getPortId())
                .ne(EipMap::getStatus, REMOVED);

        if (eipMapService.count(queryWrapper)>0)
        {
            throw new WebSystemException(ErrorCode.EIP_MAP_ALREADY_EXISTS, ErrorLevel.INFO);
        }

        Eip tblEip = eipService.getById(eipPortMapInfo.getEipId());
        if (null == tblEip || REMOVED == tblEip.getStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (null == subnetService.getById(eipPortMapInfo.getSubnetId()))
        {
            throw new WebSystemException(ErrorCode.SUBNET_NOT_EXIST, ErrorLevel.INFO);
        }

        if (!combRpcSerice.getVmService().canCreateEipMap(eipPortMapInfo.getPortId()))
        {
            throw new WebSystemException( ErrorCode.VM_STATUS_ERROR, ErrorLevel.INFO);
        }
        String eipMapId = Utils.assignUUId();
        EipMap tblEipMap = new EipMap();
        tblEipMap.setEipId(eipPortMapInfo.getEipId());
        tblEipMap.setEipMapId(eipMapId);
        tblEipMap.setMapName(eipPortMapInfo.getMapName());
        tblEipMap.setUserId(userId);
        tblEipMap.setSubnetId(eipPortMapInfo.getSubnetId());
        tblEipMap.setPortId(eipPortMapInfo.getPortId());
        tblEipMap.setBandwidth(eipPortMapInfo.getBandwidth());
//      tbl            .baremetalInstanceName(eipPortMapInfo.getBaremetalInstanceName())
        tblEipMap.setIsOneToOne(eipPortMapInfo.getOneToOne());
        tblEipMap.setIsStaticIp(false);
        tblEipMap.setStatus(EipPortMapStatus.MAPPING);
//      tbl            .insideIp(eipPortMapInfo.getInsideIp())
        tblEipMap.setCreateTime(new Date(System.currentTimeMillis()));
        tblEipMap.setUpdateTime(new Date(System.currentTimeMillis()));
        boolean ok = eipMapService.save(tblEipMap);
        if (!ok)
        {
            log.error("insert tbl_rs_eip_map error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(tblEip.getBoundId()) && !tblEip.getBoundId().contains(","))
        {
            EipMap eipMap = eipMapService.getById(tblEip.getBoundId());
            if (eipMap.getIsOneToOne() && eipMap.getStatus() != REMOVED)
            {
                throw new WebSystemException(ErrorCode.EIP_ALREADY_ATTACHED, ErrorLevel.INFO);
            }
        }

        tblEip.setUserId(userId);
        tblEip.setUpdateTime(new Date(System.currentTimeMillis()));
        tblEip.setBoundType(AgentConstant.NAT_BOUND);
        String boundId = tblEip.getBoundId();
        if (StrUtil.isBlank(boundId))
        {
            boundId = eipMapId;
        }
        else
        {
            boundId = removeUnusedBoundId(boundId,eipMapId);
        }
        tblEip.setBoundId(boundId);
        if (!eipService.updateById(tblEip))
        {
            log.error("update tbl_rs_eip error");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        if (eipPortMapInfo.getOneToOne())
        {
            long portCount = eipService.countPortByEipProtocolPort(eipPortMapInfo.getEipId(), Protocols.IP, null);
            if (portCount > 0)
            {
                log.info("eip already allocated, eip : {}", eipPortMapInfo.getEipId());
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }

            PortMap tblPortMap = new PortMap();
            tblPortMap.setPortMapId(Utils.assignUUId());
            tblPortMap.setEipMapId(tblEipMap.getEipMapId());
            tblPortMap.setProtocol(Protocols.IP);
            tblPortMap.setStatus(EipPortMapStatus.MAPPING);
            tblPortMap.setCreateTime(new Date(System.currentTimeMillis()));
            tblPortMap.setUpdateTime(new Date(System.currentTimeMillis()));

            if(!portMapService.save(tblPortMap))
            {
                log.error("insert tbl_rs_port_map error");
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            return EipMapBaseRspVo.builder().natId(tblEipMap.getEipMapId()).build();
        }

        List<EipPortMapCreateReqVo.portMap> portMaps = eipPortMapInfo.getPortMaps();
        for(EipPortMapCreateReqVo.portMap portMap: portMaps)
        {
            long portCount = eipService.countPortByEipProtocolPort(eipPortMapInfo.getEipId(),portMap.getProtocol(),portMap.getGlobalPort());
            if (portCount > 0)
            {
                log.info("global port already allocated, eip : {}, port: {}",eipPortMapInfo.getEipId(), portMap.getGlobalPort());
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
            LambdaQueryWrapper<PortMap> portMapLambdaQueryWrapper = new LambdaQueryWrapper<>();
            portMapLambdaQueryWrapper.eq(PortMap::getProtocol,portMap.getProtocol())
                    .eq(PortMap::getEipMapId, tblEipMap.getEipMapId())
                    .ne(PortMap::getStatus, REMOVED);
            if (null != portMap.getGlobalPort())
            {
                portMapLambdaQueryWrapper.eq(PortMap::getGlobalPort, portMap.getGlobalPort());
            }
            if (null != portMap.getLocalPort())
            {
                portMapLambdaQueryWrapper.eq(PortMap::getLocalPort, portMap.getLocalPort());
            }
            if (0 == portMapService.count(portMapLambdaQueryWrapper))
            {
                PortMap tblPortMap = new PortMap();
                tblPortMap.setPortMapId(Utils.assignUUId());
                tblPortMap.setEipMapId(tblEipMap.getEipMapId());
                tblPortMap.setProtocol(portMap.getProtocol());
                tblPortMap.setGlobalPort(portMap.getGlobalPort());
                tblPortMap.setLocalPort(portMap.getLocalPort());
                tblPortMap.setStatus(EipPortMapStatus.MAPPING);
                tblPortMap.setCreateTime(new Date(System.currentTimeMillis()));
                tblPortMap.setUpdateTime(new Date(System.currentTimeMillis()));

                if (!portMapService.save(tblPortMap))
                {
                    log.error("insert tbl_rs_port_map error");
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            }
        }

        return EipMapBaseRspVo.builder().natId(tblEipMap.getEipMapId()).build();
    }

    private String removeUnusedBoundId(String boundId, String eipMapId)
    {
        List<String> boundIds = StrUtil.split(boundId, ",");
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(EipMap::getEipMapId).in(EipMap::getEipMapId, boundIds)
                .ne(EipMap::getStatus, REMOVED);
        List<String> usedBoundIds = eipMapService.listObjs(queryWrapper, Object::toString);
        if (null == usedBoundIds || usedBoundIds.isEmpty())
        {
            return eipMapId;
        }
        // usedBoundIds to String
        return StrUtil.join(",", usedBoundIds)+","+eipMapId;
    }



    public EipPortMapRspVo getEipPortMap(String eipMapId, String userId)
    {
        EipMap tblEipMap= eipMapService.getById(eipMapId);
        if (null == tblEipMap || REMOVED == tblEipMap.getStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_MAP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (StrUtil.isBlank(tblEipMap.getEipId()))
        {
            throw new WebSystemException(ErrorCode.EIP_MAP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (null == userId)
        {
            log.info("user is admin, eipId: {}", eipMapId);
        }
        else if (!Objects.equals(tblEipMap.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }

        EipPortMapRspVo eipPortMapInfo = new EipPortMapRspVo();
        LambdaQueryWrapper<PortMap> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(PortMap::getEipMapId, eipMapId)
                .ne(PortMap::getStatus, REMOVED);

        if (portMapService.count(queryWrapper) < 1)
        {
            return eipPortMapInfo;
        }

        Subnet tblSubnet = subnetService.getById(tblEipMap.getSubnetId());
        if (null == tblSubnet)
        {
            throw new WebSystemException(ErrorCode.SUBNET_NOT_EXIST, ErrorLevel.INFO);
        }
        Vpc tblVpc = vpcService.getById(tblSubnet.getVpcId());
        if (null == tblVpc)
        {
            throw new WebSystemException(ErrorCode.VPC_NOT_EXIST, ErrorLevel.INFO);
        }
        Eip tblEip = eipService.getById(tblEipMap.getEipId());

        eipPortMapInfo.setEipMapId(tblEipMap.getEipMapId());
        eipPortMapInfo.setEipId(tblEip.getEipId());
        eipPortMapInfo.setSubnetCidr(tblSubnet.getCidr());
        eipPortMapInfo.setSubnetId(tblSubnet.getSubnetId());
        eipPortMapInfo.setVpcId(tblSubnet.getVpcId());
        eipPortMapInfo.setSubnetName(tblSubnet.getName());
        eipPortMapInfo.setVpcName(tblVpc.getName());
        eipPortMapInfo.setBandwidth(tblEipMap.getBandwidth());
        eipPortMapInfo.setUserId(tblEipMap.getUserId());
        eipPortMapInfo.setMapName(tblEipMap.getMapName());
        eipPortMapInfo.setEipAddress(tblEip.getIpaddr());
        eipPortMapInfo.setInsideIp(tblEipMap.getInsideIp());
        eipPortMapInfo.setOneToOne(tblEipMap.getIsOneToOne());
        eipPortMapInfo.setPhaseStatus(tblEipMap.getStatus());
        eipPortMapInfo.setPublicIp(tblEip.getPublicIp());
        eipPortMapInfo.setCreateTime(Utils.formatDate(tblEipMap.getCreateTime()));

        VmService vmService = combRpcSerice.getVmService();
        BaremetalService bmService = combRpcSerice.getBmService();

        String vmInstanceId = portService.getById(tblEipMap.getPortId()).getInstanceId();
        ComputeService.Instance instance;
        if (!StrUtil.isBlank(vmInstanceId))
        {
            eipPortMapInfo.setVm(true);
            instance = vmService.getVmInstanceFromPortId(tblEipMap.getPortId());
        }
        else
        {
            eipPortMapInfo.setVm(false);
            instance = bmService.getBaremetalInstanceFromPortId(tblEipMap.getPortId());
        }
        eipPortMapInfo.setInstanceName(instance.getInstanceName());
        eipPortMapInfo.setInstanceId(instance.getInstanceId());


        if (!tblEipMap.getIsOneToOne())
        {
            List<PortMap> portMaps = portMapService.list(queryWrapper);
            List<EipPortMapCreateReqVo.portMap> portMapList = new ArrayList<>();
            for (PortMap tblPortMap: portMaps)
            {
                EipPortMapCreateReqVo.portMap tmpPortMap = new EipPortMapCreateReqVo.portMap();
                tmpPortMap.setGlobalPort(tblPortMap.getGlobalPort());
                tmpPortMap.setLocalPort(tblPortMap.getLocalPort());
                tmpPortMap.setPortMapId(tblPortMap.getPortMapId());
                tmpPortMap.setProtocol(tblPortMap.getProtocol());
                tmpPortMap.setCreateTime(Utils.formatDate(tblPortMap.getCreateTime()));
                tmpPortMap.setUpdateTime(Utils.formatDate(tblPortMap.getUpdateTime()));
                portMapList.add(tmpPortMap);
            }
            eipPortMapInfo.setPortMaps(portMapList);
        }
        return eipPortMapInfo;
    }

    @Transactional(rollbackFor = Exception.class)
    public EipMapBaseRspVo updateEipPortMap(@NotBlank String eipMapId, List<EipPortMapCreateReqVo.portMap> portMaps,String userId)
    {
        Set<EipPortMapCreateReqVo.portMap> portMapSet = new HashSet<>(portMaps);
        LambdaQueryWrapper<PortMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PortMap::getEipMapId, eipMapId)
                .ne(PortMap::getStatus, REMOVED);
        List<PortMap> tblPortMaps = portMapService.list(queryWrapper);
        List<EipPortMapCreateReqVo.portMap> portMapList = new ArrayList<>();
        for (PortMap tblPortMap: tblPortMaps)
        {
            EipPortMapCreateReqVo.portMap tmpPortMap = new EipPortMapCreateReqVo.portMap();
            tmpPortMap.setGlobalPort(tblPortMap.getGlobalPort());
            tmpPortMap.setLocalPort(tblPortMap.getLocalPort());
            tmpPortMap.setPortMapId(tblPortMap.getPortMapId());
            tmpPortMap.setProtocol(tblPortMap.getProtocol());
            portMapList.add(tmpPortMap);
        }
        Set<EipPortMapCreateReqVo.portMap> tblPortMapSet = new HashSet<>(portMapList);
        if (Objects.equals(portMapSet, tblPortMapSet))
        {
            return EipMapBaseRspVo.builder().natId(eipMapId).build();
        }
        for ( EipPortMapCreateReqVo.portMap portMap:portMapSet)
        {
            PortMap tblPortMap = new PortMap();
            tblPortMap.setPortMapId(Utils.assignUUId());
            tblPortMap.setEipMapId(eipMapId);
            tblPortMap.setProtocol(portMap.getProtocol());
            tblPortMap.setGlobalPort(portMap.getGlobalPort());
            tblPortMap.setLocalPort(portMap.getLocalPort());
            tblPortMap.setStatus(EipPortMapStatus.MAPPING);
            tblPortMap.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
            tblPortMap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            portMapService.save(tblPortMap);
        }
        for (PortMap tblPortMap :tblPortMaps)
        {
            tblPortMap.setStatus(REMOVED);
            tblPortMap.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
            boolean ok = portMapService.updateById(tblPortMap);
            if (!ok)
            {
                throw  new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.ERROR);
            }
        }
        return EipMapBaseRspVo.builder().natId(eipMapId).build();
    }

    @Transactional(rollbackFor = {Exception.class})
    public EipMapBaseRspVo removeEipPortMap(@NotBlank String eipMapId, String userId)
    {
        EipMap tblEipMap = eipMapService.getById(eipMapId);
        boolean updateTblRsEip = false;
        if (null == tblEipMap || REMOVED == tblEipMap.getStatus())
        {
            throw  new WebSystemException(ErrorCode.EIP_MAP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (EipPortMapStatus.AGENT_UNMAPPING == tblEipMap.getStatus() || EipPortMapStatus.UNMAPPING == tblEipMap.getStatus())
        {
            throw  new WebSystemException(ErrorCode.EIP_MAP_IS_UNMAPPING, ErrorLevel.INFO);
        }

        if (!Objects.equals(userId, tblEipMap.getUserId()))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        LambdaUpdateWrapper<PortMap> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(PortMap::getEipMapId,eipMapId);
        PortMap portMap = new PortMap();
        portMap.setStatus(EipPortMapStatus.UNMAPPING);
        if (portMapService.update(portMap, updateWrapper))
        {
            tblEipMap.setStatus(PhaseStatus.DELETING);
            tblEipMap.setUpdateTime(new Date(System.currentTimeMillis()));
            if (!eipMapService.updateById(tblEipMap))
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
            if (tblEipMap.getIsOneToOne())
            {
                updateTblRsEip = true;
            }
            else
            {
                LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(EipMap::getEipId, tblEipMap.getEipId())
                        .ne(EipMap::getStatus, REMOVED);
                if (0 == eipMapService.count(queryWrapper))
                {
                    updateTblRsEip = true;
                }
            }
            if (updateTblRsEip)
            {
                Eip tblEip = eipService.getById(tblEipMap.getEipId());
                tblEip.setUserId(null);
                tblEip.setStatus(EipPortMapStatus.UNMAPPED);
                tblEip.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
                if (!eipService.updateById(tblEip))
                {
                    throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
                }
            }
        }
        return EipMapBaseRspVo.builder().natId(eipMapId).build();
    }

    public EipPortMapsRspVo getEipPortMaps(EipPortMapSearchCritical searchCritical, String userId)
    {
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(EipMap::getStatus, REMOVED);
        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(EipMap::getUserId, userId);
        }
        if (!StrUtil.isBlank(searchCritical.getName()))
        {
            queryWrapper.like(EipMap::getMapName, searchCritical.getName());
        }
        if (!StrUtil.isBlank(searchCritical.getEipId()))
        {
            queryWrapper.eq(EipMap::getEipId, searchCritical.getEipId());
        }

        EipPortMapsRspVo getEipPortMapsRsp = new EipPortMapsRspVo();

        long totalNum = eipMapService.count(queryWrapper);
        getEipPortMapsRsp.setTotalNum(totalNum);
        if (totalNum < 1)
        {
            return  getEipPortMapsRsp;
        }
        queryWrapper.orderByDesc(EipMap::getCreateTime);
        //query with page number and page size
//        int current = searchCritical.getPageSize() * (searchCritical.getPageNum()-1);
        Page<EipMap> page = new Page<>(searchCritical.getPageNum(), searchCritical.getPageSize());
        Page<EipMap> eipMapPage = eipMapService.page(page, queryWrapper);
        List<EipMap> tblEipMaps = eipMapPage.getRecords();

        List<EipPortMapRspVo> eipPortMapInfoList = new ArrayList<>();
        for (EipMap tblEipMap : tblEipMaps)
        {
            EipPortMapRspVo eipPortMap = getEipPortMap(tblEipMap.getEipMapId(),tblEipMap.getUserId());
            eipPortMapInfoList.add(eipPortMap);
        }

        getEipPortMapsRsp.setEipPortMaps(eipPortMapInfoList);

        return getEipPortMapsRsp;
    }

    public TopologyRspVo getTopology(@NotBlank String vpcId, String userId)
    {
        LambdaQueryWrapper<Subnet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Subnet::getVpcId, vpcId)
                .ne(Subnet::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(userId))
        {
            queryWrapper.eq(Subnet::getUserId, userId);
        }

        List<Subnet> tblSubnets = subnetService.list(queryWrapper);
        TopologyRspVo topologyRsp = new TopologyRspVo();
        if (0 == tblSubnets.size())
        {
            return  topologyRsp;
        }
        List<String> subnetIds = tblSubnets.stream().map(Subnet::getSubnetId).collect(Collectors.toList());
        Map<String, List<ComputeService.InstanceInfo>> subnetInstancesMap = getSubnetInstancesMap(subnetIds);
        List<TopologyRspVo.SubnetTopology> subnetTopologies = tblSubnets.stream().map(rsSubnet -> {
            TopologyRspVo.SubnetTopology subnetTopology = new TopologyRspVo.SubnetTopology();
            subnetTopology.setSubnetId(rsSubnet.getSubnetId());
            subnetTopology.setSubnetName(rsSubnet.getName());
            subnetTopology.setCidr(rsSubnet.getCidr());
            subnetTopology.setInstanceInfos(subnetInstancesMap.get(rsSubnet.getSubnetId()).stream().peek(
                    instanceInfo ->
                    {
                        if (null == instanceInfo.getPortId())
                        {
                             return;
                        }
                        String ip = portService.getById(instanceInfo.getPortId()).getIpAddress();
                        instanceInfo.setIp(ip);
                    }
            ).collect(Collectors.toList()));
            return subnetTopology;
        }).collect(Collectors.toList());
        topologyRsp.setVpcId(vpcId);
        topologyRsp.setSubnetTopologies(subnetTopologies);
        return topologyRsp;
    }

    @Transactional(rollbackFor = Exception.class)
    public EipPoolBaseRspVo createEipPool(EipPoolCreateReqVo request)
    {
        EipPool tblEipPool = new EipPool();
        tblEipPool.setPoolId(Utils.assignUUId());
        tblEipPool.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblEipPool.setUpdateTime(tblEipPool.getCreateTime());
        tblEipPool.setName(request.getName());
        tblEipPool.setDescription(request.getDescription());
        tblEipPool.setPhaseStatus(PhaseStatus.ADDED);
        boolean ok = eipPoolService.save(tblEipPool);
        if (!ok)
        {
            log.error("create eip pool failed");
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        EipPoolAndVpcRefCreateReqVo createEipPoolAndVpcRefReq = new EipPoolAndVpcRefCreateReqVo();
        createEipPoolAndVpcRefReq.setEipPoolId(tblEipPool.getPoolId());
        createEipPoolAndVpcRefReq.setVlanId(request.getVlanId());
        ok = addEipPoolAndVpcRef(createEipPoolAndVpcRefReq);
        if (!ok)
        {
            log.error("add EipPoolAndVpcRef failed");

            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return EipPoolBaseRspVo.builder().eipPoolId(tblEipPool.getPoolId()).build();
    }

    public EipPoolBaseRspVo updateEipPool(@NotBlank String eipPoolId, EipPoolCreateReqVo request)
    {
        EipPool tblEipPool = eipPoolService.getById(eipPoolId);
        if (null == tblEipPool || REMOVED == tblEipPool.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_POOL_NOT_EXISTS,ErrorLevel.INFO);
        }
        tblEipPool.setName(request.getName());
        tblEipPool.setDescription(request.getDescription());
        tblEipPool.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if ( !eipPoolService.updateById(tblEipPool))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        LambdaUpdateWrapper<EipPoolVpcRef> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(EipPoolVpcRef::getPoolId, eipPoolId);
        EipPoolVpcRef tblEipPoolVpcRef = new EipPoolVpcRef();
        tblEipPoolVpcRef.setVlanId(request.getVlanId());
        tblEipPoolVpcRef.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if (!eipPoolVpcRefService.update(tblEipPoolVpcRef, updateWrapper))
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }

        return EipPoolBaseRspVo.builder().eipPoolId(tblEipPool.getPoolId()).build();
    }

    @Transactional(rollbackFor = Exception.class)
    public  EipPoolBaseRspVo removeEipPool(@NotBlank String eipPoolId)
    {
        EipPool tblEipPool = eipPoolService.getById(eipPoolId);
        if (null == tblEipPool|| REMOVED == tblEipPool.getPhaseStatus() )
        {
            throw new WebSystemException(ErrorCode.EIP_POOL_NOT_EXISTS,ErrorLevel.INFO);
        }
        LambdaQueryWrapper<Eip> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Eip::getPoolId, eipPoolId)
                .ne(Eip::getStatus, REMOVED);
        if (eipService.count(queryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.EIP_POOL_HAS_EIPS, ErrorLevel.INFO);
        }
        tblEipPool.setPhaseStatus(REMOVED);
        tblEipPool.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = eipPoolService.updateById(tblEipPool);
//        boolean ok = eipPoolService.update(tblEipPool, Wrappers.<EipPool>lambdaUpdate()
//                .set(EipPool::getPhaseStatus, REMOVED)
//                .set(EipPool::getUpdateTime, Utils.buildDate(System.currentTimeMillis()))
//                .eq(EipPool::getPoolId, tblEipPool.getPoolId()));
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        LambdaUpdateWrapper<EipPoolVpcRef> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(EipPoolVpcRef::getPoolId, eipPoolId);
        EipPoolVpcRef eipPoolVpcRef = new EipPoolVpcRef();
        eipPoolVpcRef.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        eipPoolVpcRef.setPhaseStatus(REMOVED);
        if (eipPoolVpcRefService.count(updateWrapper) > 0)
        {
            if (!eipPoolVpcRefService.update(eipPoolVpcRef, updateWrapper))
            {
                throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
            }
        }
        return EipPoolBaseRspVo.builder().eipPoolId(eipPoolId).build();
    }

    public EipPoolDetailInfoRspVo getEipPool(@NotBlank String eipPoolId)
    {
        EipPool tblEipPool = eipPoolService.getById(eipPoolId);
        if (null == tblEipPool || REMOVED == tblEipPool.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_POOL_NOT_EXISTS,ErrorLevel.INFO);
        }
        LambdaQueryWrapper<EipPoolVpcRef> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EipPoolVpcRef::getPoolId, eipPoolId)
                .ne(EipPoolVpcRef::getPhaseStatus, REMOVED);
        int vlanId = 0;
        if (eipPoolVpcRefService.count(queryWrapper)> 0)
        {
            vlanId = eipPoolVpcRefService.getOne(queryWrapper, false).getVlanId();
        }
        EipPoolDetailInfoRspVo getEipPoolDetailInfoRsp = new EipPoolDetailInfoRspVo();
        getEipPoolDetailInfoRsp.setEipPool(tblEipPool);
        getEipPoolDetailInfoRsp.setVlanId(vlanId);

        return getEipPoolDetailInfoRsp;
    }

    public EipPoolsRspVo getEipPools(String name)
    {
        EipPoolsRspVo getEipPoolsRsp = new EipPoolsRspVo();
        LambdaQueryWrapper<EipPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(EipPool::getPhaseStatus, REMOVED);
        if (!StrUtil.isBlank(name))
        {
            queryWrapper.like(EipPool::getName, name);
        }
        long totalNum = eipPoolService.count(queryWrapper);
        getEipPoolsRsp.setTotalNum(totalNum);

        if (totalNum > 0)
        {   queryWrapper.orderByDesc(EipPool::getCreateTime);
            getEipPoolsRsp.setEipPools(eipPoolService.list(queryWrapper));
        }
        return getEipPoolsRsp;
    }

    //Relationship between eip and vpc
    public boolean addEipPoolAndVpcRef(@NotNull EipPoolAndVpcRefCreateReqVo request) throws WebSystemException
    {
        if (request.getVlanId() > 4094 || request.getVlanId() < 0)
        {
            return false;
        }

        String poolId = getEipPoolByVlanId(request.getVlanId());
        if (null != poolId)
        {
            throw new WebSystemException(ErrorCode.EIP_POOL_VPC_RELATION_ALREADY_EXISTS, ErrorLevel.INFO);
        }
        EipPoolVpcRef tblEipPoolVpcRef = new EipPoolVpcRef();
        tblEipPoolVpcRef.setPoolVpcId(Utils.assignUUId());
        tblEipPoolVpcRef.setPoolId(request.getEipPoolId());
        tblEipPoolVpcRef.setVlanId(request.getVlanId());
        tblEipPoolVpcRef.setCreateTime(Utils.buildDate(System.currentTimeMillis()));
        tblEipPoolVpcRef.setUpdateTime(tblEipPoolVpcRef.getCreateTime());
        tblEipPoolVpcRef.setPhaseStatus(PhaseStatus.ADDED);
        return eipPoolVpcRefService.save(tblEipPoolVpcRef);

    }

    public  EipPoolVpcRefBaseRspVo removeEipPoolAndVpcRef(@NotBlank String eipPoolVpcRefId)
    {
        EipPoolVpcRef tblEipPoolVpcRef = eipPoolVpcRefService.getById(eipPoolVpcRefId);
        if (null == tblEipPoolVpcRef)
        {
            throw new WebSystemException(ErrorCode.EIP_POOL_VPC_RELATION_NOT_EXISTS,ErrorLevel.INFO);
        }
        boolean ok = eipPoolVpcRefService.update(tblEipPoolVpcRef, Wrappers.<EipPoolVpcRef>lambdaUpdate()
                .set(EipPoolVpcRef::getPhaseStatus, REMOVED)
                .set(EipPoolVpcRef::getUpdateTime, Utils.buildDate(System.currentTimeMillis()))
                .eq(EipPoolVpcRef::getPoolVpcId, tblEipPoolVpcRef.getPoolVpcId()));
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return EipPoolVpcRefBaseRspVo.builder().eipPoolVpcRefId(tblEipPoolVpcRef.getPoolVpcId()).build();

    }

    public List<String> getVips(String subnetId)
    {
        LambdaQueryWrapper<Port> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(Port::getIpAddress)
                .ne(Port::getPhaseStatus, REMOVED)
                .eq(Port::getSubnetId, subnetId)
                .eq(Port::getType, PortType.vip);

        return portService.listObjs(queryWrapper, Objects::toString);
    }

    public NetSummeryInfo getNetSummery(String userId)
    {
        LambdaQueryWrapper<EipMap> eipMapQueryWrapper = new LambdaQueryWrapper<>();
        eipMapQueryWrapper.ne(EipMap::getStatus, REMOVED);

        LambdaQueryWrapper<Eip> eipQueryWrapper = new LambdaQueryWrapper<>();
        eipQueryWrapper.ne(Eip::getStatus, REMOVED);

        LambdaQueryWrapper<SecurityGroup> sgQueryWrapper = new LambdaQueryWrapper<>();
        sgQueryWrapper.ne(SecurityGroup::getPhaseStatus, REMOVED);

        if (!combRpcSerice.getUmsService().isAdminUser(userId))
        {
            eipMapQueryWrapper.eq(EipMap::getUserId, userId);
            eipQueryWrapper.eq(Eip::getUserId, userId);
            sgQueryWrapper.eq(SecurityGroup::getUserId, userId);
        }
        NetSummeryInfo netSummeryInfo = new NetSummeryInfo();
        netSummeryInfo.setNatCount((int)eipMapService.count(eipMapQueryWrapper));
        netSummeryInfo.setEipCount((int)eipService.count(eipQueryWrapper));
        netSummeryInfo.setSgCount((int)sgService.count(sgQueryWrapper));
        return netSummeryInfo;
    }

    public long getVpcCount(String userId)
    {
        LambdaQueryWrapper<Vpc> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Vpc::getPhaseStatus, REMOVED);
        if (combRpcSerice.getUmsService().isAdminUser(userId))
        {
            queryWrapper.eq(Vpc::getUserId, userId);
        }
        return vpcService.count(queryWrapper);

    }

    public long getSubnetCount(String userId)
    {
        LambdaQueryWrapper<Subnet> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(Subnet::getPhaseStatus, REMOVED);
        if (combRpcSerice.getUmsService().isAdminUser(userId))
        {
            queryWrapper.eq(Subnet::getUserId, userId);
        }
        return subnetService.count(queryWrapper);
    }

    public Map<String, List<ComputeService.InstanceInfo>> getSubnetInstancesMap(List<String> subnetIds)
    {
        VmService vmService = combRpcSerice.getVmService();
        BaremetalService bmService = combRpcSerice.getBmService();
        Map<String, List<ComputeService.InstanceInfo>> bmInstanceInfos =  bmService.getInstanceInfos(subnetIds);
        Map<String, List<ComputeService.InstanceInfo>> vmInstanceInfos = vmService.getInstanceInfos(subnetIds);
        Map<String, List<ComputeService.InstanceInfo>> result = new HashMap<>();

        bmInstanceInfos.forEach(
                (k,v)->{
                    List<ComputeService.InstanceInfo> vmInstanceInfoList = vmInstanceInfos.get(k);
                    List<ComputeService.InstanceInfo> bmInstanceInfoList = bmInstanceInfos.get(k);
                    vmInstanceInfoList.addAll(bmInstanceInfoList);
                    result.put(k,vmInstanceInfoList);
                }
        );
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public String attachEip(String eipId, String portId, String userId)
    {
        if(StrUtil.isBlank(eipId) || StrUtil.isBlank(portId))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        Eip tblEip = eipService.getById(eipId);
        if (null == tblEip || REMOVED == tblEip.getStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_NOT_EXISTS, ErrorLevel.INFO);
        }
        Port tblPort = portService.getById(portId);
        if (null == tblPort || REMOVED == tblPort.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        if (PhaseStatus.ADDED != tblPort.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.PORT_NOT_CREATED, ErrorLevel.INFO);
        }
        if (StrUtil.isNotBlank(tblPort.getEipId()) && !Objects.equals(tblPort.getEipId(), eipId))
        {
            throw new WebSystemException(ErrorCode.PORT_HAS_EIP, ErrorLevel.INFO);
        }
        if (Objects.equals(tblEip.getBoundId(), portId) && Objects.equals(tblPort.getEipId(), eipId))
        {
            return tblEip.getEipId();
        }
        if (tblPort.getEipPhaseStatus() != null && tblPort.getEipPhaseStatus() != PhaseStatus.DETACH_EIP_DONE)
        {
            throw new WebSystemException(ErrorCode.PORT_HAS_EIP, ErrorLevel.INFO);
        }
        LambdaQueryWrapper<EipMap> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EipMap::getPortId, portId)
                .ne(EipMap::getStatus, REMOVED);
        if (eipMapService.count(queryWrapper) > 0)
        {
            throw new WebSystemException(ErrorCode.INSTANCE_USED_BY_NAT, ErrorLevel.INFO);
        }

        tblPort.setEipId(eipId);
        tblPort.setEipPhaseStatus(PhaseStatus.ATTACH_EIP_INIT);
        tblPort.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        if (StrUtil.isNotBlank(tblEip.getBoundId()))
        {
            throw new WebSystemException(ErrorCode.PORT_HAS_EIP, ErrorLevel.INFO);
        }
        tblEip.setUserId(userId);
        tblEip.setBoundId(portId);
        tblEip.setBoundType(AgentConstant.PORT_BOUND);
        tblEip.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = portService.updateById(tblPort);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        ok = eipService.updateById(tblEip);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
        return tblEip.getEipId();
    }

    @Transactional(rollbackFor = Exception.class)
    public String detachEip(String portId, String eipId, String userId)
    {
        if(StrUtil.isBlank(eipId) && StrUtil.isBlank(portId))
        {
            throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
        }
        String userName = logRpcSerice.getUmsService().getUser(userId).getUserName();

        if (StrUtil.isBlank(eipId) && StrUtil.isNotBlank(portId))
        {
           detachEipFromVm(portId, userId, userName);
        }
        if (StrUtil.isBlank(portId) && StrUtil.isNotBlank(eipId))
        {
            detachFromEip(eipId, userId, userName);
        }
        return StrUtil.isBlank(portId) ? eipId : portId;
    }

    @Transactional(rollbackFor = Exception.class)
    public void detachFromEip(String eipId, String userId, String userName)
    {
        Eip tblEip = eipService.getById(eipId);
        if (null == tblEip || REMOVED == tblEip.getStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_NOT_EXISTS, ErrorLevel.INFO);
        }
        String boundId = tblEip.getBoundId();
        String eipAddr = tblEip.getIpaddr();
        if (AgentConstant.NAT_BOUND.equals(tblEip.getBoundType()))
        {
            if (StrUtil.isBlank(tblEip.getBoundId()))
            {
                throw new WebSystemException(ErrorCode.EIP_MAP_NOT_EXISTS, ErrorLevel.INFO);
            }
            //如果含有","则不允许删除
            if (tblEip.getBoundId().contains(","))
            {
                throw new WebSystemException(ErrorCode.PARAM_ERROR, ErrorLevel.INFO);
            }
            removeEipPortMap(tblEip.getBoundId(), userId);
            String desc = StrUtil.format("删除Nat网关【id：{}】， EIP【ip：{}】", boundId, eipAddr);
            logRpcSerice.getLogService().addLog(userId, userName, "网络-Nat网关", desc );
            return;
        }
        if (StrUtil.isBlank(boundId))
        {
            throw new WebSystemException(ErrorCode.PORT_EIP_NOT_EXIST, ErrorLevel.INFO);
        }
        if (!Objects.equals(tblEip.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }
        Port tblPort = portService.getById(boundId);
        if (PhaseStatus.ATTACH_EIP_DONE != tblPort.getEipPhaseStatus() &&
           PhaseStatus.ATTACH_EIP_ERR != tblPort.getEipPhaseStatus() && PhaseStatus.DETACH_EIP_ERR != tblPort.getEipPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.PORT_EIP_NOT_EXIST, ErrorLevel.INFO);
        }
        tblPort.setEipPhaseStatus(PhaseStatus.DETACH_EIP_INIT);
        tblPort.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        boolean ok = portService.updateById(tblPort);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
    }

    public void detachEipFromVm(String portId,  String userId, String userName)
    {
        Port tblPort = portService.getById(portId);
        if (null == tblPort || REMOVED == tblPort.getPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.VM_INSTANCE_NOT_EXIST, ErrorLevel.INFO);
        }
        String desc = StrUtil.format("从虚机【id：{}】 解绑EIP【id：{}】", tblPort.getInstanceId(), tblPort.getEipId());
        logRpcSerice.getLogService().addLog(userId,userName, "网络-EIP", desc );
        if (PhaseStatus.ATTACH_EIP_DONE != tblPort.getEipPhaseStatus())
        {
            throw new WebSystemException(ErrorCode.PORT_EIP_NOT_EXIST, ErrorLevel.INFO);
        }
        String eipId = tblPort.getEipId();
        if (StrUtil.isBlank(eipId))
        {
            throw new WebSystemException(ErrorCode.PORT_EIP_NOT_EXIST, ErrorLevel.INFO);
        }
//            tblPort.setEipId(null);
        tblPort.setEipPhaseStatus(PhaseStatus.DETACH_EIP_INIT);
        tblPort.setUpdateTime(Utils.buildDate(System.currentTimeMillis()));
        Eip tblEip = eipService.getById(eipId);
        if (null == tblEip || REMOVED == tblEip.getStatus())
        {
            throw new WebSystemException(ErrorCode.EIP_NOT_EXISTS, ErrorLevel.INFO);
        }
        if (!Objects.equals(tblEip.getUserId(), userId))
        {
            throw new WebSystemException(ErrorCode.User_Not_Grant, ErrorLevel.INFO);
        }

        boolean ok = portService.updateById(tblPort);
        if (!ok)
        {
            throw new WebSystemException(ErrorCode.UPDATE_DATABASE_ERR, ErrorLevel.INFO);
        }
    }
}
