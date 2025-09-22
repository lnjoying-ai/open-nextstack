virt-builder centos-7.8 --cache /vms/backing/cache/ --install cloud-init,cloud-utils,cloud-utils-growpart,gdisk --format qcow2 --root-password file:/tmp/rootpw   --update --selinux-relabel
