#!/bin/bash
source ./env_config.sh
./start_operation.sh
./start_ums.sh
#./start_compute.sh
./start_network.sh
./start_vm.sh
./start_bm.sh
./start_repo.sh
#./start_image.sh
./start_api.sh
./start_webgw.sh
#./start_stat.sh
#./start_bill.sh
#./start_scheduler.sh
