package vlab.server_java.calculate;

import rlcp.calculate.CalculatingResult;
import rlcp.generate.GeneratingResult;
import rlcp.server.processor.calculate.CalculateProcessor;

/**
 * Simple CalculateProcessor implementation. Supposed to be changed as needed to provide necessary Calculate method support.
 */
public class CalculateProcessorImpl implements CalculateProcessor {
    @Override
    public CalculatingResult calculate(String condition, String instructions, GeneratingResult generatingResult) {
        //do calculate logic here
        String text = "text";
        String code = "code";

        return new CalculatingResult(text, code);
    }
}
