import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Generates a tag cloud from a given input text.
 *
 * @author Victor Ruan
 */
public final class TagCloudGeneratorJC {

    /**
     * Private constructor so this utility class cannot be instantiated.
     */
    private TagCloudGeneratorJC() {

    }

    /**
     * Compare {Map.Pair<String, Integer>}'s count values in decreasing order.
     */
    private static class CompareWordCounts
            implements Comparator<Map.Entry<String, Integer>> {
        @Override
        public int compare(Map.Entry<String, Integer> o1,
                Map.Entry<String, Integer> o2) {
            Integer a = o1.getValue();
            Integer b = o2.getValue();
            return b.compareTo(a);
        }
    }

    /**
     * Sort {Map.Pair<String, Integer>}'s word keys in alphabetical order.
     */
    private static class SortAlphabetical
            implements Comparator<Map.Entry<String, Integer>> {
        @Override
        public int compare(Map.Entry<String, Integer> o1,
                Map.Entry<String, Integer> o2) {

            String s1 = o1.getKey();
            String s2 = o2.getKey();

            return s1.compareToIgnoreCase(s2);
        }
    }

    /**
     * Generates the set of characters in the given {@code String} into the
     * given {@code Set}.
     *
     * @param str
     *            the given {@code String}
     * @param strSet
     *            the {@code Set} to be replaced
     * @replaces strSet
     * @ensures strSet = entries(str)
     */

    public static void generateElements(String str, Set<Character> strSet) {
        assert str != null : "Violation of: str is not null";
        assert strSet != null : "Violation of: strSet is not null";

        strSet.clear();
        for (int i = 0; i < str.length(); i++) {
            if (!strSet.contains(str.charAt(i))) {
                strSet.add(str.charAt(i));
            }
        }
    }

    /**
     * Returns the first "word" (maximal length string of characters not in
     * {@code separators}) or "separator string" (maximal length string of
     * characters in {@code separators}) in the given {@code text} starting at
     * the given {@code position}.
     *
     * @param text
     *            the {@code String} from which to get the word or separator
     *            string
     * @param position
     *            the starting index
     * @param separators
     *            the {@code Set} of separator characters
     * @return the first word or separator string found in {@code text} starting
     *         at index {@code position}
     * @requires 0 <= position < |text|
     * @ensures <pre>
     * nextWordOrSeparator =
     *   text[position, position + |nextWordOrSeparator|)  and
     * if entries(text[position, position + 1)) intersection separators = {}
     * then
     *   entries(nextWordOrSeparator) intersection separators = {}  and
     *   (position + |nextWordOrSeparator| = |text|  or
     *    entries(text[position, position + |nextWordOrSeparator| + 1))
     *      intersection separators /= {})
     * else
     *   entries(nextWordOrSeparator) is subset of separators  and
     *   (position + |nextWordOrSeparator| = |text|  or
     *    entries(text[position, position + |nextWordOrSeparator| + 1))
     *      is not subset of separators)
     * </pre>
     */
    public static String nextWordOrSeparator(String text, int position,
            Set<Character> separators) {
        assert text != null : "Violation of: text is not null";
        assert separators != null : "Violation of: separators is not null";
        assert 0 <= position : "Violation of: 0 <= position";
        assert position < text.length() : "Violation of: position < |text|";

        // Create end index for substring
        int pos = position + 1;

        // Finds position of the character after the next word or separator to retrieve
        // a substring of characters before pos
        if (!separators.contains(text.charAt(position))) {
            while (pos < text.length()
                    && !separators.contains(text.charAt(pos))) {
                pos++;
            }
        }
        return text.substring(position, pos);
    }

