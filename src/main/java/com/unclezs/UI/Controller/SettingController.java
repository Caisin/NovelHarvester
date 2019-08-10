package com.unclezs.UI.Controller;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXRadioButton;
import com.jfoenix.controls.JFXToggleButton;
import com.unclezs.Mapper.SettingMapper;
import com.unclezs.Model.DownloadConfig;
import com.unclezs.UI.Node.ProgressFrom;
import com.unclezs.UI.Utils.AlertUtil;
import com.unclezs.UI.Utils.DataManager;
import com.unclezs.UI.Utils.ToastUtil;
import com.unclezs.Utils.ConfUtil;
import com.unclezs.Utils.MybatisUtils;
import com.unclezs.Utils.ProxyUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

/*
 *设置控制器
 *@author unclezs.com
 *@date 2019.07.07 11:52
 */
public class SettingController implements Initializable {
    @FXML
    JFXToggleButton merge, autoImport;
    @FXML
    JFXComboBox<Integer> chapterNum, delay;
    @FXML
    JFXRadioButton dmobi, depub, dtxt;
    @FXML
    JFXButton testProxy,saveProxy;
    @FXML
    TextField proxyPort,proxyHost;
    @FXML
    Label pathLabel, changePath;

    ToggleGroup group = new ToggleGroup();
    private static DownloadConfig config;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dmobi.setToggleGroup(group);
        dtxt.setToggleGroup(group);
        depub.setToggleGroup(group);
        initData();
        initEventHandler();
    }

    void initData() {//初始化数据
        for (int i = 0; i < 30; i++) {
            delay.getItems().add(i);
        }
        for (int i = 30; i < 1000; i += 5) {
            delay.getItems().add(i);
        }
        for (int i = 50; i < 10000; i += 100) {
            chapterNum.getItems().add(i);
        }
        SettingMapper mapper = MybatisUtils.getMapper(SettingMapper.class);
        config = mapper.querySetting();
        MybatisUtils.getCurrentSqlSession().close();
        merge.setSelected(config.isMergeFile());
        merge.setDisableVisualFocus(true);//禁用焦点过渡
        chapterNum.setValue(config.getPerThreadDownNum());
        delay.setValue(config.getSleepTime() / 1000);
        pathLabel.setText(config.getPath());
        autoImport.setSelected(Boolean.valueOf(ConfUtil.get(ConfUtil.USE_ANALYSIS_PASTE)));
        switch (config.getFormat()) {
            case "epub":
                depub.setSelected(true);
                break;
            case "txt":
                dtxt.setSelected(true);
                break;
            default:
                dmobi.setSelected(true);
                break;
        }
        //读取本地配置
        proxyPort.setText(ConfUtil.get(ConfUtil.PROXY_PORT));
        proxyHost.setText(ConfUtil.get(ConfUtil.PROXY_HOSTNAME));
    }

    //初始化事件监听
    void initEventHandler() {
        //值改变监听
        merge.selectedProperty().addListener(e -> {
            config.setMergeFile(merge.isSelected());
        });
        chapterNum.valueProperty().addListener(e -> {
            config.setPerThreadDownNum(chapterNum.getValue());
        });
        delay.valueProperty().addListener(e -> {
            config.setSleepTime(delay.getValue() * 1000);
        });
        changePath.setOnMouseClicked(e -> {
            //文件选择
            DirectoryChooser chooser = new DirectoryChooser();
            File dir = new File(config.getPath());
            if (dir.exists())
                chooser.setInitialDirectory(dir);
            chooser.setTitle("选择下载位置");
            File file = chooser.showDialog(DataManager.mainStage);
            //防空
            if (file == null || !file.exists()) {
                return;
            }
            //更新
            String path = file.getAbsolutePath() + "\\";
            pathLabel.setText(path);
            config.setPath(path);
        });
        dmobi.selectedProperty().addListener(e -> {
            config.setFormat("mobi");
        });
        dtxt.selectedProperty().addListener(e -> {
            config.setFormat("txt");
        });
        depub.selectedProperty().addListener(e -> {
            config.setFormat("epub");
        });
        autoImport.selectedProperty().addListener(e->{
            ConfUtil.set(ConfUtil.USE_ANALYSIS_PASTE,autoImport.isSelected()+"");
        });
        testProxy.setOnMouseClicked(e->{
            String host = proxyHost.getText();
            String port = proxyPort.getText();
            if(host!=null&&"".equals(host)&&port!=null&&"".equals(port)){
                ToastUtil.toast("请填先写完整",DataManager.settingStage);
                return;
            }
            Task<String> task=new Task<String>() {
                @Override
                protected String call() throws Exception {
                    return  ProxyUtil.testProxy(host, port);
                }
            };
            ProgressFrom pf=new ProgressFrom(DataManager.settingStage,task);
            pf.activateProgressBar();
            task.setOnSucceeded(es->{
                pf.cancelProgressBar();
                if(task.getValue()==null){
                    ToastUtil.toast("代理无效");
                }else {
                    AlertUtil.getAlert("代理信息",task.getValue()).show();
                }
            });
        });

        saveProxy.setOnMouseClicked(e->{
            String host = proxyHost.getText();
            String port = proxyPort.getText();
            if(host!=null&&"".equals(host)&&port!=null&&"".equals(port)){
                ToastUtil.toast("请填写完整",DataManager.settingStage);
                return;
            }
            ConfUtil.set(ConfUtil.PROXY_HOSTNAME,host);
            ConfUtil.set(ConfUtil.PROXY_PORT,port);
            ToastUtil.toast("保存成功",DataManager.settingStage);
        });
    }

    //保存更新设置
    public static void updateSetting() {
        new Thread(() -> {
            SettingMapper mapper = MybatisUtils.getMapper(SettingMapper.class);
            mapper.updateSetting(config);
            MybatisUtils.getCurrentSqlSession().close();
        }).start();
    }
}
