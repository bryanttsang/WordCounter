import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class WordCounter {

    // The following are the ONLY variables we will modify for grading.
    // The rest of your code must run with no changes.
    public static final Path FOLDER_OF_TEXT_FILES  = Paths.get("..."); // path to the folder where input text files are located
    public static final Path WORD_COUNT_TABLE_FILE = Paths.get("..."); // path to the output plain-text (.txt) file
    public static final int  NUMBER_OF_THREADS     = 2;                // max. number of threads to spawn

    private static List<Path> paths;
    private static ArrayList<TreeMap<String, Integer>> counts = new ArrayList<>();
    private static TreeMap<String, Integer> total = new TreeMap<>();

    private static void taskDivider(List<Path> files, int threadCount) {
        if (threadCount <= 1) {
            wordCounter(files);
        }
        if (threadCount > 1) {
            List<Path> sub1 = files.subList(0, files.size()/2);
            List<Path> sub2 = files.subList(files.size()/2, files.size());
            Thread t1 = new Thread(() -> taskDivider(sub1, threadCount/2));
            Thread t2 = new Thread(() -> taskDivider(sub2, threadCount-threadCount/2));
            t1.start();
            t2.start();
            try {
                t1.join();
                t2.join();
            } catch (InterruptedException ignored) {}
        }
    }

    private static void wordCounter(List<Path> files) {
        if (files.size() > 0) {
            for (Path path : files) {
                String file, word;
                try {
                    file = Files.lines(path, Charset.defaultCharset())
                            .collect(Collectors.joining(System.lineSeparator()))
                            .toLowerCase()
                            .replaceAll("[^a-z \\t\\n\\r]", "")
                            .replaceAll("[\\t\\n\\r]", " ")
                            .replace("'", "")
                            .trim().replaceAll(" +", " ")
                            + "  ";
                    do {
                        word = file.substring(0, file.indexOf(" "));
                        if (word.equals("")) {
                            break;
                        }
                        file = file.substring(file.indexOf(" ")+1);
                        increment(word, paths.indexOf(path));
                        incrementTotal(word);
                    } while (!file.equals(" "));
                } catch (IOException ignored) {}
            }
        }
    }

    private static void increment(String word, int index) {
        if (!counts.get(index).containsKey(word)) {
            counts.get(index).put(word, 1);
        }
        else {
            counts.get(index).replace(word, counts.get(index).get(word)+1);
        }
    }

    private synchronized static void incrementTotal(String word) {
        if (!total.containsKey(word)) {
            total.put(word, 1);
        }
        else {
            total.replace(word, total.get(word)+1);
        }
    }

    private static void taskPreparer() {
        try {
            paths = Files.walk(FOLDER_OF_TEXT_FILES)
                    .filter(p -> p.toString().endsWith(".txt"))
                    .collect(Collectors.toList());
            paths.sort(Comparator.comparing(p ->
                    p.toString().substring(p.toString().lastIndexOf("/") + 1, p.toString().lastIndexOf("."))));
            for (Path ignored : paths) {
                counts.add(new TreeMap<>());
            }
            taskDivider(paths, NUMBER_OF_THREADS);
        } catch (IOException ignored) {}
        generate();
    }

    private static void generate() {
        ArrayList<String> words = new ArrayList<>();
        int longest = 0;
        for (String word : total.keySet()) {
            words.add(word);
            longest = Math.max(longest, word.length());
        }
        ArrayList<Integer> width = new ArrayList<>();
        width.add(longest+1);
        for (Path p : paths) {
            width.add(Math.max(11, p.toString()
                    .substring(p.toString().lastIndexOf("/")+1, p.toString().lastIndexOf(".")).length()+1));
        }
        String table = spaceGenerator("", longest+1);
        for (int i = 0; i < paths.size(); i++) {
            table = table.concat(spaceGenerator(paths.get(i).toString()
                    .substring(paths.get(i).toString().lastIndexOf("/") + 1, paths.get(i).toString().lastIndexOf(".")), width.get(i+1)));
        }
        table = table.concat("total");
        for (String word : words) {
            table = table.concat("\n" + spaceGenerator(word, width.get(0)));
            for (int i = 1; i < width.size(); i++) {
                if (counts.get(i-1).get(word) == null) {
                    table = table.concat(spaceGenerator(Integer.toString(0), width.get(i)));
                } else {
                    table = table.concat(spaceGenerator(Integer.toString(counts.get(i-1).get(word)), width.get(i)));
                }
            }
            table = table.concat(Integer.toString(total.get(word)));
        }
        try {
            Files.write(WORD_COUNT_TABLE_FILE, table.getBytes());
        } catch (IOException ignored) {}
    }

    private static String spaceGenerator(String word, int length) {
        while (word.length() < length) {
            word = word.concat(" ");
        }
        return word;
    }

    public static void main(String... args) {
        // your implementation of how to run the WordCounter as a stand-alone multi-threaded program
        taskPreparer();
    }
}