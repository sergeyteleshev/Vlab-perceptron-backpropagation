import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rlcp.calculate.CalculatingResult;
import rlcp.generate.GeneratingResult;
import rlcp.server.processor.calculate.CalculateProcessor;
import rlcp.server.processor.factory.DefaultConstructorProcessorFactory;
import rlcp.server.processor.factory.ProcessorFactory;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-*-server-config.xml")
@ActiveProfiles(profiles = "java")
//@ActiveProfiles(profiles = "js")
public class CalculateLogicTests {

    @Autowired
    private ProcessorFactory calculateProcessor;

    @Test
    public void testProcess() {
        CalculateProcessor processor = (CalculateProcessor) calculateProcessor.getInstance();

        GeneratingResult generatingResult = mock(GeneratingResult.class);
        when(generatingResult.getText()).thenReturn("textPreGenerated");
        when(generatingResult.getCode()).thenReturn("codePreGenerated");
        when(generatingResult.getInstructions()).thenReturn("instructionsPreGenerated");

        CalculatingResult calculatingResult = processor.calculate("condition", "instructions", generatingResult);
        assertThat(calculatingResult.getText(), is(not(equalTo(""))));
        assertThat(calculatingResult.getCode(), is(not(equalTo(""))));
    }
}
