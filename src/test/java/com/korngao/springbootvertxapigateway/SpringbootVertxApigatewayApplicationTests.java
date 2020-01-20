package com.korngao.springbootvertxapigateway;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringbootVertxApigatewayApplicationTests {

	@Autowired
	private EurekaClient eurekaClient  ;


	@Test
	public void eurekaClientTest() {

		InstanceInfo srvInfo =  this.eurekaClient.getNextServerFromEureka("acturtorexample" , false );

		assertThat( srvInfo.getPort() ).isEqualTo(8081) ;

	}

}
