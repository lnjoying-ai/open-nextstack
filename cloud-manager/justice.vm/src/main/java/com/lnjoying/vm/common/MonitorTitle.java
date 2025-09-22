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

public class MonitorTitle
{
    public static final String VmDashboard = "Libvirt Dashboard";

    public static final String GPUDashboard = "Nvidia GPU Metrics";

    public static final String NodeDashboard = "Node Exporter Server Metrics";

    // 虚机状态
    public static final String VmState = "Vm State";
    // 全部虚机的CPU总核数
    public static final String TotalVCPUUsage = "Total vCPU Usage";
    //CPU核数
    public static final String CpuCount = "Cpu Count";
    //CPU 使用率
    public static final String CpuUsed = "Cpu Usage";
    // 全部虚机的内存大小
    public static final String TotalMemUsageBytes = "Total Memory Usage";
    // 内存使用率
    public static final String MemUsed = "Memory Usage";
    //单虚机的内存大小
    public static final String VmMemMaxBytes = "Memory Max Bytes";
    //文件系统使用率
    public static final String FileSystemUsed = "FileSystem Usage";
    // 硬盘吞吐率
    public static final String BlockRWBytes = "Block Read/Write Bytes";
    // 硬盘IOPS
    public static final String BlockRWRequest = "Block Read/Write Request";
    // 网卡吞吐率
    public static final String InterfaceRXTXBytes = "Interface Stats bytes";
    //网卡 PPS
    public static final String InterfacePPS = "Interface PPS";
    //网卡 Error/Drop
    public static final String InterfaceErrorDrop = "Interface Error/Drop";
    // 负载
    public static final String Load = "Load";
    //上下文切换量
    public static final String ContextSwitches = "Context Switches";
    //TCP 统计
    public static final String TcpStats = "Tcp Stats";
    //UDP 统计
    public static final String UdpStats = "Udp Stats";

    public static final String Conntrack = "Conntrack";
//
//    public static final int VM_DASHBOARD_ID = 1;
//
//    public static final int NODE_DASHBOARD_ID = 100;
//
//    public static final int vmItemMin = 10;
//
//    public static final int vmItemMax = 21;
//
//    public static final int vmStateID = 10;
//
//    public static final int totalVCPUUsageID = 11;
//
//    public static final int vmCPUCountID = 12;
//
//    public static final int vmCPUUsedID = 13;
//
//    public static final int totalMemUsageBytesID = 14;
//
//    public static final int vmMemUsedID = 15;
//
//    public static final int vmFileSystemUsedID = 16;
//
//    public static final int blockRWBytesID = 17;
//
//    public static final int blockRWRequestID = 18;
//
//    public static final int interfaceRXTXBytesID = 19;
//
//    public static final int interfacePPSID = 20;
//
//    public static final int interfaceErrorDropID = 21;
}
