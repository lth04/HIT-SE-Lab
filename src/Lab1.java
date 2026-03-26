import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class Lab1 {
    private static final Graph GRAPH = new Graph();
    private static final Random RANDOM = new Random();
    private static final int PAGE_RANK_ITERATIONS = 100;
    private static final double DAMPING_FACTOR = 0.85;

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            Path corpusPath = buildPath(args, scanner);
            buildGraph(corpusPath);
            runInteractiveMenu(scanner);
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
        }
    }

    private static Path buildPath(String[] args, Scanner scanner) {
        if (args.length > 0) {
            return Path.of(args[0]);
        }
        System.out.print("请输入文本文件路径: ");
        return Path.of(scanner.nextLine().trim());
    }

    private static void buildGraph(Path source) throws IOException {
        List<String> words = new ArrayList<>();
        Files.lines(source)
                .map(line -> line.replaceAll("[^a-zA-Z]", " ").toLowerCase())
                .forEach(line -> {
                    for (String token : line.split("\\s+")) {
                        if (!token.isBlank()) {
                            words.add(token);
                        }
                    }
                });

        for (int i = 0; i + 1 < words.size(); i++) {
            GRAPH.addEdge(words.get(i), words.get(i + 1));
        }
    }

    private static void runInteractiveMenu(Scanner scanner) {
        while (true) {
            printMenu();
            String choice = scanner.nextLine().trim();
            if ("0".equals(choice)) {
                break;
            }
            handleChoice(choice, scanner);
        }
    }

    private static void printMenu() {
        System.out.println("\n--- Lab1 程序功能选择 ---");
        System.out.println("1. 展示有向图");
        System.out.println("2. 查询桥接词");
        System.out.println("3. 根据桥接词生成新文本");
        System.out.println("4. 计算两个单词之间的最短路径");
        System.out.println("5. 计算 PageRank");
        System.out.println("6. 随机游走");
        System.out.println("0. 退出");
        System.out.print("请选择功能: ");
    }

    private static void handleChoice(String choice, Scanner scanner) {
        switch (choice) {
            case "1" -> showDirectedGraph(GRAPH);
            case "2" -> handleBridgeWords(scanner);
            case "3" -> handleGenerateText(scanner);
            case "4" -> handleShortestPath(scanner);
            case "5" -> handlePageRank(scanner);
            case "6" -> System.out.println(randomWalk());
            default -> System.out.println("未知选项，请重新输入。");
        }
    }

    private static void handleBridgeWords(Scanner scanner) {
        String word1 = prompt(scanner, "输入第一个单词: ");
        String word2 = prompt(scanner, "输入第二个单词: ");
        System.out.println(queryBridgeWords(word1, word2));
    }

    private static void handleGenerateText(Scanner scanner) {
        String text = prompt(scanner, "输入一段新文本: ");
        System.out.println(generateNewText(text));
    }

    private static void handleShortestPath(Scanner scanner) {
        String start = prompt(scanner, "输入起点单词: ");
        String end = prompt(scanner, "输入终点单词（留空输出所有路径）: ");
        System.out.println(calcShortestPath(start, end));
    }

    private static void handlePageRank(Scanner scanner) {
        String word = prompt(scanner, "输入要计算 PageRank 的单词: ");
        System.out.println("PageRank: " + calPageRank(word));
    }

    private static String prompt(Scanner scanner, String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    public static void showDirectedGraph(Graph graph) {
        System.out.println("有向图结构（节点 -> 邻居(权重)）：");
        List<String> nodes = new ArrayList<>(graph.getNodes());
        Collections.sort(nodes);
        for (String source : nodes) {
            System.out.printf("%-20s -> ", source);
            Map<String, Integer> edges = graph.getEdges(source);
            if (edges.isEmpty()) {
                System.out.println("(no outgoing edges)");
                continue;
            }
            List<Map.Entry<String, Integer>> sortedEdges = new ArrayList<>(edges.entrySet());
            sortedEdges.sort(Comparator.<Map.Entry<String, Integer>, Integer>comparing(Map.Entry::getValue).reversed()
                    .thenComparing(Map.Entry::getKey));
            String joined = sortedEdges.stream()
                    .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                    .collect(Collectors.joining(", "));
            System.out.println(joined);
        }
        try {
            if (exportGraphImage(graph)) {
                System.out.println("有向图已输出为图像文件 graph.png，dot 文件 graph.dot 也已生成。");
            } else {
                System.out.println("未能生成图像，请确认系统已安装 Graphviz 并且 dot 命令可用。");
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("生成图像时出错：" + e.getMessage());
        }
    }

    private static boolean exportGraphImage(Graph graph) throws IOException, InterruptedException {
        Path dotFile = Path.of("graph.dot");
        Path imageFile = Path.of("graph.png");
        List<String> nodes = new ArrayList<>(graph.getNodes());
        Collections.sort(nodes);
        StringBuilder dot = new StringBuilder();
        dot.append("digraph G {\n");
        for (String source : nodes) {
            dot.append("    \"").append(escape(source)).append("\";\n");
            Map<String, Integer> edges = graph.getEdges(source);
            for (Map.Entry<String, Integer> entry : edges.entrySet()) {
                dot.append("    \"").append(escape(source)).append("\" -> \"")
                        .append(escape(entry.getKey())).append("\" [label=\"")
                        .append(entry.getValue()).append("\"];\n");
            }
        }
        dot.append("}\n");
        Files.writeString(dotFile, dot.toString(), StandardCharsets.UTF_8);
        ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFile.toString(), "-o", imageFile.toString());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }
        int exitCode = process.waitFor();
        return exitCode == 0 && Files.exists(imageFile);
    }

    private static String escape(String node) {
        return node.replace("\"", "\\\"");
    }


    public static String queryBridgeWords(String word1, String word2) {
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();
        if (!GRAPH.containsNode(word1) || !GRAPH.containsNode(word2)) {
            return missingWordMessage(word1, word2);
        }

        Set<String> bridges = findBridgeWords(word1, word2);
        if (bridges.isEmpty()) {
            return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
        }

        List<String> sorted = new ArrayList<>(bridges);
        Collections.sort(sorted);
        return formatBridgeWords(word1, word2, sorted);
    }

    private static String missingWordMessage(String word1, String word2) {
        boolean missing1 = !GRAPH.containsNode(word1);
        boolean missing2 = !GRAPH.containsNode(word2);
        if (missing1 && missing2) {
            return "No \"" + word1 + "\" and \"" + word2 + "\" in the graph!";
        }
        return "No \"" + (missing1 ? word1 : word2) + "\" in the graph!";
    }

    private static Set<String> findBridgeWords(String word1, String word2) {
        Set<String> result = new HashSet<>();
        Map<String, Integer> neighbors = GRAPH.getEdges(word1);
        for (String word3 : neighbors.keySet()) {
            if (GRAPH.getEdges(word3).containsKey(word2)) {
                result.add(word3);
            }
        }
        return result;
    }

    private static String formatBridgeWords(String word1, String word2, List<String> sorted) {
        if (sorted.size() == 1) {
            return "The bridge word from \"" + word1 + "\" to \"" + word2 + "\" is: " + sorted.get(0) + ".";
        }
        StringBuilder sb = new StringBuilder("The bridge words from \"");
        sb.append(word1).append("\" to \"").append(word2).append("\" are: ");
        for (int i = 0; i < sorted.size(); i++) {
            sb.append(sorted.get(i));
            if (i < sorted.size() - 2) {
                sb.append(", ");
            } else if (i == sorted.size() - 2) {
                sb.append(", and ");
            }
        }
        sb.append(".");
        return sb.toString();
    }

    public static String generateNewText(String inputText) {
        String[] tokens = inputText.replaceAll("[^a-zA-Z]", " ").toLowerCase().split("\\s+");
        List<String> cleaned = new ArrayList<>();
        for (String token : tokens) {
            if (!token.isBlank()) {
                cleaned.add(token);
            }
        }
        if (cleaned.size() < 2) {
            return String.join(" ", cleaned);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cleaned.size() - 1; i++) {
            String current = cleaned.get(i);
            String next = cleaned.get(i + 1);
            sb.append(current);

            Set<String> bridges = findBridgeWords(current, next);
            if (!bridges.isEmpty()) {
                String chosen = new ArrayList<>(bridges).get(RANDOM.nextInt(bridges.size()));
                sb.append(" ").append(chosen);
            }
            sb.append(" ");
        }
        sb.append(cleaned.get(cleaned.size() - 1));
        return sb.toString();
    }

    public static String calcShortestPath(String word1, String word2) {
        word1 = word1.toLowerCase();
        if (!GRAPH.containsNode(word1)) {
            return "单词 \"" + word1 + "\" 不在图中。";
        }
        if (word2 == null || word2.isBlank()) {
            return printAllShortestPaths(word1);
        }
        word2 = word2.toLowerCase();
        if (!GRAPH.containsNode(word2)) {
            return "单词 \"" + word2 + "\" 不在图中。";
        }
        return calcSingleShortestPath(word1, word2);
    }

    private static String printAllShortestPaths(String base) {
        StringBuilder builder = new StringBuilder("从 \"" + base + "\" 到其他单词的最短路径：\n");
        for (String node : GRAPH.getNodes()) {
            if (node.equals(base)) {
                continue;
            }
            builder.append(node).append(": ").append(calcSingleShortestPath(base, node)).append("\n");
        }
        return builder.toString();
    }

    private static String calcSingleShortestPath(String start, String end) {
        Map<String, Integer> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>((a, b) -> Integer.compare(a.distance, b.distance));

        for (String node : GRAPH.getNodes()) {
            distances.put(node, Integer.MAX_VALUE);
        }
        distances.put(start, 0);
        queue.add(new Node(start, 0));

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            if (current.distance > distances.get(current.name)) {
                continue;
            }
            if (current.name.equals(end)) {
                break;
            }
            for (Map.Entry<String, Integer> entry : GRAPH.getEdges(current.name).entrySet()) {
                String neighbor = entry.getKey();
                int weight = entry.getValue();
                int newDistance = distances.get(current.name) + weight;
                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    previous.put(neighbor, current.name);
                    queue.add(new Node(neighbor, newDistance));
                }
            }
        }
        if (distances.get(end) == Integer.MAX_VALUE) {
            return "不可达。";
        }
        List<String> path = new ArrayList<>();
        for (String at = end; at != null; at = previous.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return String.join(" -> ", path) + " (权重: " + distances.get(end) + ")";
    }

    public static Double calPageRank(String word) {
        word = word.toLowerCase();
        if (!GRAPH.containsNode(word)) {
            return 0.0;
        }
        int N = GRAPH.getNodes().size();
        Map<String, Double> rank = new HashMap<>();
        for (String node : GRAPH.getNodes()) {
            rank.put(node, 1.0 / N);
        }

        for (int i = 0; i < PAGE_RANK_ITERATIONS; i++) {
            Map<String, Double> nextRank = new HashMap<>();
            double sinkRank = 0;
            for (String node : GRAPH.getNodes()) {
                if (GRAPH.getEdges(node).isEmpty()) {
                    sinkRank += rank.get(node);
                }
            }
            for (String node : GRAPH.getNodes()) {
                double contributed = sinkRank / N;
                for (String neighbor : GRAPH.getNodes()) {
                    Map<String, Integer> edges = GRAPH.getEdges(neighbor);
                    if (edges.containsKey(node)) {
                        contributed += rank.get(neighbor) / edges.size();
                    }
                }
                nextRank.put(node, (1 - DAMPING_FACTOR) / N + DAMPING_FACTOR * contributed);
            }
            rank = nextRank;
        }
        return rank.get(word);
    }

    public static String randomWalk() {
        List<String> nodes = new ArrayList<>(GRAPH.getNodes());
        if (nodes.isEmpty()) {
            return "图为空。";
        }
        String current = nodes.get(RANDOM.nextInt(nodes.size()));
        StringBuilder builder = new StringBuilder(current);
        Set<String> visitedEdges = new HashSet<>();
        while (true) {
            Map<String, Integer> edges = GRAPH.getEdges(current);
            if (edges.isEmpty()) {
                break;
            }
            List<String> nextSteps = new ArrayList<>(edges.keySet());
            String next = nextSteps.get(RANDOM.nextInt(nextSteps.size()));
            String edgeKey = current + "->" + next;
            if (!visitedEdges.add(edgeKey)) {
                builder.append(" ").append(next);
                break;
            }
            builder.append(" ").append(next);
            current = next;
        }
        writeRandomWalk(builder.toString());
        return builder.toString();
    }

    private static void writeRandomWalk(String result) {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of("random_walk.txt"))) {
            writer.write(result);
            System.out.println("结果写入 random_walk.txt");
        } catch (IOException e) {
            System.err.println("写入随机游走失败: " + e.getMessage());
        }
    }

    private static class Node {
        private final String name;
        private final int distance;

        private Node(String name, int distance) {
            this.name = name;
            this.distance = distance;
        }
    }
}
