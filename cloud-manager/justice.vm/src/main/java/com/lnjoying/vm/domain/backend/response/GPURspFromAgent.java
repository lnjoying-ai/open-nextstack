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

package com.lnjoying.vm.domain.backend.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties
public class GPURspFromAgent
{
//    {"attach_phase":"Attaching","group":"1",
//    "name":"NVIDIA Corporation GA104 [GeForce RTX 3070] ",
//    "pci":"0000:01:00.0","phase":"Added","status":"ok",
//    "type":"VGA","uuid":"87ae539d-8c1e-459e-b5ff-745533588649",
//    "vm_uuid":"56e33661-8c8b-4517-bb2c-caeba78a99f7"}

    private String status;

    @JsonProperty("phase_status")
    private String phase;

    @JsonProperty("phase_type")
    private String phaseType;

    @JsonProperty("group")
    private String deviceGroup;

    @JsonProperty("device_name")
    private String gpuName;

    @JsonProperty("partition_id")
    private String partitionId;

    @JsonProperty("type")
    private String deviceType;

//    @JsonProperty("attach_phase")
//    private String attachPhase;
//
//    @JsonProperty("detach_phase")
//    private String detachPhase;

    @JsonProperty("export_phase")
    private String exportPhase;

    @JsonProperty("uuid")
    private String gpuId;

    @JsonProperty("vm")
    private String vmId;

    private String reason;
}
