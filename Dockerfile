FROM hub.c.163.com/library/java:8-alpine
#作者
MAINTAINER VincentVic <hougq.rd@gmail.com>

#系统编码
ENV LANG=C.UTF-8 LC_ALL=C.UTF-8

#声明一个挂载点，容器内此路径会对应宿主机的某个文件夹
VOLUME /tmp/gateway

#应用构建成功后的jar文件被复制到镜像内，名字也改成了app.jar
ADD meshed-cloud-gateway-start/target/*.jar app.jar
#设置变量 JAVA_OPTS
ENV JAVA_OPTS=""

#启动容器时的进程
ENTRYPOINT java ${JAVA_OPTS} -jar app.jar

#暴露9000端口
EXPOSE 9000

#执行前 clean package -U -DskipTests