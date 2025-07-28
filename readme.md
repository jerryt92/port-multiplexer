### README.md

#### English

For the English version of this README, please refer to [README_en.md](readme_en.md).

#### 中文

# 端口复用器

这是一个基于 Netty 实现的端口复用器

## 特性

- 监听一个外部端口，将不同类型的流量分发到不同的内部端口。
- 支持 TCP 和 UDP 协议。

图例：

```
                            ┌─ SSH <-> 22
30001 <-> port-multiplexer ─┼─ MQTT <-> 1883
                            └─ SSL <-> 443
```

## 使用说明

1. 编译项目：
    ```bash
    mvn clean package
    ```
编译完成后，会在 `target` 目录下生成一个 `tar.gz` 文件。

2. 解压文件：
    解压完成后的目录结构如下：
    ```
    port-multiplexer
    ├── bin
    │   ├── start_for_linux.sh
    │   ├── stop_for_linux.sh
    │   └── ...
    ├── lib
    ├── classes
    │    ├── conf.yaml
    │    └── ...
    └── ...
    ```
    其中 `bin` 目录下包含启动和停止脚本，`classes` 目录下包含配置文件。
3. 启动服务：
    ```bash
    ./bin/start_for_linux.sh --java_home /opt/jre # 指定 JRE 路径
    ```

## 许可证

本项目基于 MIT 许可证开源。详见 `LICENSE` 文件。