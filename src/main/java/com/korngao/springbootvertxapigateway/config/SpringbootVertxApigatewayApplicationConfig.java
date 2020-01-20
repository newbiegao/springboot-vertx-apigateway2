package com.korngao.springbootvertxapigateway.config;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;

@Configuration
@ConfigurationProperties(prefix="api.gateway")
public class SpringbootVertxApigatewayApplicationConfig {


    /** 服务监听端口号 */
    public static final Integer PORT = 8090 ;

    /** 服务启动默认实例数 */
    public static final Integer INSTANCES = 8 ;

    /** 默认的请求超时时间毫秒，系统默认60S */
    private final int DEFAULT_TIME_OUT = 1000;

    /** httpclient 连接池大小 */
    private final int DEFAULT_CLIENT_POOL_SIZE = 500 ;


    @NotNull
    private Integer port = PORT ;

    @NotNull
    private Integer instances = INSTANCES ;

    /** 连接超时时间默认6000ms */
    @NotNull
    private long timeOut = DEFAULT_TIME_OUT;

    @NotNull
    private int clientPoolSize  = DEFAULT_CLIENT_POOL_SIZE ;

    public long getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(long timeOut) {
        this.timeOut = timeOut;
    }

    public int getClientPoolSize() {
        return clientPoolSize;
    }

    public void setClientPoolSize(int clientPoolSize) {
        this.clientPoolSize = clientPoolSize;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getInstances() {
        return instances;
    }

    public void setInstances(Integer instances) {
        this.instances = instances;
    }

    public JsonObject toJson(){

        JsonObject jsonObject = new JsonObject() ;

        jsonObject.put("port" , this.port ) ;
        jsonObject.put("timeOut" , this.timeOut);
        jsonObject.put("clientPoolSize" , this.clientPoolSize) ;

        return jsonObject ;
    }
}
