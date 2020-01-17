package javagbasm;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ref: https://www.pastraiser.com/cpu/gameboy/gameboy_opcodes.html
 *      http://gameboy.mongenel.com/dmg/gbspec.txt
 * @author admin
 */
public class JavaGBASM {

    public static Map<String, String> table = new HashMap<>();
    public static List<String> instructions;
    public static String parentLabel = null;
    public static Map<String, Integer> labels = new HashMap<>();
    public static Map<Integer, String> relocatable = new HashMap<>();
    public static Map<Integer, String> relativeRelocatable = new HashMap<>();
    
    public static int[] assembledCode = new int[16384 * 2];
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        loadTable();
        assemble("test.asm");
        write("D:/rgbds/tests/java2.gb");
    }
    
    public static void loadTable() throws Exception { 
        Set<String> instructions = new HashSet<>();
        
        InputStream in = JavaGBASM.class.getResourceAsStream("opcodes.txt");
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = null;
        
        int horizontalOpcode = -1;
        int verticalOpcode = 0;
        int opcode = 0;
        boolean usePrefix = false;
        
        while ((line = br.readLine()) != null) {
            line = line.trim().toUpperCase();
            if (line.isEmpty()) {
                continue;
            }
            
            if (line.charAt(0) == '*') {
                line = line.replace("" + line.charAt(0), "");
                line = line.replace("X", "");
                int newHorizontalOpcode = Integer.parseInt(line, 16);
                usePrefix = (newHorizontalOpcode == horizontalOpcode);
                horizontalOpcode = newHorizontalOpcode;
                verticalOpcode = 0;
                continue;
            }

            if (Character.isAlphabetic(line.charAt(0)) 
                && Character.isAlphabetic(line.charAt(1))) {
                
                opcode = verticalOpcode + horizontalOpcode;
                String hexCode = "00" + Integer.toHexString(opcode);
                hexCode = hexCode.substring(hexCode.length() - 2, hexCode.length());
                String opcodeStr = (usePrefix ? "cb " : "") + hexCode;
                // System.out.println(line + " -> " + opcodeStr);
                
                // fix STOP instruction
                if (line.equals("STOP 0")) {
                    line = "STOP";
                    opcodeStr += " 00";
                }
                
                table.put(line, opcodeStr);
                instructions.add(line.split(" ")[0]);
                
                verticalOpcode += 16;
            }
        }
        br.close();
        
        // pseudo instructions
        instructions.add("EQU");
        instructions.add("DB");
        instructions.add("ORG");

        JavaGBASM.instructions = new ArrayList<>(instructions);
        Collections.sort(JavaGBASM.instructions);

        //for (String instruction : instructions) {
        //    System.out.println(instruction);
        //}
        
    }

    private static int address = 0;
    private static int lineNumber = 1;

    public static void assemble(String source) throws Exception {
        InputStream in = JavaGBASM.class.getResourceAsStream(source);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = null;
        outer:
        while ((line = br.readLine()) != null) {
            line = line.trim();
            line = line.toUpperCase();
            
            // handle comment
            int commentLocation = line.indexOf(";");
            if (commentLocation == 0) {
                line = "";
            }
            else if (commentLocation > 0) {
                line = line.substring(0, commentLocation - 1);
            }
                
            if (line.isEmpty()) {
                lineNumber++;
                continue;
            }
            
            String lineLabel = null;
            int argStartIndex = 1;
            String[] tokens = line.split("(?<=,)|(?=,)|(?<=\\()|(?=\\()|(?<=\\))|(?=\\))|(?<=:)|(?=:)| ");
            String instr = tokens[0].trim();
            if (!instructions.contains(instr)) {
                if (instr.startsWith(".") || Character.isAlphabetic(instr.charAt(0)) && instr.length() > 2) {

                    // fix label parent.label if necessary
                    if (instr.startsWith(".")) {
                        if (parentLabel == null) {
                            error(line, "parent label not declared !");
                        }
                        else {
                            instr = parentLabel + instr;
                        }
                    }
                    else {
                        parentLabel = instr;
                    }
                        
                    labels.put(instr, address);
                    lineLabel = instr;
                    if (tokens.length == 1) {
                        lineNumber++;
                        continue outer;
                    }
                    instr = tokens[1].trim();
                    argStartIndex = 2;
                    
                    // ignore ':' after label
                    if (instr.equals(":")) {
                        if (tokens.length == 2) {
                            lineNumber++;
                            continue outer;
                        }
                        instr = tokens[2].trim();
                        argStartIndex = 3;
                    }
                }
                else {
                    error(line, "'" + tokens[0] + "' invalid label or instruction !");
                }
            }

            if (!instructions.contains(instr)) {
                error(line, "'" + instr + "' invalid instruction !");
            }
            
            // handle instructions
            
            for (int t = 0; t < 8; t++) {

                String arg = "";
                String replacedToken = "";
                int replacementCount = 0;
                int opSize = 0;
                
                for (int i = argStartIndex; i < tokens.length; i++) {
                    tokens[i] = tokens[i].trim();
                    if (tokens[i].isEmpty()) {
                        continue;
                    }

                    if (tokens[i].startsWith("%") || (!tokens[i].isEmpty() && Character.isDigit(tokens[i].charAt(0)) && tokens[i].endsWith("B"))) {
                        try {
                            String testNumber = tokens[i];
                            testNumber = testNumber.replace("%", "");
                            testNumber = testNumber.replace("B", "");
                            int n = Integer.parseInt(testNumber, 2);
                            tokens[i] = "" + n;
                        }
                        catch (Exception e) {
                            error(line, "invalid binary number !");
                        }
                    }
                    else if (tokens[i].startsWith("$") || (!tokens[i].isEmpty() && Character.isDigit(tokens[i].charAt(0)) && tokens[i].endsWith("H"))) {
                        try {
                            String testNumber = tokens[i];
                            testNumber = testNumber.replace("$", "");
                            testNumber = testNumber.replace("H", "");
                            int n = Integer.parseInt(testNumber, 16);
                            tokens[i] = "" + n;
                        }
                        catch (Exception e) {
                        }
                    }
                    
                    String r = replaceToken(t, tokens[i]);
                    arg += r;
                    if (!r.equals(tokens[i])) {
                        replacementCount++;
                        replacedToken = tokens[i];
                        opSize = r.contains("16") ? 2 : 1;
                    }
                }
                
                String key = instr;
                if (!arg.isEmpty()) {
                    key += " " + arg;
                }
                
                // handle pseudo instructions
                
                if (instr.equals("EQU")) {
                    handleEQU(line, tokens, argStartIndex, lineLabel);
                    lineNumber++;
                    continue outer;
                }
                else if (instr.equals("ORG")) {
                    handleORG(line, tokens, argStartIndex);
                    lineNumber++;
                    continue outer;
                }
                else if (instr.equals("DB")) {
                    handleDB(line, tokens, argStartIndex);
                    lineNumber++;
                    continue outer;
                }
            
                if (table.containsKey(key)) {
                    if (replacementCount > 1) {
                        error(line, "invalid argument !");
                    }
                    String codeStr = table.get(key);
                    codeStr = codeStr.trim();
                    System.out.print(address + ": " + key + " -> " + codeStr + " ");
                    
                    // write machine code
                    if (!codeStr.isEmpty()) {
                        int code = Integer.parseInt(codeStr.substring(0, 2), 16);
                        assembledCode[address] = code;
                        if (codeStr.length() == 5) {
                            code = Integer.parseInt(codeStr.substring(3, 5), 16);
                            assembledCode[address + 1] = code;
                        }
                    }
                    
                    address += (table.get(key).length() / 2);
                    if (replacementCount == 1) {
                        if (replacedToken.startsWith(".") || Character.isAlphabetic(replacedToken.charAt(0))) {
                            if (replacedToken.startsWith(".")) {
                                if (parentLabel == null) {
                                    error(line, "parent label not declared !");
                                }
                                else {
                                    replacedToken = parentLabel + replacedToken;
                                }
                            }
                            
                            if (opSize == 1) {
                                relativeRelocatable.put(address, replacedToken);
                                System.out.print("## (relative label " + replacedToken + ")");
                            }
                            else {
                                relocatable.put(address, replacedToken);
                                System.out.print("## ## (label " + replacedToken + ")");
                            }
                        }
                        else {
                            System.out.print(replacedToken);
                            int code = Integer.parseInt(replacedToken);
                            assembledCode[address] = code & 0xff;
                            if (opSize == 2){
                                assembledCode[address + 1] = code >> 8;
                            }
                        }
                        address += opSize;
                    }
                    System.out.println();
                    lineNumber++;
                    continue outer;
                }
            }
            
            br.close();
            error(line, "invalid arguments !");
        }
        br.close();
        
        System.out.println("\nlabels:\n-------");
        for (String label : labels.keySet()) {
            System.out.println(label + ": " + labels.get(label));
        }        

        // fix physical addresses
        System.out.println();
        for (Integer rt : relocatable.keySet()) {
            String label = relocatable.get(rt);
            Integer dst = labels.get(label);
            if (dst == null) {
                error("", "label '" + label + "' not declared !");
            }

            System.out.println("fixing physical address " + rt + ": " + label);
            
            assembledCode[rt] = dst & 0xff;
            assembledCode[rt + 1] = (dst >> 8) & 0xff;
        }        
        
        // fix relative addresses
        System.out.println();
        for (Integer rt : relativeRelocatable.keySet()) {
            String label = relativeRelocatable.get(rt);
            Integer dst = labels.get(label);
            if (dst == null) {
                error("", "label '" + label + "' not declared !");
            }
            int r8 = dst - (rt + 1);
            assembledCode[rt] = r8;
            System.out.println("fixing relative address " + rt + ": " + label + " -> r8=" + r8);
        }        
    }
 
    /*
        d8  means immediate 8 bit data
        d16 means immediate 16 bit data
        a8  means 8 bit unsigned data, which are added to $FF00 in certain instructions (replacement for missing IN and OUT instructions)
        a16 means 16 bit address
        r8  means 8 bit signed data, which are added to program counter    
    */   
    public static String replaceToken(int i, String arg) {
        if (i == 0) {
            return arg;
        }
        int c = -1;
        String type = "";
        boolean isLabel = false;
        
        try {
            c = Integer.parseInt(arg);
        }
        catch (Exception e) {
            if (!instructions.contains(arg) && (arg.startsWith(".") || Character.isAlphabetic(arg.charAt(0))) && arg.length() > 2) {
                isLabel = true;
                c = 65535;
            }
            else {
                return arg;
            }
        }
        
        switch (i) {
            case 1:
                if (c >= 0 && c < 256) {
                    type = "D8";
                }
                break;
            case 2:
                if (isLabel || (c >= 0 && c <= 65535)) {
                    type = "D16";
                }
                break;
            case 3:
                if (c >= 0 && c <= 255) {
                    type = "A8";
                }
                break;
            case 4:
                if (isLabel || (c >= 0 && c <= 65535)) {
                    type = "A16";
                }
                break;
            case 5:
                if (isLabel || (c >= -128 && c <= 127)) {
                    type = "R8";
                }
                break;
            case 6:
                type = "00" + Integer.toHexString(c);
                type = type.substring(type.length() - 2, type.length());
                type = type + "H";
                break;
            //case 7:
            //    type = "0000" + Integer.toHexString(c);
            //    type = type.substring(type.length() - 4, type.length());
            //    type = type + "H";
            //    break;
        }
        return type;
    }
    
    public static void handleEQU(String line, String[] tokens, int argIndex, String lineLabel) {
        if (lineLabel == null) {
            error(line, "label is missing !");
        }
        int code = Integer.parseInt(tokens[argIndex].trim());
        if (code < address || code > 65535) {
            error(line, "'" + tokens[argIndex].trim() + "' invalid value for EQU operation !");
        }
        labels.put(lineLabel, code);
    }
    
    public static void handleORG(String line, String[] tokens, int argIndex) {
        int code = Integer.parseInt(tokens[argIndex].trim());
        if (code < address || code > 65535) {
            error(line, "'" + tokens[argIndex].trim() + "' invalid value for ORG operation !");
        }
        address = code;
    }

    public static void handleDB(String line, String[] tokens, int argIndex) {
        for (int i = argIndex; i < tokens.length; i++) {
            if (tokens[i].trim().isEmpty()) {
                continue;
            }
            int code = Integer.parseInt(tokens[i].trim());
            if (code < 0 || code > 255) {
                error(line, "'" + tokens[i].trim() + "' invalid value for DB operation !");
            }
            System.out.println(address + ": DB -> " + code + " ");
            assembledCode[address++] = code;
            while (i + 1 < tokens.length - 1 && tokens[++i].trim().isEmpty()) { };
            if (!tokens[i].trim().equals(",")) {
                break;
            }
        }
    }
    
    public static void error(String line, String message) {
        System.err.println("Line " + lineNumber + ": " + line + "\n" + message);
        System.exit(-1);
    }
    
    public static void write(String outfilename) throws Exception {
        OutputStream os = new FileOutputStream(outfilename);
        for (int i = 0; i < assembledCode.length; i++) {
            os.write(assembledCode[i]);
            // System.out.println(i + " : " + assembledCode[i]);
        }
        os.close();
    }
   
    
}
