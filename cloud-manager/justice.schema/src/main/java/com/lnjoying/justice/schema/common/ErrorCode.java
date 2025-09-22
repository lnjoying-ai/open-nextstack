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

package com.lnjoying.justice.schema.common;

public enum ErrorCode
{
    /**********
     * COMMON *
     **********/
    SUCCESS(0, "Success"),
    /** Bad username */
    InvalidMetadataFieldName(10, "Metadata field name contains invalid characters."),

    /*********
     * USERS *
     *********/

    /** Bad username */
    InvalidUsername(1111, "Username invalid."),

    /** Bad password */
    InvalidPassword(1112, "Password did not match."),

    /** Username already used */
    DuplicateUser(1113, "the info for user already in use."),

    /** Invalid authority reference */
    User_Params_Error(1114, "user params error"),
    UpdateUserError(1115, "failed to update user"),
    UNCHANGEDEmail(1116, "the input email is not changed"),
    EmailOccupied(1117, "the email is occupied by other"),
    DuplicatePhone(1118, "the input cell phone num is not changed"),
    PhoneOccupied(1119, "the cell phone num is occupied by other"),
    InvalidOldPasswd(1120, "invalid old password"),
    Invalid_validateCode(1121, "invalid validate code"),
    User_Not_Exist(1122, "user is not exist"),
    VER_Params_Error(1123, "verification params error"),
    User_Not_Grant(1124, "the user is not granted for the obj"),
    KeyOccupied(1125, "the key is occupied by other"),
    NewPassowrdError(1126, "the new password is invalid."),
    /** Authority name already used */
    DuplicateAuthority(1127, "Authority name already in use."),
    InvalidAuthority(1128, "Authority failed"),
    InvalidReq(1129, "url not exist, invalid request"),
    No_Permission(1140, "no permission to access resources"),

    /** No user logged in for action that requires authorization */
    NotLoggedIn(1130, "You must provide credentials to perform this action."),
    CreateUserError(1131, "failed to create user "),
    User_OVER_DUE(1132, "user overdue"),
    RM_User_ERROR(1133, "remove user error."),
    USER_NOT_ACTIVE(1134, "user have not been active"),

    BP_NOT_EXIST(1151, "BP not exist"),
    BP_KEY_Occupied(1152,"create bp error, bp param have been occupied"),
    BP_NAME_INVALID(1153, "bp name is invalid"),
    BP_LIC_INVALID(1154, "bp license is invalid"),

    /****************
     * ACTION
     ***************/
    BAD_REQUST(1200, "bad request"),
    ACTION_NOT_SUPPORT(1201, "action is not supported"),


    /*********
     * Project*
     *********/
    Project_Not_Exist(1260, "Project not exist"),
    Project_Parmas_Error(1261, "project params error"),

    /*********
     * Dev*
     *********/
    DEV_NOT_EXIST(1360, "dev not exist"),
    DEV_LOGIN_RECORD_ERROR(1361, "dev can not login"),
    DEV_NOT_REG(1362, "dev have not been reg"),
    DEV_HAVE_NOT_CHECKPASS(1363, "dev have not been check passed"),
    DEV_ABNORMAL(1364, "dev abnormal"),
    DEV_REG_TOKEN_EMPTY(1365, "dev reg token is empty"),
    DEV_NODEID_OCCUPIED(1366, "nodeId have been occupied"),
    DEV_REG_TOKEN_INVALID(1367, "dev reg token is invalid"),
    DEV_DROPPED(1368, "dev have been dropped"),


    /*********
     * sms*
     *********/
    SMSError(1501, "failed to send sms"),
    EMAILError(1502, "failed to send sms"),


    /**Region**/
    REGION_REMOVED(1551, "region has been removed"),
    REGION_NOT_EXIST(1552, "region not exist"),
    REGION_EMPTY(1553, "region is empty"),


    /***
     * Site Error
     */
    SITE_NOT_EXIST(1651, "site not exist"),
    SITE_REMOVED(1652, "site has been removed"),


    /**
     * CIS Error
     */
    INST_DROPPED(1701, "inst has been dropped"),

    /**
     * Aos
     */
    TEMPLATE_NOT_EXIST(1901, "template not exist"),
    TEMPLATE_DROPPED(1902,  "template have been dropped"),
    TEMPLATE_DUP(1903,  "template name:version duplicated"),
    STACK_NOT_EXIST(1951, "stack  not exist"),
    STACK_DROPPED(1952, "stack have been dropped"),
    STACK_DUP(1953,  "stack name duplicated"),

    STACK_SERVICE_NOT_EXIST(2001, "stack service not exist"),
    STACK_SERVICE_DROPPED(2002, "stack service have been dropped"),

