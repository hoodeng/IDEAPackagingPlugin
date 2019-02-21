package com.tengteng.transform

import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent.ContentType
import com.android.build.api.transform.QualifiedContent.Scope
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import javassist.CtMethod
import org.apache.commons.io.FileUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import org.gradle.internal.impldep.org.apache.commons.codec.digest.DigestUtils;

/**
 * Prints out some information and copies inputs to outputs.
 */
public class AppClassTransform extends Transform {

    protected
    def ignoreClzList = ["com.vip.sdk.base.BaseApplication", "com.vipshop.vshhc.base.FlowerApplication",
                         "com.vip.sdk.patcher.PatcherHelper", "com.vip.sdk.patcher.dexpatch.DexLoadUtil"]

    protected static ClassPool classesPool
    protected Project mProject
    protected def windows
    protected def hasInjectLib = false

    public AppClassTransform(Project project) {
        mProject = project
        if (classesPool == null) {
            buildClassPool()
        }
        windows = Os.isFamily(Os.FAMILY_WINDOWS)

    }

    @Override
    public String getName() {
        return this.class.name;
    }

    @Override
    public Set<ContentType> getInputTypes() {
        return ImmutableSet.<ContentType> of(QualifiedContent.DefaultContentType.CLASSES);
    }

    @Override
    Set<ContentType> getOutputTypes() {
        return ImmutableSet.<ContentType> of(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    public Set<Scope> getScopes() {
        return Sets.immutableEnumSet(Scope.PROJECT, Scope.SUB_PROJECTS);
    }


    @Override
    Set<Scope> getReferencedScopes() {
        return Sets.immutableEnumSet(Scope.EXTERNAL_LIBRARIES, Scope.PROJECT_LOCAL_DEPS,
                Scope.SUB_PROJECTS_LOCAL_DEPS, Scope.PROVIDED_ONLY)
    }


    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {

        println "start transform " + mProject.name
//        Collection<TransformInput> inputs = transformInvocation.inputs
//        TransformOutputProvider outputProvider = transformInvocation.outputProvider
//        Collection<TransformInput> referencedInputs = transformInvocation.referencedInputs
//
//        referencedInputs.each { TransformInput input ->
//            input.jarInputs.each { JarInput jarInput ->
//                classesPool.appendClassPath(jarInput.file.absolutePath)
//            }
//            input.directoryInputs.each { DirectoryInput dirInput ->
//                classesPool.appendClassPath(dirInput.file.absolutePath)
//            }
//        }
////        }
//        inputs.each { TransformInput input ->
//            def i = 0
//            input.jarInputs.each { JarInput jarInput ->
//                classesPool.appendClassPath(jarInput.file.absolutePath)
//            }
//            input.jarInputs.each { JarInput jarInput ->
//                if (hasInjectLib) {
//                    def dest = outputProvider.getContentLocation(jarInput.file.name + i, jarInput.contentTypes, jarInput.scopes, Format.JAR)
//                    i++
//                    FileUtils.copyFile(jarInput.file, dest);
//                } else {
//                    def newJar = injectJar(jarInput.file.absolutePath);
//                    def dest = outputProvider.getContentLocation(jarInput.file.name + i, jarInput.contentTypes, jarInput.scopes, Format.JAR)
//                    i++
//                    FileUtils.copyFile(newJar, dest);
//                }
//            }
//            hasInjectLib = true
//
//            input.directoryInputs.each { DirectoryInput dirInput ->
//                def dest = outputProvider.getContentLocation(dirInput.name, dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY);
//                def tmpJar = new File(dirInput.file.parentFile.absolutePath, "tmp.jar")
//                JarUtils.zipJar(dirInput.file.absolutePath, new JarOutputStream(new FileOutputStream(tmpJar)))
//                try {
//                    FileUtils.deleteDirectory(dirInput.file)
//                } catch (Exception e) {
//                }
//                List clzList = JarUtils.unzipJar(tmpJar.absolutePath, dirInput.file.absolutePath)
//                def classCollection = getClassFromDir(clzList)
//                tmpJar.delete()
//                if (!classCollection.empty) {
//                    classesPool.appendClassPath(dirInput.file.absolutePath)
//                    modifyConstructor(dirInput.file.absolutePath, classCollection)
//
//                }
//
//                FileUtils.copyDirectory(dirInput.getFile(), dest);
//            }
//
//        }
//
//        ClassPool.'defaultPool' = null
//        buildClassPool()

        Collection<TransformInput> inputs = transformInvocation.inputs;
        TransformOutputProvider outputProvider = transformInvocation.outputProvider;

        inputs.each { transformInput ->
            // Bypass the directories
            transformInput.directoryInputs.each { directoryInput ->

                injectDir(directoryInput.file.absolutePath)

                File dest = outputProvider.getContentLocation(
                        directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY);
                FileUtils.copyDirectory(directoryInput.file, dest)
            }

            // Filter the jars
            transformInput.jarInputs.each { jarInput ->

                def destName = jarInput.name
//
//                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
//                if(jarName.endsWith(".jar")) {
//                    jarName = jarName.substring(0,jarName.length()-4)
//                }
                File dest = outputProvider.getContentLocation(
                        destName, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }
        println "-------transform module " + mProject.name + "  end-----------";
    }


    private ClassPool pool = ClassPool.getDefault()

    public void injectDir(String path) {
        pool.appendClassPath(path)
        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->

                String filePath = file.absolutePath
                System.out.println("injectDir filePath----> " + filePath)
                //确保当前文件是class文件，并且不是系统自动生成的class文件
                if (file.getName().equals("FlowerApplication.class")) {
                    def propValue = mProject.findProperty("channels")

                    println("============================================================")
                    println("propValue ------->  " + propValue);
                    println("============================================================")


                    if ("360".equalsIgnoreCase(propValue)) {
                        println("hook 360 ==========================")
                        String hookStr = """ return new com.vipshop.vshhc.base.utils.ProductFlavors.Flavor("android_360", "q9t0027k:al80ssgp:a4q7rxzj:q9t0027e"); """
                        hookChannel(path,hookStr);
                    } else if ("yingyongbao".equalsIgnoreCase(propValue)) {
                        println("hook yingyongbao ==========================")

                        String hookStr = """ return new com.vipshop.vshhc.base.utils.ProductFlavors.Flavor("安卓_腾讯应用宝", "q9t0027u:al80ssgp:a4q7rxzj:q9t0027o"); """
                        hookChannel(path,hookStr);
                    }
                }
            }
        }
    }

    private void hookChannel(String path,String hook) {
        //获取 FlowerApplication.class
        CtClass ctClass = pool.getCtClass("com.vipshop.vshhc.base.FlowerApplication");
        println("ctClass = " + ctClass)
        //解冻
        if (ctClass.isFrozen())
            ctClass.defrost()

        //获取到OnCreate方法
        CtMethod ctMethod = ctClass.getDeclaredMethod("createFlavor")

        println("方法名 = " + ctMethod)

        String insetBeforeStr = """ android.widget.Toast.makeText(this,"我是被插入的Toast代码~!!",android.widget.Toast.LENGTH_SHORT).show();
                                                """

        String insetAfterStr = """ return new com.vipshop.vshhc.base.utils.ProductFlavors.Flavor("安卓_腾讯应用宝", "q9t0027u:al80ssgp:a4q7rxzj:q9t0027o"); """

        //在方法开头插入代码
        ctMethod.insertBefore(hook);
//                    ctMethod.insertAfter(insetAfterStr);

        ctClass.writeFile(path)
        ctClass.detach()//释放
    }

    protected void buildClassPool() {
        classesPool = ClassPool.getDefault()
        def buildSrcPath = mProject.rootProject.projectDir.absolutePath + "/buildSrc"
        classesPool.appendClassPath(buildSrcPath + "/hack_dex.jar")
        classesPool.appendClassPath(buildSrcPath + "/android.jar")
    }

    public File injectJar(String path) {
        File jarFile = new File(path)


        String jarZipDir = jarFile.getParent() + "/" + jarFile.getName().replace('.jar', '')

        List classNameList = JarUtils.unzipJar(path, jarZipDir)

        jarFile.delete()
        def classCollection = new ArrayList<>()
        for (String className : classNameList) {
            if (className.endsWith(".class")
                    && !className.contains('\\R\\$')
                    && !className.contains('/R/\\$')
                    && !className.contains('R.class')
                    && !className.contains("BuildConfig.class")) {
                className = className.substring(0, className.length() - 6)
                classCollection.add(className)
            }
        }
        modifyConstructor(jarZipDir, classCollection)
        JarOutputStream target = new JarOutputStream(new FileOutputStream(path))

        JarUtils.zipJar(jarZipDir, target)
        classesPool.appendClassPath(path)
        try {
            FileUtils.deleteDirectory(new File(jarZipDir))
        } catch (Exception e) {
        }

        return new File(path)
    }

    protected List getClassFromDir(List<String> clzList) {
        List result = new ArrayList();

        for (String className : clzList) {
            if (className.endsWith(".class")) {
                if (className.endsWith('.R.class')
                        || className.contains('.R$')
                        || className.endsWith("BuildConfig.class")
                        || className.startsWith("android.")) {
                    continue
                }
                className = className.substring(0, className.length() - 6)
                if (className in ignoreClzList) {
                    continue
                }
//                println("inject " + className)
                result.add(className)

            }
        }

        return result
    }

    protected void getClassFromDir(File dir, List result, String pre) {
        dir.eachFileRecurse { File f ->
            if (!f.directory) {
                boolean vaildInput = true
                if (windows) {
                    if (f.absolutePath.contains("\\R\$")
                            || f.absolutePath.contains("\\R.class")
                            || f.absolutePath.contains("\$")
                            || f.absolutePath.endsWith("BuildConfig.class")
                            || f.absolutePath.endsWith("ClassVerifier.class")) {
                        vaildInput = false
                    }
                } else {
                    if (f.absolutePath.contains("/R/\$")
                            || f.absolutePath.contains("/R.class")
                            || f.absolutePath.contains("\$")
                            || f.absolutePath.endsWith("BuildConfig.class")
                            || f.absolutePath.endsWith("ClassVerifier.class")) {
                        vaildInput = false
                    }
                }
                if (vaildInput) {
                    def parentDir = f.parent
                    if (!parentDir.endsWith("dexpatch")
                            && !parentDir.endsWith("reflect")) {
                        String className = f.absolutePath.replace(".class", "");
                        className = className.replace(pre, "")
                        if (windows) {
                            className = className.replaceAll("\\\\", ".")
                        } else {
                            className = className.replaceAll("/", ".")
                        }
                        if (className in ignoreClzList) {
                            return
                        }
                        result.add(className)
                    }

                }
            }


        }
    }


    protected List<String> getClassNameFromJar(String jarPath) {
        List<String> myClassName = new ArrayList<String>()
        if (jarPath.contains("com.android.support")) {
            return null
        }
        if (jarPath.contains("exploded-aar")) {
            try {
                JarFile jarFile = new JarFile(jarPath)
                Enumeration<JarEntry> entrys = jarFile.entries()
                while (entrys.hasMoreElements()) {
                    JarEntry jarEntry = entrys.nextElement()
                    String entryName = jarEntry.getName()
                    if (entryName.endsWith("BuildConfig.class")) {
                        continue
                    }
                    def className = entryName.substring(0, entryName.lastIndexOf(".class"))
                    className = className.replaceAll("/", ".")
                    myClassName.add(className)
//                    print(className + "\n")
                }
            } catch (Exception e) {
                print(e.getMessage())
            }

        }
        return myClassName
    }

    protected void modifyConstructor(String buildDir, List<String> classSet) {
        if (classSet != null && !classSet.empty) {
            classSet.each { String className ->

//                println("add default Constructor " + className)
                try {
                    CtClass c = classesPool.getCtClass(className)
                    if (!c.interface) {
                        if (c.isFrozen()) {
                            c.defrost()
                        }
                        def constructors = c.getConstructors()
                        if (constructors != null && constructors.length > 0) {
                            def constructor = constructors[0];
                            constructor.insertBefore("if(com.vip.sdk.plugin.ClassVerifier.PREVENT_VERIFY){" +
                                    "System.out.println(com.vip.sdk.plugin.HackLoger.class);" +
                                    "}")
                            c.writeFile(buildDir)
                        }

                    }
                } catch (Exception e) {
                    println("modify default Constructor " + className + " got a exception")
                    throw e
                }
            }
        }


    }

    protected void printDirContent(File dir) {
        if (dir.isDirectory()) {
            File[] contents = dir.listFiles()
            for (File subFile : contents) {
                printDirContent(subFile)
            }
        } else {
            print(dir.absolutePath)
        }
    }


}