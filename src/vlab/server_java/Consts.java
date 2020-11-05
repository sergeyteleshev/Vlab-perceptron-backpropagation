package vlab.server_java;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Consts
{
    public static final int minInputNeuronValue = 0;
    public static final int maxInputNeuronValue = 1;
    public static final int inputNeuronsAmount = 2;
    public static final int outputNeuronsAmount = 1;
    public static final int amountOfHiddenLayers = 1;
    public static final int amountOfNodesInHiddenLayer = 2;
    public static final double errorPoints = 0.1;
    public static final double tablePoints = 0.9;

    public static double doubleToTwoDecimal(double number)
    {
        return Math.round(number * 100.0) / 100.0;
    }
}
