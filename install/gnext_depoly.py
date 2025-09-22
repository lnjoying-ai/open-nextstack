#!/usr/bin/env python
# -*- coding: utf-8 -*-
# This is a script to deploy gnext agents to network and computer nodes
import socket
import sys
import os
import argparse
import subprocess
import yaml
import re
import time
import ipaddress
from loguru import logger


class Settings:
    def __init__(self):
        self.config_file = "/opt/gnext/config.deploy.yml"
        self.config: dict = None


g_settings = Settings()
manager_dir = "/opt/gnext/mount"


def local_exec(cmd, mode="check_call"):
    _cmd = cmd
    logger.debug("cmd: %s" % (_cmd))
    # 执行命令返回错误码
    if mode == "call":
        return subprocess.call(_cmd, shell=True, env=dict(os.environ, LANG="en_US.UTF-8"))
    # 执行命令返回输出, 出错抛异常
    elif mode == "check_output":
        return subprocess.check_output(_cmd, shell=True, env=dict(os.environ, LANG="en_US.UTF-8")).decode()
    # 执行命令, 等待命令结束后返回进程对象，不抛异常
    elif mode == "run":
        return subprocess.run(_cmd, stdout=subprocess.PIPE, shell=True, env=dict(os.environ, LANG="en_US.UTF-8"))
    # 执行命令, 出错时抛异常，否则返回0
    else:
        return subprocess.check_call(_cmd, shell=True, env=dict(os.environ, LANG="en_US.UTF-8"))


def remote_exec(host, user, cmd, mode="check_call"):
    proxy = g_settings.config.get("proxy")
    if proxy:
        if cmd.find("sudo -S") != -1:
            cmd = cmd.replace(
                "sudo -S", "sudo -S http_proxy=http://localhost:%s https_proxy=http://localhost:%s" % (proxy, proxy)
            )
        else:
            cmd = "http_proxy=http://localhost:%s https_proxy=http://localhost:%s %s" % (proxy, proxy, cmd)
    _cmd = "ssh -o StrictHostKeyChecking=no %s@%s '%s'" % (user, host, cmd)
    return local_exec(_cmd, mode)


def scp(host, user, src, dst):
    cmd = "scp -o StrictHostKeyChecking=no -r %s %s@%s:%s" % (src, user, host, dst)
    local_exec(cmd)


def load_config(config_file):
    if config_file:
        g_settings.config_file = os.path.abspath(config_file)
    if not os.path.exists(g_settings.config_file):
        raise Exception("config file %s not exists" % (g_settings.config_file))
    with open(g_settings.config_file, "r", encoding="UTF-8") as file:
        g_settings.config = yaml.safe_load(file)
    gnext_src = g_settings.config.get("gnext_src")
    if not gnext_src or not os.path.exists(gnext_src):
        raise Exception("gnext_src %s not exists" % (gnext_src))


def init_log():
    dirname = os.path.dirname(g_settings.config_file)
    log_file = os.path.join(dirname, "log", "deploy.log")
    logger.add(sink=g_settings.config.get("log_file", log_file), rotation="100 MB", retention=10)


def add2bashrc(host, user, env):
    cmd = 'grep -F "%s" ~/.bashrc' % (env)

    rtn = remote_exec(host, user, cmd, mode="call")
    if rtn != 0:
        cmd = 'echo -e "%s" >> ~/.bashrc' % (env)
        remote_exec(host, user, cmd)


def disable_firewall(host, user):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        cmd = "systemctl stop firewalld"
        remote_exec(host, user, cmd)
        cmd = "systemctl disable firewalld"
        remote_exec(host, user, cmd)
    elif linux_dist == "ubuntu":
        cmd = "systemctl stop ufw"
        remote_exec(host, user, cmd)
        cmd = "systemctl disable ufw"
        remote_exec(host, user, cmd)


def disable_selinux(host, user):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        cmd = "setenforce 0"
        remote_exec(host, user, cmd, mode="call")
        cmd = 'sed -i -E "s/^\s*SELINUX=.*/SELINUX=disabled/g" /etc/selinux/config'
        remote_exec(host, user, cmd)
    elif linux_dist == "ubuntu":
        # AppArmor需要看看是否需要处理, ubuntu下没有selinux，不需要处理
        pass


def update_os(host, user):
    # TODO 需要支持本地源，以应对无法访问外网的情况
    # 修改为阿里源
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        cmd = 'cd /etc/yum.repos.d && find . -name "*.repo" |grep -v Sources# | xargs sed -i -E -e "s!^mirrorlist=!#mirrorlist=!g" -e "s!^#baseurl=http.?://mirror.centos.org/.contentdir!baseurl=https://mirrors.aliyun.com/centos!g"'
        remote_exec(host, user, cmd)

        # 启用PowerTools源
        cmd = 'sed -i -E "s!^enabled=.*!enabled=1!g" /etc/yum.repos.d/CentOS-Stream-PowerTools.repo'
        remote_exec(host, user, cmd)

        cmd = "yum install -y epel-release centos-release-nfs-ganesha30 vim-enhanced net-tools lsof"
        remote_exec(host, user, cmd)

        cmd = 'cd /etc/yum.repos.d && sed -i "s!^#baseurl=https://download.example/pub!baseurl=https://mirrors.aliyun.com!" epel*'
        remote_exec(host, user, cmd)

        cmd = 'cd /etc/yum.repos.d && sed -i "s!^metalink!#metalink!" epel*'
        remote_exec(host, user, cmd)

        cmd = "rpm -e runc --nodeps"
        remote_exec(host, user, cmd, mode="call")

        if g_settings.config.get("upgrade", False):
            cmd = "yum update -y"
            remote_exec(host, user, cmd)

        # 生成缓存
        cmd = "yum makecache"
        remote_exec(host, user, cmd)
    elif linux_dist == "ubuntu":
        cmd = 'sed -i -E "s!^deb \S+!deb http://mirrors.aliyun.com/ubuntu!" /etc/apt/sources.list'
        remote_exec(host, user, cmd)
        cmd = "DEBIAN_FRONTEND=noninteractive apt-get update -y; DEBIAN_FRONTEND=noninteractive apt-get --fix-broken install; DEBIAN_FRONTEND=noninteractive apt autoremove -y;"
        if g_settings.config.get("upgrade", False):
            cmd += "DEBIAN_FRONTEND=noninteractive apt-get upgrade -y"
        remote_exec(host, user, cmd)


def prepare_gnext_package():
    """
    获取gnext二进制文件路径
    返回: gnext二进制文件的绝对路径
    """
    gnext_src = g_settings.config.get("gnext_src")
    gnext_path = os.path.join(gnext_src, "gnext")

    if not os.path.exists(gnext_path):
        raise Exception(f"gnext binary not found at {gnext_path}")

    return os.path.abspath(gnext_path)
    
