package me.antonio.noack.all

import me.antonio.noack.tasks.FileHistory
import me.antonio.noack.tasks.FileWatcher
import me.antonio.noack.tasks.Scanner
import me.antonio.noack.tasks.getPass
import java.io.File

val key = getPass()

val flagDeleted = "deleted"

fun main(){

    val projectFolder = File("/home/antonio/IdeaProjects/Productivity2")
    val projectName = projectFolder.name

    val srcFolder = Scanner.getSrcFolder(projectFolder)

    val watcher = FileWatcher(projectName, srcFolder)
    watcher.initFileList()

    FileHistory.init(projectName, watcher)
    Scanner.scanSrcFolder(srcFolder, watcher)

}