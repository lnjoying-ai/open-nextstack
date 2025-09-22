INSERT INTO "public"."tbl_flavor" ("flavor_id", "name", "type", "cpu", "mem", "root_disk", "create_time", "update_time") VALUES ('ddcd173d0ee24161b91097b96174ebc8', 'm1.medium', 1, 2, 4, 20, '2022-05-27 11:37:47.494+00', '2022-05-27 11:37:47.494+00');
INSERT INTO "public"."tbl_flavor" ("flavor_id", "name", "type", "cpu", "mem", "root_disk", "create_time", "update_time") VALUES ('34cb7cf6268e485586c49fad0d73a481', 'm1.small', 1, 1, 2, 10, '2022-05-27 11:20:35.905+00', '2022-05-27 11:20:35.905+00');
INSERT INTO "public"."tbl_flavor" ("flavor_id", "name", "type", "cpu", "mem", "root_disk", "create_time", "update_time") VALUES ('259a9e9ff4dd47e0870b3e798fd9e713', 'm1.large', 1, 16, 16, 100, '2022-08-02 01:51:47.423+00', '2022-08-02 01:51:47.423+00');
INSERT INTO "public"."tbl_flavor" ("flavor_id", "name", "type", "cpu", "mem", "root_disk", "create_time", "update_time") VALUES ('ac7337c1ba4747478c6275606712373d', 'm1.medium-x', 1, 4, 8, 40, '2022-08-15 06:59:27.795+00', '2022-08-15 06:59:27.795+00');
INSERT INTO "public"."tbl_flavor" ("flavor_id", "name", "type", "cpu", "mem", "root_disk", "create_time", "update_time") VALUES ('c458a7c68e0344de80b661110aab5302', 'm1.small-x', 1, 2, 2, 20, '2022-10-12 12:14:47.299+00', '2022-10-12 12:14:47.299+00');



INSERT INTO "public"."tbl_image" ("image_id", "file_id_from_agent", "user_id", "image_os_type", "image_os_vendor", "image_os_version", "image_name", "image_format", "agent_ip", "phase_status", "phase_info", "is_public", "create_time", "update_time", "description", "vm_instance_id", "image_base") VALUES ('aeead951c95b46c187050bdc136e5948', '77c547af-f087-416e-88b3-ed6e99fba019', NULL, 0, 1, '7-x86_64-GenericCloud', 'CentOS-7-x86_64-GenericCloud', 0, NULL, 1, NULL, 't', '2022-04-07 18:42:04+00', '2022-04-07 18:42:13+00', NULL, NULL, 'aeead951c95b46c187050bdc136e5948');
INSERT INTO "public"."tbl_image" ("image_id", "file_id_from_agent", "user_id", "image_os_type", "image_os_vendor", "image_os_version", "image_name", "image_format", "agent_ip", "phase_status", "phase_info", "is_public", "create_time", "update_time", "description", "vm_instance_id", "image_base") VALUES ('b0bbae563d1941b2-8399d5bb11b0e84d', '9e249623-d965-429e-94ad-0b54987631bb', NULL, 0, 2, '20.04-3', 'ubuntu-20.04-3', 0, NULL, 1, NULL, 't', '2022-07-07 08:41:20.302+00', '2022-07-07 08:41:20.302+00', NULL, NULL, 'b0bbae563d1941b2-8399d5bb11b0e84d');
INSERT INTO "public"."tbl_image" ("image_id", "file_id_from_agent", "user_id", "image_os_type", "image_os_vendor", "image_os_version", "image_name", "image_format", "agent_ip", "phase_status", "phase_info", "is_public", "create_time", "update_time", "description", "vm_instance_id", "image_base") VALUES ('fd8a73a357bf4c21af75690a813fe6ff', NULL, NULL, 0, 2, '20.04', 'ubuntu-20.04', 2, NULL, 1, NULL, 't', '2022-08-15 10:51:48+00', '2022-08-15 10:51:51+00', NULL, NULL, 'fd8a73a357bf4c21af75690a813fe6ff');
INSERT INTO "public"."tbl_image" ("image_id", "file_id_from_agent", "user_id", "image_os_type", "image_os_vendor", "image_os_version", "image_name", "image_format", "agent_ip", "phase_status", "phase_info", "is_public", "create_time", "update_time", "description", "vm_instance_id", "image_base") VALUES ('d485a9cc9a6a4781836674932a5d5755', NULL, NULL, 0, 1, '7.8', 'centos-7.8', 2, NULL, 1, NULL, 't', '2022-05-27 21:25:53+00', '2022-05-27 21:25:57+00', NULL, NULL, 'd485a9cc9a6a4781836674932a5d5755');
INSERT INTO "public"."tbl_image" ("image_id", "file_id_from_agent", "user_id", "image_os_type", "image_os_vendor", "image_os_version", "image_name", "image_format", "agent_ip", "phase_status", "phase_info", "is_public", "create_time", "update_time", "description", "vm_instance_id", "image_base") VALUES ('7917dda9466b4f65b3a56bb09bf320ce', NULL, NULL, 1, 0, '2019', 'windowsserver-2019', 2, NULL, 1, NULL, 't', '2022-11-24 10:54:25+00', '2022-11-24 10:54:28+00', NULL, NULL, '7917dda9466b4f65b3a56bb09bf320ce');




INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (1, 'APPSTORE', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (2, 'APPSTORE', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (3, 'APPSTORE', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (4, 'DEVELOPER', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (5, 'DEVELOPER', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (6, 'DEVELOPER', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (7, 'COMPUTE', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (8, 'COMPUTE', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (9, 'COMPUTE', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (10, 'NETWORK', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (11, 'NETWORK', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (12, 'NETWORK', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (13, 'STORAGE', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (14, 'STORAGE', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (15, 'STORAGE', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (16, 'UMS', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (17, 'UMS', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (18, 'UMS', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (19, 'STAT', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (20, 'STAT', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (21, 'STAT', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (22, 'BILL', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (23, 'BILL', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (24, 'BILL', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (25, 'IMAGE', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (26, 'IMAGE', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (27, 'IMAGE', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (28, 'CIS', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (29, 'CIS', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (30, 'CIS', 'ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (100, 'ALL', 'GUEST');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (101, 'ALL', 'TENANT');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (102, 'ALL', 'TENANT_ADMIN');
INSERT INTO "public"."tbl_role_info" ("role_id", "platform", "role") VALUES (103, 'ALL', 'ADMIN');

INSERT INTO "public"."tbl_user_info" ("user_id", "bp_id", "user_name", "password", "phone", "email", "address", "is_allowed", "gender", "status", "kind", "level", "create_time", "update_time") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', NULL, 'guest', '59756fda85ebddee6024d5cc0e6edcde3226693257a9c1eb662c56426b8a4f232b3d56c321adbd91', '13800000000', '13800000000@edgegallery.org', NULL, 't', 1, NULL, NULL, NULL, '2023-01-12 11:49:13.787285+00', '2023-01-12 11:49:13.787285+00');
INSERT INTO "public"."tbl_user_info" ("user_id", "bp_id", "user_name", "password", "phone", "email", "address", "is_allowed", "gender", "status", "kind", "level", "create_time", "update_time", "api_key") VALUES ('39937079-99fe-4cd8-881f-04ca8c4fe09d', NULL, 'admin', '0be4b3eb0c71fd9984f1d133490f63238a3bcdd0d9ccd6fb88c3c1550f13bfe3a833e92f19954da5', '13800000001', NULL, NULL, 't', 1, NULL, NULL, NULL, '2023-01-12 11:56:04.018953+00', '2023-01-12 11:56:04.018953+00', 'c4d9bbb5cf234330b03423316014b68e');

INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 1);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 4);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 7);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 10);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 13);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 16);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 19);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 22);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 25);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 28);
INSERT INTO "public"."tbl_user_role_info" ("user_id", "role_id") VALUES ('39937079-99fe-4cd8-881f-04ca8c4fe09d', 103);