def prepare_openvswitch_package():
    ovs_version = "3.2.0"

    # 进入源代码目录
    gnext_src = g_settings.config.get("gnext_src")
    ovs_dir = os.path.join(gnext_src, "third_party/ovs")
    if not os.path.exists(ovs_dir):
        os.makedirs(ovs_dir)
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        ovs_rpm_dir = os.path.abspath(os.path.join(ovs_dir, "rpms"))
        if not os.path.exists(ovs_rpm_dir):
            os.makedirs(ovs_rpm_dir)

        # 检查是否已经有ovs rpm
        cmd = "find %s -name 'openvswitch-%s-*.x86_64.rpm'" % (ovs_rpm_dir, ovs_version)
        output = local_exec(cmd, mode="check_output")
        if output:
            lines = output.split("\n")
            if lines:
                ovs_rpm = lines[0]
                if os.path.exists(ovs_rpm):
                    return os.path.abspath(ovs_rpm)

        # 检查是否已经有ovs源码包
        ovs_src_pkg = os.path.abspath(os.path.join(ovs_dir, "openvswitch-%s.tar.gz" % (ovs_version)))
        if not os.path.exists(ovs_src_pkg):
            cmd = "cd %s && wget https://www.openvswitch.org/releases/openvswitch-%s.tar.gz" % (ovs_dir, ovs_version)
            local_exec(cmd)

        # 解压到临时目录
        cmd = "cd /tmp && rm -rf openvswitch-%s && tar xvfz %s" % (ovs_version, ovs_src_pkg)
        local_exec(cmd)

        # 安装依赖包
        cmd = 'yum install -y @"Development Tools" rpm-build yum-utils python3-sphinx groff libbpf-devel libcap-ng-devel libxdp-devel numactl-devel python3-devel unbound'
        local_exec(cmd)

        # 修改spec文件
        cmd = 'cd /tmp/openvswitch-%s && sed -i -E "s/python-six/python3-six/g" ./rhel/openvswitch.spec' % (
            ovs_version
        )
        local_exec(cmd)

        # 编译rpm
        cmd = (
            "cd /tmp/openvswitch-%s && yum-builddep -y ./rhel/openvswitch.spec && ./boot.sh && ./configure --prefix=/usr --localstatedir=/var --sysconfdir=/etc && make -j$(nproc) rpm-fedora"
            % (ovs_version)
        )
        local_exec(cmd)

        # copy rpms to ovs_rpm_dir
        cmd = "cp -r /tmp/openvswitch-%s/rpm/rpmbuild/RPMS/* %s" % (ovs_version, ovs_rpm_dir)
        local_exec(cmd)

        cmd = "find %s -name 'openvswitch-%s-*.x86_64.rpm'" % (ovs_rpm_dir, ovs_version)
        output = local_exec(cmd, mode="check_output")
        if output:
            lines = output.split("\n")
            if lines:
                ovs_rpm = lines[0]
                if os.path.exists(ovs_rpm):
                    return os.path.abspath(ovs_rpm)
        raise Exception("openvswitch rpm not found")
    elif linux_dist == "ubuntu":
        ovs_deb_dir = os.path.abspath(os.path.join(ovs_dir, "debs"))
        pkg_list = []
        if os.path.exists(ovs_deb_dir):
            # 检查openvswitch-common，openvswitch-switch，python3-openvswitch这 3个包是否存在
            pkgs = ["openvswitch-common", "openvswitch-switch", "python3-openvswitch"]
            for pkg in pkgs:
                cmd = "ls %s/%s_*.deb" % (ovs_deb_dir, pkg)
                output = local_exec(cmd, mode="check_output")
                if output:
                    lines = output.split("\n")
                    if lines:
                        ovs_deb = lines[0]
                        if os.path.exists(ovs_deb):
                            pkg_list.append(os.path.abspath(ovs_deb))
            return pkg_list


def install_libvirt(host, user):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        cmd = "yum install -y libvirt virt-viewer virt-install libguestfs-tools"
        remote_exec(host, user, cmd)
    elif linux_dist == "ubuntu":
        cmd = "DEBIAN_FRONTEND=noninteractive apt-get install -y qemu-kvm virtinst libvirt-daemon virt-manager cloud-image-utils"
        remote_exec(host, user, cmd)

    cmd = "usermod --append --groups libvirt root"
    remote_exec(host, user, cmd)

    cmd = "systemctl enable libvirtd"
    remote_exec(host, user, cmd)

    cmd = "systemctl restart libvirtd"
    remote_exec(host, user, cmd)


def install_fmvm(host, user, gnext):
    fm_ip = gnext.get("fm_ip")
    if not fm_ip:
        return
    fm_script = os.path.join(g_settings.config.get("gnext_src"), "scripts", "fm_vm.pl")
    if not os.path.exists(fm_script):
        raise Exception("fm script %s not found" % (fm_script))
    scp(host, user, fm_script, "/tmp")
    proxy = g_settings.config.get("proxy", "0")
    cmd = "perl /tmp/%s '%s' %s" % (os.path.basename(fm_script), fm_ip, proxy)
    remote_exec(host, user, cmd)
    cmd = "rm -f /tmp/%s" % (os.path.basename(fm_script))


def install_nginx(host, user, node):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        cmd = "yum install -y nginx"
        remote_exec(host, user, cmd)
        nginx_user = "nginx"
    elif linux_dist == "ubuntu":
        cmd = "DEBIAN_FRONTEND=noninteractive apt-get install -y nginx"
        remote_exec(host, user, cmd)
        nginx_user = "www-data"

    cmd = "systemctl enable nginx"
    remote_exec(host, user, cmd)

    nginx_conf = """
user %s;
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /run/nginx.pid;

include /usr/share/nginx/modules/*.conf;

events {
    worker_connections 1024;
}

http {
    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;

    sendfile            on;
    tcp_nopush          on;
    tcp_nodelay         on;
    keepalive_timeout   65;
    types_hash_max_size 2048;

    include             /etc/nginx/mime.types;
    default_type        application/octet-stream;

    include /etc/nginx/conf.d/*.conf;

    server {
        listen       8901;
        location /v1/phonehome/ {
            proxy_pass http://127.0.0.1:%s;
        }
    }
}
""" % (
        nginx_user,
        8899,
    )
    tmp_nginx_conf = "/tmp/nginx.conf"
    with open(tmp_nginx_conf, "w", encoding="UTF-8") as file:
        file.write(nginx_conf)
    scp(host, user, tmp_nginx_conf, "/etc/nginx/nginx.conf")
    cmd = "rm -f %s" % (tmp_nginx_conf)
    cmd = "systemctl restart nginx"
    remote_exec(host, user, cmd)


def install_ovs(host, user, ovs_pkg):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        scp(host, user, ovs_pkg, "/tmp")

        cmd = "yum install -y /tmp/%s" % (os.path.basename(ovs_pkg))
        remote_exec(host, user, cmd)

        cmd = "rm -rf /tmp/%s" % (os.path.basename(ovs_pkg))
        remote_exec(host, user, cmd)
        cmd = "systemctl enable openvswitch"
        remote_exec(host, user, cmd)

        cmd = "systemctl restart openvswitch"
        remote_exec(host, user, cmd)
    elif linux_dist == "ubuntu":
        pkg_list = []
        pkgs = ovs_pkg
        for pkg in pkgs:
            scp(host, user, pkg, "/tmp")
            pkg_list.append("/tmp/%s" % (os.path.basename(pkg)))

        cmd = "DEBIAN_FRONTEND=noninteractive apt-get install -y %s" % (
            " ".join(["libunbound8", "python3-sortedcontainers"])
        )
        remote_exec(host, user, cmd, mode="call")

        cmd = "dpkg -i %s" % (" ".join(pkg_list))
        remote_exec(host, user, cmd)

        cmd = "rm -rf %s" % (" ".join(pkg_list))
        remote_exec(host, user, cmd)

        cmd = "systemctl enable ovs-vswitchd"
        remote_exec(host, user, cmd)

        cmd = "systemctl enable ovsdb-server"
        remote_exec(host, user, cmd)

        cmd = "systemctl restart ovs-vswitchd"
        remote_exec(host, user, cmd)

        cmd = "systemctl restart ovsdb-server"
        remote_exec(host, user, cmd)


def install_python(host, user):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        # install python39 if not exists
        cmd = "rpm -e --nodeps python39"
        remote_exec(host, user, cmd, mode="call")
        cmd = "yum install -y python39"
        remote_exec(host, user, cmd)
        cmd = "rm -rf /opt/.venv"
        remote_exec(host, user, cmd)
        cmd = "mkdir -p /opt"
        remote_exec(host, user, cmd)
        cmd = "/usr/bin/python3.9 -m venv /opt/.venv"
        remote_exec(host, user, cmd)

    elif linux_dist == "ubuntu":
        cmd = "DEBIAN_FRONTEND=noninteractive apt-get install -y python3.10-venv"
        remote_exec(host, user, cmd, mode="call")
        cmd = "mkdir -p /opt"
        remote_exec(host, user, cmd)
        cmd = "/usr/bin/python3.10 -m venv /opt/.venv"
        remote_exec(host, user, cmd)

    # TODO 需要支持本地源，以应对无法访问外网的情况
    proxy = g_settings.config.get("proxy")
    if proxy:
        cmd = (
            "source /opt/.venv/bin/activate && pip install --proxy http://localhost:%s wheel setuptools websockify -i https://pypi.tuna.tsinghua.edu.cn/simple"
            % (proxy)
        )
    else:
        cmd = "source /opt/.venv/bin/activate && pip install wheel setuptools websockify -i https://pypi.tuna.tsinghua.edu.cn/simple"
    remote_exec(host, user, cmd)

    envs = [
        r"if [ -f /opt/.venv/bin/activate ]; then source /opt/.venv/bin/activate; fi",
        r"export LANG=en_US.UTF-8",
        r"set -o vi",
        r"alias vi=vim",
        r"export PATH=/opt/gnext/bin:/opt/.venv/bin:\$PATH",
        r"export GNEXT_HOST=" + host,
        r"export ETCDCTL_ENDPOINTS=10.254.30.11:2379,10.254.30.12:2379,10.254.30.13:2379",
    ]
    for env in envs:
        add2bashrc(host, user, env)


