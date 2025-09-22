#!/usr/bin/bash

if [ $# -lt 3 ]; then
    echo "Usage: $0 start|stop [vm_uuid] [nbd_dev]"
    exit 1
fi

vm_uuid=$2
nbd_dev=$3

if [[ ${nbd_dev} == /dev/* ]]; then
    nbd_dev=${nbd_dev:5}
fi

if [ "$1" = "start" ]; then
    vmagent vm inject2 -U ${vm_uuid} --nbddev ${nbd_dev}
elif [ "$1" = "stop" ]; then
    vmagent vm eject2 -U ${vm_uuid} --nbddev ${nbd_dev}
fi
