package vlab.server_java.check;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import org.json.JSONArray;
import org.json.JSONException;
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

import static vlab.server_java.Consts.doubleToTwoDecimal;
import static vlab.server_java.Consts.outputNeuronsAmount;

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
        JSONArray clientAnswer = jsonInstructions.getJSONArray("neuronsTableData");

        double checkError = countMSE(serverAnswer.getJSONObject(serverAnswer.length() - 1).getDouble("neuronOutputSignalValue"));
        checkError = (double) Math.round(checkError * 100) / 100;

        JSONObject compareResult = compareAnswers(serverAnswer, clientAnswer, Consts.tablePoints);

        double comparePoints = compareResult.getDouble("points");

        String compareComment = compareResult.getString("comment");
        comment += compareComment;

        points += comparePoints;

        if(checkError == error)
            points += Consts.errorPoints;
        else
            comment += "Wrong MSE. ";

        if(points == 1.0)
            comment += "Perfect!";

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

        JSONObject test = backpropagation(signalOutputArray, twoDimentionalJsonArrayToDouble(edgeWeight));
        double newError = 0;

        return new CheckingSingleConditionResult(BigDecimal.valueOf(points), test.toString() + " " + Double.toString(newError));
    }

    private static JSONArray jsonObjectToJsonArray(JSONObject jsonObject)
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

    private static JSONObject compareAnswers(JSONArray serverAnswer, JSONArray clientAnswer, double pointPercent)
    {
        double pointDelta = pointPercent / serverAnswer.length();
        double points = 0;
        JSONObject result = new JSONObject();
        StringBuilder comment = new StringBuilder();

        JSONArray sortedServerAnswer = sortJsonArrays(serverAnswer.toString(), "nodeId");
        JSONArray sortedClientAnswer = sortJsonArrays(clientAnswer.toString(), "nodeId");
        ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");

        for(int i = 0; i < sortedClientAnswer.length(); i++)
        {
            boolean isNeuronInputSignalValueCorrect = false;
            boolean isNeuronOutputSignalValueCorrect = false;
            boolean isNeuronInputSignalFormulaCorrect = false;
            boolean isNeuronNodeSectionCorrect = false;

            if(sortedClientAnswer.getJSONObject(i).getDouble("neuronInputSignalValue") ==
                    sortedServerAnswer.getJSONObject(i).getDouble("neuronInputSignalValue"))
            {
                isNeuronInputSignalValueCorrect = true;
            }
            else
            {
                comment.append("Incorrect value of neuron input signal ").append(sortedClientAnswer.getJSONObject(i).getString("nodeId")).append(". ");
            }

            if(sortedClientAnswer.getJSONObject(i).getDouble("neuronOutputSignalValue") ==
                    sortedServerAnswer.getJSONObject(i).getDouble("neuronOutputSignalValue"))
            {
                isNeuronOutputSignalValueCorrect = true;
            }
            else
            {
                comment.append("Incorrect value of neuron output signal ").append(sortedClientAnswer.getJSONObject(i).getString("nodeId")).append(". ");
            }

            try {
                double clientCountedFormula = new Double(engine.eval(sortedClientAnswer.getJSONObject(i).getString("neuronInputSignalFormula")).toString());
                clientCountedFormula = (double) Math.round(clientCountedFormula * 100) / 100;
                if(clientCountedFormula ==
                        sortedServerAnswer.getJSONObject(i).getDouble("countedFormula"))
                {
                    isNeuronInputSignalFormulaCorrect = true;
                }
                else
                {
                    comment.append(" Incorrect formula of neuron output signal ").append(sortedClientAnswer.getJSONObject(i).getString("nodeId")).append(". ");
                }
            } catch (ScriptException e) {
                e.printStackTrace();
            }

            if(compareArrays(sortedClientAnswer.getJSONObject(i).getJSONArray("nodeSection"), sortedServerAnswer.getJSONObject(i).getJSONArray("nodeSection")))
            {
                isNeuronNodeSectionCorrect = true;
            }
            else
            {
                comment.append("Incorrect selection of signal source neurons").append(sortedClientAnswer.getJSONObject(i).getString("nodeId")).append(". ");
            }

            if(isNeuronInputSignalFormulaCorrect)
                points += pointDelta / 4;

            if(isNeuronInputSignalValueCorrect)
                points += pointDelta / 4;

            if(isNeuronOutputSignalValueCorrect)
                points += pointDelta / 4;

            if(isNeuronNodeSectionCorrect)
                points += pointDelta / 4;
        }

        int rowsDiff = serverAnswer.length() - clientAnswer.length();
        if(rowsDiff > 0)
        {
            comment.append("Missing ").append(String.valueOf(rowsDiff)).append(" rows in table. ");
        }

        result.put("points", points);
        result.put("comment", comment.toString());

        return result;
    }

    private static double countMSE(double outputNeuronValue)
    {
        return Math.pow(1 - outputNeuronValue, 2) / 1;
    }

    private static double[] getDoublerrayByKey(JSONArray arr, String key)
    {
        double[] result = new double[arr.length()];

        for(int i = 0; i < arr.length(); i++)
        {

            result[i] = arr.getJSONObject(i).getDouble(key);
        }

        return result;
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

    private double getSigmoidValue(double x)
    {
        return (1 / (1 + Math.exp(-x)));
    }

    private double getHiperbolicTangensValue(double x)
    {
        return (2 / (1 + Math.exp(-2 * x)));
    }

    private static JSONArray sortJsonArrayWithoutKey(JSONArray jsonArr)
    {
        String[] stringArr = new String[jsonArr.length()];

        for(int i = 0; i < jsonArr.length(); i++)
        {
            stringArr[i] = jsonArr.getString(i);
        }

        Arrays.sort(stringArr);
        Gson gson=new GsonBuilder().create();
        String jsonArray = gson.toJson(stringArr);

        return new JSONArray(jsonArray);
    }

    private JSONObject generateRightAnswer(JSONArray nodes, JSONArray edges, JSONArray nodesValue, JSONArray edgeWeight)
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

    private double[][] twoDimentionalJsonArrayToDouble(JSONArray arr)
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

    private ArrayList<Integer> findEdgesToNeuron(double[][] edges, int neuronIndex)
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

    private JSONObject backpropagation(double[] neuronOutputSignalValue, double[][] edgesWeight)
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
                delta[i] = (1 - neuronOutputSignalValue[i]) * ((1 - neuronOutputSignalValue[i]) * neuronOutputSignalValue[i]);
            }
            else if (i < Consts.inputNeuronsAmount)
            {
                for(int j = 0; j < connectedNeurons.size(); j++)
                {
                    grad[i][connectedNeurons.get(j)] = neuronOutputSignalValue[i] * delta[connectedNeurons.get(j)];
                    deltaW[i][connectedNeurons.get(j)] = E * grad[i][connectedNeurons.get(j)];
                    edgesWeight[i][connectedNeurons.get(j)] += deltaW[i][connectedNeurons.get(j)];
                }
            }
            else
            {
                for(int j = 0; j < connectedNeurons.size(); j++)
                {
                    delta[i] = ((1 - neuronOutputSignalValue[i]) * neuronOutputSignalValue[i]) * edgesWeight[i][connectedNeurons.get(j)] * delta[connectedNeurons.get(j)];
                    grad[i][connectedNeurons.get(j)] = neuronOutputSignalValue[i] * delta[connectedNeurons.get(j)];
                    deltaW[i][connectedNeurons.get(j)] = E * grad[i][connectedNeurons.get(j)];
                    edgesWeight[i][connectedNeurons.get(j)] += deltaW[i][connectedNeurons.get(j)];
                }
            }
        }

        result.put("neuronOutputSignalValue", neuronOutputSignalValue);
        result.put("edgesWeight", edgesWeight);

        return result;
    }

    @Override
    public void setPreCheckResult(PreCheckResult<String> preCheckResult) {}
}
