#!/usr/bin/env python
import subprocess
import json
import time
import os
import sys


def get_ret(result):
    lines = result.stdout.splitlines()
    code = lines[0].decode()
    if code not in ["200", "202"]:
        cmd = " ".join(result.args)
        raise Exception("wrong http response code %s for cmd: %s" % (code, cmd))
    return json.loads(lines[1])


switches = [
    {
        "ip": "192.168.8.254",
        "username": "admin",
        "password": "lnjoying2021!@#",
        "slots": [
            # 192.168.8.202
            {"dev": "GigabitEthernet1/0/17", "mac": "28:80:23:9f:7d:63"}
        ],
        "type": "leafspine",
    }
]

firewalls = [{"ip": "192.168.8.253", "username": "admin", "password": "lnjoying2021!@#"}]
sflinks = [
    {"switch_ip": "192.168.8.254", "sdev": "GigabitEthernet1/0/23", "firewall_ip": "192.168.8.253", "fdev": "GE1/0/0"}
]

ip2uuid = {}

for switch in switches:
    cmd = "l3agent.py switch add --ip %s --username %s --password %s --type %s --vendor %s" % (
        switch["ip"],
        switch["username"],
        switch["password"],
        switch["type"],
        "h3c",
    )
    print(cmd)
    result = subprocess.run(
        cmd.split(),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=True,
        env=dict(os.environ, LANG="en_US.UTF-8"),
    )
    time.sleep(1)
    switch_uuid = get_ret(result).get("uuid")
    print("switch uuid: " + switch_uuid)
    ip2uuid[switch["ip"]] = switch_uuid
    for slot in switch.get("slots", []):
        cmd = "l3agent.py slot add --dev %s --mac %s --uuid %s" % (slot["dev"], slot["mac"], switch_uuid)
        print(cmd)
        result = subprocess.run(
            cmd.split(),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            check=True,
            env=dict(os.environ, LANG="en_US.UTF-8"),
        )
        time.sleep(1)


for firewall in firewalls:
    cmd = "l3agent.py firewall add --ip %s --username %s --password %s --vendor %s" % (
        firewall["ip"],
        firewall["username"],
        firewall["password"],
        "h3c",
    )
    print(cmd)
    result = subprocess.run(
        cmd.split(),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=True,
        env=dict(os.environ, LANG="en_US.UTF-8"),
    )
    time.sleep(1)
    firewall_uuid = get_ret(result).get("uuid")
    print("firewall uuid: " + firewall_uuid)
    ip2uuid[firewall["ip"]] = firewall_uuid


for sflink in sflinks:
    fuuid = ip2uuid[sflink["firewall_ip"]]
    suuid = ip2uuid[sflink["switch_ip"]]
    cmd = "l3agent.py sflink add --fuuid %s --suuid %s --fdev %s --sdev %s" % (
        fuuid,
        suuid,
        sflink["fdev"],
        sflink["sdev"],
    )
    print(cmd)
    subprocess.run(
        cmd.split(),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=True,
        env=dict(os.environ, LANG="en_US.UTF-8"),
    )
    time.sleep(1)

# 添加vpc
cmd = "l3agent.py vpc add --cidr %s" % ("10.0.0.0/8")
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)
vpc_uuid = get_ret(result).get("uuid")

cmd = "l3agent.py vpc add --cidr %s" % ("10.0.0.0/8")
cmd = "l3agent.py vpc get --uuid %s" % (vpc_uuid)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)
vpc_vlanid = get_ret(result).get("vlanid")

# 添加 subnet
cmd = "l3agent.py subnet add --cidr %s --uuid %s" % ("10.255.0.0/16", vpc_uuid)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)
subnet_uuid = get_ret(result).get("uuid")

cmd = "l3agent.py subnet get --uuid %s" % (subnet_uuid)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)
subnet_cidr = get_ret(result).get("cidr")

# 添加 port
cmd = "l3agent.py port add --agent_ip %s --agent_port %s --vm %s --uuid %s" % (
    "192.168.8.240",
    "8901",
    "kvm",
    subnet_uuid,
)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)
port_uuid = get_ret(result).get("uuid")

cmd = "l3agent.py port get --uuid %s" % (port_uuid)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)
ret = get_ret(result)

# 添加安全组
cmd = "l3agent.py sg add"
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)
sg_uuid = get_ret(result).get("uuid")

cmd = """l3agent.py sg update --uuid %s --rule \
         DIR:in;PRIORITY:1;PROTOCOL:tcp;PORT:22;ADDR:0.0.0.0/0;ACTION:accept  \
         DIR:in;PRIORITY:1;PROTOCOL:tcp;PORT:3389;ADDR:0.0.0.0/0;ACTION:accept \
         DIR:in;PRIORITY:1;PROTOCOL:icmp;PORT:all;ADDR:0.0.0.0/0;ACTION:accept \
         DIR:in;PRIORITY:100;PROTOCOL:all;PORT:all;ADDR:default;ACTION:accept \
         DIR:out;PRIORITY:100;PROTOCOL:all;PORT:all;ADDR:0.0.0.0/0;ACTION:accept""" % (
    sg_uuid
)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)

# 应用安全组
cmd = "l3agent.py port apply --sg %s --uuid %s" % (sg_uuid, port_uuid)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)

sys.exit(0)

# 添加虚拟机
cmd = "vmagent.py vm add -F m1.medium -V %s -M %s -J %s -X %s -I centos-7.8" % (
    vpc_vlanid,
    ret["mac"],
    ret["ip"],
    ret["ofport"],
)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(5)
vm_uuid = get_ret(result).get("uuid")

# 配置虚拟机
cmd = "vmagent.py vm inject --subnet %s  -G linux -O testvm1 -N cloud -S %s -Q %s --uuid %s" % (
    subnet_cidr,
    "admin1234!",
    "static",
    vm_uuid,
)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)

sys.exit(0)

# 添加虚拟机
cmd = "vmagent.py vm add -F m1.medium -V %s -M %s -J %s -X %s -I windowsserver-2019" % (
    vpc_vlanid,
    ret["mac"],
    ret["ip"],
    ret["ofport"],
)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(5)
vm_uuid = get_ret(result).get("uuid")

# 配置虚拟机
cmd = "vmagent.py vm inject --subnet %s  -G windows -O win1 -S %s -Q %s --uuid %s" % (
    subnet_cidr,
    "admin1234!",
    "static",
    vm_uuid,
)
print(cmd)
result = subprocess.run(
    cmd.split(), stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=True, env=dict(os.environ, LANG="en_US.UTF-8")
)
time.sleep(1)