def install_agents(host, user, agents_pkg):
    cmd = "mkdir -p /opt/gnext/{bin,db,log,nfs,cache}"
    remote_exec(host, user, cmd)

    cmd = "systemctl stop lnjoying-nbd.service"
    remote_exec(host, user, cmd, mode="call")

    cmd = "systemctl stop dnsmasq.service"
    remote_exec(host, user, cmd, mode="call")

    cmd = "systemctl disable dnsmasq.service"
    remote_exec(host, user, cmd, mode="call")


    # 传输gnext二进制文件
    # 备份已存在的etcd二进制文件
    cmd = """
    if [ -d /opt/gnext ]; then
        mv /opt/gnext /opt/gnext.bak.$(date +%Y%m%d_%H%M%S)
    fi
    """
    remote_exec(host, user, cmd)

    scp(host, user, agents_pkg, "/opt/")

    # 添加环境变量
    envs = [r"alias cdd=\"cd /opt/gnext\""]
    for env in envs:
        add2bashrc(host, user, env)
    
    
    

def install_nbd(host, user, nbd_lisen_ip="0.0.0.0", nbd_lisen_port=8000):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        cmd = "yum install -y nbd"
        remote_exec(host, user, cmd)
    elif linux_dist == "ubuntu":
        cmd = "DEBIAN_FRONTEND=noninteractive apt-get install -y nbd-server nbd-client"
        remote_exec(host, user, cmd)

    cmd = "echo 'options nbd max_part=255' >/etc/modprobe.d/nbd.conf"
    remote_exec(host, user, cmd)

    cmd = "echo 'nbd' >/etc/modules-load.d/nbd.conf"
    remote_exec(host, user, cmd)

    cmd = "modprobe nbd"
    remote_exec(host, user, cmd)

    gnext_src = g_settings.config.get("gnext_src")
    nbdscript = os.path.join(gnext_src, "scripts", "lnjoying-nbd.service")
    if not os.path.exists(nbdscript):
        raise Exception("nbd script %s not found" % (nbdscript))

    cmd = "rm -f /tmp/%s && cp %s /tmp" % (os.path.basename(nbdscript), nbdscript)
    local_exec(cmd)
    # 替换nbd监听ip和端口
    cmd = 'sed -i -E "s/--address=\S+/--address=%s/g" /tmp/%s' % (nbd_lisen_ip, os.path.basename(nbdscript))
    local_exec(cmd)

    cmd = 'sed -i -E "s/--port=\S+/--port=%s/g" /tmp/%s' % (nbd_lisen_port, os.path.basename(nbdscript))
    local_exec(cmd)

    scp(host, user, "/tmp/%s" % (os.path.basename(nbdscript)), "/usr/lib/systemd/system/")

    cmd = "chmod 644 /usr/lib/systemd/system/%s && systemctl enable %s && systemctl restart %s" % (
        os.path.basename(nbdscript),
        os.path.basename(nbdscript),
        os.path.basename(nbdscript),
    )
    remote_exec(host, user, cmd)

    cmd = "rm -f /tmp/%s" % (os.path.basename(nbdscript))
    local_exec(cmd)


def config_ssh(host, user, passwd):
    if not os.path.exists(os.path.expanduser("~/.ssh/id_rsa.pub")):
        cmd = "ssh-keygen -t rsa -P '' -f ~/.ssh/id_rsa"
        local_exec(cmd)
    cmd = "sshpass -p '%s' ssh-copy-id -o StrictHostKeyChecking=no %s@%s" % (passwd, user, host)
    local_exec(cmd)
    # 如果user不是root，检查是否有sudo权限
    if user != "root":
        linux_dist = g_settings.config.get("linux_dist", "centos")
        if linux_dist == "centos":
            cmd = "echo %s| sudo -S yum install -y sshpass" % (passwd)
        elif linux_dist == "ubuntu":
            cmd = "echo %s| sudo -S DEBIAN_FRONTEND=noninteractive apt --fix-broken -y install" % (passwd)
            remote_exec(host, user, cmd, mode="call")
            cmd = "echo %s| sudo -S DEBIAN_FRONTEND=noninteractive apt-get install -y sshpass" % (passwd)
        remote_exec(host, user, cmd, mode="call")
        # 使用sudo权限，设置免密码登录到root用户
        cmd = "cat ~/.ssh/id_rsa.pub | ssh %s@%s \"sshpass -p '%s' sudo mkdir -p /root/.ssh\"" % (user, host, passwd)
        local_exec(cmd)
        cmd = "cat ~/.ssh/id_rsa.pub | ssh %s@%s \"sshpass -p '%s' sudo tee /root/.ssh/authorized_keys\"" % (
            user,
            host,
            passwd,
        )
        local_exec(cmd)


# 安装节点需要能访问外网, 安装节点启动代理，然后建立ssh隧道，让目标节点能访问外网
def config_proxy(host, user, proxy):
    if not proxy:
        return

    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        cmd = "yum install -y squid;systemctl enable squid;systemctl start squid"
        local_exec(cmd, mode="call")
    elif linux_dist == "ubuntu":
        cmd = "DEBIAN_FRONTEND=noninteractive apt-get install -y squid;systemctl enable squid;systemctl start squid"
        local_exec(cmd, mode="call")

    cmd = "ps -ef |grep %s|grep %s|awk '{print $2}'|xargs kill -9" % (proxy, host)
    local_exec(cmd, mode="call")
    cmd = (
        "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -fN -R 0.0.0.0:%s:127.0.0.1:%s %s@%s"
        % (proxy, proxy, user, host)
    )
    local_exec(cmd)


def config_dns(host, user, dns):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        tmp_dnsconf = "/tmp/90-dns-none.conf"
        with open(tmp_dnsconf, "w", encoding="UTF-8") as file:
            file.write("[main]\ndns=none\n")
        scp(host, user, tmp_dnsconf, "/etc/NetworkManager/conf.d/90-dns-none.conf")
        cmd = "rm -f %s" % (tmp_dnsconf)
        local_exec(cmd)

        cmd = "systemctl reload NetworkManager"
        remote_exec(host, user, cmd)

        tmp_resolvconf = "/tmp/resolv.conf"
        with open(tmp_resolvconf, "w", encoding="UTF-8") as file:
            print("%s" % (type(dns)))
            if type(dns) is list or type(dns) is tuple:
                for d in dns:
                    file.write("nameserver %s\n" % (d))
            else:
                file.write("nameserver %s\n" % (dns))
        scp(host, user, tmp_resolvconf, "/etc/resolv.conf")
        cmd = "rm -f %s" % (tmp_resolvconf)
        local_exec(cmd)
    elif linux_dist == "ubuntu":
        pass


