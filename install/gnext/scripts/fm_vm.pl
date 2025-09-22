#!/usr/bin/perl
use strict;

sub cmd {
    my $cmd = shift;
    $cmd =~ s/\n/ /g;
    $cmd =~ s/\s+/ /g;
    print("==>$cmd\n");
    my $ret = `$cmd`;
    print("<==$ret\n");
    return $ret;
}

sub cmd_remote {
    my ( $ip, $cmd ) = @_;
    my $ret = cmd("ssh -o StrictHostKeyChecking=no root\@$ip $cmd");
    return $ret;
}

sub wait_up {
    my ( $ip, $name ) = @_;
    print("============== wait for vm $ip $name up ==============\n");
    my $i = 60 * 10;
    while ( $i > 0 ) {
        my $ret = cmd("ping -c 1 $ip");
        if ( $ret =~ /1 packets transmitted, 1 received, 0% packet loss/m ) {
            cmd("ssh-keygen -f \"/root/.ssh/known_hosts\" -R $ip");
            while ( $i > 0 ) {
                my $ret = cmd_remote( $ip, "hostname 2>/dev/null" );
                if ( $ret =~ /$name/m ) {
                    sleep(5);
                    print("============== vm $name is up ==============\n");
                    return;
                }
                sleep(5);
                $i -= 5;
            }
            last;
        }
        sleep(5);
        $i -= 5;
    }
    print("wait for vm $name up failed\n");
    exit(1);
}

# 脚本的第二个参数为代理端口, 0表示不使用代理
my $proxy_port = 0;
if ( $#ARGV > 0 ) {
    $proxy_port = $ARGV[1];
}

# 脚本的第一个参数为虚拟机的ip地址
my $vm_ip = '192.168.122.100';
if ( $#ARGV > -1 ) {
    $vm_ip = $ARGV[0];
}

# 启用proxy
if ($proxy_port) {
    $ENV{http_proxy} = "http://localhost:$proxy_port";
    $ENV{https_proxy} = "http://localhost:$proxy_port";
}

my $username = 'cloud';
my $password = '$6$rounds=4096$.zD6KAaPpzdPJAKZ$XALCPfgJO6q7CMrS.4TYghiQnhYv/u0NqI87vwI7Wg.mnHBwK9r8fEmALgvzNUw3VqhCNVxRKRChTNr7.v9a40';    #cloud1234
my $vm_name = 'nvsvm';
my $vm_mac = '52:54:00:' . sprintf( "%02x", int( rand(256) ) ) . ':' . sprintf( "%02x", int( rand(256) ) ) . ':' . sprintf( "%02x", int( rand(256) ) );

cmd("killall -9 unattended-upgrade;apt update;apt install qemu-utils libvirt cloud-utils whois -y 2>/dev/null");
cmd("mkdir -p /var/lib/libvirt/images/$vm_name");
cmd("mkdir -p /opt/nextstack/cache");

chdir('/opt/nextstack/cache');

if ( -f "/var/lib/libvirt/images/$vm_name/root-disk.qcow2" ) {
    cmd("virsh destroy $vm_name");
    cmd("virsh undefine $vm_name");
    cmd("rm -f /var/lib/libvirt/images/$vm_name/root-disk.qcow2");
    cmd("rm -f /var/lib/libvirt/images/$vm_name/cloud-init.iso");
    cmd("rm -f /opt/nextstack/cache/$vm_name.xml");
}

if ( !-f '/opt/nextstack/cache/jammy-server-cloudimg-amd64.img' ) {
    cmd("wget https://cloud-images.ubuntu.com/jammy/current/jammy-server-cloudimg-amd64.img");
}
if ( !-f '/opt/nextstack/cache/cuda_12.2.2_535.104.05_linux.run' ) {
    cmd("wget https://developer.download.nvidia.com/compute/cuda/12.2.2/local_installers/cuda_12.2.2_535.104.05_linux.run");
}

