#!/usr/bin/perl
use strict;

my $username = 'cloud';
my $password = '$6$rounds=4096$.zD6KAaPpzdPJAKZ$XALCPfgJO6q7CMrS.4TYghiQnhYv/u0NqI87vwI7Wg.mnHBwK9r8fEmALgvzNUw3VqhCNVxRKRChTNr7.v9a40'; #cloud1234
my $vm_name = 'gpuvm';
my $vm_mac = '52:54:00:aa:bb:02';
my $vm_ip = '192.168.122.101/24';
my $vm_os = "ubuntu-22.04";
# set env
$ENV{http_proxy} = 'http://localhost:3128';
$ENV{https_proxy} = 'http://localhost:3128';

sub cmd{
    my $cmd = shift;
    $cmd =~ s/\n/ /g;
    $cmd =~ s/\s+/ /g;
    print("==>$cmd\n");
    my $ret = `$cmd`;
    print("<==$ret\n");
    return $ret;
}
cmd("killall -9 unattended-upgrade");
cmd("apt update");
cmd("apt install qemu-utils virt-manager cloud-utils whois -y");
cmd("mkdir -p /var/lib/libvirt/images/$vm_name");
cmd("mkdir -p /opt/next");
chdir('/opt/next');

if(-f "/var/lib/libvirt/images/$vm_name/root-disk.qcow2"){
    cmd("virsh destroy $vm_name");
    cmd("virsh undefine $vm_name");
    cmd("rm -f /var/lib/libvirt/images/$vm_name/root-disk.qcow2");
    cmd("rm -f /var/lib/libvirt/images/$vm_name/cloud-init.iso");
    cmd("rm -f /opt/next/$vm_name.xml");
}
#if(! -f '/opt/next/focal-server-cloudimg-amd64.img'){
#    cmd("wget https://cloud-images.ubuntu.com/focal/current/focal-server-cloudimg-amd64.img");
#}
#cmd("qemu-img convert -f qcow2 -O qcow2 /opt/next/focal-server-cloudimg-amd64.img /var/lib/libvirt/images/$vm_name/root-disk.qcow2");

if(! -f '/opt/next/jammy-server-cloudimg-amd64.img'){
    cmd("wget https://cloud-images.ubuntu.com/jammy/current/jammy-server-cloudimg-amd64.img");
}
cmd("qemu-img convert -f qcow2 -O qcow2 /opt/next/jammy-server-cloudimg-amd64.img /var/lib/libvirt/images/$vm_name/root-disk.qcow2");
cmd("qemu-img resize /var/lib/libvirt/images/$vm_name/root-disk.qcow2 20G");

my $pub_key = cmd("cat ~/.ssh/id_rsa.pub");
my $user_data= qq{
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
};

my $network_config = qq{
version: 2
ethernets:
  id0:
    set-name: eth0
    dhcp4: no
    dhcp6: no
    addresses:
      -  $vm_ip
    match:
      macaddress: $vm_mac
};

# write $cfg to cloud-init.cfg
open(my $fh, '>', '/opt/next/user-data.yml');
print $fh $user_data;
close $fh;

open($fh, '>', '/opt/next/network-config.yml');
print $fh $network_config;
close $fh;
cmd("cloud-localds -N /opt/next/network-config.yml /var/lib/libvirt/images/$vm_name/cloud-init.iso /opt/next/user-data.yml");

my $install_cmd = qq{virt-install --name $vm_name
  --memory 8192
  --vcpus 4
  --disk /var/lib/libvirt/images/$vm_name/root-disk.qcow2,device=disk,bus=virtio
  --disk /var/lib/libvirt/images/$vm_name/cloud-init.iso,device=cdrom
  --os-variant ubuntu20.04
  --virt-type kvm
  --graphics none
  --network bridge=virbr0,model=virtio,mac=$vm_mac
  --import
  --noautoconsole
  --filesystem /opt/next/share,opt-n-share,driver.type=virtiofs
  --memorybacking=source.type=memfd,access.mode=shared
  };
cmd($install_cmd);

my $hostdevs = "";
my $pcsi = cmd("lspci -nk  |grep -E  '10de:2330'|awk '{print \$1}'");
my @pcis = split /\n/,$pcsi;
for my $pci (@pcis){
    my @parts = split /[:.]/,$pci;
    my $xml = qq{
    <hostdev mode='subsystem' type='pci' managed='yes'>
        <source>
            <address domain='0x0000' bus='0x$parts[0]' slot='0x$parts[1]' function='0x$parts[2]'/>
        </source>
    </hostdev>
    };
    $hostdevs .= $xml;
}

cmd("virsh dumpxml $vm_name > /opt/next/$vm_name.xml");

my $pciroot= qq{
    <controller type='pci' index='0' model='pcie-root'>
      <pcihole64 unit='GiB'>4096</pcihole64>
    </controller>
};
my $def = cmd("cat /opt/next/$vm_name.xml");
$def =~ s/(<\/devices>)/$hostdevs$1/;

# replace pcie-root tag with $pcieroot
$def =~ s/<controller type='pci' index='0' model='pcie-root'>.*?<\/controller>/$pciroot/s;
$def =~ s/<controller type='pci' index='0' model='pcie-root'\/>/$pciroot/s;

open($fh, '>', "/opt/next/$vm_name.xml");
print $fh $def;
close $fh;
cmd("virsh destroy $vm_name");
cmd("virsh undefine $vm_name");
cmd("virsh define /opt/next/$vm_name.xml");
cmd("virsh autostart $vm_name");
cmd("virsh start $vm_name");

# wait for vm to boot till $vm_ip is reachable
$vm_ip =~ s/\/.*//;
while(1){
    my $ret = cmd("ping -c 1 $vm_ip");
    if($ret =~ /1 packets transmitted, 1 received, 0% packet loss/m){
        last;
    }
    sleep(5);
}

my $cdrom_dev = qq{
    <disk type='file' device='cdrom'>
      <driver name='qemu' type='raw'/>
      <target dev='sda' bus='sata'/>
    </disk>
};

# write to cdrom.xml
open($fh, '>', '/opt/next/cdrom.xml');
print $fh $cdrom_dev;
close $fh;

cmd("virsh update-device $vm_name /opt/next/cdrom.xml --persistent");

# setup ssh tunnel to $vm_ip
cmd("/usr/lib/autossh/autossh -M 0 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -o ServerAliveInterval=60 -o ServerAliveCountMax=3 -fN  -R 0.0.0.0:3128:127.0.0.1:3128 root\@$vm_ip");

print("vm $vm_name is ready\n");