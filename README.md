# eating-comment-platform

## 吃货点评前端

https://github.com/1czy/eating-comment-platform

- 拉取项目后在Nginx目录（即存在Nginx.exe)目录打开cmd 输入（失败以管理员身份运行）

  ```sh
  start nginx.exe
  ```

## 吃货点评平台后端

1. 技术选型及版本

 - JDK 1.8

 - SpringBoot 2.3.12.RELEASE

 - MySQL 8

 - Rabbitmq 3-management

 - Redis 6.2.6

2. 项目启动

 - resources/db 运行sql文件

 - 修改 util 包下 SMSUtil.java配置，该文件有相关说明，建议使用腾讯云短信服务（申请个人公众号，免费100条），也可不用，使用时被注释，这只是短信功能的实现；用可以在控制台看到日志输出

 - 修改 util 包 SystemConstants.java 文件路径为自身 nginx 路径

   - ```java
     public static final String IMAGE_UPLOAD_DIR = "D:\\Code\\nginx-1.18.0\\html\\hmdp\\imgs\\";
     ```

 - 修改application.properties相关配置，如 MySQL、Rabbitmq、Redis

 - 启动MySQL、Rabbitmq、Redis服务(建议linux安装docker启动，更方便)

   

3. 打开游览器输入 http://localhost:8080/