    /**
     * ims
     */
    REGISTRY_EXIST(2200, "registry exist"),
    REGISTRY_DUP(2201, "registry  duplicated"),
    REGISTRY_NOT_EXIST(2202, "registry  not exist"),
    REGISTRY_DROPPED(2203, "registry has been dropped"),
    REGISTRY_DISABLE(2204, "registry disabled"),
    REGISTRY_UNAVAILABLE(2205, "registry unavailable"),
    NO_AVAILABLE_REGISTRY(2206, "There is no registry available"),
    REGION_ID_ERROR(2250, "wrong region id"),
    REGISTRY_REGION_INSERT_FAILED(2251, "create registry region fail"),
    REGISTRY_PASSWORD_ERROR(2252, "incorrect password"),
    MODIFY_REGISTRY_PASSWORD_FAILED(2253, "failed to modify registry password"),
    REGISTRY_USER_EXIST(2254, "registry user already exists"),
    REGISTRY_USER_NOT_EXIST(2255, "registry user does not exist"),
    REGISTRY_USER_DROPPED(2255, "registry user has been dropped"),
    ADD_REGISTRY_USER_ERROR(2256, "add registry user fail"),
    REGISTRY_PROJECT_EXIST(2257, "registry project already exists"),
    FAILED_TO_ASSOCIATE_PROJECT_AND_USER(2258, "failed to associate project and user"),
    THE_USER_HAS_CREATED_A_REGISTRY_USER(2259, "Each user can only create one registry user, and the current user has already created a registry user"),
    FAILED_TO_CREATE_REGISTRY_PROJECT(2260, "failed to create registry project"),
    THE_PROJECT_ALREADY_EXISTS_IN_THE_REGISTRY(2261, "The project already exists in the registry"),
    REGISTRY_PROJECT_NOT_EXIST(2262, "registry project does not exist"),
    REGISTRY_PROJECT_DROPPED(2264, "registry project has been dropped"),
    REGISTRY_REPO_NOT_EXIST(2265, "registry repo does not exist"),
    MODIFY_REGISTRY_REPO_FAILED(2266, "failed to modify registry repo"),
    REGISTRY_REQUEST_HTTP_CLIENT_ERROR(2267, "registry request http client error"),
    REGISTRY_REPO_TAG_NOT_EXIST(2268, "registry repo tag does not exist"),
    REGISTRY_3RD_DUP(2269, "third party registry duplicated"),
    REGISTRY_3RD_NOT_EXIST(2270, "third party registry  not exist"),
    REGISTRY_3RD_EXIST(2271, "same name registry exist"),
    REGISTRY_3RD_DROPPED(2272, "third party registry has been dropped"),
    FAILED_TO_UPDATE_PRE_DOWNLOAD(2273, "failed to update pre download record"),
    REGION_REGISTRY_NOT_FOUND(2274, "region registry not found"),
    GET_REPO_LIST_ERROR(2275, "get repo list error"),
    ONLY_ONE_REGISTRY_USER(2276, "A user can only have one registry user"),
    UPDATE_REGISTRY_USER_PASSWORD_FAIL(2256, "update registry user password fail"),
    USER_ID_NOT_FOUND(2270, "User authentication is invalid"),
    BP_ID_NOT_FOUND(2271, "User authentication is invalid"),
    MSG_PROCESSING_ERROR(2280, "msg processing failed"),
    TASK_EXECUTE_EXCEPTION(2290, "task execute exception"),
    AUTHORITIES_NOT_FOUND(2278, "Authorities is invalid"),
    USER_NAME_NOT_FOUND(2279, "User name is invalid"),
    BP_NAME_NOT_FOUND(2277,"BP name is invalid"),

    /**
     * compute
     */
    OPERATION_RETRY(2905, "please retry"),
    VM_INSTANCE_NOT_ALLOW_UPDATE(2904, "vm instance cannot be modified"),
    VM_STATUS_ERROR(2903, "vm status error"),
    VM_PCI_DEVICE_POWER_OFF(2911, "vm with pci device should be power off"),
    PCI_DEVICE_NOT_DETACHED(2900, "pci device is not detached"),
    PCI_DEVICE_NOT_ATTACHED(2901, "pci device is not attached"),
    PCI_DEVICE_GROUP_ALREADY_ATTACHED(2902, "pci device group already attached by the other vm"),
    NODE_RESOURCE_NOT_ENOUGH(2906, "node resource not enough"),
    FLAVOR_ROOT_DISK_SIZE_NOT_SAME(2909, "the size of root disk is not same with the flavor"),
    FLAVOR_GPU_NAME_NOT_SAME(2910, "the name of gpu is not same with the flavor"),

    PCI_DEVICE_GROUP_NOT_EXIST(2208, "pci device group does not exists"),

    PCI_DEVICE_NOT_EXIST(2209, "pci device does not exists"),

    FLAVOR_IS_USING(2210, "falvor is using"),

