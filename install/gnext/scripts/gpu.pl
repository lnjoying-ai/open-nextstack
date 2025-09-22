#!/usr/bin/perl
use strict;

sub get_cpu_type {
    my $cpuinfo = `cat /proc/cpuinfo`;
    if($cpuinfo =~ /model name\s*:\s*intel/i){
        return 'intel';
    }
    return 'amd';
}

sub get_distro {
    my $distro = `cat /etc/os-release | grep -i ^name`;
    if($distro =~ /centos/i){
        return 'centos';
    }
    return 'ubuntu';
}

sub config_kernel{
    my ($ids, $hugepages) = @_;
    my $cpu_type = get_cpu_type();
    my $distro = get_distro();
    my $grub = `cat /etc/default/grub`;
    my $grub_cmdline = "";
    # 如果iommu没有开启，就开启iommu
    if($cpu_type eq 'intel'){
        $grub_cmdline = " intel_iommu=on iommu=pt vfio-pci.ids=$ids";
    }
    else{
        $grub_cmdline .= " amd_iommu=on iommu=pt vfio-pci.ids=$ids";
    }
    if ($hugepages){
        $grub_cmdline .= " default_hugepagesz=1G hugepagesz=1G hugepages=$hugepages";
    }
    `sed -i 's/GRUB_CMDLINE_LINUX=".*"/GRUB_CMDLINE_LINUX="$grub_cmdline"/' /etc/default/grub`;
    if($distro eq 'centos'){
        `grub2-mkconfig -o /boot/grub2/grub.cfg`;
        `grub2-mkconfig -o /etc/grub2-efi.cfg`;
    }
    else{
        `update-grub`;
    }
}

sub config_vfio{
    my $distro = get_distro();
    `tee /etc/modules-load.d/vfio.conf << EOF
vfio
vfio_pci
vfio_virqfd
vfio_iommu_type1
EOF`;
    `tee /etc/modprobe.d/nouveau.conf << EOF
blacklist nouveau
blacklist nvidia
options nouveau modeset=0
EOF`;
    `tee /etc/modprobe.d/vfio.conf << EOF
softdep radeon pre: vfio-pci
softdep amdgpu pre: vfio-pci
softdep nouveau pre: vfio-pci
softdep nvidiafb pre: vfio-pci
softdep drm pre: vfio-pci
softdep snd_hda_intel pre: vfio-pci
options kvm_amd avic=1
EOF`;
    if($distro eq 'centos'){
        `dracut -f`;
    }
    else{
        `update-initramfs -u`;
    }
}
# get hugepages from command line
my $hugepages = 0;
my $ids = '10de:22a3,10de:2330,8086:1bb0,11f8:4128';
if($#ARGV >= 1) {
    $hugepages = $ARGV[1];
}
if($#ARGV >= 0) {
    $ids = $ARGV[0];
}
config_kernel($ids, $hugepages);
config_vfio();