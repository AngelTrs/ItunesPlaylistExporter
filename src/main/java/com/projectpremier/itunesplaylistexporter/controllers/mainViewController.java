package com.projectpremier.itunesplaylistexporter.controllers;

import com.dd.plist.*;
import com.projectpremier.itunesplaylistexporter.Song;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;


import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;


/**
 * FXML Controller class for main orders sView.
 * reads orders for selected customer to a table view and provides
 * gateway for user to edit an order or create a new one.
 *
 * @author Angel Torres
 */
public class mainViewController implements Initializable {

    @FXML
    private ListView<String> nPlaylistList;
    @FXML
    private TextField nItunesFolderText;
    @FXML
    private TextField nSaveFolderText;

    @FXML
    private Button nItunesDirectoryButton;
    @FXML
    private Button nSaveDirectoryButton;
    @FXML
    private Button nAutoDetectButton;
    @FXML
    private Button nFetchButton;
    @FXML
    private Button nTransferButton;

    @FXML
    private Label nStatusLabel;


    // Data Holders
    private File mItunesFile;
    private File mSaveFile;
    private ObservableList<String> mPlaylistBackList;
    private Map mSongMap;
    private ArrayList<String> mSongList;
    private Map mPlaylistsMap;
    private String mMusicFolder;

    public BufferedWriter mWriter;


