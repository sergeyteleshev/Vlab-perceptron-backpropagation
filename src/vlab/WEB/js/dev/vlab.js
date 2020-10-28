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
    edgesAmount: 6,
};

//todo виснет весь проект. оптимизировать отрисовку
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
    let t = 1;
    let maxLevel = 1;
    let yLevel = 1;
    let currentEdgeSource = state.currentEdge.length === 2 ? state.currentEdge[0] : null;
    let currentEdgeTarget = state.currentEdge.length === 2 ? state.currentEdge[1] : null;

    for (let i = 0; i < nodesLevel.length; i++) {
        nodesLevelAmount[nodesLevel[i]] = 1 + (nodesLevelAmount[nodesLevel[i]] || 0);
    }

    nodesLevelAmount.map(el => {
       if (maxLevel < el)
        maxLevel = el;
    });

    let yCenter = maxLevel / 2;

    for (let i = 0; i < edges.length; i++) {
        let nodeValue = nodesValue[i] !== null ? `(I${i} = ${nodesValue[i]})` : "";
        let nodeColor = "#000";
        let nodeId = "n" + i;

        //рисует всё равно криво: порядок нод не тот по вертикали, но хотя бы выравнено, лол
        if(i === 0 || i === nodes.length - 1)
        {
            yLevel = yCenter;
        }
        else
        {
            let dy = nodesLevel[i] / maxLevel;

            yLevel = i * dy;

            // if(nodesLevel[i] === nodesLevel[i - 1])
            // {
            //     yLevel = dy * t;
            //     t++;
            // }
            // else
            // {
            //     t = 1;
            // }
        }

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

        for (let j = 0; j < edges.length; j++) {
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
        currentEdgeString = "w" + String(templateData.currentEdge[0]) + String(templateData.currentEdge[1]);

    if(templateData.edgesTableData)
    {
        for(let i = 0; i < templateData.edgesTableData.length; i++)
        {
            backPropagationData += `<tr>
            <td>
                "w${String(templateData.edgesTableData[i].edge[0]) + String(templateData.edgesTableData[i].edge[1])}"
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
                            <div id="container"></div>
                        </div>
                    </td>
                    <td class="step-td">                      
                        <div class="steps">
                            <div class="steps-buttons-backpropagation">
                                <input id="addStepBackpropagation" class="addStepBackpropagation btn btn-success" type="button" value="+"/>
                                <input type="button" class="minusStepBackpropagation btn btn-danger" value="-">
                            </div>
                            <table class="backpropagation steps-table">     
                                <tr>
                                    <th>Ребро</th>
                                    <th>DELTA</th>
                                    <th>GRAD</th>
                                    <th>DELTA W</th>
                                    <th>NEW W</th>
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

function subscriber() {
    const events = {};

    return {
        subscribe: function (event, fn) {
            if (!events[event]) {
                events[event] = [fn]
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

            selectedEdges.push("w" + state.currentEdge[0] + state.currentEdge[1]);

            if(selectedEdges.length === state.edgesAmount)
            {
                isBackpropagationDone = true;
                for(let i = 0; i < nodesValue.length; i++)
                {
                    if(i >= state.inputNeuronsAmount)
                        nodesValue[i] = null;
                }

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
                    delta: Number(document.getElementById("delta").value),
                    grad: Number(document.getElementById("grad").value),
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

    document.getElementById("addStep").addEventListener('click', () => {
        // обновляем стейт приложение
        const state = appInstance.state.updateState((state) => {
            let currentStep = state.currentStep;
            let neuronsTableData = state.neuronsTableData.slice();
            let nodesValue = state.nodesValue.slice();
            let currentSelectedNodeIdNumber = Number(state.currentSelectedNodeId.match(/(\d+)/)[0]);

            let prevSelectedNodeId = state.currentSelectedNodeId;
            let prevNeuronInputSignalFormula = state.currentNeuronInputSignalFormula;
            let prevNeuronInputSignalValue = state.currentNeuronInputSignalValue;
            let prevNeuronOutputSignalValue = state.currentNeuronOutputSignalValue;
            let prevNodeSection = state.currentNodeSection.slice();

            if(state.currentSelectedNodeId.length > 0
                && state.currentNodeSection.length > 0
                && !isNaN(document.getElementsByClassName("currentNeuronInputSignalFormula")[0].value)
                && !isNaN(document.getElementsByClassName("currentNeuronInputSignalValue")[0].value)
                && !isNaN(document.getElementsByClassName("currentNeuronOutputSignalValue")[0].value))
            {
                nodesValue[currentSelectedNodeIdNumber] = document.getElementsByClassName("currentNeuronOutputSignalValue")[0].value;
                currentStep++;
                neuronsTableData.push({
                    nodeId: state.currentSelectedNodeId,
                    neuronInputSignalFormula: document.getElementsByClassName("currentNeuronInputSignalFormula")[0].value,
                    neuronInputSignalValue: Number(document.getElementsByClassName("currentNeuronInputSignalValue")[0].value),
                    neuronOutputSignalValue: Number(document.getElementsByClassName("currentNeuronOutputSignalValue")[0].value),
                    nodeSection: state.currentNodeSection,
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
                currentStep,
                neuronsTableData,
                nodesValue,
                prevSelectedNodeId,
                prevNeuronInputSignalFormula,
                prevNeuronInputSignalValue,
                prevNeuronOutputSignalValue,
                prevNodeSection,
                currentSelectedNodeId: "",
                currentNeuronInputSignalFormula: "",
                currentNeuronInputSignalValue: "",
                currentNeuronOutputSignalValue: "",
                currentNodeSection: [],
                isSelectingNodesModeActivated: false,
            }
        });

        // перересовываем приложение
        appInstance.subscriber.emit('render', state);
    });

    document.getElementsByClassName("minusStep")[0].addEventListener('click', () => {
        // обновляем стейт приложение
        const state = appInstance.state.updateState((state) => {
            if(state.currentStep > 0)
            {
                let neuronsTableData = state.neuronsTableData.slice();
                let currentSelectedNodeIdNumber = Number(state.prevSelectedNodeId.match(/(\d+)/)[0]);
                neuronsTableData.pop();
                let nodesValueCopy = state.nodesValue.slice();
                nodesValueCopy[currentSelectedNodeIdNumber] = null;
                let prevNeuronInputSignalFormula = state.currentNeuronInputSignalFormula;
                let prevNeuronInputSignalValue = state.currentNeuronInputSignalValue;
                let prevNeuronOutputSignalValue = state.currentNeuronOutputSignalValue;
                let prevNodeSection = state.currentNodeSection;

                return  {
                    ...state,
                    neuronsTableData,
                    prevNeuronInputSignalFormula,
                    prevNeuronInputSignalValue,
                    prevNeuronOutputSignalValue,
                    currentStep: state.currentStep - 1,
                    currentSelectedNodeId: state.prevSelectedNodeId,
                    currentNeuronInputSignalFormula: state.prevNeuronInputSignalFormula,
                    currentNeuronInputSignalValue: state.prevNeuronInputSignalValue,
                    currentNeuronOutputSignalValue: state.prevNeuronOutputSignalValue,
                    currentNodeSection: state.prevNodeSection,
                    isSelectingNodesModeActivated: false,
                    nodesValue: nodesValueCopy,
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
            container: document.getElementById('container'),
            type: "canvas",
        }],
        settings: {
            defaultEdgeLabelSize: 15,
            enableEdgeHovering: true,
        },
    });

    let testData = dataToSigma(state);

    testData.nodes.map(node => {
        s.graph.addNode(node);
    });

    testData.edges.map(edge => {
        s.graph.addEdge(edge);
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
                alert("Сначала найдите все новые веса дендритов");

                return {
                    ...state,
                }
            }

        });

        appInstance.subscriber.emit('render', state);
    });

    s.bind('clickEdge', (res) => {
        const state = appInstance.state.updateState((state) => {
            if(state.isBackpropagationDone === false)
            {
                if(state.currentEdge.length === 2 && state.currentEdge[0] === res.data.edge.source
                    && state.currentEdge[1] === res.data.edge.target)
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

            if(document.getElementById("preGeneratedCode"))
            {
                if(document.getElementById("preGeneratedCode").value !== "")
                {
                    const state = appInstance.state.updateState((state) => {
                        // console.log(document.getElementById("preGeneratedCode").value, 'beforeParse');
                        let graph = JSON.parse(document.getElementById("preGeneratedCode").value);
                        // console.log(graph);
                        return {
                            ...state,
                            ...graph,
                        }
                    });
                }
            }

            const state = appInstance.state.updateState((state) => {
                return {
                    ...state,
                    ...test_graph,
                }
            });

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
            return appInstance.state.getState();
        },
        calculateHandler: function (text, code) {
        },
    }
}

var Vlab = init_lab();