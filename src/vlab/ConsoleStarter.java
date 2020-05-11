package vlab;

import rlcp.Rlcp;
import rlcp.RlcpRequestBody;
import rlcp.RlcpResponseBody;
import rlcp.exception.BadRlcpBodyException;
import rlcp.server.ServerMethod;
import rlcp.server.flow.RlcpRequestFlow;
import rlcp.server.processor.factory.ProcessorFactoryContainer;

import java.io.BufferedReader;
import java.io.File;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConsoleStarter {

    public RlcpResponseBody runConsoleServer(File request, ProcessorFactoryContainer container){
        String rawRequest = readFile(request.toPath());
        try {
            RlcpRequestBody rlcpRequestBody = Rlcp.parseRequestBody(rawRequest);
            runConsoleServer(rlcpRequestBody, container);
        } catch (BadRlcpBodyException e) {
            e.printStackTrace();
        }
        return null;
    }


    public RlcpResponseBody runConsoleServer(RlcpRequestBody rlcpRequestBody, ProcessorFactoryContainer container) {
        RlcpRequestFlow flow = null;
        System.out.println(rlcpRequestBody.getMethod().getName());
        switch (rlcpRequestBody.getMethod().getName().toLowerCase()) {
            case "generate":
                flow = ServerMethod.GENERATE.getFlow();
                break;
            case "check":
                flow = ServerMethod.CALCULATE.getFlow();
                break;
            case "calculate":
                flow = ServerMethod.CHECK.getFlow();
                break;
        }

        return flow.processBody(container, rlcpRequestBody);
    }

    private static String readFile(Path p) {
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
