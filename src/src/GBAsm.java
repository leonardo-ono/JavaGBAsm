
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Very simple Java GameBoy assembler (CPU‎: ‎Sharp LR35902)
 * 
 * 16/jan/2020 - can generate only 32Kb 'ROM ONLY' (00h) Cartridge Type 
 *             - can't evaluate expressions
 *             - labels must have at least 3 bytes
 *             - support non local + local labels (similar to nasm)
 *             - supported pseudo instructions: org, equ & db
 *             - decimal literal numbers: 127, 10
 *             - hex literal numbers: $ab, 0abh
 *             - binary literal numbers: %10010001, 10010001b
 * 
 * @author Leonardo Ono (ono.leo@gmail.com)
 */
public class GBAsm {

    public static Map<String, String> conversionTable = new HashMap<>();
    public static List<String> validInstructions = new ArrayList<>();

    private static String lineLabel = null;
    private static String parentLabel = null;
    private static Map<String, Integer> labels = new HashMap<>();
    private static Map<Integer, String> physicalAddressToFix = new HashMap<>();
    private static Map<Integer, String> relativeAddressToFix = new HashMap<>();

    private static int lineNumber = 1;
    private static String currentLine;
    
    private static String[] tokens;
    private static int tokenIndex;

    private static int address = 0;
    private static int[] machineCode = new int[0x8000];
    
    public static void main(String[] args) throws Exception {
        loadInstructions();
        String inFile = null;
        String outFile = null;
        try {
            inFile = args[0];
            outFile = args[1];
        }
        catch (Exception e) {
            System.out.println("jgbasm - v0.01 [requires java 8+]");
            System.out.println("2020 (c) Leonardo Ono (ono.leo@gmail.com)");
            System.out.println("");
            System.out.println("usage: java -jar jgbasm.jar in.asm out.gb");
            System.out.println("");
            System.exit(0);
        }
        assemble(inFile);
        writeOutBinFile(outFile);

        System.out.println("");
        System.out.println("sucess!");
    }

    public static void loadInstructions() throws Exception {
        InputStream in = GBAsm.class.getResourceAsStream("instr.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        while ((line = br.readLine()) != null) {
            String[] instrInfo = line.split("\\:");
            String mnemonic = instrInfo[0];
            String opcode = instrInfo[1];
            conversionTable.put(mnemonic, opcode);
            String instr = mnemonic.split(" ")[0];
            if (!validInstructions.contains(instr)) {
                validInstructions.add(instr);
            }
        }
        br.close();
    }

    private static void assemble(String source) throws Exception {
        InputStream in = new FileInputStream(source);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        currentLine = null;
        outer:
        while ((currentLine = br.readLine()) != null) {
            currentLine = removeComment(currentLine).trim();
            if (!currentLine.isEmpty()) {
                debug(lineNumber + ": " + currentLine);
                setTokens(currentLine.split(
                    "\\s+|(?<=[,\\(\\):])|(?=[,\\(\\):])"));
                
                if (handleLabel()) {
                    String instr = handleInstr();
                    if (!checkPseudoInstr(instr)) {
                        checkInstr(instr);
                    }
                }
                debug("\n");
            }
            lineNumber++;
        }
        br.close();
        debugPrintLabels();
        fixPhysicalAddress();
        fixRelativeAddress();
    }

    private static void debug(String message) {
        System.out.print(message);
    }

    private static void debugPrintLabels() {
        debug("\nlabels:\n-------\n");
        labels.keySet().forEach((label) -> {
            debug(label + ": " + labels.get(label) + "\n");
        });    
    }
    
    private static void error(int lineNumber, String line, String message) {
        System.err.println("\nLine " + lineNumber + ": " + line + "\n" + message);
        System.exit(-1);
    }
    
