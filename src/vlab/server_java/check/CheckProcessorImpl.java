package vlab.server_java.check;
import org.json.JSONArray;
import org.json.JSONObject;
import rlcp.check.ConditionForChecking;
import rlcp.generate.GeneratingResult;
import rlcp.server.processor.check.PreCheckProcessor.PreCheckResult;
import rlcp.server.processor.check.PreCheckResultAwareCheckProcessor;
import vlab.server_java.Consts;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.util.*;

import static vlab.server_java.Consts.*;

/**
 * Simple CheckProcessor implementation. Supposed to be changed as needed to provide
 * necessary Check method support.
 */

public class CheckProcessorImpl implements PreCheckResultAwareCheckProcessor<String> {
    @Override
    public CheckingSingleConditionResult checkSingleCondition(ConditionForChecking condition, String instructions, GeneratingResult generatingResult) throws Exception {
        //do check logic here
        double points = 0;
        String comment = "";

        String code = generatingResult.getCode();

        JSONObject jsonCode = new JSONObject(code);
        JSONObject jsonInstructions = new JSONObject(instructions);

        JSONArray nodes = jsonCode.getJSONArray("nodes");
        JSONArray edges = jsonCode.getJSONArray("edges");

        double error = jsonInstructions.getDouble("error");
        JSONArray edgeWeight = jsonCode.getJSONArray("edgeWeight");
        JSONArray nodesValue = jsonCode.getJSONArray("nodesValue");

        JSONArray serverAnswer = jsonObjectToJsonArray(generateRightAnswer(nodes, edges, nodesValue, edgeWeight));
        JSONArray clientAnswer = jsonInstructions.getJSONArray("edgesTableData");

        double[] signalOutputArray = new double[nodesValue.length()];
        double[] serverAnswerNeuronOutputSignalValue = getDoublerrayByKey(serverAnswer, "neuronOutputSignalValue");

        for(int i = 0; i < signalOutputArray.length; i++)
        {
            if(i < Consts.inputNeuronsAmount)
            {
                signalOutputArray[i] = nodesValue.getDouble(i);
            }
            else
            {
                signalOutputArray[i] = serverAnswerNeuronOutputSignalValue[i - Consts.inputNeuronsAmount];
            }
        }

        JSONObject backpropagationAnswer = backpropagation(signalOutputArray, twoDimentionalJsonArrayToDouble(edgeWeight));
        JSONObject convertedClientAnswer = convertClientAnswer(clientAnswer);
        JSONArray nodesValueJsonArray = new JSONArray(serverAnswerNeuronOutputSignalValue);
        double newError = countMSE(nodesValueJsonArray);

        JSONObject compareResult = compareAnswers(backpropagationAnswer, convertedClientAnswer, Consts.tablePoints);
        double comparePoints = compareResult.getDouble("points");
        String compareComment = compareResult.getString("comment");
        comment += compareComment;
        points += comparePoints;

        if (error != newError)
            comment += "Неверно посчитанно новое MSE. MSE = " + newError;
        else
            points += Consts.errorPoints;

        return new CheckingSingleConditionResult(BigDecimal.valueOf(points), comment);
    }

    private static JSONObject convertClientAnswer(JSONArray clientAnswer)
    {
        int nodesAmount = Consts.inputNeuronsAmount + Consts.outputNeuronsAmount + Consts.amountOfHiddenLayers * Consts.amountOfNodesInHiddenLayer;
        JSONObject clientAnswerConverted = new JSONObject();
        double[][] newW = new double[nodesAmount][nodesAmount];
        double[] delta = new double[nodesAmount];
        double[][] deltaW = new double[nodesAmount][nodesAmount];
        double[][] grad = new double[nodesAmount][nodesAmount];

        for (int i = 0; i < nodesAmount; i++)
        {
            delta[i] = 0;
            for (int j = 0; j < nodesAmount; j++)
            {
                newW[i][j] = 0;
                deltaW[i][j] = 0;
                grad[i][j] = 0;
            }
        }

        for (int i = 0; i < nodesAmount; i++)
        {
            int nodeFromIndex = clientAnswer.getJSONObject(i).getJSONArray("edge").getInt(0);
            int nodeToIndex = clientAnswer.getJSONObject(i).getJSONArray("edge").getInt(1);

            newW[nodeFromIndex][nodeToIndex] = clientAnswer.getJSONObject(i).getDouble("newW");
            delta[i] = clientAnswer.getJSONObject(i).getDouble("delta");
            deltaW[nodeFromIndex][nodeToIndex] = clientAnswer.getJSONObject(i).getDouble("deltaW");
            grad[nodeFromIndex][nodeToIndex] = clientAnswer.getJSONObject(i).getDouble("grad");
        }

        clientAnswerConverted.put("newW", newW);
        clientAnswerConverted.put("delta", delta);
        clientAnswerConverted.put("deltaW", deltaW);
        clientAnswerConverted.put("grad", grad);

        return clientAnswerConverted;
    }

