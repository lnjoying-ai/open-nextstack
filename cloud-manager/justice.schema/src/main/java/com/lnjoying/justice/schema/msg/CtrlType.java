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

package com.lnjoying.justice.schema.msg;

public interface CtrlType
{
    String SYNC_REGION_CONNECTED_DEV_REQ = "sync_region_connected_dev_req";
    String SYNC_EDGE_IF_STATE_REQ        = "sync_edge_if_state_req";
    String SYNC_GW_REQ                   = "sync_gw_req";
    String SET_REGION_INFO_REQ           = "set_region_info_req";
    String GET_GW_LIST_REQ               = "get_gw_list_req";//无消息体
    String GET_GW_LIST_RSP               = "get_gw_list_rsp";
}
