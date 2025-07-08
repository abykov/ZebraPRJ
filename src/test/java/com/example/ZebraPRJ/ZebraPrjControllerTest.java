package com.example.ZebraPRJ;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@TestExecutionListeners(listeners = {
        DependencyInjectionTestExecutionListener.class
})
public class ZebraPrjControllerTest extends AbstractTestNGSpringContextTests {

    @Autowired
    private TestRestTemplate restTemplate;

    private final String helloEndpoint = "/hello";
    private final String invalidEndpoint = "/invalid";
    private final String helloBody = "Hello";

    @Test (description = "Check GET /hello returns 200 and 'Hello'",
            groups = "Positive")
    public void testGetHelloSuccess(){
        ResponseEntity<String> response = restTemplate.getForEntity(helloEndpoint, String.class);
        assertThat(response.getStatusCode().value(), equalTo(200));
        assertThat(response.getBody(), equalTo(helloBody));
    }

    @Test (description = "Check Content-Type for GET /hello",
            groups = "Positive")
    public void testGetHelloContentType(){
        ResponseEntity<String> response = restTemplate.getForEntity(helloEndpoint, String.class);
        assertThat(response.getStatusCode().value(), equalTo(200));
        assertThat(response.getHeaders().getContentType().toString(), equalTo("text/plain;charset=UTF-8"));
    }

    @Test (description = "Check response time for GET /hello",
            groups = "Positive")
    public void testGetHelloResponseTime(){
        long startTime = System.currentTimeMillis();
        ResponseEntity<String> response = restTemplate.getForEntity(helloEndpoint, String.class);
        long elapsedTime = System.currentTimeMillis() - startTime;
        assertThat(response.getStatusCode().value(), equalTo(200));
        assertThat(elapsedTime, lessThan(1000L));
    }

    @Test (description = "Check GET /invalid returns 404",
            groups = "Negative")
    public void testGetInvalidEndpoint(){
        ResponseEntity<String> response = restTemplate.getForEntity(invalidEndpoint, String.class);
        assertThat(response.getStatusCode().value(), equalTo(404));
    }

    @Test (description = "Check POST /hello returns 405",
            groups = "Negative")
    public void testPostHelloMethodNotAllowed(){
        ResponseEntity<String> response = restTemplate.postForEntity(helloEndpoint, null, String.class);
        assertThat(response.getStatusCode().value(), equalTo(405));
    }


}
