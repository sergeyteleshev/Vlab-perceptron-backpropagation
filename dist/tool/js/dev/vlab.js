const test_graph = {
    nodes: [0,1,2,3,4],
    nodesLevel: [1,1,2,2,3],
    nodesValue: [1,0,0.61,0.69,0.33],
    edges: [
        [0,0,1,1,0],
        [0,0,1,1,0],
        [0,0,0,0,1],
        [0,0,0,0,1],
        [0,0,0,0,0],
    ],
    edgeWeight: [
        [0,0,0.45,0.78,0],
        [0,0,-0.12,0.13,0],
        [0,0,0,0,1.5],
        [0,0,0,0,-2.3],
        [0,0,0,0,0],
    ],
    inputNeuronsAmount: 2,
    outputNeuronsAmount: 1,
    amountOfNodesInHiddenLayer: 1,
    edgesAmount: 6,
    newNodesValue: [],
    initialNodesValue: [],
};

function roundToTwoDecimals(num)
{
    return Math.round(num * 100) / 100;
}

function getNodesSignal(nodes, edges, edgesWeight, nodesValue, inputNeuronsAmount)
{
    for(let i = inputNeuronsAmount; i < nodes.length; i++)
    {
        let nodeInputSignal = 0;
        let nodeOutputSignal = 0;

        for(let j = 0; j < i; j++)
        {
            if(edges[j][i] === 1)
            {
                nodeInputSignal += nodesValue[j] * edgesWeight[j][i];
            }
        }

        nodeInputSignal = roundToTwoDecimals(nodeInputSignal);
        nodeOutputSignal = sigmoid(nodeInputSignal);
        nodeOutputSignal= roundToTwoDecimals(nodeOutputSignal);

        nodesValue[i] = nodeOutputSignal;
    }

    return nodesValue;
}

function roundToTwoDecimals(x)
{
    return Math.round((x + Number.EPSILON) * 100) / 100;
}

function sigmoid(x)
{
    return (1 / (1 + Math.exp(-x)));
}

function dataToSigma(state) {
    let edges = state.edges;
    let nodes = state.nodes;
    let nodesLevel = state.nodesLevel;
    let edgeWeight = state.edgeWeight;
    let nodesValue = state.nodesValue;
    let neuronsTableData = state.neuronsTableData;
    let currentNodeSection = state.currentNodeSection;
    let currentSelectedNodeId = state.currentSelectedNodeId;
    let resultEdges = [];
    let resultNodes = [];
    let nodesLevelAmount = [];
    let maxLevel = 1;
    let currentEdgeSource = state.currentEdge.length === 2 ? state.currentEdge[0] : null;
    let currentEdgeTarget = state.currentEdge.length === 2 ? state.currentEdge[1] : null;
    let yLevelRandomDisplacement = state.yLevelRandomDisplacement;
    let inputNeuronsAmount = state.inputNeuronsAmount;
    let outputNeuronsAmount = state.outputNeuronsAmount;
    let amountOfNodesInHiddenLayer = state.amountOfNodesInHiddenLayer;
    let maxNeuronsInLayer = Math.max(inputNeuronsAmount, outputNeuronsAmount, amountOfNodesInHiddenLayer);

    for (let i = 0; i < nodesLevel.length; i++) {
        nodesLevelAmount[nodesLevel[i]] = 1 + (nodesLevelAmount[nodesLevel[i]] || 0);
    }

    nodesLevelAmount.map(el => {
        if (maxLevel < el)
            maxLevel = el;
    });

    for (let i = 0; i < edges.length; i++) {
        let nodeValue = nodesValue[i] !== null ? `(I${i} = ${nodesValue[i]})` : "";
        let nodeColor = "#000";
        let nodeId = "n" + i;
        let yLevel = 0;

        yLevel = maxNeuronsInLayer / (nodesLevel[i]) + i + 1 - nodesLevel[i];
        yLevel += yLevelRandomDisplacement[i];

        for(let j = 0; j > neuronsTableData.length; j++) {
            if (neuronsTableData[j].nodeId === nodeId)
            {
                nodeColor = "#28a745";
            }
        }

        if(typeof nodesValue[i] === "number")
        {
            nodeColor = "#28a745";
        }

        if(currentSelectedNodeId === nodeId)
        {
            nodeColor = "#00F";
        }

        currentNodeSection.map(currentNodeSectionId => {
            if(currentNodeSectionId === nodeId)
            {
                nodeColor = "#FF0";
            }
        });

        resultNodes[i] = {
            id: nodeId,
            label:  `${i.toString()} ${nodeValue}`,
            x: nodesLevel[i],
            y: yLevel,
            size: 4,
            color: nodeColor,
        };

        for (let j = 0; j < edges.length; j++)
        {
            let edgeColor = "#000";

            if(currentEdgeSource !== null && currentEdgeSource !== null && edges[currentEdgeSource][currentEdgeTarget] === 1
                && i === currentEdgeSource && j === currentEdgeTarget)
            {
                edgeColor = "#F00";
                currentEdgeSource = null;
                currentEdgeTarget = null;
            }

            for(let k = 0; k < state.selectedEdges.length; k++)
            {
                if(state.selectedEdges[k] === "w" + i + j)
                    edgeColor = "#0F0";
            }

            if(edges[i][j] === 1)
            {
                resultEdges.push({
                    id: "w" + i + j,
                    source: "n" + i,
                    target: "n" + j,
                    label: edgeWeight[i][j].toString(),
                    color: edgeColor,
                    size: 200,
                });
            }
        }
    }

    return {
        nodes: resultNodes,
        edges: resultEdges,
    }
}