    @Override
    public void initialize(URL location, ResourceBundle resources) {

        mItunesFile = null;
        mSaveFile = null;

        mSongList = new ArrayList<>();

        mSongMap = new HashMap<String, Song>();
        mPlaylistsMap = new HashMap<String, ArrayList<NSObject>>();

        mPlaylistBackList = FXCollections.observableArrayList();
        nPlaylistList.setItems(mPlaylistBackList);

        nPlaylistList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                nStatusLabel.setText("");
            }
        });
    }


    // (Click) Action Methods

    @FXML
    public void chooseItunesDirClick() {
        resetFile();

        Stage mNewStage = new Stage();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        String str = directoryChooser.showDialog(mNewStage).toString();

        str += "/iTunes Music Library.xml";
        File file = new File(str);
        mItunesFile = file;
        if (mItunesFile != null)
            nItunesFolderText.setText(mItunesFile.getAbsolutePath());
    }

    @FXML
    public void chooseSaveDirClick() {

        Stage mNewStage = new Stage();
        DirectoryChooser directoryChooser = new DirectoryChooser();
        mSaveFile = directoryChooser.showDialog(mNewStage);
        if (mSaveFile != null)
            nSaveFolderText.setText(mSaveFile.getAbsolutePath());
    }

    @FXML
    private void autoDetectClick() {
        resetFile();

        // Open the xml file
        String str = null;
        if (System.getProperty("os.name").startsWith("Mac OS X")) {
            //TODO Add OSX exception
            //http://developer.apple.com/library/mac/#documentation/Java/Conceptual/Java14Development/00-Intro/JavaDevelopment.html
            str = System.getProperty("user.home") + "/Music/iTunes/";


        } else if (System.getProperty("os.name").startsWith("Windows")) {
            float ver = Float.valueOf(System.getProperty("os.version"));

            if (ver >= 5 && ver < 6) { //For Windows 2000 & XP (Ver 5 & 5.1)
                str = System.getProperty("user.home") + "/My Documents/My Music/iTunes/";
            } else if (ver >= 6 && ver < 6.1) { //For Windows Vista (Ver 6)
                str = System.getProperty("user.home") + "/Music/iTunes/";
            } else if (ver >= 6.1 && ver < 6.3) { //For Windows 7 & 8 (Ver 6.1 & 6.2)
                str = System.getProperty("user.home") + "/My Music/iTunes/";
            } else {
                str = "Unsupported version of Windows detected!\n" +
                        "Unknown results are expected.";
                //throw new FileNotFoundException("Your system is not supported.");
            }
        } else {
            // str = "Unsupported Operating System detected!\n";
        }

        str += "iTunes Music Library.xml";
        mItunesFile = new File(str);
        nItunesFolderText.setText(mItunesFile.getAbsolutePath());

    }

    @FXML
    private void fetchDataClick() throws Exception {

        if (mItunesFile != null && mSaveFile != null && mItunesFile.exists() && mSaveFile.exists()) {

            File file = mItunesFile;

            NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(file);
            String[] strings = rootDict.allKeys();

            for (int i = 0; i < strings.length; i++) {

                // Process Tracks Portion of ITunes PList
                if (strings[i].equals("Tracks")) {

                    // Pull NSDictionary of the Tracks section of PList
                    NSDictionary tempNSDictionary = (NSDictionary) rootDict.get("Tracks");

                    // Iterate through NSDictionary via entry set like HashMap it implements
                    Iterator it = tempNSDictionary.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry pair = (Map.Entry) it.next();

                        NSDictionary trackInfoNSDictionary = (NSDictionary) pair.getValue();
                        if (trackInfoNSDictionary.get("Kind") != null && trackInfoNSDictionary.get("Track Type") != null) {
                            String kind = (String) trackInfoNSDictionary.get("Kind").toJavaObject();
                            String type = (String) trackInfoNSDictionary.get("Track Type").toJavaObject();

                            if (type.equals("File") && kind.contains("audio")) {

                                String songId = (String) pair.getKey();
                                String songTitle = (String) trackInfoNSDictionary.get("Name").toJavaObject();
                                System.out.println(songTitle);
                                String songArtist = (String) trackInfoNSDictionary.get("Artist").toJavaObject();
                                int songDuration = (int) trackInfoNSDictionary.get("Total Time").toJavaObject();
                                String fileLocation = (String) trackInfoNSDictionary.get("Location").toJavaObject();

                                int songDurationSeconds = songDuration / 1000;

                                mSongList.add(fileLocation);

                                Song song = new Song(songId, songTitle, songArtist, songDurationSeconds, fileLocation);
                                mSongMap.put(songId, song);
                            }
                        }

                        it.remove(); // avoids a ConcurrentModificationException
                    }

                } else if (strings[i].equals("Playlists")) {

                    NSArray plNSArray = (NSArray) rootDict.get("Playlists");

                    // Loop through every Playlist dictionary
                    for (int j = 0; j < plNSArray.count(); j++) {

                        NSDictionary tempPLTrackNSDictionary = (NSDictionary) plNSArray.objectAtIndex(j);

                        if ((!tempPLTrackNSDictionary.containsKey("Smart Info")) &&
                                (!tempPLTrackNSDictionary.containsKey("Distinguished Kind"))
                                && (!tempPLTrackNSDictionary.get("Name").toJavaObject().equals("Library"))) {

                            String name = tempPLTrackNSDictionary.get("Name").toString();
                            NSArray playListItemsArray = (NSArray) tempPLTrackNSDictionary.get("Playlist Items");

                            ArrayList<String> tracksPLList = new ArrayList<>();

                            System.out.println(playListItemsArray.count());

                            for (int k = 0; k < playListItemsArray.count(); k++) {
                                NSDictionary tempDict = (NSDictionary) playListItemsArray.objectAtIndex(k);
                                String trackId = String.valueOf(tempDict.get("Track ID"));
                                tracksPLList.add(trackId);
                            }

                            mPlaylistsMap.put(name, tracksPLList);
                            mPlaylistBackList.add(name);
                        }
                    }
                } else if (strings[i].equals("Music Folder")) mMusicFolder = rootDict.get(strings[i]).toString();
            }

            nFetchButton.setDisable(true);
            nTransferButton.setDisable(false);
            nItunesDirectoryButton.setDisable(true);
            nSaveDirectoryButton.setDisable(true);
            nAutoDetectButton.setDisable(true);
        } else {
            System.out.println("FILE NOT FIND");
        }
    }

    @FXML
    private void transferClick() throws IOException {

        String selectedPlaylist = nPlaylistList.getSelectionModel().getSelectedItem();

        ArrayList<String> selectedTracks = (ArrayList) mPlaylistsMap.get(selectedPlaylist);

        File theDir = new File(mSaveFile.getAbsolutePath() + "/_" + selectedPlaylist);

        // if the directory does not exist, create it
        if (!theDir.exists()) {
            System.out.println("creating directory: " + theDir.getName());
            boolean result = false;

            try {
                theDir.mkdir();
                result = true;
            } catch (SecurityException se) {
                //handle it
                System.out.println("ERROR");
            }
        }

        // begin writing playlist file
        // FUNCTIONALITY REMOVED
       /* Path newPlaylistFile = Paths.get(mSaveFile.getAbsolutePath() + "/" + selectedPlaylist + "/" + selectedPlaylist + ".m3u");
        mWriter = Files.newBufferedWriter(newPlaylistFile, StandardCharsets.UTF_8);
        mWriter.write("#EXTM3U\n"); */


        for (String trackOfPlaylist : selectedTracks) {
            Song song = (Song) mSongMap.get(trackOfPlaylist);

            // copy mp3 file to new folder location
            String src = song.getPath();
            //src = src.replace("%20", " ");
            //src = src.replace("a%CC%81", "á");
            //src = src.replace("o%CC%81", "ó");
            src = URLDecoder.decode(src, "UTF-8");
            src = src.replace("file://", "");
            File srcFile = new File(src);
            String dest = theDir.getAbsolutePath() + "/" + srcFile.getName();
            File destFile = new File(dest);

            if (!destFile.exists()) {
                Files.copy(srcFile.toPath(), destFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            }


            // add to song text to playlist
            // FUNCTIONALITY REMOVED
         /*   mWriter.write("#EXTINF:" + song.getDurationSeconds() + "," + song.getArtist() + " - " + song.getTitle() + "\n");
            mWriter.write(destFile.getName() + "\n");*/


        }
        //mWriter.close();

        nStatusLabel.setText("Done!");
    }

    @FXML
    private void quitClick() {
        System.exit(0);
    }


    // Process

    private void resetFile() {
        nItunesFolderText.clear();
        mItunesFile = null;
    }
}