    private static void setTokens(String[] tokens) {
        //debug("Tokens: ");
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = handleLiteralNumber(tokens[i].trim().toUpperCase());
            //debug("'" + tokens[i] + "' | ");
        }
        GBAsm.tokens = tokens;
        tokenIndex = -1;
        nextToken();
    }
    
    private static boolean hasNextToken() {
        return tokenIndex < tokens.length - 1;
    }
    
    private static String nextToken() {
        while (hasNextToken() && tokens[++tokenIndex].isEmpty()) {};
        return tokens[tokenIndex];
    }

    private static String getToken() {
        return tokens[tokenIndex];
    }
    
    private static String removeComment(String line) {
        int commentLocation = line.indexOf(";");
        if (commentLocation == 0) {
            line = "";
        }
        else if (commentLocation > 0) {
            line = line.substring(0, commentLocation - 1);
        }
        return line;
    }

    private static String handleLiteralNumber(String token) {
        if (token.isEmpty()) {
            return "";
        }
        else if (token.startsWith("%") || 
            (Character.isDigit(token.charAt(0)) && token.endsWith("B"))) {
            
            try {
                String binNumber = token.replace("%", "").replace("B", "");
                int n = Integer.parseInt(binNumber, 2);
                token = "" + n;
            }
            catch (NumberFormatException e) {
                error(lineNumber, currentLine, "invalid binary number !");
            }
        }
        else if (token.startsWith("$") || 
            (Character.isDigit(token.charAt(0)) && token.endsWith("H"))) {
            
            try {
                String hexNumber = token.replace("$", "").replace("H", "");
                int n = Integer.parseInt(hexNumber, 16);
                token = "" + n;
            }
            catch (NumberFormatException e) {
                error(lineNumber, currentLine, "invalid hex number !");
            }
        }
        return token;
    }
    
    private static boolean isLabel(String token) {
        return !validInstructions.contains(token) && (token.startsWith(".") || 
            Character.isAlphabetic(token.charAt(0)) && token.length() > 2);
    }
    
    private static boolean handleLabel() {
        String possibleLabel = getToken();
        if (!validInstructions.contains(possibleLabel)) {
            if (isLabel(possibleLabel)) {
                // fix label parent.label if necessary
                if (possibleLabel.startsWith(".")) {
                    if (parentLabel == null) {
                        error(lineNumber, currentLine, 
                            "parent label not declared !");
                    }
                    else {
                        possibleLabel = parentLabel + possibleLabel;
                    }
                }
                else {
                    parentLabel = possibleLabel;
                }

                labels.put(possibleLabel, address);
                lineLabel = possibleLabel;

                if (nextToken().equals(":")) {
                    nextToken();
                }
                return hasNextToken();
            }
            else {
                error(lineNumber, currentLine, "'" + possibleLabel + 
                    "' invalid label or instruction !");
            }
        }
        return true;
    }

    private static String handleInstr() {
        String possibleInstr = getToken();
        if (validInstructions.contains(possibleInstr)) {
            return possibleInstr;
        }
        else {
            error(lineNumber, currentLine, "'" + possibleInstr + 
                "' invalid instruction !");
        }
        return null;
    }
    
    private static void checkInstr(String instr) {
        int argTokenStartIndex = -1;
        for (int type = 0; type < 6; type++) {
            int opSize = 0;
            String originalToken = "";
            boolean replaced = false;
            String arg = "";
            while (hasNextToken()) {
                if (argTokenStartIndex < 0) {
                    argTokenStartIndex = tokenIndex;
                }
                nextToken();
                String replacedToken = replaceArgument(type, getToken());
                arg += replacedToken;
                if (!replaced && !getToken().equals(replacedToken)) {
                    originalToken = getToken();
                    opSize = replacedToken.contains("16") ? 2 : 1;
                    replaced = true;
                }
            }
            String conversionKey = instr + (arg.isEmpty() ? "" : " " + arg);
            if (conversionTable.containsKey(conversionKey)) {
                debug(" -> " + conversionKey + " -> " + address + ": ");
                writeMachineCodeHex(conversionTable.get(conversionKey));
                if (replaced) {
                    if (isLabel(originalToken)) {
                        saveFixableAddress(originalToken, opSize == 1);
                    }
                    else {
                        writeMachineCodeInt(originalToken, opSize);
                    }
                }
                return;
            }
            else {
                tokenIndex = argTokenStartIndex;
            }
        }
        error(lineNumber, currentLine, "invalid arguments or combination !");
    }

    /**
     * d8  means immediate 8 bit data.
     * d16 means immediate 16 bit data.
     * a8  means 8 bit unsigned data, which are added to $FF00 in certain 
     *     instructions (replacement for missing IN and OUT instructions).
     * a16 means 16 bit address.
     * r8  means 8 bit signed data, which are added to program counter.
     */
    private static String replaceArgument(int type, String token) {
        if (type == 0) {
            return token;
        }
        
        int c;
        boolean isLabel = false;
        
        try {
            c = Integer.parseInt(token);
        }
        catch (NumberFormatException e) {
            if (isLabel(token)) {
                isLabel = true;
                c = 65535;
            }
            else {
                return token;
            }
        }
        
        switch (type) {
            case 1:
                if (c >= 0 && c < 256) {
                    token = "D8";
                }
                break;
            case 2:
                if (isLabel || (c >= 0 && c <= 65535)) {
                    token = "D16";
                }
                break;
            case 3:
                if (c >= 0 && c <= 255) {
                    token = "A8";
                }
                break;
            case 4:
                if (isLabel || (c >= 0 && c <= 65535)) {
                    token = "A16";
                }
                break;
            case 5:
                if (isLabel || (c >= -128 && c <= 127)) {
                    token = "R8";
                }
                break;
        }
        return token;
    }
    
    private static void writeMachineCodeHex(String values) {
        debug(values + " ");
        String[] data = values.split(" ");
        for (String value : data) {
            value = value.trim();
            if (!value.isEmpty()) {
                int intValue = Integer.parseInt(value, 16);
                machineCode[address++] = intValue;
            }
        }
    }
    
    private static void writeMachineCodeInt(String value, int size) {
        int intValue = Integer.parseInt(value);
        debug(Integer.toHexString(intValue));
        machineCode[address++] = intValue & 0xff;
        if (size == 2) {
            machineCode[address++] = (intValue >> 8) & 0xff;
            debug(" " + Integer.toHexString((intValue >> 8) & 0xff));
        }
    }
    
    private static void saveFixableAddress(String label, boolean isRelativeAddress) {
        if (label.startsWith(".")) {
            if (parentLabel == null) {
                error(lineNumber, currentLine, "non-local label not declared !");
            }
            else {
                label = parentLabel + label;
            }
        }

        if (isRelativeAddress) {
            relativeAddressToFix.put(address, label);
            debug("## (relative label " + label + ")");
            address++;
        }
        else {
            physicalAddressToFix.put(address, label);
            debug("## ## (label " + label + ")");
            address += 2;
        }
    }

    // fix physical addresses
    private static void fixPhysicalAddress() {
        debug("\n");
        physicalAddressToFix.keySet().forEach((rt) -> {
            String label = physicalAddressToFix.get(rt);
            Integer dst = labels.get(label);
            if (dst == null) {
                error(-1, "", "label '" + label + "' not declared !");
            }
            debug("fixing physical address " + rt + ": " + label + "\n");
            machineCode[rt] = dst & 0xff;
            machineCode[rt + 1] = (dst >> 8) & 0xff;
        });        
    }
    
    // fix relative addresses
    private static void fixRelativeAddress() {
        debug("\n");
        relativeAddressToFix.keySet().forEach((rt) -> {
            String label = relativeAddressToFix.get(rt);
            Integer dst = labels.get(label);
            if (dst == null) {
                error(-1, "", "label '" + label + "' not declared !");
            }
            int r8 = dst - (rt + 1);
            // TODO: check -128 <= r8 <= 127
            machineCode[rt] = r8;
            
            debug("fixing relative address " + rt + ": " +
                    label + " -> r8=" + r8 + "\n");
        });    
    }
    
    private static boolean checkPseudoInstr(String token) {
        if (token.equals("ORG")) {
            handleORG();
        }
        else if (token.equals("EQU")) {
            handleEQU();
        }
        else if (token.equals("DB")) {
            handleDB();
        }
        else {
            return false;
        }
        return true;
    }

    private static void handleORG() {
        if (!hasNextToken()) {
            error(lineNumber, currentLine, 
                "missing argument for ORG operation !");
        }
        nextToken();
        int value = 0;
        try {
            value = Integer.parseInt(getToken());
        }
        catch (NumberFormatException e) {
            error(lineNumber, currentLine, "invalid '" + getToken() + 
                "' value for ORG operation !");
        }
        if (value < address || value > 65535) {
            error(lineNumber, currentLine, "'" + getToken() + 
                "' invalid value for ORG operation !");
        }
        address = value;
        nextToken();
    }

    private static void handleEQU() {
        if (!hasNextToken()) {
            error(lineNumber, currentLine, 
                "missing argument for EQU operation !");
        }
        nextToken();
        if (lineLabel == null) {
            error(lineNumber, currentLine, "label for EQU is missing !");
        }
        int value = Integer.parseInt(getToken());
        if (value < 0 || value > 65535) {
            error(lineNumber, currentLine, "'" + getToken() + 
                "' invalid value for EQU operation !");
        }
        labels.put(lineLabel, value);
    }
    
    private static void handleDB() {
        while (hasNextToken()) {
            nextToken();
            int value = Integer.parseInt(getToken());
            if (value < 0 || value > 255) {
                error(lineNumber, currentLine, "'" + getToken() + 
                    "' invalid value for DB operation !");
            }
            debug("\n" + address + ": DB -> " + value + "");
            machineCode[address++] = value;
            if (hasNextToken()) {
                nextToken();
                if (!getToken().equals(",")) {
                    error(lineNumber, currentLine, 
                        "expected ',' in DB operation !");
                }
            }
        }
    }
    
    private static void writeOutBinFile(String outfilename) throws Exception {
        OutputStream os = new FileOutputStream(outfilename);
        for (int i = 0; i < machineCode.length; i++) {
            os.write(machineCode[i]);
            //debug("writing " + Integer.toHexString(i) + " : " + 
            //        Integer.toHexString(machineCode[i]));
        }
        os.close();
    }    
    
}
