import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rlcp.Rlcp;
import rlcp.calculate.RlcpCalculateRequest;
import rlcp.calculate.RlcpCalculateRequestBody;
import rlcp.calculate.RlcpCalculateResponse;
import rlcp.calculate.RlcpCalculateResponseBody;
import rlcp.check.RlcpCheckRequest;
import rlcp.check.RlcpCheckRequestBody;
import rlcp.check.RlcpCheckResponse;
import rlcp.check.RlcpCheckResponseBody;
import rlcp.generate.RlcpGenerateRequest;
import rlcp.generate.RlcpGenerateRequestBody;
import rlcp.generate.RlcpGenerateResponse;
import rlcp.generate.RlcpGenerateResponseBody;
import rlcp.method.Calculate;
import rlcp.method.Check;
import rlcp.method.Generate;
import rlcp.server.Server;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-java-server-config.xml")
//@ContextConfiguration("classpath:test-js-server-config.xml")
public class ServerFilesTests {

    @Autowired
    private Server server;
    @Autowired
    private String url;
    @Autowired
    private String testDir;


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
        PathMatcher requestPathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.generateRlcpRequest");
        try {
            Files.walk(new File(testDir).toPath()).filter(p -> requestPathMatcher.matches(p)).forEach(
                    p -> {
                        String rawRequest = readFile(p);
                        try {

                            RlcpGenerateRequestBody rlcpRequestBody = Rlcp.parseRequestBody(rawRequest, RlcpGenerateRequestBody.class);
                            RlcpGenerateRequest rlcpRequest = rlcpRequestBody.prepareRequest(url);
                            RlcpGenerateResponse actualRlcpResponse = rlcpRequest.execute();

                            Path responsePath = p.getParent().resolve(p.getFileName().toString().replaceAll(".generateRlcpRequest", ".generateRlcpResponse"));
                            String rawResponse = readFile(responsePath);
                            RlcpGenerateResponseBody rlcpResponse = Rlcp.parseResponseBody(rawResponse, RlcpGenerateResponseBody.class);

                            assertEquals(actualRlcpResponse.getMethod().getName(), rlcpResponse.getMethod().getName());
                            assertEquals(actualRlcpResponse.getBody().getGeneratingResult().getText(), rlcpResponse.getGeneratingResult().getText());
                            assertEquals(actualRlcpResponse.getBody().getGeneratingResult().getCode(), rlcpResponse.getGeneratingResult().getCode());
                            assertEquals(actualRlcpResponse.getBody().getGeneratingResult().getInstructions(), rlcpResponse.getGeneratingResult().getInstructions());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCheckLogic(){
        PathMatcher requestPathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.checkRlcpRequest");
        try {
            Files.walk(new File(testDir).toPath()).filter(p -> requestPathMatcher.matches(p)).forEach(
                    p -> {
                        String rawRequest = readFile(p);
                        try {

                            RlcpCheckRequestBody rlcpRequestBody = Rlcp.parseRequestBody(rawRequest, RlcpCheckRequestBody.class);
                            RlcpCheckRequest rlcpRequest = rlcpRequestBody.prepareRequest(url);
                            RlcpCheckResponse actualRlcpResponse = rlcpRequest.execute();

                            Path responsePath = p.getParent().resolve(p.getFileName().toString().replaceAll(".checkRlcpRequest", ".checkRlcpResponse"));
                            String rawResponse = readFile(responsePath);
                            RlcpCheckResponseBody rlcpResponse = Rlcp.parseResponseBody(rawResponse, RlcpCheckResponseBody.class);

                            assertEquals(actualRlcpResponse.getMethod().getName(), rlcpResponse.getMethod().getName());
                            actualRlcpResponse.getBody().getResults().forEach(
                                    (result) -> {
                                        int id = result.getId();
                                        assertEquals(result.getOutput(), rlcpResponse.getResultById(id).getOutput());
                                        assertEquals(result.getResult(), rlcpResponse.getResultById(id).getResult());
                                    }
                            );

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCalculateLogic(){
        PathMatcher requestPathMatcher = FileSystems.getDefault().getPathMatcher("glob:**.calculateRlcpRequest");
        try {
            Files.walk(new File(testDir).toPath()).filter(p -> requestPathMatcher.matches(p)).forEach(
                    p -> {
                        String rawRequest = readFile(p);
                        try {

                            RlcpCalculateRequestBody rlcpRequestBody = Rlcp.parseRequestBody(rawRequest, RlcpCalculateRequestBody.class);
                            RlcpCalculateRequest rlcpRequest = rlcpRequestBody.prepareRequest(url);
                            RlcpCalculateResponse actualRlcpResponse = rlcpRequest.execute();

                            Path responsePath = p.getParent().resolve(p.getFileName().toString().replaceAll(".calculateRlcpRequest", ".calculateRlcpResponse"));
                            String rawResponse = readFile(responsePath);
                            RlcpCalculateResponseBody rlcpResponse = Rlcp.parseResponseBody(rawResponse, RlcpCalculateResponseBody.class);

                            assertEquals(actualRlcpResponse.getMethod().getName(), rlcpResponse.getMethod().getName());
                            assertEquals(actualRlcpResponse.getBody().getCalculatingResult().getText(), rlcpResponse.getCalculatingResult().getText());
                            assertEquals(actualRlcpResponse.getBody().getCalculatingResult().getCode(), rlcpResponse.getCalculatingResult().getCode());

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readFile(Path p) {
        String rawRequest = "";
        try {
            StringBuilder rawRequestBuilder = new StringBuilder();
            CharBuffer charBuffer = CharBuffer.allocate((int) Files.size(p));
            BufferedReader br = Files.newBufferedReader(p);
            while (br.ready()) {
                br.read(charBuffer);
                charBuffer.flip();
                rawRequestBuilder.append(charBuffer.toString());
            }
            rawRequest = rawRequestBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rawRequest;
    }
}
