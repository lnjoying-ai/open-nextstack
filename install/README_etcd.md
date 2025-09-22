# Etcd 集群配置功能说明

## 功能概述

`config_etcd_cluster()` 函数现在支持以下两种 etcd 部署模式：

### 1. 单节点模式 (Single Node)
- 当配置文件中只有一个 etcd 节点时自动启用
- 使用 `--force-new-cluster` 参数
- 启动速度更快，适合开发环境或小规模部署
- 服务名称: `etcd key-value store (single node)`

### 2. 集群模式 (Cluster Mode) 
- 当配置文件中有多个 etcd 节点时自动启用
- 使用标准的集群配置参数
- 提供高可用性，适合生产环境
- 服务名称: `etcd key-value store (cluster node)`

## 配置文件格式

### 单节点配置示例
```yaml
nodes:
- host: 192.168.8.111
  user: root
  passwd: lnjoying
  etcd:
    etcd_name: etcd-single
```

### 集群配置示例（3节点）
```yaml
nodes:
- host: 192.168.8.111
  user: root
  passwd: lnjoying
  etcd:
    etcd_name: etcd01

- host: 192.168.8.112
  user: root
  passwd: lnjoying
  etcd:
    etcd_name: etcd02

- host: 192.168.8.113
  user: root
  passwd: lnjoying
  etcd:
    etcd_name: etcd03
```

## 自动功能

### 1. 自动检测模式
函数会自动检测配置中的 etcd 节点数量：
- 1个节点 → 单节点模式
- 多个节点 → 集群模式

### 2. 自动安装
- 下载并安装 etcd 3.5.9 版本
- 支持代理下载（如果配置了proxy）
- 自动创建 etcd 用户和数据目录

### 3. 自动配置
- 生成适合的 systemd 服务文件
- 配置防火墙端口（2379/2380）
- 设置开机自启动

### 4. 自动验证
- 检查服务状态
- 验证集群健康状态
- 测试读写操作
- 收集诊断信息（如果失败）

## 主要区别

| 特性 | 单节点模式 | 集群模式 |
|------|------------|----------|
| 节点数量 | 1 | ≥2 |
| 启动参数 | `--force-new-cluster` | 标准集群参数 |
| 集群令牌 | `etcd-single-node` | `etcd-cluster` |
| 启动时间 | 较快 (5秒) | 较慢 (10秒) |
| 验证重试 | 15次 | 30次 |
| 高可用性 | 无 | 有 |

## 使用方法

1. 编辑配置文件 `config.etcd.example.yml`
2. 根据需要选择单节点或集群模式配置
3. 运行部署脚本：
   ```bash
   python gnext_depoly_geo.py -C config.etcd.example.yml
   ```

## 测试

运行测试脚本验证配置：
```bash
python test_etcd_config.py
```

## 支持的操作系统

- CentOS/RHEL (firewall-cmd)
- Ubuntu (ufw)

## 端口要求

- 2379: 客户端通信端口
- 2380: 节点间通信端口（集群模式）

## 故障排除

如果 etcd 配置失败，函数会自动：
1. 收集每个节点的服务状态
2. 显示最近的 etcd 日志
3. 提供详细的错误信息

通过 `journalctl -u etcd` 查看详细日志。