    /**
     * Adds words from an input file and their counts into a map counting words
     * with different capitalization as a different word.
     *
     * @param in
     *            the input stream
     * @return a {@code map} containing all words from an input file and their
     *         counts
     * @throws IOException
     * @ensures mapWithWordCount = a map with word keys and count values from
     *          the given input file
     */
    public static Map<String, Integer> mapWithWordCount(BufferedReader in)
            throws IOException {

        // Create a set with separator characters
        String separator = " \t\n\r,-.!?[]';:/()\"*`";
        Set<Character> separatorSet = new HashSet<>();
        generateElements(separator, separatorSet);

        Map<String, Integer> wordCountMap = new HashMap<String, Integer>();
        String nextLine = in.readLine();
        while (nextLine != null) {

            // Iterate through the line of text and add each word to the map or
            // increment the count if the word key already exists in the map
            int startPos = 0;
            while (startPos < nextLine.length()) {
                String wordOrSep = nextWordOrSeparator(nextLine, startPos,
                        separatorSet);

                if (!separatorSet.contains(wordOrSep.charAt(0))) {
                    if (wordCountMap.containsKey(wordOrSep)) {
                        wordCountMap.replace(wordOrSep,
                                wordCountMap.get(wordOrSep) + 1);
                    } else {
                        // Add word into map with count of 1
                        wordCountMap.put(wordOrSep, 1);
                    }
                }
                startPos += wordOrSep.length();
            }
            nextLine = in.readLine();
        }
        return wordCountMap;
    }

    /**
     * Determines an appropriate font size for a word based on the ratio between
     * the word's number of occurrences and the number of occurrences of the
     * most common word.
     *
     * @param wordCount
     *            number of occurrences of the word whose font is to be
     *            determined
     * @param largestCount
     *            number of occurrences of the most common word in the file
     * @return the font size
     *
     * @ensures <pre> getFontSize = the appropriate font size of a given word </pre>
     */
    private static String getFontSize(Integer wordCount, int largestCount) {

        // Guaranteed to increment at least once so start at 10
        final int minFontSize = 10;
        // 48-11=37. This incrementer allows for font sizes from 11-48
        final float incrementer = 1.0f / 37;

        int fontSizeNum = minFontSize;
        float ratio = (float) wordCount / largestCount;

        // Increase font size until ratio is reached
        for (float i = 0; i <= ratio; i += incrementer) {
            fontSizeNum++;
        }

        return "f" + Integer.toString(fontSizeNum);
    }

    /**
     * Outputs the "opening" tags and css links in the generated HTML file.
     *
     * @param mainPage
     *            the output stream
     * @param fileInName
     *            Name of input file
     * @param numOfWords
     *            number of words user chose to display in the tag cloud
     * @updates mainPage.content
     * @requires mainPage.is_open
     * @ensures mainPage.content = #out.mainPage * [the HTML "opening" tags]
     */
    private static void outputHeader(PrintWriter mainPage, String fileInName,
            int numOfWords) {
        assert mainPage != null : "Violation of: mainPage is not null";
        assert fileInName != null : "Violation of: fileInName is not null";

        mainPage.println("<html>");
        mainPage.println("<head>");
        mainPage.println("<title>" + "Top" + numOfWords + " words in "
                + fileInName + "</title>");
        mainPage.println(
                "<link href=\"http://www.cse.ohio-state.edu/software/2231"
                        + "/web-sw2/assignments/projects/tag-cloud-generator/data/"
                        + "tagcloud.css\" rel=\"stylesheet\" type=\"text/css\">");
        mainPage.println(
                "<link href=\"tagcloud.css\" rel=\"stylesheet\" type=\"text/css\">");
        mainPage.println("</head>");

        mainPage.println("<body>");
        mainPage.println(
                "<h2>Top " + numOfWords + " words in " + fileInName + "</h2>");
        mainPage.println("<hr>");

        mainPage.println("<div class=\"cdiv\">");
        mainPage.println("<p class=\"cbox\">");
    }

