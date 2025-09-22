-- supported platforms and roles
    insert into tbl_role_info (role_id, platform, role) values
    (1, 'APPSTORE', 'GUEST'),(2, 'APPSTORE', 'TENANT'),(3, 'APPSTORE', 'ADMIN'),
    (4, 'DEVELOPER', 'GUEST'),(5, 'DEVELOPER', 'TENANT'),(6, 'DEVELOPER', 'ADMIN'),
    (7, 'ECRM', 'GUEST'),(8, 'ECRM', 'TENANT'),(9, 'ECRM', 'ADMIN'),
    (10, 'ATP', 'GUEST'),(11, 'ATP', 'TENANT'),(12, 'ATP', 'ADMIN'),
    (13, 'UMS', 'GUEST'),(14, 'UMS', 'TENANT'),(15, 'UMS', 'ADMIN'),
    (16, 'AOS', 'GUEST'),(17, 'AOS', 'TENANT'),(18, 'AOS', 'ADMIN'),
    (19, 'STAT', 'GUEST'),(20, 'STAT', 'TENANT'),(21, 'STAT', 'ADMIN'),
    (22, 'BILL', 'GUEST'),(23, 'BILL', 'TENANT'),(24, 'BILL', 'ADMIN'),
    (25, 'IMS', 'GUEST'),(26, 'IMS', 'TENANT'),(27, 'IMS', 'ADMIN'),
    (28, 'CIS', 'GUEST'),(29, 'CIS', 'TENANT'),(30, 'CIS', 'ADMIN'),
    (100, 'ALL', 'GUEST'),
    (101, 'ALL', 'TENANT'),
    (102, 'ALL', 'TENANT_ADMIN'),
    (103, 'ALL', 'ADMIN')

    ON CONFLICT(role_id) do nothing;

-- add a guest user
    insert into tbl_user_info (user_id, user_name, password, phone, email, gender, is_allowed, create_time, update_time)
    values('de3565b1-a7c2-42b9-b281-3f032af29ff7', 'guest',
    '59756fda85ebddee6024d5cc0e6edcde3226693257a9c1eb662c56426b8a4f232b3d56c321adbd91', '13800000000', '13800000000@edgegallery.org', 1, true, now(), now())
    ON CONFLICT(user_id) do nothing;

-- add a admin user
    insert into tbl_user_info (user_id, user_name, password, phone, email, gender, is_allowed, create_time, update_time)
    values('39937079-99fe-4cd8-881f-04ca8c4fe09d', 'admin',
    '472645ad1af0101adaa6769cc865fec3b29fedeba6dc912900a59b1364b7a6bb17bb9a0575854547', '13800000001', '13800000001@edgegallery.org', 1, true, now(), now())
    ON CONFLICT(user_id) do nothing;

-- set the permissions for guest user
    insert into tbl_user_role_info (user_id, role_id) values
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 1),
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 4),
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 7),
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 10),
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 13),
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 16),
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 19),
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 22),
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 25),
    ('de3565b1-a7c2-42b9-b281-3f032af29ff7', 28)
    ON CONFLICT(user_id, role_id) do nothing;

    insert into tbl_user_role_info (user_id, role_id) values
    ('39937079-99fe-4cd8-881f-04ca8c4fe09d', 103)
    ON CONFLICT(user_id, role_id) do nothing;
