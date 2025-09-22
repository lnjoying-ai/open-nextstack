
# 关闭防火墙
systemctl stop firewalld
systemctl disable firewalld

# 关闭selinux
setenforce 0
awk -i inplace '/^SELINUX=/ { print "SELINUX=disabled" }' /etc/selinux/config

# 更新系统
yum update -y

# 安装编译工具
yum group  install -y "Development Tools"

# 安装libvirt及相关软件包
yum install -y libvirt virt-viewer virt-install python39 nginx libguestfs-tools

# 将libvirt用户添加到root组
usermod --append --groups libvirt root

# 拷贝服务开机启动脚本
cp /opt/nextstack/scripts/ovs.service /usr/lib/systemd/system
cp /opt/nextstack/scripts/lnjoying-novnc.service /usr/lib/systemd/system
cp /opt/nextstack/scripts/lnjoying-vmagent.service /usr/lib/systemd/system
cp /opt/nextstack/scripts/nginx.conf.vm /etc/nginx/nginx.conf

# 设置服务开机自启动
systemctl enable ovs
systemctl enable lnjoying-novnc
systemctl enable lnjoying-vmagent
systemctl enable nginx

# 编译安装openvswitch
cd ~
wget http://www.openvswitch.org/releases/openvswitch-2.13.6.tar.gz
tar xvfz openvswitch-2.13.6.tar.gz;cd openvswitch-2.13.6;./configure;make install;modprobe openvswitch

# 创建python环境
python3.9 -m venv /opt/.v3.9

# 配置环境变量
echo "export PATH=$PATH:/usr/local/share/openvswitch/scripts" >>~/.bashrc
echo "source /opt/.v3.9/bin/activate" >>~/.bashrc
echo "alias cdd=\"cd /opt/nextstack\"" >>~/.bashrc
echo "alias vi=vim" >>~/.bashrc
echo "set -o vi" >>~/.bashrc
source ~/.bashrc

# 安装python库包
pip install -r /opt/nextstack/requirements.txt

#umount /home
#lvresize -L 10G /dev/cs/home
#mkfs.xfs -f /dev/cs/home
#mount /home
#mkdir /vms
#lvcreate -L 334G -n vms cs
#mkfs.xfs -f /dev/cs/vms
#mount /dev/cs/vms /vms

# 创建/vms目录
mkdir -p /vms/backing; mkdir -p /vms/tokens; mkdir -p /vms/volumes; mkdir -p /vms/cdrom
chown -R qemu:qemu /vms/backing
chown -R qemu:qemu /vms/cdrom
chown -R qemu:qemu /vms/volumes

# 配置glusterfs目录自动挂载
#echo "/dev/mapper/cs-vms      /vms                    xfs     defaults        0 0" >>/etc/fstab

# 启动服务
#systemctl start  ovs
#systemctl start  lnjoying-novnc
#systemctl start  lnjoying-vmagent
#systemctl start  nginx
