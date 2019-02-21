package com.tengteng.transform

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * Created by hp on 2016/4/13.
 */
public class JarUtils {


    public static List unzipJar(String jarPath, String destDirPath) {

        List list = new ArrayList()
        if (jarPath.endsWith('.jar')) {

            JarFile jarFile = new JarFile(jarPath)
            Enumeration<JarEntry> jarEntrys = jarFile.entries()
            while (jarEntrys.hasMoreElements()) {
                JarEntry jarEntry = jarEntrys.nextElement()
                if (jarEntry.directory) {
                    continue
                }
                String entryName = jarEntry.getName()
                if (entryName.endsWith('.class')) {
                    String className = entryName.replace('\\', '.').replace('/', '.')
                    list.add(className)
                }
                String outFileName = destDirPath + "/" + entryName
                File outFile = new File(outFileName)
                outFile.getParentFile().mkdirs()
                InputStream inputStream = jarFile.getInputStream(jarEntry)
                FileOutputStream fileOutputStream = new FileOutputStream(outFile)
                fileOutputStream << inputStream
                fileOutputStream.close()
                inputStream.close()
            }
            jarFile.close()
        }
        return list
    }


    public static void zipJar(String packagePath, JarOutputStream os) {

        File file = new File(packagePath)
        file.eachFileRecurse { File f ->
            if (f.directory) {
                String name = f.getPath().substring(packagePath.length() + 1).replace("\\", "/");
                if (!name.isEmpty()) {
                    if (!name.endsWith("/"))
                        name += "/";
                    JarEntry entry = new JarEntry(name);
                    entry.setTime(f.lastModified());
                    os.putNextEntry(entry);
                    os.closeEntry();
                }

            } else {
                String name = f.getPath().substring(packagePath.length() + 1).replace("\\", "/");
                JarEntry entry = new JarEntry(name);
                entry.setTime(f.lastModified());
                os.putNextEntry(entry);
                InputStream inputStream = new FileInputStream(f)
                os << inputStream
                inputStream.close()
                os.closeEntry();
            }
        }
        os.close()
    }
}