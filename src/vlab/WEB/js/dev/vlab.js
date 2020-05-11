const test_graph = {
    nodes: [0,1,2,3,4,5,6,7,8],
    nodesLevel: [1, 2, 2, 2, 3, 3, 3, 3, 4],
    edges: [
        [0, 1, 1, 1, 0, 0, 0, 0, 0],
        [0, 0, 0, 0, 1, 1, 1, 1, 0],
        [0, 0, 0, 0, 1, 1, 1, 1, 0],
        [0, 0, 0, 0, 1, 1, 1, 1, 0],
        [0, 0, 0, 0, 1, 1, 1, 1, 0],
        [0, 0, 0, 0, 0, 0, 0, 0, 1],
        [0, 0, 0, 0, 0, 0, 0, 0, 1],
        [0, 0, 0, 0, 0, 0, 0, 0, 1],
        [0, 0, 0, 0, 0, 0, 0, 0, 0],
    ],
};

const test_graph_2 = {
    nodes: [0,1,2,3,4],
    nodesLevel: [1, 1, 2, 2, 3],
    nodesValue: [1, 0, null, 3, null],
    edges: [
        [0, 0, 1, 1, 0],
        [0, 0, 1, 1, 0],
        [0, 0, 0, 0, 1],
        [0, 0, 0, 0, 1],
        [0, 0, 0, 0, 0],
    ],
    edgeWeight: [
        [0, 0, -0.12, 0.78, 0],
        [0, 0, 0.45, 0.13, 0],
        [0, 0, 0, 0, 1.5],
        [0, 0, 0, 0, -2.3],
        [0, 0, 0, 0, 0],
    ],
};