def config_nic(host, user, nic, ip=None, prefix=None, gateway=None):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        # 检查网卡是否存在
        cmd = "ip link show %s" % (nic)
        remote_exec(host, user, cmd)

        # 生成网卡配置文件
        tmp_nic_cfg = "/tmp/ifcfg-%s" % (nic)
        if ip:
            if not prefix:
                raise Exception("ip is set, but prefix or gateway is not set")
            with open(tmp_nic_cfg, "w", encoding="UTF-8") as file:
                file.write("DEVICE=%s\n" % (nic))
                file.write("BOOTPROTO=static\n")
                file.write("ONBOOT=yes\n")
                file.write("IPADDR=%s\n" % (ip))
                file.write("PREFIX=%s\n" % (prefix))
                if gateway:
                    file.write("GATEWAY=%s\n" % (gateway))
        else:
            with open(tmp_nic_cfg, "w", encoding="UTF-8") as file:
                file.write("DEVICE=%s\n" % (nic))
                file.write("BOOTPROTO=none\n")
                file.write("ONBOOT=yes\n")
        scp(host, user, tmp_nic_cfg, "/etc/sysconfig/network-scripts/ifcfg-%s" % (nic))
        cmd = "rm -f %s" % (tmp_nic_cfg)
        local_exec(cmd)
        # 使网卡配置文件生效
        cmd = "nmcli connection reload"
        remote_exec(host, user, cmd)
    elif linux_dist == "ubuntu":
        cmd = "ip link show %s" % (nic)
        output = remote_exec(host, user, cmd, mode="check_output")
        mac = None
        for line in output.split("\n"):
            if "link/ether" in line:
                # get mac address from line
                mac = line.split()[1]
                logger.debug("mac: %s" % (mac))
                break
        if mac:
            # 生成yaml格式的netplan配置文件
            tmp_netplan_cfg = "/tmp/%s-config.yaml" % (nic)
            with open(tmp_netplan_cfg, "w", encoding="UTF-8") as file:
                file.write("network:\n")
                file.write("  version: 2\n")
                file.write("  renderer: networkd\n")
                file.write("  ethernets:\n")
                file.write("    %s:\n" % (nic))
                if ip:
                    file.write("      dhcp4: no\n")
                    file.write("      dhcp6: no\n")
                    file.write("      addresses: [%s/%s]\n" % (ip, prefix))
                    if gateway:
                        file.write("      gateway4: %s\n" % (gateway))
                    file.write("      match:\n")
                    file.write("        macaddress: %s\n" % (mac))
                else:
                    file.write("      dhcp4: no\n")
                    file.write("      dhcp6: no\n")
                    file.write("      match:\n")
                    file.write("        macaddress: %s\n" % (mac))
            scp(host, user, tmp_netplan_cfg, "/etc/netplan/%s-config.yaml" % (nic))
            cmd = "rm -f %s" % (tmp_netplan_cfg)
            local_exec(cmd)
            # 使网卡配置文件生效
            cmd = "chmod 600 /etc/netplan/*.yaml;netplan apply"
            remote_exec(host, user, cmd)


def assign_ha_uuids():
    """为所有gnext节点分配uuid和ha_uuid
    
    逻辑：
    - 如果节点没有uuid，自动生成一个
    - 如果有多个节点，每个节点的ha_uuid为另一个节点的uuid
    - 如果只有一个节点，ha_uuid设置为None
    """
    import uuid
    
    # 获取所有gnext节点
    gnext_nodes = []
    for node in g_settings.config.get("nodes", []):
        gnext_config = node.get("gnext")
        if gnext_config is not None:  # 只要有gnext配置就算，即使为空字典
            gnext_nodes.append(node)
    
    if not gnext_nodes:
        logger.info("没有找到gnext节点")
        return
    
    logger.info(f"找到 {len(gnext_nodes)} 个gnext节点")
    
    # 为每个节点分配uuid（如果没有的话）
    for node in gnext_nodes:
        gnext = node.get("gnext")
        if not gnext.get("uuid"):
            gnext["uuid"] = str(uuid.uuid4())
            logger.info(f"为节点 {node.get('host')} 生成UUID: {gnext['uuid']}")
    
    # 分配ha_uuid
    if len(gnext_nodes) == 1:
        # 单节点情况，ha_uuid设置为None
        gnext_nodes[0]["gnext"]["ha_uuid"] = None
        logger.info(f"单节点部署，节点 {gnext_nodes[0].get('host')} 的ha_uuid设置为None")
    elif len(gnext_nodes) == 2:
        # 两个节点，互相设置为对方的uuid
        node1_uuid = gnext_nodes[0]["gnext"]["uuid"]
        node2_uuid = gnext_nodes[1]["gnext"]["uuid"]
        
        gnext_nodes[0]["gnext"]["ha_uuid"] = node2_uuid
        gnext_nodes[1]["gnext"]["ha_uuid"] = node1_uuid
        
        logger.info(f"节点 {gnext_nodes[0].get('host')} (uuid: {node1_uuid}) 的ha_uuid设置为: {node2_uuid}")
        logger.info(f"节点 {gnext_nodes[1].get('host')} (uuid: {node2_uuid}) 的ha_uuid设置为: {node1_uuid}")
    else:
        # 多个节点情况，采用循环分配策略
        for i, node in enumerate(gnext_nodes):
            # 获取下一个节点的uuid作为当前节点的ha_uuid
            next_index = (i + 1) % len(gnext_nodes)
            ha_uuid = gnext_nodes[next_index]["gnext"]["uuid"]
            node["gnext"]["ha_uuid"] = ha_uuid
            
            logger.info(f"节点 {node.get('host')} (uuid: {node['gnext']['uuid']}) 的ha_uuid设置为: {ha_uuid}")


def config_gnext(node):
    gnext = node.get("gnext")
    if not gnext:
        return
    if not gnext.get("agent_ip"):
        gnext["agent_ip"] = node.get("host")
    if not gnext.get("agent_port"):
        gnext["agent_port"] = 8899
    if not gnext.get("log_level"):
        gnext["log_level"] = "debug"
    # uuid和ha_uuid现在由assign_ha_uuids()函数统一分配
    if not gnext.get("l3_mode"):
        gnext["l3_mode"] = True
    if not gnext.get("vm_mode"):
        gnext["vm_mode"] = True
    # 配置网卡
    # config_nic(node.get("host"), node.get("user"), gnext["lan_nic"])
    # config_nic(node.get("host"), node.get("user"), gnext["wan_nic"])

    # 创建/vms子目录
    cmd = "mkdir -p /vms/zh_data/vms/{backing,cdrom,migrate,volumes,tokens}"
    remote_exec(node.get("host"), node.get("user"), cmd)
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        cmd = "chown -R qemu:qemu /vms/zh_data/vms"
        remote_exec(node.get("host"), node.get("user"), cmd)
    elif linux_dist == "ubuntu":
        cmd = "chown -R libvirt-qemu:libvirt-qemu /vms/zh_data/vms"
        remote_exec(node.get("host"), node.get("user"), cmd)

    if "gpu" in gnext:
        # 配置直通GPU
        vfio_ids = gnext["gpu"].get(
            "vfio_ids", "10de:2684,10de:22ba,10de:26ba,10de:2330,10de:2324,10de:1c82,10de:22a3,8086:1bb0,11f8:4128"
        )
        hugepages = gnext["gpu"].get("hugepages", 0)
        gpu_script = os.path.join(g_settings.config.get("gnext_src"), "scripts", "gpu.pl")
        if not os.path.exists(gpu_script):
            raise Exception("gpu script %s not found" % (gpu_script))
        if gnext.get("fm_ip"):
            libnvfm_file = os.path.join(g_settings.config.get("gnext_src"), "bin", "libnvfm.so.1")
            scp(node.get("host"), node.get("user"), libnvfm_file, "/lib/x86_64-linux-gnu/")
        scp(node.get("host"), node.get("user"), gpu_script, "/tmp")
        cmd = "perl /tmp/%s '%s' %s" % (os.path.basename(gpu_script), vfio_ids, hugepages)
        remote_exec(node.get("host"), node.get("user"), cmd)
        cmd = "rm -f /tmp/%s" % (os.path.basename(gpu_script))
        remote_exec(node.get("host"), node.get("user"), cmd)
        cmd = "reboot"
        remote_exec(node.get("host"), node.get("user"), cmd, mode="call")
        # 等待重启完成
        time.sleep(5)
        proxy = g_settings.config.get("proxy")
        while True:
            try:
                remote_exec(node.get("host"), node.get("user"), "uptime")
                if proxy:
                    cmd = (
                        "ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -fN -R 0.0.0.0:%s:127.0.0.1:%s %s@%s"
                        % (proxy, proxy, node.get("user"), node.get("host"))
                    )
                    local_exec(cmd)
                break
            except:
                time.sleep(10)

    # lnjoying-gnext.service
    gnextservice = os.path.join(g_settings.config.get("gnext_src"), "scripts", "lnjoying-gnext.service")
    if not os.path.exists(gnextservice):
        raise Exception("gnext service %s not found" % (gnextservice))
    scp(node.get("host"), node.get("user"), gnextservice, "/usr/lib/systemd/system/")

    # lnjoying-loopvolume.service
    #loopvolume_service = os.path.join(g_settings.config.get("gnext_src"), "scripts", "lnjoying-loopvolume.service")
    #if not os.path.exists(loopvolume_service):
    #    raise Exception("loop volume script %s not found" % (loopvolume_service))
    #scp(node.get("host"), node.get("user"), loopvolume_service, "/usr/lib/systemd/system/")

    # lnjoying-novnc.service
    novnc_service = os.path.join(g_settings.config.get("gnext_src"), "scripts", "lnjoying-novnc.service")
    if not os.path.exists(novnc_service):
        raise Exception("novnc script %s not found" % (novnc_service))
    cmd = "cp %s /tmp" % (novnc_service)
    local_exec(cmd)
    # 替换novnc监听目录为agent uuid
    cmd = 'sed -i -E "s/tokens\/\S+\s+6090/tokens\/%s 6090/" /tmp/%s' % (
        gnext["uuid"],
        os.path.basename(novnc_service),
    )
    local_exec(cmd)
    scp(node.get("host"), node.get("user"), "/tmp/%s" % (os.path.basename(novnc_service)), "/usr/lib/systemd/system/")
    cmd = "rm -f /tmp/%s" % (os.path.basename(novnc_service))
    local_exec(cmd)

    # 加载新的systemd配置
    cmd = "systemctl daemon-reload"
    remote_exec(node.get("host"), node.get("user"), cmd)
    for service in ["lnjoying-gnext", "lnjoying-novnc"]:
        cmd = "systemctl enable %s" % (service)
        remote_exec(node.get("host"), node.get("user"), cmd)
        cmd = "systemctl restart %s" % (service)

    if g_settings.config.get("exporter"):
        config_exporter(node.get("host"), node.get("user"))
        if "gpu" in gnext:
            config_gpu_exporter(node.get("host"), node.get("user"))
    if "gpu" in gnext:
        del gnext["gpu"]
    # 保存gnext配置文件
    tmp_config_file = "/tmp/ndeploy.gnext.yml"
    with open(tmp_config_file, "w", encoding="UTF-8") as file:
        yaml.safe_dump(gnext, file)
    scp(node.get("host"), node.get("user"), tmp_config_file, "/opt/gnext/config.yml")
    cmd = "rm -f /tmp/ndeploy.gnext.yml"
    local_exec(cmd)


