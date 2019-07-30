package com.unclezs.UI.App;
/*
 *@author unclezs.com
 *@date 2019.06.22 14:48
 */

import com.unclezs.UI.Utils.DataManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;


public class Reader extends Application{
    public StageStyle stageStyle=StageStyle.DECORATED;

    public Reader() {
    }

    public Reader(StageStyle stageStyle) {
        this.stageStyle = stageStyle;
    }

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage readerStage) throws IOException {
        readerStage.initStyle(stageStyle);
        DataManager.readerStage=readerStage;
        readerStage.getIcons().add(new Image("images/图标/圆角图标.png"));
        readerStage.setMinHeight(350);
        readerStage.setMinWidth(250);
        FXMLLoader loader=new FXMLLoader();
        loader.setLocation(Main.class.getResource("/fxml/reader.fxml"));
        Pane root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/reader.css").toExternalForm());
        readerStage.setScene(scene);
        readerStage.show();
    }

}
