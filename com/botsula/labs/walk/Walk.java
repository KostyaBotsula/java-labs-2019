package com.botsula.labs.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;

public class Walk {
    private final int BLOCK_SIZE = 1024;
    private final int FNV_32_INIT = 0x811c9dc5;
    private final int FNV_32_PRIME = 0x01000193;
    private File inputFile, outputFile;

    private Walk(File inputFile, File outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    private Set<String> readFile() {
        Set<String> processedPaths = new LinkedHashSet<>();

        try (BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
            String line;

            while ((line = bufReader.readLine()) != null) {
                processedPaths.add(line);
            }
        } catch (InvalidPathException | IOException e) {
            System.err.format("IOException");
        }
        return processedPaths;
    }

    private void writeHash(Set<String> paths) {
        try (BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            for (String path : paths) {
                try (FileInputStream inputStream = new FileInputStream(Paths.get(path).toFile())) {
                    byte[] block = new byte[BLOCK_SIZE];
                    int blockLength;
                    int hash = FNV_32_INIT;

                    while ((blockLength = inputStream.read(block)) != -1) {
                        for (int i = 0; i < blockLength; i++) {
                            hash = (hash * FNV_32_PRIME) ^ (block[i] & 0xff);
                        }
                    }

                    bufWriter.write((String.format("%08x %s\n", hash, path)));
                } catch (IOException | InvalidPathException e) {
                    bufWriter.write(String.format("%08x %s\n", 0, path));
                }
            }
        } catch (IOException e) {
            System.err.println("IOException");
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Invalid arguments");
            return;
        }

        String inputFileName = args[0];
        String outputFileName = args[1];
        File inputFile = new File(inputFileName);
        File outputFile = new File(outputFileName);
        Walk walk = new Walk(inputFile, outputFile);
        Set<String> paths;

        paths = walk.readFile();
        walk.writeHash(paths);
    }

}
