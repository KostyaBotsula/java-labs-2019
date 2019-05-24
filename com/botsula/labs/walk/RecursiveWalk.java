package com.botsula.labs.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {
    private static int BLOCK_SIZE = 4096;
    private static int FNV_32_INIT = 0x811c9dc5;
    private static int FNV_32_PRIME = 0x01000193;

    private void processFiles(Path path, BufferedWriter bufWriter) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) throws IOException {
                    bufWriter.write(String.format("%08x %s\n", getHash(filePath), filePath));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path filePath, IOException e) throws IOException {
                    bufWriter.write(String.format("%08x %s\n", 0, filePath));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getHash(Path path) {
        try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
            byte[] block = new byte[BLOCK_SIZE];
            int blockLength;
            int hash = FNV_32_INIT;
            while ((blockLength = inputStream.read(block)) != -1) {
                for (int i = 0; i < blockLength; i++) {
                    hash = (hash * FNV_32_PRIME) ^ (block[i] & 0xff);
                }
            }
            return hash;
        } catch (IOException | InvalidPathException e) {
            System.err.println("IOException");
            return 0;
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
        RecursiveWalk recursiveWalk = new RecursiveWalk();
        String line = "";

        try (BufferedWriter bufWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            try (BufferedReader bufReader = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), StandardCharsets.UTF_8))) {
                while ((line = bufReader.readLine()) != null) {
                    recursiveWalk.processFiles(Paths.get(line), bufWriter);
                }
            } catch (InvalidPathException | IOException e) {
                bufWriter.write(String.format("%08x %s\n", 0, line));
            }
        } catch (IOException | InvalidPathException exe) {
            System.err.println("IOException");
        }
    }
}