    VM_RUNNING_ON_HYPERVISOR_NODE(2211,"vms are running on the node"),

    BAREMETAL_DEVICE_NOT_EXIST(2200, "baremetal device does not exists"),

    BAREMETAL_INSTANCE_EXISTED(2220, "baremetal instance exists"),

    BAREMETAL_INSTANCE_NOT_EXIST(2221, "baremetal instance does not exists"),

    BAREMETAL_INSTANCE_NETWORK_ERROR(2222, "baremetal instance get network info error"),

    PUBKEY_NOT_EXIST(2223, "pubkey does not exists"),

    BAREMETAL_INSTANCE_IS_RUNNING(2224,"baremtal instance is running with the baremetal_device_id"),

    FLAVOR_NOT_EXIST(2225, "flavor does not exists"),

    VM_INSTANCE_NOT_EXIST(2226, "vm instance does not exists"),

    VM_SNAP_NOT_EXIST(2227, "vm snap does not exists"),

    HYPERVISOR_NODE_EXIST( 2228, "hypervisor node does not exists"),


    VM_HAS_SNAPS(2229, "remove the snaps first"),
    VM_SNAP_SWITCHING(2230, "vm snap is switching,please wait"),

    INSTANCE_GROUP_NOT_EMPTY(2231, "instance group is not empty"),
    VM_INSTANCE_HAS_GROUP(2232, "the groupId of the vm instance is not null"),
    INSTANCE_GROUP_ID_NOT_CORRECT(2233, "the groupId of the vm instance is not correct"),
    INSTANCE_GROUP_NOT_EXIST(2234, "instance group does not exist"),
    INSTANCE_GROUP_CREATE_FAILED(2235, "instance group create failed"),

    VM_INSTANCE_IS_MIGRATING(2241, "vm instance is migrating,please wait"),
    VM_SNAP_IS_UPDATING(2242, "vm snap is updating,please wait"),
    VOLUME_IS_UPDATING(2243, "volume is updating,please wait"),



    NFS_NOT_EXIST(3000, "nfs does not exist"),
    /**
     * network
     */
    BACKEND_NOT_EXIST(2950,"backend not exist"),
    FRONTEND_NOT_EXIST(2951,"frontend not exist"),
    LOADBALANCER_NOT_EXIST(2952,"loadbalancer not exist"),
    VPC_NOT_EXIST(2300, "vpc not exist"),
    SUBNET_NOT_EXIST(2301, "subnet not exist"),
    AGENT_SERVICE_ERR(2302, "backend service error"),
    VPC_HAS_SUBNETS(2303,"remove the subnets first"),
    CIDR_OVERLAP(2320, "cidr overlap"),
    NETWORK_ADDRESS_ERROR(2321, "network address error"),
    PORT_HAS_EIP(2322, "detach the eip first"),
    PORT_EIP_NOT_EXIST(2323, "eip is not attached"),
    PORT_NOT_CREATED(2324, "port is not created"),
    SUBNET_HAS_PORTS(2304,"remove the instances first"),
    UPDATE_DATABASE_ERR(2305,"update database error"),
    EIP_NOT_EXISTS(2306,"eip does not exists"),
    EIP_MAP_NOT_EXISTS(2307, "eipMap does not exists"),
    PORT_MAP_NOT_EXISTS(2308, "portMap does not exists"),
    EIP_NOT_ENOUGH(2309, "eip not enough"),
    EIP_MAP_ALREADY_EXISTS(2310, "eipMap already exists"),
    SUBNET_CIDR_EXISTS(2311, "cidr already exists"),
    SECURITY_GROUP_NOT_EXISTS(2312, "security group does not exists"),
    SECURITY_GROUP_HAS_RULES(2313, "remove the rules first"),
    SECURITY_GROUP_RULE_NOT_EXISTS(2314, "security group rule does not exists"),
    DEFAULT_SECURITY_GROUP_NOT_REMOVE(2315,"default security group can't remove"),
    DEFAULT_SECURITY_GROUP_NOT_UPDATE(2316, "default security group can't update"),
    INSTANCE_USED_BY_NAT(2317, "remove the nat gateway first"),
//    INSTANCE_USED_BY_EIP(2406, "detach the eip first"),
    EIP_CHECK_ERR(2407, "nat gateway check error"),
    EIP_MAP_IS_UNMAPPING(2408, "nat gateway is removing"),
    EIP_ALREADY_ATTACHED(2409, "eip already attached"),
    // 虚机已经绑定了该安全组
    SG_INSTANCE_EXISTS(2411, "the instance has been bound to the security group"),
    SECURITY_GROUP_USED_BY_RULE(2318,"security group is used by other rules"),
    SECURITY_GROUP_IS_BOUND(2410,"security group is bound"),
    EIP_POOL_NOT_EXISTS(2319, "eip pool does not exists"),
    EIP_POOL_VPC_RELATION_NOT_EXISTS(2400, "the relationship between the eip pool and the vpc does not exists" ),
    EIP_POOL_VPC_RELATION_ALREADY_EXISTS(2401, "the relationship between the eip pool and the vpc already exists"),
    EIP_POOL_HAS_EIPS(2402, "remove the eips first"),
    OBJECT_IS_DELETING(2403, "object is deleting,can't update it"),
    STATIC_IP_INVALID(2404, "static ip is invalid"),
    STATIC_IP_IS_OCCUPIED(2405, "ip address is occupied"),