function getHTML(templateData) {
    let tableData = "";
    let backPropagationData = "";
    let currentEdgeString = "";

    let countInvalidNodesValue = 0;

    if(templateData.nodesValue)
    {
        for(let i=0, l = templateData.nodesValue.length; i < l; i++){
            countInvalidNodesValue += (templateData.nodesValue[i] === null) ? 1 : 0;
        }
    }

    if (templateData.currentEdge[0] && templateData.currentEdge[1])
        currentEdgeString = "w" + templateData.currentEdge[0].toString() + templateData.currentEdge[1].toString();

    if(templateData.edgesTableData)
    {
        for(let i = 0; i < templateData.edgesTableData.length; i++)
        {
            backPropagationData += `<tr>
            <td>
                w${templateData.edgesTableData[i].edge[0].toString() + templateData.edgesTableData[i].edge[1].toString()}
            </td>
            <td>
                ${templateData.edgesTableData[i].delta}
            </td>
            <td>
                ${templateData.edgesTableData[i].grad}            
            </td>
            <td>
                ${templateData.edgesTableData[i].deltaW}            
            </td>
            <td>
                ${templateData.edgesTableData[i].newW}            
            </td>
        </tr>`;
        }
    }

    if(templateData.edgesTableData.length !== templateData.edgesAmount)
    {
        backPropagationData += `<tr>
            <td>
                ${currentEdgeString}
            </td>
            <td>
                <input value="${templateData.currentDelta}" id="delta" placeholder="Введите число" ${templateData.currentEdge.length !== 2 ? "disabled" : ""} class="tableInputData" type="number"/>
            </td>
            <td>
                <input value="${templateData.currentGrad}" id="grad" placeholder="Введите число" ${templateData.currentEdge.length !== 2 ? "disabled" : ""} class="tableInputData" type="number"/>
            </td>
            <td>
                <input value="${templateData.currentDeltaW}" id="deltaW" placeholder="Введите число" ${templateData.currentEdge.length !== 2 ? "disabled" : ""} class="tableInputData" type="number"/>
            </td>
            <td>
                <input value="${templateData.currentNewW}" id="newW" placeholder="Введите число" ${templateData.currentEdge.length !== 2 ? "disabled" : ""} class="tableInputData" type="number"/>
            </td>
        </tr>
    `;
    }

    if(templateData.neuronsTableData)
    {
        for(let i = 0; i < templateData.neuronsTableData.length; i++)
        {
            tableData += `<tr>
            <td>
                ${templateData.neuronsTableData[i].nodeId}
            </td>
            <td>
                ${templateData.neuronsTableData[i].neuronInputSignalFormula}
            </td>
            <td>
                ${templateData.neuronsTableData[i].neuronInputSignalValue}            
            </td>
            <td>
                ${templateData.neuronsTableData[i].neuronOutputSignalValue}            
            </td>
        </tr>`;
        }
    }

    return `
        <div class="lab">
            <table class="lab-table">
                <tr>
                    <td colspan="2">
                        <div class="lab-header">
                            <div></div>
                            <span>Метод обратного распространения сигнала в перцептроне</span>
                            <!-- Button trigger modal -->
                            <button type="button" class="btn btn-info" data-toggle="modal" data-target="#exampleModalScrollable">
                              Справка
                            </button>
                            
                            <!-- Modal -->
                            <div class="modal fade" id="exampleModalScrollable" tabindex="-1" role="dialog" aria-labelledby="exampleModalScrollableTitle" aria-hidden="true">
                              <div class="modal-dialog modal-dialog-scrollable" role="document">
                                <div class="modal-content">
                                  <div class="modal-header">
                                    <h5 class="modal-title" id="exampleModalScrollableTitle">Справка по интерфейсу лабораторной работы</h5>
                                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                      <span aria-hidden="true">&times;</span>
                                    </button>
                                  </div>
                                  <div class="modal-body">
                                        <p><b>Алгоритм работы с интерфейсом:</b></p>
                                        <p>
                                            1) Для того, чтобы начать строить путь из истока к стоку, нужно кликнуть на исток. Путь может начинаться только из него.                                            
                                            Далее нужно включить в текущий путь только те вершины, в которые есть ребро с <u>не</u> нулевым весом. Если вес <u>равен</u> нулю(в любую из сторон), то 
                                            лабораторная работа не позволит вам выделить эту вершину.
                                        </p>
                                        <p>
                                            2) После того как путь построен нужно в текстовом поле "минимальный поток текущей итерации" ввести то, что требуется и нажать на "+". Тем самым вы перейдёте на следующую итерацию алгоритма.
                                        </p>
                                        <p>
                                            3) Повторять шаги 2 и 3 до тех пор пока существует путь из истока к стоку.
                                        </p>
                                        <p>
                                            4) После того как путей больше нет, необходимо нажать на кнопку "завершить". Тем самым разблокируется текстовое поле "Максимальный поток графа", и можно будет ввести полученный ответ.                                        
                                        </p>
                                        <p>
                                            5) Чтобы завершить лабораторную работу, нужно нажать кнопку "отправить".
                                        </p>
                                        <p><b>Примечание:</b></p>
                                        <p>1) После ввода значений в текстовые поля кнопки не кликаются с первого раза, так как фокус остаётся на текстовом поле. Первым кликом(в любое место окна ЛР) нужно убрать фокус, а затем нажать на нужную кнопку</p>
                                        <p>2) После нажатия кнопки "завершить" весь остальной интерфейс остаётся кликабельным, так что стоит быть аккуратнее, чтобы не "сбить" результат работы.</p>
                                  </div>                                 
                                </div>
                              </div>
                            </div>                           
                        </div>
                    </td>
                </tr>
                <tr>
                    <td>
                        <div class="graphComponent">                          
                                <button type="button" class="btn btn-info redrawGraph">Перерисовать граф</button>
                            <div id="container"></div>
                        </div>
                    </td>
                    <td class="step-td">                      
                        <div class="steps">
                            <div class="steps-buttons-backpropagation">
                                <input id="addStepBackpropagation" class="addStepBackpropagation btn btn-success" type="button" value="Следующий шаг"/>
                                <input type="button" class="minusStepBackpropagation btn btn-danger" value="Предыдущий шаг">                                
                            </div>
                            <table class="backpropagation steps-table">     
                                <tr>
                                    <th>Ребро</th>
                                    <th>DELTA</th>
                                    <th>GRAD</th>
                                    <th>dtW</th>
                                    <th>W</th>
                                </tr>                
                                ${backPropagationData}
                            </table>                                       
                            <div class="maxFlow">
                                <span>MSE:</span>
                                <input type='number' ${countInvalidNodesValue !== 0 || templateData.isBackpropagationDone === false ? "disabled" : ""} class='maxFlow-input' id="error" value="${templateData.error}"'/>                       
                            </div>                                                                                                                                            
                        </div>
                    </td>
                </tr>
            </table>                                                                         
        </div>`;
}