    /**
     * Generates the content of the HTML output file styled using css. Each word
     * is printed to the given output file with an appropriate font size based
     * on the word's number of occurrences in the input file.
     *
     * @param wordCountMap
     *            {@code map} with word keys and count values
     * @param numOfWords
     *            number of words user chose to display in the tag cloud
     * @param fileInName
     *            the name of the file the user enters
     * @param inFile
     *            reads the input file
     * @param mainPage
     *            file where output will be generated
     * @ensures generatePage includes title and table of words with their own
     *          counts.
     */
    public static void generatePage(Map<String, Integer> wordCountMap,
            int numOfWords, String fileInName, BufferedReader inFile,
            PrintWriter mainPage) {

        outputHeader(mainPage, fileInName, numOfWords);

        // Add map entries to a {@code List} that will sort counts in
        // decreasing order
        List<Entry<String, Integer>> sortCounts = new ArrayList<>();

        for (Entry<String, Integer> entry : wordCountMap.entrySet()) {
            sortCounts.add(entry);
        }
        sortCounts.sort(new CompareWordCounts());

        // Create {@code List} that will sort words in alphabetical order
        List<Entry<String, Integer>> sortAlphabetical = new ArrayList<>();

        // Remove first pair before loop to obtain the count of the most common word
        Entry<String, Integer> largestCountPair = sortCounts.remove(0);
        int largestCount = largestCountPair.getValue();
        sortAlphabetical.add(largestCountPair);

        // Add the first {@code numOfWords} to the alphabetical List
        for (int i = 1; i < numOfWords; i++) {
            sortAlphabetical.add(sortCounts.remove(0));
        }
        sortAlphabetical.sort(new SortAlphabetical());

        // Print each tag cloud word in alphabetical order with a specific font
        while (sortAlphabetical.size() > 0) {
            Entry<String, Integer> removed = sortAlphabetical.remove(0);
            String fontSize = getFontSize(removed.getValue(), largestCount);
            mainPage.println("<span style=\"cursor:default\" class=\""
                    + fontSize + "\" title=\"count: " + removed.getValue()
                    + "\">" + removed.getKey() + "</span>");
        }

        outputFooter(mainPage);
    }

    /**
     * Outputs the "closing" tags in the generated HTML file.
     *
     * @param mainPage
     *            the output stream
     * @updates mainPage.content
     * @requires mainPage.is_open
     * @ensures mainPage.content = #out.mainPage * [the HTML "closing" tags]
     */
    private static void outputFooter(PrintWriter mainPage) {
        mainPage.println("</p>");
        mainPage.println("</div>");
        mainPage.println("</body>");
        mainPage.println("</html>");
    }

    /**
     * Main method.
     *
     * @param args
     *            the command line arguments; unused here
     */
    public static void main(String[] args) {

        BufferedReader in = new BufferedReader(
                new InputStreamReader(System.in));
        System.out.print("Enter the name of an input file: ");
        String fileInName = "";
        try {
            fileInName = in.readLine();
        } catch (IOException e) {
            System.err.println("File Invalid.");
            return;
        }

        BufferedReader inFile = null;
        try {
            inFile = new BufferedReader(new FileReader(fileInName));
        } catch (FileNotFoundException e) {
            System.err.println(
                    "Error producing reader of input file (file not found)");
            return;
        }

        System.out.print("Enter the name of an output file: ");
        String outputFile = "";
        try {
            outputFile = in.readLine();
        } catch (IOException e) {
            System.err.println("Error with output file.");
        }

        int numOfWords = 0;
        System.out.print("Enter the number of words to be in the tag cloud: ");
        try {
            numOfWords = Integer.parseInt(in.readLine());
        } catch (IOException e1) {
            System.err.println("Error reading from keyboard");
        }

        // Get {@code Map} with words from input file and their counts
        Map<String, Integer> wordCountMap = new HashMap<>();
        try {
            wordCountMap = mapWithWordCount(inFile);
        } catch (IOException e) {
            System.err.println("Error passing file reader as method paramter");
        }

        // Open output file and generate page
        try {
            PrintWriter mainPage = new PrintWriter(
                    new BufferedWriter(new FileWriter(outputFile)));
            generatePage(wordCountMap, numOfWords, fileInName, inFile,
                    mainPage);
            mainPage.close();
            inFile.close();
        } catch (IOException e) {
            System.err.println("Error creating or closing output file.");
        }
    }
}
