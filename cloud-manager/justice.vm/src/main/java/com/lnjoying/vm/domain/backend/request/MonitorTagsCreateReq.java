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

package com.lnjoying.vm.domain.backend.request;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

@Data
public class MonitorTagsCreateReq
{
    String domainId;
    Metadata metadata;

    public MonitorTagsCreateReq(String domainId, String instanceId, String instanceName, String flavorName,
                                String userId, String groupId, String cmpTenantId, String cmpUserId)
    {
        this(domainId, instanceId, instanceName, flavorName, userId, StrUtil.isBlank(groupId) ? "" : groupId,
                "", "", "", StrUtil.isBlank(cmpTenantId) ? "" : cmpTenantId, StrUtil.isBlank(cmpUserId) ? "" : cmpUserId);
    }

    public MonitorTagsCreateReq(String domainId, String instanceId, String instanceName, String flavorName,
                                String userId, String groupId, String projectId, String userName, String projectName,
                                String cmpTenantId, String cmpUserId)
    {
        User user = new User();
        user.setUserId(userId);
        user.setUserName(userName);
        Project project = new Project();
        project.setProjectId(projectId);
        project.setProjectName(projectName);
        Owner owner = new Owner();
        owner.setProject(project);
        owner.setUser(user);

        Flavor flavor = new Flavor();
        flavor.setFlavorName(flavorName);

        Instance instance = new Instance();
        instance.setInstanceName(instanceName);
        instance.setInstanceId(instanceId);
        instance.setFlavor(flavor);
        instance.setOwner(owner);
        instance.setGroupId(groupId);
        instance.setCmpTenantId(cmpTenantId);
        instance.setCmpUserId(cmpUserId);

        Metadata metadata = new Metadata();
        metadata.setInstance(instance);

        this.metadata = metadata;
        this.domainId = domainId;
    }

    @Data
    private static class Metadata
    {
        Instance instance;
    }

    @Data
    private static class Instance
    {
        String groupId;
        String instanceId;
        String instanceName;
        Flavor flavor;
        Owner owner;
        String cmpTenantId;
        String cmpUserId;
    }

    @Data
    private static class Flavor
    {
        String flavorName;
    }

    @Data
    private static class Owner
    {
        User user;
        Project project;
    }

    @Data
    private static class User
    {
        String userId;
        String userName;
    }

    @Data
    private static class Project
    {
        String projectId;
        String projectName;
    }
}
