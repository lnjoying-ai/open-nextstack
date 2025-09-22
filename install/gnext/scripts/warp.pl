#!/usr/bin/perl

use strict;

my $rule = `ip rule show|grep 'lookup 100'`;
if ( $rule eq "" ) {
    `ip rule add lookup 100`;
}

my $table = `ip route show table 100`;
if ( $table eq "" ) {
    `ip route add default dev wg0 table 100`;
}

my $output = `iptables -t nat -L --line-number|grep 1234`;
if ( $output =~ /^(\d+).*/ ) {
    my $idx = $1;
    print("$output $idx\n");

    # mark the package with x1234
`iptables -t nat -I SS_SPEC_WAN_FW $idx -p tcp -m multiport --dports 22,53,587,465,995,993,143,80,443,853,9418 -j MARK --set-mark 0x1234`;

# add ip rule to route the packages marked with x1234 to the wireguard interface
# check ip rule exists for table 100

}