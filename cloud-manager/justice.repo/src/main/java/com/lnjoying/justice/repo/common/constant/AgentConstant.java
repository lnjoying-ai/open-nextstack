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

package com.lnjoying.justice.repo.common.constant;

public class AgentConstant
{
    public static final String EXPECTED = "Added";

    public final static String PENDING = "pending";

    public final static String FAILED = "failed";

    public final static String UPDATED = "Updated";

    public final static String UPDATE_FAILED = "UpdateFailed";

    public final static String ADDED = "Added";

    public final static String SUCCESS = "success";

    public final static String FAIL = "fail";

    public final static String ADD = "add";

    public final static String DEL = "del";

    public final static String REMOVED = "Removed";

    public final static String ADD_FAILED = "AddFailed";

    public final static String DELETE_FAILED = "DeleteFailed";

    public final static String DEFAULT = "default";

    public final static String ALREADY_EXISTS = "already exists";

    public final static String POOL_ALREADY_EXISTS = "pool dir already exists in the agent";

    public final static String VOLUME_NOT_EXIST = "Volume not found.";

    public final static String VOLUME_NOT_ATTACHED = "Volume is not in use.";

    public final static String VOLUME_ALREADY_ATTACHED = "Volume is in use.";

    public final static String POOL_NOT_EXIST = "Pool not found.";

    public final static String IMG_NOT_EXIST = "Img does not found.";

    public final static String SNAP_NOT_EXIST = "Snapshot not found.";

    public final static String NOT_EXIST = "not found";

    public final static String ATTACHED = "Attached";

    public final static String ATTACH_FAILED ="AttachFailed";

    public final static String ATTACHING = "Attaching";

    public final static String DETACHING = "Detaching";

    public final static String DETACHED = "Detached";

    public final static String DETACH_FAILED = "DetachFailed";

    public final static String NOT_READY = "not ready";

    public final static String OK = "ok";

    public final static String SUSPENDED = "Suspended";

    public final static String SUSPEND_FAILED = "SuspendFailed";

    public final static String RESUMED = "Resumed";

    public final static String RESUME_FAILED = "ResumeFailed";

    public final static String SWITCHED = "Switched";

    public final static String SWITCH = "switch";

    public final static String SWITCH_FAILED = "SwitchFailed";

    public final static String EXPORTED = "Exported";

    public final static String EXPORT_FAILED = "ExportFailed";

    public final static String VOLUME_ATTACHING = "The vol is attaching.";

    public final static int MAX_DATA_DISK_NUM = 4;

    public final static int DISK_MAX_SIZE = 102400;

    public final static int maxVcpu = 256;

    public final static int maxVmMemory = 2048;

    public final static int maxRootDisk = 200;
}
