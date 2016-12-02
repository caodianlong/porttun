# porttun
这是一个包含了TCP端口映射(PortMap)和TCP反向隧道链接(Reverse Tunnel)的JAVA程序。

可以实现以下或更多的目的：

    1、端口映射功能。将本机的TCP端口映射到另一机器（也可以是同一台机器）的另一端口（也可以是另一机器的同一端口）。
    
    2、端口映射加上反向隧道链接可以实现内网穿透，使外部机器可以访问局域网内部的机器。
    
    3、可以在只有单向网络权限的网络中实现双向网络权限。
    
    4、其他更多用途
    
    
    
## 用途示例：

A 不能直接访问 C，但B可以访问C，在B上运行porttun，配置80端口映射到C的80端口，即可实现A访问C。
```
    [Host A] ---> [Host B : 80] ----> [Host C : 80]
```
        
```
    <map SrcIp="" SrcPort="80" DestIP="Host C" DestPort="80" ProxyHost="" />
```

B不能访问C，但C可以访问B，在B上运行porttun监听，C上配置反向连接到B，即在B和C之间建立反向隧道链接，A即可访问C。
```
    [Host A] ---> [Host B : 80] <----reveres tunnel----> [Host C : 80] 
```
    
```
    Host B配置参考：
    
    <portmap>
        <map SrcIp="" SrcPort="80" DestIP="Host C" DestPort="80" ProxyHost="" />
    </portmap>
    <pool>
        <port>1088</port>
        <tunnel>
        </tunnel>
    </pool>
```
    
```
    Host C配置参考：
    
    <portmap>
    </portmap>
    <pool>
        <port>0</port>
        <tunnel>
            <connect DestIP="Host B" DestPort="1088" />
        </tunnel>
    </pool>
```

A、B与C、D在不同的两个子网中，A、B不能访问C、D，但C可以访问B，在B上运行porttun监听，C上配置反向连接到B，即在B和C之间建立反向隧道链接，在B上配置端口映射并指定C为ProxyHost，A即可访问D。
```
    [Host A] ---> [Host B : 80] <----reveres tunnel----> [Host C : 80] ----> [Host D]
```
    
```
    Host B配置参考：
    
    <portmap>
        <map SrcIp="" SrcPort="80" DestIP="Host D" DestPort="80" ProxyHost="Host C" />
    </portmap>
    <pool>
        <port>1088</port>
        <tunnel>
        </tunnel>
    </pool>
```
    
```
    Host C配置参考：
    
    <portmap>
    </portmap>
    <pool>
        <port>0</port>
        <tunnel>
            <connect DestIP="Host B" DestPort="1088" />
        </tunnel>
    </pool>
```
    
你还可以想到更多其他用途……

## 配置说明：
```
    porttunnel-config.xml为配置文件。

    配置文件分两个段落<portmap>和<pool>，<portmap>为端口映射表配置，<pool>为反向隧道连接配置。
    
    <portmap>段配置：
        SrcIp       源IP
        SrcPort     源端口
        DestIP      目标IP
        DestPort    目标端口
        ProxyHost   指定使用来自某IP地址的反向隧道链接
    
    <pool>段配置：
        port        等待反向连接的监听端口
        tunnel段    主动发起反向连接
           DestIP   反向连接目标IP
           DestPort 反向链接目标端口
```

## 配置示例：
```
    <config>
        <portmap>
            <!-- "ProxyHost" is host of a tunnel where it from  -->
            <map SrcIp="" SrcPort="80" DestIP="172.18.18.9" DestPort="80" ProxyHost="" />
            <map SrcIp="" SrcPort="8001" DestIP="172.18.13.44" DestPort="8090" ProxyHost="" />
            <map SrcIp="" SrcPort="9002" DestIP="104.225.148.114" DestPort="9002" ProxyHost="172.18.9.52" />
        </portmap>
        <pool>
            <!-- the "Port" is listening port for "tunnel" form the other side, 0 meant no listening -->
            <port>0</port>
            <!-- "tunnel" is connection to the other side who is listening -->
            <tunnel>
                <connect DestIP="10.137.37.2" DestPort="1088" />
            </tunnel>
        </pool>
    </config>
```
