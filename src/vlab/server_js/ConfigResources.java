package vlab.server_js;

import org.apache.commons.io.IOUtils;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;


public class ConfigResources implements ResourceLoaderAware {

    private ResourceLoader resourceLoader;
    private String resource;
    private File tempFile;

    public void init() throws IOException, URISyntaxException {
        tempFile = File.createTempFile("file", "js");
        tempFile.deleteOnExit();
        FileOutputStream out = new FileOutputStream(tempFile);
        IOUtils.copy(ConfigResources.class.getResourceAsStream(resource), out);
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setResourceLoader(ResourceLoader arg0) {
        this.resourceLoader = arg0;
    }

    public File getResource() {
        return tempFile;
    }

}
