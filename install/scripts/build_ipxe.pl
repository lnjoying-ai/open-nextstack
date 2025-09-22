#!/usr/bin/env perl
use strict;

# get directory of current script
my $dir = $0;
$dir =~ s/\/[^\/]*$//;

# get absolute path of current script
my $absdir = `cd $dir; pwd`;
chomp($absdir);

if ( !-f "$absdir/chain.ipxe" ) {
    die("chain.ipxe not found");
}

if ( !-d "$absdir/../../third_party/ipxe-1.21.1/src" ) {
    die("ipxe source not found");
}

# goto ipxe source directory
chdir("$absdir/../../third_party/ipxe-1.21.1/src");
if ( -f "bin-x86_64-efi/snponly.efi" ) {
    unlink("bin-x86_64-efi/snponly.efi");
}
`make -j8 bin-x86_64-efi/snponly.efi EMBED=$absdir/chain.ipxe`;
if ( !-f "bin-x86_64-efi/snponly.efi" ) {
    die("snponly.efi not built out");
}
if ( -f "bin/undionly.kpxe" ) {
    unlink("bin/undionly.kpxe");
}
`make -j8 bin/undionly.kpxe EMBED=$absdir/chain.ipxe`;
if ( !-f "bin/undionly.kpxe" ) {
    die("undionly.kpxe not built out");
}
