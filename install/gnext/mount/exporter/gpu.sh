#!/bin/bash

product=`dmidecode -t 1 | grep "Product Name" | awk -F ': ' '{print $2}'`
[ ${product} = "KVM" ] && /opt/nextstack/bin/nvidia_gpu_exporter || /opt/nextstack/bin/nvidia_gpu_exporter --run-on-host
