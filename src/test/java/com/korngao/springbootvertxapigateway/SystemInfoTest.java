package com.korngao.springbootvertxapigateway;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Locale;

@RunWith(SpringRunner.class)
public class SystemInfoTest {

    @Test
    public void evnInfo(){

        System.out.println( System.getProperty("os.name").toLowerCase(Locale.US).contains("mac"));

    }

}