function renderTemplate(element, html) {
    element.innerHTML = html;
}

function initState() {
    let _state = {
        currentNodeSection: [],
        neuronsTableData: [],
        edgesTableData: [],
        currentSelectedNodeId: "",
        prevSelectedNodeId: "",
        prevNeuronInputSignalFormula: "",
        prevNeuronInputSignalValue: "",
        prevNeuronOutputSignalValue: "",
        prevNodeSection: [],
        currentNeuronInputSignalFormula: "",
        currentNeuronInputSignalValue: "",
        currentNeuronOutputSignalValue: "",
        error: 0,
        isSelectingNodesModeActivated: false,
        currentStep: 0,
        currentEdgeStep: 0,
        isBackpropagationDone: false,
        currentEdge: [],
        selectedEdges: [],
        currentDelta: null,
        currentGrad: null,
        currentDeltaW: null,
        currentNewW: null,
        newNodesValue: [],
        initialNodesValue: [],
    };

    return {
        getState: function () {
            return _state
        },
        updateState: function (callback) {
            _state = callback(_state);
            return _state;
        }
    }
}

function convertEdgesTableDataToMatrix(clientAnswer)
{
    console.log(clientAnswer);
    let newW = [];
    let delta = [];
    let deltaW = [];
    let grad = [];

    for (let i = 0; i < clientAnswer.length; i++)
    {
        delta.push(0);
        newW.push([]);
        deltaW.push([]);
        grad.push([]);

        for (let j = 0; j < clientAnswer.length; j++)
        {
            newW[i].push(0);
            deltaW[i].push(0);
            grad[i].push(0);
        }
    }

    for (let i = 0; i < clientAnswer.length; i++)
    {
        let nodeFromIndex = +clientAnswer[i].edge[0];
        let nodeToIndex = +clientAnswer[i].edge[1];

        newW[nodeFromIndex][nodeToIndex] = clientAnswer[i].newW;
        delta[i] = clientAnswer[i].delta;
        deltaW[nodeFromIndex][nodeToIndex] = clientAnswer[i].deltaW;
        grad[nodeFromIndex][nodeToIndex] = clientAnswer[i].grad;
    }

    return {
        newW,
        delta,
        deltaW,
        grad
    };
}

