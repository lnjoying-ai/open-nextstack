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

package com.lnjoying.justice.operation.controller;

import com.lnjoying.justice.commonweb.util.ServiceCombRequestUtils;
import com.lnjoying.justice.operation.domain.dto.response.EventsResp;
import com.lnjoying.justice.operation.entity.search.EventSearchCritical;
import com.lnjoying.justice.operation.service.TblEventService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.provider.rest.common.RestSchema;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

@RestSchema(schemaId = "event")
@Controller
@Api(value = "Event Controller", tags = {"Event Controller"})
@RequestMapping("/operation/v1")
@Slf4j
public class EventController
{
    private static final String REG_UUID = "[0-9a-f]{32}";

    @Resource
    private TblEventService tblEventService;

    //    @PreAuthorize("hasAnyRole('ROLE_ALL_ADMIN', 'ROLE_ALL_TENANT_ADMIN')")
    @GetMapping(value = "/events", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "get events")
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public EventsResp getEvents(@ApiParam(name = "content") @RequestParam(required = false, value = "content") String content,
                                 @ApiParam(name = "detail_info") @RequestParam(required = false, value = "detail_info") String detailInfo,
                                 @ApiParam(name = "start_time") @RequestParam(required = false, value = "start_time") Long startTime,
                                 @ApiParam(name = "end_time") @RequestParam(required = false, value = "end_time") Long endTime,
                                 @RequestHeader(name = "X-UserId", required = false) String userId,
                                 @ApiParam(name = "page_size") @RequestParam(required = false, value = "page_size") Integer pageSize,
                                 @ApiParam(name = "page_num") @RequestParam(required = false, value = "page_num") Integer pageNum)
    {
        log.info("get events, content: {}, start_time: {}, end_time: {}, page_size: {}, page_num: {}", content, startTime, endTime, pageSize, pageNum);
        EventSearchCritical pageSearchCritical = new EventSearchCritical();
        pageSearchCritical.setContent(content);
        pageSearchCritical.setStartTime(startTime);
        pageSearchCritical.setEndTime(endTime);
        pageSearchCritical.setDetailInfo(detailInfo);
        if (null != pageNum) pageSearchCritical.setPageNum(pageNum);
        if (null != pageSize) pageSearchCritical.setPageSize(pageSize);
        if (ServiceCombRequestUtils.isAdmin())
        {
            return tblEventService.selectEventPage(null,pageSearchCritical);
        }

        return tblEventService.selectEventPage(userId, pageSearchCritical) ;
    }
}
