package dirsizerecognizer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

/**
 * defines the main behavior of the program
 */
public class Controller implements Initializable {
	private final static long DEFAULT_MIN_DIR_SIZE = 100_000_000;
	private final static int DEFAULT_QTY_SUBDIR = 3;

	@FXML
	private TextField directory;
	@FXML
	private TextField minDirSize;
	@FXML
	private TextField qtySubdir;
	@FXML
	private TextArea mainTextArea;
	@FXML
	private TextArea messageTextArea;
	@FXML
	private Label readableSize;
	@FXML
	private RadioButton sizeRadioButton;
	@FXML
	private RadioButton pathRadioButton;
	@FXML
	private RadioButton dirStrucRadioButton;
	@FXML
	private Button start;

	private ArrayList<Map<String, String>> list = new ArrayList<Map<String, String>>();
	private DirectoryChooser directoryChooser = new DirectoryChooser();
	private StringBuilder stringBuilder = new StringBuilder();
	private int counter;
	private boolean stopFlag;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		minDirSize.setText(Long.toString(DEFAULT_MIN_DIR_SIZE));
		qtySubdir.setText(Integer.toString(DEFAULT_QTY_SUBDIR));
		readableSize.setText(GetReadableSize(minDirSize.getText()));
		minDirSize.setOnKeyTyped(e -> readableSize.setText(GetReadableSize(minDirSize.getText())));
		directory.setOnKeyTyped(e -> checkIsDir());
		sizeRadioButton.setSelected(true);
		start.setDisable(true);
		directoryChooser.setTitle("Select directory");
	}

	@FXML
	private void selectDirectory() {
		try {
			directory.setText(directoryChooser.showDialog(new Stage()).getAbsolutePath());
		} catch (Exception e) {
			directory.setText("");
		}
		checkIsDir();
	}

	@FXML
	private void startToRecognize() {
		new Thread(() -> {
			counter = 0;
			stopFlag = false;
			start.setDisable(true);
			list.clear();
			appendTextarea("\n New search... \n\n");
			messageTextArea.setText("Searching... " + stringBuilder.toString());
			processFilesFromFolder(new File(stringBuilder.toString()), 0);
			messageTextArea.setText("");
			printSortedDirs();
			start.setDisable(false);
		}).start();
	}

	@FXML
	private void clear() {
		messageTextArea.setText("");
		mainTextArea.setText("");
		list.clear();
	}

	@FXML
	private void stop() {
		stopFlag = true;
	}

	/**
	 * checks that the entered string is an existing directory
	 */
	private void checkIsDir() {
		stringBuilder.replace(0, stringBuilder.length(), directory.getText());
		try {
			if (Files.isDirectory(Paths.get(stringBuilder.toString()))
					&& !stringBuilder.toString().trim().equals("")) {
				start.setDisable(false);
				messageTextArea.setText("");
			} else
				setBadDir();
		} catch (Exception e) {
			setBadDir();
		}
	}

	/**
	 * determines the behavior of the program if an incorrect directory is entered
	 */
	private void setBadDir() {
		messageTextArea.setText("\"" + stringBuilder.toString() + "\"" + " is not the directory!");
		start.setDisable(true);
	}

	/**
	 * defines the basic process of finding suitable directories
	 * 
	 * @param folder
	 * @param lvlSubdir
	 */
	private void processFilesFromFolder(File folder, int lvlSubdir) {
		File[] folderEntries = folder.listFiles();
		long size;
		String strSize;

		if (folder.exists()) {
			size = GetFolderSize(folder.toPath());
			if (size >= Long.parseLong(minDirSize.getText())) {
				Map<String, String> tempMap = new HashMap<>();
				strSize = String.format("%-13s", GetReadableSize(Long.toString(size))) + folder;
				tempMap.put("size", strSize);
				tempMap.put("bySize", String.format("%019d", size) + folder);
				tempMap.put("byPath", folder.toString());
				tempMap.put("byDirStruc", String.format("%019d", counter));
				list.add(tempMap);
				counter++;
				appendTextarea(strSize + "\n");
				messageTextArea.setText("Searching... " + "\"" + folder + "\"");
				if (lvlSubdir < Integer.parseInt(qtySubdir.getText())) {
					lvlSubdir++;
					folderEntries = folder.listFiles();
					if (folderEntries.length > 0) {
						for (File entry : folderEntries) {
							if (entry.isDirectory())
								processFilesFromFolder(entry, lvlSubdir);
						}
					}
				}
			}
		}
	}


	/**
	 * defines output behavior of found directories
	 */
	@FXML
	private void printSortedDirs() {
		if (!list.isEmpty()) {
			Map<String, String> tempMap = new TreeMap<>();
			for (Map<String, String> entry : list) {
				if (sizeRadioButton.isSelected()) {
					tempMap.put(entry.get("bySize"), entry.get("size"));
				} else if (pathRadioButton.isSelected())
					tempMap.put(entry.get("byPath"), entry.get("size"));
				else if (dirStrucRadioButton.isSelected()) {
					tempMap.put(entry.get("byDirStruc"), entry.get("size"));
				}
			}
			if (sizeRadioButton.isSelected()) {
				appendTextarea("\n Sorted results by size: \n\n");
			} else if (pathRadioButton.isSelected())
				appendTextarea("\n Sorted results by path: \n\n");
			else if (dirStrucRadioButton.isSelected()) {
				appendTextarea("\n Sorted results by dir structure: \n\n");
			}
			for (String o : tempMap.keySet()) {
				appendTextarea(tempMap.get(o) + "\n");
			}
		} else
			appendTextarea("\n no data \n\n");
	}

	/**
	 * gets the size of the directory
	 * 
	 * @param folder
	 * @throws IOException
	 */
	private long GetFolderSize(Path folder) {
		AtomicLong size = new AtomicLong(0);

		try {
			Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					size.addAndGet(attrs.size());
					if (stopFlag)
						return FileVisitResult.TERMINATE;
					else
						return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException e) {
					return FileVisitResult.SKIP_SUBTREE;
				}
			});
		} catch (IOException e) {
			messageTextArea.setText("�an't determines the size " + "\"" + folder + "\"");
		}
		return size.longValue();
	}

	/**
	 * converts directory size to readable format
	 * 
	 * @param size
	 */
	public String GetReadableSize(String size) {
		size = size.trim();
		try {
			if (size.equals("") || Long.parseLong(size) == 0)
				return "0 B";
			else {
				String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
				long longSize = Long.parseLong(size);
				int unitIndex = (int) (Math.log10(longSize) / 3);
				double unitValue = 1 << (unitIndex * 10);
				return new DecimalFormat("#,##0.##").format(longSize / unitValue) + " "
						+ units[unitIndex];
			}
		} catch (Exception e) {
			return "0 B";
		}
	}

	/**
	 * determines the output to the form of basic information
	 * 
	 * @param message
	 */
	private void appendTextarea(String message) {
		if (Platform.isFxApplicationThread()) {
			mainTextArea.appendText(message);
		} else {
			Platform.runLater(() -> mainTextArea.appendText(message));
		}
	}
}