function subscriber() {
    const events = {};

    return {
        subscribe: function (event, fn) {
            if (!events[event]) {
                events[event] = [fn];
            } else {
                events[event] = [fn];
            }
        },
        emit: function (event, data = undefined) {
            events[event].map(fn => data ? fn(data) : fn());
        }
    }
}

function App() {
    return {
        state: initState(),
        subscriber: subscriber(),
    }
}

function bindActionListeners(appInstance)
{
    document.getElementsByClassName('redrawGraph')[0].addEventListener('click', () => {
        const state = appInstance.state.updateState((state) => {
            let yLevelRandomDisplacement = state.nodes.map(() => {
                return 2 + Math.random() * 3; //смещение ноты по Y из-за того, что не видно значение ребра при отрисовке
            });

            return {
                ...state,
                yLevelRandomDisplacement,
            }
        });

        // перересовываем приложение
        appInstance.subscriber.emit('render', state);
    });

    document.getElementById("error").addEventListener('change', () => {
        const state = appInstance.state.updateState((state) => {

            if(isNaN(document.getElementById("error").value))
            {
                return {
                    ...state,
                    error: 0,
                }
            }

            return {
                ...state,
                error: Number(document.getElementById("error").value),
            }
        });

        appInstance.subscriber.emit('render', state);
    });

    document.getElementById("addStepBackpropagation").addEventListener('click', () => {
        const state = appInstance.state.updateState((state) => {
            let currentEdgeStep = state.currentEdgeStep;
            let edgesTableData = state.edgesTableData.slice();
            let edgeWeight = state.edgeWeight.slice();
            let nodesValue = state.nodesValue.slice();

            let prevDelta = state.currentDelta;
            let prevGrad = state.currentGrad ;
            let prevDeltaW = state.currentDeltaW ;
            let prevNewW = state.currentNewW;
            let prevEdge = state.currentEdge.slice();
            let selectedEdges = state.selectedEdges.slice();
            let isBackpropagationDone = false;
            let nodes = state.nodes;
            let edges = state.edges;
            let inputNeuronsAmount = state.inputNeuronsAmount;
            let newNodesValue = state.newNodesValue;

            selectedEdges.push("w" + state.currentEdge[0] + state.currentEdge[1]);

            if(selectedEdges.length === state.edgesAmount)
            {
                isBackpropagationDone = true;

                let newEdgesWeight = convertEdgesTableDataToMatrix(edgesTableData).newW;
                console.log("newEdgesWeight", newEdgesWeight);
                nodesValue = getNodesSignal(nodes, edges, newEdgesWeight, nodesValue, inputNeuronsAmount);
                newNodesValue = nodesValue.slice();

                for(let i = 0; i < edgesTableData.length; i++)
                    edgeWeight[edgesTableData[i].edge[0]][edgesTableData[i].edge[1]] = edgesTableData[i].newW;

                selectedEdges = [];
            }

            if(state.currentEdge.length > 0 && !isNaN(state.currentDelta)
                && !isNaN(state.currentGrad) && !isNaN(state.currentDeltaW) && !isNaN(state.currentNewW))
            {
                currentEdgeStep++;
                edgesTableData.push({
                    edge: state.currentEdge.slice(),
                    delta: roundToTwoDecimals(Number(document.getElementById("delta").value)),
                    grad: roundToTwoDecimals(Number(document.getElementById("grad").value)),
                    deltaW: roundToTwoDecimals(Number(document.getElementById("deltaW").value)),
                    newW: roundToTwoDecimals(Number(document.getElementById("newW").value)),
                });
            }
            else
            {
                return {
                    ...state,
                }
            }

            return  {
                ...state,
                edgeWeight,
                newNodesValue,
                nodesValue,
                isBackpropagationDone,
                currentEdgeStep,
                edgesTableData,
                prevDelta,
                prevGrad,
                prevDeltaW,
                prevNewW,
                prevEdge,
                currentDelta: "",
                currentGrad: "",
                currentDeltaW: "",
                currentNewW: "",
                currentEdge: [],
                selectedEdges,
            }
        });

        // перересовываем приложение
        appInstance.subscriber.emit('render', state);
    });

    document.getElementsByClassName("minusStepBackpropagation")[0].addEventListener('click', () => {
        // обновляем стейт приложение
        const state = appInstance.state.updateState((state) => {
            if(state.currentEdgeStep > 0)
            {
                let edgesTableData = state.edgesTableData.slice();
                // let currentSelectedNodeIdNumber = Number(state.prevSelectedNodeId.match(/(\d+)/)[0]);
                let prevDelta = edgesTableData[edgesTableData.length - 1].delta;
                let prevGrad = edgesTableData[edgesTableData.length - 1].grad;
                let prevDeltaW = edgesTableData[edgesTableData.length - 1].deltaW;
                let prevNewW = edgesTableData[edgesTableData.length - 1].newW;
                let prevEdge = edgesTableData[edgesTableData.length - 1].edge;
                let selectedEdges = state.selectedEdges.slice();

                edgesTableData.pop();
                selectedEdges.pop();

                return  {
                    ...state,
                    edgesTableData,
                    selectedEdges,
                    currentDelta: prevDelta,
                    currentGrad: prevGrad,
                    currentDeltaW: prevDeltaW,
                    currentNewW: prevNewW,
                    currentEdge: prevEdge,
                    currentEdgeStep: state.currentEdgeStep - 1,
                }
            }

            return  {
                ...state,
            }
        });

        // перересовываем приложение
        appInstance.subscriber.emit('render', state);
    });
}

