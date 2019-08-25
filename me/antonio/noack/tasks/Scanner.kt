package me.antonio.noack.tasks

import java.io.File

object Scanner {

    fun min(a: Int, b: Int): Int {
        return if(a < b) a else b
    }

    fun min(a: Int, b: Int, c: Int): Int {
        val d = if(a < b) a else b
        return if(d < c) d else c
    }

    fun getSrcFolder(projectFolder: File): File {
        val src1 = File(projectFolder, "app/src/main")
        val src2 = File(projectFolder, "src/main")
        val src3 = File(projectFolder, "src")
        return when {
            src1.exists() -> src1
            src2.exists() -> src2
            src3.exists() -> src3
            else -> projectFolder
        }
    }

    fun scanSrcFolder(srcFolder: File, watcher: FileWatcher?){
        traverseAll(srcFolder, "", { _ , _ -> // is already done in watcher.init
            // val name = child.name
            // scan(child, if(path.isEmpty()) name else "$path/$name")
        }, { folder, path ->
            watcher?.addListener(folder, path)
        })
    }

    fun traverseAll(folder: File, path: String, onFile: (child: File, path: String) -> Unit, onFolder: (folder: File, path: String) -> Unit){
        val flc = folder.name.toLowerCase()
        if(path.isEmpty() && when(flc){
                "java" -> true // java is discarded
                "cpp", "resources" -> false // cpp and resources are kept in paths
                else -> false
            }){
            for(child in folder.listFiles()){
                if(!child.name.startsWith(".")){
                    onFolder(child, "")
                    traverseAll(child, "", onFile, onFolder)
                }
            }
        } else {
            for(child in folder.listFiles()){
                val name = child.name
                if(!name.startsWith(".")){
                    if(child.isDirectory){
                        val newPath = if(path.isEmpty()) name else "$path/$name"
                        onFolder(child, newPath)
                        traverseAll(child, newPath, onFile, onFolder)
                    } else {
                        if(Scanner.isWatchedFile(name)){
                            onFile(child, if(path.isEmpty()) name else "$path/$name")
                        }
                    }
                }
            }
        }
    }

    fun isWatchedFile(name: String): Boolean {
        val ending = if(name.contains('.')) name.split('.').last() else ""
        return when(ending.toLowerCase()){
            "java", "kt", "c", "cc", "", "cpp", "h", "hpp" -> true
            "xml" -> true
            "json" -> false // sure?...
            else -> false
        }
    }

}