    public static JSONArray jsonObjectToJsonArray(JSONObject jsonObject)
    {
        JSONArray result = new JSONArray();
        Iterator x = jsonObject.keys();
        int arraySize = 0;
        String[] keys = new String[jsonObject.length()];
        int j = 0;

        if(x.hasNext())
        {
            keys[j++] = (String) x.next();
            arraySize = jsonObject.getJSONArray(keys[0]).length();
        }
        else
            return result;

        while (x.hasNext()) {
            String key = (String) x.next();
            keys[j++] = key;
        }

        for(int i = 0; i < arraySize; i++) {
            JSONObject currentJsonObject = new JSONObject();

            for(int m = 0; m < keys.length - 1; m++)
            {
                currentJsonObject.put(keys[m], jsonObject.getJSONArray(keys[m]).get(i));
            }

            result.put(i, currentJsonObject);
        }

        return result;
    }

    public static double[] addToBeginOfArray(double[] elements, int element)
    {
        double[] newArray = Arrays.copyOf(elements, elements.length + 1);
        newArray[0] = element;
        System.arraycopy(elements, 0, newArray, 1, elements.length);

        return newArray;
    }

    private static JSONObject compareAnswers(JSONObject serverAnswer, JSONObject clientAnswer, double pointPercent)
    {
        int nodesAmount = Consts.inputNeuronsAmount + Consts.outputNeuronsAmount + Consts.amountOfHiddenLayers * Consts.amountOfNodesInHiddenLayer;
        double pointDelta = pointPercent / serverAnswer.length();
        double points = pointPercent;
        JSONObject result = new JSONObject();
        StringBuilder comment = new StringBuilder();

        double[] serverDelta = (double[]) serverAnswer.get("delta");
        double[] clientDelta = (double[]) clientAnswer.get("delta");

        double[][] serverNewW = (double[][]) serverAnswer.get("newW");
        double[][] clientNewW = (double[][]) clientAnswer.get("newW");

        double[][] serverGrad = (double[][]) serverAnswer.get("grad");
        double[][] clientGrad = (double[][]) clientAnswer.get("grad");

        double[][] serverDeltaW = (double[][]) serverAnswer.get("deltaW");
        double[][] clientDeltaW = (double[][]) clientAnswer.get("deltaW");

        for (int i = 0; i < nodesAmount; i++)
        {
            boolean isDeltaCorrect = false;

            for (int k = inputNeuronsAmount; k < nodesAmount; k++)
            {
                if(serverDelta[k] == clientDelta[i])
                    isDeltaCorrect = true;
            }

            if(!isDeltaCorrect)
            {
                points -= pointDelta / nodesAmount;
                comment.append("Неверно рассчитано delta вершины n").append(Integer.toString(i)).append(". ");
            }

            for (int j = 0; j < nodesAmount; j++)
            {
                if(serverNewW[i][j] != clientNewW[i][j])
                {
                    points -= pointDelta / nodesAmount;
                    comment.append("Неверно рассчитано NewW ребра w").append(Integer.toString(i)).append(Integer.toString(j)).append(". ");
                }

                if(serverGrad[i][j] != clientGrad[i][j])
                {
                    points -= pointDelta / nodesAmount;
                    comment.append("Неверно рассчитано grad ребра w").append(Integer.toString(i)).append(Integer.toString(j)).append(". ");
                }

                //todo с клиента почему-то не приходит значение послденего нового ребра
                if(serverDeltaW[i][j] != clientDeltaW[i][j])
                {
                    points -= pointDelta / nodesAmount;
                    comment.append("Неверно рассчитано deltaW ребра w").append(Integer.toString(i)).append(Integer.toString(j)).append(". ");
                }
            }
        }

        result.put("points", points);
        result.put("comment", comment.toString());

        return result;
    }

