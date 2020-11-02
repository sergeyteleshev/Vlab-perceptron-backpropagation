package vlab.server_java.generate;

import org.json.JSONArray;
import org.json.JSONObject;
import rlcp.generate.GeneratingResult;
import rlcp.server.processor.generate.GenerateProcessor;
import vlab.server_java.Consts;


import static vlab.server_java.Consts.doubleToTwoDecimal;
import static vlab.server_java.check.CheckProcessorImpl.*;

/**
 * Simple GenerateProcessor implementation. Supposed to be changed as needed to
 * provide necessary Generate method support.
 */
public class GenerateProcessorImpl implements GenerateProcessor {
    @Override
    public GeneratingResult generate(String condition) {
        //do Generate logic here
        String text;
        String code;
        String instructions = "instructions";

        JSONObject graph = new JSONObject();

//        int minInputNeuronValue = Consts.minInputNeuronValue;
//        int maxInputNeuronValue = Consts.maxInputNeuronValue;
//        int inputNeuronsAmount = Consts.inputNeuronsAmount;
//        int outputNeuronsAmount = Consts.outputNeuronsAmount;
//
//        int amountOfHiddenLayers = Consts.amountOfHiddenLayers;
//        int amountOfNodesInHiddenLayer = Consts.amountOfNodesInHiddenLayer;
//        int[] hiddenLayerNodesAmount = new int[amountOfHiddenLayers];
//        int currentHiddenLayer = 2;
//
//        int nodesAmount = inputNeuronsAmount + outputNeuronsAmount + amountOfNodesInHiddenLayer * amountOfHiddenLayers; //всего вершин в графе
//
//        int[][] edges = new int[nodesAmount][nodesAmount];
//        int[] nodes = new int[nodesAmount];
//        Object[] nodesValue = new Object[nodesAmount];
//        float[][] edgeWeight = new float[nodesAmount][nodesAmount];
//        int[] nodesLevel = new int[nodesAmount];
//        double initialGraphMSE;
//        int edgesAmount;
//
//        //начальные значения для рецепторов
//        for(int i = 0; i < nodesAmount; i++)
//        {
//            nodesLevel[i] = 1;
//            int nodesLevelTemp = (int) ((int) minInputNeuronValue + (float)(Math.random() * ((maxInputNeuronValue - minInputNeuronValue) + 1)) * 100);
//            nodesValue[i] = (float) nodesLevelTemp / 100;
//        }
//
//        for(int i = 1; i <= outputNeuronsAmount; i++)
//        {
//            nodesLevel[nodesLevel.length - i] = 1 + amountOfHiddenLayers + 1;
//        }
//
//        //уровни словёв
//        int countTemp = 0;
//        int amountOfNodesBeforeOutputNeurons = inputNeuronsAmount + amountOfNodesInHiddenLayer * amountOfHiddenLayers;
//        for(int i = inputNeuronsAmount; i < amountOfNodesBeforeOutputNeurons; i++)
//        {
//            nodesLevel[i] = currentHiddenLayer;
//            countTemp++;
//            if(countTemp % amountOfNodesInHiddenLayer == 0 && countTemp != 0)
//            {
//                currentHiddenLayer++;
//            }
//        }
//
//        for(int i = 0; i < nodesAmount; i++)
//        {
//            nodes[i] = i;
//        }
//
//        for(int i = 0; i < nodesAmount; i++)
//        {
//            int currentNodeLevel = nodesLevel[i];
//
//            for(int j = 0; j < nodesLevel.length; j++)
//            {
//                if(nodesLevel[j] == currentNodeLevel + 1)
//                {
//                    edges[i][j] = 1;
//                    // от -1 до 1 с двумя знаками после запятой
//                    edgeWeight[i][j] = (int)(((float)(Math.random() * ((1 + 1) + 1)) - 1) * 100);
//                    edgeWeight[i][j] = (float) (edgeWeight[i][j]) / 100;
//                }
//                else
//                {
//                    edges[i][j] = 0;
//                    edgeWeight[i][j] = 0;
//                }
//            }
//        }

        int inputNeuronsAmount = 2;
        int outputNeuronsAmount = 1;

        int amountOfHiddenLayers = 1;
        int amountOfNodesInHiddenLayer = 2;
        int[] hiddenLayerNodesAmount = new int[amountOfHiddenLayers];

        int nodesAmount = inputNeuronsAmount + outputNeuronsAmount + amountOfNodesInHiddenLayer * amountOfHiddenLayers; //всего вершин в графе

        int[][] edges = {
            {0,0,1,1,0},
            {0,0,1,1,0},
            {0,0,0,0,1},
            {0,0,0,0,1},
            {0,0,0,0,0},
        };
        int[] nodes = {0,1,2,3,4};
        Object[] nodesValue = {1,0,0.61,0.69,0.33};
        double[][] edgeWeight = {
            {0,0,0.45,0.78,0},
            {0,0,-0.12,0.13,0},
            {0,0,0,0,1.5},
            {0,0,0,0,-2.3},
            {0,0,0,0,0},
        };
        int[] nodesLevel = {1,1,2,2,3};
        double initialGraphMSE;
        int edgesAmount;

        edgesAmount = countEdges(edges);

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
        graph.put("edgesAmount", edgesAmount);

        code = graph.toString();

        //костылём рассчитываем как в методе Check наше MSE
        JSONObject jsonCode = new JSONObject(code);
        JSONArray jsonNodesValue = jsonCode.getJSONArray("nodesValue");
        initialGraphMSE = doubleToTwoDecimal(countMSE(jsonNodesValue));

        text = "Найдите веса рёбер графа при помощи метода обратного распространения и посчитайте новый MSE. Текущее MSE = " + Double.toString(initialGraphMSE);

        return new GeneratingResult(text, code, instructions);
    }

    private int countEdges(int[][] edges)
    {
        int edgesAmount = 0;

        for(int i = 0; i < edges.length; i++)
            for(int j = 0; j < edges[i].length; j++)
                if(edges[i][j] == 1)
                    edgesAmount++;

        return edgesAmount;
    }
}