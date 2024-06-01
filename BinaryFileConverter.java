/*
To compile and run this JavaFX application on a Debian-based system, you need to follow these steps:

1. Install OpenJDK 11 or higher and JavaFX. You can do this using the following commands:
   sudo apt update
   sudo apt install openjdk-17-jdk
   sudo apt install openjfx

2. Save this file with a .java extension, for example, `Main.java`.

3. Compile the Java file using the Java compiler (`javac`). You need to specify the path to the JavaFX SDK using the `--module-path` option, and you need to specify the modules your program uses with the `--add-modules` option. Run the following command:
   javac --module-path /usr/share/openjfx/lib --add-modules javafx.controls Main.java
   Replace `/usr/share/openjfx/lib` with the path to your `lib` directory in the JavaFX SDK, if it's different.

4. Run the program using the Java launcher, again specifying the module path and the modules:
   java --module-path /usr/share/openjfx/lib --add-modules javafx.controls Main
   Replace `/usr/share/openjfx/lib` with the path to your `lib` directory in the JavaFX SDK, if it's different.
*/

import javafx.application.Application;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.control.ComboBox;
import javafx.util.StringConverter;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.control.ComboBox;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Priority;
import javafx.concurrent.Task;

public class BinaryFileConverter extends Application {

    private File sourceFile;
    private File destinationFile;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Binary File Converter");

        FileChooser fileChooser = new FileChooser();

        Button openButton = new Button("Source File");
        Label openLabel = new Label();
        Button saveButton = new Button("Destination File");
        Label saveLabel = new Label();
        Button processButton = new Button("Start Process");
        TextArea messageArea = new TextArea();
        messageArea.setEditable(false);
        messageArea.setWrapText(true);

        messageArea.textProperty().addListener(new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
                messageArea.setScrollTop(Double.MAX_VALUE); //this will scroll to the bottom
            }
        });

        ComboBox<String> delimiterBox = new ComboBox<>();
        delimiterBox.setPromptText("Delimiter (Optional)");
        delimiterBox.setItems(FXCollections.observableArrayList(" ", "\n", "\t", ",", ":", "|", ""));

        Label chunkSizeLabel = new Label("Chunk Size (Bytes):");
        TextField byteField = new TextField("1");

        delimiterBox.setConverter(new StringConverter<String>() {
            @Override
            public String toString(String object) {
                switch (object) {
                    case " ":
                        return "Empty Space";
                    case "\n":
                        return "Line Ending";
                    case "\t":
                        return "Tab";
                    case ",":
                        return "Comma";
                    case ":":
                        return "Colon";
                    case "|":
                        return "|";
                    case "":
                        return "No delimiter";
                    default:
                        return "";
                }
            }
        
            @Override
            public String fromString(String string) {
                switch (string) {
                    case "Empty Space":
                        return " ";
                    case "Line Ending":
                        return "\n";
                    case "Tab":
                        return "\t";
                    case "Comma":
                        return ",";
                    case "Colon":
                        return ":";
                    case "|":
                        return "|";
                    case "No delimiter":
                        return "";
                    default:
                        return "";
                }
            }
        });

        AtomicBoolean processRunning = new AtomicBoolean(false);

        openButton.setOnAction(e -> {
            sourceFile = fileChooser.showOpenDialog(primaryStage);
            if (sourceFile != null) {
                openLabel.setText(sourceFile.getPath());
            }
        });

        saveButton.setOnAction(e -> {
            destinationFile = fileChooser.showSaveDialog(primaryStage);
            if (destinationFile != null) {
                saveLabel.setText(destinationFile.getPath());
            }
        });

        processButton.setOnAction(e -> {
            if (processRunning.get()) {
                processRunning.set(false);
                processButton.setText("Start Process");
                messageArea.appendText("The process was cancelled before completing.\n");
                messageArea.setScrollTop(Double.MAX_VALUE);
            } else {
                if (sourceFile == null || destinationFile == null) {
                    messageArea.appendText("Please select both source and destination files before starting the process.\n");
                    messageArea.setScrollTop(Double.MAX_VALUE);
                } else {
                    processRunning.set(true);
                    processButton.setText("Cancel Process");
                    int byteCount = Integer.parseInt(byteField.getText());
                    String delimiter = delimiterBox.getValue();
                    long totalChunks = (sourceFile.length() + byteCount - 1) / byteCount; // calculate total chunks
        
                    Task<Void> task = new Task<Void>() {
                        @Override
                        protected Void call() throws Exception {
                            long currentChunk = 0;
                            try (InputStream in = new FileInputStream(sourceFile);
                                 PrintWriter out = new PrintWriter(destinationFile)) {
                                byte[] buffer = new byte[byteCount];
                                int bytesRead;
                                while ((bytesRead = in.read(buffer)) != -1) {
                                    currentChunk++;
                                    updateMessage("Processing chunk " + currentChunk + " out of " + totalChunks + "\n");
                                    String binaryString = toBinary(buffer, bytesRead);
                                    out.print(binaryString);
                                    if (bytesRead == byteCount) {
                                        out.print(delimiter);
                                    }
                                    if (!processRunning.get()) {
                                        break;
                                    }
                                }
                                processRunning.set(false);
                                processButton.setText("Re-run Process");
                                updateMessage("Process completed.\n");
                            } catch (IOException ex) {
                                updateMessage("An error occurred: " + ex.getMessage() + "\n");
                            }
                            return null;
                        }
                    };
        
                    messageArea.textProperty().bind(task.messageProperty());
                    new Thread(task).start();
                }
            }
        });

        MenuBar menuBar = new MenuBar();
        Menu menuMain = new Menu("General Controls");
        MenuItem closeApp = new MenuItem("Close Application");
        closeApp.setOnAction(e -> System.exit(0));
        menuMain.getItems().add(closeApp);
        menuBar.getMenus().add(menuMain);

        HBox openBox = new HBox(openButton, openLabel);
        HBox saveBox = new HBox(saveButton, saveLabel);
        HBox chunkSizeBox = new HBox(chunkSizeLabel, byteField);
        VBox topBox = new VBox(menuBar, openBox, saveBox, chunkSizeBox, delimiterBox, new Separator());

        BorderPane borderPane = new BorderPane();
        borderPane.setTop(topBox);
        borderPane.setCenter(messageArea);
        
        HBox bottomBox = new HBox(new Separator(), processButton);
        HBox.setHgrow(processButton, Priority.ALWAYS);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        borderPane.setBottom(bottomBox);

        Scene scene = new Scene(borderPane, 500, 200);

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private String toBinary(byte[] bytes, int length) {
        StringBuilder binary = new StringBuilder();
        for (int i = 0; i < length; i++) {
            String binString = Integer.toBinaryString(bytes[i] & 255);
            while (binString.length() < 8) { // pad with leading zeros
                binString = "0" + binString;
            }
            binary.append(binString);
        }
        return binary.toString();
    }
}