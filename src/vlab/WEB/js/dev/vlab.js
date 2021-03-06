function getCookie(name)
{

    let matches = document.cookie.match(new RegExp(
        "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
    ))
    return matches ? decodeURIComponent(matches[1]) : undefined
}

function setCookie(name, value, props)
{

    props = props || {}
    let exp = props.expires

    if (typeof exp == "number" && exp) {
        let d = new Date()
        d.setTime(d.getTime() + exp*1000)
        exp = props.expires = d
    }

    if(exp && exp.toUTCString) { props.expires = exp.toUTCString() }

    value = encodeURIComponent(value)
    let updatedCookie = name + "=" + value

    for(let propName in props){
        updatedCookie += "; " + propName
        let propValue = props[propName]

        if(propValue !== true){ updatedCookie += "=" + propValue }
    }

    document.cookie = updatedCookie
}

function deleteCookie(name)
{
    setCookie(name, null, { expires: -1 })
}

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
        nodeOutputSignal = roundToTwoDecimals(nodeOutputSignal);

        nodesValue[i] = nodeOutputSignal;
    }

    return nodesValue;
}

function sigmoid(x)
{
    return (1 / (1 + Math.exp(-x)));
}

function isNumber(value)
{
    return typeof value === 'number' && isFinite(value);
}

