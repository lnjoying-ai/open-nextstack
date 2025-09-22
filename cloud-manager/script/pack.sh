#!/bin/bash

cd ~

sed -i 's/\r$//' ~/lnjoying_cloud/script/*

tar cvfz lnjoying_cloud.tar.gz lnjoying_cloud

sudo docker build -t lnjoying/lnjoying-cloud:v1.0.0 .

sudo docker-compose down

sudo rm -rf ~/mount/postgre/data/*

sudo docker-compose up -d

###初始化SQL脚本###

sudo docker cp ~/ums-default-data.sql lnjoying-postgres:/root/
sudo docker exec -it lnjoying-postgres psql -U postgres -d justicedb -f /root/ums-default-data.sql

sudo docker ps
