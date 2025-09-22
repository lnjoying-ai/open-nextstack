#!/usr/bin/perl

use strict;

# 关闭所有服务程序
`ps -ef |grep agent.py|grep -v grep|awk '{print \$2}'|xargs kill 2>/dev/null`;
`systemctl stop lnjoying-l3agent 2>/dev/null`;
`systemctl stop lnjoying-vmagent 2>/dev/null`;
`systemctl stop lnjoying-pxeagent 2>/dev/null`;

# 杀掉l3/pxe所使用的dnsmasq进程
`ps -ef |grep dnsmasq |grep -v grep |grep -E 'l3|pxe|vm'|grep -v grep |awk '{print $2}'|xargs kill 2>/dev/null`;

# 清除iscsi共享
`targetcli clearconfig confirm=True`;

# 删除所有的虚拟机及其快照
my $ret = `virsh list --all`;
for my $line (split /\n/,$ret){
  if ($line =~ /^\s*(?:\d+|-)\s+(\S+)\s+(\S+)/){
    my ($vm,$state) = ($1,$2);
    if($state == 'running'){
      `virsh destroy $vm`;
    }

    my $r = `virsh snapshot-list $vm`;
    for my $l (split /\n/,$r){
      if ($l =~ /^\s*(\d+)/){
        my $snap = $1;
       `virsh snapshot-delete $vm $snap`;	
      }
    }
    `virsh undefine $vm`;
    `rm -f /vms/volumes/root-$vm.qcow2`;
  }
}
`rm -f /vms/volumes/root-*.qcow2`;
`rm -f /vms/tokens/*`;
`find /vms/cdrom/ -name '*.iso' |grep -v none|xargs rm -f `;

# 删除netns
$ret = `ip netns`;
for my $line (split /\n/,$ret){
  if ($line =~ /^(\S+)/){
    my $ns = $1;
    my $ret1 = `ip netns pids $ns`;
    for my $line1 (split /\n/,$ret1){
       if($line1 =~ /(\d+)/){
	       `kill -9 $1 2>/dev/null`;
       }
    }
    `ip netns del $1`;
  }
}

# 删除ovs网桥
`ovs-vsctl --if-exists del-br br0`;

# 删除所有vlan网卡
$ret = `ip -br link show type vlan`;
for my $line (split /\n/,$ret){
  if ($line =~ /(\S+)@/){
    my $nic = $1;
    if($nic =~ /\./)
    {
        `ip l d dev $nic`;
    }
  }
}

# 删除所有veth网卡
$ret = `ip -br link show type veth`;
for my $line (split /\n/,$ret){
  if ($line =~ /(\S+)@/){
    my $nic = $1;
    if($nic =~ /\./)
    {
        `ip l d dev $nic`;
    }
  }
}

# 删除所有的虚拟网卡
$ret = `ip -br link show type tun`;
for my $line (split /\n/,$ret){
  $line =~ s/(\S+).*/$1/;
  if($line=~ /vnet-/)
  {
        `ip l d dev $line`;
  }
}

# 删除日志
`rm -f /opt/nextstack/log/*.log`;

# 删除数据库
`rm -f /opt/nextstack/db/*.db`;
