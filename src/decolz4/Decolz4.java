package decolz4;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream;

/**
 * Decompress hybrid binary load file to normal binary load file
 */
public class Decolz4 {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        /*If arguments not proper, display usage and exit*/
        if (args.length != 2) {
            printUsage();
            return;
        }

        FileInputStream fis;
        FileOutputStream fos;

        try {
            fis = new FileInputStream(args[0]);
        } catch (Exception e1) {
            System.out.println("Unable to open input file " + args[0]);
            printException(e1);
            return;
        }
        try {
            fos = new FileOutputStream(args[1]);
        } catch (Exception e2) {
            System.out.println("Unable to open output file " + args[1]);
            printException(e2);
            return;
        }

        try {

            /*Get the 255 255 header*/
            int b1 = fis.read();
            int b2 = fis.read();

            if (b1 != 255 || b2 != 255) {
                System.out.println("The input file is not a binary load file");
                return;
            }

            /*Write the header to the output file*/
            fos.write(b1);
            fos.write(b2);

            /*Keep reading the segments*/
            bload:
            while (true) {

                int firstLo = -1;
                int firstHi = -1;

                boolean validSegment = false;

                seghead:
                while (validSegment == false) {

                    /*Get first two bytes*/
                    firstLo = fis.read();
                    if (firstLo == -1) {
                        break bload;
                    }
                    firstHi = fis.read();
                    if (firstHi == -1) {
                        break bload;
                    }

                    /*Check if a dummy header*/
                    if (firstHi == 255 && firstLo == 255) {
                        continue;
                    }
                    validSegment = true;
                }

                int lastLo = fis.read();
                int lastHi = fis.read();

                int first = firstLo + 256 * firstHi;
                int last = lastLo + 256 * lastHi;

               

                /*Normal segment*/
                if (last >= first) {
                    System.out.format("Normal Segment: $%04X - $%04X\n",first,last);
                    fos.write(firstLo);
                    fos.write(firstHi);
                    fos.write(lastLo);
                    fos.write(lastHi);
                    for (int i = 0; i < last - first + 1; i++) {
                        int b = fis.read();
                        fos.write(b);
                    }
                } /*Compressed segment*/ else {
                    
                    /*Get compression type*/
                    int cpType = fis.read();
                    System.out.format("Compressed segment: $%04X - $%04X CP: $%02X\n",first,last,cpType);
                    
                    /*Decompress the LZ4 block*/
                    BlockLZ4CompressorInputStream cis = new BlockLZ4CompressorInputStream(fis);

                    int b;
                    ArrayList<Integer> buffer = new ArrayList<>();
                    try {
                        while ((b = cis.read()) != -1) {
                            buffer.add(b);
                        }
                    } catch (Exception e) {

                    }
                    /*Write uncompressed segment*/
                    fos.write(firstLo);
                    fos.write(firstHi);
                    fos.write((first + buffer.size() - 1) % 256);
                    fos.write((first + buffer.size() - 1) / 256);
                    for (int bb : buffer) {
                        fos.write(bb);
                    }
                    System.out.format("Decompressed $%04X bytes\n",buffer.size());

                }

            }
            /*Close the input file*/
            fis.close();
            fos.close();
        } catch (IOException ioe) {
            System.out.println("Input/Output error");
            printException(ioe);
            return;
        }

    }

    public static void printUsage() {
        System.out.println("LZ4 Hybrid Binary Load File Decompressor 0.1");
        System.out.println("Usage: java -jar decolz4.jar <infile> <outfile>");
    }

    private static void printException(Exception e1) {
        String msg = e1.getMessage();
        if (msg == null) {
            msg = "";
        }
        System.out.println(e1.getClass().getName() + ":" + msg);
    }

}
