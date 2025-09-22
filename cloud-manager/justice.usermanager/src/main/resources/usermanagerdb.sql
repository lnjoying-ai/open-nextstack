/*==============================================================*/
/*==============================================================*/
/* DBMS name:      PostgreSQL 9.x                               */
/* Created on:     2021/5/22 20:25:50                           */
/*==============================================================*/
/*==============================================================*/

/*==============================================================*/
/*==============================================================*/
/* Table: tbl_bp_info                                           */
/*==============================================================*/
/*==============================================================*/
CREATE TABLE if not exists tbl_bp_info (
   bp_id                VARCHAR(64)          not null,
   master_id            VARCHAR(64)          null,
   bp_name              VARCHAR(64)          null,
   website              VARCHAR(64)          null,
   license_id           VARCHAR(64)          null,
   status               INT4                 null,
   contact_info        VARCHAR(512)         null,
   create_time          TIMESTAMP WITH TIME ZONE null default CURRENT_TIMESTAMP,
   update_time          TIMESTAMP WITH TIME ZONE null default CURRENT_TIMESTAMP,
   constraint PK_TBL_BP_INFO primary key (bp_id)
);

/*==============================================================*/
/*==============================================================*/
/* Table: tbl_container_inst_info                               */
/*==============================================================*/
/*==============================================================*/
CREATE TABLE if not exists tbl_container_inst_info (
   inst_id              VARCHAR(64)          not null,
   ref_id               VARCHAR(64)          null,
   user_id              VARCHAR(64)          null,
   node_id              VARCHAR(64)          null,
   region_id            VARCHAR(64)          null,
   site_id              VARCHAR(64)          null,
   container_name       VARCHAR(64)          null,
   image_name           VARCHAR(64)          null,
   status               INT4                 null,
   container_params     TEXT                 null,
   cpu_num              INT4                 null,
   mem_limit            INT8                 null,
   disk_limit           INT8                 null,
   transmit_band_limit  INT4                 null,
   recv_band_limit      INT4                 null,
   create_time          TIMESTAMP WITH TIME ZONE null default CURRENT_TIMESTAMP,
   update_time          TIMESTAMP WITH TIME ZONE null default CURRENT_TIMESTAMP,
   stop_time            TIMESTAMP WITH TIME ZONE null default CURRENT_TIMESTAMP,
   constraint PK_TBL_CONTAINER_INST_INFO primary key (inst_id)
);

/*==============================================================*/
/*==============================================================*/
/* Table: tbl_edge_compute_info                                 */
/*==============================================================*/
/*==============================================================*/
CREATE TABLE if not exists tbl_edge_compute_info (
   node_id              VARCHAR(64)          not null,
   node_name            VARCHAR(64)          null,
   region_id            VARCHAR(64)          null,
   host_name            VARCHAR(64)          null,
   host_public_ip       VARCHAR(64)          null,
   host_inner_ip        VARCHAR(64)          null,
   create_time          TIMESTAMP WITH TIME ZONE null default CURRENT_TIMESTAMP,
   update_time          TIMESTAMP WITH TIME ZONE null default CURRENT_TIMESTAMP,
   constraint PK_TBL_EDGE_COMPUTE_INFO primary key (node_id)
);

/*==============================================================*/
/*==============================================================*/
/* Table: tbl_role_info                                         */
/*==============================================================*/
/*==============================================================*/
CREATE TABLE if not exists tbl_role_info (
   role_id              BIGSERIAL            not null,
   platform             VARCHAR(20)          null,
   role                 VARCHAR(20)          null,
   constraint PK_TBL_ROLE_INFO primary key (role_id)
);

/*==============================================================*/
/*==============================================================*/
/* Table: tbl_user_info                                         */
/*==============================================================*/
/*==============================================================*/
CREATE TABLE if not exists tbl_user_info (
   user_id              VARCHAR(64)          not null,
   bp_id                VARCHAR(64)          null,
   user_name            VARCHAR(64)          null,
   password             VARCHAR(100)         null,
   phone                VARCHAR(32)          null,
   email                VARCHAR(64)          null,
   address              VARCHAR(512)         null,
   is_allowed             BOOL               null,
   gender               INT4                 null,
   status               INT4                 null,
   kind                 INT4                 null,
   level                INT4                 null,
   create_time          TIMESTAMP WITH TIME ZONE null default CURRENT_TIMESTAMP,
   update_time          TIMESTAMP WITH TIME ZONE null default CURRENT_TIMESTAMP,
   constraint PK_TBL_USER_INFO primary key (user_id)
);

/*==============================================================*/
/*==============================================================*/
/* Table: tbl_user_role_info                                    */
/*==============================================================*/
/*==============================================================*/
CREATE TABLE if not exists tbl_user_role_info (
   user_id              VARCHAR(64)          not null,
   role_id              INT8                 not null,
   constraint PK_TBL_USER_ROLE_INFO primary key (user_id, role_id)
);

