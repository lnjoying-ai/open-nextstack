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

package com.lnjoying.justice.operation.utils;

import com.lnjoying.justice.commonweb.exception.WebSystemException;
import com.lnjoying.justice.schema.common.ErrorCode;
import com.lnjoying.justice.schema.common.ErrorLevel;
import com.lnjoying.justice.operation.common.constant.AlarmElementType;
import com.lnjoying.justice.operation.common.constant.ResourceType;
import com.lnjoying.justice.operation.common.constant.TemplateType;
import lombok.Data;

import java.util.List;

import static com.lnjoying.justice.operation.common.constant.PrometheusConfig.RULE_FILE_PATH;

public class TsQueryParameters
{
    public static String cpuUsage(List<String> resourceIds, Integer resourceType, String threshold)
    {
        String resource = getResource(resourceIds, resourceType);
        if (ResourceType.HYPERVISOR_NODE == resourceType )
        {
            return String.format("round(100-(sum(increase(node_cpu_seconds_total{mode='idle', %s}[5m]))by(instance)) /" +
                    " (sum(increase(node_cpu_seconds_total[5m]))by(instance))  *100 %s,0.01)", resource,  threshold);
        }
        return String.format("round(rate(libvirt_domain_info_cpu_time_seconds_total{%s}[10m])/" +
                "(libvirt_domain_info_virtual_cpus{%s})*100 %s, 0.01)",resource, resource, threshold);
    }

    public static String memUsage(List<String> resourceIds, Integer resourceType, String threshold)
    {
        String resource = getResource(resourceIds, resourceType);
        if (ResourceType.HYPERVISOR_NODE == resourceType )
        {
            return String.format("round((1-(node_memory_MemAvailable_bytes{%s}/node_memory_MemTotal_bytes{%s}))*100 %s, 0.01)",resource, resource, threshold);
        }
        return String.format("round((1-libvirt_domain_stat_memory_usable_bytes{%s}/libvirt_domain_stat_memory_available_bytes{%s})*100 %s, 0.01)"
            ,resource, resource, threshold);
    }

    public static String filesystemUsage(List<String> resourceIds, Integer resourceType, String threshold)
    {
        String resource = getResource(resourceIds, resourceType);
        if (ResourceType.HYPERVISOR_NODE == resourceType )
        {
            return String.format("round(100.0 - 100 * (node_filesystem_avail_bytes{%s,device !~'tmpfs',device!~'by-uuid'} " +
                    "/ node_filesystem_size_bytes{%s,device !~'tmpfs',device!~'by-uuid'}) %s, 0.01)",resource,resource,threshold);
        }
        return String.format("round(ceil(libvirt_domain_filesystem_stats_used_percent{%s}*100) %s, 0.01)",resource, threshold);
    }

    public static String networkThroughput(List<String> resourceIds, Integer resourceType, String threshold)
    {
        String resource = getResource(resourceIds, resourceType);
        if (ResourceType.HYPERVISOR_NODE == resourceType )
        {
            return String.format("round((irate(node_network_receive_bytes_total{%s, device !~ \"br.*|v.*|ovs.*|lo*|docker.*|.*o\"}[5m]) " +
                    "or irate(node_network_transmit_bytes_total{%s,device !~ \"br.*|v.*|ovs.*|lo*|docker.*|.*o\"}[5m]))/1024/1024 %s, 0.01)",resource,resource,threshold);
        }
        return String.format("round((rate(libvirt_domain_interface_stats_receive_bytes_total{%s}[10m])+" +
                "rate(libvirt_domain_interface_stats_transmit_bytes_total{%s}[10m]))/1024/1024 %s, 0.01)",resource,resource, threshold);
    }

    public static String diskIops(List<String> resourceIds, Integer resourceType, String threshold)
    {
        String resource = getResource(resourceIds, resourceType);
        if (ResourceType.HYPERVISOR_NODE == resourceType)
        {
            return String.format("round(irate(node_disk_reads_completed_total{%s, device !~ \"dm.*|nbd.*\"}[5m])+" +
                    "irate(node_disk_writes_completed_total{%s,device !~ \"dm.*|nbd.*\"}[5m]) %s, 0.01)", resource,resource, threshold);
        }
        return String.format("round(rate(libvirt_domain_block_stats_read_requests_total{%s}[10m])+" +
                        "rate(libvirt_domain_block_stats_write_requests_total{%s}[10m])%s, 0.01)", resource,resource, threshold);
    }

    public static String diskThroughput(List<String> resourceIds, Integer resourceType, String threshold)
    {
        String resource = getResource(resourceIds, resourceType);
        if (ResourceType.HYPERVISOR_NODE == resourceType)
        {
            return String.format("round((irate(node_disk_read_bytes_total{%s,device !~ \"dm.*|nbd.*\"}[5m])+" +
                    "irate(node_disk_written_bytes_total{%s,device !~ \"dm.*|nbd.*\"}[5m]))/1024/1024 %s, 0.01)", resource,resource,threshold);
        }
        return String.format("round((rate(libvirt_domain_block_stats_read_bytes_total{%s}[10m])+" +
                "rate(libvirt_domain_block_stats_write_bytes_total{%s}[10m]))/1024/1024 %s, 0.01)",resource,resource, threshold);
    }

