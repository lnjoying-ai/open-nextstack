#!/usr/bin/bash

if [ $# -lt 3 ]; then
    echo "Usage: $0 start|stop [uri] [nbd_dev]"
    exit 1
fi
uri=$2
# remove the leading / if exists
if [[ ${uri} == /* ]]; then
    uri=${uri:1}
fi

# uri is something like 31db53d8-67a7-4121-a261-96ff6831f39c?file=cn_windows_server_2019.iso&token=xxxx
# get box_uuid and file and token from uri
if [[ ${uri} == *\?* ]]; then
    box_uuid=${uri%%\?*}
    file=${uri#*\?file=}
    token=${file#*\&token=}
    file=${file%%\&*}

    logger -t nbd-hook "box_uuid=${box_uuid}"
    logger -t nbd-hook "file=${file}"
    logger -t nbd-hook "token=${token}"
else
    logger -t nbd-hook "uri=${uri} is invalid"
    exit 1
fi


nbd_dev=$3

if [[ ${nbd_dev} == /dev/* ]]; then
    nbd_dev=${nbd_dev:5}
fi

# if file /opt/nextstack/config.box.yml exists get manager_host from it
if [ -f /opt/nextstack/config.box.yml ]; then
    manager_http_scheme=$(grep manager_http_scheme /opt/nextstack/config.box.yml | awk '{print $2}')
    manager_host=$(grep manager_host /opt/nextstack/config.box.yml | awk '{print $2}')
    manager_http_port=$(grep manager_http_port /opt/nextstack/config.box.yml | awk '{print $2}')
    logger -t nbd-hook "manager_http_scheme=${manager_http_scheme}"
    logger -t nbd-hook "manager_host=${manager_host}"
    logger -t nbd-hook "manager_http_port=${manager_http_port}"
else
    logger -t nbd-hook "file /opt/nextstack/config.box.yml not found"
    exit 1
fi

if [ "$1" = "start" ]; then
    curl -k -X PUT ${manager_http_scheme}://${manager_host}:${manager_http_port}/v1/boxes/${box_uuid}/inject?token=${token} -H 'Content-Type: application/json' -d "{\"nbddev\":\"${nbd_dev}\",\"file\":\"${file}\"}"
elif [ "$1" = "stop" ]; then
    curl -k -X PUT ${manager_http_scheme}://${manager_host}:${manager_http_port}/v1/boxes/${box_uuid}/eject?token=${token} -H 'Content-Type: application/json' -d "{\"nbddev\":\"${nbd_dev}\", \"file\":\"${file}\"}"
fi