    public static double countMSE(JSONArray nodesValue)
    {
        double sum = 0;
        double mse;

        for (int i = 1; i < outputNeuronsAmount + 1; i++)
        {
            double currentOutputNeuronValue = nodesValue.getDouble(nodesValue.length() - i);
            sum += Math.pow((1 - currentOutputNeuronValue), 2);
        }

        mse = sum / outputNeuronsAmount;

        return mse;
    }

    public static double[] getDoublerrayByKey(JSONArray arr, String key)
    {
        double[] result = new double[arr.length()];

        for(int i = 0; i < arr.length(); i++)
        {

            result[i] = arr.getJSONObject(i).getDouble(key);
        }

        return result;
    }

    private static double getSigmoidValue(double x)
    {
        return (1 / (1 + Math.exp(-x)));
    }

    private double getHiperbolicTangensValue(double x)
    {
        return (2 / (1 + Math.exp(-2 * x)));
    }

    public static JSONObject generateRightAnswer(JSONArray nodes, JSONArray edges, JSONArray nodesValue, JSONArray edgeWeight)
    {
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
        double error = 0;

        JSONArray jsonNeuronInputSignalValue = new JSONArray();
        JSONArray jsonNeuronOutputSignalValue = new JSONArray();
        JSONArray jsonNodeId = new JSONArray();
        JSONArray jsonNeuronInputSignalFormula = new JSONArray();
        JSONArray jsonNodeSection = new JSONArray();
        JSONArray jsonCountedFormula = new JSONArray();
        JSONObject serverAnswer = new JSONObject();

        for(int i = Consts.inputNeuronsAmount; i < nodes.length(); i++)
        {
            double currentNodeValue = 0;
            String nodeId = "n" + Integer.toString(i);
            StringBuilder nodeFormula = new StringBuilder();
            JSONArray nodeSection = new JSONArray();

            for(int j = 0; j < edges.getJSONArray(i).length(); j++)
            {
                if(edges.getJSONArray(j).getInt(i) == 1)
                {
                    if(nodeFormula.length() == 0)
                    {
                        nodeFormula.append(Double.toString(nodesValue.getDouble(j))).append("*").append(Double.toString(edgeWeight.getJSONArray(j).getDouble(i)));
                    }
                    else
                    {
                        nodeFormula.append("+").append(Double.toString(nodesValue.getDouble(j))).append("*").append(Double.toString(edgeWeight.getJSONArray(j).getDouble(i)));
                    }

                    nodeSection.put(nodeSection.length(), "n" + Integer.toString(j));
                    currentNodeValue += nodesValue.getDouble(j) * edgeWeight.getJSONArray(j).getDouble(i);
                }
            }

            jsonNeuronInputSignalValue.put(i, doubleToTwoDecimal(currentNodeValue));

            currentNodeValue = getSigmoidValue(currentNodeValue);
            currentNodeValue = doubleToTwoDecimal(currentNodeValue);
            nodesValue.put(i, currentNodeValue);

            jsonNeuronOutputSignalValue.put(i, currentNodeValue);
            jsonNodeId.put(i, nodeId);
            jsonNeuronInputSignalFormula.put(i, nodeFormula.toString());
            jsonNodeSection.put(i, nodeSection);

            Object countedFormula = null;

            try {
                countedFormula = engine.eval(nodeFormula.toString());
                countedFormula = doubleToTwoDecimal(new Double(countedFormula.toString()));

            } catch (ScriptException e) {
                e.printStackTrace();
            }

            jsonCountedFormula.put(i, countedFormula);
        }

        for(int i = 0; i < Consts.inputNeuronsAmount; i++)
        {
            jsonNeuronInputSignalValue.remove(0);
            jsonNeuronOutputSignalValue.remove(0);
            jsonNodeId.remove(0);
            jsonNeuronInputSignalFormula.remove(0);
            jsonNodeSection.remove(0);
            jsonCountedFormula.remove(0);
        }

        serverAnswer.put("neuronInputSignalValue", jsonNeuronInputSignalValue);
        serverAnswer.put("neuronOutputSignalValue", jsonNeuronOutputSignalValue);
        serverAnswer.put("nodeId", jsonNodeId);
        serverAnswer.put("neuronInputSignalFormula", jsonNeuronInputSignalFormula);
        serverAnswer.put("nodeSection", jsonNodeSection);
        serverAnswer.put("countedFormula", jsonCountedFormula);

        return serverAnswer;
    }