    /**
     * image
     */
    IMAGE_NOT_EXIST(2500, "image does not exists"),
    IMAGE_IS_USING(2501,"image is using"),
    SHARE_NOT_EXIST(2502, "share does not exists"),
    IMAGE_SYSTEM_DEFAULT(2503, "default image,cannot be deleted"),
    IMAGE_NAME_EXIST(2504, "image name already exists"),

    /**
     * volume
     */
    VOLUME_NOT_EXIST(2700, "volume does not exists"),
    VOLUME_IS_USING(2701, "volume is using"),
    VOLUME_SNAP_NOT_EXIST(2702,"volume snap does not exists"),
    VOLUME_NOT_DETACHED(2703, "volume is not detached"),
    VOLUME_NOT_ATTACHED(2704, "volume is not attached"),
    VOLUME_NEVER_USED(2721, "volume has never been used before"),
    INSTANCE_SHOULD_BE_POWER_OFF(2705, "vm instance should be shut down"),
    INSTANCE_NOT_POWER_ON(2907,"vm instance is not powered on"),
    INSTANCE_NOT_POWER_OFF(2908,"vm instance is not powered off"),
    VOLUME_CANNOT_DETACHED_OR_ATTACHED(2706, "vm instance has vmSnaps,can't attach or detach more volumes"),
    STORAGE_POOL_NOT_EXIST(2720, "storage pool does not exists"),
    VOLUME_HAS_SNAPS(2707,"remove the volumeSnaps first"),
    VOLUME_ALREADY_ATTACHED(2708, "volume is already attached"),
    VOLUME_COUNT_EXCEED(2709, "volume count exceed"),
    VOLUME_SIZE_TOO_LARGE(2710, "volume size too large"),
    VOLUME_ATTACHED_TOO_MANY(2711, "volume attached too many"),
    VOLUME_IS_EXPORTING(2712, "volume is exporting,please wait"),
    /*********
     * plugin*
     *********/
    NO_PLUGIN(2600, "no plugin for the software"),
    PLUGIN_PARAM_ERROR(2601,"plugin param error"),

    /**
     * operation
     */
    RULE_NOT_EXIST(2800, "rule does not exists"),
    RECEIVER_NOT_EXIST(2801, "receiver does not exists"),
    RULE_RECEIVER_IS_USING(2802, "rule receiver is using"),
    CURRENT_ALARM_IS_PRESENT(2803, "current alarm is present"),
    HYPERVISOR_NODE_TO_ADMIN(2804, "Please set up monitoring for node  through the administrator."),


    /****content ****/
    CONTENT_NOT_EXIST(3300, "content not exist"),
    CONTENT_UNKOWN_ERROR(3301, "get content list error"),
    CONTENT_UNSUPPORT_ERROR(3302, "unsupport content type"),

    /******count error*************/
    STATISTIC_ERRROR(4800, "statistic error"),


    /*********
     * OTHER *
     *********/
    PARAM_ERROR(9000, "param error."),
    /** No user logged in for action that requires authorization */
    SystemError(9999, "System error."),
    /** Error with no explanation */
    Unknown(9998, "Unknown error."),
    Redirect(9997,"redirect"),
    UNKNOW_SERVICE(9996, "Unknown service."),
    SQL_ERROR(9100, "sql exception"),
    ILLEGAL_ARGUMENT(9200, "illegal argument"),
    PARAMETER_IS_NOT_VALID_FOR_OPERATION(9201, "Parameter is not valid for operation"),
    ENCODE_FAILED(9300, "encode failed");


    /** Numeric code */
    private int code;

    /** Error message */
    private String message;

    private ErrorCode(int code, String message) {
        this.setCode(code);
        this.setMessage(message);
    }

    /**
     * Look up the enum based on error code.
     *
     * @param code
     * @return
     */
    public static ErrorCode fromCode(int code) {
        for (ErrorCode current : ErrorCode.values()) {
            if (current.getCode() == code) {
                return current;
            }
        }
        throw new RuntimeException("Invalid error code: " + code);
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setErrorCode(ErrorCode errorCode)
    {
        this.code = errorCode.getCode();
        this.message = errorCode.getMessage();
    }
}