function renderDag(state, appInstance) {
    //удаляем содержимое графа для экономия памяти браузера
    let graphContainer = document.getElementById('container');
    while (graphContainer.firstChild) {
        graphContainer.removeChild(graphContainer.firstChild);
    }

    let s = new sigma({
        renderers: [{
            container: document.getElementById('container'),
            type: "canvas",
        }],
        settings: {
            defaultEdgeLabelSize: 15,
            enableEdgeHovering: true,
        },
    });

    let graphData = dataToSigma(state);

    graphData.nodes.map(node => {
        s.graph.addNode(node);
    });

    graphData.edges.map(edge => {
        s.graph.addEdge(edge);
    });

    s.bind('clickEdge', (res) => {
        const state = appInstance.state.updateState((state) => {
            if(state.isBackpropagationDone === false)
            {
                if(state.currentEdge.length)
                {
                    return {
                        ...state,
                        currentEdge: []
                    }
                }
                else if(state.currentEdge.length === 0)
                {
                    return {
                        ...state,
                        currentEdge: [Number(res.data.edge.source.match(/(\d+)/)[0]), Number(res.data.edge.target.match(/(\d+)/)[0])]
                    }
                }
            }

            return {
                ...state,
            }
        });

        appInstance.subscriber.emit('render', state);
    });

    s.bind('clickNode', (res) => {
        const state = appInstance.state.updateState((state) => {
            if(state.isBackpropagationDone)
            {
                if(state.isSelectingNodesModeActivated)
                {
                    let currentNodeSectionCopy = [...state.currentNodeSection];
                    let isNodeInList = false;

                    currentNodeSectionCopy.map((nodeId,index)=> {
                        if(nodeId === res.data.node.id)
                        {
                            currentNodeSectionCopy.splice(index, 1);
                            isNodeInList = true;
                            return;
                        }
                    });

                    if(!isNodeInList && res.data.node.id !== state.currentSelectedNodeId)
                    {
                        currentNodeSectionCopy.push(res.data.node.id);
                    }
                    else if (res.data.node.id === state.currentSelectedNodeId)
                    {
                        return {
                            ...state,
                            currentNodeSection: [],
                            currentSelectedNodeId: "",
                            isSelectingNodesModeActivated: false,
                        }
                    }

                    return {
                        ...state,
                        currentNodeSection: currentNodeSectionCopy,
                    }
                }
                else
                {
                    if(state.currentSelectedNodeId === res.data.node.id)
                    {
                        return {
                            ...state,
                            currentSelectedNodeId: "",
                            isSelectingNodesModeActivated: false,
                        }
                    }
                    else
                    {
                        return {
                            ...state,
                            currentSelectedNodeId: res.data.node.id,
                            isSelectingNodesModeActivated: true,
                        }
                    }
                }
            }
            else
            {
                alert("Сначала найдите все новые веса рёбер");

                return {
                    ...state,
                }
            }

        });

        appInstance.subscriber.emit('render', state);
    });

    s.refresh();
}