function dataToSigma(state) {
    if(state.edges && state.nodes && state.nodesLevel && state.edgeWeight && state.nodesValue)
    {

    }

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

    for (let i = 0; i < nodesLevel.length; i++) {
        nodesLevelAmount[nodesLevel[i]] = 1 + (nodesLevelAmount[nodesLevel[i]] || 0);
    }

    nodesLevelAmount.map(el => {
       if (maxLevel < el)
        maxLevel = el;
    });

    let yCenter = maxLevel / 2;

    for (let i = 0; i < nodes.length; i++) {
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
    }

    for (let i = 0; i < edges.length; i++) {
        for (let j = 0; j < edges.length; j++) {
            if(edges[i][j] === 1)
            {
                resultEdges.push({
                    id: "e" + i + j,
                    source: "n" + i,
                    target: "n" + j,
                    label: edgeWeight[i][j].toString(),
                    color: "#000"
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

    let countInvalidNodesValue = 0;

    if(templateData.nodesValue)
    {
        for(let i=0, l = templateData.nodesValue.length; i < l; i++){
            countInvalidNodesValue += (templateData.nodesValue[i] === null) ? 1 : 0;
        }
    }

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

    tableData += `<tr>
        <td>
            ${templateData.currentSelectedNodeId ? templateData.currentSelectedNodeId : ""}
        </td>
        <td>
            <input id="currentNeuronInputSignalFormula" placeholder="Введите числовую формулу" class="tableInputData" type="text" value="${templateData.currentNeuronInputSignalFormula}"/>
        </td>
        <td>
            <input id="currentNeuronInputSignalValue" placeholder="Введите число" class="tableInputData" type="number" value="${templateData.currentNeuronInputSignalValue}"/>
        </td>
        <td>
            <input id="currentNeuronOutputSignalValue" placeholder="Введите число" class="tableInputData" type="number" value="${templateData.currentNeuronOutputSignalValue}"/>
        </td>
    </tr>`;

    return `
        <div class="lab">
            <table class="lab-table">
                <tr>
                    <td colspan="2">
                        <div class="lab-header">
                            <div></div>
                            <span>Ток сигнала в перцептроне Розенблатта</span>
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
                            <div class="steps-buttons">
                                <input id="addStep" class="addStep btn btn-success" type="button" value="+"/>
                                <input type="button" class="minusStep btn btn-danger" value="-">
                            </div>  
                            <table class="steps-table">
                                <tr>
                                    <th>№ нейрона</th>
                                    <th>Формула входного сигнала</th>
                                    <th>Значение входного сигнала</th>
                                    <th>Значение выходного сигнала</th>
                                </tr>                        
                                ${tableData}                                        
                            </table>                             
                            <div class="maxFlow">
                                <span>MSE:</span>
                                <input type='number' ${countInvalidNodesValue !== 0 ? "disabled" : ""} class='maxFlow-input' id="error" value="${templateData.error}"'/>                       
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
        // ...test_graph_2,
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
                events[event].push(fn);
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

    document.getElementById("currentNeuronOutputSignalValue").addEventListener('change', () => {
        const state = appInstance.state.updateState((state) => {
            return {
                ...state,
                currentNeuronOutputSignalValue: Number(document.getElementById("currentNeuronOutputSignalValue").value),
            }
        });

        appInstance.subscriber.emit('render', state);
    });

    document.getElementById("currentNeuronInputSignalFormula").addEventListener('change', () => {
        const state = appInstance.state.updateState((state) => {
            return {
                ...state,
                currentNeuronInputSignalFormula: document.getElementById("currentNeuronInputSignalFormula").value,
            }
        });

        appInstance.subscriber.emit('render', state);
    });

    document.getElementById("currentNeuronInputSignalValue").addEventListener('change', () => {
        const state = appInstance.state.updateState((state) => {
            return {
                ...state,
                currentNeuronInputSignalValue: Number(document.getElementById("currentNeuronInputSignalValue").value),
            }
        });

        appInstance.subscriber.emit('render', state);
    });

    document.getElementById("addStep").addEventListener('click', () => {
        // обновляем стейт приложение
        const state = appInstance.state.updateState((state) => {
            let currentStep = state.currentStep;
            let neuronsTableData = state.neuronsTableData.slice();
            let nodesValue = state.nodesValue.slice();
            let currentSelectedNodeIdNumber = state.currentSelectedNodeId.match(/(\d+)/)[0];

            let prevSelectedNodeId = state.currentSelectedNodeId;
            let prevNeuronInputSignalFormula = state.currentNeuronInputSignalFormula ;
            let prevNeuronInputSignalValue = state.currentNeuronInputSignalValue ;
            let prevNeuronOutputSignalValue = state.currentNeuronOutputSignalValue;
            let prevNodeSection = state.currentNodeSection.slice();

            if(state.currentSelectedNodeId.length > 0 && state.currentNeuronInputSignalFormula.length > 0
                && !isNaN(state.currentNeuronInputSignalValue) && !isNaN(state.currentNeuronOutputSignalValue)
                && state.currentNodeSection.length > 0)
            {
                nodesValue[currentSelectedNodeIdNumber] = state.currentNeuronOutputSignalValue;
                currentStep++;
                neuronsTableData.push({
                    nodeId: state.currentSelectedNodeId,
                    neuronInputSignalFormula: state.currentNeuronInputSignalFormula,
                    neuronInputSignalValue: state.currentNeuronInputSignalValue,
                    neuronOutputSignalValue: state.currentNeuronOutputSignalValue,
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
        // renderDag(state, appInstance);
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
    var s = new sigma({
        renderers: [{
            container: document.getElementById('container'),
            type: "canvas",
        }],
        settings: {
            defaultEdgeLabelSize: 15,
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
            if(document.getElementById("preGeneratedCode"))
            {
                if(document.getElementById("preGeneratedCode").value !== "")
                {
                    const state = appInstance.state.updateState((state) => {
                        console.log(document.getElementById("preGeneratedCode").value, 'beforeParse');
                        let graph = JSON.parse(document.getElementById("preGeneratedCode").value);
                        console.log(graph);
                        return {
                            ...state,
                            ...graph,
                        }
                    });
                }

                const root = document.getElementById('jsLab');

                // основная функция для рендеринга
                const render = (state) => {
                    console.log('state', state);
                    renderTemplate(root, getHTML({...state}));
                    renderDag(state, appInstance);
                    bindActionListeners(appInstance);
                };

                appInstance.subscriber.subscribe('render', render);

                // инициализируем первую отрисовку
                appInstance.subscriber.emit('render', appInstance.state.getState());
            }
            else
            {
                // const state = appInstance.state.updateState((state) => {
                //     return {
                //         ...state,
                //         stepsVariantData: [{...test_graph}],
                //         graphSkeleton: [...test_graph.edges],
                //     }
                // });

                //appInstance.subscriber.emit('render', appInstance.state.getState());
            }
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