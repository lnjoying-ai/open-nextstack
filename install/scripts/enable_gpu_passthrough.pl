#!/usr/bin/env perl
use strict;

sub cmd{
    my $cmd = shift;
    #print("$cmd\n");
    my $ret = `$cmd`;
    return $ret;
}

sub update_grub{
    my $grub_file = shift;
    #my $grub_file = "/etc/default/grub";
    my $grub = cmd("cat $grub_file");
    my @lines = ();
    for my $line (split /\n/, $grub){
        if($line =~/^\s*GRUB_CMDLINE_LINUX\s*=\s*\"(.*)\"\s*$/){
            my $paras = $1;
            my @paras = ();
            for my $para (split /\s+/,$paras){
                next if($para =~ /iommu/);
                push @paras, $para;
            }
            $paras  = join(" ", @paras);
            $line = "GRUB_CMDLINE_LINUX=\"$paras\"";
            print("$line\n");
        }
        push @lines, $line;;
    }
    for my $line (@lines)
    {
        print("$line\n");
    }
    #open(my $f, '>', $grub_file) or die("cannot open $grub_file for write");
    #close($f);
}

my $ret = cmd("mokutil --sb-state");
if ($ret !~ /SecureBoot disabled/){
    die("secureboot mode is not disabled");
}

update_grub("/tmp/grub");
