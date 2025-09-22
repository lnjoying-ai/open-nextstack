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
import com.lnjoying.justice.network.domain.dto.request.*;
import com.lnjoying.justice.network.domain.dto.response.*;
import com.lnjoying.justice.network.entity.search.SecurityGroupSearchCritical;
import com.lnjoying.justice.network.service.biz.SgService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestSchema(schemaId = "securityGroup")
@RequestMapping("/network/v1")
@Api(value = "Security Group Controller",tags = {"Security Group Controller"})
public class SecurityGroupController  extends RestWebController
{
    
    @Autowired
    private SgService securityGroupService;

    public SecurityGroupController()
    {
        System.out.println("SecurityGroupController: ");
    }

    // security groups
    @LogAnnotation(resource = "网络-安全组",description = "添加安全组【名称：{}，描述：{}】", obtainParameter = "name,description")
    @PostMapping(value = "/sgs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create securityGroup", response = Object.class)
    public ResponseEntity<SecurityGroupBaseRspVo> createSg(@ApiParam(value = "CreateSecurityGroupReq", required = true, name = "CreateSecurityGroupReq") @RequestBody @Valid SecurityGroupCreateReqVo request) throws WebSystemException
    {
        try
        {
            log.info("create security group: {}", request);
            String userId = ServiceCombRequestUtils.getUserId();
            return ResponseEntity.status(HttpStatus.CREATED).body(securityGroupService.createSecurityGroup(request,userId));
        }
        catch (Exception e)
        {
            log.error("create  security group failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/sgs", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get security groups", response = Object.class)
    public ResponseEntity<SecurityGroupsRspVo> getSgs(
            @ApiParam(name = "name") @RequestParam(required = false,value = "name") String name,
            @ApiParam(name = "sg_id") @RequestParam(required = false,value = "sg_id") String sgId,
            @ApiParam(name = "page_size") @RequestParam(required = false,value = "page_size") Integer pageSize,
            @ApiParam(name = "page_num") @RequestParam(required = false,value = "page_num") Integer pageNum,
            @ApiParam(name = "user_id") @RequestParam(required = false,value = "user_id") String userId

    ) throws WebSystemException
    {
        try
        {
            log.debug("get security group  list");
            SecurityGroupSearchCritical pageSearchCritical = new SecurityGroupSearchCritical();
            if(null != pageSize) pageSearchCritical.setPageSize(pageSize);
            if(null != pageNum) pageSearchCritical.setPageNum(pageNum);
            if(!StrUtil.isBlank(name)) pageSearchCritical.setName(name);
            if(!StrUtil.isBlank(sgId)) pageSearchCritical.setSgId(sgId);
            if(ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(securityGroupService.getSecurityGroups(pageSearchCritical, userId));
            }
            SecurityGroupsRspVo getSecurityGroupsRsp = securityGroupService.getSecurityGroups(pageSearchCritical,ServiceCombRequestUtils.getUserId());
            return ResponseEntity.ok(getSecurityGroupsRsp);
        }
        catch (Exception e)
        {
            log.error("get security groups failed: {}", e.getMessage());
            throw throwWebException(e);
        }
    }

    @GetMapping(value = "/sgs/{sgId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get sg detail info", response = Object.class)
    public ResponseEntity<SecurityGroupDetailInfoRspVo> getSecurityGroupDetailInfo(
            @ApiParam(value = "sgId", required = true, name = "sgId") @PathVariable("sgId") String sgId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    ) throws WebSystemException
    {
        try
        {
            log.info("get security group , request:{}, userId:{}",sgId, userId);
            if (ServiceCombRequestUtils.isAdmin())
            {
                return ResponseEntity.ok(securityGroupService.getSecurityGroup(sgId, null));
            }
            SecurityGroupDetailInfoRspVo getSecurityGroupDetailInfoRsp = securityGroupService.getSecurityGroup(sgId, userId);
            return ResponseEntity.ok(getSecurityGroupDetailInfoRsp);
        }
        catch (Exception e)
        {
            log.error("get security group error: {}, sgId: {}",e.getMessage(), sgId);
            throw  throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-安全组",description = "编辑安全组【名称：{}，描述：{}】", obtainParameter = "name,description")
    @PutMapping(value = "/sgs/{sgId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "update security group", response = Object.class)
    public ResponseEntity<SecurityGroupBaseRspVo> updateSg(
            @ApiParam(value = "sgId", required = true, name = "sgId") @PathVariable("sgId") String sgId,
            @ApiParam(value = "CommonReq", required = true, name = "CommonReq") @RequestBody @Valid CommonReq request
    )throws WebSystemException
    {
        try
        {
            String userId = ServiceCombRequestUtils.getUserId();
            log.debug("update security group: {}, userId: {}", sgId, userId);
            SecurityGroupBaseRspVo baseRsp = securityGroupService.updateSecurityGroup(sgId, userId, request);
            return ResponseEntity.ok(baseRsp);
        }
        catch (Exception e)
        {
            log.error("update security group error: {}, vmInstanceId: {}",e.getMessage(), sgId);
            throw  throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-安全组",description = "删除安全组【id：{}】",obtainParameter = "sgId")
    @DeleteMapping(value = "/sgs/{sgId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete security group", response = Object.class)
    public ResponseEntity<SecurityGroupBaseRspVo> removeSg(
            @ApiParam(value = "sgId", required = true, name = "sgId") @PathVariable("sgId") String sgId,
            @RequestHeader(name = "X-UserId", required = false) String userId
    )throws WebSystemException
    {
        try
        {
            log.debug("remove security group: {}, userId: {}", sgId, userId);
            SecurityGroupBaseRspVo baseRsp = securityGroupService.removeSecurityGroup(sgId, userId);
            return ResponseEntity.ok(baseRsp);
        }
        catch (Exception e)
        {
            log.error("remove security group error: {}, vmInstanceId: {}",e.getMessage(), sgId);
            throw  throwWebException(e);
        }
    }

    //security rules、
//    @LogAnnotation(resource = "网络-安全组",description = "添加安全规则")
    @PostMapping(value = "/sgs/{sgId}/rules", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "create security groups", response = Object.class)
    public ResponseEntity<List<String>> createSgRule(
            @ApiParam(value = "sgId", required = true, name = "sgId") @PathVariable("sgId") String sgId,
            @ApiParam(value = "CreateUpdateSgRulesReq", required = true, name = "CreateUpdateSgRulesReq") @RequestBody @Valid SgRulesCreateUpdateReqVo request
    ) throws IOException
    {
        try
        {
            log.info("create security rules: {}", request);
            String userId = ServiceCombRequestUtils.getUserId();
            List<String> results = new ArrayList<>();
            for (SgRuleCreateUpdateReqVo createUpdateSgRuleReq: request.getCreateSgRules())
            {
                SgRuleBaseRspVo baseRsp = securityGroupService.createSgRule(createUpdateSgRuleReq,sgId,userId);
                results.add(baseRsp.getRuleId());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(results);
        }
        catch (Exception e)
        {
            log.error("create security rule failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

//    @LogAnnotation(resource = "网络-安全组",description = "编辑安全规则")
    @PutMapping(value = "/sgs/{sgId}/rules/{ruleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "put security rule detail info", response = Object.class)
    public ResponseEntity<SgRuleBaseRspVo> updateRule(
            @ApiParam(value = "CreateUpdateSgRuleReq", required = true, name = "CreateUpdateSgRuleReq") @RequestBody SgRuleCreateUpdateReqVo request,
            @ApiParam(value = "sgId", required = true, name = "sgId") @PathVariable("sgId") String sgId,
            @ApiParam(value = "ruleId", required = true, name = "ruleId") @PathVariable("ruleId") String ruleId
    ) throws WebSystemException
    {
        try
        {
            String userId = ServiceCombRequestUtils.getUserId();
            log.info("update security rule, request:{}, userId:{}",ruleId, userId);
            SgRuleBaseRspVo baseRsp= securityGroupService.updateSgRule(request,sgId,ruleId, userId);
            return ResponseEntity.ok(baseRsp);
        }
        catch (Exception e)
        {
            log.error("update security group error: {}, vmInstanceId: {}",e.getMessage(), ruleId);
            throw  throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-安全组",description = "删除安全规则【id：{}】",obtainParameter = "ruleId")
    @DeleteMapping(value = "/rules/{ruleId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "delete security rule", response = Object.class)
    public ResponseEntity<SgRuleBaseRspVo> deleteRule(
            @ApiParam(value = "ruleId", required = true, name = "ruleId") @PathVariable("ruleId") String ruleId

    ) throws WebSystemException
    {
        try
        {
            String userId = ServiceCombRequestUtils.getUserId();
            log.info("delete security rule, request:{}, userId:{}",ruleId, userId);
            SgRuleBaseRspVo baseRsp= securityGroupService.removeSgRule(ruleId,userId);
            return ResponseEntity.ok(baseRsp);
        }
        catch (Exception e)
        {
            log.error("update security group error: {}, vmInstanceId: {}",e.getMessage(), ruleId);
            throw  throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-安全组",description = "添加关联实例【sgId：{}，vmInstanceId：{}】",obtainParameter = "sgId,vmInstances")
    @PostMapping(value = "/sgs/{sgId}/bound", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "the security group is applied to the VM", response = Object.class)
    public ResponseEntity<List<String>> boundVmInstance(@ApiParam(value = "sgId", required = true, name = "sgId") @PathVariable("sgId") String sgId,
                                                        @ApiParam(value = "BoundOrUnBoundSecurityGroup", required = true, name = "BoundOrUnBoundSecurityGroup") @RequestBody @Valid SecurityGroupBoundUnboundReqVo request
    ) throws WebSystemException
    {
        try
        {
            log.info("security group is applied to the vm ,vmInstanceIds:{}, sgId:{}",request,sgId );
            List<String> result = new ArrayList<>();
            String userId = ServiceCombRequestUtils.getUserId();
            for (String vmInstanceId: request.getVmInstances())
            {
                BaseRsp baseRsp = securityGroupService.vmInstanceBoundSg(vmInstanceId, sgId, userId);
                result.add(baseRsp.getUuid());
            }
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }
        catch (Exception e)
        {
            log.error("bound security group failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

    @LogAnnotation(resource = "网络-安全组",description = "解除关联实例【sgId：{}，vmInstanceId：{}】",obtainParameter = "sgId,vmInstances")
    @PostMapping(value = "/sgs/{sgId}/unbound", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "unbound the security group", response = Object.class)
    public ResponseEntity<List<String>> unboundVmInstance(@ApiParam(value = "sgId", required = true, name = "sgId") @PathVariable("sgId") String sgId,
                                                          @ApiParam(value = "BoundOrUnBoundSecurityGroup", required = true, name = "BoundOrUnBoundSecurityGroup") @RequestBody @Valid SecurityGroupBoundUnboundReqVo request
    ) throws WebSystemException
    {
        try
        {
            log.info("VM is unbound from the security group ,vmInstanceId:{}, sgId:{}",request,sgId );
            List<String> result = new ArrayList<>();
            String userId = ServiceCombRequestUtils.getUserId();
            for (String vmInstanceId: request.getVmInstances())
            {
                BaseRsp baseRsp = securityGroupService.vmInstanceUnBoundSg(vmInstanceId, sgId, userId);
                result.add(baseRsp.getUuid());
            }
            return ResponseEntity.status(HttpStatus.OK).body(result);
        }
        catch (Exception e)
        {
            log.error("unbound  security group failed: {}", e.getMessage());

            throw throwWebException(e);
        }
    }

}
