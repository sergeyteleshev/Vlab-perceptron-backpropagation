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

    if(nodesLevel)
    {
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
                nodeColor = "#DE4E5A";
            }

            currentNodeSection.map(currentNodeSectionId => {
                if(currentNodeSectionId === nodeId)
                {
                    nodeColor = "#DEBF59";
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
                    edgeColor = "#DE4E5A";
                    currentEdgeSource = null;
                    currentEdgeTarget = null;
                }

                for(let k = 0; k < state.selectedEdges.length; k++)
                {
                    if(state.selectedEdges[k] === "w" + i + j)
                        edgeColor = "#28a745";
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
        currentEdgeString = "(" + templateData.currentEdge[0].toString() + ", " + templateData.currentEdge[1].toString() + ")";

    if(templateData.edgesTableData)
    {
        for(let i = 0; i < templateData.edgesTableData.length; i++)
        {
            backPropagationData += `<tr>
            <td>
                (${templateData.edgesTableData[i].edge[0].toString() + ", " + templateData.edgesTableData[i].edge[1].toString()})
            </td>
            <td>
                ${templateData.edgesTableData[i].wijZero}            
            </td>
            <td>
                ${templateData.edgesTableData[i].deltaWijZero}            
            </td>
            <td>
                ${templateData.edgesTableData[i].delta}
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
                <input value="${templateData.currentWijZero}" id="wijZero" placeholder="Введите число" ${templateData.currentEdge.length !== 2 ? "disabled" : ""} class="tableInputData" type="number"/>
            </td>
            <td>
                <input value="${templateData.currentDeltaWijZero}" id="deltaWijZero" placeholder="Введите число" ${templateData.currentEdge.length !== 2 ? "disabled" : ""} class="tableInputData" type="number"/>
            </td>
            <td>
                <input value="${templateData.currentDelta}" id="delta" placeholder="Введите число" ${templateData.currentEdge.length !== 2 ? "disabled" : ""} class="tableInputData" type="number"/>
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
            <div class="lab-table">                                
                <div class="lab-header_text">Алгоритм обратного распространения ошибки в перцептроне</div>
                <div class="header-buttons">
                    <button type="button" class="btn btn-info redrawGraph">Перерисовать граф</button>
                    <button type="button" class="btn btn-info showReference" data-toggle="modal" data-target="#exampleModalScrollable">Справка</button>
                </div>            
                <div class="graphComponent">                                                  
                    <div id="graphContainer"></div>
                </div>                               
                <div class="steps">
                    <div class="steps-buttons-backpropagation">
                        <input id="addStepBackpropagation" class="addStepBackpropagation btn btn-success" type="button" value="Следующий шаг"/>
                        <input type="button" class="minusStepBackpropagation btn btn-danger" value="Предыдущий шаг">                                
                    </div>
                    <table class="backpropagation steps-table">     
                        <tr>
                            <th>
                                <p>Дуга</p>
                                <p>(X<sub>i</sub>, X<sub>j</sub>)</p>
                            </th>
                            <th>W<sub>ij</sub><sup>0</sup></th>
                            <th>&Delta;W<sub>ij</sub><sup>0</sup></th>
                            <th>&delta;(X<sub>j</sub>)</th>                           
                            <th>&Delta;W<sub>ij</sub><sup>1</sup></th>
                            <th>W<sub>ij</sub><sup>1</sup></th>
                        </tr>                
                        ${backPropagationData}
                    </table>                                       
                    <div class="maxFlow">
                        <span>MSE:</span>
                        <input type='number' ${countInvalidNodesValue !== 0 || templateData.isBackpropagationDone === false ? "disabled" : ""} class='maxFlow-input' id="error" value="${templateData.error}"'/>                       
                    </div>                                                                                                                                            
                </div> 
                <div class="lab-header">                                        
                    <!-- Button trigger modal -->                                        
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
                                <p>1) Если граф отобразился так, что не видно значение рёбер или вершин графа, то нужно нажать <b>кнопку "перерисовать граф"</b>.</p>
                                <p>2) При клике на ребро, оно становится красным и в таблице появляется <b>значение ребра для текущей итерации</b>.</p>                                        
                                <p>3) После того как все данные таблицы для текущей итерации заполнены, нужно нажать <b>кнопку "следующий шаг"</b>. Введённые числовые <b>значения автоматически округлятся до 2х знаков</b>.</p>
                                <p>4) Если вы совершили ошибку, то вы можете отменить текущую итерацию нажав кнопку <b>"Предыдущий шаг"</b>.</p>
                                <p>5) <b>Поле MSE</b> откроется сразу после того как будут рассчитаны все новые значения весов рёбер.</p>                                                                              
                          </div>                                 
                        </div>
                      </div>
                    </div>                           
                </div>                       
            </div>                                                                                     
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
        currentWijZero: null,
        currentDeltaWijZero: null,
        currentDelta: null,
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
    let wijZero = [];
    let deltaWijZero = [];
    let delta = [];
    let deltaW = [];
    let newW = [];

    for (let i = 0; i < clientAnswer.length; i++)
    {
        delta.push(0);
        newW.push([]);
        deltaW.push([]);
        wijZero.push([]);
        deltaWijZero.push([]);

        for (let j = 0; j < clientAnswer.length; j++)
        {
            newW[i].push(0);
            deltaW[i].push(0);
            wijZero[i].push(0);
            deltaWijZero[i].push(0);
        }
    }

    for (let i = 0; i < clientAnswer.length; i++)
    {
        let nodeFromIndex = +clientAnswer[i].edge[0];
        let nodeToIndex = +clientAnswer[i].edge[1];

        newW[nodeFromIndex][nodeToIndex] = clientAnswer[i].newW;
        delta[i] = clientAnswer[i].delta;
        deltaW[nodeFromIndex][nodeToIndex] = clientAnswer[i].deltaW;
        deltaWijZero[nodeFromIndex][nodeToIndex] = clientAnswer[i].deltaWijZero;
        wijZero[nodeFromIndex][nodeToIndex] = clientAnswer[i].wijZero;
    }

    return {
        newW,
        delta,
        deltaW,
        deltaWijZero,
        wijZero,
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
            let prevWijZero = state.currentWijZero;
            let prevDeltaWijZero = state.currentDeltaWijZero;
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
                && !isNaN(state.currentWijZero) && !isNaN(state.currentDeltaWijZero) && !isNaN(state.currentDeltaW) && !isNaN(state.currentNewW))
            {
                currentEdgeStep++;
                edgesTableData.push({
                    edge: state.currentEdge.slice(),
                    delta: Number(document.getElementById("delta").value),
                    wijZero: Number(document.getElementById("wijZero").value),
                    deltaWijZero: Number(document.getElementById("deltaWijZero").value),
                    deltaW: Number(document.getElementById("deltaW").value),
                    newW: Number(document.getElementById("newW").value),
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
                prevWijZero,
                prevDeltaWijZero,
                prevDeltaW,
                prevNewW,
                prevEdge,
                currentDelta: "",
                currentDeltaWijZero: "",
                currentWijZero: "",
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
                let prevWijZero = edgesTableData[edgesTableData.length - 1].wijZero
                let prevDeltaWijZero = edgesTableData[edgesTableData.length - 1].deltaWijZero;
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
                    currentWijZero: prevWijZero,
                    currentDeltaWijZero: prevDeltaWijZero,
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
    let s = new sigma({
        renderers: [{
            container: document.getElementById('graphContainer'),
            type: "canvas",
        }],
        settings: {
            defaultEdgeLabelSize: 15,
            enableEdgeHovering: true,
        },
    });

    let graphData = dataToSigma(state);
    console.log('graphData', graphData);

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
            let result = {...appInstance.state.getState()};
            delete result.edgeWeight;
            console.log('getResults', result);
            return JSON.stringify(result);
        },
        calculateHandler: function (text, code) {
        },
    }
}

var Vlab = init_lab();