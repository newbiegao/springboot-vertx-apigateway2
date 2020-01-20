package com.korngao.springbootvertxapigateway.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.micrometer.MicrometerMetricsOptions;
import io.vertx.micrometer.VertxPrometheusOptions;
import io.vertx.micrometer.backends.BackendRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;
import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix="vertx")
public class ApiGatewayVertxConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiGatewayVertxConfig.class) ;

    private final static int DEFAULT_PROMETHUES_PORT = 8082 ;
    private final static String DEFAULT_PROMETHUES_PATH = "/metrics/vertx" ;

    @NotNull
    private Integer eventLoopPoolSize ;

    @NotNull
    private Integer blockingPoolSize ;

    @NotNull
    private Integer workerPoolSize ;

    @NotNull
    private Integer loopExecuteTime ;

    @NotNull
    private String prometheusPath = DEFAULT_PROMETHUES_PATH ;

    @NotNull
    private Integer prometheusPort = DEFAULT_PROMETHUES_PORT ;

    @NotNull
    private Boolean enablePrometheus = false  ;

    public Boolean getEnablePrometheus() {
        return enablePrometheus;
    }

    public void setEnablePrometheus(Boolean enablePrometheus) {
        this.enablePrometheus = enablePrometheus;
    }

    public String getPrometheusPath() {
        return prometheusPath;
    }

    public void setPrometheusPath(String prometheusPath) {
        this.prometheusPath = prometheusPath;
    }

    public Integer getPrometheusPort() {
        return prometheusPort;
    }

    public void setPrometheusPort(Integer prometheusPort) {
        this.prometheusPort = prometheusPort;
    }

    public Integer getEventLoopPoolSize() {
        return eventLoopPoolSize;
    }

    public void setEventLoopPoolSize(Integer eventLoopPoolSize) {
        this.eventLoopPoolSize = eventLoopPoolSize;
    }

    public Integer getBlockingPoolSize() {
        return blockingPoolSize;
    }

    public void setBlockingPoolSize(Integer blockingPoolSize) {
        this.blockingPoolSize = blockingPoolSize;
    }

    public Integer getWorkerPoolSize() {
        return workerPoolSize;
    }

    public void setWorkerPoolSize(Integer workerPoolSize) {
        this.workerPoolSize = workerPoolSize;
    }

    public Integer getLoopExecuteTime() {
        return loopExecuteTime;
    }

    public void setLoopExecuteTime(Integer loopExecuteTime) {
        this.loopExecuteTime = loopExecuteTime;
    }

    @Bean
    public Vertx vertxInstance() {

        VertxOptions vertxOptions = new VertxOptions() ;

        vertxOptions.setWorkerPoolSize(workerPoolSize) ;
        vertxOptions.setInternalBlockingPoolSize(blockingPoolSize) ;
        vertxOptions.setEventLoopPoolSize(eventLoopPoolSize);
        vertxOptions.setMaxEventLoopExecuteTime(loopExecuteTime) ;
        vertxOptions.setMaxEventLoopExecuteTimeUnit(TimeUnit.SECONDS) ;
        vertxOptions.setPreferNativeTransport(true) ;

       // Prometheus config
        if( enablePrometheus ){

            MicrometerMetricsOptions metricsOptions = new MicrometerMetricsOptions();

            VertxPrometheusOptions vertxPrometheusOptions = new VertxPrometheusOptions();
            HttpServerOptions httpServerOptions = new HttpServerOptions() ;
            httpServerOptions.setPort(prometheusPort) ;
            vertxPrometheusOptions.setEnabled(true) ;
            vertxPrometheusOptions.setStartEmbeddedServer(true) ;
            vertxPrometheusOptions.setEmbeddedServerOptions(httpServerOptions) ;
            vertxPrometheusOptions.setEmbeddedServerEndpoint(prometheusPath) ;

            metricsOptions.setPrometheusOptions(vertxPrometheusOptions) ;
            metricsOptions.setEnabled(true) ;
            vertxOptions.setMetricsOptions(metricsOptions) ;

        }

        Vertx vertx =  Vertx.vertx(vertxOptions) ;

        // 注册 Prometheus 其它指标
        MeterRegistry registry = BackendRegistries.getDefaultNow();
        if( registry != null  ){
            new ClassLoaderMetrics().bindTo(registry);
            new JvmMemoryMetrics().bindTo(registry);
            new JvmGcMetrics().bindTo(registry);
            new ProcessorMetrics().bindTo(registry);
            new JvmThreadMetrics().bindTo(registry);
        }

        LOGGER.info( " starting vertx config info -> " + vertxOptions.toString()  ) ;

        return vertx ;

    }

}