if ( !-d '/opt/nextstack/cache/fabricmanager-linux-x86_64-535.104.05-archive' ) {
    cmd("wget https://developer.download.nvidia.com/compute/cuda/redist/fabricmanager/linux-x86_64/fabricmanager-linux-x86_64-535.104.05-archive.tar.xz");
    cmd("xz -d /opt/nextstack/cache/fabricmanager-linux-x86_64-535.104.05-archive.tar.xz");
    cmd("tar -xf /opt/nextstack/cache/fabricmanager-linux-x86_64-535.104.05-archive.tar");
}

cmd("qemu-img convert -f qcow2 -O qcow2 /opt/nextstack/cache/jammy-server-cloudimg-amd64.img /var/lib/libvirt/images/$vm_name/root-disk.qcow2");
cmd("qemu-img resize /var/lib/libvirt/images/$vm_name/root-disk.qcow2 20G");

if ( !-e '/root/.ssh/id_rsa.pub' ) {
    cmd("ssh-keygen -t rsa -N '' -f ~/.ssh/id_rsa");
}
my $pub_key = cmd("cat ~/.ssh/id_rsa.pub");
my $user_data = <<"USER_DATA";
#cloud-config
hostname: $vm_name
system_info:
  default_user:
    name: root
    lock_passwd: true
users:
  - name: $username
    passwd: $password
    chpasswd: { expire: False }
    home: /home/$username
    sudo: ALL=(ALL) NOPASSWD:ALL
    shell: /bin/bash
    lock_passwd: false
ssh_authorized_keys:
  - $pub_key
runcmd:
  - mount -t virtiofs opt-n-share /mnt
hostname: $vm_name
disable_root: false
ssh_pwauth: true
allow_public_ssh_keys: true
USER_DATA

my $network_config = <<"NETWORK_CONFIG";
version: 2
ethernets:
  id0:
    set-name: eth0
    dhcp4: no
    dhcp6: no
    addresses:
      -  $vm_ip/24
    match:
      macaddress: $vm_mac
NETWORK_CONFIG

open( my $fh, '>', '/opt/nextstack/cache/user-data.yml' );
print $fh $user_data;
close $fh;

open( $fh, '>', '/opt/nextstack/cache/network-config.yml' );
print $fh $network_config;
close $fh;

# 生成cloud-init.iso
cmd("cloud-localds -N /opt/nextstack/cache/network-config.yml /var/lib/libvirt/images/$vm_name/cloud-init.iso /opt/nextstack/cache/user-data.yml");

# 启动虚拟机
my $install_cmd = <<"INSTALL_CMD";
virt-install --name $vm_name
  --machine pc-i440fx-6.2
  --memory 8192
  --vcpus 4
  --disk /var/lib/libvirt/images/$vm_name/root-disk.qcow2,device=disk,bus=virtio
  --disk /var/lib/libvirt/images/$vm_name/cloud-init.iso,device=cdrom
  --os-variant ubuntu22.04
  --virt-type kvm
  --graphics none
  --network bridge=virbr0,model=virtio,mac=$vm_mac
  --import
  --noautoconsole
  --filesystem /opt/nextstack/cache,opt-n-share,driver.type=virtiofs
  --memorybacking=source.type=memfd,access.mode=shared
INSTALL_CMD

cmd($install_cmd);
if ( $? != 0 ) {
    print("start vm $vm_name (first time) failed\n");
    exit(1);
}

# wait for vm to boot first time
wait_up( $vm_ip, $vm_name );

my $cdrom_dev = qq{
    <disk type='file' device='cdrom'>
      <driver name='qemu' type='raw'/>
      <target dev='sda' bus='sata'/>
    </disk>
};

# write to cdrom.xml
open( $fh, '>', '/opt/nextstack/cache/cdrom.xml' );
print $fh $cdrom_dev;
close $fh;

# eject cdrom
cmd("virsh update-device $vm_name /opt/nextstack/cache/cdrom.xml --persistent");

