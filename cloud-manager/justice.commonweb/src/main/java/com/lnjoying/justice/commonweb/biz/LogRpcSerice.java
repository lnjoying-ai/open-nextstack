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

package com.lnjoying.justice.commonweb.biz;

import com.lnjoying.justice.schema.service.operation.LogService;
import com.lnjoying.justice.schema.service.ums.UmsService;
import org.apache.servicecomb.provider.pojo.RpcReference;
import org.springframework.stereotype.Component;

@Component("LogRpcService")
public class LogRpcSerice {

    @RpcReference(microserviceName = "operation", schemaId = "logService")
    private LogService logService;
    @RpcReference(microserviceName = "ums", schemaId = "umsService")
    private UmsService umsService;

    public LogService getLogService() {
        return logService;
    }

    public UmsService getUmsService() {
        return umsService;
    }


}
