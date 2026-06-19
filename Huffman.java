import java.io.*;
import java.nio.file.*;
import java.util.*;

public class Huffman {
    private static class Node implements Comparable<Node> {
        byte value;
        int freq;
        Node left, right;

        Node(byte value, int freq) {
            this.value = value;
            this.freq = freq;
        }

        Node(Node left, Node right) {
            this.left = left;
            this.right = right;
            this.freq = left.freq + right.freq;
        }

        boolean isLeaf() {
            return left == null && right == null;
        }

        @Override

        public int compareTo(Node other) {
            return Integer.compare(this.freq, other.freq);
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.out.println("Использование:");
            System.out.println("  java Huffman encode inputFile outputFile");
            System.out.println("  java Huffman decode inputFile outputFile");
            return;
        }

        String mode = args[0];
        String inputFile = args[1];
        String outputFile = args[2];

        if (mode.equals("encode")) {
            encode(inputFile, outputFile);
            System.out.println("Файл закодирован: " + outputFile);
        } else if (mode.equals("decode")) {
            decode(inputFile, outputFile);
            System.out.println("Файл декодирован: " + outputFile);
        } else {
            System.out.println("Неизвестный режим: " + mode);
        }
    }

    private static void encode(String inputFile, String outputFile) throws IOException {
        byte[] data = Files.readAllBytes(Paths.get(inputFile));
        int[] freq = new int[256];

        for (byte b : data) {
            freq[b & 0xFF]++;
        }

        Node root = buildTree(freq);
        Map<Byte, String> codes = new HashMap<>();
        buildCodes(root, "", codes);

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(outputFile))) {
            out.writeInt(data.length);
            for (int f : freq) {
                out.writeInt(f);
            }

            int currentByte = 0;
            int bitCount = 0;

            for (byte b : data) {
                String code = codes.get(b);
                for (char bit : code.toCharArray()) {
                    currentByte <<= 1;
                    if (bit == '1') {
                        currentByte |= 1;
                    }

                    bitCount++;

                    if (bitCount == 8) {
                        out.write(currentByte);
                        currentByte = 0;
                        bitCount = 0;
                    }
                }
            }

            if (bitCount > 0) {
                currentByte <<= (8 - bitCount);
                out.write(currentByte);
            }
        }
    }

    private static void decode(String inputFile, String outputFile) throws IOException {
        try (DataInputStream in = new DataInputStream(new FileInputStream(inputFile));
             FileOutputStream out = new FileOutputStream(outputFile)) {
            int originalSize = in.readInt();
            int[] freq = new int[256];

            for (int i = 0; i < 256; i++) {
                freq[i] = in.readInt();
            }

            Node root = buildTree(freq);

            if (root.isLeaf()) {
                for (int i = 0; i < originalSize; i++) {
                    out.write(root.value);
                }
                return;
            }

            Node current = root;
            int writtenBytes = 0;

            while (writtenBytes < originalSize) {
                int nextByte = in.read();
                if (nextByte == -1) {
                    break;
                }

                for (int i = 7; i >= 0 && writtenBytes < originalSize; i--) {
                    int bit = (nextByte >> i) & 1;
                    if (bit == 0) {
                        current = current.left;
                    } else {
                        current = current.right;
                    }

                    if (current.isLeaf()) {
                        out.write(current.value);
                        writtenBytes++;
                        current = root;
                    }
                }
            }
        }
    }

    private static Node buildTree(int[] freq) {
        PriorityQueue<Node> queue = new PriorityQueue<>();

        for (int i = 0; i < 256; i++) {
            if (freq[i] > 0) {
                queue.add(new Node((byte) i, freq[i]));
            }
        }

        if (queue.isEmpty()) {
            return new Node((byte) 0, 0);
        }

        if (queue.size() == 1) {
            return queue.poll();
        }

        while (queue.size() > 1) {
            Node left = queue.poll();
            Node right = queue.poll();
            Node parent = new Node(left, right);
            queue.add(parent);
        }
        return queue.poll();
    }

    private static void buildCodes(Node node, String code, Map<Byte, String> codes) {
        if (node.isLeaf()) {
            if (code.isEmpty()) {
                code = "0";
            }
            codes.put(node.value, code);
            return;
        }
        buildCodes(node.left, code + "0", codes);
        buildCodes(node.right, code + "1", codes);
    }
}
