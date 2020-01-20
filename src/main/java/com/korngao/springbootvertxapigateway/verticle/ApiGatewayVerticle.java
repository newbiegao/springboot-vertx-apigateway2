package com.korngao.springbootvertxapigateway.verticle;


import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.http.*;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;


public class ApiGatewayVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiGatewayVerticle.class) ;

    private HttpClient httpClient = null;

    private Router httpRouter = null;

    private EurekaClient eurekaClient ;

    private String gatewayName  ;

    private String gatewayPath ;

    public String getGatewayPath() {
        return gatewayPath;
    }

    public void setGatewayPath(String gatewayPath) {
        this.gatewayPath = gatewayPath;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public EurekaClient getEurekaClient() {
        return eurekaClient;
    }

    public void setEurekaClient(EurekaClient eurekaClient) {
        this.eurekaClient = eurekaClient;
    }

    @Override
    public void start() {

        createHttpClient() ;
        createHttpRouter() ;
        createHttpServer() ;

    }

    private void createHttpClient(){

        HttpClientOptions httpClientOptions = new HttpClientOptions() ;
        httpClientOptions.setConnectTimeout(config().getInteger("timeOut")) ;
        httpClientOptions.setKeepAlive(true) ;
        httpClientOptions.setMaxPoolSize(config().getInteger("clientPoolSize")) ;

        httpClientOptions.setReusePort(true) ;
        httpClientOptions.setReuseAddress(true) ;
        httpClientOptions.setTcpCork(true) ;
        httpClientOptions.setTcpFastOpen(true) ;
        httpClientOptions.setTcpKeepAlive(true) ;
        httpClientOptions.setTcpQuickAck(true) ;
        httpClientOptions.setTryUseCompression(true) ;

        this.httpClient = vertx.createHttpClient(httpClientOptions) ;

        LOGGER.info(" create httpClient info -> " + httpClientOptions.toJson().toString());

    }

    private void createHttpRouter(){

        Router proxyRouter = Router.router(vertx);

        proxyRouter.route( this.gatewayPath ).handler( (RoutingContext requestHandler) -> {

            String serviceName = requestHandler.request().getParam("serviceName") ;

            if( serviceName.isEmpty() ){

                requestHandler.response().setStatusCode(404) ;
                String uri = requestHandler.request().uri()  ;
                requestHandler.response().end(" apigateway error : Request URI " +  uri + " ; can't find serviceName " );
                return ;
            }

            // 通过服务注册中心获取远程服务地址信息
            InstanceInfo backServer = null ;
            try{

                 backServer  = this.eurekaClient.getNextServerFromEureka( serviceName , false ) ;
            }
            catch( RuntimeException e ){

                String uri = requestHandler.request().uri()  ;
                requestHandler.response().setStatusCode(404).end(" apigateway error : Request URI " +  uri + " ; can't find serviceName : " + serviceName );
                LOGGER.warn(e.getMessage());
                return;
            }

            // 处理请求
            // requestHandler.response().end("time" + System.currentTimeMillis()  + " instacne count : " + vertx.getOrCreateContext().getInstanceCount() + " service info : " + backServer.toString()  );

            // 处理请求头
            RequestOptions requestOptions = new RequestOptions() ;
            requestOptions.setHeaders(requestHandler.request().headers()) ;
            requestOptions.setHost(backServer.getHostName()) ;
            requestOptions.setPort(backServer.getPort());
            requestOptions.setURI(getRemoteServicePath(serviceName , requestHandler.request().uri())) ;


            // 发起请求并处理 response 对象 client response -> server response
            HttpClientRequest httpClientRequest = httpClient.request( requestHandler.request().method() , requestOptions , response -> {

                requestHandler.response().headers().addAll(response.headers());
                requestHandler.response().setStatusCode(response.statusCode());
                requestHandler.response().setStatusMessage(response.statusMessage());

                // 如果远程响应没有数据返回数据需要设置Chunked模式
                if (response.headers().get("Content-Length") == null) {
                    requestHandler.response().setChunked(true);
                }

                // client response --> server response
                Pump responsePump =  Pump.pump( response , requestHandler.response()).start();

                // body 接收完成后执行
                response.endHandler( handler -> {

                    requestHandler.response().end();
                    responsePump.stop() ;

                }) ;

                response.exceptionHandler( error -> {

                    requestHandler.response().setStatusCode(500) ;
                    requestHandler.response().end("api gateway error :" + error.getMessage() );

                } );

            } ) ;

            if (requestHandler.request().headers().get("Content-Length")==null) {
                httpClientRequest.setChunked(true);
            }

            // server request --> client request
            Pump requestPump = Pump.pump( requestHandler.request() , httpClientRequest ).start() ;

            httpClientRequest.exceptionHandler( error -> {

                requestPump.stop() ;
                requestHandler.request().connection().close();
                requestHandler.request().response().setStatusCode(500).end( " api gateway request copy error : " + error.toString());
                LOGGER.warn(" api gateway request remote service error : " + error.toString() );

            } );

            // 监听数据是否写完,写完发送请求
            requestHandler.request().endHandler( handler ->{

                httpClientRequest.end();
                requestPump.stop() ;

            });

            requestHandler.request().exceptionHandler( error -> {

                requestPump.stop() ;
                LOGGER.warn(" api gateway request copy  error : " + error.toString() );

            }) ;

            proxyRouter.exceptionHandler( handler -> {

                requestHandler.response().setStatusCode(500).end(" api gateway inner error : " + handler.getMessage() );

            } ) ;

            proxyRouter.errorHandler( 500 , handler -> {

               System.out.println( " error 500  --------------- " ) ;

            } ) ;

        } ) ;

        this.httpRouter = proxyRouter ;

    }


    private void createHttpServer() {

        // 创建http服务器
        HttpServerOptions serverOptions = new HttpServerOptions() ;

        if(vertx.isNativeTransportEnabled()) {
            serverOptions.setTcpFastOpen(true).setTcpCork(true).setTcpQuickAck(true).setReusePort(true);
        }
        serverOptions.setReusePort(true) ;
        serverOptions.setReuseAddress(true) ;
        serverOptions.setTcpKeepAlive(true) ;
        serverOptions.setCompressionSupported(true) ;


        serverOptions.setPort(config().getInteger("port")) ;

        vertx.createHttpServer(serverOptions).requestHandler(httpRouter)
            .exceptionHandler( error -> {

                LOGGER.error(" api gateway error : {} " , error.getMessage() );

            }).listen(res -> {
                if (res.succeeded()) {

                    LOGGER.info(MessageFormat.format(" Running on port {0} by HTTP", Integer.toString(config().getInteger("port"))));

                } else {

                    LOGGER.error(MessageFormat.format( "create HTTP Server failed : {0} " , res.cause() )); ;

                }
        });

    }


    private String getRemoteServicePath( String serviceName , String uri  )
    {
        String prefix = "/" + this.gatewayName + "/" + serviceName ;
        String path = uri.replace(prefix, "") ;
        return path ;
    }



}
