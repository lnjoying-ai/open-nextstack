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

public class VmInstanceStatus
{
    public final static int VM = 0;

    public final static int INSTANCE_INIT = 0;

    public final static int INSTANCE_CREATING = 1;

    public final static int INSTANCE_CREATED = 2;

    public final static int INSTANCE_INJECTING = 3;

    public final static int INSTANCE_INJECT_BOOTING = 4;

    public final static int INSTANCE_CLOUDINIT_DONE = 5;

    public final static int INSTANCE_RUNNING = 6;

    public final static int INSTANCE_POWEROFFING = 7;

    public final static int INSTANCE_POWEROFF = 8;

    public final static int INSTANCE_POWERONING = 9;

    public final static int INSTANCE_MONITOR_TAG_DONE = 10;

    public final static int INSTANCE_REBOOT_POWEROFFING = 60;

    //关机卸载GPU的初始状态,
    public final static int INSTANCE_POWERING_OFF_DETACH_PCI = 61;
    public final static int GET_INSTANCE_POWERING_OFF_DETACH_PCI_STATUS = 62;
    public final static int INSTANCE_POWERED_OFF_DETACH_PCI = 63;
    public final static int INSTANCE_POWERING_ON_PREPARE_PCI = 64;
    public final static int INSTANCE_POWERING_ON_ATTACH_PCI = 65;

    public final static int INSTANCE_POWER_ON_FAILED = 66;


    //migrate
    public final static int INSTANCE_MIGRATE_INIT = 300;

    public final static int INSTANCE_SUSPENDING = 301;

    public final static int INSTANCE_SUSPENDED = 302;

    public final static int GET_PORT_MIGRATED_STATUS = 303;

    public final static int GET_INSTANCE_RESUME_STATUS = 304;

    public final static int INSTANCE_RESUMED = 305;

    //resize
    public final static int INSTANCE_RESIZE_INIT = 350;

    public final static int GET_INSTANCE_UPDATED_STATUS = 351;

    public final static int INSTANCE_BOOT_DEV_SWITCHING = 352;

    public final static int GET_INSTANCE_BOOT_DEV_STATUS = 353;

    public final static int INSTANCE_RESET_PASSWORD_HOSTNAME = 360;

    public final static int WAIT_INSTANCE_RESET_PASSWORD_HOSTNAME = 361;

//    public final static int INSTANCE_MIGRATE_DONE = 306;

    //check
    public final static int GET_PORT_PHASE_STATUS = 11;

    public final static int GET_INSTANCE_CREATED_STATUS = 12;

    public final static int WAIT_INSTANCE_CLOUD_INIT_RESULT = 13;

    public final static int GET_INSTANCE_REMOVED_STATUS = 14;

    public final static int GET_SNAP_REMOVED_STATUS = 15;

    public final static int GET_INSTANCE_POWERON_RESULT = 16;

    public final static int GET_INSTANCE_POWEROFF_RESULT = 17;

    public final static int GET_SNAP_SWITCHED_STATUS = 18;

    public final static int GET_APPLY_SG_RESULT = 19;

    // failed
    public final static int INSTANCE_CREATE_FAILED = 21;

    public final static int INSTANCE_REMOVE_FAILED = 22;

    public final static int INSTANCE_INJECT_BOOT_FAILED = 23;

    public final static int INSTANCE_EJECT_FAILED = 24;

    public final static int SNAP_CREATE_FAILED = 25;

    public final static int SNAP_REMOVE_FAILED = 26;

    public final static int SNAP_SWITCH_FAILED = 27;

    public final static int INSTANCE_MIGRATE_FAILED = 28;

    public final static int INSTANCE_MIGRATE_CLEAN = 29;

    //
    public final static int INSTANCE_REMOVING = 101;

    public final static int INSTANCE_REMOVED_FAILED = 103;

    public final static int INSTANCE_EJECTING = 104;

    public final static int INSTANCE_EJECTED = 105;

    public final static int INSTANCE_CREATE_FAILED_CLEANING = 110;

    public final static int INSTANCE_CREATE_FAILED_CLEANED = 111;
    // snap
    public final static int SNAP_INIT = 30;
    public final static int SNAP_CREATING = 31;
    public final static int SNAP_CREATED = 32;
    public final static int SNAP_SWITCHING = 33;
    public final static int SNAP_SWITCHED = 34;
    public final static int SNAP_REMOVING = 35;


    //hypervisor node
    public final static int HYPERVISOR_NODE_CREATED = 40;
    public final static int HYPERVISOR_NODE_CHECKING = 41;
    public final static int HYPERVISOR_NODE_OFFLINE = 42;

    //security group

    public final static int SG_ADDED = 50;

    public final static int SG_UPDATED = 51;

    public final static int SG_UPDATE_FAILED = 52;

    public final static int APPLIED = 53;

    public final static int UNAPPLIED = 54;

    public final static int APPLY_FAILED = 55;

    public final static int UNAPPLY_FAILED = 56;

    public final static int APPLYING = 57;

    public final static int UNAPPLYING = 58;


    //
    public final static int PORT_CREATE = 70;

    // pci device

    public final static int DEVICE_ATTACHING = 80;

    public final static int DEVICE_AGENT_ATTACHING = 81;

    public final static int DEVICE_DETACHING = 82;

    public final static int DEVICE_AGENT_DETACHING = 83;

    public final static int DEVICE_ATTACH_FAILED = 84;

    public final static int DEVICE_DETACH_FAILED = 85;

    public final static int DEVICE_ATTACHED = 86;

    public final static int DEVICE_DETACHED = 87;

    // 新建虚机时，GPU 设备的初始状态
    public final static int DEVICE_INIT_CREATE = 88;

    public final static int GROUP_CREATED = 101;

    public final static int INSTANCE_GROUP_CHANGED = 200;

//    public final static int
}
