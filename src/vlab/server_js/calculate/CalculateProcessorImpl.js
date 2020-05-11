function calculate(condition, instructions, generatingResult){
    var calculatingResult = Java.type('rlcp.calculate.CalculatingResult');
    //do calculate logic here
    print(condition);

    print(instructions);

    print(generatingResult['text']);
    print(generatingResult['code']);
    print(generatingResult['instructions']);


    var result = new calculatingResult("text", "code");
    return result;
}