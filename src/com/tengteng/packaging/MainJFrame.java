package com.tengteng.packaging;


import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MainJFrame extends JFrame {


    private final Map<String, Channel> mSelectedChannels = new HashMap<>();

    private Map<String, Channel> channels;

    private final String batPath;

    private Project project;

    public MainJFrame(Project project,String batPath, Map<String, Channel> channels) {
        this.project = project;
        this.batPath = batPath;
        if (channels == null) return;
        // 创建内容面包容器，指定使用 边界布局
        JPanel panel = new JPanel(new BorderLayout());


        Box channelsBox = Box.createVerticalBox();

        for (Map.Entry<String, Channel> entry : channels.entrySet()) {
            String key = entry.getKey();
            Channel channel = entry.getValue();

            JCheckBox checkBox = new JCheckBox(channel.NAME);
            channelsBox.add(checkBox);
            checkBox.addItemListener(e -> {
                System.out.println(channel + " 选中了  ---- > " + (e.getStateChange() == 1));
                boolean selected = e.getStateChange() == 1;
                if (selected) {
                    mSelectedChannels.put(key, channel);
                } else {
                    mSelectedChannels.remove(key);
                }
            });
        }

        JScrollPane wrapperPanel = new JScrollPane(channelsBox, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(wrapperPanel, BorderLayout.NORTH);

        Box bottomBox = Box.createHorizontalBox();
        Button cancelButton = new Button("取消");
        bottomBox.add(cancelButton);
        Button okButton = new Button("确认");
        bottomBox.add(okButton);

        okButton.addActionListener(e -> {
            System.out.println("开始打包");

//            exeCmd();

//            exeCmdMock();
//            setVisible(false);

            setVisible(false);

            ProgressManager.getInstance().run(new Task.Backgroundable(project,"wings") {
                @Override
                public void run(@NotNull ProgressIndicator progressIndicator) {
//                    exeCmdMock(progressIndicator);
                    exeCmd(progressIndicator);
                }
            });
        });

        cancelButton.addActionListener(e -> {
            System.out.println("取消");
            setVisible(false);
        });

        panel.add(bottomBox, BorderLayout.SOUTH);

        this.setContentPane(panel);
        this.addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {

            }

            @Override
            public void windowClosing(WindowEvent e) {

            }

            @Override
            public void windowClosed(WindowEvent e) {
                setVisible(false);
            }

            @Override
            public void windowIconified(WindowEvent e) {

            }

            @Override
            public void windowDeiconified(WindowEvent e) {

            }

            @Override
            public void windowActivated(WindowEvent e) {

            }

            @Override
            public void windowDeactivated(WindowEvent e) {

            }
        });
    }

    private void exeCmdMock(ProgressIndicator progressIndicator) {
        int count = 0;
        Logger log = Logger.getInstance(MainJFrame.class);
        while (count < 10) {
            System.out.println("execute cmd mock + " + count);
            log.info("execute cmd mock + " + count);

            progressIndicator.setText("execute cmd mock + " + count);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
        }
        setVisible(false);
    }

    private void exeCmd(ProgressIndicator progressIndicator) {
        if (mSelectedChannels.isEmpty()) return;

        StringBuffer channels = new StringBuffer();

        for (Map.Entry<String, Channel> entry : mSelectedChannels.entrySet()) {
            if (channels.length() > 0) {
                channels.append(",");
            }
            channels.append(entry.getKey());
        }


        System.out.println(channels.toString());
        runbat(batPath, "gradlew", channels.toString(),progressIndicator);
    }

    private void exeCmd(String cmd) throws IOException, InterruptedException {
        Runtime runtime = Runtime.getRuntime();
        Process pro = runtime.exec(cmd);
        int status = pro.waitFor();
        if (status != 0) {
            System.out.println("Failed to call shell's command ");
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(pro.getInputStream()));
        StringBuffer strbr = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            strbr.append(line).append("\n");
        }

        String result = strbr.toString();
        System.out.println(result);
    }

    public void runbat(String batPath, String batName, String channels,ProgressIndicator progressIndicator) {
        String os = System.getProperty("os.name");
        if (!os.toLowerCase().contains("windows")) {
            batName = "./" + batName;
        }else{
            batName = batPath+ File.separator + batName +".bat";
        }
        System.out.println("batName ---> " + batName);


        ProcessBuilder pb;
        if (channels != null) {
            pb = new ProcessBuilder(batName, "clean", "apkRelease", "-Pchannels=" + channels);
        } else {
            pb = new ProcessBuilder(batName, "clean", "apkRelease");
        }
        pb.directory(new File(batPath));
        int runningStatus = 0;
        String s = null;
        try {
            Process p = pb.start();
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
                log(progressIndicator,s);
            }
            try {
                runningStatus = p.waitFor();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showMessageDialog(project, "请在" + batPath + File.separator + "build 目录下面查看包" , "编译成功 ", Messages.getInformationIcon());
                    }
                });
            } catch (InterruptedException e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Messages.showMessageDialog(project, "InterruptedException " + e.getMessage(), "编译异常 ", Messages.getInformationIcon());
                    }
                });
            }
        } catch (IOException e) {
            log(progressIndicator,"IOException  --> " + e.getMessage());
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    Messages.showMessageDialog(project, "IOException " + e.getMessage(), "编译异常 ", Messages.getInformationIcon());
                }
            });
        }
        if (runningStatus == 0) {
            System.out.println("running");
            log(progressIndicator,"running");
        } else {
            System.out.println("exist");
            log(progressIndicator,"exist");
        }
    }


    void log( ProgressIndicator progressIndicator,String text){
        progressIndicator.setText(text);
    }


}