function init_lab() {
    const appInstance = App();
    return {
        setletiant: function (str) {
        },
        setPreviosSolution: function (str) {
        },
        setMode: function (str) {
        },

        //Инициализация ВЛ
        init: function () {
            const root = document.getElementById('jsLab');

            if(document.getElementById("preGeneratedCode") && document.getElementById("preGeneratedCode").value !== "")
            {
                const state = appInstance.state.updateState((state) => {
                    let graph = JSON.parse(document.getElementById("preGeneratedCode").value);
                    let nodes = graph.nodes;
                    let initialNodesValue = graph.nodesValue.slice();
                    let yLevelRandomDisplacement = nodes.map(node => {
                        return 2 + Math.random() * 3; //смещение ноты по Y из-за того, что не видно значение ребра при отрисовке
                    });

                    return {
                        ...state,
                        ...graph,
                        yLevelRandomDisplacement,
                        initialNodesValue,
                    }
                });
            }

            // тестовый граф
            // const state = appInstance.state.updateState((state) => {
            //     return {
            //         ...state,
            //         ...test_graph,
            //     }
            // });

            // основная функция для рендеринга
            const render = (state) => {
                console.log('state', state);
                console.log(appInstance);
                renderTemplate(root, getHTML({...state}));
                renderDag(state, appInstance);
                bindActionListeners(appInstance);
            };

            appInstance.subscriber.subscribe('render', render);

            // инициализируем первую отрисовку
            appInstance.subscriber.emit('render', appInstance.state.getState());
        },
        getCondition: function () {
        },
        getResults: function () {
            console.log('getResults', appInstance.state.getState());
            return appInstance.state.getState();
        },
        calculateHandler: function (text, code) {
        },
    }
}

var Vlab = init_lab();