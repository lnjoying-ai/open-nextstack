--
-- PostgreSQL database dump
--

-- Dumped from database version 13.3 (Debian 13.3-1.pgdg100+1)
-- Dumped by pg_dump version 13.3 (Debian 13.3-1.pgdg100+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: baremetal_image; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.baremetal_image (
    device_id character varying(64) NOT NULL,
    name character varying(128),
    os character varying(64),
    core_version character varying(64),
    iscsi_target character varying(512),
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.baremetal_image OWNER TO postgres;

--
-- Name: tbl_alarm_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_alarm_info (
    info_id character varying(64) NOT NULL,
    rule_id character varying(64),
    alarm_count integer,
    summery_info character varying(200),
    detail_info text,
    user_id character varying(64),
    phase_status integer,
    create_time timestamp(6) with time zone,
    update_time timestamp(6) with time zone,
    trigger_behavior character varying(64),
    confirm_time date,
    "interval" integer,
    level integer,
    resource_type integer,
    alarm_element integer
);


ALTER TABLE public.tbl_alarm_info OWNER TO postgres;

--
-- Name: TABLE tbl_alarm_info; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.tbl_alarm_info IS '运维管理--报警';


--
-- Name: COLUMN tbl_alarm_info.info_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.info_id IS '主键';


--
-- Name: COLUMN tbl_alarm_info.rule_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.rule_id IS '报警器表--主键';


--
-- Name: COLUMN tbl_alarm_info.alarm_count; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.alarm_count IS '报警次数';


--
-- Name: COLUMN tbl_alarm_info.summery_info; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.summery_info IS '消息内容';


--
-- Name: COLUMN tbl_alarm_info.detail_info; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.detail_info IS '消息内容[全部详情]';


--
-- Name: COLUMN tbl_alarm_info.user_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.user_id IS '用户id';


--
-- Name: COLUMN tbl_alarm_info.phase_status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.phase_status IS '报警状态（0，未报警 1，已报警 2，标记为解决 3，恢复报警）';


--
-- Name: COLUMN tbl_alarm_info.create_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.create_time IS '创建时间';


--
-- Name: COLUMN tbl_alarm_info.update_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.update_time IS '修改时间';


--
-- Name: COLUMN tbl_alarm_info.trigger_behavior; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.trigger_behavior IS '触发行为';


--
-- Name: COLUMN tbl_alarm_info.confirm_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.confirm_time IS '确定时间';


--
-- Name: COLUMN tbl_alarm_info."interval"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info."interval" IS '报警间隔';


--
-- Name: COLUMN tbl_alarm_info.level; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.level IS '报警级别';


--
-- Name: COLUMN tbl_alarm_info.resource_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.resource_type IS '资源类型';


--
-- Name: COLUMN tbl_alarm_info.alarm_element; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_info.alarm_element IS '监控类型如CPU、内存、磁盘';


--
-- Name: tbl_alarm_rule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_alarm_rule (
    rule_id character varying(64) NOT NULL,
    name character varying(64),
    expr text,
    duration_time integer,
    notice boolean,
    "interval" integer,
    level integer,
    user_id character varying(64),
    phase_status integer,
    create_time timestamp(6) with time zone,
    update_time timestamp(6) with time zone,
    description character varying(256),
    comparison integer,
    alarm_value numeric,
    unit character varying(64)
);


ALTER TABLE public.tbl_alarm_rule OWNER TO postgres;

--
-- Name: TABLE tbl_alarm_rule; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.tbl_alarm_rule IS '运维管理--报警设置--报警器';


--
-- Name: COLUMN tbl_alarm_rule.rule_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.rule_id IS '主键';


--
-- Name: COLUMN tbl_alarm_rule.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.name IS '名称';


--
-- Name: COLUMN tbl_alarm_rule.expr; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.expr IS '触发规则【cpu > 80%; mem>70%】';


--
-- Name: COLUMN tbl_alarm_rule.duration_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.duration_time IS '持续时间';


--
-- Name: COLUMN tbl_alarm_rule.notice; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.notice IS '是否通知(0,不通知 1，选择已有通知 2，新建通知)';


--
-- Name: COLUMN tbl_alarm_rule."interval"; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule."interval" IS '报警间隔';


--
-- Name: COLUMN tbl_alarm_rule.level; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.level IS '报警级别';


--
-- Name: COLUMN tbl_alarm_rule.user_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.user_id IS '用户id ';


--
-- Name: COLUMN tbl_alarm_rule.phase_status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.phase_status IS '报警状态（0，未报警 1，已报警）';


--
-- Name: COLUMN tbl_alarm_rule.create_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.create_time IS '创建时间';


--
-- Name: COLUMN tbl_alarm_rule.update_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.update_time IS '修改时间';


--
-- Name: COLUMN tbl_alarm_rule.description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.description IS '描述';


--
-- Name: COLUMN tbl_alarm_rule.comparison; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.comparison IS '比较符';


--
-- Name: COLUMN tbl_alarm_rule.alarm_value; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.alarm_value IS '报警值';


--
-- Name: COLUMN tbl_alarm_rule.unit; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule.unit IS '单位';


--
-- Name: tbl_alarm_rule_receiver; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_alarm_rule_receiver (
    rule_receiver_id character varying(64) NOT NULL,
    receiver_id character varying(64),
    rule_id character varying(64),
    phase_status integer,
    create_time timestamp(6) with time zone,
    update_time timestamp(6) with time zone
);


ALTER TABLE public.tbl_alarm_rule_receiver OWNER TO postgres;

--
-- Name: tbl_alarm_rule_resource; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_alarm_rule_resource (
    alarm_rule_resource_id character varying(64),
    resource_type integer,
    resource_id character varying(64),
    phase_status integer,
    create_time timestamp(6) with time zone,
    update_time timestamp(6) with time zone,
    rule_id character varying(64),
    alarm_element integer,
    unit character varying(64)
);


ALTER TABLE public.tbl_alarm_rule_resource OWNER TO postgres;

--
-- Name: TABLE tbl_alarm_rule_resource; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.tbl_alarm_rule_resource IS '虚拟机资源表--》监控报警';


--
-- Name: COLUMN tbl_alarm_rule_resource.alarm_rule_resource_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule_resource.alarm_rule_resource_id IS '主键';


--
-- Name: COLUMN tbl_alarm_rule_resource.resource_type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule_resource.resource_type IS '资源类型';


--
-- Name: COLUMN tbl_alarm_rule_resource.resource_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule_resource.resource_id IS '监控对象';


--
-- Name: COLUMN tbl_alarm_rule_resource.phase_status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule_resource.phase_status IS '报警状态（0，未报警 1，已报警）';


--
-- Name: COLUMN tbl_alarm_rule_resource.create_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule_resource.create_time IS '创建时间';


--
-- Name: COLUMN tbl_alarm_rule_resource.update_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule_resource.update_time IS '修改时间';


--
-- Name: COLUMN tbl_alarm_rule_resource.rule_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule_resource.rule_id IS '报警器表--关联id ';


--
-- Name: COLUMN tbl_alarm_rule_resource.alarm_element; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule_resource.alarm_element IS '监控类型如CPU、内存、磁盘';


--
-- Name: COLUMN tbl_alarm_rule_resource.unit; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_alarm_rule_resource.unit IS '触发规则-->单位';


--
-- Name: tbl_backend; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_backend (
    backend_id character varying(64) NOT NULL,
    lb_id character varying(64),
    name character varying(64),
    backend_server text,
    user_id character varying(64),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    phase_status integer,
    backend_id_from_agent character varying(64),
    vpc_id character varying(64),
    balance character varying(64),
    protocol character varying(64)
);


ALTER TABLE public.tbl_backend OWNER TO postgres;

--
-- Name: TABLE tbl_backend; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.tbl_backend IS '负载均衡-后端服务组';


--
-- Name: tbl_baremetal_cluster; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_baremetal_cluster (
    cluster_id character varying(64) NOT NULL,
    description text,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.tbl_baremetal_cluster OWNER TO postgres;

--
-- Name: tbl_baremetal_device; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_baremetal_device (
    device_id character varying(64) NOT NULL,
    name character varying(64),
    phase_status integer,
    cluster_id character varying(64),
    device_spec_id character varying(64),
    user_id character varying(64),
    address_type smallint,
    ipmi_ip character varying(40),
    ipmi_port smallint,
    ipmi_username character varying(64),
    ipmi_password character varying(128),
    ipmi_mac character varying(17),
    device_id_from_agent character varying(64),
    description text,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    bmc_type character varying(64)
);


ALTER TABLE public.tbl_baremetal_device OWNER TO postgres;

--
-- Name: tbl_baremetal_device_spec; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_baremetal_device_spec (
    device_spec_id character varying(64) NOT NULL,
    name character varying(64),
    product_name character varying(128),
    serial_number character varying(64) NOT NULL,
    manufacturer character varying(256),
    architecture character varying(32),
    processor_count integer,
    cpu_logic_num integer,
    cpu_physical_num integer,
    cpu_model_name character varying(64),
    cpu_frequency double precision,
    cpu_num integer,
    mem_total bigint,
    disk_total bigint,
    disk_type character varying(20),
    disk_detail text,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.tbl_baremetal_device_spec OWNER TO postgres;

--
-- Name: tbl_baremetal_instance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_baremetal_instance (
    instance_id character varying(64) NOT NULL,
    name character varying(64),
    device_id character varying(64),
    phase_status integer,
    image_id character varying(64) NOT NULL,
    vpc_id character varying(64) NOT NULL,
    subnet_id character varying(64) NOT NULL,
    host_name character varying(128),
    sys_username character varying(64),
    sys_password character varying(128),
    pubkey_id character varying(64),
    iscsi_target character varying(256),
    iscsi_initiator character varying(256),
    iscsi_ipport character varying(256),
    share_id_from_agent character varying(64),
    nic_id_from_agent character varying(64),
    port_id_from_agent character varying(64),
    instance_id_from_agent character varying(64),
    description text,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    static_ip character varying(40)
);


ALTER TABLE public.tbl_baremetal_instance OWNER TO postgres;

--
-- Name: tbl_bill_month_container_detail_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_bill_month_container_detail_info (
    bill_id character varying(64) NOT NULL,
    inst_id character varying(64),
    count_begin_time timestamp with time zone,
    count_end_time timestamp with time zone,
    resources_type integer,
    cpu integer,
    gpu integer,
    mem bigint,
    disk bigint,
    network text,
    user_id character varying(64),
    bp_id character varying(64),
    bill_cycle integer,
    create_time timestamp with time zone
);


ALTER TABLE public.tbl_bill_month_container_detail_info OWNER TO postgres;

--
-- Name: tbl_bill_month_stack_detail_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_bill_month_stack_detail_info (
    bill_id character varying(64) NOT NULL,
    stack_id character varying(64),
    count_begin_time timestamp with time zone,
    count_end_time timestamp with time zone,
    resources_type integer,
    cpu integer,
    gpu integer,
    mem integer,
    disk integer,
    network text,
    user_id character varying(64),
    bp_id character varying(64),
    bill_cycle integer,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.tbl_bill_month_stack_detail_info OWNER TO postgres;

--
-- Name: tbl_bill_task_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_bill_task_info (
    task_id integer NOT NULL,
    bill_type integer,
    bill_cycle integer,
    product_type integer,
    status integer,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.tbl_bill_task_info OWNER TO postgres;

--
-- Name: tbl_bill_task_info_task_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tbl_bill_task_info_task_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tbl_bill_task_info_task_id_seq OWNER TO postgres;

--
-- Name: tbl_bill_task_info_task_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tbl_bill_task_info_task_id_seq OWNED BY public.tbl_bill_task_info.task_id;


--
-- Name: tbl_bp_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_bp_info (
    bp_id character varying(64) NOT NULL,
    bp_name character varying(64),
    website character varying(64),
    license_id character varying(64),
    master_user character varying(64),
    status integer,
    contact_info character varying(512),
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.tbl_bp_info OWNER TO postgres;

--
-- Name: tbl_dick_spec; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_dick_spec (
    disk_spec_id character varying(64),
    device_spec_id character varying(64),
    disk_type character varying(20),
    vendor character varying(64),
    model character varying(64),
    size bigint,
    trans_speed bigint
);


ALTER TABLE public.tbl_dick_spec OWNER TO postgres;

--
-- Name: tbl_disk_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_disk_info (
    disk_id character varying(64) NOT NULL,
    volume_id character varying(64),
    vm_instance_id character varying(64),
    name character varying(128),
    size integer,
    disk_type integer,
    phase_status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    is_new boolean
);


ALTER TABLE public.tbl_disk_info OWNER TO postgres;

--
-- Name: tbl_eip; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_eip (
    eip_id character varying(64) NOT NULL,
    user_id character varying(64),
    address_type smallint,
    ipaddr character varying(128),
    prefix_len smallint,
    status smallint,
    bandwidth character varying(32),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    pool_id character varying(64),
    bound_type character varying(64),
    bound_id text,
    public_ip character varying(64)
);


ALTER TABLE public.tbl_eip OWNER TO postgres;

--
-- Name: tbl_eip_map; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_eip_map (
    eip_map_id character varying(64) NOT NULL,
    map_name character varying(64),
    eip_id character varying(64),
    subnet_id character varying(64),
    user_id character varying(64),
    port_id character varying(64),
    is_static_ip boolean,
    inside_ip character varying(128),
    status smallint,
    bandwidth character varying(32),
    is_one_to_one boolean,
    create_time timestamp with time zone,
    update_time timestamp with time zone
);


ALTER TABLE public.tbl_eip_map OWNER TO postgres;

--
-- Name: tbl_eip_pool; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_eip_pool (
    pool_id character varying(64) NOT NULL,
    name character varying(64),
    description text,
    phase_status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone
);


ALTER TABLE public.tbl_eip_pool OWNER TO postgres;

--
-- Name: tbl_eip_pool_vpc_ref; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_eip_pool_vpc_ref (
    pool_vpc_id character varying(64) NOT NULL,
    pool_id character varying(64),
    vpc_id character varying(64),
    vlan_id integer,
    phase_status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone
);


ALTER TABLE public.tbl_eip_pool_vpc_ref OWNER TO postgres;

--
-- Name: tbl_event; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_event (
    event_id character varying(64) NOT NULL,
    user_id character varying(64),
    content character varying(128),
    detail_info text,
    create_time timestamp(6) with time zone,
    update_time timestamp(6) with time zone,
    phase_status integer,
    result character varying(64),
    username character varying(128)
);


ALTER TABLE public.tbl_event OWNER TO postgres;

--
-- Name: tbl_flavor; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_flavor (
    flavor_id character varying(64) NOT NULL,
    name character varying(64),
    type smallint,
    cpu integer,
    mem integer,
    root_disk integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    phase_status integer,
    user_id character varying(64),
    gpu_count integer,
    gpu_name character varying(128),
    need_ib boolean
);


ALTER TABLE public.tbl_flavor OWNER TO postgres;

--
-- Name: tbl_frontend; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_frontend (
    frontend_id character varying(64) NOT NULL,
    lb_id character varying(64),
    backend_id character varying(64),
    name character varying(64),
    listen_port character varying(64),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    user_id character varying(64),
    phase_status integer,
    frontend_id_from_agent character varying(64),
    port integer
);


ALTER TABLE public.tbl_frontend OWNER TO postgres;

--
-- Name: TABLE tbl_frontend; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.tbl_frontend IS '负载均衡器-监听器';


--
-- Name: tbl_hypervisor_node; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_hypervisor_node (
    node_id character varying(64) NOT NULL,
    instance_id character varying(64),
    name character varying(64),
    phase_status integer,
    manage_ip character varying(40),
    host_name character varying(128),
    sys_username character varying(64),
    sys_password character varying(128),
    pubkey_id character varying(64),
    description text,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    backup_node_id character varying(64),
    error_count integer,
    cpu_model character varying(128),
    cpu_log_count integer,
    mem_total integer,
    cpu_phy_count integer,
    agent_id character varying(64),
    available_ib_count integer,
    master_l3 boolean
);


ALTER TABLE public.tbl_hypervisor_node OWNER TO postgres;

--
-- Name: tbl_image; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_image (
    image_id character varying(64) NOT NULL,
    file_id_from_agent character varying(64),
    user_id character varying(64),
    image_os_type smallint,
    image_os_vendor smallint,
    image_os_version character varying(32),
    image_name character varying(64),
    image_format smallint,
    agent_ip character varying(64),
    phase_status smallint,
    phase_info character varying(64),
    is_public boolean,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    description text,
    vm_instance_id character varying(64),
    image_base character varying(64)
);


ALTER TABLE public.tbl_image OWNER TO postgres;

--
-- Name: tbl_instance_group; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_instance_group (
    instance_group_id character varying(64) NOT NULL,
    name character varying(64),
    description text,
    user_id character varying(64),
    create_time timestamp(6) with time zone,
    update_time timestamp(6) with time zone,
    phase_status integer
);


ALTER TABLE public.tbl_instance_group OWNER TO postgres;

--
-- Name: tbl_instance_network_ref; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_instance_network_ref (
    instance_network_id character varying(64) NOT NULL,
    instance_id character varying(64),
    vpc_id character varying(64),
    subnet_id character varying(64),
    port_id character varying(64),
    instance_type integer,
    static_ip character varying(40),
    phase_status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    is_vip boolean
);


ALTER TABLE public.tbl_instance_network_ref OWNER TO postgres;

--
-- Name: tbl_ip_pool; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_ip_pool (
    pool_id character varying(64) NOT NULL,
    name character varying(64),
    address_type smallint,
    user_id character varying(64),
    ip_addresses text,
    description text,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.tbl_ip_pool OWNER TO postgres;

--
-- Name: tbl_loadbalancer; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_loadbalancer (
    lb_id character varying(64) NOT NULL,
    name character varying(64),
    description text,
    user_id character varying(64),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    phase_status integer,
    lb_id_from_agent character varying(64),
    subnet_id character varying(64)
);


ALTER TABLE public.tbl_loadbalancer OWNER TO postgres;

--
-- Name: TABLE tbl_loadbalancer; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.tbl_loadbalancer IS '负载均衡器实例';


--
-- Name: tbl_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_log (
    log_id character varying(64) NOT NULL,
    description text,
    resource character varying(64),
    create_time timestamp(6) with time zone,
    user_id character varying(64),
    operator character varying(64)
);


ALTER TABLE public.tbl_log OWNER TO postgres;

--
-- Name: tbl_nfs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_nfs (
    nfs_id character varying(64) NOT NULL,
    name character varying(64),
    vpc_id character varying(64),
    subnet_id character varying(64),
    port_id character varying(64),
    size integer,
    nfs_id_from_agent character varying(64),
    phase_status integer,
    node_ip character varying(64),
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    user_id character varying(64),
    description text
);


ALTER TABLE public.tbl_nfs OWNER TO postgres;

--
-- Name: tbl_nic_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_nic_info (
    nic_id character varying(64) NOT NULL,
    device_id character varying(64),
    nic_name character varying(64),
    link_state character varying(32),
    network_type smallint,
    ipmi_mac character varying(17),
    ip character varying(40),
    address_type smallint
);


ALTER TABLE public.tbl_nic_info OWNER TO postgres;

--
-- Name: tbl_nic_spec; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_nic_spec (
    nic_spec_id character varying(64) NOT NULL,
    device_spec_id character varying(64),
    nic_product_name character varying(64),
    speed character varying(64),
    mac character varying(17),
    link_is_up boolean,
    slot_id_from_agent character varying(64),
    phase_status integer,
    switch_id_from_agent character varying(64),
    switch_interface character varying(32),
    create_time timestamp with time zone,
    update_time timestamp with time zone
);


ALTER TABLE public.tbl_nic_spec OWNER TO postgres;

--
-- Name: tbl_node_image; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_node_image (
    node_image_id character varying(64) NOT NULL,
    image_id character varying(64),
    storage_pool_id character varying(64),
    node_image_id_from_agent character varying(64),
    phase_status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    user_id character varying(64),
    node_id character varying(64),
    storage_pool_id_from_agent character varying(64),
    node_ip character varying(64)
);


ALTER TABLE public.tbl_node_image OWNER TO postgres;

--
-- Name: tbl_node_storage_pool; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_node_storage_pool (
    node_storage_pool_id character varying(64) NOT NULL,
    storage_pool_id character varying(64),
    storage_pool_id_from_agent character varying(64),
    node_ip character varying(64),
    sid character varying(64),
    paras character varying(128),
    phase_status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    type integer
);


ALTER TABLE public.tbl_node_storage_pool OWNER TO postgres;

--
-- Name: tbl_operation_log; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_operation_log (
    id character varying(64) NOT NULL,
    description character varying(100),
    user_id character varying(64),
    user_name character varying(64),
    start_time timestamp with time zone,
    spend_time integer,
    uri character varying(500),
    method character varying(10),
    parameter text,
    user_agent character varying(500),
    ip character varying(30),
    result text
);


ALTER TABLE public.tbl_operation_log OWNER TO postgres;

--
-- Name: TABLE tbl_operation_log; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.tbl_operation_log IS 'operation log';


--
-- Name: COLUMN tbl_operation_log.id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_operation_log.id IS 'id';


--
-- Name: COLUMN tbl_operation_log.spend_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_operation_log.spend_time IS 'ms';


--
-- Name: tbl_pci_device; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_pci_device (
    device_id character varying(64) NOT NULL,
    device_group_id character varying(64),
    type character varying(64),
    name character varying(64),
    phase_status integer,
    user_id character varying(64),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    device_id_from_agent character varying(64),
    version integer,
    vm_instance_id character varying(64),
    partition_id character varying(64),
    node_id character varying(64)
);


ALTER TABLE public.tbl_pci_device OWNER TO postgres;

--
-- Name: tbl_pci_device_group; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_pci_device_group (
    device_group_id character varying(64) NOT NULL,
    node_id character varying(64),
    type integer,
    name character varying(64),
    phase_status integer,
    user_id character varying(64),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    device_group_id_from_agent character varying(64),
    vm_instance_id character varying(64)
);


ALTER TABLE public.tbl_pci_device_group OWNER TO postgres;

--
-- Name: tbl_port; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_port (
    port_id character varying(64) NOT NULL,
    subnet_id character varying(64),
    port_id_from_agent character varying(64),
    ip_address character varying(128),
    mac_address character varying(20),
    phase_status smallint,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    type smallint,
    instance_id character varying(64),
    of_port integer,
    host_id_from_agent character varying(64),
    agent_id character varying(64),
    eip_id character varying(64),
    eip_phase_status integer,
    speed integer
);


ALTER TABLE public.tbl_port OWNER TO postgres;

--
-- Name: tbl_port_map; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_port_map (
    port_map_id character varying(64) NOT NULL,
    eip_map_id character varying(64),
    protocol smallint,
    global_port integer,
    local_port integer,
    status smallint,
    create_time timestamp with time zone,
    update_time timestamp with time zone
);


ALTER TABLE public.tbl_port_map OWNER TO postgres;

--
-- Name: tbl_pubkey; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_pubkey (
    pubkey_id character varying(64) NOT NULL,
    user_id character varying(64) NOT NULL,
    name character varying(64),
    pubkey text,
    description text,
    create_time timestamp(6) with time zone,
    update_time timestamp(6) with time zone
);


ALTER TABLE public.tbl_pubkey OWNER TO postgres;

--
-- Name: tbl_receiver; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_receiver (
    receiver_id character varying(64) NOT NULL,
    type integer,
    contact_info text,
    contact_count integer,
    name character varying(64),
    description text,
    user_id character varying(64),
    phase_status integer,
    create_time timestamp(6) with time zone,
    update_time timestamp(6) with time zone
);


ALTER TABLE public.tbl_receiver OWNER TO postgres;

--
-- Name: TABLE tbl_receiver; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.tbl_receiver IS '运维管理--报警设置--通知对象';


--
-- Name: COLUMN tbl_receiver.receiver_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.receiver_id IS '主键';


--
-- Name: COLUMN tbl_receiver.type; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.type IS '类型（0，邮箱 1，手机号）';


--
-- Name: COLUMN tbl_receiver.contact_info; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.contact_info IS '邮箱地址或手机号--多个';


--
-- Name: COLUMN tbl_receiver.contact_count; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.contact_count IS '邮箱地址或手机号--数量';


--
-- Name: COLUMN tbl_receiver.name; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.name IS '名称';


--
-- Name: COLUMN tbl_receiver.description; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.description IS '描述';


--
-- Name: COLUMN tbl_receiver.user_id; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.user_id IS '用户id';


--
-- Name: COLUMN tbl_receiver.phase_status; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.phase_status IS '报警状态（0，未报警 1，已报警）';


--
-- Name: COLUMN tbl_receiver.create_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.create_time IS '创建时间';


--
-- Name: COLUMN tbl_receiver.update_time; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.tbl_receiver.update_time IS '修改时间';


--
-- Name: tbl_resource_stats; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_resource_stats (
    stats_id character varying(64) NOT NULL,
    user_id character varying(64),
    name character varying(64),
    total integer,
    used integer,
    running integer,
    unit character varying(64),
    phase_status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone
);


ALTER TABLE public.tbl_resource_stats OWNER TO postgres;

--
-- Name: tbl_role_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_role_info (
    role_id integer NOT NULL,
    platform character varying(20),
    role character varying(20)
);


ALTER TABLE public.tbl_role_info OWNER TO postgres;

--
-- Name: tbl_role_info_role_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.tbl_role_info_role_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.tbl_role_info_role_id_seq OWNER TO postgres;

--
-- Name: tbl_role_info_role_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.tbl_role_info_role_id_seq OWNED BY public.tbl_role_info.role_id;


--
-- Name: tbl_sched_edge_monopoly; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_sched_edge_monopoly (
    node_id character varying(64) NOT NULL,
    ref_id character varying(64) NOT NULL,
    status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone
);


ALTER TABLE public.tbl_sched_edge_monopoly OWNER TO postgres;

--
-- Name: tbl_security_group; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_security_group (
    sg_id character varying(64) NOT NULL,
    name character varying(64),
    phase_status integer,
    description text,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    user_id character varying(64),
    sg_id_from_agent character varying(64)
);


ALTER TABLE public.tbl_security_group OWNER TO postgres;

--
-- Name: tbl_security_group_rule; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_security_group_rule (
    rule_id character varying(64) NOT NULL,
    sg_id character varying(64),
    phase_status integer,
    priority smallint,
    direction smallint,
    protocol smallint,
    address_type smallint,
    port character varying(128),
    cidr character varying(40),
    sg_id_reference character varying(64),
    description text,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    pool_id character varying(64),
    action smallint
);


ALTER TABLE public.tbl_security_group_rule OWNER TO postgres;

--
-- Name: tbl_sg_vm_instance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_sg_vm_instance (
    sg_vm_id character varying(64) NOT NULL,
    sg_id character varying(64),
    instance_id character varying(64),
    phase_status integer,
    sg_id_from_agent character varying(64),
    vpc_id_from_agent character varying(64),
    create_time timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp(6) with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.tbl_sg_vm_instance OWNER TO postgres;

--
-- Name: tbl_share; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_share (
    share_id character varying(64) NOT NULL,
    share_id_from_agent character varying(64),
    user_id character varying(64),
    baremetal_id character varying(64),
    image_id character varying(64),
    iscsi_initiator character varying(80),
    iscsi_target character varying(96),
    iscsi_ipport character varying(64),
    phase_status smallint,
    phase_info character varying(64),
    create_time timestamp with time zone,
    update_time timestamp with time zone
);


ALTER TABLE public.tbl_share OWNER TO postgres;

--
-- Name: tbl_storage_pool; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_storage_pool (
    storage_pool_id character varying(64) NOT NULL,
    storage_pool_id_from_agent character varying(64),
    type integer,
    sid character varying(64),
    paras character varying(128),
    phase_status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    name character varying(64),
    description text
);


ALTER TABLE public.tbl_storage_pool OWNER TO postgres;

--
-- Name: tbl_subnet; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_subnet (
    subnet_id character varying(64) NOT NULL,
    subnet_id_from_agent character varying(64),
    name character varying(64),
    user_id character varying(64) NOT NULL,
    vpc_id character varying(64),
    phase_status smallint,
    phase_info character varying(64),
    address_type smallint,
    subnet_cidr character varying(40),
    gateway_ip character varying(40),
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    description text
);


ALTER TABLE public.tbl_subnet OWNER TO postgres;

--
-- Name: tbl_user_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_user_info (
    user_id character varying(64) NOT NULL,
    bp_id character varying(64),
    user_name character varying(64),
    password character varying(100),
    phone character varying(32),
    email character varying(64),
    address character varying(512),
    is_allowed boolean,
    gender integer,
    status integer,
    kind integer,
    level integer,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    api_key character varying(64)
);


ALTER TABLE public.tbl_user_info OWNER TO postgres;

--
-- Name: tbl_user_role_info; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_user_role_info (
    user_id character varying(64) NOT NULL,
    role_id bigint NOT NULL
);


ALTER TABLE public.tbl_user_role_info OWNER TO postgres;

--
-- Name: tbl_user_sg; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_user_sg (
    user_sg_id character varying(64) NOT NULL,
    user_id character varying(64),
    sg_id character varying(64)
);


ALTER TABLE public.tbl_user_sg OWNER TO postgres;

--
-- Name: tbl_vm_instance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_vm_instance (
    vm_instance_id character varying(64) NOT NULL,
    instance_id_from_agent character varying(64),
    name character varying(64),
    phase_status integer,
    user_id character varying(64),
    node_id character varying(64),
    flavor_id character varying(64),
    image_id character varying(64) NOT NULL,
    vpc_id character varying(64) NOT NULL,
    subnet_id character varying(64) NOT NULL,
    port_id character varying(64),
    static_ip character varying(40),
    host_name character varying(128),
    sys_username character varying(64),
    sys_password character varying(128),
    pubkey_id character varying(64),
    description text,
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    last_node_id character varying(64),
    dest_node_id character varying(64),
    volume_id character varying(64),
    storage_pool_id character varying(64),
    instance_group_id character varying(64),
    boot_dev character varying(64),
    os_type character varying(64),
    cmp_tenant_id character varying(64),
    cmp_user_id character varying(64),
    cpu_count integer,
    mem_size integer,
    eip_id character varying(64),
    root_disk integer,
    recycle_mem_size integer,
    recycle_cpu_count integer
);


ALTER TABLE public.tbl_vm_instance OWNER TO postgres;

--
-- Name: tbl_vm_snap; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_vm_snap (
    snap_id character varying(64) NOT NULL,
    vm_instance_id character varying(64),
    name character varying(64),
    user_id character varying(64),
    snap_id_from_agent character varying(64),
    phase_status integer,
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    description text,
    is_current boolean,
    parent_id character varying(64)
);


ALTER TABLE public.tbl_vm_snap OWNER TO postgres;

--
-- Name: tbl_volume; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_volume (
    volume_id character varying(64) NOT NULL,
    storage_pool_id character varying(64),
    volume_id_from_agent character varying(64),
    image_id character varying(64),
    name character varying(64),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    user_id character varying(64),
    phase_status integer,
    type integer,
    description text,
    size integer,
    export_name character varying(255),
    vm_id character varying(64),
    node_ip character varying(64),
    last_ip character varying(64),
    dest_ip character varying(64)
);


ALTER TABLE public.tbl_volume OWNER TO postgres;

--
-- Name: tbl_volume_snap; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_volume_snap (
    volume_snap_id character varying(64) NOT NULL,
    volume_snap_id_from_agent character varying(64),
    user_id character varying(64),
    volume_id character varying(64),
    create_time timestamp with time zone,
    update_time timestamp with time zone,
    phase_status integer,
    description text,
    name character varying(64),
    is_current boolean,
    parent_id character varying(64)
);


ALTER TABLE public.tbl_volume_snap OWNER TO postgres;

--
-- Name: tbl_vpc; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.tbl_vpc (
    vpc_id character varying(64) NOT NULL,
    vpc_id_from_agent character varying(64),
    name character varying(64),
    user_id character varying(64) NOT NULL,
    vlan_id integer,
    phase_status smallint,
    phase_info character varying(64),
    address_type smallint,
    vpc_cidr character varying(40),
    create_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    update_time timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    description text
);


ALTER TABLE public.tbl_vpc OWNER TO postgres;

--
-- Name: tbl_bill_task_info task_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_bill_task_info ALTER COLUMN task_id SET DEFAULT nextval('public.tbl_bill_task_info_task_id_seq'::regclass);


--
-- Name: tbl_role_info role_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_role_info ALTER COLUMN role_id SET DEFAULT nextval('public.tbl_role_info_role_id_seq'::regclass);


--
-- Name: tbl_subnet ak_key_2_tbl_rs_s; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_subnet
    ADD CONSTRAINT ak_key_2_tbl_rs_s UNIQUE (vpc_id, subnet_cidr, phase_status);


--
-- Name: tbl_eip pk_rs_eip; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_eip
    ADD CONSTRAINT pk_rs_eip PRIMARY KEY (eip_id);


--
-- Name: tbl_alarm_info pk_tbl_alarm_info; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_alarm_info
    ADD CONSTRAINT pk_tbl_alarm_info PRIMARY KEY (info_id);


--
-- Name: tbl_alarm_rule pk_tbl_alarm_rule; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_alarm_rule
    ADD CONSTRAINT pk_tbl_alarm_rule PRIMARY KEY (rule_id);


--
-- Name: tbl_alarm_rule_receiver pk_tbl_alarm_rule_receiver; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_alarm_rule_receiver
    ADD CONSTRAINT pk_tbl_alarm_rule_receiver PRIMARY KEY (rule_receiver_id);


--
-- Name: tbl_backend pk_tbl_backend; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_backend
    ADD CONSTRAINT pk_tbl_backend PRIMARY KEY (backend_id);


--
-- Name: tbl_baremetal_cluster pk_tbl_baremetal_cluster; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_baremetal_cluster
    ADD CONSTRAINT pk_tbl_baremetal_cluster PRIMARY KEY (cluster_id);


--
-- Name: tbl_baremetal_device pk_tbl_baremetal_device; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_baremetal_device
    ADD CONSTRAINT pk_tbl_baremetal_device PRIMARY KEY (device_id);


--
-- Name: tbl_baremetal_device_spec pk_tbl_baremetal_device_spec; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_baremetal_device_spec
    ADD CONSTRAINT pk_tbl_baremetal_device_spec PRIMARY KEY (device_spec_id);


--
-- Name: tbl_baremetal_instance pk_tbl_baremetal_instance; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_baremetal_instance
    ADD CONSTRAINT pk_tbl_baremetal_instance PRIMARY KEY (instance_id);


--
-- Name: tbl_bill_month_container_detail_info pk_tbl_bill_month_container_de; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_bill_month_container_detail_info
    ADD CONSTRAINT pk_tbl_bill_month_container_de PRIMARY KEY (bill_id);


--
-- Name: tbl_bill_month_stack_detail_info pk_tbl_bill_month_stack_detail; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_bill_month_stack_detail_info
    ADD CONSTRAINT pk_tbl_bill_month_stack_detail PRIMARY KEY (bill_id);


--
-- Name: tbl_bill_task_info pk_tbl_bill_task_info; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_bill_task_info
    ADD CONSTRAINT pk_tbl_bill_task_info PRIMARY KEY (task_id);


--
-- Name: tbl_bp_info pk_tbl_bp_info; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_bp_info
    ADD CONSTRAINT pk_tbl_bp_info PRIMARY KEY (bp_id);


--
-- Name: tbl_disk_info pk_tbl_disk_info; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_disk_info
    ADD CONSTRAINT pk_tbl_disk_info PRIMARY KEY (disk_id);


--
-- Name: tbl_eip_pool pk_tbl_eip_pool; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_eip_pool
    ADD CONSTRAINT pk_tbl_eip_pool PRIMARY KEY (pool_id);


--
-- Name: tbl_eip_pool_vpc_ref pk_tbl_eip_pool_vpc_ref; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_eip_pool_vpc_ref
    ADD CONSTRAINT pk_tbl_eip_pool_vpc_ref PRIMARY KEY (pool_vpc_id);


--
-- Name: tbl_event pk_tbl_event; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_event
    ADD CONSTRAINT pk_tbl_event PRIMARY KEY (event_id);


--
-- Name: tbl_flavor pk_tbl_flavor; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_flavor
    ADD CONSTRAINT pk_tbl_flavor PRIMARY KEY (flavor_id);


--
-- Name: tbl_frontend pk_tbl_frontend; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_frontend
    ADD CONSTRAINT pk_tbl_frontend PRIMARY KEY (frontend_id);


--
-- Name: tbl_hypervisor_node pk_tbl_hypervisor_node; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_hypervisor_node
    ADD CONSTRAINT pk_tbl_hypervisor_node PRIMARY KEY (node_id);


--
-- Name: tbl_instance_group pk_tbl_instance_group; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_instance_group
    ADD CONSTRAINT pk_tbl_instance_group PRIMARY KEY (instance_group_id);


--
-- Name: tbl_instance_network_ref pk_tbl_instance_network_ref; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_instance_network_ref
    ADD CONSTRAINT pk_tbl_instance_network_ref PRIMARY KEY (instance_network_id);


--
-- Name: tbl_ip_pool pk_tbl_ip_pool; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_ip_pool
    ADD CONSTRAINT pk_tbl_ip_pool PRIMARY KEY (pool_id);


--
-- Name: tbl_loadbalancer pk_tbl_loadbalaner; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_loadbalancer
    ADD CONSTRAINT pk_tbl_loadbalaner PRIMARY KEY (lb_id);


--
-- Name: tbl_nfs pk_tbl_nfs; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_nfs
    ADD CONSTRAINT pk_tbl_nfs PRIMARY KEY (nfs_id);


--
-- Name: tbl_nic_info pk_tbl_nic_info; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_nic_info
    ADD CONSTRAINT pk_tbl_nic_info PRIMARY KEY (nic_id);


--
-- Name: tbl_nic_spec pk_tbl_nic_spec; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_nic_spec
    ADD CONSTRAINT pk_tbl_nic_spec PRIMARY KEY (nic_spec_id);


--
-- Name: tbl_node_image pk_tbl_node_image; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_node_image
    ADD CONSTRAINT pk_tbl_node_image PRIMARY KEY (node_image_id);


--
-- Name: tbl_node_storage_pool pk_tbl_node_storage_pool; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_node_storage_pool
    ADD CONSTRAINT pk_tbl_node_storage_pool PRIMARY KEY (node_storage_pool_id);


--
-- Name: tbl_operation_log pk_tbl_operation_log; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_operation_log
    ADD CONSTRAINT pk_tbl_operation_log PRIMARY KEY (id);


--
-- Name: tbl_pci_device pk_tbl_pci_device; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_pci_device
    ADD CONSTRAINT pk_tbl_pci_device PRIMARY KEY (device_id);


--
-- Name: tbl_pci_device_group pk_tbl_pci_device_group; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_pci_device_group
    ADD CONSTRAINT pk_tbl_pci_device_group PRIMARY KEY (device_group_id);


--
-- Name: tbl_pubkey pk_tbl_pubkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_pubkey
    ADD CONSTRAINT pk_tbl_pubkey PRIMARY KEY (pubkey_id);


--
-- Name: tbl_receiver pk_tbl_receiver; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_receiver
    ADD CONSTRAINT pk_tbl_receiver PRIMARY KEY (receiver_id);


--
-- Name: tbl_resource_stats pk_tbl_resource_stats; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_resource_stats
    ADD CONSTRAINT pk_tbl_resource_stats PRIMARY KEY (stats_id);


--
-- Name: tbl_role_info pk_tbl_role_info; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_role_info
    ADD CONSTRAINT pk_tbl_role_info PRIMARY KEY (role_id);


--
-- Name: tbl_eip_map pk_tbl_rs_eip_map; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_eip_map
    ADD CONSTRAINT pk_tbl_rs_eip_map PRIMARY KEY (eip_map_id);


--
-- Name: tbl_image pk_tbl_rs_image; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_image
    ADD CONSTRAINT pk_tbl_rs_image PRIMARY KEY (image_id);


--
-- Name: tbl_port pk_tbl_rs_port; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_port
    ADD CONSTRAINT pk_tbl_rs_port PRIMARY KEY (port_id);


--
-- Name: tbl_port_map pk_tbl_rs_port_map; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_port_map
    ADD CONSTRAINT pk_tbl_rs_port_map PRIMARY KEY (port_map_id);


--
-- Name: tbl_share pk_tbl_rs_share; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_share
    ADD CONSTRAINT pk_tbl_rs_share PRIMARY KEY (share_id);


--
-- Name: tbl_subnet pk_tbl_rs_subnet; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_subnet
    ADD CONSTRAINT pk_tbl_rs_subnet PRIMARY KEY (subnet_id);


--
-- Name: tbl_vpc pk_tbl_rs_vpc; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_vpc
    ADD CONSTRAINT pk_tbl_rs_vpc PRIMARY KEY (vpc_id);


--
-- Name: tbl_sched_edge_monopoly pk_tbl_sched_edge_monopoly; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_sched_edge_monopoly
    ADD CONSTRAINT pk_tbl_sched_edge_monopoly PRIMARY KEY (node_id, ref_id);


--
-- Name: tbl_security_group pk_tbl_security_group; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_security_group
    ADD CONSTRAINT pk_tbl_security_group PRIMARY KEY (sg_id);


--
-- Name: tbl_security_group_rule pk_tbl_security_group_rule; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_security_group_rule
    ADD CONSTRAINT pk_tbl_security_group_rule PRIMARY KEY (rule_id);


--
-- Name: tbl_sg_vm_instance pk_tbl_sg_vm_instance; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_sg_vm_instance
    ADD CONSTRAINT pk_tbl_sg_vm_instance PRIMARY KEY (sg_vm_id);


--
-- Name: tbl_storage_pool pk_tbl_storage_pool; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_storage_pool
    ADD CONSTRAINT pk_tbl_storage_pool PRIMARY KEY (storage_pool_id);


--
-- Name: tbl_user_info pk_tbl_user_info; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_user_info
    ADD CONSTRAINT pk_tbl_user_info PRIMARY KEY (user_id);


--
-- Name: tbl_user_role_info pk_tbl_user_role_info; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_user_role_info
    ADD CONSTRAINT pk_tbl_user_role_info PRIMARY KEY (user_id, role_id);


--
-- Name: tbl_user_sg pk_tbl_user_sg; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_user_sg
    ADD CONSTRAINT pk_tbl_user_sg PRIMARY KEY (user_sg_id);


--
-- Name: tbl_vm_instance pk_tbl_vm_instance; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_vm_instance
    ADD CONSTRAINT pk_tbl_vm_instance PRIMARY KEY (vm_instance_id);


--
-- Name: tbl_vm_snap pk_tbl_vm_snap; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_vm_snap
    ADD CONSTRAINT pk_tbl_vm_snap PRIMARY KEY (snap_id);


--
-- Name: tbl_volume pk_tbl_volume; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_volume
    ADD CONSTRAINT pk_tbl_volume PRIMARY KEY (volume_id);


--
-- Name: tbl_volume_snap pk_tbl_volume_snap; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_volume_snap
    ADD CONSTRAINT pk_tbl_volume_snap PRIMARY KEY (volume_snap_id);


--
-- Name: tbl_log tbl_alarm_rule_receiver_copy1_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_log
    ADD CONSTRAINT tbl_alarm_rule_receiver_copy1_pkey PRIMARY KEY (log_id);


--
-- Name: tbl_eip unique_ip_addr; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_eip
    ADD CONSTRAINT unique_ip_addr UNIQUE (ipaddr, status);


--
-- Name: bp_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX bp_index ON public.tbl_bp_info USING btree (bp_id);


--
-- Name: container_cycle_bp_user_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX container_cycle_bp_user_index ON public.tbl_bill_month_container_detail_info USING btree (bill_cycle, bp_id, user_id);


--
-- Name: month_container_detail_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX month_container_detail_index ON public.tbl_bill_month_container_detail_info USING btree (bill_id);


--
-- Name: month_stack_detail_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX month_stack_detail_index ON public.tbl_bill_month_stack_detail_info USING btree (bill_id);


--
-- Name: role_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX role_index ON public.tbl_role_info USING btree (role_id);


--
-- Name: stack_cycle_bp_user_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX stack_cycle_bp_user_index ON public.tbl_bill_month_stack_detail_info USING btree (bill_cycle, bp_id, user_id);


--
-- Name: task_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX task_index ON public.tbl_bill_task_info USING btree (task_id);


--
-- Name: user_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX user_index ON public.tbl_user_info USING btree (user_id);


--
-- Name: user_role_index; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX user_role_index ON public.tbl_user_role_info USING btree (user_id, role_id);


--
-- Name: tbl_alarm_info fk_tbl_alar_reference_tbl_alar; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_alarm_info
    ADD CONSTRAINT fk_tbl_alar_reference_tbl_alar FOREIGN KEY (rule_id) REFERENCES public.tbl_alarm_rule(rule_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_alarm_rule_receiver fk_tbl_alar_reference_tbl_alar; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_alarm_rule_receiver
    ADD CONSTRAINT fk_tbl_alar_reference_tbl_alar FOREIGN KEY (rule_id) REFERENCES public.tbl_alarm_rule(rule_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_alarm_rule_receiver fk_tbl_alar_reference_tbl_rece; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_alarm_rule_receiver
    ADD CONSTRAINT fk_tbl_alar_reference_tbl_rece FOREIGN KEY (receiver_id) REFERENCES public.tbl_receiver(receiver_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_backend fk_tbl_back_reference_tbl_load; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_backend
    ADD CONSTRAINT fk_tbl_back_reference_tbl_load FOREIGN KEY (lb_id) REFERENCES public.tbl_loadbalancer(lb_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_baremetal_instance fk_tbl_bare_reference_tbl_bare; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_baremetal_instance
    ADD CONSTRAINT fk_tbl_bare_reference_tbl_bare FOREIGN KEY (device_id) REFERENCES public.tbl_baremetal_device(device_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_baremetal_instance fk_tbl_bare_reference_tbl_pubk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_baremetal_instance
    ADD CONSTRAINT fk_tbl_bare_reference_tbl_pubk FOREIGN KEY (pubkey_id) REFERENCES public.tbl_pubkey(pubkey_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: tbl_dick_spec fk_tbl_dick_reference_tbl_bare; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_dick_spec
    ADD CONSTRAINT fk_tbl_dick_reference_tbl_bare FOREIGN KEY (device_spec_id) REFERENCES public.tbl_baremetal_device_spec(device_spec_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_disk_info fk_tbl_disk_reference_tbl_vm_i; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_disk_info
    ADD CONSTRAINT fk_tbl_disk_reference_tbl_vm_i FOREIGN KEY (vm_instance_id) REFERENCES public.tbl_vm_instance(vm_instance_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_frontend fk_tbl_fron_reference_tbl_back; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_frontend
    ADD CONSTRAINT fk_tbl_fron_reference_tbl_back FOREIGN KEY (backend_id) REFERENCES public.tbl_backend(backend_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_frontend fk_tbl_fron_reference_tbl_load; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_frontend
    ADD CONSTRAINT fk_tbl_fron_reference_tbl_load FOREIGN KEY (lb_id) REFERENCES public.tbl_loadbalancer(lb_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_hypervisor_node fk_tbl_hype_reference_tbl_bare; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_hypervisor_node
    ADD CONSTRAINT fk_tbl_hype_reference_tbl_bare FOREIGN KEY (instance_id) REFERENCES public.tbl_baremetal_instance(instance_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_nic_info fk_tbl_nic__reference_tbl_bare; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_nic_info
    ADD CONSTRAINT fk_tbl_nic__reference_tbl_bare FOREIGN KEY (device_id) REFERENCES public.tbl_baremetal_device(device_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_nic_spec fk_tbl_nic__reference_tbl_bare; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_nic_spec
    ADD CONSTRAINT fk_tbl_nic__reference_tbl_bare FOREIGN KEY (device_spec_id) REFERENCES public.tbl_baremetal_device_spec(device_spec_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_node_image fk_tbl_node_reference_tbl_imag; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_node_image
    ADD CONSTRAINT fk_tbl_node_reference_tbl_imag FOREIGN KEY (image_id) REFERENCES public.tbl_image(image_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_node_image fk_tbl_node_reference_tbl_stor; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_node_image
    ADD CONSTRAINT fk_tbl_node_reference_tbl_stor FOREIGN KEY (storage_pool_id) REFERENCES public.tbl_storage_pool(storage_pool_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_node_storage_pool fk_tbl_node_reference_tbl_stor; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_node_storage_pool
    ADD CONSTRAINT fk_tbl_node_reference_tbl_stor FOREIGN KEY (storage_pool_id) REFERENCES public.tbl_storage_pool(storage_pool_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_pci_device_group fk_tbl_pci__reference_tbl_hype; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_pci_device_group
    ADD CONSTRAINT fk_tbl_pci__reference_tbl_hype FOREIGN KEY (node_id) REFERENCES public.tbl_hypervisor_node(node_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_pci_device fk_tbl_pci__reference_tbl_pci_; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_pci_device
    ADD CONSTRAINT fk_tbl_pci__reference_tbl_pci_ FOREIGN KEY (device_group_id) REFERENCES public.tbl_pci_device_group(device_group_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_eip_map fk_tbl_rs_e_reference_rs_eip; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_eip_map
    ADD CONSTRAINT fk_tbl_rs_e_reference_rs_eip FOREIGN KEY (eip_id) REFERENCES public.tbl_eip(eip_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_eip_map fk_tbl_rs_e_reference_tbl_rs_s; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_eip_map
    ADD CONSTRAINT fk_tbl_rs_e_reference_tbl_rs_s FOREIGN KEY (subnet_id) REFERENCES public.tbl_subnet(subnet_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_port_map fk_tbl_rs_p_reference_tbl_rs_e; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_port_map
    ADD CONSTRAINT fk_tbl_rs_p_reference_tbl_rs_e FOREIGN KEY (eip_map_id) REFERENCES public.tbl_eip_map(eip_map_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_port fk_tbl_rs_p_reference_tbl_rs_s; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_port
    ADD CONSTRAINT fk_tbl_rs_p_reference_tbl_rs_s FOREIGN KEY (subnet_id) REFERENCES public.tbl_subnet(subnet_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_share fk_tbl_rs_s_reference_tbl_rs_i; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_share
    ADD CONSTRAINT fk_tbl_rs_s_reference_tbl_rs_i FOREIGN KEY (image_id) REFERENCES public.tbl_image(image_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_subnet fk_tbl_rs_s_reference_tbl_rs_v; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_subnet
    ADD CONSTRAINT fk_tbl_rs_s_reference_tbl_rs_v FOREIGN KEY (vpc_id) REFERENCES public.tbl_vpc(vpc_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_security_group_rule fk_tbl_secu_reference_tbl_ip_p; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_security_group_rule
    ADD CONSTRAINT fk_tbl_secu_reference_tbl_ip_p FOREIGN KEY (pool_id) REFERENCES public.tbl_ip_pool(pool_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_security_group_rule fk_tbl_secu_reference_tbl_secu; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_security_group_rule
    ADD CONSTRAINT fk_tbl_secu_reference_tbl_secu FOREIGN KEY (sg_id) REFERENCES public.tbl_security_group(sg_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_sg_vm_instance fk_tbl_sg_v_reference_tbl_secu; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_sg_vm_instance
    ADD CONSTRAINT fk_tbl_sg_v_reference_tbl_secu FOREIGN KEY (sg_id) REFERENCES public.tbl_security_group(sg_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_share fk_tbl_shar_reference_tbl_imag; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_share
    ADD CONSTRAINT fk_tbl_shar_reference_tbl_imag FOREIGN KEY (image_id) REFERENCES public.tbl_image(image_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_user_info fk_tbl_user_reference_tbl_bp_i; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_user_info
    ADD CONSTRAINT fk_tbl_user_reference_tbl_bp_i FOREIGN KEY (bp_id) REFERENCES public.tbl_bp_info(bp_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_user_role_info fk_tbl_user_reference_tbl_role; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_user_role_info
    ADD CONSTRAINT fk_tbl_user_reference_tbl_role FOREIGN KEY (role_id) REFERENCES public.tbl_role_info(role_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_user_sg fk_tbl_user_reference_tbl_secu; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_user_sg
    ADD CONSTRAINT fk_tbl_user_reference_tbl_secu FOREIGN KEY (sg_id) REFERENCES public.tbl_security_group(sg_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_user_role_info fk_tbl_user_reference_tbl_user; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_user_role_info
    ADD CONSTRAINT fk_tbl_user_reference_tbl_user FOREIGN KEY (user_id) REFERENCES public.tbl_user_info(user_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_vm_snap fk_tbl_vm_s_reference_tbl_vm_i; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_vm_snap
    ADD CONSTRAINT fk_tbl_vm_s_reference_tbl_vm_i FOREIGN KEY (vm_instance_id) REFERENCES public.tbl_vm_instance(vm_instance_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_volume fk_tbl_volu_reference_tbl_stor; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_volume
    ADD CONSTRAINT fk_tbl_volu_reference_tbl_stor FOREIGN KEY (storage_pool_id) REFERENCES public.tbl_storage_pool(storage_pool_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- Name: tbl_volume_snap fk_tbl_volu_reference_tbl_volu; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.tbl_volume_snap
    ADD CONSTRAINT fk_tbl_volu_reference_tbl_volu FOREIGN KEY (volume_id) REFERENCES public.tbl_volume(volume_id) ON UPDATE RESTRICT ON DELETE RESTRICT;


--
-- PostgreSQL database dump complete
--

