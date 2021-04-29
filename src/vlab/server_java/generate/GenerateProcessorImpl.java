package vlab.server_java.generate;

import org.json.JSONArray;
import org.json.JSONObject;
import rlcp.generate.GeneratingResult;
import rlcp.server.processor.generate.GenerateProcessor;
import vlab.server_java.Consts;


import static vlab.server_java.Consts.*;
import static vlab.server_java.Consts.roundNodesValueSign;
import static vlab.server_java.check.CheckProcessorImpl.*;

/**
 * Simple GenerateProcessor implementation. Supposed to be changed as needed to
 * provide necessary Generate method support.
 */
public class GenerateProcessorImpl implements GenerateProcessor {
    @Override
    public GeneratingResult generate(String condition) {
        //do Generate logic here
        StringBuilder text = new StringBuilder();
        String code = "";
        String instructions = "instructions";

        double learningRate = 0.9;
        double alpha = 0.1;

        try
        {
            learningRate = Double.parseDouble(condition.split(",")[0]);
            alpha = Double.parseDouble(condition.split(",")[1]);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        try
        {
            JSONObject graph = new JSONObject();

            int inputNeuronsAmount = Consts.inputNeuronsAmount;
            int outputNeuronsAmount = Consts.outputNeuronsAmount;

            int amountOfHiddenLayers = Consts.amountOfHiddenLayers;
            int amountOfNodesInHiddenLayer = Consts.amountOfNodesInHiddenLayer;
            int[] hiddenLayerNodesAmount = new int[amountOfHiddenLayers];
            int nodesAmount = inputNeuronsAmount + outputNeuronsAmount + amountOfNodesInHiddenLayer * amountOfHiddenLayers; //всего вершин в графе

            JSONObject randomGraph = generateVariant(sigmoidFunction);

            double[][] edgeWeight = (double[][]) randomGraph.get("edgeWeight");
            int[][] edges = (int[][]) randomGraph.get("edges");
            int[] nodes = (int[]) randomGraph.get("nodes");
            double[] nodesValue = (double[]) randomGraph.get("nodesValue");
            int[] nodesLevel = (int[]) randomGraph.get("nodesLevel");

            // для теста
//            int inputNeuronsAmount = 2;
//            int outputNeuronsAmount = 1;
//
//            int amountOfHiddenLayers = 1;
//            int amountOfNodesInHiddenLayer = 2;
//            int[] hiddenLayerNodesAmount = new int[amountOfHiddenLayers];
//
//            int nodesAmount = inputNeuronsAmount + outputNeuronsAmount + amountOfNodesInHiddenLayer * amountOfHiddenLayers; //всего вершин в графе
//            int[][] edges = {
//                    {0,0,1,1,0},
//                    {0,0,1,1,0},
//                    {0,0,0,0,1},
//                    {0,0,0,0,1},
//                    {0,0,0,0,0},
//            };
//            int[] nodes = {0,1,2,3,4};
//            double[] nodesValue = {1,0,0.61,0.69,0.33};
//            double[][] edgeWeight = {
//                    {0,0,0.45,0.78,0},
//                    {0,0,-0.12,0.13,0},
//                    {0,0,0,0,1.5},
//                    {0,0,0,0,-2.3},
//                    {0,0,0,0,0},
//            };
//            int[] nodesLevel = {1,1,2,2,3};

            int edgesAmount = countEdges(edges);

            graph.put("edgeWeight", edgeWeight);
            graph.put("nodes", nodes);
            graph.put("nodesLevel", nodesLevel);
            graph.put("nodesValue", nodesValue);
            graph.put("edges", edges);
            graph.put("hiddenNodesLeft", hiddenLayerNodesAmount);
            graph.put("inputNeuronsAmount", inputNeuronsAmount);
            graph.put("outputNeuronsAmount", outputNeuronsAmount);
            graph.put("amountOfHiddenLayers", amountOfHiddenLayers);
            graph.put("amountOfNodesInHiddenLayer", amountOfNodesInHiddenLayer);
            graph.put("nodesAmount", nodesAmount);
            graph.put("edgesAmount", edgesAmount);
            graph.put("learningRate", learningRate);
            graph.put("alpha", alpha);

            code = graph.toString();

            //костылём рассчитываем как в методе Check наше MSE
            JSONObject jsonCode = new JSONObject(code);
            JSONArray jsonNodesValue = jsonCode.getJSONArray("nodesValue");

            for (int i = 0; i < inputNeuronsAmount; i++)
            {
                text.append(("input(X")).append(i).append(") = ").append(nodesValue[i]).append(". ");
            }

            for (int i = nodesAmount - outputNeuronsAmount; i < nodesAmount; i++)
            {
                text.append(("output*(X")).append(i).append(") = ").append(1).append(". ");
            }

            text.append("&#951; = ").append(learningRate).append(", &#9082; = 0.3, k-max = 1");

            //раскомментить, чтобы увидеь ответ в описании лабы
//            double initialGraphMSE = doubleToTwoDecimal(countMSE(new JSONArray(getSignalWithNewEdges(nodes,edges,edgeWeight,nodesValue, sigmoidFunction))));
//            JSONObject backpropagationAnswer = backpropagation(getSignalWithNewEdges(nodes,edges,edgeWeight,nodesValue, sigmoidFunction), edgeWeight, learningRate, alpha);
//            text.append("Найдите веса рёбер графа при помощи метода обратного распространения и посчитайте новый MSE. Текущее MSE = ").append(Double.toString(initialGraphMSE)).append(" ").append(backpropagationAnswerToReadble(backpropagationAnswer));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return new GeneratingResult(text.toString(), code, instructions);
    }

    private String backpropagationAnswerToReadble(JSONObject backpropagationAnswer)
    {
        StringBuilder result = new StringBuilder();
        double[] delta = (double[]) backpropagationAnswer.get("delta");
        double[][] grad = (double[][]) backpropagationAnswer.get("grad");
        double[][] deltaW = (double[][]) backpropagationAnswer.get("deltaW");
        double[][] newW = (double[][]) backpropagationAnswer.get("newW");

        for (int i = 0; i < delta.length; i++)
        {
            for (int j = 0; j < delta.length; j++)
            {
                result.append(" w").append(i).append(j).append(":");
                result.append(" delta = ").append(delta[j]);
                result.append(" grad = ").append(grad[i][j]);
                result.append(" deltaW = ").append(deltaW[i][j]);
                result.append(" newW = ").append(newW[i][j]);
            }
        }

        return result.toString();
    }

    public static int countEdges(int[][] edges)
    {
        int edgesAmount = 0;

        for(int i = 0; i < edges.length; i++)
            for(int j = 0; j < edges[i].length; j++)
                if(edges[i][j] == 1)
                    edgesAmount++;

        return edgesAmount;
    }

    public JSONObject generateVariant(String activationFunction)
    {
        JSONObject randomGraph = generateRandomGraph();
        double[][] edgeWeight = (double[][]) randomGraph.get("edgeWeight");
        int[][] edges = (int[][]) randomGraph.get("edges");
        int[] nodes = (int[]) randomGraph.get("nodes");
        double[] nodesValue = (double[]) randomGraph.get("nodesValue");
        double[] currentNodesValue = getSignalWithNewEdges(nodes, edges, edgeWeight, nodesValue, activationFunction);

        double currentMSE = countMSE(new JSONArray(currentNodesValue));

        while(currentMSE <= classBorderline)
        {
            randomGraph = generateRandomGraph();
            edgeWeight = (double[][]) randomGraph.get("edgeWeight");
            edges = (int[][]) randomGraph.get("edges");
            nodes = (int[]) randomGraph.get("nodes");
            nodesValue = (double[]) randomGraph.get("nodesValue");
            currentNodesValue = getSignalWithNewEdges(nodes, edges, edgeWeight, nodesValue, activationFunction);
            currentMSE = countMSE(new JSONArray(currentNodesValue));
        }

        return randomGraph;
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

    public JSONObject generateRandomGraph()
    {
        int nodesAmount = inputNeuronsAmount + outputNeuronsAmount + amountOfNodesInHiddenLayer * amountOfHiddenLayers; //всего вершин в графе
        double[][] edgeWeight = new double[nodesAmount][nodesAmount];
        int currentHiddenLayer = 2;
        int[] nodesLevel = new int[nodesAmount];
        JSONObject result = new JSONObject();
        int[][] edges = new int[nodesAmount][nodesAmount];
        int[] nodes = new int[nodesAmount];
        double[] nodesValue = new double[nodesAmount];

        for(int i = 0; i < nodesValue.length; i++)
        {
            if(i < inputNeuronsAmount)
            {
                nodesValue[i] = roundDoubleToNDecimals(generateRandomDoubleRange(minInputNeuronValue, maxInputNeuronValue), roundNodesValueSign);
            }
        }

        for(int i = 0; i < nodesAmount; i++)
        {
            nodes[i] = i;
        }

        //начальные значения для рецепторов
        for(int i = 0; i < inputNeuronsAmount; i++)
        {
            nodesLevel[i] = 1;
        }

        for(int i = 1; i <= outputNeuronsAmount; i++)
        {
            nodesLevel[nodesLevel.length - i] = 1 + amountOfHiddenLayers + 1;
        }

        //уровни словёв
        int countTemp = 0;
        int amountOfNodesBeforeOutputNeurons = inputNeuronsAmount + amountOfNodesInHiddenLayer * amountOfHiddenLayers;
        for(int i = inputNeuronsAmount; i < amountOfNodesBeforeOutputNeurons; i++)
        {
            nodesLevel[i] = currentHiddenLayer;
            countTemp++;
            if(countTemp % amountOfNodesInHiddenLayer == 0 && countTemp != 0)
            {
                currentHiddenLayer++;
            }
        }

        for(int i = 0; i < nodesAmount; i++)
        {
            int currentNodeLevel = nodesLevel[i];

            for(int j = 0; j < nodesLevel.length; j++)
            {
                if(nodesLevel[j] == currentNodeLevel + 1)
                {
                    edges[i][j] = 1;
                    // от -1 до 1 с двумя знаками после запятой
                    edgeWeight[i][j] = roundDoubleToNDecimals(generateRandomDoubleRange(minEdgeValue, maxEdgeValue), roundEdgeWeightSign);
                }
                else
                {
                    edges[i][j] = 0;
                    edgeWeight[i][j] = 0;
                }
            }
        }

        result.put("nodesLevel", nodesLevel);
        result.put("edgeWeight", edgeWeight);
        result.put("edges", edges);
        result.put("nodes", nodes);
        result.put("nodesValue", nodesValue);

        return result;
    }
}