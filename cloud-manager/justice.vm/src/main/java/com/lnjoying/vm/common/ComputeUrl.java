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

public class ComputeUrl
{
    public final static String V1_BAREMETAL_URL = "/v1/baremetals";

    public final static String V1_VM_URL = "/v1/vms";

    public final static String V1_SNAP_URL = "/v1/cps";

    public final static String V1_PORTS_URL = "/v1/ports";

    public final static String V1_NFS_URL = "/v1/nfses";

    public final static String V1_AGENT_URL = "/v1/agents";

    public final static int VM_AGENT_PORT = 8899;

    public final static int VM_PHONE_HOME_PORT = 8901;

    public final static int BAREMETAL_PHONE_HOME_PORT = 8900;

    public final static Integer L3AGENT_PORT = 8899;

    public final static String V1_SG_URL = "/v1/sgs";

//    public final static int LIBVERT_EXPORTER_PORT = 9000;

    public final static String LIBVERT_EXPORTER_TAG_PORT_URL = ":9000/metadatas";

    public final static String GPU_EXPORTER_TAG_PORT_URL = ":9835/metadatas";

    public final static String V1_GPU_URL = "/v1/gpus";

    public final static String V1_CONFIG_URL = "/v1/config";

    public final static String V1_VPC_URL = "/v1/vpcs";
}
