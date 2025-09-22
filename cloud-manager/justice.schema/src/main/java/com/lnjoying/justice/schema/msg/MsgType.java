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

public enum MsgType
{
    CREATE_TASK,
    UPDATE_TASK,
    START_TASK,
    RENDER_REQ_DEV,
    RENDERSUBTASK_REQ_DEV,
    MAP_MERGE_REQ_DEV,
    STITCH_REQ_DEV,
    REQ_NEW_DEV_FOR_OVERTIME60MIN_SUBTASK,
    RENDER_FWD,
    RENDER_FWD_CHECK,
    STOP_TASK,
    ADMIN_STOP_TASK,
    OVERDUE_STOP_TASK,
    START_SUB_TASK,
    STOP_SUB_TASK,
    ADMIN_STOP_SUB_TASK,
    DELETE_TASK,
    START_TASK_RSP,
    STOP_TASK_RSP,
    RPT_TASK_STATE,
    QUERY_TASK_STATE,
    FETCH_BROTHER_TASK,
    REPAIR_RENDER_TASK,
    RESTART_RENDER_TASK,
    CHANNEL_LOGIN_NOTIFY,          //render service recv worker login signal, then send the  task which assigned to the worker
    DELETE_RENDER_TASK,
    DELETE_RENDER_PROJECT,
    SUBMIT_START_TASK,
    FILE_CP,
    FILE_DOWN,
    START_ANALYSIS,
    RPT_ANALYSIS,
    STOP_ANALYSIS,
    SUBMIT_MSG,
    DELIVER_MSG,
    DELIVER_RPT_MSG,
    TASK_BILLING_FROZEN,
    GET_SUB_TASK_ARRIVE_EDGE_STATE,
    SEND_OVER_TIME_ALERT_MSG,
    TASK_STOP_CONFIRM_TOKEN,
    BILL_CONFIRM_MSG,
    ABNORMAL_BILL_CONFIRM_MSG,
    ABNORMAL_SUB_TASK_BRIEF_CONFIRM,
    CHECK_RENDER_TASK,
    CHECK_ROOT_RENDER_TASK,
    CHECK_LTIME_UNFINISH_TASK,
    ////////////////
    RM_IPFS_BLOCK,
    ////////////////
    CREATE_CONTAINER,
    START_CONTAINER,
    STOP_CONTAINER,
    DELETE_CONTAINER,
    RESTART_CONTAINER,
    REMOTE_EXECUTE,
    ///////////////////
    CREATE_EC_REQ;

}