def config_hosts(host, user, name, ip):
    cmd = "cat /etc/hosts"
    logger.debug("cmd: %s" % (cmd))
    output = remote_exec(host, user, cmd, mode="check_output")
    print(output)
    for line in output.strip().split("\n"):
        line = line.strip()
        if not line:
            continue
        parts = line.split()
        if parts[0] == ip:
            if name in parts[1:]:
                return
            else:
                raise Exception("ip %s already used by %s" % (ip, parts[1]))
        else:
            if name in parts[1:]:
                raise Exception("name %s already used by %s" % (name, parts[0]))
    cmd = "echo '%s %s' >> /etc/hosts" % (ip, name)
    remote_exec(host, user, cmd)


def install_vnc(host, user, vnc_passwd):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        cmd = 'yum groupinstall -y "Server with GUI" --skip-broken'
        remote_exec(host, user, cmd)
        cmd = "yum install -y tigervnc-server"
        remote_exec(host, user, cmd)
        # set vncpasswd with non-interactive mode
        cmd = 'mkdir -p /root/.vnc && echo "%s" | vncpasswd -f >/root/.vnc/passwd' % (vnc_passwd)
        remote_exec(host, user, cmd)
        cmd = "chmod 400 /root/.vnc/passwd"
        remote_exec(host, user, cmd)

        cfg = ["session=gnome", "geometry=1920x1200", "localhost=no", "alwaysshared"]

        with open("/tmp/config", "w") as f:
            f.write("\n".join(cfg))
        scp(host, user, "/tmp/config", "/root/.vnc/config")
        cmd = "rm -f /tmp/config"
        local_exec(cmd)

        cmd = "echo ':1=root' > /etc/tigervnc/vncserver.users"
        remote_exec(host, user, cmd)

        cmd = "systemctl enable vncserver@:1"
        remote_exec(host, user, cmd)

        cmd = "systemctl restart vncserver@:1"
        remote_exec(host, user, cmd)
    elif linux_dist == "ubuntu":
        # TODO 为ubuntu配置vnc
        pass

def config_locale(host, user):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "ubuntu":
        cmd = "locale-gen en_US.UTF-8;locale-gen en_DK.UTF-8"
        remote_exec(host, user, cmd)
    

def config_basic():
    dns = g_settings.config.get("dns", ("114.114.114.114", "114.114.115.115"))
    proxy = g_settings.config.get("proxy")
    for node in g_settings.config.get("nodes"):
        host = node.get("host")
        user = node.get("user")
        passwd = node.get("passwd")
        # 设置无密码登录
        config_ssh(host, user, passwd)
        # 禁用防火墙
        if user != "root":
            node["user"] = "root"
            user = "root"
        disable_firewall(host, user)
        # 禁用selinux
        disable_selinux(host, user)
        # 配置locale
        config_locale(host, user)
        # 配置代理
        #config_proxy(host, user, proxy)
        # 配置dns
        #config_dns(host, user, dns)
        # 更新系统
        # update_os(host, user)
        # 设置python环境
        install_python(host, user)
        #install_docker(host, user)

# 通过配置etcd systemd服务，配置etcd集群
def config_etcd_cluster():
    # 从配置文件中获取包含etcd配置的节点
    nodes = g_settings.config.get("nodes", [])
    etcd_nodes = []
    
    # 筛选出配置了etcd的节点
    for node in nodes:
        if node.get("etcd"):
            etcd_nodes.append(node)
    
    if not etcd_nodes:
        logger.info("No etcd nodes found in configuration")
        return
    
    # 判断是单节点还是集群模式
    is_single_node = len(etcd_nodes) == 1
    
    if is_single_node:
        logger.info("Configuring etcd in single node mode")
    else:
        logger.info(f"Configuring etcd cluster with {len(etcd_nodes)} nodes")
    
    # 构建集群成员列表（单节点时只有一个成员）
    cluster_members = []
    for node in etcd_nodes:
        etcd_name = node["etcd"]["etcd_name"]
        host_ip = node["host"]
        cluster_members.append(f"{etcd_name}=http://{host_ip}:2385")
    
    initial_cluster = ",".join(cluster_members)
    
    # 为每个etcd节点配置服务
    for node in etcd_nodes:
        host = node["host"]
        user = node["user"]
        passwd = node.get("passwd")
        etcd_config = node["etcd"]
        etcd_name = etcd_config["etcd_name"]
        
        if is_single_node:
            logger.info(f"Configuring single etcd node on {host} with name {etcd_name}")
        else:
            logger.info(f"Configuring etcd cluster node on {host} with name {etcd_name}")
        
        # 如果需要密码验证，先配置SSH免密登录
        if passwd:
            config_ssh(host, user, passwd)
            if user != "root":
                user = "root"  # 切换到root用户进行后续操作
        
        # 下载并安装etcd
        install_etcd(host, user)
        
        # 生成etcd systemd服务文件
        # 单节点模式和集群模式的配置略有不同
        if is_single_node:
            # 单节点模式配置
            etcd_service_content = f"""[Unit]
Description=etcd key-value store (single node)
Documentation=https://github.com/etcd-io/etcd
After=network.target

[Service]
Type=notify
ExecStart=/usr/local/bin/etcd \\
  --name {etcd_name} \\
  --data-dir /var/lib/etcd \\
  --listen-client-urls http://{host}:2379,http://127.0.0.1:2379 \\
  --advertise-client-urls http://{host}:2379 \\
  --listen-peer-urls http://{host}:2385 \\
  --initial-advertise-peer-urls http://{host}:2385 \\
  --initial-cluster {etcd_name}=http://{host}:2385 \\
  --initial-cluster-token etcd-single-node \\
  --initial-cluster-state new \\
  --force-new-cluster
Restart=on-failure
RestartSec=5s
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
"""
        else:
            # 集群模式配置
            etcd_service_content = f"""[Unit]
Description=etcd key-value store (cluster node)
Documentation=https://github.com/etcd-io/etcd
After=network.target

[Service]
Type=notify
ExecStart=/usr/local/bin/etcd \\
  --name {etcd_name} \\
  --data-dir /var/lib/etcd \\
  --listen-peer-urls http://{host}:2385 \\
  --listen-client-urls http://{host}:2379,http://127.0.0.1:2379 \\
  --advertise-client-urls http://{host}:2379 \\
  --initial-advertise-peer-urls http://{host}:2385 \\
  --initial-cluster {initial_cluster} \\
  --initial-cluster-token etcd-cluster \\
  --initial-cluster-state new
Restart=on-failure
RestartSec=5s
LimitNOFILE=65536

[Install]
WantedBy=multi-user.target
"""
        
        # 将服务文件写入临时文件并传输到目标主机
        tmp_service_file = "/tmp/etcd.service"
        with open(tmp_service_file, "w", encoding="UTF-8") as f:
            f.write(etcd_service_content)
        
        # 传输服务文件到目标主机
        scp(host, user, tmp_service_file, "/usr/lib/systemd/system/etcd.service")
        
        # 创建etcd用户和数据目录
        setup_etcd_environment(host, user)
        
        # 开放etcd所需端口
        open_etcd_ports(host, user)
        
        # 重新加载systemd并启动etcd服务
        cmd = "systemctl daemon-reload"
        remote_exec(host, user, cmd)
        
        cmd = "systemctl enable etcd"
        remote_exec(host, user, cmd)
        
        cmd = "systemctl restart etcd"
        remote_exec(host, user, cmd)
        
        # 清理临时文件
        cmd = f"rm -f {tmp_service_file}"
        local_exec(cmd)
        
        if is_single_node:
            logger.info(f"Single etcd node configured and started on {host}")
        else:
            logger.info(f"Etcd cluster node configured and started on {host}")
    
    # 等待服务启动
    if is_single_node:
        time.sleep(5)  # 单节点启动更快
    else:
        time.sleep(10)  # 集群需要更多时间同步
    
    # 验证etcd状态
    verification_result = verify_etcd_cluster(etcd_nodes, is_single_node)
    if not verification_result:
        logger.error("Etcd cluster verification failed, but continuing with deployment...")


