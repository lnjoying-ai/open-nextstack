#!/usr/bin/perl

use strict;

print("generate deploy.tgz\n");
#`tar cvfz /tmp/deploy.tgz config.*.yml *.py requirement* bin l3 pxe repo sc scripts test utils vm`;
`tar cvfz /tmp/deploy.tgz *.py requirement* bin l3 pxe repo sc scripts test utils vm`;

print("uncompress to /opt/nextstack\n");
`mkdir -p /opt/nextstack/log;mkdir -p /opt/nextstack/db;cd /opt/nextstack;tar xvfz /tmp/deploy.tgz`;

my @hosts = qw( 192.168.1.57 192.168.1.58 192.168.1.21);
#my @hosts = qw( 192.168.1.67);

for my $host (@hosts){
    print("copy deploy.tgz to $host\n");
    `scp /tmp/deploy.tgz root\@$host:/tmp/`;
    print("uncompress to /opt/nextstack on $host\n");
    #`ssh root\@$host 'mkdir -p /opt/nextstack/log;mkdir -p /opt/nextstack/db;cd /opt/nextstack;tar xvfz /tmp/deploy.tgz;rm -f /tmp/deploy.tgz'`;
    `ssh root\@$host 'mkdir -p /opt/nextstack/log;mkdir -p /opt/nextstack/db;cd /opt/nextstack;tar xvfz /tmp/deploy.tgz;'`;
}
