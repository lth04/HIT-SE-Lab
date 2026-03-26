import java.util.*;

public class Graph {
    private Map<String, Map<String, Integer>> adjacencyList;

    public Graph() {
        this.adjacencyList = new HashMap<>();
    }

    public void addEdge(String source, String destination) {
        source = source.toLowerCase();
        destination = destination.toLowerCase();
        
        adjacencyList.putIfAbsent(source, new HashMap<>());
        adjacencyList.putIfAbsent(destination, new HashMap<>());
        
        Map<String, Integer> edges = adjacencyList.get(source);
        edges.put(destination, edges.getOrDefault(destination, 0) + 1);
    }

    public Set<String> getNodes() {
        return adjacencyList.keySet();
    }

    public Map<String, Integer> getEdges(String node) {
        return adjacencyList.getOrDefault(node.toLowerCase(), Collections.emptyMap());
    }

    public boolean containsNode(String node) {
        return adjacencyList.containsKey(node.toLowerCase());
    }

    public int getWeight(String source, String destination) {
        Map<String, Integer> edges = adjacencyList.get(source.toLowerCase());
        if (edges == null) return 0;
        return edges.getOrDefault(destination.toLowerCase(), 0);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String source : adjacencyList.keySet()) {
            sb.append(source).append(" -> ");
            Map<String, Integer> edges = adjacencyList.get(source);
            if (edges.isEmpty()) {
                sb.append("(no outgoing edges)");
            } else {
                List<String> edgeStrings = new ArrayList<>();
                for (Map.Entry<String, Integer> entry : edges.entrySet()) {
                    edgeStrings.add(entry.getKey() + "(" + entry.getValue() + ")");
                }
                sb.append(String.join(", ", edgeStrings));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
// Resolved conflict: Comments from B1 and C4 preserved
// Comment from B1
// Comment from C4