def open_etcd_ports(host, user):
    """开放etcd所需的端口（2379和2385）"""
    linux_dist = g_settings.config.get("linux_dist", "centos")
    
    if linux_dist == "centos":
        # CentOS/RHEL 使用 firewall-cmd
        cmd = "systemctl status firewalld"
        result = remote_exec(host, user, cmd, mode="call")
        
        if result == 0:  # firewalld is running
            logger.info(f"Opening etcd ports on {host} (CentOS/RHEL)")
            cmd = "firewall-cmd --permanent --add-port=2379/tcp --add-port=2385/tcp"
            remote_exec(host, user, cmd, mode="call")
            cmd = "firewall-cmd --reload"
            remote_exec(host, user, cmd, mode="call")
    
    elif linux_dist == "ubuntu":
        # Ubuntu 使用 ufw
        cmd = "systemctl status ufw"
        result = remote_exec(host, user, cmd, mode="call")
        
        if result == 0:  # ufw is running
            logger.info(f"Opening etcd ports on {host} (Ubuntu)")
            cmd = "ufw allow 2379/tcp && ufw allow 2385/tcp"
            remote_exec(host, user, cmd, mode="call")


def install_etcd(host, user):
    """安装etcd二进制文件"""
    linux_dist = g_settings.config.get("linux_dist", "centos")
    # etcd_version = "3.5.9"

    # 检查etcd是否已安装
    cmd = "which etcd"
    result = remote_exec(host, user, cmd, mode="call")
    if result == 0:
        logger.info(f"Etcd already installed on {host}")
        return
    
    logger.info(f"Installing etcd  on {host}")
    
    # 首先安装必要的依赖
    if linux_dist == "centos":
        cmd = "yum install -y wget tar"
        remote_exec(host, user, cmd, mode="call")
    elif linux_dist == "ubuntu":
        cmd = "DEBIAN_FRONTEND=noninteractive apt-get update && apt-get install -y wget tar"
        remote_exec(host, user, cmd, mode="call")
    
    scp(host, user, "./bin/etcd*", '/usr/local/bin/')
    # remote_exec(host, user, cmd)
    
    logger.info(f"Etcd installation completed on {host}")


def setup_etcd_environment(host, user):
    """设置etcd运行环境"""
    # 创建etcd用户
    cmd = "id etcd || useradd --system --shell /bin/false etcd"
    remote_exec(host, user, cmd, mode="call")
    
    # 创建数据目录
    cmd = "mkdir -p /var/lib/etcd"
    remote_exec(host, user, cmd)
    
    cmd = "chmod 755 /var/lib/etcd"
    remote_exec(host, user, cmd)


def verify_etcd_cluster(etcd_nodes, is_single_node=False):
    """验证etcd集群状态"""
    if not etcd_nodes:
        logger.warning("No etcd nodes provided for verification")
        return False
    
    mode_name = "single node" if is_single_node else "cluster"
    logger.info(f"Verifying etcd {mode_name} status...")
    
    # 构建验证策略
    verification_config = {
        "max_retries": 15 if is_single_node else 30,
        "retry_interval": 3 if is_single_node else 5,
        "timeout": 180 if is_single_node else 300
    }
    
    # 构建etcd endpoints
    endpoints = [f"http://{node['host']}:2379" for node in etcd_nodes]
    endpoints_str = ",".join(endpoints)
    
    # 选择用于验证的节点（优先使用第一个节点）
    primary_node = etcd_nodes[0]
    host, user = primary_node["host"], primary_node["user"]
    
    # 执行验证步骤
    start_time = time.time()
    
    for attempt in range(1, verification_config["max_retries"] + 1):
        try:
            # 步骤1: 检查基本连通性
            if not _check_etcd_connectivity(host, user, endpoints_str):
                raise Exception("Etcd connectivity check failed")
            
            # 步骤2: 验证集群成员
            member_info = _verify_cluster_members(host, user, endpoints_str, etcd_nodes, is_single_node)
            if not member_info:
                raise Exception("Cluster member verification failed")
            
            # 步骤3: 检查节点健康状态
            health_status = _check_cluster_health(host, user, endpoints_str, is_single_node)
            if not health_status:
                raise Exception("Cluster health check failed")
            
            # 步骤4: 测试读写操作
            if not _test_read_write_operations(host, user, endpoints_str):
                raise Exception("Read/write operation test failed")
            
            # 验证成功
            elapsed_time = time.time() - start_time
            logger.info(f"Etcd {mode_name} verification completed successfully in {elapsed_time:.2f} seconds")
            return True
            
        except Exception as e:
            elapsed_time = time.time() - start_time
            
            if attempt >= verification_config["max_retries"]:
                logger.error(f"Etcd {mode_name} verification failed after {attempt} attempts ({elapsed_time:.2f}s): {e}")
                _collect_diagnostic_info(etcd_nodes)
                return False
            
            # 检查是否超时
            if elapsed_time > verification_config["timeout"]:
                logger.error(f"Etcd {mode_name} verification timed out after {elapsed_time:.2f} seconds")
                _collect_diagnostic_info(etcd_nodes)
                return False
            
            logger.warning(f"Etcd {mode_name} verification attempt {attempt}/{verification_config['max_retries']} failed: {e}")
            logger.info(f"Retrying in {verification_config['retry_interval']} seconds...")
            time.sleep(verification_config["retry_interval"])
    
    return False


def _check_etcd_connectivity(host, user, endpoints_str):
    """检查etcd基本连通性"""
    try:
        cmd = f"ETCDCTL_API=3 etcdctl --endpoints={endpoints_str} --dial-timeout=5s endpoint status"
        remote_exec(host, user, cmd, mode="check_output")
        logger.debug("Etcd connectivity check passed")
        return True
    except Exception as e:
        logger.debug(f"Etcd connectivity check failed: {e}")
        return False


def _verify_cluster_members(host, user, endpoints_str, etcd_nodes, is_single_node):
    """验证集群成员信息"""
    try:
        cmd = f"ETCDCTL_API=3 etcdctl --endpoints={endpoints_str} member list"
        output = remote_exec(host, user, cmd, mode="check_output")
        
        # 解析成员信息
        members = []
        for line in output.strip().split('\n'):
            if line.strip():
                members.append(line.strip())
        
        expected_count = len(etcd_nodes)
        actual_count = len(members)
        
        if actual_count != expected_count:
            logger.warning(f"Expected {expected_count} members, but found {actual_count}")
            return False
        
        mode_name = "single node" if is_single_node else "cluster"
        logger.info(f"Etcd {mode_name} has {actual_count} member(s) as expected")
        
        # 记录成员详情（调试级别）
        for i, member in enumerate(members, 1):
            logger.debug(f"  Member {i}: {member}")
        
        return True
        
    except Exception as e:
        logger.error(f"Failed to verify cluster members: {e}")
        return False


