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

package com.lnjoying.justice.network.common;

public class AgentConstant
{
    public static final String SUBNETS = "subnets";

    public static final String SUBNET = "subnet";

    public static final String VPC = "vpc";

    public static final String EXPECTED = "Added";

    public static final String PORTS = "ports";

    public final static String PENDING = "pending";

    public final static String OK = "ok";

    public final static String SUCCESS = "success";

    public final static String FAIL = "fail";

    public final static String FAILED = "failed";

    public final static String APPLIED = "Applied";

    public final static String UNAPPLIED = "Unapplied";

    public final static String APPLY_FAILED = "ApplyFailed";

    public final static String UNAPPLY_FAILED = "UnapplyFailed";

    public final static String APPLYING = "Applying";

    public final static String UNAPPLYING = "Unapplying";

    public final static String UPDATED = "Updated";

    public final static String MIGRATE_FAILED = "MigrateFailed";

    public final static String MIGRATED = "Migrated";

    public final static String UPDATE_FAILED = "UpdateFailed";

    public final static String ADDED = "Added";

    public final static String DEFAULT = "default";

    public final static String NOT_EXIST = "not exist";

    public final static String NOT_FOUND = "not found";

    public final static String NOT_READY = "not ready";

    public final static String ALL_IP = "0.0.0.0/0";

    public final static String PORT_NOT_EXIST = "The port does not exist.";

    public final static String PORT_NOT_BOUND = "not bound";

    public final static String PORT_ALREADY_BOUND ="The port is already bound to an Elastic IP.";

    public final static String SUBNET_NOT_EXIST = "The subnet does not exist.";

    public final static String VPC_NOT_EXIST = "VPC not ready.";

    public final static String SG_NOT_EXIST = "The sg does not exist.";

    public final static String FLOATING_IP_EXIST = "The floating IP exists.";

    public final static int IN = 0;

    public final static int OUT = 1;

    public final static int DROP = 0;

    public final static int ACCEPT = 1;

    public final static int MAX_PRIORITY = 100;

    public final static int MIN_PRIORITY = 1;

    public final static int IPV4 = 0;

    public final static int IPV6 = 1;

    public final static String HAPROXY_URL = "/v1/haproxies";

    public final static String FRONTEND_URL = "/v1/frontends";

    public final static String BACKEND_URL = "/v1/backends";

    public final static String PHONE_HOME_PORT = "8901";

    public final static String HAPROXY_NOT_EXIST = "The haproxy does not exist.";

    public final static String BACKEND_NOT_EXIST = "The backend does not exist.";

    public final static String FRONTEND_NOT_EXIST = "The frontend does not exist.";

    public final static String ADD_FAILED = "AddFailed";

    public final static String DELETE_FAILED = "DeleteFailed";

    // EIP绑定的方式
    public final static String PORT_BOUND = "port";

    public final static String NAT_BOUND = "nat";

    public final static String UNBOUND = "unbound";

    public final static String BOUND = "bound";

}
