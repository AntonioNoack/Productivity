package me.antonio.noack.tasks

import me.antonio.noack.tasks.Scanner.isWatchedFile
import me.antonio.noack.tasks.WebAPI.getText

object FileHistory {

    // todo lol
    // - make it easy
    // - make it simple
    // - make it practical
    // - make it cool ^^

    // todo test example
    //      here are all my examples for what it shall do...
    // todo - make me butter #lol
    // todo - improve it #people
    // done - sth that is done #coaster
    //      that it is simpler than you'd expect

    const val char0 = 0.toChar()
    const val str0 = char0.toString()

    fun init(projectName: String, watcher: FileWatcher){
        // download all old stuff from the database
        val text = getText("hist/listAll.php?project=$projectName")
        val lines = text.replace('\r', ' ').split("###").map { it.trim() }
        // println(text)
        val newestOnes = HashMap<Int, Entry>()
        var first = true
        for(line in lines){
            // println("$first $line")
            if(first){
                first = false
                // the info about what is what
            } else {
                val parts = line.replace("\\;", str0).split(';').map { it.replace(char0, ';').trim() }
                if(parts.size > 6){
                    val taskUID = parts[0].toIntOrNull() ?: continue
                    val version = parts[1].toIntOrNull() ?: continue
                    val taskText = parts[2]
                        .replace("\\n", "\n")
                        .replace("\\\\", "\\")
                    val created = parts[3]
                    val fileName = parts[4]
                    val lastPartOfName = fileName.split('/').last()
                    if(isWatchedFile(lastPartOfName)){
                        val todoIndex = parts[5].toIntOrNull() ?: continue
                        val taskFlags = parts[6].split(',').map { it.trim() }.toHashSet()
                        val entry = Entry(taskUID, version, taskText, created, fileName, todoIndex, taskFlags)
                        val oldEntry = newestOnes[taskUID]
                        println("got $fileName/$entry, $version > ${oldEntry?.version}")
                        if(oldEntry == null || entry.version > oldEntry.version){
                            newestOnes[taskUID] = entry
                        }
                    }//  else println("ignored $lastPartOfName/$fileName")
                }
            }
        }
        for((_, entry) in newestOnes){
            watcher.addEntryByWeb(entry)
        }
    }

}