def _check_cluster_health(host, user, endpoints_str, is_single_node):
    """检查集群健康状态"""
    try:
        cmd = f"ETCDCTL_API=3 etcdctl --endpoints={endpoints_str} endpoint health"
        output = remote_exec(host, user, cmd, mode="check_output")
        
        # 解析健康状态
        healthy_endpoints = []
        unhealthy_endpoints = []
        
        for line in output.strip().split('\n'):
            if "is healthy" in line:
                endpoint = line.split()[0]
                healthy_endpoints.append(endpoint)
            elif "is unhealthy" in line:
                endpoint = line.split()[0]
                unhealthy_endpoints.append(endpoint)
        
        if unhealthy_endpoints:
            logger.warning(f"Unhealthy endpoints detected: {unhealthy_endpoints}")
            return False
        
        mode_name = "single node" if is_single_node else "cluster"
        logger.info(f"Etcd {mode_name} health check passed - all {len(healthy_endpoints)} endpoint(s) healthy")
        
        return True
        
    except Exception as e:
        logger.error(f"Failed to check cluster health: {e}")
        return False


def _test_read_write_operations(host, user, endpoints_str):
    """测试读写操作"""
    test_key = f"gnext-test-{int(time.time())}"
    test_value = f"test-value-{int(time.time())}"
    
    try:
        # 写入测试数据
        cmd = f"ETCDCTL_API=3 etcdctl --endpoints={endpoints_str} put {test_key} {test_value}"
        remote_exec(host, user, cmd)
        logger.debug(f"Successfully wrote test key: {test_key}")
        
        # 读取测试数据
        cmd = f"ETCDCTL_API=3 etcdctl --endpoints={endpoints_str} get {test_key}"
        output = remote_exec(host, user, cmd, mode="check_output")
        
        if test_value in output:
            logger.debug(f"Successfully read test key: {test_key}")
            
            # 清理测试数据
            cmd = f"ETCDCTL_API=3 etcdctl --endpoints={endpoints_str} del {test_key}"
            remote_exec(host, user, cmd, mode="call")
            logger.debug(f"Successfully cleaned up test key: {test_key}")
            
            logger.info("Read/write operation test passed")
            return True
        else:
            logger.error(f"Read operation failed - expected '{test_value}' but got: {output}")
            return False
            
    except Exception as e:
        logger.error(f"Read/write operation test failed: {e}")
        # 尝试清理，但不影响返回结果
        try:
            cmd = f"ETCDCTL_API=3 etcdctl --endpoints={endpoints_str} del {test_key}"
            remote_exec(host, user, cmd, mode="call")
        except:
            pass
        return False


def _collect_diagnostic_info(etcd_nodes):
    """收集诊断信息"""
    logger.info("Collecting diagnostic information...")
    
    for i, node in enumerate(etcd_nodes, 1):
        node_host = node["host"]
        node_user = node["user"]
        etcd_name = node.get("etcd", {}).get("etcd_name", f"node-{i}")
        
        logger.info(f"=== Diagnostic info for {etcd_name} ({node_host}) ===")
        
        # 检查服务状态
        try:
            cmd = "systemctl is-active etcd"
            status = remote_exec(node_host, node_user, cmd, mode="check_output").strip()
            logger.info(f"Service status: {status}")
        except Exception as e:
            logger.warning(f"Could not get service status: {e}")
        
        # 检查服务详细状态
        try:
            cmd = "systemctl status etcd --no-pager -l"
            status_output = remote_exec(node_host, node_user, cmd, mode="check_output")
            logger.info(f"Service details:\n{status_output}")
        except Exception as e:
            logger.warning(f"Could not get service details: {e}")
        
        # 检查最近的日志
        try:
            cmd = "journalctl -u etcd --no-pager -n 10 --since '5 minutes ago'"
            log_output = remote_exec(node_host, node_user, cmd, mode="check_output")
            logger.info(f"Recent logs:\n{log_output}")
        except Exception as e:
            logger.warning(f"Could not get recent logs: {e}")
        
        # 检查端口监听状态
        try:
            cmd = "ss -tlnp | grep -E ':(2379|2385)'"
            port_output = remote_exec(node_host, node_user, cmd, mode="check_output")
            logger.info(f"Port status:\n{port_output}")
        except Exception as e:
            logger.warning(f"Could not get port status: {e}")
        
        logger.info(f"=== End diagnostic info for {etcd_name} ===\n")


def main_handle(args):
    load_config(args.config)
    init_log()

    agents_pkg = prepare_gnext_package()
    #ovs_pkg = prepare_openvswitch_package()

    config_basic()
    
    # 配置etcd集群
    config_etcd_cluster()
    
    # 为所有gnext节点分配uuid和ha_uuid
    assign_ha_uuids()

    manager_host = ""
    manager_user = ""

    for node in g_settings.config.get("nodes"):
        host = node.get("host")
        user = node.get("user")

        if not node.get("gnext"):
            continue
        if node.get("vnc"):
            vnc_passwd = node.get("vnc").get("passwd", "lnjoying")
            install_vnc(host, user, vnc_passwd)
        # 安装ovs, l3和vm都需要ovs
        #install_ovs(host, user, ovs_pkg)
        # 安装gnext agents
        install_agents(host, user, agents_pkg)
        # 安装libvirt
        install_libvirt(host, user)
        # 安装nbd
        install_nbd(host, user)
        # 安装nginx, vmagent需要配置phonehome的请求从8901转发到本地的8896
        config_gnext(node)

        install_nginx(host, user, node)


        # 部署fabric manager service vm
        install_fmvm(host, user, node["gnext"])

        if node.get("manager"):
            manager_host = host
            manager_user = user
    # 配置云管理
    if manager_host and manager_user:
        config_cloud_manager(manager_host, manager_user)


# 获取Java 云管的所有node的ip(promethus 需要)
def get_hosts():
    nodes = g_settings.config.get("nodes")
    node_ip_list = list()
    for node in nodes:
        node_ip_list.append(node.get("host"))
    return node_ip_list


# 安装docker/docker-compose
def install_docker(host, user):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        install_yum_utils_cmd = "yum install -y yum-utils"
        remote_exec(host, user, install_yum_utils_cmd)
        add_repo_cmd = "yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo"
        remote_exec(host, user, add_repo_cmd)
        install_docker_cmd = (
            "yum install -y --allowerasing docker-ce docker-ce-cli containerd.io docker-compose-plugin"
        )
        remote_exec(host, user, install_docker_cmd)
        start_docker_cmd = "systemctl start docker && systemctl enable docker"
        remote_exec(host, user, start_docker_cmd)
    elif linux_dist == "ubuntu":
        tmp_docker_install_script = "/tmp/docker_install.sh"
        content = """
for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do sudo apt-get remove $pkg; done
apt-get update
apt-get install ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
apt-get update -y
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
"""
        with open(tmp_docker_install_script, "w") as f:
            f.write(content)
        scp(host, user, tmp_docker_install_script, tmp_docker_install_script)
        cmd = "bash %s" % (tmp_docker_install_script)
        remote_exec(host, user, cmd)


# 修改prometheus 配置
def config_prometheus(host, user):
    libvirt_port = 9000
    node_port = 9100
    libvirt_server_list = list()
    node_server_list = list()
    for node in g_settings.config.get("nodes"):
        node_ip = node.get("host")
        libvirt_server = "%s:%d" % (node_ip, libvirt_port)
        node_server = "%s:%d" % (node_ip, node_port)
        if node.get("gnext"):
            libvirt_server_list.append(libvirt_server)
            node_server_list.append(node_server)
    node_cmd = 'sed -i "s/list/%s/" %s/prometheus/node.json' % (str(node_server_list).replace("'", '\\"'), manager_dir)
    libvirt_cmd = 'sed -i "s/list/%s/" %s/prometheus/libvirt.json' % (
        str(libvirt_server_list).replace("'", '\\"'),
        manager_dir,
    )
    prometheus_config_cmd = 'sed -i "s/need_update_ip/%s/" %s/prometheus/prometheus.yml' % (host, manager_dir)
    remote_exec(host, user, node_cmd)
    remote_exec(host, user, libvirt_cmd)
    remote_exec(host, user, prometheus_config_cmd)


