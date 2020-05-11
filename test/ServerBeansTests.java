import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rlcp.calculate.RlcpCalculateRequest;
import rlcp.calculate.RlcpCalculateRequestBody;
import rlcp.calculate.RlcpCalculateResponse;
import rlcp.calculate.RlcpCalculateResponseBody;
import rlcp.check.RlcpCheckRequest;
import rlcp.check.RlcpCheckRequestBody;
import rlcp.check.RlcpCheckResponse;
import rlcp.check.RlcpCheckResponseBody;
import rlcp.exception.BadRlcpRequestException;
import rlcp.exception.BadRlcpResponseException;
import rlcp.exception.RlcpException;
import rlcp.generate.RlcpGenerateRequest;
import rlcp.generate.RlcpGenerateRequestBody;
import rlcp.generate.RlcpGenerateResponse;
import rlcp.generate.RlcpGenerateResponseBody;
import rlcp.server.Server;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-java-server-config.xml")
//@ContextConfiguration("classpath:test-js-server-config.xml")
public class ServerBeansTests {

    @Autowired
    private Server server;
    @Autowired
    private String url;
    @Resource
    private HashMap<RlcpGenerateRequestBody, RlcpGenerateResponseBody> generateTest;
    @Resource
    private HashMap<RlcpCheckRequestBody, RlcpCheckResponseBody> checkTest;
    @Resource
    private HashMap<RlcpCalculateRequestBody, RlcpCalculateResponseBody> calculateTest;


    private Thread serverThread;

    @Before
    public void startServer() {
        serverThread = new Thread(server);
        serverThread.start();
    }

    @After
    public void closeServer() throws InterruptedException {
        server.stop();
        serverThread.join();
    }

    @Test
    public void testGenerateLogic() {
        generateTest.forEach(
                (generateRequest, generateResponse) -> {
                    RlcpGenerateRequest request = generateRequest.prepareRequest(url);
                    try {
                        RlcpGenerateResponse actualResponse = request.execute();

                        assertThat(actualResponse.toString(), is(not(equalTo(""))));
                        assertEquals(actualResponse.getMethod().getName(), generateResponse.getMethod().getName());

                        assertEquals(actualResponse.getBody().getGeneratingResult().getText(), generateResponse.getGeneratingResult().getText());
                        assertEquals(actualResponse.getBody().getGeneratingResult().getCode(), generateResponse.getGeneratingResult().getCode());
                        assertEquals(actualResponse.getBody().getGeneratingResult().getInstructions(), generateResponse.getGeneratingResult().getInstructions());

                    } catch (RlcpException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    public void testCheckLogic() {
        checkTest.forEach(
                (checkRequest, checkResponse) -> {
                    RlcpCheckRequest request = checkRequest.prepareRequest(url);
                    try {
                        RlcpCheckResponse actualResponse = request.execute();

                        assertThat(actualResponse.toString(), is(not(equalTo(""))));
                        assertEquals(actualResponse.getMethod().getName(), checkResponse.getMethod().getName());

                        actualResponse.getBody().getResults().forEach(
                                (result) -> {
                                    int id = result.getId();
                                    assertEquals(result.getOutput(), checkResponse.getResultById(id).getOutput());
                                    assertEquals(result.getResult(), checkResponse.getResultById(id).getResult());
                                }
                        );

                    } catch (RlcpException e) {
                        e.printStackTrace();
                    }
                });
    }

    @Test
    public void testCalculate() {
        calculateTest.forEach(
                (calculateRequest, calculateResponse) -> {
                    RlcpCalculateRequest request = calculateRequest.prepareRequest(url);
                    try {
                        RlcpCalculateResponse actualResponse = request.execute();

                        assertThat(actualResponse.toString(), is(not(equalTo(""))));
                        assertEquals(actualResponse.getMethod().getName(), calculateResponse.getMethod().getName());

                        assertEquals(actualResponse.getBody().getCalculatingResult().getText(), calculateResponse.getCalculatingResult().getText());
                        assertEquals(actualResponse.getBody().getCalculatingResult().getCode(), calculateResponse.getCalculatingResult().getCode());
                    } catch (RlcpException e) {
                        e.printStackTrace();
                    }
                });
    }

}
