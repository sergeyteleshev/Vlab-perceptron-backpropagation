import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rlcp.generate.GeneratingResult;
import rlcp.server.processor.factory.DefaultConstructorProcessorFactory;
import rlcp.server.processor.factory.ProcessorFactory;
import rlcp.server.processor.generate.GenerateProcessor;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-*-server-config.xml")
@ActiveProfiles(profiles = "java")
//@ActiveProfiles(profiles = "js")
public class GenerateLogicTests {

    @Autowired
    private ProcessorFactory generateProcessor;

    @Test
    public void testProcess() {
        GenerateProcessor processor = (GenerateProcessor) generateProcessor.getInstance();
        GeneratingResult result = processor.generate("generate");
        assertThat(result.getText(), is(not(equalTo(""))));
        assertThat(result.getCode(), is(not(equalTo(""))));
        assertThat(result.getInstructions(), is(not(equalTo(""))));
    }


}
