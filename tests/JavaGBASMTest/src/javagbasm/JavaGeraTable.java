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
 *
 * @author admin
 */
public class JavaGeraTable {

    public static Map<String, String> table = new HashMap<>();
    public static List<String> instructions;
    public static Map<String, Integer> labels = new HashMap<>();
    public static Map<Integer, String> relocatable = new HashMap<>();
    public static Map<Integer, String> relativeRelocatable = new HashMap<>();
    
    public static int[] assembledCode = new int[16384];
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        loadTable();
    }
    
    public static void loadTable() throws Exception { 
        Set<String> instructions = new HashSet<>();
        
        InputStream in = JavaGeraTable.class.getResourceAsStream("opcodes.txt");
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
                opcodeStr = opcodeStr.toUpperCase();
                System.out.println(line + ":" + opcodeStr);
                
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
        
        table.put("DB D8", "");
        instructions.add("DB");

        JavaGeraTable.instructions = new ArrayList<>(instructions);
        Collections.sort(JavaGeraTable.instructions);

        //for (String instruction : instructions) {
        //    System.out.println(instruction);
        //}
        
    }
   
}
