package vlab.server_java.generate;

import org.json.JSONObject;
import rlcp.generate.GeneratingResult;
import rlcp.server.processor.generate.GenerateProcessor;
import vlab.server_java.Consts;
import java.util.Random;

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
        final Random random = new Random();
        JSONObject graph = new JSONObject();

        int minInputNeuronValue = Consts.minInputNeuronValue;
        int maxInputNeuronValue = Consts.maxInputNeuronValue;
        int inputNeuronsAmount = Consts.inputNeuronsAmount;
        int outputNeuronsAmount = Consts.outputNeuronsAmount;

        int amountOfHiddenLayers = Consts.amountOfHiddenLayers;
        int amountOfNodesInHiddenLayer = Consts.amountOfNodesInHiddenLayer;
        int[] hiddenLayerNodesAmount = new int[amountOfHiddenLayers];
        int nodesPerHiddenLayer = (int) Math.round(amountOfNodesInHiddenLayer / amountOfHiddenLayers);
        int currentHiddenLayer = 2;

        int nodesAmount = inputNeuronsAmount + outputNeuronsAmount + amountOfNodesInHiddenLayer ; //всего вершин в графе

        int[][] edges = new int[nodesAmount][nodesAmount];
        int[] nodes = new int[nodesAmount];
        Object[] nodesValue = new Object[nodesAmount];
        float[][] edgeWeight = new float[nodesAmount][nodesAmount];
        int[] nodesLevel = new int[nodesAmount];

        //начальные значения для рецепторов
        for(int i = 0; i < inputNeuronsAmount; i++)
        {
            nodesLevel[i] = 1;
            int nodesLevelTemp = (int) ((int) minInputNeuronValue + (float)(Math.random() * ((maxInputNeuronValue - minInputNeuronValue) + 1)) * 100);
            nodesValue[i] = (float) nodesLevelTemp / 100;
        }

        for(int i = 1; i <= outputNeuronsAmount; i++)
        {
            nodesLevel[nodesLevel.length - i] = 1 + amountOfHiddenLayers + 1;
        }

        //уровни словёв
        int countTemp = 0;
        for(int i = inputNeuronsAmount; i < inputNeuronsAmount + amountOfNodesInHiddenLayer; i++)
        {
            nodesLevel[i] = currentHiddenLayer;
            countTemp++;
            if(countTemp % nodesPerHiddenLayer == 0)
            {
                currentHiddenLayer++;
            }

            if(currentHiddenLayer == nodesLevel[0]  )
            {
                nodesLevel[i] = currentHiddenLayer - 1;
            }
        }

        for(int i = 0; i < nodesAmount; i++)
        {
            nodes[i] = i;
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
                    edgeWeight[i][j] = (int)(((float)(Math.random() * ((1 + 1) + 1)) - 1) * 100);
                    edgeWeight[i][j] = (float) (edgeWeight[i][j]) / 100;
                }
                else
                {
                    edges[i][j] = 0;
                    edgeWeight[i][j] = 0;
                }
            }
        }

        graph.put("edgeWeight", edgeWeight);
        graph.put("nodes", nodes);
        graph.put("nodesLevel", nodesLevel);
        graph.put("nodesValue", nodesValue);
        graph.put("edges", edges);
        graph.put("hiddenNodesLeft", hiddenLayerNodesAmount);

        code = graph.toString();
        text = "Find signal in every neuron and calculate MSE of last one";

        return new GeneratingResult(text, code, instructions);
    }
}