# 修改grafana 配置
def config_grafana(host, user):
    grafana_cmd = 'sed -i "/^root_url/s/need_update_ip/%s/" %s/grafana/grafana.ini' % (host, manager_dir)
    datesource_cmd = 'sed -i "s/need_update_ip/%s/" %s/grafana/provisioning/datasources/datasources.yaml' % (
        host,
        manager_dir,
    )
    remote_exec(host, user, grafana_cmd)
    remote_exec(host, user, datesource_cmd)


# 同步文件，并复制*.temp 到对应的目录下，如docker-compose.yml.temp->docker-compose.yml
def sync_files(host, user):
    cloud_manager_mount = os.path.join(g_settings.config.get("gnext_src"), "gnext/mount.tar.gz")
    if not os.path.exists(cloud_manager_mount):
        raise Exception("cloud manager mount file %s" % (cloud_manager_mount))
    scp(host, user, cloud_manager_mount, "/opt/gnext")
    unzip_cmd = "tar xvfz /opt/gnext/mount.tar.gz -C /opt/gnext"
    remote_exec(host, user, unzip_cmd)
    cp_temp_files_cmd = 'find %s -name "*.temp"  -exec cp -f {} {}.bak \;' % manager_dir
    remote_exec(host, user, cp_temp_files_cmd)

    rename_script = os.path.join(g_settings.config.get("gnext_src"), "gnext/mount/exporter/rename.sh")
    scp(host, user, rename_script, "/tmp")
    rename_cmd = 'sed -i "s#manager_dir#%s#" /tmp/rename.sh && chmod +x /tmp/rename.sh' % (manager_dir)
    remote_exec(host, user, rename_cmd)

    #rename_temp_files_cmd = 'find %s -name "*.temp.bak" -exec rename ".temp.bak" "" {} \;' % manager_dir
    #rename_temp_files_cmd = 'find %s -name "*.temp.bak" -exec bash -c \'mv $0 ${0%.temp.bak}\' {} \;' % manager_dir

    remote_exec(host, user, "/tmp/rename.sh")
    remote_exec(host, user, "rm -f /tmp/rename.sh")

# 配置openresty
def config_openresty(host, user):
    openresty_cmd = 'sed -i "s/need_update_ip:3000/%s:3000/g" %s/openresty/nginx.conf' % (host, manager_dir)
    remote_exec(host, user, openresty_cmd)


# 配置nginx
def config_nginx(host, user):
    nginx_cmd = 'sed -i "s/need_update_ip:3000/%s:3000/" %s/nginx/nginx.conf' % (host, manager_dir)
    remote_exec(host, user, nginx_cmd)


# 配置java 云管的计算服务
def config_java_compute(host, pxe_agent_ip, user):
    grafana_cmd = 'sed -i "/^monitorServer/s/need_update_ip/%s/" %s/justice/config/etc/compute.yaml' % (
        host,
        manager_dir,
    )
    prometheus_cmd = 'sed -i "/^prometheusServer/s/need_update_ip/%s/" %s/justice/config/etc/compute.yaml' % (
        host,
        manager_dir,
    )
    pxe_cmd = 'sed -i "s/need_update_ip/%s/" %s/justice/config/etc/compute.yaml' % (pxe_agent_ip, manager_dir)
    remote_exec(host, user, grafana_cmd)
    remote_exec(host, user, prometheus_cmd)
    remote_exec(host, user, pxe_cmd)


# 配置java 云管的网络服务
def config_java_network(host, user):
    network_cmd = 'sed -i "s/need_update_ip/%s/" %s/justice/config/etc/network.yaml' % (host, manager_dir)
    remote_exec(host, user, network_cmd)


# 配置docker-compose.yml
def config_docker_compose(host, user):
    config_yaml_file_cmd = 'sed -i "s/need_update_ip/%s/" %s/docker-compose.yml' % (host, manager_dir)
    remote_exec(host, user, config_yaml_file_cmd)
    run_docker_compose_cmd = (
        "[ -f /usr/local/bin/docker-compose ] || ln -s /usr/libexec/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose; cd %s && docker-compose up -d"
        % manager_dir
    )
    remote_exec(host, user, run_docker_compose_cmd)


# 配置gpu_exporter
def config_gpu_exporter(host, user):
    gpu_exporter = os.path.join(g_settings.config.get("gnext_src"), "../../cloud-manager/exporter/nvidia_gpu_exporter")
    if not os.path.exists(gpu_exporter):
        raise Exception("gpu exporter %s not found" % (gpu_exporter))

    gpu_exporter_script = os.path.join(g_settings.config.get("gnext_src"), "../../cloud-manager/exporter/gpu.sh")
    if not os.path.exists(gpu_exporter_script):
        raise Exception("gpu exporter script %s not found" % (gpu_exporter_script))

    gpu_exporter_service = os.path.join(
        g_settings.config.get("gnext_src"), "../../cloud-manager/exporter/gpu_exporter.service"
    )
    if not os.path.exists(gpu_exporter_service):
        raise Exception("gpu exporter service script %s not found" % (gpu_exporter_service))

    scp(host, user, gpu_exporter_service, "/usr/lib/systemd/system")
    remote_exec(host, user, "systemctl daemon-reload")
    remote_exec(host, user, "systemctl stop gpu_exporter", mode="call")

    scp(host, user, gpu_exporter, "/opt/gnext/bin")
    scp(host, user, gpu_exporter_script, "/opt/gnext/bin")

    remote_exec(host, user, "chmod +x /opt/gnext/bin/nvidia_gpu_exporter && chmod +x /opt/gnext/bin/gpu.sh")
    remote_exec(host, user, "systemctl enable gpu_exporter && systemctl restart gpu_exporter")


# 配置node_exporter, libvirt_exporter
def config_exporter(host, user):
    linux_dist = g_settings.config.get("linux_dist", "centos")
    if linux_dist == "centos":
        install_node_exporter_cmd = (
            "yum install -y golang-github-prometheus-node-exporter libguestfs-winsupport libguestfs-tools"
        )

    elif linux_dist == "ubuntu":
        install_node_exporter_cmd = "apt install -y prometheus-node-exporter libguestfs-tools"

    remote_exec(host, user, install_node_exporter_cmd)
    # remote_exec(host, user, "systemctl stop libvirt_exporter.service; systemctl stop node_exporter.service")

    node_exporter_service = os.path.join(
        g_settings.config.get("gnext_src"), "./exporter/node_exporter.service"
    )
    if not os.path.exists(node_exporter_service):
        raise Exception("node exporter service script %s not found" % (node_exporter_service))
    scp(host, user, node_exporter_service, "/usr/lib/systemd/system")

    libvirt_exporter_service = os.path.join(
        g_settings.config.get("gnext_src"), "./exporter/libvirt_exporter.service"
    )
    if not os.path.exists(libvirt_exporter_service):
        raise Exception("libvirt exporter service script %s not found" % (libvirt_exporter_service))
    scp(host, user, libvirt_exporter_service, "/usr/lib/systemd/system")

    virt_monitor_file_cmd = "ls /usr/lib/systemd/system/libvirt_exporter.service && systemctl stop libvirt_exporter"
    remote_exec(host, user, virt_monitor_file_cmd)
    node_exporter_file_cmd = "ls /usr/lib/systemd/system/node_exporter.service && systemctl stop node_exporter"
    remote_exec(host, user, node_exporter_file_cmd)

    systemctl_daemon_reload_cmd = (
        "systemctl daemon-reload && systemctl enable node_exporter && systemctl enable libvirt_exporter"
    )
    remote_exec(host, user, "systemctl restart libvirt_exporter && systemctl restart node_exporter")
    remote_exec(host, user, systemctl_daemon_reload_cmd)


# 配置云管
def config_cloud_manager(host, user):
    sync_files(host, user)
    config_prometheus(host, user)
    config_grafana(host, user)
    config_openresty(host, user)
    config_nginx(host, user)
    config_java_compute(host, host, user)
    config_java_network(host, user)
    config_docker_compose(host, user)


def main():
    args = sys.argv[1:]
    parser = argparse.ArgumentParser(description="gnext deployment utility")
    parser.add_argument("-C", "--config", help="configation file", required=False)

    parser.set_defaults(func=main_handle)

    _args = parser.parse_args(args)
    if not hasattr(_args, "func"):
        parser.parse_args(["-h"])
    else:
        _args.func(_args)


if __name__ == "__main__":
    main()