    public static double[][] twoDimentionalJsonArrayToDouble(JSONArray arr)
    {
        double[][] result = new double[arr.length()][arr.getJSONArray(0).length()];

        for(int i = 0; i < arr.length(); i++)
        {
            for(int j = 0; j < arr.getJSONArray(i).length(); j++)
            {
                result[i][j] = arr.getJSONArray(i).getDouble(j);
            }
        }

        return result;
    }

    private static ArrayList<Integer> findEdgesToNeuron(double[][] edges, int neuronIndex)
    {
        ArrayList<Integer> result = new ArrayList<>();

        for(int i = 0; i < edges.length; i++)
        {
            if(edges[neuronIndex][i] != 0)
            {
                result.add(i);
            }
        }

        return result;
    }

    public static JSONObject backpropagation(double[] neuronOutputSignalValue, double[][] edgesWeight)
    {
        JSONObject result = new JSONObject();
        double[] delta = new double[neuronOutputSignalValue.length];
        double[][] grad = new double[edgesWeight.length][edgesWeight.length];
        double[][] deltaW = new double[edgesWeight.length][edgesWeight.length];
        double E = 0.7;
        double A = 0.3;

        for(int i = neuronOutputSignalValue.length - 1; i >= 0; i--)
        {
            ArrayList<Integer> connectedNeurons = findEdgesToNeuron(edgesWeight, i);

            //esli eto posledniy sloy neyronov, to odna formula
            if(i + Consts.outputNeuronsAmount > neuronOutputSignalValue.length - 1)
            {
                delta[i] = doubleToTwoDecimal((1 - neuronOutputSignalValue[i]) * ((1 - neuronOutputSignalValue[i]) * neuronOutputSignalValue[i]));
            }
            else if (i < Consts.inputNeuronsAmount)
            {
                for(int j = 0; j < connectedNeurons.size(); j++)
                {
                    grad[i][connectedNeurons.get(j)] = doubleToTwoDecimal(neuronOutputSignalValue[i] * delta[connectedNeurons.get(j)]);
                    deltaW[i][connectedNeurons.get(j)] = doubleToTwoDecimal(E * grad[i][connectedNeurons.get(j)]);
                    edgesWeight[i][connectedNeurons.get(j)] += doubleToTwoDecimal(deltaW[i][connectedNeurons.get(j)]);
                }
            }
            else
            {
                for(int j = 0; j < connectedNeurons.size(); j++)
                {
                    delta[i] = doubleToTwoDecimal(((1 - neuronOutputSignalValue[i]) * neuronOutputSignalValue[i]) * edgesWeight[i][connectedNeurons.get(j)] * delta[connectedNeurons.get(j)]);
                    grad[i][connectedNeurons.get(j)] = doubleToTwoDecimal(neuronOutputSignalValue[i] * delta[connectedNeurons.get(j)]);
                    deltaW[i][connectedNeurons.get(j)] = doubleToTwoDecimal(E * grad[i][connectedNeurons.get(j)]);
                    edgesWeight[i][connectedNeurons.get(j)] += doubleToTwoDecimal(deltaW[i][connectedNeurons.get(j)]);
                }
            }
        }

        result.put("newW", edgesWeight);
        result.put("delta", delta);
        result.put("deltaW", deltaW);
        result.put("grad", grad);

        return result;
    }

    @Override
    public void setPreCheckResult(PreCheckResult<String> preCheckResult) {}
}