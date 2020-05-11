import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import rlcp.check.ConditionForChecking;
import rlcp.generate.GeneratingResult;
import rlcp.server.processor.check.CheckProcessor;
import rlcp.server.processor.check.PreCheckResultAwareCheckProcessor;
import rlcp.server.processor.factory.ProcessorFactory;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-*-server-config.xml")
@ActiveProfiles(profiles = "java")
//@ActiveProfiles(profiles = "js")
public class CheckLogicTests {
    private static final double DELTA = 1e-15;
    @Autowired
    private ProcessorFactory checkProcessor;

    @Test
    public void testCheckSingleCondition() throws Exception {
        PreCheckResultAwareCheckProcessor processor = (PreCheckResultAwareCheckProcessor) checkProcessor.getInstance();

        GeneratingResult generatingResult = mock(GeneratingResult.class);
        when(generatingResult.getText()).thenReturn("textPreGenerated");
        when(generatingResult.getCode()).thenReturn("codePreGenerated");
        when(generatingResult.getInstructions()).thenReturn("instructionsPreGenerated");

        ConditionForChecking conditionForChecking = mock(ConditionForChecking.class);
        when(conditionForChecking.getId()).thenReturn(1);
        when(conditionForChecking.getTime()).thenReturn(Long.parseLong("50"));
        when(conditionForChecking.getOutput()).thenReturn("getOutput");
        when(conditionForChecking.getInput()).thenReturn("getInput");

        CheckProcessor.CheckingSingleConditionResult result = processor.checkSingleCondition(conditionForChecking, "instructions", generatingResult);
        assertThat(result.getComment(), is(not(equalTo(""))));
        assertThat(result.getResult(), is(not(equalTo(""))));
    }
}
