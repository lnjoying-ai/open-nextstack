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

package com.lnjoying.vm.common;

import java.util.concurrent.atomic.AtomicReference;

public class AgentConstant
{
    public final static String FAILED = "failed";

    public final static String ADDED = "Added";

    public final static String ADD_FAILED = "AddFailed";

    public final static String UPDATED = "Updated";

    public final static String UPDATE_FAILED = "UpdateFailed";

    public final static String PENDING_STATUS = "pending";

    public final static String NFS_NOT_EXIST = "The nfs does not exist.";

    public final static String DEPLOYING_STATUS = "Deploying";

    public final static String CLOUD_INITED_STATUS = "CloudInited";

    public final static String INSPECT_FAILED = "InspectFailed";

    public final static String INSPECT_BOOT_FAILED = "InspectBootFailed";

    public final static String INSPECTED = "Inspected";

    public final static String DEPLOY_BOOT_FAILED = "DeployBootFailed";

    public final static String DEPLOY_IMAGE_FAILED = "DeployImageFailed";

    public final static String DEPLOY_POWEROFF_FAILED = "DeployPowerOffFailed";

    public final static String INJECT_BOOTING = "InjectBooting";

    public final static String INJECTED = "Injected";

    public final static String INJECT_BOOT_FAILED = "InjectBootFailed";

    public final static String EJECT_FAILED = "EjectFailed";

    public final static String DEPLOY_POWEROFF = "DeployPoweroff";

    public final static String CLOUD_INIT_DONE = "CloudInitDone";

    public final static String POWER_OFF = "off";

    public final static String SHUT_OFF = "shut off";

    public final static String RUNNING = "running";

    public final static String POWER_ON = "on";

    public final static String POWER_PENDING = "pending";

    public final static String DESTROYING = "Destroying";

    public final static String DESTROY_BOOTING = "DestroyBooting";

    public final static String DESTROY_BOOTED = "DestroyBooted";

    public final static String DESTROY_IMAGE_DONE = "DestroyImageDone";

    public final static String GET_STATUS_FAILED = "failed";

    public final static String DELETE_FAILED = "DeleteFailed";

    public final static String SWITCH_FAILED = "SwitchFailed";

    public final static String SWITCHED = "Switched";

    public final static String SWITCHING = "Switching";

    public final static String APPLIED = "Applied";

    public final static String UNAPPLIED = "Unapplied";

    public final static String APPLY_FAILED = "ApplyFailed";

    public final static String UNAPPLY_FAILED = "UnapplyFailed";

    public final static String APPLYING = "Applying";

    public final static String UNAPPLYING = "Unapplying";

    public final static String SUSPENDED = "Suspended";

    public final static String SUSPENDING = "Suspending";

    public final static String SUSPEND_FAILED = "SuspendFailed";

    public final static String RESUMING = "Resuming";

    public final static String RESUME = "resume";

    public final static String RESUMED = "Resumed";

    public final static String RESUME_FAILED = "ResumeFailed";

    public final static String UP = "up";

    public final static String DOWN = "down";

    public final static String SET = "Set";

    public final static String SET_FAILED = "SetFailed";

    public final static int DISK_MAX_SIZE = 102400;

    public final static int MAX_DATA_DISK_NUM = 4;

    public final static int MONITOR_TAG_OK = 9000;

    public final static int MONITOR_TAG_ERR = 9100;

    public final static String OK = "ok";

    public final static String NOT_EXIST = "not found";

    public final static String SUCCESS = "success";

    public final static String FAIL = "fail";

    public final static String ADD = "add";

    public final static String DEL = "del";

    public final static String ATTACHING = "Attaching";

    public final static String ATTACHED = "Attached";

    public final static String ATTACH_FAILED = "AttachFailed";

    public final static String DETACHED = "Detached";

    public final static String DETACH_FAILED = "DetachFailed";

    public final static String DETACHING = "Detaching";

    public final static String GPU_NOT_ATTACHED = "GPU is not in use.";

    public final static String GPU_ATTACHED = "The gpu has already been attached.";

    public final static String VM_NOT_EXIST = "Virtual machine not found.";

    public final static String VM_DELETING = "The vm is being deleted.";

    public final static String ADD_ACTION = "Add";

    public final static String REMOVE_ACTION = "Remove";

    public final static String HYPERVISOR_NODE_EXPORTER_PORT = ":9100";

    public final static String SHUT = "shut off";

    public final static String AUDIO = "Audio";

    public final static int NFS_PORT = 4;

    public static AtomicReference<String> L3Ip = new AtomicReference<>();
}
