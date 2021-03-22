package vlab.server_java.check;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rlcp.check.ConditionForChecking;
import rlcp.generate.GeneratingResult;
import rlcp.server.processor.check.PreCheckProcessor.PreCheckResult;
import rlcp.server.processor.check.PreCheckResultAwareCheckProcessor;
import vlab.server_java.Consts;

import java.math.BigDecimal;
import java.util.*;

import static vlab.server_java.Consts.*;
import static vlab.server_java.Consts.neuronOutputSignalValueEpsilon;

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

        JSONArray serverAnswerBackpropagation = jsonObjectToJsonArray(generateRightAnswer(nodes, edges, nodesValue, edgeWeight));

        JSONArray clientAnswerZeroForwardPropagation = jsonInstructions.getJSONArray("neuronsTableData");
        JSONArray clientAnswerFirstForwardPropagation = jsonInstructions.getJSONArray("firstPropagationNeuronsTableData");
        JSONArray clientAnswerBackpropagation = jsonInstructions.getJSONArray("edgesTableData");

        double[] signalOutputArray = new double[nodesValue.length()];
        double[] serverAnswerNeuronOutputSignalValue = getDoublerrayByKey(serverAnswerBackpropagation, "neuronOutputSignalValue");

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
        JSONObject convertedClientAnswer = convertClientAnswer(clientAnswerBackpropagation);

        backpropagationAnswer.put("wijZero", edgeWeight);
        backpropagationAnswer.put("deltaWijZero", new double[edgeWeight.length()][edgeWeight.length()]);

        JSONObject compareResult = compareAnswers(backpropagationAnswer, convertedClientAnswer, Consts.backpropagationTablePoints);
        double comparePoints = compareResult.getDouble("points");
        String compareComment = compareResult.getString("comment");
        comment += compareComment;
        points += comparePoints;

        //Новое MSE после выполнения МОР
        JSONArray outputNeuronsValueAfterBackPropagation = getSignalWithNewEdgesJsonArrays(nodes, edges, new JSONArray(backpropagationAnswer.get("newW")), nodesValue);
        double newError = countMSE(outputNeuronsValueAfterBackPropagation);

        if (newError >= error - mseEpsilon && newError <= error + mseEpsilon)
            points += Consts.errorPoints;
        else
            comment += "Неверно посчитанно новое MSE. MSE = " + newError;

        points = doubleToTwoDecimal(points);

        return new CheckingSingleConditionResult(BigDecimal.valueOf(points), comment);
    }

    private static JSONArray sortJsonArrays(String jsonArrStr, String KEY_NAME)
    {
        JSONArray jsonArr = new JSONArray(jsonArrStr);
        JSONArray sortedJsonArray = new JSONArray();

        List<JSONObject> jsonValues = new ArrayList<JSONObject>();
        for (int i = 0; i < jsonArr.length(); i++) {
            jsonValues.add(jsonArr.getJSONObject(i));
        }
        Collections.sort( jsonValues, new Comparator<JSONObject>() {
            //You can change "Name" with "ID" if you want to sort by ID
            @Override
            public int compare(JSONObject a, JSONObject b) {
                String valA = new String();
                String valB = new String();

                try {
                    valA = (String) a.get(KEY_NAME);
                    valB = (String) b.get(KEY_NAME);
                }
                catch (JSONException e) {
                    //do something
                }

                return valA.compareTo(valB);
                //if you want to change the sort order, simply use the following:
                //return -valA.compareTo(valB);
            }
        });

        for (int i = 0; i < jsonArr.length(); i++) {
            sortedJsonArray.put(jsonValues.get(i));
        }

        return sortedJsonArray;
    }

    private static boolean compareArrays(JSONArray arr1, JSONArray arr2) {
        Object[] normalArr1 = new Object[arr1.length()];
        Object[] normalArr2 = new Object[arr2.length()];

        for(int i = 0; i < arr1.length(); i++)
        {
            normalArr1[i] = arr1.get(i);
        }

        for(int i = 0; i < arr2.length(); i++)
        {
            normalArr2[i] = arr2.get(i);
        }

        Arrays.sort(normalArr1);
        Arrays.sort(normalArr2);
        return Arrays.equals(normalArr1, normalArr2);
    }

    private static JSONObject compareAnswersForwardPropagation(JSONArray serverAnswer, JSONArray clientAnswer, double pointPercent)
    {
        double pointDelta = pointPercent / serverAnswer.length();
        double points = 0;
        JSONObject result = new JSONObject();
        StringBuilder comment = new StringBuilder();

        JSONArray sortedServerAnswer = sortJsonArrays(serverAnswer.toString(), "nodeId");
        JSONArray sortedClientAnswer = sortJsonArrays(clientAnswer.toString(), "nodeId");

        for(int i = 0; i < sortedClientAnswer.length(); i++)
        {
            boolean isNeuronInputSignalValueCorrect = false;
            boolean isNeuronOutputSignalValueCorrect = false;
            boolean isNeuronNodeSectionCorrect = false;

            //равны входные значения сигнала на конкретный нейрон в рамках окрестности
            if(sortedClientAnswer.getJSONObject(i).getDouble("neuronInputSignalValue") >= sortedServerAnswer.getJSONObject(i).getDouble("neuronInputSignalValue") - neuronInputSignalValueEpsilon
                    &&
                    sortedClientAnswer.getJSONObject(i).getDouble("neuronInputSignalValue") <= sortedServerAnswer.getJSONObject(i).getDouble("neuronInputSignalValue") + neuronInputSignalValueEpsilon
            )
            {
                isNeuronInputSignalValueCorrect = true;
            }
            else
            {
                comment.append("Неверное значение входного сигнала нейрона ").append(sortedClientAnswer.getJSONObject(i).getString("nodeId")).append(". ");
            }

            //равны выходные значения сигнала на конкретный нейрон в рамках окрестности
            if(sortedClientAnswer.getJSONObject(i).getDouble("neuronOutputSignalValue") >=
                    sortedServerAnswer.getJSONObject(i).getDouble("neuronOutputSignalValue") - neuronOutputSignalValueEpsilon
                    &&
                    sortedClientAnswer.getJSONObject(i).getDouble("neuronOutputSignalValue") <= sortedServerAnswer.getJSONObject(i).getDouble("neuronOutputSignalValue") + neuronOutputSignalValueEpsilon
            )
            {
                isNeuronOutputSignalValueCorrect = true;
            }
            else
            {
                comment.append("Неверное значение выходного сигнала нейрона ").append(sortedClientAnswer.getJSONObject(i).getString("nodeId")).append(". ");
            }

            //если правильно в графе выделил нейроны, из которых сигнал течёт в текущий нейрон по таблице
            if(compareArrays(sortedClientAnswer.getJSONObject(i).getJSONArray("nodeSection"), sortedServerAnswer.getJSONObject(i).getJSONArray("nodeSection")))
            {
                isNeuronNodeSectionCorrect = true;
            }
            else
            {
                comment.append("Неверно выделены нейроны из которых течёт сигнал в нейрон ").append(sortedClientAnswer.getJSONObject(i).getString("nodeId")).append(". ");
            }

            if(isNeuronInputSignalValueCorrect)
                points += pointDelta / 3;

            if(isNeuronOutputSignalValueCorrect)
                points += pointDelta / 3;

            if(isNeuronNodeSectionCorrect)
                points += pointDelta / 3;
        }

        int rowsDiff = serverAnswer.length() - clientAnswer.length();
        if(rowsDiff > 0)
        {
            comment.append("В таблице не хватает ").append(String.valueOf(rowsDiff)).append(" строк. ");
        }

        result.put("points", points);
        result.put("comment", comment.toString());

        return result;
    }

    public static double[] getInputSignalWithNewEdges(int[] nodes, int[][] edges, double[][] edgesWeight, double[] nodesValue, String activationFunction)
    {
        double[] currentNodesValue = new double[nodesValue.length];
        double[] currentInputSignalValues = new double[nodesValue.length];

        for(int i = 0; i < inputNeuronsAmount; i++)
        {
            currentNodesValue[i] = nodesValue[i];
            currentInputSignalValues[i] = nodesValue[i];
        }

        for(int i = Consts.inputNeuronsAmount; i < nodes.length; i++)
        {
            double nodeInputSignal = 0;
            double nodeOutputSignal = 0;

            for(int j = 0; j < i; j++)
            {
                if(edges[j][i] == 1)
                {
                    nodeInputSignal += currentNodesValue[j] * edgesWeight[j][i];
                }
            }

            nodeInputSignal = roundDoubleToNDecimals(nodeInputSignal, roundNodesValueSign);
            currentInputSignalValues[i] = nodeInputSignal;

            switch (activationFunction) {
                case sigmoidFunction:
                    nodeOutputSignal = sigmoid(nodeInputSignal);
                    break;
                case linearFunction:
                    nodeOutputSignal = linear(nodeInputSignal);
                    break;
                case tgFunction:
                    nodeOutputSignal = tg(nodeInputSignal);
                    break;
            }

            nodeOutputSignal = roundDoubleToNDecimals(nodeOutputSignal, roundNodesValueSign);

            currentNodesValue[i] = nodeOutputSignal;
        }

        return currentInputSignalValues;
    }

    public static double[] getSignalWithNewEdges(int[] nodes, int[][] edges, double[][] edgesWeight, double[] nodesValue, String activationFunction)
    {
        double[] currentNodesValue = new double[nodesValue.length];
        for(int i = 0; i < inputNeuronsAmount; i++)
        {
            currentNodesValue[i] = nodesValue[i];
        }

        for(int i = Consts.inputNeuronsAmount; i < nodes.length; i++)
        {
            double nodeInputSignal = 0;
            double nodeOutputSignal = 0;

            for(int j = 0; j < i; j++)
            {
                if(edges[j][i] == 1)
                {
                    nodeInputSignal += currentNodesValue[j] * edgesWeight[j][i];
                }
            }

            nodeInputSignal = roundDoubleToNDecimals(nodeInputSignal, roundNodesValueSign);

            switch (activationFunction) {
                case sigmoidFunction:
                    nodeOutputSignal = sigmoid(nodeInputSignal);
                    break;
                case linearFunction:
                    nodeOutputSignal = linear(nodeInputSignal);
                    break;
                case tgFunction:
                    nodeOutputSignal = tg(nodeInputSignal);
                    break;
            }

            nodeOutputSignal = roundDoubleToNDecimals(nodeOutputSignal, roundNodesValueSign);

            currentNodesValue[i] = nodeOutputSignal;
        }

        return currentNodesValue;
    }

    public JSONObject generateRightAnswerForwardPropagation(JSONArray nodes, JSONArray edges, JSONArray nodesValue, JSONArray edgeWeight, String activationFunction)
    {
        JSONArray jsonNodeId = new JSONArray();
        JSONArray jsonNodeSection = new JSONArray();
        JSONObject serverAnswer = new JSONObject();

        double[] newNodesValues = getSignalWithNewEdges(jsonArrayToInt(nodes), twoDimensionalJsonArrayToInt(edges), twoDimensionalJsonArrayToDouble(edgeWeight), jsonArrayToDouble(nodesValue), activationFunction);
        double[] inputSignals = getInputSignalWithNewEdges(jsonArrayToInt(nodes), twoDimensionalJsonArrayToInt(edges), twoDimensionalJsonArrayToDouble(edgeWeight), jsonArrayToDouble(nodesValue), activationFunction);

        //нашли значение всех выходных сигналов нейронов
        for(int i = 0; i < nodes.length(); i++)
        {
            String nodeId = "n" + Integer.toString(i);
            JSONArray nodeSection = new JSONArray();

            for(int j = 0; j < edges.getJSONArray(i).length(); j++)
            {
                if(edges.getJSONArray(j).getInt(i) == 1)
                {
                    nodeSection.put(nodeSection.length(), "n" + Integer.toString(j));
                }
            }

            jsonNodeId.put(i, nodeId);
            jsonNodeSection.put(i, nodeSection);
        }

        serverAnswer.put("neuronInputSignalValue", new JSONArray(inputSignals));
        serverAnswer.put("neuronOutputSignalValue", new JSONArray(newNodesValues));
        serverAnswer.put("nodeId", jsonNodeId);
        serverAnswer.put("nodeSection", jsonNodeSection);

        return serverAnswer;
    }

    private static JSONArray getSignalWithNewEdgesJsonArrays(JSONArray nodes, JSONArray edges, JSONArray edgesWeight, JSONArray nodesValue)
    {
        for(int i = Consts.inputNeuronsAmount; i < nodes.length(); i++)
        {
            double nodeInputSignal = 0;
            double nodeOutputSignal = 0;

            for(int j = 0; j < i; j++)
            {
                if(edges.getJSONArray(j).getDouble(i) == 1)
                {
                    nodeInputSignal += nodesValue.getDouble(j) * edgesWeight.getJSONArray(j).getDouble(i);
                }
            }

            nodeInputSignal = doubleToTwoDecimal(nodeInputSignal);
            nodeOutputSignal = getSigmoidValue(nodeInputSignal);
            nodeOutputSignal = doubleToTwoDecimal(nodeOutputSignal);

            nodesValue.put(i, nodeOutputSignal);
        }

        return nodesValue;
    }

    private static JSONObject convertClientAnswer(JSONArray clientAnswer)
    {
        JSONObject clientAnswerConverted = new JSONObject();
        double[][] newW = new double[clientAnswer.length()][clientAnswer.length()];
        double[] delta = new double[clientAnswer.length()];
        double[][] deltaW = new double[clientAnswer.length()][clientAnswer.length()];
        double[][] deltaWijZero = new double[clientAnswer.length()][clientAnswer.length()];
        double[][] wijZero = new double[clientAnswer.length()][clientAnswer.length()];
        double[][] grad = new double[clientAnswer.length()][clientAnswer.length()];

        for (int i = 0; i < clientAnswer.length(); i++)
        {
            delta[i] = 0;
            for (int j = 0; j < clientAnswer.length(); j++)
            {
                newW[i][j] = 0;
                deltaW[i][j] = 0;
                deltaWijZero[i][j] = 0;
                wijZero[i][j] = 0;
                grad[i][j] = 0;
            }
        }

        for (int i = 0; i < clientAnswer.length(); i++)
        {
            int nodeFromIndex = clientAnswer.getJSONObject(i).getJSONArray("edge").getInt(0);
            int nodeToIndex = clientAnswer.getJSONObject(i).getJSONArray("edge").getInt(1);

            newW[nodeFromIndex][nodeToIndex] = clientAnswer.getJSONObject(i).getDouble("newW");
            delta[i] = clientAnswer.getJSONObject(i).getDouble("delta");
            deltaW[nodeFromIndex][nodeToIndex] = clientAnswer.getJSONObject(i).getDouble("deltaW");
            deltaWijZero[nodeFromIndex][nodeToIndex] = clientAnswer.getJSONObject(i).getDouble("deltaWijZero");
            wijZero[nodeFromIndex][nodeToIndex] = clientAnswer.getJSONObject(i).getDouble("wijZero");
            grad[nodeFromIndex][nodeToIndex] = clientAnswer.getJSONObject(i).getDouble("grad");
        }

        clientAnswerConverted.put("newW", newW);
        clientAnswerConverted.put("delta", delta);
        clientAnswerConverted.put("deltaW", deltaW);
        clientAnswerConverted.put("deltaWijZero", deltaWijZero);
        clientAnswerConverted.put("wijZero", wijZero);
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
        //todo сделать норм оценку
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

        double[][] serverWijZero = twoDimensionalJsonArrayToDouble(serverAnswer.getJSONArray("wijZero"));
        double[][] clientWijZero = (double[][]) clientAnswer.get("wijZero");

        double[][] serverDeltaWijZero = (double[][]) serverAnswer.get("deltaWijZero");
        double[][] clientDeltaWijZero = (double[][]) clientAnswer.get("deltaWijZero");

        double[][] serverDeltaW = (double[][]) serverAnswer.get("deltaW");
        double[][] clientDeltaW = (double[][]) clientAnswer.get("deltaW");

        try
        {
            for (int i = 0; i < nodesAmount; i++)
            {
                boolean isDeltaCorrect = false;

                for (int k = inputNeuronsAmount; k < nodesAmount; k++)
                {
                    if(clientDelta[i] >= serverDelta[k] - deltaEpsilon && clientDelta[i] <= serverDelta[k] + deltaEpsilon)
                    {
                        isDeltaCorrect = true;
                        break;
                    }
                }

                if(!isDeltaCorrect)
                {
                    points -= pointDelta / nodesAmount;
                    comment.append("Неверно рассчитана дельта нейрона ").append(Integer.toString(i)).append(": ").append("sys = ").append(serverDelta[i]).append("; user = ").append(clientDelta[i]).append(". ");
                }

                for (int j = 0; j < nodesAmount; j++)
                {
                    if(!(clientNewW[i][j] >= serverNewW[i][j] - WEpsilon && clientNewW[i][j] <= serverNewW[i][j] + WEpsilon))
                    {
                        points -= pointDelta / nodesAmount;
                        comment.append("Неверно рассчитан вес дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append(") на первой итерации: ").append("sys = ").append(serverNewW[i][j]).append("; user = ").append(clientNewW[i][j]).append(". ");
                    }

                    if(!(clientDeltaWijZero[i][j] >= serverDeltaWijZero[i][j] - deltaWijZeroEpsilon && clientDeltaWijZero[i][j] <= serverDeltaWijZero[i][j] + deltaWijZeroEpsilon))
                    {
                        points -= pointDelta / nodesAmount;
                        comment.append("Неверно рассчитана дельта дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append(") на нулевой итерации: ").append(" sys = ").append(serverDeltaWijZero[i][j]).append("; user = ").append(clientDeltaWijZero[i][j]).append(". ");
                    }

                    if(!(clientWijZero[i][j] >= serverWijZero[i][j] - wijZeroEpsilon && clientWijZero[i][j] <= serverWijZero[i][j] + wijZeroEpsilon))
                    {
                        points -= pointDelta / nodesAmount;
                        comment.append("Неверно рассчитан вес дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append(") на нулевой итерации: ").append(" sys = ").append(serverWijZero[i][j]).append("; user = ").append(clientWijZero[i][j]).append(". ");
                    }

                    if(!(clientDeltaW[i][j] >= serverDeltaW[i][j] - dtWEpsilon && clientDeltaW[i][j] <= serverDeltaW[i][j] + dtWEpsilon))
                    {
                        points -= pointDelta / nodesAmount;
                        comment.append("Неверно рассчитана дельта дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append(") на первой итерации: ").append(") sys = ").append(serverDeltaW[i][j]).append("; user = ").append(clientDeltaW[i][j]).append(". ");
                    }

                    if(!(clientGrad[i][j] >= serverGrad[i][j] - gradEpsilon && clientGrad[i][j] <= serverGrad[i][j] + gradEpsilon))
                    {
                        points -= pointDelta / nodesAmount;
                        comment.append("Неверно рассчитано значение вектора антиградиента для дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append("): sys = ").append(serverGrad[i][j]).append("; user = ").append(clientGrad[i][j]).append(". ");
                    }
                }
            }

            result.put("points", points);
            result.put("comment", comment.toString());
        }
        catch (Exception e)
        {
            result.put("points", 0.0);
            result.put("comment", "Вы не заполнили таблицу. ");
        }

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

        mse = sum / 2;
//        mse = sum / outputNeuronsAmount;

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
        JSONArray jsonNeuronInputSignalValue = new JSONArray();
        JSONArray jsonNeuronOutputSignalValue = new JSONArray();
        JSONArray jsonNodeId = new JSONArray();
        JSONArray jsonNeuronInputSignalFormula = new JSONArray();
        JSONArray jsonNodeSection = new JSONArray();
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
        }

        for(int i = 0; i < Consts.inputNeuronsAmount; i++)
        {
            jsonNeuronInputSignalValue.remove(0);
            jsonNeuronOutputSignalValue.remove(0);
            jsonNodeId.remove(0);
            jsonNeuronInputSignalFormula.remove(0);
            jsonNodeSection.remove(0);
        }

        serverAnswer.put("neuronInputSignalValue", jsonNeuronInputSignalValue);
        serverAnswer.put("neuronOutputSignalValue", jsonNeuronOutputSignalValue);
        serverAnswer.put("nodeId", jsonNodeId);
        serverAnswer.put("neuronInputSignalFormula", jsonNeuronInputSignalFormula);
        serverAnswer.put("nodeSection", jsonNodeSection);

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