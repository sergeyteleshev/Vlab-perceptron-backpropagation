function checkSingleCondition(condition, instructions, generatingResult){
    var checkingSingleConditionResult = Java.type('rlcp.server.processor.check.CheckProcessor.CheckingSingleConditionResult');
    var bigDecimal = java.math.BigDecimal;

    print(condition['id']);
    print(condition['time']);
    print(condition['input']);
    print(condition['output']);

    print(instructions);

    print(generatingResult['text']);
    print(generatingResult['code']);
    print(generatingResult['instructions']);

    var result = new checkingSingleConditionResult(new bigDecimal(1.0), "ok");
    return result;
}

function setPreCheckResult(preCheckResult){}