function getHTML(templateData) {
    let tableData = "";
    let backPropagationData = "";
    let currentEdgeString = "";
    let initEdgeWeightTable = "";

    if(templateData.initEdgeWeight && templateData.initEdgeWeight.length)
    {
        let initEdgeWeight = [...JSON.parse(templateData.initEdgeWeight)];
        initEdgeWeightTable += `<table class="initEdgeWeightTable">`;

        initEdgeWeightTable += `<tr>`;

        for(let i = 0; i < initEdgeWeight.length + 1; i++)
        {
            if(i === 0)
                initEdgeWeightTable += `<td> </td>`;
            else
                initEdgeWeightTable += `<td class="initialEdgeWeightBg">${i}</td>`;
        }

        initEdgeWeightTable += `</tr>`;

        for(let i = 0; i < initEdgeWeight.length; i++)
        {
            initEdgeWeightTable += `<tr>`;

            for(let j = 0; j < initEdgeWeight[i].length; j++)
            {
                if(j === 0)
                    initEdgeWeightTable += `<td class="initialEdgeWeightBg">${i + 1}</td>`;

                initEdgeWeightTable += `<td>${initEdgeWeight[i][j]}</td>`;
            }

            initEdgeWeightTable += `</tr>`;
        }

        initEdgeWeightTable += `</table>`;
    }

    let countInvalidNodesValue = 0;
    let countSelectedEdges = templateData.selectedEdges.length;

    if(templateData.nodesValue)
    {
        for(let i=0, l = templateData.nodesValue.length; i < l; i++){
            countInvalidNodesValue += (templateData.nodesValue[i] === null) ? 1 : 0;
        }
    }

    if (isNumber(templateData.currentEdge[0]) && isNumber(templateData.currentEdge[1]))
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
                <input value="${templateData.currentWijZero}" id="wijZero" placeholder="Введите число" ${templateData.currentEdge.length !== 2 ? "disabled" : ""} class="tableInputData" type="number"/>
            </td>
            <td>
                <input value="${templateData.currentDeltaWijZero}" id="deltaWijZero" placeholder="Введите число" ${templateData.currentEdge.length !== 2 ? "disabled" : ""} class="tableInputData" type="number"/>
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
        </tr>`;
    }

    for(let i = 0; i < templateData.neuronsTableData.length; i++)
    {
        let currentNodeSection = [templateData.neuronsTableData[i].nodeSection].toString().replaceAll("n", "");
        if (currentNodeSection.length === 0)
            currentNodeSection = "-";

        tableData += `<tr>
            <td>
                ${templateData.neuronsTableData[i].nodeId.substring(1)}
            </td>
            <td>
                ${currentNodeSection}
            </td>
            <td>
                ${templateData.neuronsTableData[i].neuronInputSignalValue}
            </td>
            <td>
                ${templateData.neuronsTableData[i].neuronOutputSignalValue}
            </td>
        </tr>`;
    }

    let currentNeuronInputSignalValue = `<input id="currentNeuronInputSignalValue" placeholder="Введите число" class="tableInputData" type="number" value="${templateData.currentNeuronInputSignalValue}"/>`;
    let currentNeuronOutputSignalValue = `<input id="currentNeuronOutputSignalValue" placeholder="Введите число" class="tableInputData" type="number" value="${templateData.currentNeuronOutputSignalValue}"/>`;

    if(templateData.currentStep !== templateData.inputNeuronsAmount + templateData.amountOfHiddenLayers * templateData.amountOfNodesInHiddenLayer + templateData.outputNeuronsAmount)
    {
        let currentNodeSection = [...templateData.currentNodeSection];
        for(let i = 0; i < currentNodeSection.length; i++)
        {
            currentNodeSection[i] = currentNodeSection[i].substring(1);
        }

        if (currentNodeSection.length === 0)
            currentNodeSection = "-";

        tableData += `<tr>
            <td>
                ${templateData.currentSelectedNodeId ? templateData.currentSelectedNodeId.substring(1) : ""}
            </td>
            <td>
                ${currentNodeSection}
            </td>
            <td>
                ${currentNeuronInputSignalValue}
            </td>
            <td>
                ${currentNeuronOutputSignalValue}
            </td>
        </tr>`;
    }

    let backpropagationContainer = "";

    if(templateData.isZeroForwardPropagationDone === true)
    {
        backpropagationContainer = `<div class="backpropagationContainer">
            <div class="steps-buttons-backpropagation">
                <input id="addStepBackpropagation" class="addStepBackpropagation btn btn-success" type="button"
                       value="+"/>
                <input type="button" class="minusStepBackpropagation btn btn-danger" value="-">
            </div>
            <table class="backpropagation steps-table">
                <tr>
                    <th>(X<sub>i</sub>, X<sub>j</sub>)</th>
                    <th>W<sub>ij</sub><sup>0</sup></th>
                    <th>&Delta;W<sub>ij</sub><sup>0</sup></th>
                    <th>&delta;(X<sub>j</sub>)</th>
                    <th>p<sup>1</sup>(W<sub>i</sub><sub>j</sub>)</th>
                    <th>&Delta;W<sub>ij</sub><sup>1</sup></th>
                    <th>W<sub>ij</sub><sup>1</sup></th>
                </tr>
                ${backPropagationData}
            </table>                 
            <div class="next-table-step-buttons">
                <input id="createFirstForwardPropagationTable" class="btn btn-info" type="button" value="Далее"/>
                <input id="cancelBackpropagationTable" class="btn btn-secondary" type="button" value="Назад"/>
            </div>
        </div>`;
    }

    let firstForwardPropagationContainer = "";

    if(templateData.isBackpropagationDone === true && templateData.isZeroForwardPropagationDone === true)
    {
        let firstForwardPropagationTableData = "";

        for(let i = 0; i < templateData.firstPropagationNeuronsTableData.length; i++)
        {
            let currentNodeSection = [templateData.firstPropagationNeuronsTableData[i].nodeSection].toString().replaceAll("n", "");
            if (currentNodeSection.length === 0)
                currentNodeSection = "-";

            firstForwardPropagationTableData += `<tr>
                <td>
                    ${templateData.firstPropagationNeuronsTableData[i].nodeId.substring(1)}
                </td>
                <td>
                    ${currentNodeSection}
                </td>
                <td>
                    ${templateData.firstPropagationNeuronsTableData[i].neuronInputSignalValue}
                </td>
                <td>
                    ${templateData.firstPropagationNeuronsTableData[i].neuronOutputSignalValue}
                </td>
            </tr>`;
        }

        if(templateData.currentFirstPropagationStep !== templateData.inputNeuronsAmount + templateData.amountOfHiddenLayers * templateData.amountOfNodesInHiddenLayer + templateData.outputNeuronsAmount)
        {
            let currentNodeSection = [...templateData.currentNodeSection];
            for(let i = 0; i < currentNodeSection.length; i++)
            {
                currentNodeSection[i] = currentNodeSection[i].substring(1);
            }

            if (currentNodeSection.length === 0)
                currentNodeSection = "-";

            firstForwardPropagationTableData += `<tr>
                <td>
                    ${templateData.currentSelectedNodeId ? templateData.currentSelectedNodeId.substring(1) : ""}
                </td>
                <td>
                    ${currentNodeSection}
                </td>
                <td>
                    ${currentNeuronInputSignalValue}
                </td>
                <td>
                    ${currentNeuronOutputSignalValue}
                </td>
            </tr>`;
        }

        firstForwardPropagationContainer = `<div class="firstForwardContainer">
            <p>k = 1</p>
            <div class="steps-buttons">
                <input id="addStepFirstForwardPropagation" class="addStepFirstForwardPropagation addStep btn btn-success" type="button" value="+"/>
                <input id="minusStepFirstForwardPropagation" type="button" class="minusStepFirstForwardPropagation btn btn-danger" value="-">
            </div>
            <table class="steps-table">
                <tr>
                    <th>X</th>
                    <th>Прообразы X</th>
                    <th>input(X)</th>
                    <th>output(X)</th>
                </tr>
                ${firstForwardPropagationTableData}
            </table>
            <div class="maxFlow">
                <span>E<sup>1</sup>(w):</span>
                <input type='number' ${countInvalidNodesValue !== 0 || templateData.isFirstForwardPropagationDone === true ? "disabled" : ""} class='maxFlow-input' id="error" value="${templateData.error}"/>
            </div>  
            <div class="next-table-step-buttons">
                <input id="cancelFirstPropagationTable" class="btn btn-primary" type="button" value="Назад"/>
            </div>
        </div>`;
    }

    let zeroForwardPropagationContainer = `<div class="zeroForwardPropagationContainer">
        <p>k = 0</p>
        <div class="steps-buttons">
            <input id="addStep" class="addStep btn btn-success" type="button" value="+"/>
            <input type="button" class="minusStep btn btn-danger" value="-">
        </div>
        <table class="steps-table">
            <tr>
                <th>X</th>
                <th>Прообразы X</th>                                   
                <th>input(X)</th>
                <th>output(X)</th>
            </tr>
            ${tableData}
        </table>
        <div class="maxFlow">
            <span>E<sup>0</sup>(w):</span>
            <input type='number' ${countInvalidNodesValue !== 0 || templateData.isZeroForwardPropagationDone === true ? "disabled" : ""} class='maxFlow-input' id="errorZero" value="${templateData.errorZero}"/>
        </div>
        <div class="next-table-step-buttons">
            <input id="createBackpropagationTable" class="btn btn-info" type="button" value="Далее"/>
        </div>
    </div>`;

    return `
        <div class="lab">
            <div class="lab-table">
                <div class="lab-header_text">Алгоритм обратного распространения ошибки в перцептроне</div>
                <div class="header-buttons">
                    <button type="button" class="btn btn-info redrawGraph">Перерисовать граф</button>
                    <button type="button" class="btn btn-info showReference" data-toggle="modal" data-target="#exampleModalScrollable">Справка</button>
                </div>
                <div id="newGraph"></div>
                <div class="steps">
                    ${templateData.isZeroForwardPropagationDone === false && templateData.isBackpropagationDone === false && templateData.isFirstForwardPropagationDone === false ? zeroForwardPropagationContainer: ""}
                    ${templateData.isZeroForwardPropagationDone === true && templateData.isFirstForwardPropagationDone === false && templateData.isBackpropagationDone === false ? backpropagationContainer: ""}
                    ${templateData.isZeroForwardPropagationDone === true && templateData.isBackpropagationDone === true ? firstForwardPropagationContainer: ""}
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
                                <p>Заполните таблицу с входными и выходными сигналами нейронов в заданной НС по аналогии с предыдущей задачей. Посчитайте оценку решения и внесите ее в соответствующую ячейку. После этого нажмите кнопку <b>«Далее»</b> и перейдите к заполнению таблицы с результатами обучения НС на шаге k=1.</p>
                                <p>Кликните на рисунке на очередную дугу, она выделится <b>красным цветом</b>. Заполните соответствующую ей строку. Для перехода к следующей строке нажмите кнопку «+», после этого обработанная алгоритмом дуга станет на рисунке <b>зелёной</b>. Для отмены текущей строки в таблице — кнопку «-». После того, как таблица будет заполнена полностью, нажмите кнопку <b>«Далее»</b>. Если нужно отменить заполненную таблицу и вернуться к предыдущей таблице, нажмите кнопук <b>«Назад»</b>.</p>                                
                                <p>Заполните таблицу с входными и выходными сигналами нейронов в заданной НС по аналогии с предыдущей задачей. Посчитайте оценку решения после обучения и внесите ее в соответствующую ячейку. Нажмите кнопку в правом нижнем углу стенда <b>«Ответ готов»</b>.</p>
                                <p>Если ячейки не кликаются для заполнения, то необходимо <b>свернуть и затем развернуть окно</b>.</p>
                                <p>Округление E<sup>n</sup>(w) происходит <b>до 4-х знаков</b> после запятой.</p>
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
        neuronsTableData: [],
        firstPropagationNeuronsTableData: [],
        edgesTableData: [],
        currentNodeSection: [],
        currentSelectedNodeId: "",
        currentNeuronInputSignalValue: "",
        currentNeuronOutputSignalValue: "",
        prevSelectedNodeId: "",
        prevNeuronInputSignalValue: "",
        prevNeuronOutputSignalValue: "",
        prevNodeSection: [],
        error: 0,
        errorZero: 0,
        isSelectingNodesModeActivated: false,
        currentStep: 0,
        currentFirstPropagationStep: 0,
        currentEdgeStep: 0,
        isBackpropagationDone: false,
        isZeroForwardPropagationDone: false,
        isFirstForwardPropagationDone: false,
        currentEdge: [],
        selectedEdges: [],
        currentWijZero: null,
        currentDeltaWijZero: null,
        currentDelta: null,
        currentDeltaW: null,
        currentNewW: null,
        currentGrad: null,
        newNodesValue: [],
        initialNodesValue: [],
        initNodesValue: [],
        initEdgeWeight: "",
    };

    return {
        getState: function () {
            return _state
        },
        updateState: function (callback) {
            _state = callback(_state);
            setCookie('state', JSON.stringify({..._state}));
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
    let grad = [];

    for (let i = 0; i < clientAnswer.length; i++)
    {
        delta.push(0);
        newW.push([]);
        deltaW.push([]);
        wijZero.push([]);
        deltaWijZero.push([]);
        grad.push([]);

        for (let j = 0; j < clientAnswer.length; j++)
        {
            newW[i].push(0);
            deltaW[i].push(0);
            wijZero[i].push(0);
            deltaWijZero[i].push(0);
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
        deltaWijZero[nodeFromIndex][nodeToIndex] = clientAnswer[i].deltaWijZero;
        wijZero[nodeFromIndex][nodeToIndex] = clientAnswer[i].wijZero;
        grad[nodeFromIndex][nodeToIndex] = clientAnswer[i].grad;
    }

    return {
        newW,
        delta,
        deltaW,
        deltaWijZero,
        wijZero,
        grad
    };
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
    if(document.getElementById("cancelFirstPropagationTable"))
    {
        document.getElementById("cancelFirstPropagationTable").addEventListener('click', () => {
            // обновляем стейт приложение
            const state = appInstance.state.updateState((state) => {
                let selectedEdges = [];
                let edgesTableData = state.edgesTableData.slice();
                let neuronsTableData = state.neuronsTableData.slice();
                let nodesValue = state.initNodesValue.slice();
                let edgeWeight = [...JSON.parse(state.initEdgeWeight)];

                for (let i = 0; i < edgesTableData.length; i++)
                {
                    selectedEdges.push("w" + edgesTableData[i].edge[0].toString() + edgesTableData[i].edge[1].toString());
                }

                for(let i = 0; i < neuronsTableData.length; i++)
                {
                    let currentNodeId = neuronsTableData[i].nodeId;
                    let currentNodeIndex = +currentNodeId.replace("n", "");

                    nodesValue[currentNodeIndex] = neuronsTableData[i].neuronOutputSignalValue;
                }

                return {
                    ...state,
                    nodesValue,
                    isBackpropagationDone: false,
                    isFirstForwardPropagationDone: false,
                    firstPropagationNeuronsTableData: [],
                    error: 0,
                    currentEdge: [],
                    selectedEdges,
                    currentFirstPropagationStep: 0,
                    edgeWeight,
                }
            });

            // перересовываем приложение
            appInstance.subscriber.emit('render', state);
        });
    }


    if(document.getElementById("cancelBackpropagationTable"))
    {
        document.getElementById("cancelBackpropagationTable").addEventListener('click', () => {
            // обновляем стейт приложение
            const state = appInstance.state.updateState((state) => {
                let neuronsTableData = state.neuronsTableData.slice();
                let nodesValue = state.initNodesValue.slice();

                for(let i = 0; i < neuronsTableData.length; i++)
                {
                    let currentNodeId = neuronsTableData[i].nodeId;
                    let currentNodeIndex = +currentNodeId.replace("n", "");

                    nodesValue[currentNodeIndex] = neuronsTableData[i].neuronOutputSignalValue;
                }

                return  {
                    ...state,
                    nodesValue,
                    isBackpropagationDone: false,
                    isZeroForwardPropagationDone: false,
                    edgesTableData: [],
                    error: 0,
                    currentEdge: [],
                    selectedEdges: [],
                    currentEdgeStep: 0,
                    edgeWeight: [...JSON.parse(state.initEdgeWeight)],
                }
            });

            // перересовываем приложение
            appInstance.subscriber.emit('render', state);
        });
    }

    if(document.getElementById("createFirstForwardPropagationTable"))
    {
        document.getElementById("createFirstForwardPropagationTable").addEventListener('click', () => {
            // обновляем стейт приложение
            const state = appInstance.state.updateState((state) => {
                if(state.edgesTableData.length === state.edgesAmount)
                {
                    let nodesValue = state.initNodesValue.slice();
                    let edgesTableData = state.edgesTableData.slice();
                    let edgeWeight = [...state.edgeWeight];

                    for(let i = 0; i < edgesTableData.length; i++)
                    {
                        let neuronFromIndex = edgesTableData[i].edge[0];
                        let neuronToIndex = edgesTableData[i].edge[1];
                        edgeWeight[neuronFromIndex][neuronToIndex] = edgesTableData[i].newW;
                    }

                    return  {
                        ...state,
                        nodesValue,
                        selectedEdges: [],
                        currentEdge: [],
                        isBackpropagationDone: true,
                    }
                }
                else
                {
                    alert("Сначала заполните всю таблицу МОР!");
                    return {
                        ...state,
                    }
                }
            });

            // перересовываем приложение
            appInstance.subscriber.emit('render', state);
        });
    }

    if(document.getElementById("errorZero"))
    {
        document.getElementById("errorZero").addEventListener('change', () => {
            const state = appInstance.state.updateState((state) => {

                if(isNaN(document.getElementById("errorZero").value))
                {
                    return {
                        ...state,
                        errorZero: 0,
                    }
                }

                return {
                    ...state,
                    errorZero: Number(document.getElementById("errorZero").value),
                }
            });

            appInstance.subscriber.emit('render', state);
        });
    }

    if(document.getElementById("createBackpropagationTable"))
    {
        document.getElementById("createBackpropagationTable").addEventListener('click', () => {
            // обновляем стейт приложение
            let zeroForwardPropagationMSE = +document.getElementById("errorZero").value;

            const state = appInstance.state.updateState((state) => {
                if(state.nodesValue.length === state.neuronsTableData.length && !Number.isNaN(zeroForwardPropagationMSE))
                {
                    return  {
                        ...state,
                        errorZero: zeroForwardPropagationMSE,
                        isZeroForwardPropagationDone: true,
                    }
                }
                else
                {
                    alert("Сначала заполните всю таблицу и рассчитайте MSE!");
                }

                return {
                    ...state,
                }
            });

            // перересовываем приложение
            appInstance.subscriber.emit('render', state);
        });
    }

    if(document.getElementById("addStepFirstForwardPropagation"))
    {
        document.getElementById("addStepFirstForwardPropagation").addEventListener('click', () => {
            // обновляем стейт приложение
            const state = appInstance.state.updateState((state) => {
                let currentFirstPropagationStep = state.currentFirstPropagationStep;
                let firstPropagationNeuronsTableData = state.firstPropagationNeuronsTableData.slice();
                let nodesValue = state.nodesValue.slice();
                let currentSelectedNodeIdNumber = state.currentSelectedNodeId.match(/(\d+)/)[0];
                let currentNeuronInputSignalValue = state.currentNeuronInputSignalValue;
                let currentNeuronOutputSignalValue = state.currentNeuronOutputSignalValue;

                let prevSelectedNodeId = state.currentSelectedNodeId;
                let prevNeuronInputSignalValue = state.currentNeuronInputSignalValue;
                let prevNeuronOutputSignalValue = state.currentNeuronOutputSignalValue;
                let prevNodeSection = state.currentNodeSection.slice();

                if(currentNeuronInputSignalValue === "")
                    currentNeuronInputSignalValue = 0;
                if(currentNeuronOutputSignalValue === "")
                    currentNeuronOutputSignalValue = 0;

                if(firstPropagationNeuronsTableData.length < state.inputNeuronsAmount && !isNaN(currentNeuronInputSignalValue) && !isNaN(currentNeuronOutputSignalValue))
                {
                    nodesValue[currentSelectedNodeIdNumber] = currentNeuronOutputSignalValue;
                    currentFirstPropagationStep++;
                    firstPropagationNeuronsTableData.push({
                        nodeId: state.currentSelectedNodeId,
                        neuronInputSignalValue: currentNeuronInputSignalValue,
                        neuronOutputSignalValue: currentNeuronOutputSignalValue,
                        nodeSection: [],
                    });
                }
                else if(state.currentSelectedNodeId.length > 0 && !isNaN(currentNeuronInputSignalValue) && !isNaN(currentNeuronOutputSignalValue)
                    && state.currentNodeSection.length > 0)
                {
                    nodesValue[currentSelectedNodeIdNumber] = currentNeuronOutputSignalValue;
                    currentFirstPropagationStep++;
                    firstPropagationNeuronsTableData.push({
                        nodeId: state.currentSelectedNodeId,
                        neuronInputSignalValue: currentNeuronInputSignalValue,
                        neuronOutputSignalValue: currentNeuronOutputSignalValue,
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
                    currentFirstPropagationStep,
                    firstPropagationNeuronsTableData,
                    nodesValue,
                    prevSelectedNodeId,
                    prevNeuronInputSignalValue,
                    prevNeuronOutputSignalValue,
                    prevNodeSection,
                    currentSelectedNodeId: "",
                    currentNeuronInputSignalValue: "",
                    currentNeuronOutputSignalValue: "",
                    currentNodeSection: [],
                    isSelectingNodesModeActivated: false,
                }
            });

            // перересовываем приложение
            appInstance.subscriber.emit('render', state);
        });
    }

    if(document.getElementById("minusStepFirstForwardPropagation"))
    {
        document.getElementById("minusStepFirstForwardPropagation").addEventListener('click', () => {
            // обновляем стейт приложение
            const state = appInstance.state.updateState((state) => {
                if(state.currentFirstPropagationStep > 0)
                {
                    let firstPropagationNeuronsTableData = state.firstPropagationNeuronsTableData.slice();
                    let currentSelectedNodeIdNumber = Number(firstPropagationNeuronsTableData[firstPropagationNeuronsTableData.length - 1].nodeId.match(/(\d+)/)[0]);
                    let currentNodeSection = firstPropagationNeuronsTableData[firstPropagationNeuronsTableData.length - 1].nodeSection;
                    let currentNeuronInputSignalValue = firstPropagationNeuronsTableData[firstPropagationNeuronsTableData.length - 1].neuronInputSignalValue;
                    let currentNeuronOutputSignalValue = firstPropagationNeuronsTableData[firstPropagationNeuronsTableData.length - 1].neuronOutputSignalValue;
                    let currentSelectedNodeId = firstPropagationNeuronsTableData[firstPropagationNeuronsTableData.length - 1].nodeId;

                    firstPropagationNeuronsTableData.pop();
                    let nodesValueCopy = state.nodesValue.slice();
                    nodesValueCopy[currentSelectedNodeIdNumber] = null;
                    let prevNeuronInputSignalValue = state.currentNeuronInputSignalValue;
                    let prevNeuronOutputSignalValue = state.currentNeuronOutputSignalValue;
                    let prevNodeSection = state.currentNodeSection;

                    return  {
                        ...state,
                        firstPropagationNeuronsTableData,
                        prevNeuronInputSignalValue,
                        prevNeuronOutputSignalValue,
                        currentFirstPropagationStep: state.currentFirstPropagationStep - 1,
                        currentSelectedNodeId,
                        currentNeuronInputSignalValue,
                        currentNeuronOutputSignalValue,
                        currentNodeSection,
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

    if(document.getElementById("addStep"))
    {
        document.getElementById("addStep").addEventListener('click', () => {
            // обновляем стейт приложение
            const state = appInstance.state.updateState((state) => {
                if(state.currentSelectedNodeId && !isNaN(state.currentNeuronInputSignalValue) && !isNaN(state.currentNeuronOutputSignalValue))
                {
                    let currentStep = state.currentStep;
                    let neuronsTableData = state.neuronsTableData.slice();
                    let nodesValue = state.nodesValue.slice();
                    let currentSelectedNodeIdNumber = state.currentSelectedNodeId.match(/(\d+)/)[0];
                    let currentNeuronInputSignalValue = state.currentNeuronInputSignalValue;
                    let currentNeuronOutputSignalValue = state.currentNeuronOutputSignalValue;

                    let prevSelectedNodeId = state.currentSelectedNodeId;
                    let prevNeuronInputSignalValue = state.currentNeuronInputSignalValue;
                    let prevNeuronOutputSignalValue = state.currentNeuronOutputSignalValue;
                    let prevNodeSection = state.currentNodeSection.slice();

                    if(currentNeuronInputSignalValue === "")
                        currentNeuronInputSignalValue = 0;
                    if(currentNeuronOutputSignalValue === "")
                        currentNeuronOutputSignalValue = 0;

                    if(neuronsTableData.length < state.inputNeuronsAmount && !isNaN(currentNeuronInputSignalValue) && !isNaN(currentNeuronOutputSignalValue))
                    {
                        nodesValue[currentSelectedNodeIdNumber] = currentNeuronOutputSignalValue;
                        currentStep++;
                        neuronsTableData.push({
                            nodeId: state.currentSelectedNodeId,
                            neuronInputSignalValue: currentNeuronInputSignalValue,
                            neuronOutputSignalValue: currentNeuronOutputSignalValue,
                            nodeSection: [],
                        });
                    }
                    else if(state.currentSelectedNodeId.length > 0 && !isNaN(currentNeuronInputSignalValue) && !isNaN(currentNeuronOutputSignalValue)
                        && state.currentNodeSection.length > 0)
                    {
                        nodesValue[currentSelectedNodeIdNumber] = currentNeuronOutputSignalValue;
                        currentStep++;
                        neuronsTableData.push({
                            nodeId: state.currentSelectedNodeId,
                            neuronInputSignalValue: currentNeuronInputSignalValue,
                            neuronOutputSignalValue: currentNeuronOutputSignalValue,
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
                        prevNeuronInputSignalValue,
                        prevNeuronOutputSignalValue,
                        prevNodeSection,
                        currentSelectedNodeId: "",
                        currentNeuronInputSignalValue: "",
                        currentNeuronOutputSignalValue: "",
                        currentNodeSection: [],
                        isSelectingNodesModeActivated: false,
                    }
                }
                else {
                    alert("Сначала заполните все поля в строке таблицы!")
                    return {
                        ...state,
                    }
                }
            });

            // перересовываем приложение
            appInstance.subscriber.emit('render', state);
        });
    }

    if(document.getElementsByClassName("minusStep")[0])
    {
        document.getElementsByClassName("minusStep")[0].addEventListener('click', () => {
            // обновляем стейт приложение
            const state = appInstance.state.updateState((state) => {
                if(state.currentStep > 0)
                {
                    let neuronsTableData = state.neuronsTableData.slice();
                    let currentSelectedNodeIdNumber = Number(neuronsTableData[neuronsTableData.length - 1].nodeId.match(/(\d+)/)[0]);
                    let currentNodeSection = neuronsTableData[neuronsTableData.length - 1].nodeSection;
                    let currentNeuronInputSignalValue = neuronsTableData[neuronsTableData.length - 1].neuronInputSignalValue;
                    let currentNeuronOutputSignalValue = neuronsTableData[neuronsTableData.length - 1].neuronOutputSignalValue;
                    let currentSelectedNodeId = neuronsTableData[neuronsTableData.length - 1].nodeId;

                    neuronsTableData.pop();
                    let nodesValueCopy = state.nodesValue.slice();
                    nodesValueCopy[currentSelectedNodeIdNumber] = null;
                    let prevNeuronInputSignalValue = state.currentNeuronInputSignalValue;
                    let prevNeuronOutputSignalValue = state.currentNeuronOutputSignalValue;
                    let prevNodeSection = state.currentNodeSection;

                    return  {
                        ...state,
                        neuronsTableData,
                        prevNeuronInputSignalValue,
                        prevNeuronOutputSignalValue,
                        currentStep: state.currentStep - 1,
                        currentSelectedNodeId,
                        currentNeuronInputSignalValue,
                        currentNeuronOutputSignalValue,
                        currentNodeSection,
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

    if(document.getElementById("error"))
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
    }

    if(document.getElementById("addStepBackpropagation"))
    {
        document.getElementById("addStepBackpropagation").addEventListener('click', () => {
            const state = appInstance.state.updateState((state) => {
                let currentEdgeStep = state.currentEdgeStep;
                let edgesTableData = state.edgesTableData.slice();
                let nodesValue = state.nodesValue.slice();

                let prevDelta = state.currentDelta;
                let prevWijZero = state.currentWijZero;
                let prevDeltaWijZero = state.currentDeltaWijZero;
                let prevDeltaW = state.currentDeltaW ;
                let prevNewW = state.currentNewW;
                let prevGrad = state.currentGrad;
                let prevEdge = state.currentEdge.slice();
                let selectedEdges = state.selectedEdges.slice();
                let isBackpropagationDone = false;
                let newNodesValue = state.newNodesValue;

                selectedEdges.push("w" + state.currentEdge[0] + state.currentEdge[1]);

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
                        grad: Number(document.getElementById("grad").value),
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
                    newNodesValue,
                    nodesValue,
                    isBackpropagationDone,
                    currentEdgeStep,
                    edgesTableData,
                    prevDelta,
                    prevWijZero,
                    prevGrad,
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
                    currentGrad: "",
                    selectedEdges,
                }
            });

            // перересовываем приложение
            appInstance.subscriber.emit('render', state);
        });
    }

    if(document.getElementsByClassName("minusStepBackpropagation")[0])
    {
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
                    let prevGrad = edgesTableData[edgesTableData.length - 1].grad;
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
                        currentGrad: prevGrad,
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

    if(document.getElementById("currentNeuronOutputSignalValue"))
    {
        document.getElementById("currentNeuronOutputSignalValue").addEventListener('change', () => {
            const state = appInstance.state.updateState((state) => {
                return {
                    ...state,
                    currentNeuronOutputSignalValue: Number(document.getElementById("currentNeuronOutputSignalValue").value),
                }
            });

            appInstance.subscriber.emit('render', state);
        });
    }

    if(document.getElementById("currentNeuronInputSignalValue"))
    {
        document.getElementById("currentNeuronInputSignalValue").addEventListener('change', () => {
            const state = appInstance.state.updateState((state) => {
                return {
                    ...state,
                    currentNeuronInputSignalValue: Number(document.getElementById("currentNeuronInputSignalValue").value),
                }
            });

            appInstance.subscriber.emit('render', state);
        });
    }
}

function dataToNewGraph(state)
{
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
            let nodeValue = nodesValue[i] !== null ? `(${nodesValue[i]})` : "";
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
                data: {
                    id: nodeId,
                    label: `${i.toString()} ${nodeValue}`,
                    weight: nodeValue,
                    color: nodeColor,
                    // size: 4,
                },
                renderedPosition: {x: nodesLevel[i] * 100, y: yLevel * 100},
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
                        data: {
                            id: "w" + i + j,
                            source: "n" + i,
                            target: "n" + j,
                            label: edgeWeight[i][j].toString(),
                            color: edgeColor,
                            // size: 200,
                        }
                    });
                }
            }
        }
    }

    return {
        nodes: [...resultNodes],
        edges: [...resultEdges],
    };
}

function renderGraph(state, appInstance)
{
    const elementsData = dataToNewGraph(state);
    console.log('elementsData', elementsData);
    let cy = cytoscape({
        container: document.getElementById('newGraph'), // container to render in
        elements: elementsData,
        style: [ // the stylesheet for the graph
            {
                selector: 'node',
                style: {
                    'background-color': 'data(color)',
                    'label': 'data(label)',
                    'color': '#000',
                }
            },

            {
                selector: 'edge',
                style: {
                    'width': 1,
                    'line-color': 'data(color)',
                    'curve-style': 'bezier',
                    'label': 'data(label)'
                }
            }
        ],
        layout: {
            name: 'preset',
        },
    });

    cy.nodes().on('click', function(e){
        let ele = e.target;
        console.log('clicked ' + ele.id());

        const state = appInstance.state.updateState((state) => {
            if(state.isZeroForwardPropagationDone === false && state.isBackpropagationDone === false ||
                state.isFirstForwardPropagationDone === false && state.isBackpropagationDone === true)
            {
                if(state.isSelectingNodesModeActivated)
                {
                    let currentNodeSectionCopy = [...state.currentNodeSection];
                    let isNodeInList = false;

                    currentNodeSectionCopy.map((nodeId,index)=> {
                        if(nodeId === ele.id())
                        {
                            currentNodeSectionCopy.splice(index, 1);
                            isNodeInList = true;
                            return;
                        }
                    });

                    if(!isNodeInList && ele.id() !== state.currentSelectedNodeId)
                    {
                        currentNodeSectionCopy.push(ele.id());
                    }
                    else if (ele.id() === state.currentSelectedNodeId)
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
                    if(state.currentSelectedNodeId === ele.id())
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
                            currentSelectedNodeId: ele.id(),
                            isSelectingNodesModeActivated: true,
                        }
                    }
                }
            }
            else
            {
                // alert("Сначала найдите все новые веса рёбер");

                return {
                    ...state,
                }
            }

        });

        appInstance.subscriber.emit('render', state);
    });

    cy.edges().on('click', function(e){
        let ele = e.target;
        console.log('clicked ' + ele.id());
        console.log(ele);

        const state = appInstance.state.updateState((state) => {
            if(state.isBackpropagationDone === false && state.isZeroForwardPropagationDone === true && state.nodesValue.length === state.neuronsTableData.length)
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
                        currentEdge: [Number(ele.data('source').match(/(\d+)/)[0]), Number(ele.data('target').match(/(\d+)/)[0])]
                    }
                }
            }
            else
            {
                alert("На данном шаге изменение дуг не доступно!");
            }

            return {
                ...state,
            }
        });

        appInstance.subscriber.emit('render', state);
    });
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
                    let yLevelRandomDisplacement = nodes.map(node => {
                        return 2 + Math.random() * 3; //смещение ноты по Y из-за того, что не видно значение ребра при отрисовке
                    });

                    let initNodesValue = graph.nodesValue.slice();
                    //просто скопировать массив не получается. меняется этот массив когда делаем изменения edgeWeight массива. ссылки разные, копирую через спред блять. маме создателя JS привет сука
                    let initEdgeWeight = JSON.stringify(graph.edgeWeight.slice());
                    initNodesValue.fill(null);
                    graph.nodesValue = initNodesValue.slice();

                    let initState = {
                        ...state,
                        ...graph,
                        yLevelRandomDisplacement,
                        initNodesValue,
                        initEdgeWeight,
                    }

                    let cookieState = getCookie('state');
                    console.log('cookieState', cookieState);

                    if(cookieState && cookieState.length)
                    {
                        cookieState = JSON.parse(cookieState);
                        let cookieEdgeWeight = JSON.stringify([...cookieState.edgeWeight]);

                        console.log(cookieEdgeWeight);
                        console.log(initEdgeWeight);

                        if(cookieEdgeWeight !== initEdgeWeight)
                        {
                            console.log('cookeDeleted');
                            deleteCookie('state');
                        }
                        else
                        {
                            console.log('got old cookie!');
                            initState = {...JSON.parse(getCookie('state'))};
                        }
                    }

                    return {...initState};
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
                renderGraph(state, appInstance);
                bindActionListeners(appInstance);
            };

            appInstance.subscriber.subscribe('render', render);

            // инициализируем первую отрисовку
            appInstance.subscriber.emit('render', appInstance.state.getState());
        },
        getCondition: function () {
        },
        getResults: function () {
            //todo чёто ругается на объект этот
            let result = {...appInstance.state.getState()};
            let normObject = {
                error: result.error,
                errorZero: result.errorZero,
                neuronsTableData: result.neuronsTableData,
                edgesTableData: result.edgesTableData,
                firstPropagationNeuronsTableData: result.firstPropagationNeuronsTableData,
            };

            let resultStr = JSON.stringify(normObject);
            resultStr = resultStr.replaceAll("&#0045;", "-");

            console.log('getResults', normObject);
            console.log('resultStr', resultStr);
            deleteCookie('state');
            return resultStr;
        },
        calculateHandler: function (text, code) {
        },
    }
}

var Vlab = init_lab();