my $def = cmd("virsh dumpxml $vm_name");

# hostdev of nvswitches
my $nvswitches = "";
my $pcsi = cmd("lspci -nk  |grep -E  '10de:22a3'|awk '{print \$1}'");
my @pcis = split /\n/, $pcsi;
for my $pci (@pcis) {
    my @parts = split /[:.]/, $pci;
    my $xml = qq{
    <hostdev mode='subsystem' type='pci' managed='yes'>
        <source>
            <address domain='0x0000' bus='0x$parts[0]' slot='0x$parts[1]' function='0x$parts[2]'/>
        </source>
    </hostdev>
    };
    $nvswitches .= $xml;
}

# hostdev of gpus
my $gpus = "";
$pcsi = cmd("lspci -nk  |grep -E  '10de:2330'|awk '{print \$1}'");
@pcis = split /\n/, $pcsi;
for my $pci (@pcis) {
    my @parts = split /[:.]/, $pci;
    my $xml = qq{
    <hostdev mode='subsystem' type='pci' managed='yes'>
        <source>
            <address domain='0x0000' bus='0x$parts[0]' slot='0x$parts[1]' function='0x$parts[2]'/>
        </source>
    </hostdev>
    };
    $gpus .= $xml;
}

# vm define file with nvswitches
$def =~ s/(<\/devices>)/$nvswitches$1/;
open( $fh, '>', "/opt/nextstack/cache/$vm_name.xml" );
print $fh $def;
close $fh;

my $pciroot = qq{
    <controller type='pci' index='0' model='pcie-root'>
      <pcihole64 unit='GiB'>2048</pcihole64>
    </controller>
};

# replace pcie-root tag with $pcieroot
$def =~ s/<controller type='pci' index='0' model='pcie-root'>.*?<\/controller>/$pciroot/s;
$def =~ s/<controller type='pci' index='0' model='pcie-root'\/>/$pciroot/s;

# vm define file with gpus and nvswitches
$def =~ s/(<\/devices>)/$gpus$1/;
open( $fh, '>', "/opt/nextstack/cache/$vm_name.tmp.xml" );
print $fh $def;
close $fh;

cmd_remote( $vm_ip, "sync" );
cmd("virsh destroy $vm_name");
cmd("virsh undefine $vm_name");
cmd("virsh define /opt/nextstack/cache/$vm_name.tmp.xml");
cmd("virsh autostart $vm_name");
cmd("virsh start $vm_name");

if ( $? != 0 ) {
    print("start vm $vm_name (second time) failed\n");
    exit(1);
}

# wait for vm to boot second time
wait_up( $vm_ip, $vm_name );

# 启动到虚拟机的代理隧道
if ($proxy_port) {
    cmd("/usr/lib/autossh/autossh -M 0 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -fN  -R 0.0.0.0:$proxy_port:127.0.0.1:$proxy_port root\@$vm_ip");
}

# 生成一个.env文件，用于虚拟机设置代理
open( $fh, '>', '/opt/nextstack/cache/.env' );
if ($proxy_port) {
    print $fh "export http_proxy=http://localhost:$proxy_port\n";
    print $fh "export https_proxy=http://localhost:$proxy_port\n";
}
print $fh "export DEBIAN_FRONTEND=noninteractive\n";
close $fh;

