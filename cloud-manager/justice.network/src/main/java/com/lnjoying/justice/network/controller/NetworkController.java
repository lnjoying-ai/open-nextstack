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

package com.lnjoying.justice.network.controller;

import cn.hutool.core.util.StrUtil;
import com.lnjoying.justice.commonweb.aspect.LogAnnotation;
import com.lnjoying.justice.commonweb.controller.RestWebController;
import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.justice.schema.service.network.NetworkService.NetSummeryInfo;
import com.lnjoying.justice.network.domain.dto.request.*;
import com.lnjoying.justice.network.domain.dto.response.*;
import com.lnjoying.justice.network.entity.search.EipPortMapSearchCritical;
import com.lnjoying.justice.network.entity.search.EipSearchCritical;
import com.lnjoying.justice.network.entity.search.SubnetSearchCritical;
import com.lnjoying.justice.network.entity.search.VpcSearchCritical;
import com.lnjoying.justice.network.service.biz.NetworkService;
import com.lnjoying.justice.network.service.biz.SgService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.apache.servicecomb.swagger.invocation.exception.InvocationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@RestSchema(schemaId = "network")
@RequestMapping("/network/v1")
@Controller
@Slf4j
@Api(value = "Network Controller", tags = {"Network Controller"})
public class NetworkController extends RestWebController
{
//    private static final Logger log = LogManager.getLogger();

    private static final String REG_UUID = "[0-9a-f]{32}";

    @Autowired
    private NetworkService networkService;

    @Autowired
    private SgService securityGroupService;