    public static String instanceDown(List<String> resourceIds, Integer resourceType, String threshold)
    {
        String resource = getResource(resourceIds, resourceType);
        if(ResourceType.HYPERVISOR_NODE == resourceType)
        {
            return String.format("up{%s}==0", resource);
        }
        return String.format("libvirt_domain_state_code{%s}==5 or libvirt_domain_state_code{%s} ==6", resource, resource);
    }

    public static String getResource(List<String> resourceIds, Integer resourceType)
    {
        String resource ;
        String strResourceIds;
        if (resourceIds.size() == 1)
        {
            strResourceIds = resourceIds.get(0);
        }
        else if (resourceIds.size() == 0)
        {
            throw new RuntimeException("resourceIds is empty");
        }
        else
        {
            // list to string
            strResourceIds = String.join("|", resourceIds);
        }

        switch (resourceType)
        {
            case ResourceType.INSTANCE_GROUP:
            default:
                resource = "groupId=~\"" + strResourceIds+"\"";
                break;
            case ResourceType.VM_INSTANCE:
                resource = "instanceId=~\"" + strResourceIds+"\"";
                break;
            case ResourceType.HYPERVISOR_NODE:
                resource = "instance=~\"" + strResourceIds+"\"";
                break;
        }
        return resource;
    }

    @Data
    public static class RuleTemplateParameters
    {
        String expr;
        String forTime;
        String ruleId;
        String threshold;
    }

    public static void generateRuleFile(AlarmRuleParameters parameters)
    {
        RuleTemplateParameters ruleTemplateParameters = new RuleTemplateParameters();
        ruleTemplateParameters.setRuleId(parameters.getRuleId());
        ruleTemplateParameters.setForTime(parameters.getForTime());
        ruleTemplateParameters.setThreshold(String.format("\"%s\"",parameters.getThreshold()));
        String templateName;
        switch (parameters.getAlarmElementType())
        {
            case AlarmElementType.CPU_USAGE:
                String expr = cpuUsage(parameters.getResourceIds(), parameters.getResourceType(), parameters.getThreshold());
                ruleTemplateParameters.setExpr(expr);
                templateName = TemplateType.CPU_USAGE;
                if (ResourceType.HYPERVISOR_NODE == parameters.getResourceType()) templateName=String.format("%s_node",templateName);
                break;
            case AlarmElementType.MEM_USAGE:
                expr = memUsage(parameters.getResourceIds(), parameters.getResourceType(), parameters.getThreshold());
                ruleTemplateParameters.setExpr(expr);
                templateName = TemplateType.MEM_USAGE;
                if (ResourceType.HYPERVISOR_NODE == parameters.getResourceType()) templateName=String.format("%s_node",templateName);
                break;
            case AlarmElementType.FILESYSTEM_USAGE:
                expr = filesystemUsage(parameters.getResourceIds(), parameters.getResourceType(), parameters.getThreshold());
                ruleTemplateParameters.setExpr(expr);
                templateName = TemplateType.FILESYSTEM_USAGE;
                if (ResourceType.HYPERVISOR_NODE == parameters.getResourceType()) templateName=String.format("%s_node",templateName);
                break;
            case AlarmElementType.NETWORK_THROUGHPUT:
                expr = networkThroughput(parameters.getResourceIds(), parameters.getResourceType(), parameters.getThreshold());
                ruleTemplateParameters.setExpr(expr);
                templateName = TemplateType.NETWORK_THROUGHPUT;
                if (ResourceType.HYPERVISOR_NODE == parameters.getResourceType()) templateName=String.format("%s_node",templateName);
                break;
            case AlarmElementType.DISK_IOPS:
                expr = diskIops(parameters.getResourceIds(), parameters.getResourceType(), parameters.getThreshold());
                ruleTemplateParameters.setExpr(expr);
                templateName = TemplateType.DISK_IOPS;
                if (ResourceType.HYPERVISOR_NODE == parameters.getResourceType()) templateName=String.format("%s_node",templateName);
                break;
            case AlarmElementType.DISK_THROUGHPUT:
                expr = diskThroughput(parameters.getResourceIds(), parameters.getResourceType(), parameters.getThreshold());
                ruleTemplateParameters.setExpr(expr);
                templateName = TemplateType.DISK_THROUGHPUT;
                if (ResourceType.HYPERVISOR_NODE == parameters.getResourceType()) templateName=String.format("%s_node",templateName);
                break;
            case AlarmElementType.INSTANCE_HEALTH_STATUS:
                expr = instanceDown(parameters.getResourceIds(), parameters.getResourceType(), parameters.getThreshold());
                ruleTemplateParameters.setExpr(expr);
                if (ResourceType.HYPERVISOR_NODE == parameters.getResourceType()) return;
                templateName = TemplateType.INSTANCE_HEALTH_STATUS;
                break;
            default:
                throw new WebSystemException(ErrorCode.SystemError, ErrorLevel.INFO);
//                throw new RuntimeException("alarmElementType is not supported :" + parameters.getAlarmElementType());
        }
        String ruleFilePath = RULE_FILE_PATH +"/"+parameters.getRuleId()+".yml";
        TemplateUtils.generateYmlFile(templateName, ruleTemplateParameters, ruleFilePath);
    }
}
