package me.antonio.noack.tasks

import me.antonio.noack.all.key
import java.lang.RuntimeException
import java.net.URL
import java.net.URLEncoder

object WebAPI {

    private fun encode(value: String): String {
        return URLEncoder.encode(value)
    }

    fun getText(site: String): String = getText(URL("https://api.phychi.com/$site"))

    fun getText(url: URL): String {
        val con = url.openConnection()
        val input = con.getInputStream()
        val bytes = input.readAllBytes()
        input.close()
        println(bytes.joinToString(" "))
        val text = String(bytes)
        if(text.startsWith("#")){
            throw RuntimeException(text)
        } else {
            return text
        }
    }

    // Project, TaskUID, Version, TaskText, FileName, TodoIndex, TaskFlags
    fun addChange(projectName: String, taskUID: Int, taskText: String, path: String, todoIndex: Int, taskFlags: List<String>){
        WebAPI.getText("hist/add.php?key=${encode(key)}&" +
                "project=${encode(projectName)}&" +
                "tuid=$taskUID&" +
                "ttxt=${encode(taskText)}&" +
                "file=${encode(path)}&" +
                "tdix=$todoIndex&" +
                "flgs=${taskFlags.joinToString(","){ encode(it) }}")
    }

    // Project, TaskUID, Version, TaskText, FileName, TodoIndex, TaskFlags
    fun addEntry(projectName: String, entry: Entry){
        WebAPI.getText("hist/add.php?key=${encode(key)}&" +
                "project=${encode(projectName)}&" +
                "tuid=${entry.taskUID}&" +
                "ttxt=${encode(entry.taskText)}&" +
                "file=${encode(entry.path)}&" +
                "tdix=${entry.todoIndex}&" +
                "flgs=${entry.taskFlags.joinToString(","){ encode(it) }}")
    }

    fun addFileRemoval(projectName: String, path: String){
        WebAPI.getText("hist/delete.php?key=${encode(key)}&" +
                "project=${encode(projectName)}&" +
                "file=${encode(path)}")
    }

    fun addEntryRemoval(projectName: String, tuid: Int){
        WebAPI.getText("hist/delete.php?key=${encode(key)}&" +
                "project=${encode(projectName)}&" +
                "tuid=$tuid")
    }

}