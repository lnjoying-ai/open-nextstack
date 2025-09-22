#!/usr/bin/perl

use strict;

my $vms = `vmagent vm list`;
if($vms =~ /"vm_uuids":\s*\[(\S*)]/)
{
  my @vms = split /,/,$1;
  for my $vm (@vms)
  {
	  my $vm_uuid = $vm;
	  $vm_uuid =~ s/"//g;
	  my $vminfo = `vmagent vm get -U $vm_uuid`;
	if($vminfo =~ /"vncport":(\d+)/)
	{
	  my $vm_vncport = $1;
	  print("echo $vm_uuid: 127.0.0.1:$vm_vncport >> $vm_uuid\n");  
	  `echo $vm_uuid: 127.0.0.1:$vm_vncport >> $vm_uuid`;  
	}
  }
}
