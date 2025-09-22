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


import java.util.List;
import java.util.stream.Collectors;

public class TsQueryParameters
{
    public static final String API = "/api/v1/query?query=";

    public static final String USED_CPUS = "sum(libvirt_domain_info_virtual_cpus)";

    public static final String USED_MEM = "sum(libvirt_domain_info_memory_usage_bytes)";

    public static final String TOTAL_CPUS = "count(node_cpu_seconds_total{ mode=\"system\", type=\"libvirt_node\"})";

    public static final String TOTAL_MEM = "sum(node_memory_MemTotal_bytes{type=\"libvirt_node\"})";

    public static final String TOP_5_AVAILABLE_MEM = "topk(5,floor((node_memory_MemFree_bytes{type=\"libvirt_node\"}+node_memory_Buffers_bytes{type=\"libvirt_node\"}+node_memory_Cached_bytes{type=\"libvirt_node\"})/1024/1024/1024))";

    public static final String TOTAL_FILESYSTEM = "ceil(sum(node_filesystem_size_bytes{mountpoint=\"/vms\", type=\"libvirt_node\"})/1024/1024/1024)";

    public static final String UNUSED_FILESYSTEM = "ceil(sum(node_filesystem_avail_bytes{mountpoint=\"/vms\", type=\"libvirt_node\"})/1024/1024/1024)";

    public static final String VMS_AVAIL_FILESYSTEM = "floor(node_filesystem_avail_bytes{ mountpoint=\"/vms\", type=\"libvirt_node\"}/1024/1024/1024)";

    public static final String PROMETHEUS_PORT = ":9100";

    public static String getInstances(List<String> manageIps)
    {
        List<String> instances = manageIps.stream().map(manageIp -> manageIp + PROMETHEUS_PORT).collect(Collectors.toList());
        return String.join("|", instances);
    }

    public static String getAvailFileSystemSql(List<String> manageIps)
    {
        String regStr = getInstances(manageIps);
        return String.format("floor(node_filesystem_avail_bytes{ mountpoint=\"/vms\", type=\"libvirt_node\", instance =~ \"%s\"}/1024/1024/1024)", regStr);
    }

    public static String getAvailMemSql(List<String> manageIps)
    {
        String regStr = getInstances(manageIps);
        return String.format("floor((node_memory_MemFree_bytes{type=\"libvirt_node\", instance =~ \"%s\"}+node_memory_Buffers_bytes{type=\"libvirt_node\"}+node_memory_Cached_bytes{type=\"libvirt_node\"})/1024/1024/1024)", regStr);
    }

    public static String getPhysicalCpuCount(String instance)
    {
        return String.format("count(count(node_cpu_info{instance=\"%s\"}) by (package))", instance + PROMETHEUS_PORT);
    }

    public static String getCpuModel(String instance)
    {
        return String.format("node_cpu_info{core=\"0\", cpu=\"0\", instance=\"%s\"}", instance + PROMETHEUS_PORT);
    }

    public static String getLogicalCpuCount(String instance)
    {
        return String.format("count(node_cpu_seconds_total{mode=\"system\", instance=\"%s\"})", instance + PROMETHEUS_PORT);
    }

    public static String getMemoryTotalGB(String instance)
    {
        return String.format("floor(node_memory_MemTotal_bytes{instance=\"%s\"}/1024/1024/1024)", instance + PROMETHEUS_PORT);
    }
}
