package javagbasm;

import java.io.InputStream;

/**
 * https://gbdev.gg8.se/wiki/articles/The_Cartridge_Header#014D_-_Header_Checksum
 * 
 * @author admin
 */
public class GbRomChecksum {

    public static void main(String[] args) throws Exception {
        InputStream in = JavaGeraTable.class.getResourceAsStream("java.gb");
        int c = -1;
        int address = 0;
        int headerChecksum = 0;
        int globalChecksum = 0;
        while ((c = in.read()) >= 0) {

            if (address != 333 && address != 334 && address != 335) {
                globalChecksum += c;
            }

            if (address >= 308 && address <= 332) {
                headerChecksum = headerChecksum - c - 1;
            }
            
            System.out.println(address + ": " + c);
            address++;
        }
        in.close();

        globalChecksum += (headerChecksum & 0xff);

        System.out.println("");
        
        String headerChecksumStr = Integer.toHexString(headerChecksum & 0xff);
        System.out.println("header checksum $014D = " + headerChecksumStr);
        
        String globalChecksumStr = 
            Integer.toHexString((globalChecksum >> 8) & 0xff) + " " + 
            Integer.toHexString(globalChecksum & 0xff);
        
        System.out.println("global checksum $014E-$014F = " + globalChecksumStr);
    }
    
}