# 生成一个shell脚本，用于安装cuda
open( $fh, '>', '/opt/nextstack/cache/install_fm.sh' );
print $fh <<"BASH";
#!/bin/bash
perl -p -i -e 's/^deb \\S+/deb http:\\/\\/mirrors\\.aliyun\\.com\\/ubuntu/' /etc/apt/sources.list
mount -t virtiofs opt-n-share /mnt
. /mnt/.env
killall -9 unattended-upgrade
apt update -y
apt-get --fix-broken install
apt autoremove -y;
apt-get install gcc g++ make indent -y
sh /mnt/cuda_12.2.2_535.104.05_linux.run --silent
## install nvidia fabric manager
cp -r /mnt/fabricmanager-linux-x86_64-535.104.05-archive/bin/* /usr/bin/
mkdir -p /usr/lib/x86_64-linux-gnu
cp -r /mnt/fabricmanager-linux-x86_64-535.104.05-archive/lib/* /usr/lib/x86_64-linux-gnu/
mkdir -p /usr/share/nvidia/nvswitch
cp -r /mnt/fabricmanager-linux-x86_64-535.104.05-archive/include/* /usr/include/
cp -r /mnt/fabricmanager-linux-x86_64-535.104.05-archive/etc/* /usr/share/nvidia/nvswitch/
cp -r /mnt/fabricmanager-linux-x86_64-535.104.05-archive/share/* /usr/share/
cp -r /mnt/fabricmanager-linux-x86_64-535.104.05-archive/systemd/* /lib/systemd/system/
sed -i -e s/FABRIC_MODE=.*/FABRIC_MODE=1/g /usr/share/nvidia/nvswitch/fabricmanager.cfg
sed -i -e s/BIND_INTERFACE_IP=.*/BIND_INTERFACE_IP=$vm_ip/g /usr/share/nvidia/nvswitch/fabricmanager.cfg
sed -i -e s/FM_CMD_BIND_INTERFACE=.*/FM_CMD_BIND_INTERFACE=$vm_ip/g /usr/share/nvidia/nvswitch/fabricmanager.cfg
systemctl enable nvidia-fabricmanager
systemctl restart nvidia-fabricmanager
rm -f /tmp/install_fm.sh
BASH
close $fh;

cmd("scp -o StrictHostKeyChecking=no /opt/nextstack/cache/install_fm.sh root\@$vm_ip:/tmp/");
cmd_remote( $vm_ip, "sh /tmp/install_fm.sh" );
if ( $? != 0 ) {
    print("install nvidia software failed\n");
    exit(1);
}

my %pci_map = ();
$def = cmd("virsh dumpxml $vm_name");
for my $hostdev ( split /<\/hostdev>/, $def ) {
    if ( $hostdev =~ /<address domain='0x(\w+)' bus='0x(\w+)' slot='0x(\w+)' function='0x(\w+)'.*?<address type='pci' domain='0x(\w+)' bus='0x(\w+)' slot='0x(\w+)' function='0x(\w+)'/s ) {
        $pci_map{"$5:$6:$7.$8"} = "$1:$2:$3.$4";
    }
}
my %card_map = ();
my $ret = cmd_remote( $vm_ip, "nvidia-smi -q |grep -E \"GPU 0|Module ID\"" );
for my $card ( split /GPU/, $ret ) {
    my $card = lc($card);
    if ( $card =~ /0000(\w+):(\w+):(\w+)\.(\w)\s*module id\s*:\s*(\d+)/s ) {
        my $vm_pci = "$1:$2:$3.$4";
        my $module_id = $5;
        if ( exists $pci_map{$vm_pci} ) {
            my $host_pci = $pci_map{$vm_pci};
            $card_map{$host_pci} = $module_id;
        }
    }
}
if (%card_map) {
    open( $fh, '>', '/opt/nextstack/.gpu_ids' );
    for my $host_pci ( keys %card_map ) {
        print $fh "$host_pci: $card_map{$host_pci}\n";
    }
    close $fh;
}

cmd_remote( $vm_ip, "sync" );
cmd("virsh destroy $vm_name");
cmd("virsh undefine $vm_name");
cmd("virsh define /opt/nextstack/cache/$vm_name.xml");
cmd("virsh autostart $vm_name");
cmd("virsh start $vm_name");

if ( $? != 0 ) {
    print("start vm $vm_name (third time) failed\n");
    exit(1);
}

# wait for vm to boot second time
wait_up( $vm_ip, $vm_name );

print("vm $vm_name is ready\n");
