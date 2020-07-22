package comp6791;

import comp6791.model.BlockDictionary;
import comp6791.model.Query;
import comp6791.model.SearchPointer;
import de.l3s.boilerpipe.BoilerpipeExtractor;
import de.l3s.boilerpipe.BoilerpipeProcessingException;
import de.l3s.boilerpipe.extractors.CommonExtractors;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * comp6791.SPIMI Algorithm - Single Pass In Memory Index
 *
 * @author Mayank Jariwala
 */
public class SPIMI {

    public static SortedMap<Integer, String> ConcordiafilesMapping = new TreeMap<>();
    public static SortedMap<Integer, String> AIfilesMapping = new TreeMap<>();

    // Primitives
    private int invertedIndex = 0, initialPointReader = 0;
    private static int blockNo = 0;
    public static int concordiaDocNumber = 1, aiDocNumber = 1;
    private static int corpusRead = 0;
    private static double noOfTokensInReutersCollectionBodyTitleTag = 0;

    //Collections
    private SortedMap<String, SortedMap<Integer, Integer>> invertedDict = new TreeMap<>();
    private SortedMap<Integer, String> filesMapping = new TreeMap<>();
    public static HashMap<Integer, Integer> docsMappingWithNoOfTokens = new HashMap<>();
    private List<SearchPointer> searchPointers = new ArrayList<>();
    private List<BlockReader> blockReaderList = new ArrayList<>();
    private List<String> keyBuffer = new ArrayList<>(blockNo);
    private List<SortedMap<Integer, Integer>> postingListId = new ArrayList<>();
    private SortedMap<String, SortedMap<Integer, Integer>> loadFileContent = new TreeMap<>();
    private SortedMap<String, SortedMap<Integer, Integer>> AILoadFileContent = new TreeMap<>();
    private List<String> aiRelatedTerms = new ArrayList<>();
    private Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        SPIMI SPIMI = new SPIMI();
        System.out.println("Do you want to start from scratch?(true/false)");
        boolean fromScratch = SPIMI.scanner.nextBoolean();
        if (fromScratch) {
            SPIMI.createIndexingFromScratch();
        } else {
            SPIMI.setUpInMemoryVariables();
            SPIMI.executeQuery();
        }
        SPIMI.scanner.close();
        System.gc();
    }

    private void createIndexingFromScratch() {
        getTheasurusData();
        String[] filesToBeIndexed = new String[]{Constants.CONCORDIA_BASE_PATH, Constants.AI_TOPICS_BASE_PATH};
        String[] indexName = new String[]{Constants.CONCORDIA_AI_INDEX_NAME, Constants.AI_INDEX_NAME};
        for (int i = 0; i < filesToBeIndexed.length; i++) {
            if (!Files.exists(Paths.get("blocks/"))) {
                try {
                    Files.createDirectory(Paths.get("blocks/"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Generating " + indexName[i] + " index");
            File[] files = new File(filesToBeIndexed[i]).listFiles();
            if (files != null)
                getFiles(files, indexName[0]);
            if (indexName[i].equalsIgnoreCase(Constants.AI_INDEX_NAME)) {
                if (AILoadFileContent.size() > 0) {
                    BlockDictionary blockDictionary = new BlockDictionary();
                    blockDictionary.setLoadFileContent(AILoadFileContent);
                    writeDictionaryFile(corpusRead, blockNo, blockDictionary);
                }
            } else {
                if (loadFileContent.size() > 0) {
                    BlockDictionary blockDictionary = new BlockDictionary();
                    blockDictionary.setLoadFileContent(loadFileContent);
                    writeDictionaryFile(corpusRead, blockNo, blockDictionary);
                }
            }
            startMergingPhase(indexName[i]);
            if (indexName[i].equalsIgnoreCase(Constants.CONCORDIA_AI_INDEX_NAME)) {
                writeConcordiaAIFilesMappingData();
                writeDocMappingWithNoOfTokens();
            }
            reInitVariables();
        }
        updateAITermsFile();
        Constants.DAVG = noOfTokensInReutersCollectionBodyTitleTag / concordiaDocNumber;
        saveValuesInMemory();
        executeQuery();
    }

    private void writeConcordiaAIFilesMappingData() {
        try {
            if (!Files.exists(Paths.get("in_memory/mapping.txt")))
                Files.createFile(Paths.get("in_memory/mapping.txt"));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("in_memory/mapping.txt"));
            for (Map.Entry data : ConcordiafilesMapping.entrySet()) {
                bufferedWriter.write(data.getKey() + "," + data.getValue() + "\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void executeQuery() {
        while (true) {
            String results = new Query().queryBoxer();
            System.out.println("OKAPI BM25 Ranking (k = " + Constants.K + " b = " + Constants.B + ") : " + results);
        }
    }

    private void setUpInMemoryVariables() {
        setUpDocsWithNoOfTokens();
        setUpDocWithDynamicMapping();
    }

    private void updateAITermsFile() {
        SortedMap<String, SortedMap<Integer, Integer>> aiContent = new TreeMap<>();
        try {
            if (!Files.exists(Paths.get("invertedIndex/AITermIndex.txt")))
                Files.createFile(Paths.get("invertedIndex/AITermIndex.txt"));
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("invertedIndex/AITermIndex.txt"));
            BufferedReader reader = new BufferedReader(new FileReader("invertedIndex/AIIndex.txt"));
            String line = "";
            while ((line = reader.readLine()) != null) {
                String[] lineData = line.split(":", 2);
                if (aiRelatedTerms.contains(lineData[0])) {
                    SortedMap<Integer, Integer> mapLocal = new TreeMap<>();
                    List<String> postingList = Arrays.stream(lineData[1].trim().substring(1, lineData[1].length() - 1)
                            .split(",")).collect(Collectors.toList());
                    for (String data : postingList) {
                        String[] mapper = data.split("=");
                        mapLocal.put(Integer.parseInt(mapper[0].trim()), Integer.parseInt(mapper[1].trim()));
                    }
                    aiContent.put(lineData[0], mapLocal);
                }
            }
            for (Map.Entry<String, SortedMap<Integer, Integer>> sortedSetEntry : aiContent.entrySet()) {
                String content = sortedSetEntry.getKey() + ":" + sortedSetEntry.getValue().toString();
                bufferedWriter.write(content + "\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Writing values for saving into memory and use as checkpoint
    private void saveValuesInMemory() {
        writeDocMappingWithNoOfTokens();
    }

    private void reInitVariables() {
        invertedDict = new TreeMap<>();
        filesMapping = new TreeMap<>();
        blockReaderList = new ArrayList<>();
        keyBuffer = new ArrayList<>(blockNo);
        postingListId = new ArrayList<>();
        loadFileContent = new TreeMap<>();
        invertedIndex = 0;
        initialPointReader = 0;
        blockNo = 0;
        corpusRead = 0;
    }

    private void getFiles(File[] files, String indexName) {
        for (File file : files) {
            if (file.isDirectory()) {
                getFiles(Objects.requireNonNull(Objects.requireNonNull(file.listFiles())), indexName);
            } else {
                if (file.getName().endsWith(".html")) {
                    boilerPlate(file.getAbsolutePath(), indexName);
                }
            }
        }
    }

    private void boilerPlate(String filePath, String indexName) {
        try {
            final BoilerpipeExtractor extractor = CommonExtractors.KEEP_EVERYTHING_EXTRACTOR;
            StringBuilder stringBuilder = new StringBuilder();
            filesMapping.put(concordiaDocNumber, filePath);
            BufferedReader fileReader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = fileReader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            if (indexName.equalsIgnoreCase(Constants.CONCORDIA_AI_INDEX_NAME)) {
                ConcordiafilesMapping.put(concordiaDocNumber, filePath);
                storeSingleFileDocsWithContentConcordAI(extractor.getText(stringBuilder.toString()), indexName);
                concordiaDocNumber++;
            } else {
                AIfilesMapping.put(aiDocNumber, filePath);
                storeSingleFileDocsWithContent(extractor.getText(stringBuilder.toString()), indexName);
                aiDocNumber++;
            }
            fileReader.close();
        } catch (IOException | BoilerpipeProcessingException e) {
            System.out.println(e.getCause() + "");
        }
    }

    /**
     * Responsible for merging all intermediate blocks
     */
    private void startMergingPhase(String indexName) {
        System.out.println("----- Merging " + blockNo + " Blocks phase -----");
        try {
            int i = 0;
            while (i < blockNo) {
                FileReader fileReader = new FileReader("blocks/block_" + i + ".txt");
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                blockReaderList.add(new BlockReader(bufferedReader));
                i++;
            }
            for (int blockReaderPoint = 0; blockReaderPoint < blockNo; blockReaderPoint++) {
                BlockReader br = blockReaderList.get(blockReaderPoint);
                if (br.isFileClosed()) {
                    String[] fileLineData = br.readNext().split(":", 2);
                    List<String> postingList = Arrays.stream(fileLineData[1].trim().substring(1, fileLineData[1].length() - 1)
                            .split(",")).collect(Collectors.toList());
                    SortedMap<Integer, Integer> postingListMap = new TreeMap<>();
                    for (String data : postingList) {
                        String[] keyValue = data.split("=");
                        postingListMap.put(Integer.valueOf(keyValue[0].trim()), Integer.valueOf(keyValue[1].trim()));
                    }
                    postingListId.add(initialPointReader, postingListMap);
                    keyBuffer.add(initialPointReader, fileLineData[0].trim());
                    initialPointReader++;
                }
            }
            while (!blockReaderList.isEmpty()) {
                findMin(keyBuffer);
            }
            if (invertedDict.size() > 0) {
                writeInvertedIndexToFile(invertedDict, indexName);
                invertedDict.clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Find min/smallest word from n blocks dictionary first pointer and put into new dictionary of
     * 25000 terms
     *
     * @param currentInTransitData First Word from each block of intermediate dictionary
     * @throws IOException Exception related to I/O operation
     */
    private void findMin(List<String> currentInTransitData) throws IOException {
        String data = Collections.min(currentInTransitData);
        int indexOfMin = currentInTransitData.indexOf(data);
        if (invertedDict.containsKey(data)) {
            SortedMap<Integer, Integer> existingList = invertedDict.get(data);
            existingList.putAll(postingListId.get(indexOfMin));
            invertedDict.put(data, existingList);
        } else {
            invertedDict.put(data, postingListId.get(indexOfMin));
        }
//        if (invertedDict.size() == 25000) {
//            writeInvertedIndexToFile(invertedDict);
//            invertedDict.clear();
//        }
        BlockReader br = blockReaderList.get(indexOfMin);
        if (br.isFileClosed()) {
            String topStringRead = br.readNext();
            if (topStringRead == null) {
                currentInTransitData.remove(indexOfMin);
                blockReaderList.remove(indexOfMin);
                br.close();
                return;
            }
            String[] fileLineData = topStringRead.split(":", 2);
            List<String> postingList = Arrays.stream(fileLineData[1].trim().substring(1, fileLineData[1].length() - 1)
                    .split(",")).collect(Collectors.toList());
            SortedMap<Integer, Integer> postingListMap = new TreeMap<>();
            for (String postingListValue : postingList) {
                String[] keyValue = postingListValue.split("=");
                postingListMap.put(Integer.valueOf(keyValue[0].trim()), Integer.valueOf(keyValue[1].trim()));
            }
            postingListId.set(indexOfMin, postingListMap);
            keyBuffer.set(indexOfMin, fileLineData[0].trim());
        }
    }

    /**
     * Process cleaning and normalize steps content of single file body text upto limit of
     * 500 docs and write the block into disk.
     *
     * @param fileContent Content of current Reading File
     */
    private void storeSingleFileDocsWithContent(String fileContent, String indexName) {
        try {
            int newDocId = aiDocNumber;
            if (!fileContent.trim().equals("")) {
                String[] cleanContentTokens = Constants.getCleanText(fileContent).split(" ");
                List<String> tokens = Arrays.stream(cleanContentTokens).filter(s -> !s.equalsIgnoreCase(""))
                        .collect(Collectors.toList());
                tokens.removeAll(Constants.stopWords);
                for (String token : tokens) {
                    if (AILoadFileContent.containsKey(token)) {
                        SortedMap<Integer, Integer> tupleWithDocIAndTokenCount = AILoadFileContent.get(token);
                        if (tupleWithDocIAndTokenCount.containsKey(newDocId)) {
                            int termCount = tupleWithDocIAndTokenCount.get(newDocId);
                            termCount++;
                            tupleWithDocIAndTokenCount.put(newDocId, termCount);
                        } else {
                            tupleWithDocIAndTokenCount.put(newDocId, 1);
                        }
                        AILoadFileContent.put(token, tupleWithDocIAndTokenCount);
                    } else {
                        SortedMap<Integer, Integer> tupleWithDocIAndTokenCount = new TreeMap<>();
                        tupleWithDocIAndTokenCount.put(newDocId, 1);
                        if (aiRelatedTerms.contains(token)) {
                            AILoadFileContent.put(token, tupleWithDocIAndTokenCount);
                        }
                    }
                }
                corpusRead++;
            }
            if (corpusRead >= Constants.DOC_TO_READ) {
                BlockDictionary blockDictionary = new BlockDictionary();
                blockDictionary.setLoadFileContent(AILoadFileContent);
                writeDictionaryFile(corpusRead, blockNo, blockDictionary);
                AILoadFileContent.clear();
                blockNo++;
                corpusRead = 0;
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Oops!! Array bound exception triggered!! " + e.getLocalizedMessage());
        }
        System.gc();
    }

    /**
     * Process cleaning and normalize steps content of single file body text upto limit of
     * 500 docs and write the block into disk.
     *
     * @param fileContent Content of current Reading File
     */
    private void storeSingleFileDocsWithContentConcordAI(String fileContent, String indexName) {
        try {
            int newDocId = concordiaDocNumber;
            if (!fileContent.trim().equals("")) {
                String[] cleanContentTokens = Constants.getCleanText(fileContent).split(" ");
                List<String> tokens = Arrays.stream(cleanContentTokens).filter(s -> !s.equalsIgnoreCase(""))
                        .collect(Collectors.toList());
                tokens.removeAll(Constants.stopWords);
                if (indexName.equalsIgnoreCase(Constants.CONCORDIA_AI_INDEX_NAME)) {
                    noOfTokensInReutersCollectionBodyTitleTag += fileContent.length();
                    docsMappingWithNoOfTokens.put(newDocId, fileContent.length());
                }
                for (String token : tokens) {
                    if (loadFileContent.containsKey(token)) {
                        SortedMap<Integer, Integer> tupleWithDocIAndTokenCount = loadFileContent.get(token);
                        if (tupleWithDocIAndTokenCount.containsKey(newDocId)) {
                            int termCount = tupleWithDocIAndTokenCount.get(newDocId);
                            termCount++;
                            tupleWithDocIAndTokenCount.put(newDocId, termCount);
                        } else {
                            tupleWithDocIAndTokenCount.put(newDocId, 1);
                        }
                        loadFileContent.put(token, tupleWithDocIAndTokenCount);
                    } else {
                        SortedMap<Integer, Integer> tupleWithDocIAndTokenCount = new TreeMap<>();
                        tupleWithDocIAndTokenCount.put(newDocId, 1);
                        loadFileContent.put(token, tupleWithDocIAndTokenCount);
                    }
                }
                corpusRead++;
            }
            if (corpusRead >= Constants.DOC_TO_READ) {
                BlockDictionary blockDictionary = new BlockDictionary();
                blockDictionary.setLoadFileContent(loadFileContent);
                writeDictionaryFile(corpusRead, blockNo, blockDictionary);
                loadFileContent.clear();
                blockNo++;
                corpusRead = 0;
            }
        } catch (IndexOutOfBoundsException e) {
            System.err.println("Oops!! Array bound exception triggered!! " + e.getLocalizedMessage());
        }
        System.gc();
    }

    /**
     * Simply write the intermediate block which is dictionary into disk.
     *
     * @param documentRead    Number of document read
     * @param blockNo         Current Block No
     * @param blockDictionary Intermediate Dictionary of Collected Terms
     */
    private void writeDictionaryFile(int documentRead, int blockNo, BlockDictionary blockDictionary) {
        SortedMap<String, SortedMap<Integer, Integer>> loadFileContent = blockDictionary.getLoadFileContent();
        System.out.println(documentRead +
                " documents are process and its being written into disk with block no. " + blockNo + "");
        String blockName = "blocks/block_" + blockNo + ".txt";
        try {
            Path path = Paths.get("blocks");
            createAndWriteContentTooFile(loadFileContent, blockName, path);
        } catch (IOException e) {
            System.err.println("Issue occur while writing blocks: " + e.getMessage());
        }
    }

    /**
     * Simply write the inverted index into disk.
     *
     * @param invertedDict Inverted Index of Max 25k Terms
     */
    private void writeInvertedIndexToFile(SortedMap<String, SortedMap<Integer, Integer>> invertedDict, String indexName) {
        try {
            String invertedIndexFileName = "invertedIndex/" + indexName + ".txt";
            Path path = Paths.get("invertedIndex");
            createAndWriteContentTooFile(invertedDict, invertedIndexFileName, path);
            if (indexName.equalsIgnoreCase(Constants.CONCORDIA_AI_INDEX_NAME))
                createEntryInSearchPointerList(invertedDict, invertedIndexFileName);
            invertedIndex++;
        } catch (IOException e) {
            System.err.println("Issue occur while writing blocks: " + e.getMessage());
        }
    }

    /**
     * This is function is responsible to register the global pointer which is use during query time,
     * as it can answer which letter is in which block
     *
     * @param invertedDict          Inverted Index of Max 25k Terms
     * @param invertedIndexFileName Inverted Index File name
     */
    private void createEntryInSearchPointerList(SortedMap<String, SortedMap<Integer, Integer>> invertedDict, String invertedIndexFileName) {
        int minAscii = (int) invertedDict.firstKey().toCharArray()[0];
        int maxAscii = (int) invertedDict.lastKey().toCharArray()[0];
        searchPointers.add(new SearchPointer(invertedIndexFileName, minAscii, maxAscii));
    }

    /**
     * Actual Perform I/O operation for writing content from memory to disk.
     *
     * @param invertedDict          Inverted Index of Max 25k Terms
     * @param invertedIndexFileName Inverted Index File name
     * @param path                  Path of storing file.
     * @throws IOException Exception related to I/O operation
     */
    private void createAndWriteContentTooFile(SortedMap<String, SortedMap<Integer, Integer>> invertedDict, String invertedIndexFileName, Path path) throws IOException {
        if (!Files.exists(path))
            Files.createDirectory(path);
        File file = new File(invertedIndexFileName);
        if (!file.exists()) {
            // File created status
            file.createNewFile();
        }
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(invertedIndexFileName));
        for (Map.Entry<String, SortedMap<Integer, Integer>> sortedSetEntry : invertedDict.entrySet()) {
            String content = sortedSetEntry.getKey() + ":" + sortedSetEntry.getValue().toString();
            bufferedWriter.write(content + "\n");
        }
        bufferedWriter.close();
    }

    private void getTheasurusData() {
        try (BufferedReader br = new BufferedReader(new FileReader("aithesaures.txt"))) {
            String line = "";
            while ((line = br.readLine()) != null) {
                String[] tokens = line.trim().split(" ");
                Collections.addAll(aiRelatedTerms, tokens);
            }
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }


    // Setting up the variables of in-memory
    private void setUpDocsWithNoOfTokens() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("in_memory/docsTokenMapping.txt"))) {
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                String[] data = line.split(",", 2);
                docsMappingWithNoOfTokens.put(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Setting up the variables of in-memory
    private void setUpDocWithDynamicMapping() {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader("in_memory/mapping.txt"))) {
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                String[] data = line.split(",", 2);
                ConcordiafilesMapping.put(Integer.parseInt(data[0]), data[1].trim());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeDocMappingWithNoOfTokens() {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("in_memory/docsTokenMapping.txt"))) {
            for (Map.Entry data : docsMappingWithNoOfTokens.entrySet()) {
                bufferedWriter.write(data.getKey() + "," + data.getValue() + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