    //vpc

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN')")
    @GetMapping(value = "/vpcs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vpcs", response = Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getVpcs(@ApiParam(name = "name") @RequestParam(required = false) String name,
                          @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
                          @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum,
                          @ApiParam(name = "vpc_phase") @RequestParam(required = false,value = "vpc_phase") Integer vpcPhase,
                          @ApiParam(name = "user_id") @RequestParam(required = false,value = "user_id") String userId
    ) throws WebSystemException
    {
        try
        {

            log.info("get vpcs ,name:{} pageSize:{} pageNum:{} ", name, pageSize, pageNum);
            VpcSearchCritical pageSearchCritical = new VpcSearchCritical();
            pageSearchCritical.setName(name);
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (vpcPhase != null) pageSearchCritical.setPhaseStatus(vpcPhase);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.getVpcs(pageSearchCritical, userId);
            }
            return networkService.getVpcs(pageSearchCritical, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @GetMapping(value = "/vpcs/{vpcId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vpc detail info", response = VpcDetailInfoRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getVpc(@ApiParam(value = "vpcId", required = true, name = "vpcId")
                         @PathVariable("vpcId") @Pattern(regexp = REG_UUID) String vpcId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get vpc detail info, vpcId: {}", vpcId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.getVpc(vpcId, null);
            }
            return networkService.getVpc(vpcId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-虚拟私有云",description = "创建虚拟私有云【名称：{},网段：{}】", obtainParameter = "name,cidr")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @PostMapping(value = "/vpcs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create  vpc", response = VpcBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object postVpc(@ApiParam(value = "CreateVpcReq", required = true, name = "CreateVpcReq")
                          @RequestBody @Valid VpcCreateReqVo vpc
    ) throws WebSystemException
    {
        try
        {
            log.info("post vpc info: {}", vpc);
            String userId = ServiceCombRequestUtils.getUserId();
            CompletableFuture.runAsync(() -> securityGroupService.createDefaultSg(userId));
            return networkService.createVpc(vpc, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            log.error(e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-虚拟私有云",description = "删除虚拟私有云【id：{}]】", obtainParameter = "vpcId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @DeleteMapping(value = "/vpcs/{vpcId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "remove vpc", response = VpcBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object removeVpc(@ApiParam(value = "vpcId", required = true, name = "vpcId")
                         @PathVariable("vpcId") @Pattern(regexp = REG_UUID) String vpcId
    ) throws WebSystemException
    {
        try
        {
            log.debug("remove vpc, vpcId: {}", vpcId);
            return networkService.removeVpc(vpcId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-虚拟私有云",description = "更新虚拟私有云【id：{}]】", obtainParameter = "vpcId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @PutMapping(value = "/vpcs/{vpcId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update vpc", response = VpcBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object updateVpc(@ApiParam(value = "vpcId", required = true, name = "vpcId")
                            @PathVariable("vpcId") @Pattern(regexp = REG_UUID) String vpcId,
                            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody @Valid CommonReq request
    ) throws WebSystemException
    {
        try
        {
            log.debug("update vpc, vpcId: {}", vpcId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.updateVpc(vpcId,request, null);
            }
            return networkService.updateVpc(vpcId,request, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    //subnet
    @LogAnnotation(resource = "网络-子网",description = "更新子网【id：{}]】", obtainParameter = "subnetId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @PutMapping(value = "/subnets/{subnetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update subnet", response = SubnetBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object updateSubnet(@ApiParam(value = "subnetId", required = true, name = "subnetId")
                            @PathVariable("subnetId") @Pattern(regexp = REG_UUID) String subnetId,
                            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody @Valid CommonReq request
    ) throws WebSystemException
    {
        try
        {
            log.debug("update subnet, subnetId: {}", subnetId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.updateSubnet(subnetId, request, null);
            }
            return networkService.updateSubnet(subnetId, request, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }



    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @GetMapping(value = "/subnets", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get subnets", response = Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getSubnets(@ApiParam(name = "name") @RequestParam(required = false) String name,
                             @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
                             @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum,
                             @ApiParam(name = "vpc_id") @RequestParam(required = false,value = "vpc_id") String vpcId,
                             @ApiParam(name = "subnet_phase") @RequestParam(required = false,value = "subnet_phase") Integer subnetPhase,
                             @ApiParam(name = "user_id") @RequestParam(required = false,value = "user_id") String userId
    ) throws WebSystemException
    {
        try
        {
            SubnetSearchCritical pageSearchCritical = new SubnetSearchCritical();
            pageSearchCritical.setName(name);
            pageSearchCritical.setVpcId(vpcId);
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (subnetPhase != null) pageSearchCritical.setPhaseStatus(subnetPhase);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.getSubnets(pageSearchCritical, userId);
            }
            return networkService.getSubnets(pageSearchCritical, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @GetMapping(value = "/subnets/{subnetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get subnet detail info", response = SubnetDetailInfoRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getSubnet(@ApiParam(value = "subnetId", required = true, name = "subnetId")
                            @PathVariable("subnetId") @Pattern(regexp = REG_UUID) String subnetId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get subnet detail info, subnetId: {}", subnetId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.getSubnet(subnetId, null);
            }
            return networkService.getSubnet(subnetId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-子网",description = "创建子网【名称：{}，网段：{}，vpcId：{}】", obtainParameter = "name,cidr,vpcId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @PostMapping(value = "/subnets", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create  subnet", response = SubnetBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object postSubnet(@ApiParam(value = "CreateSubnetReq", required = true, name = "CreateSubnetReq")
                             @RequestBody @Valid SubnetCreateReqVo subnet
    ) throws WebSystemException
    {
        try
        {
            log.info("post subnet info: {}", subnet);
            SubnetBaseRspVo baseRsp = networkService.createSubnet(subnet, ServiceCombRequestUtils.getUserId());
            return ResponseEntity.ok(baseRsp);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-子网",description = "删除子网【id：{}】", obtainParameter = "subnetId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @DeleteMapping(value = "/subnets/{subnetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "remove subnet", response = SubnetBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object removeSubnet(@ApiParam(value = "subnetId", required = true, name = "subnetId")
                            @PathVariable("subnetId") @Pattern(regexp = REG_UUID) String subnetId
    ) throws WebSystemException
    {
        try
        {
            log.debug("remove subnet, subnetId: {}", subnetId);
            return networkService.removeSubnet(subnetId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN')")
    @GetMapping(value = "/eips", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get eips", response = Object.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getEips(
                          @ApiParam(name = "oneToOne") @RequestParam(required = false) Boolean oneTOne,
                          @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
                          @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum,
                          @ApiParam(name = "user_id") @RequestParam(required = false,value = "user_id") String userId,
                          @ApiParam(name = "eip_pool_id") @RequestParam(required = false, value = "eip_pool_id") String eipPoolId,
                          @ApiParam(name = "vpc_id") @RequestParam(required = false, value = "vpc_id") String vpcId,
                          @ApiParam(name = "bound_type") @RequestParam(required = false, value = "bound_type") String boundType,
                          @ApiParam(name = "name") @RequestParam(required = false, value = "name") String ipAddress

                          ) throws WebSystemException
    {
        try
        {
            log.debug("get eip list");
            log.info("pageSize:{} pageNum:{} ", pageSize, pageNum);
            EipSearchCritical pageSearchCritical = new EipSearchCritical();

            if (ipAddress != null) pageSearchCritical.setIpAddress(ipAddress.trim());
            if (pageNum != null) pageSearchCritical.setPageNum(pageNum);
            if (pageSize != null) pageSearchCritical.setPageSize(pageSize);
            if (eipPoolId != null) pageSearchCritical.setEipPoolId(eipPoolId.trim());
            if (vpcId != null) pageSearchCritical.setVpcId(vpcId);
            if (boundType != null) pageSearchCritical.setBoundType(boundType);
            if (ServiceCombRequestUtils.isAdmin() && null == userId)
            {
                return networkService.getEips(pageSearchCritical, null, oneTOne);
            }

            return networkService.getEips(pageSearchCritical, ServiceCombRequestUtils.getUserId(), oneTOne);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @GetMapping(value = "/eips/{eipId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get eip detail info", response = EipInfoVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getEip(@ApiParam(value = "eipId", required = true, name = "eipId")
                         @PathVariable("eipId") @Pattern(regexp = REG_UUID) String eipId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get eip detail info, eipId: {}", eipId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.getEip(eipId, null);
            }
            return networkService.getEip(eipId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

//    @LogAnnotation(resource = "网络-EIP",description = "创建EIP信息【开始IP：{}，结束IP：{}】",obtainParameter = "startIpAddress,endIpAddress")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @PostMapping(value = "/eips", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create  eip", response = EipBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object postEips(@ApiParam(value = "CreatEipReq", required = true, name = "CreatEipReq")
                          @RequestBody @Valid EipCreateReqVo req) throws WebSystemException
    {
        try
        {
            log.info("post eip info: {}", req);
            return networkService.createEip(req);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-EIP",description = "删除EIP信息【id：{}】", obtainParameter = "eipId")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @DeleteMapping(value = "/eips/{eipId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "remove eip", response = EipBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object removeEip(@ApiParam(value = "eipId", required = true, name = "eipId")
                         @PathVariable("eipId") @Pattern(regexp = REG_UUID) String eipId
    ) throws WebSystemException
    {
        try
        {
            log.debug("remove eip, eipId: {}", eipId);
            return networkService.removeEip(eipId);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-EIP",description = "eip分配到用户【id：{}】", obtainParameter = "vpcId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @PostMapping(value = "/eips/allocate/{vpcId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "allocate eip to user", response = EipInfoVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object allocateEip(@ApiParam(value = "vpcId", required = true, name = "vpcId")
                              @PathVariable("vpcId") @Pattern(regexp = REG_UUID) String vpcId) throws WebSystemException
    {
        try
        {
            log.debug("allocate eip to user: {}", ServiceCombRequestUtils.getUserId());
            return networkService.allocateEip(ServiceCombRequestUtils.getUserId(), vpcId);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/eips/{eipId}/ports", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get tcp or udp ports", response = EipPortsRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getProtocolPorts(@ApiParam(value = "eipId", required = true, name = "eipId")
                                   @PathVariable("eipId") @Pattern(regexp = REG_UUID) String eipId,
                                   @ApiParam(name = "user_id") @RequestParam(required = false,value = "user_id") String userId,
                                   @ApiParam(name = "protocol") @RequestParam(required = true) short protocol
    ) throws WebSystemException
    {
        try
        {
            log.debug("get ports, eipId: {}", eipId);
            if (ServiceCombRequestUtils.isAdmin() && null != userId)
            {
                return networkService.getEipProtocolPorts(userId, eipId, protocol);
            }
            return networkService.getEipProtocolPorts(ServiceCombRequestUtils.getUserId(), eipId, protocol);
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN')")
    @GetMapping(value = "/portMaps", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get portMaps", response = EipPortMapsRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getPortMaps(@ApiParam(name = "user_id") @RequestParam(required = false, value = "user_id") String userId,
                              @ApiParam(name = "name") @RequestParam(required = false) String name,
                              @ApiParam(name = "eip_id") @RequestParam(required = false) String eipId,
                              @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
                              @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum
    ) throws WebSystemException
    {
        try
        {
            log.info("userId:{} pageSize:{} pageNum:{} ", userId, pageSize, pageNum);
            EipPortMapSearchCritical pageSearchCritical = new EipPortMapSearchCritical();
            if (null != pageNum) pageSearchCritical.setPageNum(pageNum);
            if (null != pageSize) pageSearchCritical.setPageSize(pageSize);
            if (StrUtil.isNotBlank(name)) pageSearchCritical.setName(name);
            if (StrUtil.isNotBlank(eipId)) pageSearchCritical.setEipId(eipId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.getEipPortMaps(pageSearchCritical, userId);
            }
            return networkService.getEipPortMaps(pageSearchCritical, ServiceCombRequestUtils.getUserId());
        }
        catch(InvocationException e)
        {
            return new EipPortMapsRspVo();
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @GetMapping(value = "/portMaps/{eipMapId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get portMaps detail info", response = EipPortMapRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getPortMap(@ApiParam(value = "eipMapId", required = true, name = "eipMapId")
                             @PathVariable("eipMapId") @Pattern(regexp = REG_UUID) String eipMapId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get eipPortMap info, eipMapId: {}", eipMapId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.getEipPortMap(eipMapId, null);
            }
            return networkService.getEipPortMap(eipMapId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-Nat网关",description = "创建Nat网关【名称：{}，eipId：{}】", obtainParameter = "mapName,eipId")
    @PostMapping(value = "/portMaps", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create new portMaps", response = EipMapBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object postPortMaps(@RequestBody @Valid EipPortMapCreateReqVo eipPortMapInfo) throws WebSystemException
    {
        try
        {
            log.info("post eipPortMap info: {}", eipPortMapInfo);
            return networkService.createEipPortMap(eipPortMapInfo, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-Nat 网关",description = "修改Nat网关【名称：{}】", obtainParameter = "name")
    @PutMapping(value = "/nat-gateways/{eipMapId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update nat-gateway", response = EipMapBaseRspVo.class)
    public ResponseEntity<EipMapBaseRspVo> updatePortMapName(
            @ApiParam(value = "eipMapId", required = true, name = "eipMapId") @PathVariable("eipMapId") String eipMapId,
            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody @Valid CommonReq request)
    {
        try
        {
            log.info("update portMap: eipMapId:{} name:{}", eipMapId, request);
            return ResponseEntity.ok(networkService.updatePortMap(eipMapId, request, ServiceCombRequestUtils.getUserId()));
        }
        catch (Exception e)
        {
            log.error("update portMap name failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

//    @LogAnnotation(resource = "网络-Nat 网关",description = "修改Nat网关【名称：{}】", obtainParameter = "name")
    @PutMapping(value = "/nat-gateways/{eipMapId}/ports", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update nat-gateway", response = EipMapBaseRspVo.class)
    public ResponseEntity<EipMapBaseRspVo> updatePortMap(
            @ApiParam(value = "eipMapId", required = true, name = "eipMapId") @PathVariable("eipMapId") String eipMapId,
            @ApiParam(value = "EipPortMapUpdateReqVo", required = true, name = "EipPortMapUpdateReqVo") @RequestBody @Valid EipPortMapUpdateReqVo eipPortMapInfo)
    {
        try
        {
            log.info("update portMap: eipMapId:{} eipPortMap:{}", eipMapId, eipPortMapInfo);
            return ResponseEntity.ok(networkService.updatePortMap(eipPortMapInfo,eipMapId, ServiceCombRequestUtils.getUserId()));
        }
        catch (Exception e)
        {
            log.error("update portMap failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }


    @LogAnnotation(resource = "网络-Nat网关",description = "删除Nat网关【id：{}】", obtainParameter = "mapId")
    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN' )")
    @DeleteMapping(value = "/portMaps/{mapId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete portMap", response = EipMapBaseRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object delPortMap(@ApiParam(value = "mapId", required = true, name = "mapId")
                             @PathVariable("mapId") @Pattern(regexp = REG_UUID) String mapId
    ) throws WebSystemException
    {
        try
        {
            log.debug("remove eipPortMap, mapId: {}", mapId);
            return networkService.removeEipPortMap(mapId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/topology/{vpcId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get topology", response = TopologyRspVo.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public Object getAdminTopology(@ApiParam(value = "vpcId", required = true, name = "vpcId")
                                   @PathVariable("vpcId") @Pattern(regexp = REG_UUID) String vpcId
    ) throws WebSystemException
    {
        try
        {
            log.debug("get topology, vpcId: {}", vpcId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return networkService.getTopology(vpcId, null);
            }
            return networkService.getTopology(vpcId, ServiceCombRequestUtils.getUserId());
        }
        catch (Exception e)
        {
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-EIP Pool",description = "创建EIP Pool【名称：{}，描述：{}，vlanId：{}】", obtainParameter = "name,description,vlanId")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @PostMapping(value = "/eip_pools", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create eip pool", response = EipPoolBaseRspVo.class)
    public ResponseEntity<EipPoolBaseRspVo> createEipPool(@ApiParam(value = "CreateEipPoolReq", required = true, name = "CreateEipPoolReq") @RequestBody @Valid EipPoolCreateReqVo request)
    {
        try
        {
            log.info("add eip pool: {}", request);
            return ResponseEntity.status(HttpStatus.CREATED).body(networkService.createEipPool(request));
        }
        catch (Exception e)
        {
            log.error("create  eip failed: {}", e.getMessage());

            e.printStackTrace();
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-EIP Pool",description = "修改EIP Pool【名称：{}，描述：{}，vlanId：{}】", obtainParameter = "name,description,vlanId")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @PutMapping(value = "/eip_pools/{eipPoolId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update eip pool", response = EipPoolBaseRspVo.class)
    public ResponseEntity<EipPoolBaseRspVo> updateEipPool(
            @ApiParam(value = "eipPoolId", required = true, name = "eipPoolId") @PathVariable("eipPoolId") String eipPoolId,
            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody @Valid EipPoolCreateReqVo request)
    {
        try
        {
            log.info("update eip pool: eipPoolId:{} name:{}", eipPoolId, request);
            return ResponseEntity.ok(networkService.updateEipPool(eipPoolId, request));
        }
        catch (Exception e)
        {
            log.error("update  eip pool failed: {}", e.getMessage());

            e.printStackTrace();
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-EIP Pool",description = "删除EIP Pool【id：{}】", obtainParameter = "eipPoolId")
    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN')")
    @DeleteMapping(value = "/eip_pools/{eipPoolId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "remove eip pool", response = EipPoolBaseRspVo.class)
    public ResponseEntity<EipPoolBaseRspVo> removeEipPool(
            @ApiParam(value = "eipPoolId", required = true, name = "eipPoolId") @PathVariable("eipPoolId") String eipPoolId)
    {
        try
        {
            return ResponseEntity.ok(networkService.removeEipPool(eipPoolId));
        }
        catch (Exception e)
        {
            log.error("remove  eip pool failed: {}", e.getMessage());

            e.printStackTrace();
            throw throwWebException(e);
        }
    }


    @LogAnnotation(resource = "网络-EIP",description = "绑定EIP【id：{}】", obtainParameter = "eipId")
    @PutMapping(value = "/eips/{eipId}/attach", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "attach eip", response = EipPoolBaseRspVo.class)
    public ResponseEntity<EipBaseRspVo> attachEip(
            @ApiParam(value = "eipId", required = true, name = "eipId") @PathVariable("eipId") String eipId,
            @ApiParam(value = "EipAttachReq", required = true, name = "EipAttachReq") @RequestBody @Valid EipAttachReq req)
    {
        try
        {
            String userId = ServiceCombRequestUtils.getUserId();
            eipId = networkService.attachEip(eipId,req.getPortId(),userId);
            EipBaseRspVo eipBaseRspVo = new EipBaseRspVo();
            eipBaseRspVo.setEipId(eipId);
            return ResponseEntity.ok(eipBaseRspVo);
        }
        catch (Exception e)
        {
            log.error("attach eip failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-EIP",description = "解绑EIP【id：{}】", obtainParameter = "eipId")
    @PutMapping(value = "/eips/{eipId}/detach", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "detach eip", response = EipPoolBaseRspVo.class)
    public ResponseEntity<EipBaseRspVo> detachEip(
            @ApiParam(value = "eipId", required = true, name = "eipId") @PathVariable("eipId") String eipId)
    {
        try
        {
            String userId = ServiceCombRequestUtils.getUserId();
            eipId = networkService.detachEip("",eipId,userId);
            EipBaseRspVo eipBaseRspVo = new EipBaseRspVo();
            eipBaseRspVo.setEipId(eipId);
            return ResponseEntity.ok(eipBaseRspVo);
        }
        catch (Exception e)
        {
            log.error("attach eip failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

//    @LogAnnotation(resource = "网络-EIP",description = "解绑EIP【id：{}】", obtainParameter = "eipId")
    @PutMapping(value = "/ports/{portId}/detach", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "detach eip", response = EipPoolBaseRspVo.class)
    public ResponseEntity<EipBaseRspVo> detachEipFromPort(
            @ApiParam(value = "portId", required = true, name = "portId") @PathVariable("portId") String portId)
    {
        try
        {
            String userId = ServiceCombRequestUtils.getUserId();
            String eipId = networkService.detachEip(portId,"",userId);
            EipBaseRspVo eipBaseRspVo = new EipBaseRspVo();
            eipBaseRspVo.setEipId(eipId);
            return ResponseEntity.ok(eipBaseRspVo);
        }
        catch (Exception e)
        {
            log.error("attach eip failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }


    @GetMapping(value = "/eip_pools/{eipPoolId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get eip pool  info", response = Object.class)
    public ResponseEntity<EipPoolDetailInfoRspVo> getEipPool(
            @ApiParam(value = "eipPoolId", required = true, name = "eipPoolId") @PathVariable("eipPoolId") String eipPoolId)
    {
        try
        {
            EipPoolDetailInfoRspVo getEipPoolDetailInfoRsp = networkService.getEipPool(eipPoolId);
            return ResponseEntity.ok(getEipPoolDetailInfoRsp);
        }
        catch (Exception e)
        {
            log.error("get eip pool failed: {}", e.getMessage());
            e.printStackTrace();
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/eip_pools", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get eip pools", response = Object.class)
    public ResponseEntity<EipPoolsRspVo> getEipPools(
            @ApiParam(name = "name") @RequestParam(required = false) String name)
    {
        try
        {
            EipPoolsRspVo getEipPoolsRsp = networkService.getEipPools(name);
            return ResponseEntity.ok(getEipPoolsRsp);
        }
        catch (Exception e)
        {
            log.error("get eip pool failed: {}", e.getMessage());
            e.printStackTrace();
            throw throwWebException(e);
        }
    }


    @GetMapping(value = "/net_stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get net stats", response = NetSummeryInfo.class)
    public ResponseEntity<NetSummeryInfo> getNetStats()
    {
        String userId = ServiceCombRequestUtils.getUserId();
        try
        {
            return ResponseEntity.ok(networkService.getNetSummery(userId));
        }
        catch (Exception e)
        {
            log.error("get net summery failed: {}", e.getMessage());
            e.printStackTrace();
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/vpc_count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vpc count", response = Object.class)
    public ResponseEntity<Long> getVpcCount()
    {
        String userId = ServiceCombRequestUtils.getUserId();
        try
        {
            return ResponseEntity.ok(networkService.getVpcCount(userId));
        }
        catch (Exception e)
        {
            log.error("get vpc count failed: {}", e.getMessage());
            e.printStackTrace();
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/subnet_count", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get subnet count", response = Object.class)
    public ResponseEntity<Long> getSubnetCount()
    {
        String userId = ServiceCombRequestUtils.getUserId();
        try
        {
            return ResponseEntity.ok(networkService.getSubnetCount(userId));
        }
        catch (Exception e)
        {
            log.error("get subnet count failed: {}", e.getMessage());
            e.printStackTrace();
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/vips/{subnetId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get vip list", response = Object.class)
    public ResponseEntity<List<String>> getVips(@ApiParam(value = "subnetId", required = true, name = "subnetId")
                                                @PathVariable("subnetId") String subnetId)
    {
        String userId = ServiceCombRequestUtils.getUserId();
        try
        {
            return ResponseEntity.ok(networkService.getVips(subnetId));
        }
        catch (Exception e)
        {
            log.error("get vips failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }
}
