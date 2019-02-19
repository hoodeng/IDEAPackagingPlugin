package com.tengteng.packaging;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PackagingPlugin extends AnAction {

    private final List<String> mSelectedChannels = new ArrayList<>();

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getData(PlatformDataKeys.PROJECT);


        System.out.println("project --> " + project.getProjectFilePath() + "  " + project.getProjectFile().getPath() + " " + project.getBasePath());
        String batPath = project.getBasePath();
//        String batPath = "/Users/wudeng/workspace/vs_whole_sale/hhc_wholesale_android";
        Map<String, Channel> channels = parseChannels(batPath, "/app/src/main/assets/favors.json");
        if (channels == null || channels.isEmpty()) {
            String title = "wings";
            String msg = "wings插件标示初始化异常";
            Messages.showMessageDialog(project, msg, title, Messages.getInformationIcon());
        } else {
            JFrame jFrame = new MainJFrame(project,batPath,channels);
            jFrame.setTitle("wings");
            jFrame.setSize(800, 800);
            jFrame.setLocationRelativeTo(null);
            jFrame.setVisible(true);
        }
    }

    private static Map<String, Channel> parseChannels(String projectPath, String relativePath) {
        try {
            File file = new File(projectPath + relativePath);
            if (file.exists()) {
                System.out.println("exsit");

                StringBuffer sb = new StringBuffer();
//                BufferedReader br = new BufferedReader(new FileReader(file));// 读取原始json文件
                BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
                String s;
                while ((s = br.readLine()) != null) {
                    sb.append(s);
                }

                System.out.println(sb);

//                java.lang.reflect.Type type = new TypeToken<HashMap<String, Channel>>() {
//                }.getType();

                TreeMap<String, Channel> channelMap = new Gson().fromJson(sb.toString(), new TypeToken<TreeMap<String, Channel>>() {
                }.getType());
                if (channelMap != null) {
                    for (Map.Entry entry : channelMap.entrySet()) {
//                        System.out.println(entry.getKey() + entry.getValue().getClass().getSimpleName());
                        System.out.println(entry.getKey() + "   " + ((Channel) entry.getValue()).NAME + "  " + ((Channel) entry.getValue()).CPS);
                    }
                }
                return channelMap;
            } else {
                System.out.println("no exsit");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
