package com.jaydigilstrap.sentiment;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.CSVLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class SentimentAnalysisApp {

    private JFrame frame;
    private JTextField filePathField;
    private JTextArea resultsArea;
    private File selectedFile;

    public public SentimentAnalysisApp() {
        createGUI();
    }

    // Creates the Java Swing graphical user interface.
    private void createGUI() {

        frame = new JFrame("Sentiment Analysis Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));

        // Displays the selected CSV file path.
        filePathField = new JTextField();
        filePathField.setEditable(false);

        JButton browseButton = new JButton("Browse");
        JButton analyzeButton = new JButton("Analyze");

        topPanel.add(filePathField, BorderLayout.CENTER);
        topPanel.add(browseButton, BorderLayout.EAST);

        // Displays the detailed sentiment analysis results.
        resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setLineWrap(true);
        resultsArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(resultsArea);

        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(analyzeButton, BorderLayout.SOUTH);

        // Opens the CSV file chooser.
        browseButton.addActionListener(e -> chooseCSVFile());

        // Begins the sentiment analysis process.
        analyzeButton.addActionListener(e -> analyzeFile());

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // Opens a file browser filtered to only display CSV files.
    private void chooseCSVFile() {

        JFileChooser fileChooser = new JFileChooser();

        FileNameExtensionFilter filter =
                new FileNameExtensionFilter("CSV Files (*.csv)", "csv");

        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(frame);

        if (result == JFileChooser.APPROVE_OPTION) {

            selectedFile = fileChooser.getSelectedFile();

            // Display the selected file path.
            filePathField.setText(selectedFile.getAbsolutePath());
        }
    }

    // Loads the CSV file, processes the review text with Weka,
    // classifies each review, and displays the results.
    private void analyzeFile() {

        if (selectedFile == null) {

            JOptionPane.showMessageDialog(
                    frame,
                    "Please select a CSV file first."
            );

            return;
        }

        try {

            // Load the selected CSV file using Weka's CSVLoader.
            CSVLoader loader = new CSVLoader();
            loader.setSource(selectedFile);

            // Store the CSV file contents as a Weka Instances dataset.
            Instances data = loader.getDataSet();

            // Use Weka's StringToWordVector filter to process the text.
            StringToWordVector wordVectorFilter =
                    new StringToWordVector();

            // The first attribute contains the review text.
            wordVectorFilter.setAttributeIndices("1");

            // Convert words to lowercase for consistent processing.
            wordVectorFilter.setLowerCaseTokens(true);

            // Prepare the filter using the review dataset.
            wordVectorFilter.setInputFormat(data);

            // Convert text into numerical word-based features.
            Instances processedData =
                    Filter.useFilter(data, wordVectorFilter);

            // Make sure the CSV file contains review data.
            if (processedData.numInstances() == 0) {

                resultsArea.setText(
                        "No reviews were found in the selected file."
                );

                return;
            }

            int positiveCount = 0;
            int negativeCount = 0;
            int subjectiveCount = 0;

            StringBuilder output = new StringBuilder();

            // Analyze every review in the CSV file.
            for (int i = 0; i < data.numInstances(); i++) {

                Instance instance = data.instance(i);

                // Retrieve the original review text.
                String review = instance.stringValue(0);

                // Determine the sentiment of the review.
                String sentiment = classifySentiment(review);

                if (sentiment.equals("Positive")) {
                    positiveCount++;
                } else {
                    negativeCount++;
                }

                // Determine whether the review contains opinion language.
                if (containsOpinionWord(review)) {
                    subjectiveCount++;
                }

                // Display the detailed row-by-row analysis.
                output.append("Review ")
                        .append(i + 1)
                        .append(":\n");

                output.append("Text: ")
                        .append(review)
                        .append("\n");

                output.append("Sentiment: ")
                        .append(sentiment)
                        .append("\n\n");
            }

            int totalReviews = data.numInstances();

            // Calculate the positive review percentage.
            double positivePercentage =
                    ((double) positiveCount / totalReviews) * 100;

            // Calculate the negative review percentage.
            double negativePercentage =
                    ((double) negativeCount / totalReviews) * 100;

            // Polarity ranges from -1 to 1.
            double polarity =
                    ((double) positiveCount - negativeCount)
                            / totalReviews;

            // Subjectivity ranges from 0 to 1.
            double subjectivity =
                    (double) subjectiveCount / totalReviews;

            // Create the sentiment summary required by the assignment.
            output.append("=== SENTIMENT SUMMARY ===\n\n");

            output.append(String.format(
                    "Polarity Score: %.2f\n",
                    polarity
            ));

            output.append(String.format(
                    "Subjectivity Score: %.2f\n\n",
                    subjectivity
            ));

            output.append(String.format(
                    "Positive Reviews: %d (%.1f%%)\n",
                    positiveCount,
                    positivePercentage
            ));

            output.append(String.format(
                    "Negative Reviews: %d (%.1f%%)\n",
                    negativeCount,
                    negativePercentage
            ));

            resultsArea.setText(output.toString());

            // Move the results view back to the beginning.
            resultsArea.setCaretPosition(0);

        } catch (Exception ex) {

            resultsArea.setText(
                    "Error analyzing file:\n" +
                    ex.getMessage()
            );

            ex.printStackTrace();
        }
    }

    // Classifies a review as positive or negative
    // based on common sentiment-related words.
    private String classifySentiment(String review) {

        String text = review.toLowerCase();

        String[] positiveWords = {
                "excellent",
                "love",
                "great",
                "happy",
                "amazing",
                "good",
                "wonderful",
                "best",
                "fantastic",
                "quality"
        };

        String[] negativeWords = {
                "terrible",
                "hate",
                "waste",
                "bad",
                "awful",
                "poor",
                "worst",
                "horrible",
                "disappointed",
                "broken"
        };

        int positiveScore = 0;
        int negativeScore = 0;

        // Count positive sentiment words.
        for (String word : positiveWords) {

            if (text.contains(word)) {
                positiveScore++;
            }
        }

        // Count negative sentiment words.
        for (String word : negativeWords) {

            if (text.contains(word)) {
                negativeScore++;
            }
        }

        if (positiveScore >= negativeScore) {
            return "Positive";
        }

        return "Negative";
    }

    // Checks whether a review contains opinion-based language
    // for calculating the subjectivity score.
    private boolean containsOpinionWord(String review) {

        String text = review.toLowerCase();

        String[] opinionWords = {
                "love",
                "hate",
                "excellent",
                "terrible",
                "happy",
                "amazing",
                "great",
                "bad",
                "awful",
                "wonderful",
                "best",
                "worst",
                "fantastic",
                "disappointed"
        };

        for (String word : opinionWords) {

            if (text.contains(word)) {
                return true;
            }
        }

        return false;
    }

    // Starts the Swing application.
    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                () -> new SentimentAnalysisApp();
        );
    }
}
