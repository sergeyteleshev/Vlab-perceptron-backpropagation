package vlab.server_js;

import org.springframework.context.support.GenericXmlApplicationContext;
import rlcp.RlcpResponseBody;
import rlcp.server.Server;
import rlcp.server.processor.factory.ProcessorFactoryContainer;

import java.io.File;

/**
 * Main class for RLCP-server starting.
 */
public class Starter {

    /**
     * Defines applicable logic modules, configuration path and starts RLCP-server
     */
    public static void main(String[] args) throws Exception {
        GenericXmlApplicationContext context = new GenericXmlApplicationContext();
        context.load("classpath:vlab/server_js/js-server-config.xml");
        context.refresh();

        if (args.length == 0) {
            new Thread(context.getBean("server", Server.class)).start();
        } else {
            vlab.ConsoleStarter consoleStarter = new vlab.ConsoleStarter();
            RlcpResponseBody responseBody = consoleStarter.runConsoleServer(new File(args[0]), context.getBean("container", ProcessorFactoryContainer.class));
            System.out.println(responseBody);
        }
    }
}