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
import java.math.RoundingMode;
import java.util.*;

import static vlab.server_java.Consts.*;
import static vlab.server_java.Consts.neuronOutputSignalValueEpsilon;
import static vlab.server_java.generate.GenerateProcessorImpl.countEdges;

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
        BigDecimal pointsToDe = new BigDecimal(0);

        try
        {
            String code = generatingResult.getCode();

            JSONObject jsonCode = new JSONObject(code);
            //вместо минуса на серверах de добавляется html-код знака минус. объективно мать ебал человека, который такую хуйню сделал
            JSONObject jsonInstructions = new JSONObject(instructions.replaceAll("&#0045;","-"));

            JSONArray nodes = jsonCode.getJSONArray("nodes");
            JSONArray edges = jsonCode.getJSONArray("edges");

            double error = jsonInstructions.getDouble("error");
            double clientErrorZero = jsonInstructions.getDouble("errorZero");
            JSONArray edgeWeight = jsonCode.getJSONArray("edgeWeight");
            JSONArray nodesValue = jsonCode.getJSONArray("nodesValue");
            double learningRate = jsonCode.getDouble("learningRate");
            double alpha = jsonCode.getDouble("alpha");

            JSONArray serverAnswerZeroForwardPropagation = jsonObjectToJsonArray(generateRightAnswerForwardPropagation(nodes, edges, nodesValue, edgeWeight, sigmoidFunction));

            JSONArray clientAnswerZeroForwardPropagation = jsonInstructions.getJSONArray("neuronsTableData");

            JSONArray nodesValueFull = getSignalWithNewEdgesJsonArrays(nodes, edges, edgeWeight, nodesValue);
            JSONArray clientAnswerBackpropagation = jsonInstructions.getJSONArray("edgesTableData");
            JSONObject backpropagationAnswer = backpropagation(oneDimensionalJsonArrayToDouble(nodesValueFull), twoDimensionalJsonArrayToDouble(edgeWeight), learningRate, alpha);

            backpropagationAnswer.put("wijZero", edgeWeight);
            backpropagationAnswer.put("deltaWijZero", new double[edgeWeight.length()][edgeWeight.length()]);

            JSONArray clientAnswerFirstForwardPropagation = jsonInstructions.getJSONArray("firstPropagationNeuronsTableData");
            JSONArray serverAnswerFirstForwardPropagation = jsonObjectToJsonArray(generateRightAnswerForwardPropagation(nodes, edges, nodesValue, new JSONArray(backpropagationAnswer.get("newW")), sigmoidFunction));

            //проверка результатов
            JSONObject zeroForwardPropagationCompareResult = compareAnswersForwardPropagation(serverAnswerZeroForwardPropagation, clientAnswerZeroForwardPropagation, zeroForwardPropagationPoints);
            double zeroForwardComparePoints = zeroForwardPropagationCompareResult.getDouble("points");
            String zeroForwardCompareComment = zeroForwardPropagationCompareResult.getString("comment");
            points += zeroForwardComparePoints;
            if(zeroForwardCompareComment.length() > 0)
                comment += "ТАБЛИЦА СИГНАЛОВ НЕЙРОНОВ ДО МОР: ";

            comment += zeroForwardCompareComment;

            JSONObject backpropagationCompareResult = compareBackpropagationAnswers(backpropagationAnswer, clientAnswerBackpropagation, twoDimensionalJsonArrayToInt(edges), Consts.backpropagationTablePoints);
            double backpropagationComparePoints = backpropagationCompareResult.getDouble("points");
            String backpropagationCompareComment = backpropagationCompareResult.getString("comment");

            if(backpropagationCompareComment.length() > 0)
                comment += "ТАБЛИЦА МОР: ";

            comment += backpropagationCompareComment;
            points += backpropagationComparePoints;

            JSONObject firstForwardPropagationCompareResult = compareAnswersForwardPropagation(serverAnswerFirstForwardPropagation, clientAnswerFirstForwardPropagation, firstForwardPropagationPoints);
            double firstForwardComparePoints = firstForwardPropagationCompareResult.getDouble("points");
            String firstForwardCompareComment = firstForwardPropagationCompareResult.getString("comment");

            if(firstForwardCompareComment.length() > 0)
                comment += "ТАБЛИЦА СИГНАЛОВ НЕЙРОНОВ ПОСЛЕ МОР: ";

            points += firstForwardComparePoints;
            comment += firstForwardCompareComment;

            //MSE до МОР
            JSONArray outputNeuronsValueBeforeBackPropagation = getSignalWithNewEdgesJsonArrays(nodes, edges, edgeWeight, nodesValue);
            double serverErrorZero = roundDoubleToNDecimals(countMSE(outputNeuronsValueBeforeBackPropagation), 4);

            if (serverErrorZero >= clientErrorZero - mseEpsilon && serverErrorZero <= clientErrorZero + mseEpsilon)
                points += Consts.errorPoints;
            else
                comment += "Неверно посчитанно E0(w): E0(w) = " + serverErrorZero + ". ";

            //Новое MSE после выполнения МОР
            JSONArray outputNeuronsValueAfterBackPropagation = getSignalWithNewEdgesJsonArrays(nodes, edges, new JSONArray(backpropagationAnswer.get("newW")), nodesValue);
            double newError = roundDoubleToNDecimals(countMSE(outputNeuronsValueAfterBackPropagation), 4);

            if (newError >= error - mseEpsilon && newError <= error + mseEpsilon)
                points += Consts.errorPoints;
            else
                comment += "Неверно посчитанно E1(w) после МОР. E1(w) = " + newError + ". ";

            //на всякий проверка вдруг посчиталось баллов больше и поэтому крашится на самом de лаба
            if (points > 1)
                points = 1.0;

            pointsToDe = new BigDecimal(points);
            pointsToDe = pointsToDe.setScale(2, RoundingMode.HALF_DOWN);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return new CheckingSingleConditionResult(pointsToDe, comment);
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
                comment.append("Неверное значение input(X").append(i).append("): sys = ").append(sortedServerAnswer.getJSONObject(i).getDouble("neuronInputSignalValue")).append("; usr = ").append(sortedClientAnswer.getJSONObject(i).getDouble("neuronInputSignalValue")).append(". ");
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
                comment.append("Неверное значение output(X").append(i).append("): sys = ").append(sortedServerAnswer.getJSONObject(i).getDouble("neuronOutputSignalValue")).append("; usr = ").append(sortedClientAnswer.getJSONObject(i).getDouble("neuronOutputSignalValue")).append(". ");
            }

            //если правильно в графе выделил нейроны, из которых сигнал течёт в текущий нейрон по таблице
            if(compareArrays(sortedClientAnswer.getJSONObject(i).getJSONArray("nodeSection"), sortedServerAnswer.getJSONObject(i).getJSONArray("nodeSection")))
            {
                isNeuronNodeSectionCorrect = true;
            }
            else
            {
                comment.append("Неверно выделены прооборазы нейрона X").append(sortedClientAnswer.getJSONObject(i).getString("nodeId").substring(1)).append(". ");
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

            nodeInputSignal = roundDoubleToNDecimals(nodeInputSignal, 2);
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

            nodeOutputSignal = roundDoubleToNDecimals(nodeOutputSignal, 2);

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

            nodeInputSignal = roundDoubleToNDecimals(nodeInputSignal, 2);

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

            nodeOutputSignal = roundDoubleToNDecimals(nodeOutputSignal, 2);

            currentNodesValue[i] = nodeOutputSignal;
        }

        return currentNodesValue;
    }

    public JSONObject generateRightAnswerForwardPropagation(JSONArray nodes, JSONArray edges, JSONArray nodesValue, JSONArray edgeWeight, String activationFunction)
    {
        JSONArray jsonNodeId = new JSONArray();
        JSONArray jsonNodeSection = new JSONArray();
        JSONObject serverAnswer = new JSONObject();

        double[] newNodesValues = getSignalWithNewEdges(jsonArrayToInt(nodes), twoDimensionalJsonArrayToInt(edges), Consts.twoDimensionalJsonArrayToDouble(edgeWeight), jsonArrayToDouble(nodesValue), activationFunction);
        double[] inputSignals = getInputSignalWithNewEdges(jsonArrayToInt(nodes), twoDimensionalJsonArrayToInt(edges), Consts.twoDimensionalJsonArrayToDouble(edgeWeight), jsonArrayToDouble(nodesValue), activationFunction);

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
        int nodesAmount = Consts.inputNeuronsAmount + Consts.outputNeuronsAmount + Consts.amountOfHiddenLayers * Consts.amountOfNodesInHiddenLayer;
        JSONObject clientAnswerConverted = new JSONObject();
        double[][] newW = new double[nodesAmount][nodesAmount];
        double[] delta = new double[nodesAmount];
        double[][] deltaW = new double[nodesAmount][nodesAmount];
        double[][] deltaWijZero = new double[nodesAmount][nodesAmount];
        double[][] wijZero = new double[nodesAmount][nodesAmount];
        double[][] grad = new double[nodesAmount][nodesAmount];

        for (int i = 0; i < nodesAmount; i++)
        {
            for (int j = 0; j < nodesAmount; j++)
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
            delta[nodeToIndex] = clientAnswer.getJSONObject(i).getDouble("delta");
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

            for(int m = 0; m < keys.length; m++)
            {
                currentJsonObject.put(keys[m], jsonObject.getJSONArray(keys[m]).get(i));
            }

            result.put(i, currentJsonObject);
        }

        return result;
    }

    private static JSONObject compareBackpropagationAnswers(JSONObject serverAnswer, JSONArray clientAnswer, int[][] edges,double pointPercent)
    {
        int nodesAmount = inputNeuronsAmount + outputNeuronsAmount + amountOfHiddenLayers * amountOfNodesInHiddenLayer;
        double points = 0;
        JSONObject result = new JSONObject();
        StringBuilder comment = new StringBuilder();
        int edgesAmount = countEdges(edges);
        double pointDelta = pointPercent / edgesAmount / 6;

        double[] serverDelta = (double[]) serverAnswer.get("delta");
        double[][] serverNewW = (double[][]) serverAnswer.get("newW");
        double[][] serverGrad = (double[][]) serverAnswer.get("grad");
        double[][] serverWijZero = Consts.twoDimensionalJsonArrayToDouble(serverAnswer.getJSONArray("wijZero"));
        double[][] serverDeltaWijZero = (double[][]) serverAnswer.get("deltaWijZero");
        double[][] serverDeltaW = (double[][]) serverAnswer.get("deltaW");

        for(int i = 0; i < clientAnswer.length(); i++)
        {
            int nodeFromIndex = clientAnswer.getJSONObject(i).getJSONArray("edge").getInt(0);
            int nodeToIndex = clientAnswer.getJSONObject(i).getJSONArray("edge").getInt(1);

            double clientDelta = clientAnswer.getJSONObject(i).getDouble("delta");
            double clientNewW = clientAnswer.getJSONObject(i).getDouble("newW");
            double clientDeltaWijZero = clientAnswer.getJSONObject(i).getDouble("deltaWijZero");
            double clientWijZero = clientAnswer.getJSONObject(i).getDouble("wijZero");
            double clientDeltaW = clientAnswer.getJSONObject(i).getDouble("deltaW");
            double clientGrad = clientAnswer.getJSONObject(i).getDouble("grad");

            if(serverDelta[nodeToIndex] == clientDelta)
            {
                points += pointDelta;
            }
            else
            {
                comment.append("Неверно рассчитана дельта нейрона X").append(Integer.toString(nodeToIndex)).append(": ").append("sys = ").append(serverDelta[nodeToIndex]).append("; user = ").append(clientDelta).append(". ");
            }

            if(clientNewW >= serverNewW[nodeFromIndex][nodeToIndex] - WEpsilon && clientNewW <= serverNewW[nodeFromIndex][nodeToIndex] + WEpsilon)
            {
                points += pointDelta;
            }
            else
            {
                comment.append("Неверно рассчитан вес дуги (").append(Integer.toString(nodeFromIndex)).append(", ").append(Integer.toString(nodeToIndex)).append(") на первой итерации: ").append("sys = ").append(serverNewW[nodeFromIndex][nodeToIndex]).append("; user = ").append(clientNewW).append(". ");
            }

            if(clientDeltaWijZero == serverDeltaWijZero[nodeFromIndex][nodeToIndex])
            {
                points += pointDelta;
            }
            else
            {
                comment.append("Неверно рассчитана дельта дуги (").append(Integer.toString(nodeFromIndex)).append(", ").append(Integer.toString(nodeToIndex)).append(") на нулевой итерации: ").append(" sys = ").append(serverDeltaWijZero[nodeFromIndex][nodeToIndex]).append("; user = ").append(clientDeltaWijZero).append(". ");
            }

            if(clientWijZero >= serverWijZero[nodeFromIndex][nodeToIndex] - wijZeroEpsilon && clientWijZero <= serverWijZero[nodeFromIndex][nodeToIndex] + wijZeroEpsilon)
            {
                points += pointDelta;
            }
            else
            {
                comment.append("Неверно указан вес дуги (").append(Integer.toString(nodeFromIndex)).append(", ").append(Integer.toString(nodeToIndex)).append(") на нулевой итерации: ").append(" sys = ").append(serverWijZero[nodeFromIndex][nodeToIndex]).append("; user = ").append(clientWijZero).append(". ");
            }

            if(clientDeltaW >= serverDeltaW[nodeFromIndex][nodeToIndex] - dtWEpsilon && clientDeltaW <= serverDeltaW[nodeFromIndex][nodeToIndex] + dtWEpsilon)
            {
                points += pointDelta;
            }
            else
            {
                comment.append("Неверно рассчитана дельта дуги (").append(Integer.toString(nodeFromIndex)).append(", ").append(Integer.toString(nodeToIndex)).append(") на первой итерации: ").append("sys = ").append(serverDeltaW[nodeFromIndex][nodeToIndex]).append("; user = ").append(clientDeltaW).append(". ");
            }

            if(clientGrad >= serverGrad[nodeFromIndex][nodeToIndex] - gradEpsilon && clientGrad <= serverGrad[nodeFromIndex][nodeToIndex] + gradEpsilon)
            {
                points += pointDelta;
            }
            else
            {
                comment.append("Неверно рассчитано значение вектора антиградиента для дуги (").append(Integer.toString(nodeFromIndex)).append(", ").append(Integer.toString(nodeToIndex)).append("): sys = ").append(serverGrad[nodeFromIndex][nodeToIndex]).append("; user = ").append(clientGrad).append(". ");
            }
        }

        if(clientAnswer.length() < edgesAmount)
            comment.append("В таблице МОР не хватает ").append(edgesAmount - clientAnswer.length()).append(" строк. ");

        result.put("points", points);
        result.put("comment", comment.toString());

        return result;
    }

    private static JSONObject compareBackpropagationAnswersNotWorking(JSONObject serverAnswer, JSONObject clientAnswer, double pointPercent)
    {
        int nodesAmount = Consts.inputNeuronsAmount + Consts.outputNeuronsAmount + Consts.amountOfHiddenLayers * Consts.amountOfNodesInHiddenLayer;
        double points = 0;
        JSONObject result = new JSONObject();
        StringBuilder comment = new StringBuilder();

        double[] serverDelta = (double[]) serverAnswer.get("delta");
        double[] clientDelta = (double[]) clientAnswer.get("delta");

        double[][] serverNewW = (double[][]) serverAnswer.get("newW");
        double[][] clientNewW = (double[][]) clientAnswer.get("newW");

        double[][] serverGrad = (double[][]) serverAnswer.get("grad");
        double[][] clientGrad = (double[][]) clientAnswer.get("grad");

        double[][] serverWijZero = Consts.twoDimensionalJsonArrayToDouble(serverAnswer.getJSONArray("wijZero"));
        double[][] clientWijZero = (double[][]) clientAnswer.get("wijZero");

        double[][] serverDeltaWijZero = (double[][]) serverAnswer.get("deltaWijZero");
        double[][] clientDeltaWijZero = (double[][]) clientAnswer.get("deltaWijZero");

        double[][] serverDeltaW = (double[][]) serverAnswer.get("deltaW");
        double[][] clientDeltaW = (double[][]) clientAnswer.get("deltaW");

        int nonZeroNewWElements = countNonZeroElementsInTwoDimensionalDoubleArray(serverNewW);
        int nonZeroGradElements = countNonZeroElementsInTwoDimensionalDoubleArray(serverGrad);
        int nonZeroWijZeroElements = countNonZeroElementsInTwoDimensionalDoubleArray(serverWijZero);
        int nonZeroDeltaWElements = countNonZeroElementsInTwoDimensionalDoubleArray(serverDeltaW);
        int sumOfValuableElements = nonZeroNewWElements + nonZeroGradElements + nonZeroWijZeroElements + nonZeroDeltaWElements + serverDelta.length;

        double deltaPointsDeltaWijZero = (0.1 * pointPercent) / (serverDeltaWijZero.length * serverDeltaWijZero[0].length);
        double pointDelta = (0.9 * pointPercent) / sumOfValuableElements;

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

                if(isDeltaCorrect)
                {
                    points += pointDelta;
                }
                else
                {
                    comment.append("Неверно рассчитана дельта нейрона X").append(Integer.toString(i)).append(": ").append("sys = ").append(serverDelta[i]).append("; user = ").append(clientDelta[i]).append(". ");
                }

                for (int j = 0; j < nodesAmount; j++)
                {
                    if((clientNewW[i][j] >= serverNewW[i][j] - WEpsilon && clientNewW[i][j] <= serverNewW[i][j] + WEpsilon) && serverNewW[i][j] != 0)
                    {
                        points += pointDelta;
                    }
                    else if(serverNewW[i][j] != 0)
                    {
                        comment.append("Неверно рассчитан вес дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append(") на первой итерации: ").append("sys = ").append(serverNewW[i][j]).append("; user = ").append(clientNewW[i][j]).append(". ");
                    }

                    if(clientDeltaWijZero[i][j] == serverDeltaWijZero[i][j])
                    {
                        points += deltaPointsDeltaWijZero;
                    }
                    else
                    {
                        comment.append("Неверно рассчитана дельта дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append(") на нулевой итерации: ").append(" sys = ").append(serverDeltaWijZero[i][j]).append("; user = ").append(clientDeltaWijZero[i][j]).append(". ");
                    }

                    if((clientWijZero[i][j] >= serverWijZero[i][j] - wijZeroEpsilon && clientWijZero[i][j] <= serverWijZero[i][j] + wijZeroEpsilon) && serverWijZero[i][j] != 0)
                    {
                        points += pointDelta;
                    }
                    else if(serverWijZero[i][j] != 0)
                    {
                        comment.append("Неверно указан вес дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append(") на нулевой итерации: ").append(" sys = ").append(serverWijZero[i][j]).append("; user = ").append(clientWijZero[i][j]).append(". ");
                    }

                    if((clientDeltaW[i][j] >= serverDeltaW[i][j] - dtWEpsilon && clientDeltaW[i][j] <= serverDeltaW[i][j] + dtWEpsilon) && serverDeltaW[i][j] != 0)
                    {
                        points += pointDelta;
                    }
                    else if(serverDeltaW[i][j] != 0)
                    {
                        comment.append("Неверно рассчитана дельта дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append(") на первой итерации: ").append(") sys = ").append(serverDeltaW[i][j]).append("; user = ").append(clientDeltaW[i][j]).append(". ");
                    }

                    if((clientGrad[i][j] >= serverGrad[i][j] - gradEpsilon && clientGrad[i][j] <= serverGrad[i][j] + gradEpsilon) && serverGrad[i][j] != 0)
                    {
                        points += pointDelta;
                    }
                    else if(serverGrad[i][j] != 0)
                    {
                        comment.append("Неверно рассчитано значение вектора антиградиента для дуги (").append(Integer.toString(i)).append(", ").append(Integer.toString(j)).append("): sys = ").append(serverGrad[i][j]).append("; user = ").append(clientGrad[i][j]).append(". ");
                    }
                }
            }

            if(clientAnswer.length() < serverAnswer.length())
                comment.append("В таблице МОР не хватает ").append(serverAnswer.length() - clientAnswer.length()).append(" строк");

            result.put("points", points);
            result.put("comment", comment.toString());
        }
        catch (Exception e)
        {
            result.put("points", 0.0);
            result.put("comment", "Вы не заполнили таблицу МОР. ");
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

    private static double getSigmoidValue(double x)
    {
        return (1 / (1 + Math.exp(-x)));
    }

    public static double[] oneDimensionalJsonArrayToDouble(JSONArray arr)
    {
        double[] result = new double[arr.length()];

        for(int i = 0; i < arr.length(); i++)
        {
            result[i] = arr.getDouble(i);
        }

        return result;
    }

    public static double[][] twoDimensionalJsonArrayToDouble(JSONArray arr)
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

    public static JSONObject backpropagation(double[] neuronOutputSignalValue, double[][] edgesWeight, double learningRate, double alpha)
    {
        JSONObject result = new JSONObject();
        double[] delta = new double[neuronOutputSignalValue.length];
        double[][] grad = new double[edgesWeight.length][edgesWeight.length];
        double[][] deltaW = new double[edgesWeight.length][edgesWeight.length];

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
                    deltaW[i][connectedNeurons.get(j)] = doubleToTwoDecimal(learningRate * grad[i][connectedNeurons.get(j)]);
                    edgesWeight[i][connectedNeurons.get(j)] += doubleToTwoDecimal(deltaW[i][connectedNeurons.get(j)]);
                }
            }
            else
            {
                for(int j = 0; j < connectedNeurons.size(); j++)
                {
                    delta[i] += doubleToTwoDecimal(((1 - neuronOutputSignalValue[i]) * neuronOutputSignalValue[i]) * edgesWeight[i][connectedNeurons.get(j)] * delta[connectedNeurons.get(j)]);
                    grad[i][connectedNeurons.get(j)] = doubleToTwoDecimal(neuronOutputSignalValue[i] * delta[connectedNeurons.get(j)]);
                    deltaW[i][connectedNeurons.get(j)] = doubleToTwoDecimal(learningRate * grad[i][connectedNeurons.get(j)]);
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