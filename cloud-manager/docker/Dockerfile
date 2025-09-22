#FROM lnjoying/lnjoying_jdk8_ubuntu:v0.1.0
FROM swr.cn-north-1.myhuaweicloud.com/lnjoying/lnjoying_jdk8_ubuntu:v0.1.0

RUN apt-get update \
    && apt-get install -y  iputils-ping

ADD lnjoying_cloud.tar.gz /root

RUN cd /root/lnjoying_cloud/script && chmod +x *.sh

WORKDIR /root/lnjoying_cloud/script

ENTRYPOINT ["/bin/bash", "-c"]
CMD ["./startall.sh && ./sleep.sh"]
