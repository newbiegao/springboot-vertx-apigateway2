package com.korngao.springbootvertxapigateway;

import com.korngao.springbootvertxapigateway.config.SpringbootVertxApigatewayApplicationConfig;
import com.korngao.springbootvertxapigateway.verticle.ApiGatewayVerticle;
import com.netflix.discovery.EurekaClient;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableDiscoveryClient
@EnableEurekaServer
public class SpringbootVertxApigatewayApplication implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(SpringbootVertxApigatewayApplication.class) ;

	public static String APIGATEWAY_WORKER_POOL_NAME = "APIGATEWAY_WORK_POOL" ;

	@Autowired
	private Vertx vertx ;

	@Autowired
	private EurekaClient eurekaClient  ;

	@Value("${spring.application.name}")
	private String appName;

	@Autowired
	private SpringbootVertxApigatewayApplicationConfig config ;

	public static void main(String[] args) {

		// vertx system config

		InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
		System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.SLF4JLogDelegateFactory");
		System.setProperty("vertx.disableDnsResolver", "true");

		SpringApplication.run(SpringbootVertxApigatewayApplication.class, args);

	}

	@Override
	public void afterPropertiesSet() throws Exception {

		DeploymentOptions deploymentOptions = new DeploymentOptions() ;
		deploymentOptions.setWorker(true) ;
		deploymentOptions.setWorkerPoolName(APIGATEWAY_WORKER_POOL_NAME) ;
		deploymentOptions.setWorkerPoolSize(config.getInstances()) ;
		deploymentOptions.setMaxWorkerExecuteTime(2);
		deploymentOptions.setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS) ;
		deploymentOptions.setConfig(config.toJson()) ;

		for( int i = 0  ; i< config.getInstances() ; i++ ){

			ApiGatewayVerticle apiGatewayVerticle = new ApiGatewayVerticle() ;

			String gatewayPath  =  "/" + appName + "/:serviceName/*" ;

			apiGatewayVerticle.setEurekaClient(this.eurekaClient);
			apiGatewayVerticle.setGatewayName(appName);
			apiGatewayVerticle.setGatewayPath(gatewayPath);

			vertx.deployVerticle( apiGatewayVerticle , deploymentOptions ) ;

		}

	}
}
